// Tear-free window recorder built on ScreenCaptureKit.
//
// `screencapture -v` samples the WindowServer without vsync alignment, so a
// window repainting mid-sample shows as a horizontal tear line in the frame.
// ScreenCaptureKit delivers complete, vsync-aligned frames via callback, at a
// requested rate (60fps here), which also beats screencapture's ~55fps.
//
// Build: swiftc -O sckrecord.swift -o sckrecord   (macOS 13+)
// Usage: sckrecord <windowID> <out.mov> [fps]     stop with SIGINT (Ctrl-C)
import AppKit
import AVFoundation
import CoreMedia
import Foundation
import ScreenCaptureKit

// ScreenCaptureKit asserts (CGS_REQUIRE_INIT) unless the process has a
// WindowServer connection; touching NSApplication establishes one.
_ = NSApplication.shared

let args = CommandLine.arguments
guard args.count >= 3, let windowID = UInt32(args[1]) else {
    FileHandle.standardError.write(Data("usage: sckrecord <windowID> <out.mov> [fps]\n".utf8))
    exit(2)
}
let outURL = URL(fileURLWithPath: args[2])
let fps = args.count > 3 ? Int32(args[3]) ?? 60 : 60
try? FileManager.default.removeItem(at: outURL)

final class Recorder: NSObject, SCStreamOutput {
    let writer: AVAssetWriter
    let input: AVAssetWriterInput
    let adaptor: AVAssetWriterInputPixelBufferAdaptor
    var started = false
    var frames = 0

    init(url: URL, width: Int, height: Int) throws {
        writer = try AVAssetWriter(outputURL: url, fileType: .mov)
        input = AVAssetWriterInput(mediaType: .video, outputSettings: [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: width,
            AVVideoHeightKey: height,
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: 40_000_000,
                AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
            ],
        ])
        input.expectsMediaDataInRealTime = true
        adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: input, sourcePixelBufferAttributes: nil)
        writer.add(input)
        writer.startWriting()
    }

    func stream(_ stream: SCStream, didOutputSampleBuffer sb: CMSampleBuffer, of type: SCStreamOutputType) {
        guard type == .screen, sb.isValid,
              let attachments = CMSampleBufferGetSampleAttachmentsArray(sb, createIfNecessary: false)
                  as? [[SCStreamFrameInfo: Any]],
              let statusRaw = attachments.first?[.status] as? Int,
              statusRaw == SCFrameStatus.complete.rawValue,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sb) else { return }
        let pts = CMSampleBufferGetPresentationTimeStamp(sb)
        if !started {
            writer.startSession(atSourceTime: pts)
            started = true
        }
        if input.isReadyForMoreMediaData {
            adaptor.append(pixelBuffer, withPresentationTime: pts)
            frames += 1
        }
    }

    func finish() {
        input.markAsFinished()
        let sema = DispatchSemaphore(value: 0)
        writer.finishWriting { sema.signal() }
        sema.wait()
        print("sckrecord: wrote \(frames) frames")
    }
}

let sema = DispatchSemaphore(value: 0)
var recorder: Recorder?
var stream: SCStream?

SCShareableContent.getExcludingDesktopWindows(false, onScreenWindowsOnly: false) { content, error in
    guard let window = content?.windows.first(where: { $0.windowID == windowID }) else {
        FileHandle.standardError.write(Data("sckrecord: window \(windowID) not found\n".utf8))
        exit(1)
    }
    let scale = 2  // retina backing
    let cfg = SCStreamConfiguration()
    cfg.width = Int(window.frame.width) * scale
    cfg.height = Int(window.frame.height) * scale
    cfg.minimumFrameInterval = CMTime(value: 1, timescale: fps)
    cfg.queueDepth = 8
    cfg.showsCursor = false
    do {
        let rec = try Recorder(url: outURL, width: cfg.width, height: cfg.height)
        recorder = rec
        let s = SCStream(filter: SCContentFilter(desktopIndependentWindow: window),
                         configuration: cfg, delegate: nil)
        try s.addStreamOutput(rec, type: .screen,
                              sampleHandlerQueue: DispatchQueue(label: "sck.frames"))
        stream = s
        s.startCapture { err in
            if let err { FileHandle.standardError.write(Data("start failed: \(err)\n".utf8)); exit(1) }
            print("sckrecord: recording window \(windowID) at \(fps)fps")
        }
    } catch {
        FileHandle.standardError.write(Data("sckrecord: \(error)\n".utf8))
        exit(1)
    }
}

signal(SIGINT, SIG_IGN)
let sig = DispatchSource.makeSignalSource(signal: SIGINT, queue: .main)
sig.setEventHandler {
    stream?.stopCapture { _ in
        recorder?.finish()
        sema.signal()
        exit(0)
    }
}
sig.resume()
dispatchMain()
