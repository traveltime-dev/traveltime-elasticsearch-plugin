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



docker exec $IMAGE_NAME curl -X POST -H 'Content-Type: application/json' -d'{"query": {"traveltime": {"limit": 6200,"field": "coords","origin": {"lat": 51.509865,"lon": -0.118092},"mode": "pt","country": "uk"}},"_source": false}' "localhost:9200/london/_search"

docker stop $IMAGE_NAME
trap EXIT
