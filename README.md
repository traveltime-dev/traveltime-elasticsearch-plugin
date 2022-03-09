# traveltime-elasticsearch-plugin
Plugin for Elasticsearch that allows users to filter locations using the Traveltime API.

## Repository structure
We maintain separate codebases for each ElasticSearch version between 7.10 and 8.1 (inclusive), and version 5.5. They reside in the appropriate folders (e.g. the plugin for version 7.16 is in the `7.16` folder).

## Installation & configuration 
This is a standard Elasticsearch plugin. As such it can be installed by running `elasticsearch-plugin install https://github.com/traveltime-dev/traveltime-elasticsearch-plugin/releases/download/v0.2.25/traveltime-elasticsearch-plugin_v0.2.25_8.1.0.zip`

To use the plugin you **must** specify the following configuration keys:
 - `traveltime.app.id`: this is you API app id.
 - `traveltime.api.key`: this is the api key that corresponds to the app id.

You may additionally specify the following configuration options:
 - `traveltime.default.mode`: the default transportation mode that will be used if none is specified in the query.
 - `traveltime.default.country`: the default country that  will be used if none is specified in the query.
 
## Querying data
The traveltime query may only be used with fields that are indexed as `geo_point`. The querry accepts the following configuration options:
- `origin`: **[mandatory]** the point from which travel time will be measured. The accepted formats are:
    - object with `lon` and `lat` properties
    - `[lon, lat]` array
    - `"lat,lon"` string
    - geohash
- `field`: **[mandatory]** the document field that will be used as the destination in the Traveltime query.
- `limit`: **[mandatory]** the travel time limit in seconds. Must be non-negative.
- `mode`: Transportation mode used in the search. One of: `pt`, `walking+ferry`, `cycling+ferry`, `driving+ferry`. Must be set either in the query or in as a default in the config.
- `country`: Country that the `origin` is in. Currently may only be one of: `uk`, `nl`, `at`, `be`, `de`, `fr`, `ie`, `lt`. Must be set either in the query or in as a default in the config.
- `prefilter`: Arbitrary Elasticsearch query that will be used to limit which points are sent to the API.

###Examples

```json
{
  "query": {
    "traveltime": {
      "limit": 900,
      "field": "coords",
      "origin": {
        "lat": 51.509865,
        "lon": -0.118092
      }
    }
  }
}    
```

```json
{
  "traveltime": {
    "limit": 7200,
    "field": "coords",
    "origin": "gcpvj3448qb1",
    "mode": "pt",
    "country": "uk",
    "prefilter": {
      "bool": {
        "filter": [
          {
            "range": {
              "bedrooms": {
                "gte": 3
              }
            }
          }
        ]
      }
    }
  }
}
```
