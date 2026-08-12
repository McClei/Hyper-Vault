#!/usr/bin/env python3
import sys
import json
import requests

URL_INFO = "https://api.hyperliquid.xyz/info"

DEBUG = len(sys.argv) > 3 and sys.argv[3].lower() in ("debug", "1", "true")


def limpiar_argumento(arg):
    if not arg:
        return None
    cleaned = str(arg).strip().strip("'").strip('"').strip()
    if cleaned.lower() in ["", "''", '""', 'null', 'none', 'undefined']:
        return None
    return cleaned


def _val(x):
    """Acepta entradas [timestamp, valor] o valor directo."""
    return float(x[1]) if isinstance(x, list) else float(x)


def extraer_datos_crudos(vault_address, user_address):
    payload = {
        "type": "vaultDetails",
        "vaultAddress": vault_address
    }
    if user_address:
        payload["user"] = user_address

    try:
        response = requests.post(URL_INFO, json=payload, timeout=10)
        response.raise_for_status()
        data = response.json()

        tvl = "0.0"
        your_deposits = "0.0"
        all_time_earned = "0.0"
        past_month_return = "0.0"
        vault_name = ""

        if isinstance(data, dict):
            # 1. TVL de los campos raíz habituales de la API
            tvl = str(data.get("tvl", data.get("tvlTotal", "0.0")))

            # 1b. Nombre de la vault (para el título del widget)
            for k in ("name", "vaultName", "vault_name"):
                v = data.get(k)
                if v and isinstance(v, str) and v.strip():
                    vault_name = v.strip()
                    break

            # 2. Datos del inversor recorriendo 'followers'
            followers = data.get("followers", [])
            if isinstance(followers, list):
                for follower in followers:
                    if follower and isinstance(follower, dict) and follower.get("user", "").lower() == user_address.lower():
                        your_deposits = str(follower.get("vaultEquity", "0.0"))
                        all_time_earned = str(follower.get("allTimePnl", "0.0"))
                        break

            # 3. Mapear 'portfolio' -> [ ["day", {...}], ["month", {...}], ... ]
            portfolio_raw = data.get("portfolio", [])
            month_data = {}
            all_time_data = {}
            if isinstance(portfolio_raw, list):
                for item in portfolio_raw:
                    if isinstance(item, list) and len(item) >= 2:
                        periodo = item[0]
                        contenido = item[1]
                        if periodo == "month":
                            month_data = contenido
                        elif periodo == "allTime":
                            all_time_data = contenido

            # Si el TVL de la raíz vino en 0, lo rescatamos del último account value
            if (tvl == "0.0" or tvl == "0") and all_time_data:
                acc_history = all_time_data.get("accountValueHistory", [])
                if acc_history and isinstance(acc_history, list):
                    ultimo_acc = acc_history[-1]
                    tvl = str(ultimo_acc[1]) if isinstance(ultimo_acc, list) else str(ultimo_acc)

            # 4. Past Month Return en % ajustado por flujos (método Hyperliquid):
            #    por intervalo: r = ΔPnL / account_value_previo; total: Π(1+r) - 1
            if month_data and isinstance(month_data, dict):
                acc = month_data.get("accountValueHistory", []) or []
                pnl = month_data.get("pnlHistory", []) or []
                n = min(len(acc), len(pnl))
                if n >= 2:
                    try:
                        factor = 1.0
                        for i in range(1, n):
                            av_prev = _val(acc[i - 1])
                            delta_pnl = _val(pnl[i]) - _val(pnl[i - 1])
                            if av_prev > 0:
                                r = delta_pnl / av_prev
                                factor *= (1.0 + r)
                                if DEBUG:
                                    print(f"[debug] i={i} av_prev={av_prev:.2f} dpnl={delta_pnl:.2f} r={r*100:.4f}% acum={factor*100-100:.4f}%", file=sys.stderr)
                        past_month_return = str(round((factor - 1.0) * 100, 6))
                    except Exception:
                        past_month_return = "0.0"

        return {
            "TVL": tvl,
            "Your deposits": your_deposits,
            "All-time earned": all_time_earned,
            "Past month return": past_month_return,
            "Vault name": vault_name
        }
    except Exception as e:
        return {"error": f"Fallo al procesar la API: {str(e)}"}


if __name__ == "__main__":
    try:
        user = limpiar_argumento(sys.argv[1]) if len(sys.argv) > 1 else None
        vault = limpiar_argumento(sys.argv[2]) if len(sys.argv) > 2 else None
        if not vault or not user:
            print(json.dumps({"error": "Faltan parametros (?user=0x...&vault=0x...)"}))
            sys.exit(0)
        resultado = extraer_datos_crudos(vault, user)
        print(json.dumps(resultado))
    except Exception as unhandled:
        print(json.dumps({"error": f"Error: {str(unhandled)}"}))
