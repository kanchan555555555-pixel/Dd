package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.ArcadeGameState
import com.example.model.GameType
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.VoltLime
import kotlin.math.min

@Composable
fun ArcadeGameBoardCanvas(
    state: ArcadeGameState,
    modifier: Modifier = Modifier,
    onTapCell: ((Int) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianBg)
            .border(1.5.dp, ElevatedBorder, RoundedCornerShape(20.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onTapCell != null && state.activeGame == GameType.TICTACTOE) {
                        Modifier.pointerInput(state.activeGame) {
                            detectTapGestures { offset ->
                                val boardSize = min(size.width.toFloat(), size.height.toFloat()) * 0.78f
                                val bx0 = (size.width - boardSize) / 2f
                                val by0 = (size.height - boardSize) / 2f
                                val relX = offset.x - bx0
                                val relY = offset.y - by0
                                if (relX in 0f..boardSize && relY in 0f..boardSize) {
                                    val col = (relX / (boardSize / 3f)).toInt().coerceIn(0, 2)
                                    val row = (relY / (boardSize / 3f)).toInt().coerceIn(0, 2)
                                    onTapCell(row * 3 + col)
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            drawNeonGrid()
            when (state.activeGame) {
                GameType.PONG -> drawPong(state)
                GameType.SNAKE -> drawSnake(state)
                GameType.BRICKS -> drawBricks(state)
                GameType.TICTACTOE -> drawTicTacToe(state)
            }
        }
    }
}

private fun DrawScope.drawNeonGrid() {
    drawRect(color = MidnightSurface.copy(alpha = 0.35f))
    val step = 44.dp.toPx()
    var x = step
    while (x < size.width) {
        drawLine(
            color = ElectricCyan.copy(alpha = 0.06f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f,
        )
        x += step
    }
    var y = step
    while (y < size.height) {
        drawLine(
            color = ElectricCyan.copy(alpha = 0.06f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
        )
        y += step
    }
}

private fun DrawScope.drawPong(state: ArcadeGameState) {
    val p = state.pong
    val w = size.width
    val h = size.height

    // Center dashed net
    drawLine(
        color = Color.White.copy(alpha = 0.22f),
        start = Offset(w / 2f, 16f),
        end = Offset(w / 2f, h - 16f),
        strokeWidth = 3.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 16f)),
    )

    val paddleW = (w * 0.022f).coerceAtLeast(10.dp.toPx())
    val paddleH = h * 0.22f

    // P1 Paddle (Cyan)
    val p1Top = (p.p1Y * h - paddleH / 2f).coerceIn(8f, h - paddleH - 8f)
    drawRoundRect(
        color = ElectricCyan.copy(alpha = 0.28f),
        topLeft = Offset(w * 0.04f - 4f, p1Top - 4f),
        size = Size(paddleW + 8f, paddleH + 8f),
        cornerRadius = CornerRadius(12f, 12f),
    )
    drawRoundRect(
        color = ElectricCyan,
        topLeft = Offset(w * 0.04f, p1Top),
        size = Size(paddleW, paddleH),
        cornerRadius = CornerRadius(8f, 8f),
    )

    // P2 Paddle (Magenta)
    val p2Top = (p.p2Y * h - paddleH / 2f).coerceIn(8f, h - paddleH - 8f)
    drawRoundRect(
        color = ArcadeMagenta.copy(alpha = 0.28f),
        topLeft = Offset(w * 0.96f - paddleW - 4f, p2Top - 4f),
        size = Size(paddleW + 8f, paddleH + 8f),
        cornerRadius = CornerRadius(12f, 12f),
    )
    drawRoundRect(
        color = ArcadeMagenta,
        topLeft = Offset(w * 0.96f - paddleW, p2Top),
        size = Size(paddleW, paddleH),
        cornerRadius = CornerRadius(8f, 8f),
    )

    // Ball
    val ballCenter = Offset(p.ballX * w, p.ballY * h)
    val r = min(w, h) * 0.025f
    drawCircle(color = VoltLime.copy(alpha = 0.28f), radius = r * 1.9f, center = ballCenter)
    drawCircle(color = VoltLime, radius = r, center = ballCenter)
}

private fun DrawScope.drawSnake(state: ArcadeGameState) {
    val sn = state.snake
    val cellW = size.width / sn.cols
    val cellH = size.height / sn.rows

    // Food
    val foodCenter = Offset((sn.food.x + 0.5f) * cellW, (sn.food.y + 0.5f) * cellH)
    val foodR = min(cellW, cellH) * 0.36f
    drawCircle(color = ArcadeMagenta.copy(alpha = 0.3f), radius = foodR * 1.8f, center = foodCenter)
    drawCircle(color = ArcadeMagenta, radius = foodR, center = foodCenter)

    // Bonus orb
    sn.bonusFood?.let { bf ->
        val bCenter = Offset((bf.x + 0.5f) * cellW, (bf.y + 0.5f) * cellH)
        drawCircle(color = SolarAmber.copy(alpha = 0.3f), radius = foodR * 2f, center = bCenter)
        drawCircle(color = SolarAmber, radius = foodR * 1.15f, center = bCenter)
    }

    // Snake segments
    sn.snake.forEachIndexed { idx, pt ->
        val color = if (idx == 0) ElectricCyan else VoltLime
        val pad = 2.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(pt.x * cellW + pad, pt.y * cellH + pad),
            size = Size((cellW - pad * 2).coerceAtLeast(4f), (cellH - pad * 2).coerceAtLeast(4f)),
            cornerRadius = CornerRadius(6f, 6f),
        )
    }
}

private fun DrawScope.drawBricks(state: ArcadeGameState) {
    val b = state.bricks
    val w = size.width
    val h = size.height
    val palette = listOf(ArcadeMagenta, SolarAmber, ElectricCyan, VoltLime, Color(0xFFA855F7))

    val leftM = w * 0.04f
    val topM = h * 0.10f
    val cellW = (w * 0.92f) / b.cols
    val cellH = (h * 0.32f) / b.rows
    val gap = 3.dp.toPx()

    b.bricks.forEach { brick ->
        val color = palette[brick.colorIndex % palette.size]
        val tl = Offset(leftM + brick.col * cellW + gap, topM + brick.row * cellH + gap)
        val sz = Size((cellW - gap * 2).coerceAtLeast(6f), (cellH - gap * 2).coerceAtLeast(6f))
        drawRoundRect(color = color, topLeft = tl, size = sz, cornerRadius = CornerRadius(6f, 6f))
        if (brick.hp > 1) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = tl,
                size = sz,
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }

    // Paddle
    val pw = w * 0.21f
    val ph = (h * 0.032f).coerceAtLeast(10.dp.toPx())
    val px = (b.paddleX * w - pw / 2f).coerceIn(8f, w - pw - 8f)
    drawRoundRect(
        color = ElectricCyan,
        topLeft = Offset(px, h * 0.89f),
        size = Size(pw, ph),
        cornerRadius = CornerRadius(10f, 10f),
    )

    // Ball
    val ballCenter = Offset(b.ballX * w, b.ballY * h)
    val br = min(w, h) * 0.023f
    drawCircle(color = VoltLime.copy(alpha = 0.3f), radius = br * 1.8f, center = ballCenter)
    drawCircle(color = VoltLime, radius = br, center = ballCenter)
}

private fun DrawScope.drawTicTacToe(state: ArcadeGameState) {
    val t = state.ticTacToe
    val boardSize = min(size.width, size.height) * 0.78f
    val bx0 = (size.width - boardSize) / 2f
    val by0 = (size.height - boardSize) / 2f
    val cell = boardSize / 3f

    for (i in 1..2) {
        drawLine(
            color = ElevatedBorder,
            start = Offset(bx0 + i * cell, by0),
            end = Offset(bx0 + i * cell, by0 + boardSize),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ElevatedBorder,
            start = Offset(bx0, by0 + i * cell),
            end = Offset(bx0 + boardSize, by0 + i * cell),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    t.cells.forEachIndexed { idx, value ->
        val r = idx / 3
        val c = idx % 3
        val cx = bx0 + (c + 0.5f) * cell
        val cy = by0 + (r + 0.5f) * cell
        val pad = cell * 0.26f
        val inWinLine = t.winLine.contains(idx)

        if (value == 1) {
            val color = if (inWinLine) VoltLime else ElectricCyan
            drawLine(color, Offset(cx - pad, cy - pad), Offset(cx + pad, cy + pad), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color, Offset(cx + pad, cy - pad), Offset(cx - pad, cy + pad), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        } else if (value == 2) {
            val color = if (inWinLine) VoltLime else ArcadeMagenta
            drawCircle(
                color = color,
                radius = pad,
                center = Offset(cx, cy),
                style = Stroke(width = 8.dp.toPx()),
            )
        }
    }
}
