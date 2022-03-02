#!/usr/bin/env bash

set -ex

trap "docker stop $IMAGE_NAME; exit 1" EXIT

docker run -d \
  -e "discovery.type=single-node" \
  -e "traveltime.app.id=id" \
  -e "traveltime.api.key=key" \
  -e "xpack.security.enabled=false" \
  --rm \
  --name $IMAGE_NAME \
  $IMAGE_NAME

docker exec $IMAGE_NAME ./wait-for-startup.sh
docker exec $IMAGE_NAME ./load-data.sh



docker stop $IMAGE_NAME
trap EXIT
