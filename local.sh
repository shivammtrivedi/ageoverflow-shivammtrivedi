#!/bin/bash
set -e

# Start the app and database in detached mode, rebuilding the image
docker compose up --build -d

# Wait for the app to be healthy before the test runner hits it.
# Polls the health endpoint every 2 seconds, giving up after 60 seconds.
echo "Waiting for app to be ready..."
for i in $(seq 1 30); do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/health | grep -q "200"; then
        echo "App is ready."
        exit 0
    fi
    sleep 2
done

echo "App failed to start in time."
exit 1
