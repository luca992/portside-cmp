package portside.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.imageResource
import shared.generated.resources.Res
import shared.generated.resources.earth_tex
import portside.model.Sailing
import portside.model.SailingStatus
import portside.ui.PortsideColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Orthographic (true globe) projection: the earth as a sphere seen from space.
 * Works in whatever length unit its center/radius were given in.
 */
internal class GlobeView(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val centerLonDeg: Double,
    val centerLatDeg: Double,
) {
    private val lam0 = centerLonDeg * PI / 180
    private val phi0 = centerLatDeg * PI / 180
    private val sinPhi0 = sin(phi0)
    private val cosPhi0 = cos(phi0)

    /** [visible] is false on the far hemisphere (point clamped to the limb). */
    fun project(lonDeg: Double, latDeg: Double): Triple<Float, Float, Boolean> {
        val lam = lonDeg * PI / 180 - lam0
        val phi = latDeg * PI / 180
        val cosC = sinPhi0 * sin(phi) + cosPhi0 * cos(phi) * cos(lam)
        var x = (cos(phi) * sin(lam)).toFloat()
        var y = (cosPhi0 * sin(phi) - sinPhi0 * cos(phi) * cos(lam)).toFloat()
        if (cosC < 0) {
            val len = sqrt(x * x + y * y).coerceAtLeast(1e-6f)
            x /= len
            y /= len
        }
        return Triple(centerX + radius * x, centerY - radius * y, cosC >= 0)
    }
}

internal fun globeViewFor(
    sailing: Sailing?,
    width: Float,
    height: Float,
    lonOffset: Double = 0.0,
    /** Sphere size basis: capped below [width] on wide hosts so the globe
     *  stops growing and outer space fills the margins instead. */
    globeWidth: Float = width,
    /** Where the sheet's top edge sits, as a fraction of [height]. */
    sheetTopFraction: Float = 0.34f,
): GlobeView {
    val baseRadius = globeWidth * 0.80f
    if (sailing == null) {
        // North America + Atlantic + Europe across the top, drifting westward.
        return GlobeView(
            centerX = width / 2f,
            centerY = height * 0.055f + baseRadius,
            radius = baseRadius,
            centerLonDeg = -50.0 - lonOffset,
            centerLatDeg = 22.0,
        )
    }

    // Ferry crossings span a few degrees, not an ocean: scale the sphere up
    // until the route occupies a readable (but not overwhelming) share of the
    // height. Island hops zoom past the texture's resolution — the sea blurs
    // into a soft field, which is fine for a backdrop.
    val spanRad = (routeSpanDeg(sailing) * PI / 180).coerceAtLeast(0.00035)
    val radius = (height * 0.12f / spanRad.toFloat()).coerceIn(baseRadius, globeWidth * 50f)

    // The crown always rides just below the top of the frame, and the view
    // rotates until the route midpoint sits mid-band beneath it — so even an
    // extreme island-hop zoom still shows the curve of the earth above the
    // route instead of a flat straight-down field.
    val centerY = height * 0.055f + radius
    val route = greatCircle(sailing, samples = 16)
    val mid = route[route.size / 2]
    val targetY = height * sheetTopFraction * 0.5f
    val k = ((targetY - height * 0.055f) / radius).toDouble()
    val deltaDeg = asin((1.0 - k).coerceIn(-1.0, 1.0)) * 180.0 / PI
    return GlobeView(
        centerX = width / 2f,
        centerY = centerY,
        radius = radius,
        centerLonDeg = mid.first,
        centerLatDeg = mid.second - deltaDeg,
    )
}

private class Speck(val x: Float, val y: Float, val r: Float, val alpha: Float)

/** Everything expensive, prebuilt in pixel space and drawn as-is each frame. */
private class GlobeGeometry(
    val view: GlobeView,
    val disk: Path,
    val graticule: Path,
    val land: Path,
    val cityLights: List<Speck>,
    val routePath: Path?,
    val routeStart: Offset?,
    val routeEnd: Offset?,
    val ship: Offset?,
)

