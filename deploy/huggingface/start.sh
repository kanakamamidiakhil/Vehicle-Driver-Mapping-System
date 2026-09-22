#!/usr/bin/env bash
# Starts PostgreSQL, the Ollama LLM server and the Spring Boot app inside one container.
set -euo pipefail

PGBIN=$(ls -d /usr/lib/postgresql/*/bin | sort -V | tail -1)
PGDATA=${PGDATA:-$HOME/pgdata}
PGPORT=${PGPORT:-5432}

# PostgreSQL: local-only, trust auth (nothing outside the container can reach it).
if [ ! -s "$PGDATA/PG_VERSION" ]; then
  mkdir -p "$PGDATA"
  "$PGBIN/initdb" -D "$PGDATA" -U postgres --auth=trust > /dev/null
fi
mkdir -p "$HOME/pgsocket"
"$PGBIN/pg_ctl" -D "$PGDATA" -l "$HOME/postgres.log" -w \
  -o "-k $HOME/pgsocket -p $PGPORT -c listen_addresses=localhost" start
if ! "$PGBIN/psql" -h localhost -p "$PGPORT" -U postgres -tAc \
     "SELECT 1 FROM pg_database WHERE datname = 'vehicle_mapping'" | grep -q 1; then
  "$PGBIN/createdb" -h localhost -p "$PGPORT" -U postgres vehicle_mapping
fi

# Ollama: start in the background and preload the model so the first question is quicker.
if [ "${AI_ENABLED:-true}" = "true" ] && command -v ollama > /dev/null; then
  ollama serve > "$HOME/ollama.log" 2>&1 &
  (
    for _ in $(seq 60); do curl -sf "$OLLAMA_BASE_URL/api/tags" > /dev/null && break; sleep 1; done
    curl -s "$OLLAMA_BASE_URL/api/generate" \
      -d "{\"model\": \"$OLLAMA_MODEL\", \"keep_alive\": \"24h\"}" > /dev/null || true
  ) &
fi

# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -jar /app/app.jar
