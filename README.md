# simple-chat-ui

A very simple chat ui that can be used to test Spring-AI/Tanzu AI Server (or any OpenAI compatible endpoint)

Then access the UI in the browser at http://localhost:9091

## Run locally against any OpenAI endpoint

```shell
export OPENAI_API_KEY=...
export OPENAI_BASE_URL=...
export OPENAI_MODEL=...

./mvnw spring-boot:run
```

## Run locally against Tanzu AI Server

```shell
export ENDPOINT_NAME=test
export API_KEY=<key>
export API_BASE=http://<ai-server-url>/$ENDPOINT_NAME
export CONFIG_URL=$API_BASE/config/v1/endpoint
export VCAP_SERVICES='{"genai":[{ "name":"genai","label":"genai","credentials":{"endpoint":{"api_base":"'"$API_BASE"'","name":"test","api_key":"'"$API_KEY"'","config_url":"'"$CONFIG_URL"'"}}}]}'

./mvnw spring-boot:run
```

Then access the UI in the browser at http://localhost:8080

## Run on CF

```shell
./mvnw package -DskipTests
cf push simple-ui --path target/simpleui-0.0.1-SNAPSHOT.jar --no-start
cf set-env simple-ui JBP_CONFIG_OPEN_JDK_JRE '{ jre: { version: 21.+ } }'
cf create-service genai ...
cf bind-service ...
cf restage simple-ui
```

NOTE: when `cf push`'ing the application, VCAP_SERVICES will be created for you.
