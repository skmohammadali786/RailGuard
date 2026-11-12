#!/usr/bin/env bash
# RailGuard Raspberry Pi Auto-Start Service Setup
#
# Installs:
#   /etc/railguard/railguard.env        (credentials, mode 600)
#   /usr/local/bin/railguard-pi-start.sh (launcher)
#   /etc/systemd/system/railguard.service
#
# Usage:
#   ./setup_autostart.sh <FIREBASE_USER_UID> [DATABASE_URL]
#
# Then edit /etc/railguard/railguard.env and add RAILGUARD_REFRESH_TOKEN +
# RAILGUARD_FIREBASE_API_KEY so cloud sync keeps working after the ID token
# expires. See docs/FIREBASE_SCHEMA.md and the main README.

set -e

RPI_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CURRENT_USER="$(whoami)"
UID_ARG="${1:-test-user}"
DB_URL_ARG="${2:-https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app}"
ENV_FILE="/etc/railguard/railguard.env"
LAUNCHER="/usr/local/bin/railguard-pi-start.sh"
SERVICE_FILE="/etc/systemd/system/railguard.service"

echo "=================================================="
echo " RailGuard Automated Hardware Gateway Installer   "
echo "=================================================="
echo "Directory: $RPI_DIR"
echo "User:      $CURRENT_USER"
echo "UID:       $UID_ARG"
echo ""

echo "[1/4] Creating credential environment file..."
sudo mkdir -p /etc/railguard
if [ ! -f "$ENV_FILE" ]; then
    sudo tee "$ENV_FILE" > /dev/null << EOF
# RailGuard Pi environment (read by railguard-pi-start.sh)
RAILGUARD_USER_UID=$UID_ARG
RAILGUARD_DATABASE_URL=$DB_URL_ARG
RAILGUARD_DEVICE_ID=RPI-TRACK-01
RAILGUARD_PORT=8080
RAILGUARD_SCAN_INTERVAL=2.5

# --- Firebase authentication (choose ONE) -------------------------------
# Recommended: refresh token + web API key (auto-renews forever)
RAILGUARD_REFRESH_TOKEN=
RAILGUARD_FIREBASE_API_KEY=

# Quick test only: short-lived ID token (expires in ~1 hour)
RAILGUARD_ID_TOKEN=

# --- Local Wi-Fi API protection (recommended on shared networks) --------
RAILGUARD_API_KEY=
# 0.0.0.0 = reachable from the phone; 127.0.0.1 = Pi only
RAILGUARD_BIND_HOST=0.0.0.0
EOF
    echo "      Created $ENV_FILE (edit it to add credentials)"
else
    echo "      Keeping existing $ENV_FILE"
fi
sudo chmod 600 "$ENV_FILE"

echo "[2/4] Creating launcher..."
sudo tee "$LAUNCHER" > /dev/null << EOF
#!/usr/bin/env bash
set -e
[ -f "$ENV_FILE" ] && set -a && . "$ENV_FILE" && set +a

ARGS=(
  --uid "\${RAILGUARD_USER_UID:-$UID_ARG}"
  --db "\${RAILGUARD_DATABASE_URL:-$DB_URL_ARG}"
  --device "\${RAILGUARD_DEVICE_ID:-RPI-TRACK-01}"
  --port "\${RAILGUARD_PORT:-8080}"
  --interval "\${RAILGUARD_SCAN_INTERVAL:-2.5}"
)

if [ -n "\${RAILGUARD_REFRESH_TOKEN:-}" ] && [ -n "\${RAILGUARD_FIREBASE_API_KEY:-}" ]; then
  ARGS+=(--refresh-token "\$RAILGUARD_REFRESH_TOKEN" --firebase-api-key "\$RAILGUARD_FIREBASE_API_KEY")
elif [ -n "\${RAILGUARD_ID_TOKEN:-}" ]; then
  ARGS+=(--auth "\$RAILGUARD_ID_TOKEN")
fi

[ -n "\${RAILGUARD_API_KEY:-}" ] && ARGS+=(--local-api-key "\$RAILGUARD_API_KEY")
[ -n "\${RAILGUARD_BIND_HOST:-}" ] && ARGS+=(--bind "\$RAILGUARD_BIND_HOST")
[ "\${RAILGUARD_NO_CLOUD:-0}" = "1" ] && ARGS+=(--no-cloud)

exec /usr/bin/python3 "$RPI_DIR/camera_crack_detector.py" "\${ARGS[@]}"
EOF
sudo chmod 755 "$LAUNCHER"

echo "[3/4] Creating systemd service unit..."
sudo tee "$SERVICE_FILE" > /dev/null << EOF
[Unit]
Description=RailGuard Camera Crack Detector & Sensor Gateway
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=$CURRENT_USER
WorkingDirectory=$RPI_DIR
EnvironmentFile=-$ENV_FILE
ExecStart=$LAUNCHER
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

echo "[4/4] Enabling and starting service..."
sudo systemctl daemon-reload
sudo systemctl enable railguard.service
sudo systemctl restart railguard.service

sleep 2
if systemctl is-active --quiet railguard.service; then
    echo "✓ RailGuard service is ACTIVE and RUNNING."
else
    echo "! Service installed but not active. Check: journalctl -u railguard.service -n 50"
fi

LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
echo ""
echo "=================================================="
echo " Setup complete"
echo " Local Wi-Fi API: http://${LOCAL_IP:-<pi-ip>}:8080"
echo " Credentials:     $ENV_FILE"
echo ""
echo " IMPORTANT: edit $ENV_FILE and set"
echo "   RAILGUARD_REFRESH_TOKEN + RAILGUARD_FIREBASE_API_KEY"
echo " otherwise cloud sync stops after ~1 hour."
echo "=================================================="
