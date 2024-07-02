# traveltime-elasticsearch-plugin
Plugin for Elasticsearch that allows users to filter locations using the Traveltime API.

## Repository structure
We maintain separate codebases for each ElasticSearch version between 7.10 and 8.1 (inclusive), and version 5.5. They reside in the appropriate folders (e.g. the plugin for version 7.16 is in the `7.16` folder).

## Installation & configuration 
This is a standard Elasticsearch plugin. As such it can be installed by running `elasticsearch-plugin install https://github.com/traveltime-dev/traveltime-elasticsearch-plugin/releases/download/${PLUGIN_VERSION}/traveltime-elasticsearch-plugin_${PLUGIN_VERSION}_${ES_VERSION}.zip`, where `PLUGIN_VERSION` is the latest plugin version available at https://github.com/traveltime-dev/traveltime-elasticsearch-plugin/releases (e.g. `v0.2.35`), and `ES_VERSION` is the exact version string of the Elasticsearch instance the plugin will be installed to (e.g. `7.17.14`)

To use the plugin you **must** specify the following configuration keys:
 - `traveltime.app.id`: this is you API app id.
 - `traveltime.api.key`: this is the api key that corresponds to the app id.

You may additionally specify the following configuration options:
 - `traveltime.default.mode`: the default transportation mode that will be used if none is specified in the query.
 - `traveltime.default.country`: the default country that  will be used if none is specified in the query.
 - `trabeltime.default.request_type`: the default request type that will be used if none is specified in the query. Defaults to `ONE_TO_MANY`.
 
The following options are available since ES version 7.10 and control the cache that enables returning travel times in the response:
 - `traveltime.cache.size`: how many requests to cache (default: 50)
 - `traveltime.cache.expiry`: how long the travel times will be stored in the cache, in seconds (default: 60)
 - `traveltime.cache.cleanup.interval`: how often a background cleanup task will be run, in seconds (default: 120)

## Querying data
The traveltime query may only be used with fields that are indexed as `geo_point`. The querry accepts the following configuration options:
- `origin`: **[mandatory]** the point from which travel time will be measured. The accepted formats are:
    - object with `lon` and `lat` properties
    - `[lon, lat]` array
    - `"lat,lon"` string
    - geohash
- `field`: **[mandatory]** the document field that will be used as the destination in the Traveltime query.
- `limit`: **[mandatory]** the travel time limit in seconds. Must be non-negative.
- `mode`: Transportation mode used in the search. One of: `pt`, `walking+ferry`, `cycling+ferry`, `driving+ferry`.
Must be set either in the query or in as a default in the config.
- `country`: Country code (e.g. `fr`, `uk`) of the country that the `origin` is in.
May only be set to a country that is listed in the table for "Protocol Buffers API" at https://docs.traveltime.com/api/overview/supported-countries.
Must be set either in the query or as a default in the config.
- `requestType`: type of request made to the api.
Must be one of `ONE_TO_MANY`, `MANY_TO_ONE`
Can be set either in the query or as a default in the config.
Defaults to `ONE_TO_MANY`.
- `prefilter`: Arbitrary Elasticsearch query that will be used to limit which points are sent to the API.
- `output`: **[since 7.10]** name of the field that will hold the travel times in the response

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
