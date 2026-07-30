#!/bin/bash
# Rebuilds the release wasm distribution into docs/ for GitHub Pages.
#
# GitHub Pages serves the repo's docs/ folder at
# https://luca992.github.io/portside-cmp/ — the checked-in dist IS the
# deployment; no external hosting. Re-run this and commit whenever the app
# changes. (Resources are copied in manually: the 0.12-dev toolchain doesn't
# package composeResources for wasm — see scripts/serve-web.sh.)
set -euo pipefail
cd "$(dirname "$0")/.."
./kotlin build -m web-app --variant=release
rm -rf docs
mkdir -p docs
cp -R build/tasks/_web-app_buildWasmJsAppWasmJsRelease/. docs/
cp -R build/artifacts/JvmResourcesDirArtifact/sharedcommon/composeResources docs/
touch docs/.nojekyll   # Jekyll would ignore files/dirs it dislikes; serve as-is

# Optimize web-app.wasm with binaryen: -O3 halves the file (15MB -> 7MB, the
# toolchain ships the debug name section and runs no binaryen itself) and
# optimizes the generated code. PINNED to binaryen 123: versions >= 130
# re-encode with "exact heap types" (custom-descriptors proposal) that
# browsers don't accept yet. Falls back to shipping the unoptimized wasm if
# the download fails.
BINARYEN=tools/binaryen-version_123/bin/wasm-opt
if [ ! -x "$BINARYEN" ]; then
  mkdir -p tools
  curl -sL https://github.com/WebAssembly/binaryen/releases/download/version_123/binaryen-version_123-arm64-macos.tar.gz \
    | tar xz -C tools 2>/dev/null || true
fi
if [ -x "$BINARYEN" ]; then
  # KGP-style pass: --closed-world + --gufa x2 is what the Kotlin Gradle
  # plugin's production builds run, and it's another ~15% smaller than plain
  # -O3 (see KT-78426 for the measurements). Deliberately NOT using
  # --traps-never-happen / --fast-math: those are the flags implicated in
  # "RuntimeError: unreachable" miscompiles and binaryen crashes.
  "$BINARYEN" \
    --enable-gc --enable-reference-types --enable-exception-handling \
    --enable-bulk-memory --enable-nontrapping-float-to-int --enable-sign-ext \
    --enable-mutable-globals --enable-multivalue \
    --closed-world -O3 --gufa -O3 \
    --strip-debug --strip-producers \
    docs/web-app.wasm -o docs/web-app.wasm.opt
  mv docs/web-app.wasm.opt docs/web-app.wasm
  echo "web-app.wasm optimized with binaryen -O3"
else
  echo "WARNING: binaryen unavailable; shipping unoptimized wasm"
fi


echo "docs/ ready ($(du -sh docs | cut -f1)) — commit and push to deploy"
