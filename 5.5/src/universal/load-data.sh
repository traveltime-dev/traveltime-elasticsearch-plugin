#!/bin/bash

curl --fail -X PUT "localhost:9200/london" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "location": {
      "properties": {
        "coords": { "type": "geo_point" }
      }
    }
  }
}
'
curl --fail -X POST "localhost:9200/london/location/_bulk" -H 'Content-Type: application/x-ndjson' --data-binary @part0 -o /dev/null