/**
 * Draw-phase cache: the globe is rasterized into a bitmap and re-rendered only
 * when the quantized spin angle changes. Skiko (iOS/desktop) has no Android-style
 * render-node caching, so without this every sheet-drag frame would re-fill the
 * 3.5k-point coastline path; with it, each frame is a single texture blit.
 */
private class EarthTexture(val pixels: IntArray, val width: Int, val height: Int)

/** A produced sphere raster tagged with the spin angle it was sampled at. */
private class SphereFrame(
    val bitmap: ImageBitmap,
    val spinDeg: Float,
    /** Screen px the raster slides per degree of further spin. */
    val pxPerDeg: Float,
)

/**
 * Extra sampled margin (screen px) left of the canvas; see the producer.
 * Sized for ~1.8s of producer lag (1.5°/s drift · ~15px/°): the draw-phase
 * slide must never hit this ceiling, because clamping breaks the continuity
 * between consecutive keyframes and the globe visibly snaps.
 */
private const val GLOBE_OVERSCAN_PX = 40f

/** One revolution per 240 s — a slow 1.5°/s idle drift. */
private const val SPIN_PERIOD_NS = 240_000_000_000L
private const val SPIN_DEG_PER_NS = 360.0 / SPIN_PERIOD_NS

private fun buildGlobeGeometry(
    sailing: Sailing?,
    route: List<Pair<Double, Double>>?,
    widthPx: Float,
    heightPx: Float,
    lonOffset: Float,
    globeWidthPx: Float = widthPx,
    sheetTopFraction: Float = 0.34f,
): GlobeGeometry {
    val view = globeViewFor(
        sailing, widthPx, heightPx, lonOffset.toDouble(), globeWidthPx, sheetTopFraction,
    )
    val cx = view.centerX
    val cy = view.centerY
    val r = view.radius

    val disk = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }

    val graticule = Path()
    for (lonLine in -180 until 180 step 30) {
        var started = false
        for (lat in -85..85 step 5) {
            val (x, y, visible) = view.project(lonLine.toDouble(), lat.toDouble())
            if (!visible) { started = false; continue }
            if (!started) { graticule.moveTo(x, y); started = true } else graticule.lineTo(x, y)
        }
    }
    for (latLine in -60..75 step 15) {
        var started = false
        for (lon in -180..180 step 5) {
            val (x, y, visible) = view.project(lon.toDouble(), latLine.toDouble())
            if (!visible) { started = false; continue }
            if (!started) { graticule.moveTo(x, y); started = true } else graticule.lineTo(x, y)
        }
    }

    // All landmasses merged into a single multi-contour path: one fill + one
    // stroke per frame instead of two draws per polygon. Polygons entirely on
    // the far hemisphere are dropped, not just left unclosed.
    val land = Path()
    val contour = Path()
    for (poly in WORLD_LAND) {
        contour.reset()
        var anyVisible = false
        var i = 0
        var moved = false
        while (i < poly.size) {
            val (x, y, visible) = view.project(poly[i].toDouble(), poly[i + 1].toDouble())
            if (visible) anyVisible = true
            if (!moved) { contour.moveTo(x, y); moved = true } else contour.lineTo(x, y)
            i += 2
        }
        if (!anyVisible) continue
        contour.close()
        land.addPath(contour)
    }

    var lightSeed = 977
    fun nextLight(): Float {
        lightSeed = (lightSeed * 1664525 + 1013904223) and 0x7FFFFFFF
        return (lightSeed % 10_000) / 10_000f
    }
    val cityLights = ArrayList<Speck>(160)
    for (poly in WORLD_LAND) {
        var i = 0
        while (i < poly.size) {
            if (nextLight() > 0.88f) {
                val (x, y, visible) = view.project(poly[i].toDouble(), poly[i + 1].toDouble())
                if (visible) {
                    cityLights += Speck(x, y, 0.8f + nextLight(), 0.10f + nextLight() * 0.30f)
                }
            }
            i += 8
        }
    }

    var routePath: Path? = null
    var routeStart: Offset? = null
    var routeEnd: Offset? = null
    var ship: Offset? = null
    if (route != null && sailing != null) {
        val arc = Path()
        var started = false
        for ((lon, lat) in route) {
            val (x, y, visible) = view.project(lon, lat)
            if (!visible) { started = false; continue }
            if (!started) { arc.moveTo(x, y); started = true } else arc.lineTo(x, y)
        }
        routePath = arc
        val (x1, y1, v1) = view.project(route.first().first, route.first().second)
        if (v1) routeStart = Offset(x1, y1)
        val (x2, y2, v2) = view.project(route.last().first, route.last().second)
        if (v2) routeEnd = Offset(x2, y2)
        val t = when (sailing.status) {
            SailingStatus.AtSea -> sailing.progress
            SailingStatus.Docked -> 1f
            else -> 0f
        }
        val shipIndex = ((route.size - 1) * t).toInt().coerceIn(0, route.size - 1)
        val p = route[shipIndex]
        val (px, py, pv) = view.project(p.first, p.second)
        if (pv) ship = Offset(px, py)
    }

    return GlobeGeometry(
        view, disk, graticule, land, cityLights, routePath, routeStart, routeEnd, ship,
    )
}

