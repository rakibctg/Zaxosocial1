package com.zaxo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun ZaxoQRCode(
    identifier: String,
    modifier: Modifier = Modifier,
    qrColor: Color = Color.Black
) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val gridSize = 17 // 17x17 grid
        val cellSize = sizePx / gridSize

        // 1. Draw Background
        drawRect(color = Color.White)

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                // Check if we are inside a finder pattern
                val isTopLeftFinder = r < 6 && c < 6
                val isTopRightFinder = r < 6 && c >= gridSize - 6
                val isBottomLeftFinder = r >= gridSize - 6 && c < 6

                if (isTopLeftFinder || isTopRightFinder || isBottomLeftFinder) {
                    // Local coordinate inside the 6x6 finder pattern
                    val lr = if (isTopLeftFinder) r else if (isTopRightFinder) r else r - (gridSize - 6)
                    val lc = if (isTopLeftFinder) c else if (isTopRightFinder) c - (gridSize - 6) else c

                    // Finder pattern rule: outer ring is black, middle ring is white, inner is black
                    val isBlack = when {
                        lr == 0 || lr == 5 || lc == 0 || lc == 5 -> true // outer 6x6
                        lr == 1 || lr == 4 || lc == 1 || lc == 4 -> false // white ring
                        else -> true // inner 2x2
                    }
                    if (isBlack) {
                        drawRect(
                            color = qrColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                } else {
                    // Timing patterns (dashed lines connecting finders)
                    if (r == 4 && c % 2 == 0) {
                        drawRect(
                            color = qrColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    } else if (c == 4 && r % 2 == 0) {
                        drawRect(
                            color = qrColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    } else {
                        // Deterministic data module based on the hash of identifier and coordinates
                        val seed = identifier.hashCode() + r * 31 + c * 17
                        val hash = (seed xor (seed ushr 16)) * 0x45d9f3b
                        val isBlack = (hash % 100) < 45 // ~45% density
                        if (isBlack) {
                            drawRect(
                                color = qrColor,
                                topLeft = Offset(c * cellSize, r * cellSize),
                                size = Size(cellSize + 0.5f, cellSize + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
