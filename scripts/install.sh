#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -f SHA256SUMS ]; then sha256sum -c SHA256SUMS; fi
if [ ! -f .env ]; then
  umask 077
  printf 'HTTP_PORT=8080\nBIND_ADDRESS=127.0.0.1\n' > .env
  for key in MYSQL_ROOT_PASSWORD RUNTIME_DB_PASSWORD DEMO_PASSWORD APP_AUTH_SECRET; do
    value=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
    printf '%s=%s\n' "$key" "$value" >> .env
  done
fi
if [ -f images.tar ]; then docker load -i images.tar; fi
docker compose -f compose.release.yaml up -d --wait --wait-timeout 300
docker compose -f compose.release.yaml ps
printf 'Services are ready. Obtain the login password from .env locally.\n'