/**
 * The earth from space, drawn in common code with an orthographic projection
 * of real Natural Earth coastlines. Idle screens drift slowly; the animation
 * only invalidates the draw phase, and projected geometry is cached and
 * rebuilt ~15 times per second (0.1° spin steps ≈ 1px of surface motion).
 */
// Perf A/B switch — false renders a plain dark backdrop with no globe.
// Verified 2026-07-21: sheet-fling jank on iOS is identical with the globe
// disabled, so the backdrop is not a factor.
private const val GLOBE_ENABLED = true

@Composable
fun OceanBackdrop(
    sailing: Sailing?,
    mapHeightFraction: Float,
    modifier: Modifier = Modifier,
) {
    if (!GLOBE_ENABLED) {
        Box(modifier = modifier.fillMaxSize().background(PortsideColors.Sea))
        return
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(PortsideColors.Sea)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // The sheet permanently covers everything below its peek position, so
        // only rasterize (and composite) the visible map band — roughly halves
        // the bitmap that gets drawn every frame.
        // Phones raster only the band the sheet won't cover; hosts wider than
        // the phone column (web/tablet full-bleed backdrop) render the whole
        // visible world instead — the giant disk extends well below the band,
        // and a truncated texture there reads as a bug.
        val fullBleed = maxWidth > PhoneMaxWidth + 40.dp
        // The sphere stops growing past this width — beyond it, the margins
        // are space, not ever-more-gigantic planet.
        val globeWidthPx = minOf(widthPx, with(density) { 900.dp.toPx() })
        val cropFraction = (mapHeightFraction + 0.20f).coerceAtMost(0.60f)
        val cropDp = if (fullBleed) maxHeight else maxHeight * cropFraction
        val cropPx = with(density) { cropDp.toPx() }

        val route = remember(sailing?.id) { sailing?.let { greatCircle(it) } }
        val shipPainter = rememberVectorPainter(AppIcons.Ship)

        // NASA Blue Marble (public domain), pre-darkened for the space look.
        val earthImage = imageResource(Res.drawable.earth_tex)
        // GPU path: per-pixel warp as a runtime shader, spin as a uniform.
        val shaderRenderer = remember(earthImage) {
            if (earthImage.width > 1) createGlobeShaderRenderer(earthImage) else null
        }
        // CPU fallback only: the IntArray copy is ~w*h*4 bytes (33 MB at 4K),
        // so it is never built on hosts the shader serves.
        val earthTex = remember(earthImage, shaderRenderer) {
            if (shaderRenderer != null || earthImage.width <= 1) return@remember null
            val map = earthImage.toPixelMap()
            val w = map.width
            val h = map.height
            val buf = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    buf[y * w + x] = map[x, y].toArgb()
                }
            }
            EarthTexture(buf, w, h)
        }

        // The textured sphere is produced OFF the UI thread: the warp copy and
        // pixel upload cost a few ms, and doing them in the draw phase caused
        // visible hitches whenever a spin step coincided with a sheet drag
        // frame. A background loop double-buffers the sphere at ~30 Hz and
        // publishes it through snapshot state; the draw phase blits it with a
        // sub-pixel horizontal shift that carries the motion between keyframes
        // at full display rate (over ≤0.05° a spin IS a horizontal shift, and
        // the <1px residual at the limb hides under the limb shading). One
        // clock — the spin transition — drives both sampler and shift, so the
        // texture never drifts out of sync with the draw.
        // The spin is an ABSOLUTE function of the frame-clock timestamp, not
        // an accumulated animation: the native-tabbed iOS app overlaps two
        // live canvases during a tab crossfade, and any per-canvas clock
        // (wall-clock transitions pause differently, accumulators diverge by
        // their pause histories) makes the two globes render a couple of
        // degrees apart — the compositor blends them and the globe visibly
        // shakes. Deriving the angle from the shared timestamp means every
        // canvas draws the identical globe, so overlaps are invisible.
        val spinDeg: State<Float>? = if (sailing == null) {
            val spin = remember { mutableStateOf(0f) }
            LaunchedEffect(Unit) {
                while (isActive) {
                    withFrameNanos { now ->
                        spin.value = ((now % SPIN_PERIOD_NS) * SPIN_DEG_PER_NS).toFloat()
                    }
                }
            }
            spin
        } else {
            null
        }
        // Geometry is computed synchronously with the size it will be drawn
        // at: the tabbed iOS host relays out the canvas several times while a
        // tab animates in, and drawing against a stale async geometry made
        // the globe visibly jump around until the producer caught up.
        val geometry = remember(sailing?.id, widthPx, cropPx) {
            buildGlobeGeometry(
                sailing, route, widthPx, heightPx, 0f, globeWidthPx, mapHeightFraction,
            )
        }
        // Keyed on the sailing only, NOT the size: across a relayout the old
        // raster keeps drawing (scaled) until the resized one is produced —
        // for the idle globe both show the identical view, so the swap is
        // invisible, whereas blanking it flashed on every tab switch.
        val sphere = remember(sailing?.id) {
            mutableStateOf<SphereFrame?>(null)
        }
        // earthTex is a key: on wasm imageResource resolves ASYNCHRONOUSLY,
        // and without it the producer keeps sampling the 1x1 placeholder
        // forever (the texture only appeared after a resize restarted the
        // effect via widthPx).
        LaunchedEffect(sailing?.id, widthPx, cropPx, earthTex, shaderRenderer) {
            if (shaderRenderer != null) return@LaunchedEffect
            if (earthTex == null) return@LaunchedEffect   // shader path or placeholder
            withContext(Dispatchers.Default) {
                // Overscan past the left canvas edge: the draw phase slides the
                // raster rightward to carry motion between keyframes, and
                // without spare texture there the shift would expose a
                // flickering unsampled sliver. GLOBE_OVERSCAN_PX of margin
                // covers shifts from keyframe delays of several frames.
                // Half-res on phones; wider canvases (web/tablet full-bleed)
                // cap the raster so per-keyframe sampling cost stays flat —
                // beyond ~800 columns the 2048px source texture is the
                // resolution limit anyway, the blit upscales the rest.
                val scale = maxOf(2f, widthPx / 800f)
                val outW = ((widthPx + GLOBE_OVERSCAN_PX) / scale).toInt().coerceAtLeast(1)
                val outH = (cropPx / scale).toInt().coerceAtLeast(1)
                val geo = geometry
                val warp = buildGlobeWarp(
                    geo.view,
                    outW,
                    outH,
                    scale,
                    earthTex.width,
                    earthTex.height,
                    xStartPx = -GLOBE_OVERSCAN_PX,
                )
                // Each platform's fastest pixel path (Android: int setPixels;
                // Skiko: direct BGRA bytes). produce() returns a fresh bitmap
                // per keyframe — see SphereSurface.
                val surface = createSphereSurface(outW, outH)
                val pxPerDeg = geo.view.radius * (PI.toFloat() / 180f) * warp.dxFactor

                if (sailing != null || spinDeg == null) {
                    // Route-framed view: static, sample once.
                    sphere.value = SphereFrame(
                        surface.produce(
                            warp, earthTex.pixels, earthTex.width, geo.view.centerLonDeg,
                        ),
                        0f,
                        pxPerDeg,
                    )
                    return@withContext
                }

                var lastSpin = Float.NaN
                while (isActive) {
                    val spinNow = spinDeg.value
                    if (spinNow == lastSpin) {
                        // Hidden tab: rendering is paused, so the spin clock is
                        // frozen — don't burn CPU resampling identical frames.
                        delay(50.milliseconds)
                        continue
                    }
                    lastSpin = spinNow
                    val bmp = surface.produce(
                        warp,
                        earthTex.pixels,
                        earthTex.width,
                        geo.view.centerLonDeg - spinNow,
                    )
                    sphere.value = SphereFrame(bmp, spinNow, pxPerDeg)
                    // 30 Hz keyframes; the draw-phase slide carries motion at
                    // display rate between them. Direct-BGRA sampling keeps
                    // this affordable even on wasm, where Dispatchers.Default
                    // IS the UI thread.
                    delay(33.milliseconds)
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(cropDp)) {
            drawRect(PortsideColors.Sea)
            val geo = geometry
            if (shaderRenderer != null) {
                val lon = geo.view.centerLonDeg - (spinDeg?.value ?: 0f)
                val brush = shaderRenderer.brush(
                    geo.view.centerX,
                    geo.view.centerY,
                    geo.view.radius,
                    geo.view.centerLatDeg,
                    lon,
                )
                drawGlobe(geo, shipPainter, sphereBrush = brush)
                return@Canvas
            }
            val frame = sphere.value
            if (frame != null) {
                // Westward drift moves surface features rightward on screen at
                // radius · dθ (equator rate); shift the keyframe by however far
                // the spin has advanced since it was sampled.
                var shift = 0f
                if (spinDeg != null) {
                    var d = spinDeg.value - frame.spinDeg
                    if (d < -180f) d += 360f
                    // Slide at the band's measured mean rate (see dxFactor) and
                    // never past the sampled overscan margin, even if the
                    // producer stalls for a while.
                    shift = (d * frame.pxPerDeg).coerceIn(0f, GLOBE_OVERSCAN_PX)
                }
                drawGlobe(geo, shipPainter, frame.bitmap, shift)
            }
        }

        // Port chips, projected with the same (static, route-framed) view.
        if (sailing != null) {
            val chipView = remember(sailing.id, widthPx, heightPx) {
                globeViewFor(
                    sailing,
                    widthPx,
                    heightPx,
                    globeWidth = globeWidthPx,
                    sheetTopFraction = mapHeightFraction,
                )
            }
            val (ox, oy, ov) = chipView.project(sailing.origin.longitude, sailing.origin.latitude)
            val (dx, dy, dv) = chipView.project(sailing.destination.longitude, sailing.destination.latitude)
            // Short crossings can still project the ports within a chip's
            // footprint of each other — hang the origin chip above its point
            // when the two would collide.
            val closePx = with(density) { 60.dp.toPx() }
            val collide = ov && dv &&
                (ox - dx) * (ox - dx) + (oy - dy) * (oy - dy) < closePx * closePx
            if (ov) {
                PortChip(
                    code = sailing.origin.code,
                    modifier = Modifier.offset(
                        x = with(density) { ox.toDp() } - 18.dp,
                        y = with(density) { oy.toDp() } + if (collide) (-28).dp else 9.dp,
                    ),
                )
            }
            if (dv) {
                PortChip(
                    code = sailing.destination.code,
                    modifier = Modifier.offset(
                        x = with(density) { dx.toDp() } - 18.dp,
                        y = with(density) { dy.toDp() } + 9.dp,
                    ),
                )
            }
        }
    }
}

