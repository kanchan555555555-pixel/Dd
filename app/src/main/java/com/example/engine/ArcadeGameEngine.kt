package com.example.engine

import com.example.model.ArcadeGameState
import com.example.model.BrickItem
import com.example.model.BricksState
import com.example.model.GameType
import com.example.model.GridPoint
import com.example.model.InputAction
import com.example.model.PongState
import com.example.model.SnakeState
import com.example.model.TicTacToeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

class ArcadeGameEngine(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(
        ArcadeGameState(
            bricks = initialBricksState(level = 1)
        )
    )
    val state: StateFlow<ArcadeGameState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var snakeAccumulatorMs = 0L
    private var pendingSnakeDir: Pair<Int, Int>? = null
    private var snakeBoostActive = false

    // Continuous held direction flags per player slot (1 or 2)
    @Volatile private var p1UpHeld = false
    @Volatile private var p1DownHeld = false
    @Volatile private var p1LeftHeld = false
    @Volatile private var p1RightHeld = false
    @Volatile private var p2UpHeld = false
    @Volatile private var p2DownHeld = false

    fun startLoop() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch(Dispatchers.Default) {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val dt = (now - lastTime).coerceIn(8L, 48L)
                lastTime = now
                tick(dt)
                delay(16L) // ~60fps
            }
        }
    }

    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    fun setTwoPlayersConnected(connected: Boolean) {
        _state.update { s ->
            s.copy(
                twoPlayersConnected = connected,
                pong = s.pong.copy(twoPlayerMode = connected),
                ticTacToe = s.ticTacToe.copy(vsAi = !connected),
            )
        }
    }

    fun selectGame(game: GameType, autoStart: Boolean = false) {
        snakeAccumulatorMs = 0L
        pendingSnakeDir = null
        snakeBoostActive = false
        clearHeldButtons()
        _state.update { current ->
            when (game) {
                GameType.PONG -> current.copy(
                    activeGame = game,
                    isPaused = !autoStart,
                    pong = PongState(twoPlayerMode = current.twoPlayersConnected),
                )
                GameType.SNAKE -> current.copy(
                    activeGame = game,
                    isPaused = !autoStart,
                    snake = freshSnakeState(),
                )
                GameType.BRICKS -> current.copy(
                    activeGame = game,
                    isPaused = !autoStart,
                    bricks = initialBricksState(level = 1),
                )
                GameType.TICTACTOE -> current.copy(
                    activeGame = game,
                    isPaused = false,
                    ticTacToe = TicTacToeState(
                        vsAi = !current.twoPlayersConnected,
                        xWins = current.ticTacToe.xWins,
                        oWins = current.ticTacToe.oWins,
                    ),
                )
            }
        }
    }

    fun handleInput(
        playerSlot: Int,
        action: InputAction,
        value: Float = 0f,
        pressed: Boolean = true,
    ) {
        if (action == InputAction.START_PAUSE && pressed) {
            togglePauseOrRestartIfEnded()
            return
        }
        if (action == InputAction.RESET && pressed) {
            resetCurrentGame(autoStart = true)
            return
        }

        val active = _state.value.activeGame
        when (active) {
            GameType.PONG -> handlePongInput(playerSlot, action, value, pressed)
            GameType.SNAKE -> handleSnakeInput(action, pressed)
            GameType.BRICKS -> handleBricksInput(action, value, pressed)
            GameType.TICTACTOE -> if (pressed) handleTicTacToeInput(playerSlot, action, value.toInt())
        }
    }

    private fun togglePauseOrRestartIfEnded() {
        _state.update { s ->
            when (s.activeGame) {
                GameType.PONG -> {
                    if (s.pong.winner != 0) {
                        s.copy(isPaused = false, pong = PongState(twoPlayerMode = s.twoPlayersConnected))
                    } else {
                        s.copy(isPaused = !s.isPaused)
                    }
                }
                GameType.SNAKE -> {
                    if (s.snake.gameOver) {
                        pendingSnakeDir = null
                        snakeAccumulatorMs = 0L
                        s.copy(isPaused = false, snake = freshSnakeState())
                    } else {
                        s.copy(isPaused = !s.isPaused)
                    }
                }
                GameType.BRICKS -> {
                    if (s.bricks.gameOver) {
                        s.copy(isPaused = false, bricks = initialBricksState(level = 1))
                    } else if (s.bricks.ballAttached && !s.isPaused) {
                        s.copy(bricks = s.bricks.copy(ballAttached = false))
                    } else {
                        s.copy(isPaused = !s.isPaused)
                    }
                }
                GameType.TICTACTOE -> {
                    s.copy(
                        isPaused = false,
                        ticTacToe = TicTacToeState(
                            vsAi = !s.twoPlayersConnected,
                            xWins = s.ticTacToe.xWins,
                            oWins = s.ticTacToe.oWins,
                        ),
                    )
                }
            }
        }
    }

    fun resetCurrentGame(autoStart: Boolean = true) {
        selectGame(_state.value.activeGame, autoStart = autoStart)
    }

    private fun handlePongInput(playerSlot: Int, action: InputAction, value: Float, pressed: Boolean) {
        when (action) {
            InputAction.UP -> if (playerSlot == 2) p2UpHeld = pressed else p1UpHeld = pressed
            InputAction.DOWN -> if (playerSlot == 2) p2DownHeld = pressed else p1DownHeld = pressed
            InputAction.PADDLE_POS -> {
                val clamped = value.coerceIn(0.10f, 0.90f)
                _state.update { s ->
                    val updatedPong = if (playerSlot == 2) {
                        s.pong.copy(p2Y = clamped)
                    } else {
                        s.pong.copy(p1Y = clamped)
                    }
                    s.copy(pong = updatedPong)
                }
            }
            InputAction.ACTION_A -> if (pressed) {
                _state.update { s ->
                    if (s.pong.winner != 0) {
                        s.copy(isPaused = false, pong = PongState(twoPlayerMode = s.twoPlayersConnected))
                    } else if (s.isPaused) {
                        s.copy(isPaused = false)
                    } else s
                }
            }
            else -> Unit
        }
    }

    private fun handleSnakeInput(action: InputAction, pressed: Boolean) {
        if (action == InputAction.ACTION_A) {
            snakeBoostActive = pressed
            if (pressed && (_state.value.isPaused || _state.value.snake.gameOver)) {
                togglePauseOrRestartIfEnded()
            }
            return
        }
        if (!pressed) return

        val desired = when (action) {
            InputAction.UP -> 0 to -1
            InputAction.DOWN -> 0 to 1
            InputAction.LEFT -> -1 to 0
            InputAction.RIGHT -> 1 to 0
            else -> null
        } ?: return

        val snake = _state.value.snake
        // Prevent 180-degree reversal into self
        if (desired.first + snake.dirX != 0 || desired.second + snake.dirY != 0) {
            pendingSnakeDir = desired
            if (_state.value.isPaused && !snake.gameOver) {
                _state.update { it.copy(isPaused = false) }
            }
        }
    }

    private fun handleBricksInput(action: InputAction, value: Float, pressed: Boolean) {
        when (action) {
            InputAction.LEFT -> p1LeftHeld = pressed
            InputAction.RIGHT -> p1RightHeld = pressed
            InputAction.PADDLE_POS -> {
                val clamped = value.coerceIn(0.11f, 0.89f)
                _state.update { s ->
                    val b = s.bricks
                    val bx = if (b.ballAttached) clamped else b.ballX
                    s.copy(bricks = b.copy(paddleX = clamped, ballX = bx))
                }
            }
            InputAction.ACTION_A -> if (pressed) {
                _state.update { s ->
                    if (s.bricks.gameOver) {
                        s.copy(isPaused = false, bricks = initialBricksState(level = 1))
                    } else if (s.isPaused) {
                        s.copy(isPaused = false, bricks = s.bricks.copy(ballAttached = false))
                    } else if (s.bricks.ballAttached) {
                        s.copy(bricks = s.bricks.copy(ballAttached = false))
                    } else s
                }
            }
            else -> Unit
        }
    }

    private fun handleTicTacToeInput(playerSlot: Int, action: InputAction, cellIndex: Int) {
        if (action == InputAction.ACTION_A || action == InputAction.ACTION_B) {
            if (_state.value.ticTacToe.winner != 0) {
                resetCurrentGame(autoStart = true)
            }
            return
        }
        if (action != InputAction.CELL_TAP || cellIndex !in 0..8) return

        _state.update { s ->
            val t = s.ticTacToe
            if (t.winner != 0 || t.cells[cellIndex] != 0) return@update s
            // In 2-player mode, enforce player turn; in 1P vs AI mode, Player 1 is always X
            if (!t.vsAi && playerSlot != t.currentTurn) return@update s
            if (t.vsAi && t.currentTurn != 1) return@update s

            val newCells = t.cells.toMutableList()
            newCells[cellIndex] = t.currentTurn
            val eval = evaluateTicTacToe(newCells)
            if (eval.first != 0) {
                return@update s.copy(
                    eventId = s.eventId + 1,
                    lastEvent = if (eval.first == 3) "hit" else "win",
                    ticTacToe = t.copy(
                        cells = newCells,
                        winner = eval.first,
                        winLine = eval.second,
                        xWins = t.xWins + if (eval.first == 1) 1 else 0,
                        oWins = t.oWins + if (eval.first == 2) 1 else 0,
                    ),
                )
            }

            // If playing vs AI, pick smart AI counter move immediately
            if (t.vsAi) {
                val aiMove = pickSmartTicTacToeMove(newCells)
                if (aiMove != -1) {
                    newCells[aiMove] = 2
                }
                val afterAi = evaluateTicTacToe(newCells)
                return@update s.copy(
                    eventId = s.eventId + 1,
                    lastEvent = if (afterAi.first != 0) "win" else "hit",
                    ticTacToe = t.copy(
                        cells = newCells,
                        currentTurn = 1,
                        winner = afterAi.first,
                        winLine = afterAi.second,
                        xWins = t.xWins + if (afterAi.first == 1) 1 else 0,
                        oWins = t.oWins + if (afterAi.first == 2) 1 else 0,
                    ),
                )
            }

            s.copy(
                eventId = s.eventId + 1,
                lastEvent = "hit",
                ticTacToe = t.copy(
                    cells = newCells,
                    currentTurn = if (t.currentTurn == 1) 2 else 1,
                ),
            )
        }
    }

    private fun tick(dtMs: Long) {
        val current = _state.value
        if (current.isPaused) return

        when (current.activeGame) {
            GameType.PONG -> tickPong(dtMs)
            GameType.SNAKE -> tickSnake(dtMs)
            GameType.BRICKS -> tickBricks(dtMs)
            GameType.TICTACTOE -> Unit
        }
    }

    private fun tickPong(dtMs: Long) {
        val scale = dtMs / 16f
        _state.update { s ->
            val p = s.pong
            if (p.winner != 0) return@update s

            val paddleSpeed = 0.018f * scale
            var p1Y = p.p1Y
            if (p1UpHeld) p1Y -= paddleSpeed
            if (p1DownHeld) p1Y += paddleSpeed
            p1Y = p1Y.coerceIn(0.10f, 0.90f)

            var p2Y = p.p2Y
            if (p.twoPlayerMode) {
                if (p2UpHeld) p2Y -= paddleSpeed
                if (p2DownHeld) p2Y += paddleSpeed
            } else {
                // Smart responsive AI with slight human reaction cap
                val aiSpeed = 0.0115f * scale
                val diff = p.ballY - p2Y
                if (abs(diff) > 0.015f) {
                    p2Y += diff.coerceIn(-aiSpeed, aiSpeed)
                }
            }
            p2Y = p2Y.coerceIn(0.10f, 0.90f)

            var bx = p.ballX + p.ballVx * scale
            var by = p.ballY + p.ballVy * scale
            var vx = p.ballVx
            var vy = p.ballVy
            var eventId = s.eventId
            var lastEvent = s.lastEvent

            // Top / bottom bounce
            if (by <= 0.025f) {
                by = 0.025f
                vy = abs(vy)
                eventId++
                lastEvent = "hit"
            } else if (by >= 0.975f) {
                by = 0.975f
                vy = -abs(vy)
                eventId++
                lastEvent = "hit"
            }

            // Left paddle (P1 at x = 0.055)
            val paddleHalfH = 0.11f
            if (vx < 0f && bx in 0.04f..0.075f && abs(by - p1Y) <= paddleHalfH) {
                bx = 0.076f
                val hitOffset = ((by - p1Y) / paddleHalfH).coerceIn(-1f, 1f)
                val nextSpeed = (abs(vx) * 1.045f).coerceAtMost(0.020f)
                vx = nextSpeed
                vy = hitOffset * 0.013f
                eventId++
                lastEvent = "hit"
            }

            // Right paddle (P2/AI at x = 0.945)
            if (vx > 0f && bx in 0.925f..0.96f && abs(by - p2Y) <= paddleHalfH) {
                bx = 0.924f
                val hitOffset = ((by - p2Y) / paddleHalfH).coerceIn(-1f, 1f)
                val nextSpeed = (abs(vx) * 1.045f).coerceAtMost(0.020f)
                vx = -nextSpeed
                vy = hitOffset * 0.013f
                eventId++
                lastEvent = "hit"
            }

            var s1 = p.score1
            var s2 = p.score2
            var winner = 0

            if (bx < 0f) {
                s2++
                eventId++
                lastEvent = "score"
                if (s2 >= 7) {
                    winner = 2
                    lastEvent = "win"
                }
                bx = 0.5f
                by = 0.5f
                vx = 0.0085f
                vy = if (Random.nextBoolean()) 0.006f else -0.006f
            } else if (bx > 1f) {
                s1++
                eventId++
                lastEvent = "score"
                if (s1 >= 7) {
                    winner = 1
                    lastEvent = "win"
                }
                bx = 0.5f
                by = 0.5f
                vx = -0.0085f
                vy = if (Random.nextBoolean()) 0.006f else -0.006f
            }

            s.copy(
                eventId = eventId,
                lastEvent = lastEvent,
                pong = p.copy(
                    ballX = bx,
                    ballY = by,
                    ballVx = vx,
                    ballVy = vy,
                    p1Y = p1Y,
                    p2Y = p2Y,
                    score1 = s1,
                    score2 = s2,
                    winner = winner,
                ),
            )
        }
    }

    private fun tickSnake(dtMs: Long) {
        val current = _state.value.snake
        if (current.gameOver) return

        val stepInterval = if (snakeBoostActive) {
            85L
        } else {
            (175L - (current.score / 3) * 4L).coerceAtLeast(100L)
        }

        snakeAccumulatorMs += dtMs
        if (snakeAccumulatorMs < stepInterval) return
        snakeAccumulatorMs -= stepInterval

        _state.update { s ->
            val sn = s.snake
            if (sn.gameOver) return@update s

            val (dx, dy) = pendingSnakeDir?.also { pendingSnakeDir = null } ?: (sn.dirX to sn.dirY)
            val head = sn.snake.first()
            val nextHead = GridPoint(head.x + dx, head.y + dy)

            // Wall or self collision
            val hitWall = nextHead.x < 0 || nextHead.x >= sn.cols || nextHead.y < 0 || nextHead.y >= sn.rows
            val bodyToCheck = if (nextHead == sn.food || nextHead == sn.bonusFood) sn.snake else sn.snake.dropLast(1)
            val hitSelf = bodyToCheck.contains(nextHead)

            if (hitWall || hitSelf) {
                return@update s.copy(
                    eventId = s.eventId + 1,
                    lastEvent = "crash",
                    snake = sn.copy(dirX = dx, dirY = dy, gameOver = true),
                )
            }

            val ateFood = nextHead == sn.food
            val ateBonus = sn.bonusFood != null && nextHead == sn.bonusFood
            val newBody = buildList {
                add(nextHead)
                if (ateFood || ateBonus) {
                    addAll(sn.snake)
                } else {
                    addAll(sn.snake.dropLast(1))
                }
            }

            var newFood = sn.food
            var newBonus = sn.bonusFood
            var newScore = sn.score
            var eventId = s.eventId
            var lastEvent = s.lastEvent

            if (ateFood) {
                newScore += 10
                newFood = randomFreePoint(sn.cols, sn.rows, newBody, newBonus)
                if (newBonus == null && newScore % 50 == 0) {
                    newBonus = randomFreePoint(sn.cols, sn.rows, newBody, newFood)
                }
                eventId++
                lastEvent = "score"
            } else if (ateBonus) {
                newScore += 30
                newBonus = null
                eventId++
                lastEvent = "score"
            }

            s.copy(
                eventId = eventId,
                lastEvent = lastEvent,
                snake = sn.copy(
                    snake = newBody,
                    food = newFood,
                    bonusFood = newBonus,
                    dirX = dx,
                    dirY = dy,
                    score = newScore,
                ),
            )
        }
    }

    private fun tickBricks(dtMs: Long) {
        val scale = dtMs / 16f
        _state.update { s ->
            val b = s.bricks
            if (b.gameOver) return@update s

            val paddleSpeed = 0.020f * scale
            var px = b.paddleX
            if (p1LeftHeld) px -= paddleSpeed
            if (p1RightHeld) px += paddleSpeed
            px = px.coerceIn(0.11f, 0.89f)

            if (b.ballAttached) {
                return@update s.copy(
                    bricks = b.copy(paddleX = px, ballX = px, ballY = 0.86f)
                )
            }

            var bx = b.ballX + b.ballVx * scale
            var by = b.ballY + b.ballVy * scale
            var vx = b.ballVx
            var vy = b.ballVy
            var eventId = s.eventId
            var lastEvent = s.lastEvent

            // Side & top walls
            if (bx <= 0.02f) {
                bx = 0.02f
                vx = abs(vx)
                eventId++
                lastEvent = "hit"
            } else if (bx >= 0.98f) {
                bx = 0.98f
                vx = -abs(vx)
                eventId++
                lastEvent = "hit"
            }
            if (by <= 0.03f) {
                by = 0.03f
                vy = abs(vy)
                eventId++
                lastEvent = "hit"
            }

            // Paddle collision (at y = 0.89)
            val paddleHalfW = 0.105f
            if (vy > 0f && by in 0.865f..0.91f && abs(bx - px) <= paddleHalfW) {
                by = 0.864f
                val offset = ((bx - px) / paddleHalfW).coerceIn(-1f, 1f)
                vx = offset * 0.0115f
                vy = -abs(vy).coerceAtLeast(0.0085f)
                eventId++
                lastEvent = "hit"
            }

            // Brick collision
            val topMargin = 0.10f
            val brickAreaH = 0.32f
            val cellW = 0.92f / b.cols
            val cellH = brickAreaH / b.rows
            val leftMargin = 0.04f

            val remainingBricks = b.bricks.toMutableList()
            var scoreGain = 0
            val hitIndex = remainingBricks.indexOfFirst { brick ->
                val x0 = leftMargin + brick.col * cellW
                val x1 = x0 + cellW
                val y0 = topMargin + brick.row * cellH
                val y1 = y0 + cellH
                bx in (x0 - 0.015f)..(x1 + 0.015f) && by in (y0 - 0.015f)..(y1 + 0.015f)
            }
            if (hitIndex != -1) {
                val hit = remainingBricks[hitIndex]
                vy = -vy
                eventId++
                if (hit.hp > 1) {
                    remainingBricks[hitIndex] = hit.copy(hp = hit.hp - 1)
                    scoreGain = 10
                    lastEvent = "hit"
                } else {
                    remainingBricks.removeAt(hitIndex)
                    scoreGain = 20
                    lastEvent = "score"
                }
            }

            // Level cleared?
            if (remainingBricks.isEmpty()) {
                val nextLevel = b.level + 1
                val nextBricks = createBrickGrid(nextLevel, b.cols, b.rows)
                return@update s.copy(
                    eventId = eventId + 1,
                    lastEvent = "win",
                    bricks = b.copy(
                        paddleX = px,
                        ballX = px,
                        ballY = 0.86f,
                        ballVx = 0.007f * (1f + nextLevel * 0.08f),
                        ballVy = -0.009f * (1f + nextLevel * 0.08f),
                        ballAttached = true,
                        bricks = nextBricks,
                        score = b.score + scoreGain + 100,
                        level = nextLevel,
                    ),
                )
            }

            // Missed ball
            if (by > 1.02f) {
                val nextLives = b.lives - 1
                if (nextLives <= 0) {
                    return@update s.copy(
                        eventId = eventId + 1,
                        lastEvent = "crash",
                        bricks = b.copy(
                            paddleX = px,
                            lives = 0,
                            gameOver = true,
                        ),
                    )
                }
                return@update s.copy(
                    eventId = eventId + 1,
                    lastEvent = "crash",
                    bricks = b.copy(
                        paddleX = px,
                        ballX = px,
                        ballY = 0.86f,
                        ballVx = 0.007f,
                        ballVy = -0.0095f,
                        ballAttached = true,
                        bricks = remainingBricks,
                        score = b.score + scoreGain,
                        lives = nextLives,
                    ),
                )
            }

            s.copy(
                eventId = eventId,
                lastEvent = lastEvent,
                bricks = b.copy(
                    paddleX = px,
                    ballX = bx,
                    ballY = by,
                    ballVx = vx,
                    ballVy = vy,
                    bricks = remainingBricks,
                    score = b.score + scoreGain,
                ),
            )
        }
    }

    private fun clearHeldButtons() {
        p1UpHeld = false
        p1DownHeld = false
        p1LeftHeld = false
        p1RightHeld = false
        p2UpHeld = false
        p2DownHeld = false
    }

    private fun freshSnakeState(): SnakeState = SnakeState(
        cols = 24,
        rows = 15,
        snake = listOf(GridPoint(10, 7), GridPoint(9, 7), GridPoint(8, 7)),
        food = GridPoint(16, 7),
    )

    private fun randomFreePoint(cols: Int, rows: Int, occupied: List<GridPoint>, avoid: GridPoint?): GridPoint {
        val free = buildList {
            for (x in 0 until cols) {
                for (y in 0 until rows) {
                    val pt = GridPoint(x, y)
                    if (!occupied.contains(pt) && pt != avoid) add(pt)
                }
            }
        }
        return if (free.isEmpty()) GridPoint(0, 0) else free[Random.nextInt(free.size)]
    }

    private fun initialBricksState(level: Int): BricksState = BricksState(
        bricks = createBrickGrid(level, cols = 8, rows = 5),
        level = level,
    )

    private fun createBrickGrid(level: Int, cols: Int, rows: Int): List<BrickItem> = buildList {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val hp = if (r == 0 || (level > 1 && r == 1)) 2 else 1
                add(BrickItem(row = r, col = c, hp = hp, colorIndex = r))
            }
        }
    }

    private fun evaluateTicTacToe(cells: List<Int>): Pair<Int, List<Int>> {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6),
        )
        for (line in lines) {
            val a = cells[line[0]]
            if (a != 0 && a == cells[line[1]] && a == cells[line[2]]) {
                return a to line
            }
        }
        if (cells.none { it == 0 }) return 3 to emptyList()
        return 0 to emptyList()
    }

    private fun pickSmartTicTacToeMove(cells: MutableList<Int>): Int {
        // 1. Win if possible
        for (i in 0..8) {
            if (cells[i] == 0) {
                cells[i] = 2
                val win = evaluateTicTacToe(cells).first == 2
                cells[i] = 0
                if (win) return i
            }
        }
        // 2. Block player X from winning
        for (i in 0..8) {
            if (cells[i] == 0) {
                cells[i] = 1
                val threat = evaluateTicTacToe(cells).first == 1
                cells[i] = 0
                if (threat) return i
            }
        }
        // 3. Center, corners, then any open
        if (cells[4] == 0) return 4
        val corners = listOf(0, 2, 6, 8).filter { cells[it] == 0 }
        if (corners.isNotEmpty()) return corners.random()
        return (0..8).firstOrNull { cells[it] == 0 } ?: -1
    }
}
