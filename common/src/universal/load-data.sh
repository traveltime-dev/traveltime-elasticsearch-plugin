#!/bin/bash

curl -X PUT "localhost:9200/london" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "coords": { "type": "geo_point" }
    }
  }
}
'
curl -X POST "localhost:9200/london/_bulk" -H 'Content-Type: application/x-ndjson' --data-binary @part0 -o /dev/null