private fun DrawScope.drawGlobe(
    geo: GlobeGeometry,
    shipPainter: VectorPainter,
    sphere: ImageBitmap? = null,
    sphereShiftPx: Float = 0f,
    sphereBrush: Brush? = null,
) {
    val cx = geo.view.centerX
    val cy = geo.view.centerY
    val r = geo.view.radius

    drawCircle(PortsideColors.Horizon.copy(alpha = 0.50f), r + 4f, Offset(cx, cy), style = Stroke(9f))
    drawCircle(PortsideColors.Horizon.copy(alpha = 0.18f), r + 16f, Offset(cx, cy), style = Stroke(26f))
    drawCircle(PortsideColors.Horizon.copy(alpha = 0.06f), r + 38f, Offset(cx, cy), style = Stroke(50f))

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFF2A4A6E),
                0.55f to Color(0xFF16283E),
                1.0f to Color(0xFF0A1220),
            ),
            center = Offset(cx, cy - r * 0.85f),
            radius = r * 2.1f,
        ),
        radius = r,
        center = Offset(cx, cy),
    )

    if (sphereBrush != null) {
        // GPU: the runtime shader evaluates the warp per pixel; the limb
        // shading matches the CPU path's.
        clipPath(geo.disk) {
            drawPath(geo.disk, sphereBrush)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1.0f to Color(0xB308101C),
                    ),
                    center = Offset(cx, cy - r * 0.55f),
                    radius = r * 1.65f,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }
    } else if (sphere != null) {
        // Real earth imagery, warped onto the sphere. The disk clip keeps the
        // circular edge crisp despite the half-resolution upscale, and a limb
        // shading pass restores the sphere's depth over the flat texture.
        clipPath(geo.disk) {
            // Float translate (not dstOffset) so the shift stays sub-pixel.
            // The raster starts GLOBE_OVERSCAN_PX left of the canvas.
            translate(left = sphereShiftPx - GLOBE_OVERSCAN_PX) {
                drawImage(
                    image = sphere,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(sphere.width, sphere.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(
                        (size.width + GLOBE_OVERSCAN_PX).toInt(),
                        size.height.toInt(),
                    ),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1.0f to Color(0xB308101C),
                    ),
                    center = Offset(cx, cy - r * 0.55f),
                    radius = r * 1.65f,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }
    } else {
        clipPath(geo.disk) {
            drawPath(geo.graticule, Color(0xFF3A4E6E).copy(alpha = 0.35f), style = Stroke(1f))
            drawPath(geo.land, Color(0xFF243B58))
            drawPath(geo.land, Color(0xFF4E6C96).copy(alpha = 0.85f), style = Stroke(1.2f))
            for (light in geo.cityLights) {
                drawCircle(
                    PortsideColors.Shimmer.copy(alpha = light.alpha),
                    light.r,
                    Offset(light.x, light.y),
                )
            }
        }
    }

    geo.routePath?.let { arc ->
        drawPath(arc, PortsideColors.Route.copy(alpha = 0.30f), style = Stroke(10f))
        drawPath(
            arc,
            PortsideColors.Route,
            style = Stroke(3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 12f))),
        )
    }
    geo.routeStart?.let { drawCircle(Color.White, 6f, it) }
    geo.routeEnd?.let { drawCircle(Color.White, 6f, it) }
    geo.ship?.let { center ->
        // The hull glyph is a side view: it stays upright rather than
        // rotating with the route heading.
        run {
            val s = 30f
            val outline = ColorFilter.tint(Color(0xCC10201E))
            // Four offset passes of a dark tint form a light outline that keeps
            // the white glyph readable over bright parts of the globe.
            for ((ox, oy) in listOf(-1.5f to 0f, 1.5f to 0f, 0f to -1.5f, 0f to 1.5f)) {
                translate(left = center.x - s / 2 + ox, top = center.y - s / 2 + oy) {
                    with(shipPainter) { draw(Size(s, s), colorFilter = outline) }
                }
            }
            translate(left = center.x - s / 2, top = center.y - s / 2) {
                with(shipPainter) {
                    draw(Size(s, s), colorFilter = ColorFilter.tint(Color.White))
                }
            }
        }
    }
}

@Composable
private fun PortChip(code: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Text(
            text = code,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(Color(0xFF39496B), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}
