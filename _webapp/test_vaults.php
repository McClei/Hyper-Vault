<?php
header('Content-Type: text/html; charset=utf-8');

/* ══════════════════════════════════════════════════
   HL VAULT MONITOR · Skin "Terminal" (Trading Pro)
   Layout 2×2 · col 2 derecha · $ en USD
   Título: nombre de la vault · Meta: wallet ››› vault
   Uso: test_vaults.php?user=0x…&vault=0x…  (&raw=1 debug)
   ══════════════════════════════════════════════════ */

$python_path = 'python3';
$script_path = __DIR__ . '/api_vaults.py';

$user_input  = (isset($_GET['user'])  && !empty($_GET['user']))  ? trim($_GET['user'])  : 'none';
$vault_input = (isset($_GET['vault']) && !empty($_GET['vault'])) ? trim($_GET['vault']) : 'none';

$comando = "$python_path $script_path " . escapeshellarg($user_input) . ' ' . escapeshellarg($vault_input) . " 2>&1";
$output  = shell_exec($comando);
$datos   = json_decode($output, true);

/* Modo debug: salida cruda del .py */
if (isset($_GET['raw'])) {
    header('Content-Type: text/plain; charset=utf-8');
    echo $output;
    exit;
}

/* ── Helpers de formato ─────────────────────────── */
function short_addr($a){
    $a = trim((string)$a);
    return (strlen($a) > 13) ? substr($a, 0, 6) . '…' . substr($a, -4) : $a;
}
function num($v, $dec = 2){ return number_format(abs((float)$v), $dec, '.', ','); }
function sgn($v){ return ((float)$v) < 0 ? '−' : '+'; }
function cls($v){ return ((float)$v) < 0 ? 'neg' : 'pos'; }

/* ── Extracción de datos ────────────────────────── */
$error = null;
if ($user_input === 'none' || $vault_input === 'none') {
    $error = 'Faltan parámetros: ?user=0x…&vault=0x…';
} elseif ($datos === null) {
    $error = 'Error de procesamiento: ' . $output;
} elseif (isset($datos['error'])) {
    $error = $datos['error'];
}

$tvl      = isset($datos['TVL'])               ? (float)$datos['TVL']               : 0.0;
$deposits = isset($datos['Your deposits'])     ? (float)$datos['Your deposits']     : 0.0;
$earned   = isset($datos['All-time earned'])   ? (float)$datos['All-time earned']   : 0.0;
$month    = isset($datos['Past month return']) ? (float)$datos['Past month return'] : 0.0;

