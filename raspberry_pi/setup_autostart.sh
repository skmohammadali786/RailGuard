#!/usr/bin/env bash
# RailGuard Raspberry Pi Auto-Start Service Setup Script
# Configures camera_crack_detector.py to start automatically upon battery power.

set -e

RPI_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CURRENT_USER="$(whoami)"
UID_ARG="${1:-test-user}"
SERVICE_FILE="/etc/systemd/system/railguard.service"

echo "=================================================="
echo " RailGuard Automated Hardware Gateway Installer   "
echo "=================================================="
echo "Directory: $RPI_DIR"
echo "User:      $CURRENT_USER"
echo "UID:       $UID_ARG"
echo ""

echo "[1/3] Creating systemd service unit..."
sudo tee "$SERVICE_FILE" > /dev/null << EOF
[Unit]
Description=RailGuard Camera Crack Detector & Sensor Gateway
After=network.target

[Service]
Type=simple
User=$CURRENT_USER
WorkingDirectory=$RPI_DIR
ExecStart=/usr/bin/python3 $RPI_DIR/camera_crack_detector.py --uid "$UID_ARG" --port 8080
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

echo "[2/3] Enabling systemd service on boot..."
sudo systemctl daemon-reload
sudo systemctl enable railguard.service
sudo systemctl restart railguard.service

echo "[3/3] Verifying service status..."
sleep 2
if systemctl is-active --quiet railguard.service; then
    echo "✓ RailGuard service is ACTIVE and RUNNING."
else
    echo "! Service installed. Check logs with: journalctl -u railguard.service -f"
fi

LOCAL_IP=$(hostname -I | awk '{print $1}')
echo ""
echo "=================================================="
echo " Setup complete! "
echo " Local Wi-Fi API active at: http://${LOCAL_IP:-192.168.4.1}:8080"
echo " The system will now auto-start whenever battery is connected."
echo "=================================================="
