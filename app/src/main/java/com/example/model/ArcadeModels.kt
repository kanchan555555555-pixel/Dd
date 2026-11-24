package com.example.model

import org.json.JSONArray
import org.json.JSONObject

enum class GameType(val id: String) {
    PONG("pong"),
    SNAKE("snake"),
    BRICKS("bricks"),
    TICTACTOE("tictactoe");

    companion object {
        fun fromId(id: String): GameType = entries.firstOrNull { it.id == id } ?: PONG
    }
}

enum class InputAction(val wire: String) {
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    RIGHT("right"),
    ACTION_A("a"),
    ACTION_B("b"),
    START_PAUSE("start"),
    RESET("reset"),
    PADDLE_POS("pos"),
    CELL_TAP("cell");

    companion object {
        fun fromWire(wire: String): InputAction? = entries.firstOrNull { it.wire == wire }
    }
}

enum class DeviceRole {
    HOST_CONTROLLER,
    TV_DISPLAY,
    PLAYER_2_CONTROLLER,
    SOLO_LOCAL,
}

enum class AppScreen {
    HOME,
    CONNECT_TV,
    DISCOVER_HOST,
    CONTROLLER,
    TV_DISPLAY,
}

data class HotspotInfo(
    val localIp: String? = null,
    val candidateIps: List<String> = emptyList(),
    val localOnlyActive: Boolean = false,
    val localOnlySsid: String? = null,
    val localOnlyPassword: String? = null,
    val isHotspotInterfaceDetected: Boolean = false,
)

data class DiscoveredHost(
    val hostName: String,
    val ipAddress: String,
    val tcpPort: Int,
    val httpPort: Int,
    val lastSeenMs: Long = System.currentTimeMillis(),
)

data class ConnectedClient(
    val id: String,
    val name: String,
    val kind: String, // "TV_APP", "TV_WEB", "P2_APP"
    val playerSlot: Int,
)

data class HighScores(
    val pongBest: Int = 0,
    val snakeBest: Int = 0,
    val bricksBest: Int = 0,
    val ticTacToeWins: Int = 0,
)

data class PongState(
    val ballX: Float = 0.5f,
    val ballY: Float = 0.5f,
    val ballVx: Float = 0.008f,
    val ballVy: Float = 0.006f,
    val p1Y: Float = 0.5f,
    val p2Y: Float = 0.5f,
    val score1: Int = 0,
    val score2: Int = 0,
    val twoPlayerMode: Boolean = false,
    val winner: Int = 0, // 0 none, 1 p1, 2 p2
)

data class GridPoint(val x: Int, val y: Int)

data class SnakeState(
    val cols: Int = 24,
    val rows: Int = 15,
    val snake: List<GridPoint> = listOf(GridPoint(10, 7), GridPoint(9, 7), GridPoint(8, 7)),
    val food: GridPoint = GridPoint(16, 7),
    val bonusFood: GridPoint? = null,
    val dirX: Int = 1,
    val dirY: Int = 0,
    val score: Int = 0,
    val gameOver: Boolean = false,
)

data class BrickItem(
    val row: Int,
    val col: Int,
    val hp: Int,
    val colorIndex: Int,
)

data class BricksState(
    val paddleX: Float = 0.5f,
    val ballX: Float = 0.5f,
    val ballY: Float = 0.80f,
    val ballVx: Float = 0.007f,
    val ballVy: Float = -0.009f,
    val ballAttached: Boolean = true,
    val cols: Int = 8,
    val rows: Int = 5,
    val bricks: List<BrickItem> = emptyList(),
    val score: Int = 0,
    val lives: Int = 3,
    val level: Int = 1,
    val gameOver: Boolean = false,
)

data class TicTacToeState(
    val cells: List<Int> = List(9) { 0 }, // 0 empty, 1 X (P1), 2 O (P2/AI)
    val currentTurn: Int = 1,
    val winner: Int = 0, // 0 ongoing, 1 X, 2 O, 3 draw
    val winLine: List<Int> = emptyList(),
    val vsAi: Boolean = true,
    val xWins: Int = 0,
    val oWins: Int = 0,
)

