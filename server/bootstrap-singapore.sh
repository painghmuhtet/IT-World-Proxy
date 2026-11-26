#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/opt/it-world-proxy"
WG_DIR="$APP_DIR/wg-easy"
PUBLIC_HOST="209.74.81.83"
WG_PORT="51820"
WG_WEB_PORT="51821"

echo "[IT World Proxy] Preparing Singapore WireGuard server..."

if ! command -v docker >/dev/null 2>&1; then
  echo "[1/5] Installing Docker..."
  curl -fsSL https://get.docker.com | sh
fi

systemctl enable --now docker

echo "[2/5] Enabling IPv4 forwarding..."
cat >/etc/sysctl.d/99-it-world-proxy.conf <<'EOF'
net.ipv4.ip_forward=1
net.ipv4.conf.all.src_valid_mark=1
EOF
sysctl --system >/dev/null

mkdir -p "$WG_DIR/secrets"
chmod 700 "$WG_DIR/secrets"

if [ ! -s "$WG_DIR/secrets/admin_password" ]; then
  umask 077
  tr -dc 'A-Za-z0-9@#%+=_' </dev/urandom | head -c 24 > "$WG_DIR/secrets/admin_password"
  printf '\n' >> "$WG_DIR/secrets/admin_password"
fi

cat >"$WG_DIR/docker-compose.yml" <<'EOF'
services:
  wg-easy:
    image: ghcr.io/wg-easy/wg-easy:15
    container_name: wg-easy
    restart: unless-stopped
    volumes:
      - etc_wireguard:/etc/wireguard
      - /lib/modules:/lib/modules:ro
      - ./secrets/admin_password:/run/secrets/admin_password:ro
    ports:
      - "51820:51820/udp"
      - "51821:51821/tcp"
    environment:
      - INIT_ENABLED=true
      - INIT_USERNAME=admin
      - INIT_PASSWORD_FILE=/run/secrets/admin_password
      - INIT_HOST=209.74.81.83
      - INIT_PORT=51820
      - INIT_DNS=1.1.1.1,8.8.8.8
      - INIT_IPV4_CIDR=10.8.0.0/24
      - INIT_IPV6_CIDR=fd42:42:42::/64
      - INSECURE=true
    cap_add:
      - NET_ADMIN
      - SYS_MODULE
    sysctls:
      - net.ipv4.ip_forward=1
      - net.ipv4.conf.all.src_valid_mark=1
    networks:
      wg:
        ipv4_address: 10.42.42.42
        ipv6_address: fdcc:ad94:bacf:61a3::2a

volumes:
  etc_wireguard:

networks:
  wg:
    driver: bridge
    enable_ipv6: true
    ipam:
      config:
        - subnet: 10.42.42.0/24
        - subnet: fdcc:ad94:bacf:61a3::/64
EOF

echo "[3/5] Opening WireGuard UDP port..."
if command -v ufw >/dev/null 2>&1 && ufw status | grep -q "Status: active"; then
  ufw allow 51820/udp >/dev/null
fi

echo "[4/5] Starting wg-easy..."
cd "$WG_DIR"
docker compose up -d

echo "[5/5] Checking container..."
sleep 3
docker ps --filter name=wg-easy --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

echo
echo "=============================================="
echo "IT World Proxy — Singapore WireGuard"
echo "Web UI: http://209.74.81.83:51821"
echo "Username: admin"
echo "Password: $(cat "$WG_DIR/secrets/admin_password")"
echo
echo "IMPORTANT: keep the password private."
echo "WireGuard UDP: 51820"
echo "=============================================="
