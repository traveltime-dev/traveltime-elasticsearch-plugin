#!/usr/bin/env bash

set -ex

trap "docker stop $IMAGE_NAME; exit 1" EXIT

docker run -d \
  -e "discovery.type=single-node" \
  -e "traveltime.api.uri=http://localhost/" \
  -e "traveltime.app.id=id" \
  -e "traveltime.api.key=key" \
  -e "xpack.security.enabled=false" \
  --rm \
  --name $IMAGE_NAME \
  $IMAGE_NAME

docker exec $IMAGE_NAME ./mock-proto-server --port 80 &
docker exec $IMAGE_NAME ./wait-for-startup.sh
docker exec $IMAGE_NAME ./load-data.sh
sleep 5

docker exec $IMAGE_NAME curl -s --fail -H 'Content-Type: application/json' -d "$(cat test_case_1.json)" "localhost:9200/london/_search" \
  | jq '.hits.hits[]._source.id' > actual_results_1

docker exec $IMAGE_NAME curl -s --fail -H 'Content-Type: application/json' -d "$(cat test_case_2.json)" "localhost:9200/london/_search" \
  | jq '.hits.hits[]._source.id' > actual_results_2

docker exec $IMAGE_NAME curl -s --fail -H 'Content-Type: application/json' -d "$(cat test_case_3.json)" "localhost:9200/london/_search" \
  | jq '.hits.hits[]| has("fields")' > actual_results_3


diff actual_results_1 expected_results_1
diff actual_results_2 expected_results_2
diff actual_results_3 expected_results_3

docker stop $IMAGE_NAME
trap EXIT
