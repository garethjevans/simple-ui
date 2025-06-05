#!/usr/bin/env bash

set -euo pipefail

export ADMIN_API_KEY=90210
echo "DEBUG: $ADMIN_API_KEY"
export HOST=localhost:9000
echo "DEBUG: $HOST"

# List all available endpoints
curl -sSL -X GET -H "Authorization: Bearer $ADMIN_API_KEY" -H "Accept: application/json" http://$HOST/api/config/endpoint | jq

export CLIENT_NAME="my-client-$RANDOM"
export KEY_NAME="my-key-$RANDOM"

export ENDPOINT=$(curl -s -X GET -H "Authorization: Bearer $ADMIN_API_KEY" -H "Accept: application/json" http://$HOST/api/config/endpoint | jq -r '.[0].spec.name')

echo "DEBUG: Creating client $CLIENT_NAME"

curl --fail -s -X PUT \
  -H "Authorization: Bearer $ADMIN_API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"spec":{"name":"'"$CLIENT_NAME"'"}}' http://$HOST/api/config/client | jq

echo "Creating an Authorization Key for client $CLIENT_NAME"

KEY=$(curl --fail -s -X POST -H "Authorization: Bearer $ADMIN_API_KEY" -H 'Content-Type: application/json' -d '{"spec":{"name":"'"$KEY_NAME"'","clientName":"'"$CLIENT_NAME"'"}}' http://$HOST/api/config/endpoint/$ENDPOINT/key | jq -r '.spec.key')
echo "DEBUG: $KEY"

echo ""
echo "export OPENAI_KEY=$KEY"
echo "export OPENAI_URL=http://$HOST/$ENDPOINT/openai/"

#curl -i -H "Authorization: Bearer $KEY" \
#  -H "Content-type: application/json" \
#  --data '{ "model": "'"$MODEL"'", "messages": [{"role": "user", "content": "Tell me a joke"}], "max_tokens": 1024, "n": 1, "temperature": 0.5 }' \
#  http://$HOST/$ENDPOINT/openai/v1/chat/completions
