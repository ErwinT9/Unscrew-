package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.*
import kotlin.math.*

@Composable
fun BoardCanvas(
    levelData: LevelData,
    planks: List<Plank>,
    holes: List<Hole>,
    screws: List<Screw>,
    activeFlights: List<com.example.viewmodel.ActiveFlight>,
    onScrewTapped: (Screw) -> Unit,
    modifier: Modifier = Modifier
) {
    // Virtual coordinate system: 400 width x 550 height
    val virtualWidth = 400f
    val virtualHeight = 550f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val availableWidth = constraints.maxWidth.toFloat()
        val availableHeight = constraints.maxHeight.toFloat()

        val scale = min(availableWidth / virtualWidth, availableHeight / virtualHeight)
        val offsetX = (availableWidth - virtualWidth * scale) / 2f
        val offsetY = (availableHeight - virtualHeight * scale) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(screws, holes, scale, offsetX, offsetY) {
                    detectTapGestures { tapOffset ->
                        val virtualX = (tapOffset.x - offsetX) / scale
                        val virtualY = (tapOffset.y - offsetY) / scale

                        // Find closest tapped screw
                        val clickedScrew = screws
                            .filter { !it.isCleared && !it.isFlying }
                            .minByOrNull {
                                hypot((it.x - virtualX).toDouble(), (it.y - virtualY).toDouble()).toFloat()
                            }

                        if (clickedScrew != null) {
                            val dist = hypot((clickedScrew.x - virtualX).toDouble(), (clickedScrew.y - virtualY).toDouble())
                            if (dist <= 36.0) { // Generous 36dp touch target in virtual space
                                onScrewTapped(clickedScrew)
                            }
                        }
                    }
                }
        ) {
            // Transform canvas to match virtual coordinates
            withTransform({
                scale(scale, scale, pivot = Offset.Zero)
                translate(offsetX / scale, offsetY / scale)
            }) {
                // 1. Draw Themed Board Canvas Background
                drawBoardBackground(levelData.biome, virtualWidth, virtualHeight)

                // 2. Draw Top Free Holes Shelf
                drawFreeHolesShelf(holes.filter { it.isReservedSpare }, levelData.biome)

                // 3. Draw Regular Plank Holes (Base Layer)
                drawBaseHoles(holes.filter { !it.isReservedSpare })

                // 4. Draw Planks sorted by Z-Index
                val sortedPlanks = planks.filter { !it.isCleared }.sortedBy { it.zIndex }
                sortedPlanks.forEach { plank ->
                    drawPlankItem(plank, levelData.biome)
                }

                // 5. Draw Screws on Board
                screws.filter { !it.isCleared && !it.isFlying }.forEach { screw ->
                    drawScrewHead(screw.x, screw.y, screw.color, isUnscrewing = screw.isUnscrewing)
                }

                // 6. Draw Active Flight Screws
                activeFlights.forEach { flight ->
                    val curX = flight.startX + (flight.targetX - flight.startX) * flight.progress
                    val curY = flight.startY + (flight.targetY - flight.startY) * flight.progress
                    val spinAngle = flight.progress * 720f
                    rotate(spinAngle, pivot = Offset(curX, curY)) {
                        drawScrewHead(curX, curY, flight.color, isUnscrewing = true, scaleMult = 1.15f)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBoardBackground(biome: BiomeTheme, width: Float, height: Float) {
    // Outer rounded board frame
    val boardRect = androidx.compose.ui.geometry.Rect(8f, 10f, width - 8f, height - 10f)
    
    // Gradient fill
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(biome.bgGradientStart),
                Color(biome.boardBgColor),
                Color(biome.bgGradientEnd)
            )
        ),
        topLeft = Offset(8f, 10f),
        size = Size(width - 16f, height - 20f),
        cornerRadius = CornerRadius(24f, 24f)
    )

    // Decorative inner border
    drawRoundRect(
        color = Color(biome.boardBorderColor).copy(alpha = 0.45f),
        topLeft = Offset(14f, 16f),
        size = Size(width - 28f, height - 32f),
        cornerRadius = CornerRadius(18f, 18f),
        style = Stroke(width = 2.5f)
    )

    // Subtle grid/wood pattern lines
    val patternColor = Color.White.copy(alpha = 0.04f)
    for (i in 1..8) {
        val y = i * (height / 9f)
        drawLine(
            color = patternColor,
            start = Offset(20f, y),
            end = Offset(width - 20f, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawFreeHolesShelf(freeHoles: List<Hole>, biome: BiomeTheme) {
    if (freeHoles.isEmpty()) return

    // Shelf container bar
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(25f, 15f),
        size = Size(350f, 40f),
        cornerRadius = CornerRadius(20f, 20f)
    )
    drawRoundRect(
        color = Color(biome.accentColor).copy(alpha = 0.3f),
        topLeft = Offset(25f, 15f),
        size = Size(350f, 40f),
        cornerRadius = CornerRadius(20f, 20f),
        style = Stroke(width = 1.5f)
    )

    // Draw each parking hole slot
    freeHoles.forEach { hole ->
        // Hole inner shadow
        drawCircle(
            color = Color(0xFF100804),
            radius = 15f,
            center = Offset(hole.x, hole.y)
        )
        // Hole metallic rim
        drawCircle(
            color = Color(biome.accentColor),
            radius = 16f,
            center = Offset(hole.x, hole.y),
            style = Stroke(width = 2f)
        )
        // Empty slot center dot indicator
        if (hole.occupiedScrewId == null) {
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = 4f,
                center = Offset(hole.x, hole.y)
            )
        }
    }
}

private fun DrawScope.drawBaseHoles(holes: List<Hole>) {
    holes.forEach { hole ->
        // Recessed base hole
        drawCircle(
            color = Color(0xFF1A0F08),
            radius = 13f,
            center = Offset(hole.x, hole.y)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = 10f,
            center = Offset(hole.x, hole.y)
        )
    }
}

private fun DrawScope.drawPlankItem(plank: Plank, biome: BiomeTheme) {
    val alpha = plank.fallAlpha
    val offsetY = plank.fallOffsetY
    val rotation = plank.fallRotation

    val plankColor = Color(plank.colorHex).copy(alpha = alpha)
    val darkEdgeColor = Color.Black.copy(alpha = 0.35f * alpha)
    val highlightColor = Color.White.copy(alpha = 0.25f * alpha)

    when (plank.shape) {
        PlankShape.ROUNDED_BAR, PlankShape.RECTANGLE, PlankShape.DIAGONAL_BEAM -> {
            if (plank.points.size < 2) return
            val p1 = plank.points[0]
            val p2 = plank.points[1]

            val midX = (p1.x + p2.x) / 2f
            val midY = (p1.y + p2.y) / 2f + offsetY

            rotate(rotation, pivot = Offset(midX, midY)) {
                val start = Offset(p1.x, p1.y + offsetY)
                val end = Offset(p2.x, p2.y + offsetY)

                // 1. Drop shadow
                drawLine(
                    color = Color.Black.copy(alpha = 0.3f * alpha),
                    start = start + Offset(0f, 6f),
                    end = end + Offset(0f, 6f),
                    strokeWidth = plank.width + 4f,
                    cap = StrokeCap.Round
                )

                // 2. Main plank body
                drawLine(
                    color = plankColor,
                    start = start,
                    end = end,
                    strokeWidth = plank.width,
                    cap = StrokeCap.Round
                )

                // 3. Bevel border
                drawLine(
                    color = darkEdgeColor,
                    start = start,
                    end = end,
                    strokeWidth = plank.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = plankColor,
                    start = start,
                    end = end,
                    strokeWidth = plank.width - 4f,
                    cap = StrokeCap.Round
                )

                // 4. Subtle top highlight
                drawLine(
                    color = highlightColor,
                    start = start + Offset(0f, -plank.width * 0.2f),
                    end = end + Offset(0f, -plank.width * 0.2f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                // 5. Draw holes along plank
                plank.points.forEach { pt ->
                    drawCircle(
                        color = Color(0xFF1B0F07).copy(alpha = alpha),
                        radius = 12f,
                        center = Offset(pt.x, pt.y + offsetY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f * alpha),
                        radius = 13f,
                        center = Offset(pt.x, pt.y + offsetY),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }
        PlankShape.CIRCULAR_RING, PlankShape.GEAR_DISC -> {
            val center = plank.points.firstOrNull() ?: return
            val cy = center.y + offsetY

            rotate(rotation, pivot = Offset(center.x, cy)) {
                // Drop shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f * alpha),
                    radius = plank.radius,
                    center = Offset(center.x, cy + 5f),
                    style = Stroke(width = plank.width + 2f)
                )
                // Ring body
                drawCircle(
                    color = plankColor,
                    radius = plank.radius,
                    center = Offset(center.x, cy),
                    style = Stroke(width = plank.width)
                )
                // Outer highlight rim
                drawCircle(
                    color = highlightColor,
                    radius = plank.radius + plank.width / 2f - 1.5f,
                    center = Offset(center.x, cy),
                    style = Stroke(width = 1.5f)
                )
                // Inner dark rim
                drawCircle(
                    color = darkEdgeColor,
                    radius = plank.radius - plank.width / 2f + 1.5f,
                    center = Offset(center.x, cy),
                    style = Stroke(width = 1.5f)
                )
            }
        }
        else -> {
            // Generic polygon/contour fallback
            val center = plank.points.firstOrNull() ?: return
            val cy = center.y + offsetY
            drawCircle(
                color = plankColor,
                radius = plank.radius,
                center = Offset(center.x, cy)
            )
        }
    }
}

private fun DrawScope.drawScrewHead(
    x: Float,
    y: Float,
    color: ScrewColor,
    isUnscrewing: Boolean = false,
    scaleMult: Float = 1.0f
) {
    val radius = 17f * scaleMult
    val center = Offset(x, y)

    // 1. Drop shadow beneath screw
    drawCircle(
        color = Color.Black.copy(alpha = 0.45f),
        radius = radius + 2f,
        center = center + Offset(0f, 3f)
    )

    // 2. Metallic Outer Bevel Ring
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(color.lightColor), Color(color.primaryColor), Color(color.darkColor)),
            center = center - Offset(3f, 3f),
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // 3. Inner Screw Cap
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(color.lightColor), Color(color.primaryColor)),
            start = center - Offset(radius * 0.7f, radius * 0.7f),
            end = center + Offset(radius * 0.7f, radius * 0.7f)
        ),
        radius = radius * 0.78f,
        center = center
    )

    // 4. Screw Slot (Phillips Cross / Star pattern)
    val slotColor = Color(color.darkColor).copy(alpha = 0.95f)
    val slotW = 3.5f * scaleMult
    val slotLen = radius * 0.55f

    // Horizontal slot
    drawLine(
        color = slotColor,
        start = center - Offset(slotLen, 0f),
        end = center + Offset(slotLen, 0f),
        strokeWidth = slotW,
        cap = StrokeCap.Round
    )
    // Vertical slot
    drawLine(
        color = slotColor,
        start = center - Offset(0f, slotLen),
        end = center + Offset(0f, slotLen),
        strokeWidth = slotW,
        cap = StrokeCap.Round
    )

    // 5. Specular highlight sparkle
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = 2.5f * scaleMult,
        center = center - Offset(radius * 0.35f, radius * 0.35f)
    )
}
