package com.example.network

object TvWebAsset {

    fun buildHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1.0" />
<title>Hotspot Arcade — TV Display</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body {
    width: 100%; height: 100%;
    background: #0B0E17;
    color: #F0F4FF;
    font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  .topbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 28px;
    background: linear-gradient(90deg, #111629 0%, #191F38 50%, #111629 100%);
    border-bottom: 2px solid #232D4B;
  }
  .brand {
    display: flex; align-items: center; gap: 12px;
    font-size: 22px; font-weight: 800; letter-spacing: 0.5px;
    color: #00F0FF;
  }
  .pill {
    display: inline-flex; align-items: center; gap: 8px;
    padding: 6px 14px; border-radius: 999px;
    background: #17213A; border: 1px solid #2A3A63;
    font-size: 14px; font-weight: 700; color: #A6FF00;
  }
  .dot {
    width: 10px; height: 10px; border-radius: 50%;
    background: #A6FF00; box-shadow: 0 0 10px #A6FF00;
  }
  .dot.offline { background: #FF4D6D; box-shadow: 0 0 10px #FF4D6D; }
  .hud {
    display: flex; align-items: center; gap: 24px;
    font-size: 20px; font-weight: 700;
  }
  .stage {
    flex: 1; display: flex; align-items: center; justify-content: center;
    padding: 16px 24px; position: relative;
  }
  canvas {
    width: 100%; height: 100%;
    max-width: 1360px; max-height: 760px;
    background: #0F1424;
    border: 2px solid #263253;
    border-radius: 18px;
    box-shadow: 0 0 50px rgba(0, 240, 255, 0.12);
  }
  .overlay {
    position: absolute; inset: 0;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    pointer-events: none; text-align: center;
  }
  .overlay-card {
    background: rgba(11, 14, 23, 0.86);
    border: 2px solid #00F0FF;
    border-radius: 20px;
    padding: 24px 40px;
    box-shadow: 0 0 40px rgba(0, 240, 255, 0.25);
  }
  .overlay-card h2 { font-size: 36px; color: #00F0FF; margin-bottom: 8px; }
  .overlay-card p { font-size: 18px; color: #CBD5E1; }
</style>
</head>
<body>
  <div class="topbar">
    <div class="brand">
      <span>HOTSPOT ARCADE TV</span>
      <span class="pill"><span id="statusDot" class="dot offline"></span><span id="statusText">CONNECTING...</span></span>
    </div>
    <div class="hud">
      <span id="gameTitle" style="color:#FF2E93;">NEON PONG</span>
      <span id="scoreText" style="color:#F0F4FF;">0 - 0</span>
    </div>
  </div>
  <div class="stage">
    <canvas id="tvCanvas" width="1280" height="720"></canvas>
    <div id="overlay" class="overlay">
      <div class="overlay-card">
        <h2 id="overlayTitle">CONNECTED TO HOTSPOT</h2>
        <p id="overlaySub">Press START or A on your phone gamepad</p>
      </div>
    </div>
  </div>

<script>
const canvas = document.getElementById('tvCanvas');
const ctx = canvas.getContext('2d');
const statusDot = document.getElementById('statusDot');
const statusText = document.getElementById('statusText');
const gameTitle = document.getElementById('gameTitle');
const scoreText = document.getElementById('scoreText');
const overlay = document.getElementById('overlay');
const overlayTitle = document.getElementById('overlayTitle');
const overlaySub = document.getElementById('overlaySub');

let state = null;
let ws = null;

function connectWs() {
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
  ws = new WebSocket(proto + '//' + location.host + '/ws');
  ws.onopen = () => {
    statusDot.classList.remove('offline');
    statusText.textContent = 'OFFLINE HOTSPOT LIVE';
  };
  ws.onmessage = (evt) => {
    try {
      state = JSON.parse(evt.data);
      render();
    } catch (e) {}
  };
  ws.onclose = () => {
    statusDot.classList.add('offline');
    statusText.textContent = 'RECONNECTING...';
    setTimeout(connectWs, 1000);
  };
}
connectWs();

function setOverlay(show, title, sub) {
  overlay.style.display = show ? 'flex' : 'none';
  if (show) {
    overlayTitle.textContent = title;
    overlaySub.textContent = sub;
  }
}

function drawGrid(w, h) {
  ctx.fillStyle = '#0B0E17';
  ctx.fillRect(0, 0, w, h);
  ctx.strokeStyle = 'rgba(0, 240, 255, 0.06)';
  ctx.lineWidth = 1;
  for (let x = 0; x < w; x += 48) {
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
  }
  for (let y = 0; y < h; y += 48) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
  }
}

function render() {
  if (!state) return;
  const w = canvas.width, h = canvas.height;
  drawGrid(w, h);

  if (state.game === 'pong' && state.pong) {
    const p = state.pong;
    gameTitle.textContent = p.twoP ? 'NEON PONG • 2 PLAYERS' : 'NEON PONG • VS AI';
    scoreText.textContent = p.s1 + '  :  ' + p.s2;

    // Center dashed net
    ctx.setLineDash([14, 14]);
    ctx.strokeStyle = 'rgba(240, 244, 255, 0.25)';
    ctx.lineWidth = 4;
    ctx.beginPath(); ctx.moveTo(w / 2, 20); ctx.lineTo(w / 2, h - 20); ctx.stroke();
    ctx.setLineDash([]);

    // Paddles
    const ph = h * 0.22;
    ctx.shadowBlur = 22;
    ctx.shadowColor = '#00F0FF';
    ctx.fillStyle = '#00F0FF';
    ctx.fillRect(w * 0.045, p.p1y * h - ph / 2, 20, ph);

    ctx.shadowColor = '#FF2E93';
    ctx.fillStyle = '#FF2E93';
    ctx.fillRect(w * 0.955 - 20, p.p2y * h - ph / 2, 20, ph);

    // Ball
    ctx.shadowColor = '#A6FF00';
    ctx.fillStyle = '#A6FF00';
    ctx.beginPath();
    ctx.arc(p.bx * w, p.by * h, 14, 0, Math.PI * 2);
    ctx.fill();
    ctx.shadowBlur = 0;

    if (p.winner !== 0) {
      setOverlay(true, 'PLAYER ' + p.winner + ' WINS!', 'Press START or A on phone for rematch');
    } else if (state.paused) {
      setOverlay(true, 'PAUSED', 'Press START or A on your phone');
    } else {
      setOverlay(false);
    }
  } else if (state.game === 'snake' && state.snake) {
    const s = state.snake;
    gameTitle.textContent = 'TURBO SNAKE';
    scoreText.textContent = 'SCORE ' + s.score;
    const cw = w / s.cols, ch = h / s.rows;

    // Food
    ctx.shadowBlur = 20;
    ctx.shadowColor = '#FF2E93';
    ctx.fillStyle = '#FF2E93';
    ctx.beginPath();
    ctx.arc((s.food[0] + 0.5) * cw, (s.food[1] + 0.5) * ch, Math.min(cw, ch) * 0.36, 0, Math.PI * 2);
    ctx.fill();

    if (s.bonus) {
      ctx.shadowColor = '#FFB800';
      ctx.fillStyle = '#FFB800';
      ctx.beginPath();
      ctx.arc((s.bonus[0] + 0.5) * cw, (s.bonus[1] + 0.5) * ch, Math.min(cw, ch) * 0.42, 0, Math.PI * 2);
      ctx.fill();
    }

    // Snake body
    s.snake.forEach((seg, idx) => {
      ctx.shadowColor = idx === 0 ? '#00F0FF' : '#A6FF00';
      ctx.fillStyle = idx === 0 ? '#00F0FF' : '#A6FF00';
      const pad = 3;
      ctx.fillRect(seg[0] * cw + pad, seg[1] * ch + pad, cw - pad * 2, ch - pad * 2);
    });
    ctx.shadowBlur = 0;

    if (s.over) {
      setOverlay(true, 'GAME OVER • ' + s.score, 'Press START or A on phone to play again');
    } else if (state.paused) {
      setOverlay(true, 'TURBO SNAKE', 'Press D-Pad or START on your phone');
    } else {
      setOverlay(false);
    }
  } else if (state.game === 'bricks' && state.bricks) {
    const b = state.bricks;
    gameTitle.textContent = 'BRICK BREAKER • LVL ' + b.level;
    scoreText.textContent = 'SCORE ' + b.score + '  •  LIVES ' + b.lives;

    const palette = ['#FF2E93', '#FFB800', '#00F0FF', '#A6FF00', '#A855F7'];
    const leftM = w * 0.04, topM = h * 0.10;
    const cellW = (w * 0.92) / b.cols, cellH = (h * 0.32) / b.rows;
    b.bricks.forEach(br => {
      const col = palette[br[3] % palette.length];
      ctx.fillStyle = col;
      ctx.shadowBlur = 12; ctx.shadowColor = col;
      ctx.fillRect(leftM + br[1] * cellW + 4, topM + br[0] * cellH + 4, cellW - 8, cellH - 8);
      if (br[2] > 1) {
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 2;
        ctx.strokeRect(leftM + br[1] * cellW + 6, topM + br[0] * cellH + 6, cellW - 12, cellH - 12);
      }
    });

    // Paddle
    const pw = w * 0.21;
    ctx.shadowColor = '#00F0FF'; ctx.fillStyle = '#00F0FF';
    ctx.fillRect(b.px * w - pw / 2, h * 0.89, pw, 18);

    // Ball
    ctx.shadowColor = '#A6FF00'; ctx.fillStyle = '#A6FF00';
    ctx.beginPath(); ctx.arc(b.bx * w, b.by * h, 12, 0, Math.PI * 2); ctx.fill();
    ctx.shadowBlur = 0;

    if (b.over) {
      setOverlay(true, 'GAME OVER • ' + b.score, 'Press START or A on phone to restart');
    } else if (state.paused || b.attached) {
      setOverlay(true, 'READY', 'Press A on phone to launch ball');
    } else {
      setOverlay(false);
    }
  } else if (state.game === 'tictactoe' && state.ttt) {
    const t = state.ttt;
    gameTitle.textContent = t.vsAi ? 'TIC-TAC-TOE • VS AI' : 'TIC-TAC-TOE • 2P DUEL';
    scoreText.textContent = 'X: ' + t.xw + '   O: ' + t.ow;

    const boardSize = Math.min(w, h) * 0.72;
    const bx0 = (w - boardSize) / 2, by0 = (h - boardSize) / 2;
    const cell = boardSize / 3;

    ctx.strokeStyle = '#2B3A66'; ctx.lineWidth = 6;
    for (let i = 1; i <= 2; i++) {
      ctx.beginPath(); ctx.moveTo(bx0 + i * cell, by0); ctx.lineTo(bx0 + i * cell, by0 + boardSize); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(bx0, by0 + i * cell); ctx.lineTo(bx0 + boardSize, by0 + i * cell); ctx.stroke();
    }

    t.cells.forEach((v, idx) => {
      const r = Math.floor(idx / 3), c = idx % 3;
      const cx = bx0 + (c + 0.5) * cell, cy = by0 + (r + 0.5) * cell;
      const pad = cell * 0.25;
      if (v === 1) {
        ctx.strokeStyle = '#00F0FF'; ctx.lineWidth = 14; ctx.shadowBlur = 18; ctx.shadowColor = '#00F0FF';
        ctx.beginPath(); ctx.moveTo(cx - pad, cy - pad); ctx.lineTo(cx + pad, cy + pad); ctx.stroke();
        ctx.beginPath(); ctx.moveTo(cx + pad, cy - pad); ctx.lineTo(cx - pad, cy + pad); ctx.stroke();
      } else if (v === 2) {
        ctx.strokeStyle = '#FF2E93'; ctx.lineWidth = 14; ctx.shadowBlur = 18; ctx.shadowColor = '#FF2E93';
        ctx.beginPath(); ctx.arc(cx, cy, pad, 0, Math.PI * 2); ctx.stroke();
      }
      ctx.shadowBlur = 0;
    });

    if (t.winner !== 0) {
      const msg = t.winner === 3 ? 'DRAW MATCH!' : ('PLAYER ' + (t.winner === 1 ? 'X' : 'O') + ' WINS!');
      setOverlay(true, msg, 'Tap A or Reset on phone for next round');
    } else {
      setOverlay(false);
    }
  }
}
</script>
</body>
</html>
""".trimIndent()
}
