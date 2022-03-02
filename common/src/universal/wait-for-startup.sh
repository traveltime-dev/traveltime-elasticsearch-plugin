#!/usr/bin/env bash

START=$(date +%s)

until $(curl --output /dev/null --silent --fail http://127.0.0.1:9200/_cat/health); do
    NOW=$(date +%s)
    if [ $(expr ${NOW} - ${START}) -ge 60 ];then
      echo "Timeout"
      exit 1
    fi

    printf '.'
    sleep 1
done