$vault_name  = isset($datos['Vault name']) ? trim((string)$datos['Vault name']) : '';
$vault_short = ($vault_input !== 'none') ? short_addr($vault_input) : '—';
$user_short  = ($user_input  !== 'none') ? short_addr($user_input)  : '—';
$vault_title = ($vault_name !== '') ? $vault_name : $vault_short;
$hora        = date('H:i');
?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="refresh" content="3600"> <!-- auto-refresco cada hora -->
<title>HL Vault Monitor — <?= htmlspecialchars($vault_title) ?></title>
<style>
  :root{
    --bg:#05070a;      --card:#0d1117;   --line:#1d2733;
    --grid:rgba(140,160,180,.06);
    --txt:#e6edf3;     --muted:#7d8590;
    --green:#3fe081;   --red:#ff5c5c;    --live:#2eea8c;
  }
  *{box-sizing:border-box;margin:0;padding:0}
  html,body{height:100%}
  body{
    display:flex;align-items:center;justify-content:center;padding:24px;
    background:radial-gradient(1100px 600px at 70% -10%, #0a1018 0%, var(--bg) 60%);
    font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,"Roboto Mono",monospace;
    color:var(--txt);
  }
  .widget{
    width:100%;max-width:400px;position:relative;overflow:hidden;
    background:var(--card);border:1px solid var(--line);border-radius:22px;
    box-shadow:0 24px 70px rgba(0,0,0,.65);
  }
  .widget::before{
    content:"";position:absolute;inset:0;pointer-events:none;
    background:
      linear-gradient(var(--grid) 1px, transparent 1px),
      linear-gradient(90deg, var(--grid) 1px, transparent 1px);
    background-size:26px 26px;
  }
  .head{
    display:flex;align-items:center;justify-content:space-between;gap:12px;
    padding:18px 20px;border-bottom:1px solid var(--line);
  }
  .brand{display:flex;align-items:center;gap:10px;font-weight:700;font-size:14px;letter-spacing:.08em;white-space:nowrap}
  .dot{
    width:9px;height:9px;border-radius:50%;background:var(--live);
    box-shadow:0 0 10px var(--live);animation:pulse 2s ease-in-out infinite;flex:none;
  }
  @keyframes pulse{0%,100%{opacity:1}50%{opacity:.3}}
  .vault-name{
    color:var(--txt);font-size:12px;letter-spacing:.04em;
    max-width:55%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-align:right;
  }

  /* ── Rejilla 2×2 de métricas ── */
  .grid{display:grid;grid-template-columns:1fr 1fr}
  .cell{padding:16px 18px;display:flex;flex-direction:column;gap:8px;min-width:0}
  .cell:nth-child(-n+2){border-bottom:1px solid var(--line)}
  .cell:nth-child(odd){border-right:1px solid var(--line)}
  .cell:nth-child(even){align-items:flex-end;text-align:right}
  .label{
    font-size:10px;letter-spacing:.14em;text-transform:uppercase;color:var(--muted);
    white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%;
  }
  .value{font-size:20px;font-weight:700;letter-spacing:.02em;white-space:nowrap}
  .pos{color:var(--green);text-shadow:0 0 14px rgba(63,224,129,.25)}
  .neg{color:var(--red); text-shadow:0 0 14px rgba(255,92,92,.25)}

  /* ── Línea de addresses: wallet ››› vault ── */
  .meta{
    display:flex;align-items:center;justify-content:space-between;gap:10px;
    padding:12px 20px;border-top:1px solid var(--line);
    color:var(--muted);font-size:10.5px;letter-spacing:.08em;
  }
  .m-left,.m-right{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
  .m-right{text-align:right}
  .arrow{display:inline-flex;gap:2px;color:var(--green);flex:none}
  .arrow i{font-style:normal;animation:flow 1.2s ease-in-out infinite}
  .arrow i:nth-child(2){animation-delay:.15s}
  .arrow i:nth-child(3){animation-delay:.3s}
  @keyframes flow{0%,100%{opacity:.25}50%{opacity:1}}

  .foot{
    padding:12px 20px;border-top:1px solid var(--line);text-align:center;
    color:var(--muted);font-size:10.5px;letter-spacing:.12em;
  }
  .error{padding:22px 20px}
  .error-title{color:var(--red);font-weight:700;letter-spacing:.12em;margin-bottom:8px}
  .error p{color:var(--muted);font-size:12px;word-break:break-all}

  @media (max-width:360px){
    .grid{grid-template-columns:1fr}
    .cell:nth-child(odd){border-right:none}
    .cell:nth-child(3){border-bottom:1px solid var(--line)}
    .cell:nth-child(even){align-items:flex-start;text-align:left}
  }
</style>
</head>
<body>

<main class="widget" aria-label="HL Vault Monitor">
  <header class="head">
    <div class="brand"><span class="dot"></span>HL VAULT MONITOR</div>
    <div class="vault-name" title="<?= htmlspecialchars($vault_title) ?>"><?= htmlspecialchars($vault_title) ?></div>
  </header>

  <?php if ($error): ?>
    <section class="error">
      <div class="error-title">⚠ SIGNAL LOST</div>
      <p><?= htmlspecialchars($error) ?></p>
    </section>
  <?php else: ?>
    <section class="grid">
      <!-- Fila 1 -->
      <div class="cell">
        <span class="label">TVL</span>
        <span class="value">$<?= num($tvl, 0) ?></span>
      </div>
      <div class="cell">
        <span class="label">Past month return</span>
        <span class="value <?= cls($month) ?>"><?= sgn($month) . num($month, 2) ?>%</span>
      </div>
      <!-- Fila 2 -->
      <div class="cell">
        <span class="label">Your deposits</span>
        <span class="value">$<?= num($deposits, 2) ?></span>
      </div>
      <div class="cell">
        <span class="label">All-time earned</span>
        <span class="value <?= cls($earned) ?>"><?= sgn($earned) ?>$<?= num($earned, 2) ?></span>
      </div>
    </section>
    <section class="meta">
      <span class="m-left">WALLET&nbsp;<?= htmlspecialchars($user_short) ?></span>
      <span class="arrow" aria-hidden="true"><i>›</i><i>›</i><i>›</i></span>
      <span class="m-right">VAULT&nbsp;<?= htmlspecialchars($vault_short) ?></span>
    </section>
  <?php endif; ?>

  <footer class="foot">UPDATED <?= $hora ?> · AUTO-REFRESH HOURLY ⟳</footer>
</main>

</body>
</html>