data class ArcadeGameState(
    val activeGame: GameType = GameType.PONG,
    val isPaused: Boolean = true,
    val twoPlayersConnected: Boolean = false,
    val eventId: Long = 0L,
    val lastEvent: String = "", // "hit", "score", "crash", "win"
    val pong: PongState = PongState(),
    val snake: SnakeState = SnakeState(),
    val bricks: BricksState = BricksState(),
    val ticTacToe: TicTacToeState = TicTacToeState(),
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("game", activeGame.id)
        root.put("paused", isPaused)
        root.put("p2Connected", twoPlayersConnected)
        root.put("eventId", eventId)
        root.put("lastEvent", lastEvent)

        when (activeGame) {
            GameType.PONG -> {
                val p = JSONObject()
                p.put("bx", pong.ballX.toDouble())
                p.put("by", pong.ballY.toDouble())
                p.put("p1y", pong.p1Y.toDouble())
                p.put("p2y", pong.p2Y.toDouble())
                p.put("s1", pong.score1)
                p.put("s2", pong.score2)
                p.put("twoP", pong.twoPlayerMode)
                p.put("winner", pong.winner)
                root.put("pong", p)
            }
            GameType.SNAKE -> {
                val s = JSONObject()
                s.put("cols", snake.cols)
                s.put("rows", snake.rows)
                val segs = JSONArray()
                snake.snake.forEach { pt ->
                    segs.put(JSONArray().put(pt.x).put(pt.y))
                }
                s.put("snake", segs)
                s.put("food", JSONArray().put(snake.food.x).put(snake.food.y))
                if (snake.bonusFood != null) {
                    s.put("bonus", JSONArray().put(snake.bonusFood.x).put(snake.bonusFood.y))
                }
                s.put("score", snake.score)
                s.put("over", snake.gameOver)
                root.put("snake", s)
            }
            GameType.BRICKS -> {
                val b = JSONObject()
                b.put("px", bricks.paddleX.toDouble())
                b.put("bx", bricks.ballX.toDouble())
                b.put("by", bricks.ballY.toDouble())
                b.put("attached", bricks.ballAttached)
                b.put("cols", bricks.cols)
                b.put("rows", bricks.rows)
                b.put("score", bricks.score)
                b.put("lives", bricks.lives)
                b.put("level", bricks.level)
                b.put("over", bricks.gameOver)
                val arr = JSONArray()
                bricks.bricks.forEach { item ->
                    arr.put(
                        JSONArray()
                            .put(item.row)
                            .put(item.col)
                            .put(item.hp)
                            .put(item.colorIndex)
                    )
                }
                b.put("bricks", arr)
                root.put("bricks", b)
            }
            GameType.TICTACTOE -> {
                val t = JSONObject()
                val cellsArr = JSONArray()
                ticTacToe.cells.forEach { cellsArr.put(it) }
                t.put("cells", cellsArr)
                t.put("turn", ticTacToe.currentTurn)
                t.put("winner", ticTacToe.winner)
                val winArr = JSONArray()
                ticTacToe.winLine.forEach { winArr.put(it) }
                t.put("winLine", winArr)
                t.put("vsAi", ticTacToe.vsAi)
                t.put("xw", ticTacToe.xWins)
                t.put("ow", ticTacToe.oWins)
                root.put("ttt", t)
            }
        }
        return root.toString()
    }

    companion object {
        fun fromJsonString(raw: String): ArcadeGameState? {
            return try {
                val root = JSONObject(raw)
                val game = GameType.fromId(root.optString("game", "pong"))
                val paused = root.optBoolean("paused", true)
                val p2Connected = root.optBoolean("p2Connected", false)
                val eventId = root.optLong("eventId", 0L)
                val lastEvent = root.optString("lastEvent", "")

                var pong = PongState()
                var snake = SnakeState()
                var bricks = BricksState()
                var ttt = TicTacToeState()

                root.optJSONObject("pong")?.let { p ->
                    pong = PongState(
                        ballX = p.optDouble("bx", 0.5).toFloat(),
                        ballY = p.optDouble("by", 0.5).toFloat(),
                        p1Y = p.optDouble("p1y", 0.5).toFloat(),
                        p2Y = p.optDouble("p2y", 0.5).toFloat(),
                        score1 = p.optInt("s1", 0),
                        score2 = p.optInt("s2", 0),
                        twoPlayerMode = p.optBoolean("twoP", false),
                        winner = p.optInt("winner", 0),
                    )
                }
                root.optJSONObject("snake")?.let { s ->
                    val segArr = s.optJSONArray("snake")
                    val pts = mutableListOf<GridPoint>()
                    if (segArr != null) {
                        for (i in 0 until segArr.length()) {
                            val pair = segArr.getJSONArray(i)
                            pts.add(GridPoint(pair.getInt(0), pair.getInt(1)))
                        }
                    }
                    val fArr = s.optJSONArray("food")
                    val food = if (fArr != null) GridPoint(fArr.getInt(0), fArr.getInt(1)) else GridPoint(16, 7)
                    val bArr = s.optJSONArray("bonus")
                    val bonus = if (bArr != null) GridPoint(bArr.getInt(0), bArr.getInt(1)) else null
                    snake = SnakeState(
                        cols = s.optInt("cols", 24),
                        rows = s.optInt("rows", 15),
                        snake = pts,
                        food = food,
                        bonusFood = bonus,
                        score = s.optInt("score", 0),
                        gameOver = s.optBoolean("over", false),
                    )
                }
                root.optJSONObject("bricks")?.let { b ->
                    val arr = b.optJSONArray("bricks")
                    val list = mutableListOf<BrickItem>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONArray(i)
                            list.add(
                                BrickItem(
                                    row = item.getInt(0),
                                    col = item.getInt(1),
                                    hp = item.getInt(2),
                                    colorIndex = item.getInt(3),
                                )
                            )
                        }
                    }
                    bricks = BricksState(
                        paddleX = b.optDouble("px", 0.5).toFloat(),
                        ballX = b.optDouble("bx", 0.5).toFloat(),
                        ballY = b.optDouble("by", 0.8).toFloat(),
                        ballAttached = b.optBoolean("attached", false),
                        cols = b.optInt("cols", 8),
                        rows = b.optInt("rows", 5),
                        bricks = list,
                        score = b.optInt("score", 0),
                        lives = b.optInt("lives", 3),
                        level = b.optInt("level", 1),
                        gameOver = b.optBoolean("over", false),
                    )
                }
                root.optJSONObject("ttt")?.let { t ->
                    val cArr = t.optJSONArray("cells")
                    val cells = MutableList(9) { 0 }
                    if (cArr != null) {
                        for (i in 0 until minOf(9, cArr.length())) {
                            cells[i] = cArr.getInt(i)
                        }
                    }
                    val wArr = t.optJSONArray("winLine")
                    val winLine = mutableListOf<Int>()
                    if (wArr != null) {
                        for (i in 0 until wArr.length()) {
                            winLine.add(wArr.getInt(i))
                        }
                    }
                    ttt = TicTacToeState(
                        cells = cells,
                        currentTurn = t.optInt("turn", 1),
                        winner = t.optInt("winner", 0),
                        winLine = winLine,
                        vsAi = t.optBoolean("vsAi", true),
                        xWins = t.optInt("xw", 0),
                        oWins = t.optInt("ow", 0),
                    )
                }

                ArcadeGameState(
                    activeGame = game,
                    isPaused = paused,
                    twoPlayersConnected = p2Connected,
                    eventId = eventId,
                    lastEvent = lastEvent,
                    pong = pong,
                    snake = snake,
                    bricks = bricks,
                    ticTacToe = ttt,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
