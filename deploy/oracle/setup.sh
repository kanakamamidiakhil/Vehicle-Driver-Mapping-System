#!/usr/bin/env bash
# One-command install/update for an Ubuntu VM (tested target: Oracle Cloud Always Free, Ubuntu 24.04).
#   curl -fsSL https://raw.githubusercontent.com/kanakamamidiakhil/Vehicle-Driver-Mapping-System/main/deploy/oracle/setup.sh | bash
# Re-run the same command later to pull the latest code and redeploy.
set -euo pipefail

REPO_URL=${REPO_URL:-https://github.com/kanakamamidiakhil/Vehicle-Driver-Mapping-System.git}
BRANCH=${BRANCH:-main}
APP_DIR=${APP_DIR:-$HOME/Vehicle-Driver-Mapping-System}
MODEL=${OLLAMA_MODEL:-qwen2.5:3b}

step() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }

step "Opening port 80 in the VM firewall"
# Oracle's Ubuntu images reject all inbound traffic except SSH via iptables. Done before Docker is
# installed so the saved rule set does not capture Docker's own rules.
sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq git curl iptables-persistent > /dev/null
if ! sudo iptables -C INPUT -p tcp --dport 80 -j ACCEPT 2> /dev/null; then
  reject_line=$(sudo iptables -L INPUT --line-numbers | awk '/REJECT/ {print $1; exit}')
  if [ -n "$reject_line" ]; then
    sudo iptables -I INPUT "$reject_line" -p tcp --dport 80 -j ACCEPT
  else
    sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
  fi
  sudo netfilter-persistent save > /dev/null
fi

step "Installing Docker"
if ! command -v docker > /dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
fi
sudo usermod -aG docker "$USER" || true

step "Fetching the code ($BRANCH)"
if [ -d "$APP_DIR/.git" ]; then
  git -C "$APP_DIR" fetch --quiet origin "$BRANCH"
  git -C "$APP_DIR" checkout --quiet "$BRANCH"
  git -C "$APP_DIR" reset --quiet --hard "origin/$BRANCH"
else
  git clone --quiet --branch "$BRANCH" "$REPO_URL" "$APP_DIR"
fi

cd "$APP_DIR/deploy/oracle"
if [ ! -f .env ]; then
  step "Generating a database password (stored in $APP_DIR/deploy/oracle/.env)"
  umask 077
  db_secret=$(openssl rand -hex 24)
  printf 'POSTGRES_PASSWORD=%s\nDB_PASSWORD=%s\nOLLAMA_MODEL=%s\n' "$db_secret" "$db_secret" "$MODEL" > .env
fi

step "Building and starting the app (first run takes 10-20 minutes)"
sudo docker compose up -d --build

step "Downloading the AI model (first run only, about 2 GB)"
sudo docker compose logs -f ollama-pull 2>&1 | grep -vE 'pulling [0-9a-f]+:.*[0-9]+%' || true

step "Waiting for the backend to start"
for _ in $(seq 90); do
  if curl -sf http://localhost/api/ai/status > /dev/null; then break; fi
  sleep 5
done

ip=$(curl -s --max-time 5 https://api.ipify.org || true)
if curl -sf http://localhost/api/ai/status > /dev/null; then
  step "Done! Open http://${ip:-<your-vm-public-ip>} in your browser."
  curl -s http://localhost/api/ai/status; echo
else
  echo "The backend is not answering yet. Check the logs with:"
  echo "  cd $APP_DIR/deploy/oracle && sudo docker compose logs --tail=100 backend"
  exit 1
fi
