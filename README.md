# traveltime-elasticsearch-plugin
Plugin for Elasticsearch that allows users to filter locations using the Traveltime API.

## Repository structure
We maintain separate codebases for each ElasticSearch version between 7.10 and 8.1 (inclusive), and version 5.5. They reside in the appropriate folders (e.g. the plugin for version 7.16 is in the `7.16` folder).

## Installation
This is a standard Elasticsearch plugin. As such it can be installed by running `elasticsearch-plugin install https://github.com/traveltime-dev/traveltime-elasticsearch-plugin/releases/download/${PLUGIN_VERSION}/traveltime-elasticsearch-plugin_${PLUGIN_VERSION}_${ES_VERSION}.zip`,
where `PLUGIN_VERSION` is the latest plugin version available at https://github.com/traveltime-dev/traveltime-elasticsearch-plugin/releases (e.g. `v0.2.35`),
and `ES_VERSION` is the exact version string of the Elasticsearch instance the plugin will be installed to (e.g. `7.17.14`).

For example, to install `traveltime-elasticsearch-plugin` you could run: `sudo bin/elasticsearch-plugin install https://github.com/traveltime-dev/traveltime-elasticsearch-plugin/releases/download/v0.2.38/traveltime-elasticsearch-plugin_v0.2.38_8.17.0.zip`

## Configuration
In order for plugin to work, you will need to modify `elasticsearch.yml`, you can find more about ElasticSearch configuration here: https://www.elastic.co/guide/en/elasticsearch/reference/current/settings.html

You **must** specify the following configuration keys for the authentication:
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
The traveltime query may only be used with fields that are indexed as `geo_point`. The query accepts the following configuration options:
- `origin`: **[mandatory]** the point from which travel time will be measured. The accepted formats are:
    - object with `lon` and `lat` properties
    - `[lon, lat]` array
    - `"lat,lon"` string
    - geohash
- `field`: **[mandatory]** name of the field (must be `geo_point` type) that will be used as the destination in the TravelTime query.
- `limit`: **[mandatory]** the travel time limit in seconds (must be non-negative). If time to travel from origin to destination would take more than limit, such result would be excluded.
If the destination is unreachable, the result will be excluded as well.
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
- `distanceOutput` **[since 7.10]**: name of the field that will hold the travel distances in the response. Cannot be used with `mode` set to `pt`.
- `boost` **[optional]**: Floating point number used to multiply the relevance score of matching documents. This value cannot be negative. Defaults to `1.0`.

### Examples

TravelTime plugin could be used as an alternative to ElasticSearch geo related queries, for example,
instead of `geo_distance` (https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-geo-distance-query.html) it is possible to filter out results not by distance, but by travel time.

Let's say we want to filter out universities in the United Kingdom which can be reached by driving up to 60 minutes from Tower of London.

Data:
```
PUT universities
{
  "mappings": {
    "properties": {
      "name":{
        "type": "text"
      },
      "location":{
        "type": "geo_point"
      }
    }
  }
}
 
PUT universities/_doc/1
{
  "name":"London School of Economics and Political Science",
  "location":[-0.116422, 51.5145976]
}
 
PUT universities/_doc/2
{
  "name":"Imperial College London",
  "location":[-0.1756407, 51.4989595]
}
 
PUT universities/_doc/3
{
  "name":"University of Oxford",
  "location":[-1.2556685, 51.7587075]
}
 
PUT universities/_doc/4
{
  "name":"University of Cambridge",
  "location":[0.092005, 52.2109456]
}
```

Query:
```
GET universities/_search
{
  "query": {
    "traveltime": {
      "limit": 3600,
      "mode": "driving+ferry",
      "country": "uk",
      "field": "location",
      "origin": {
        "lat": 51.508217,
        "lon": -0.0761879
      },
      "output": "travel_time",
      "distanceOutput": "distance"
    }
  }
}
```

Would result in:
```
{
  "took": 293,
  "timed_out": false,
  "_shards": {
    "total": 1,
    "successful": 1,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": {
      "value": 2,
      "relation": "eq"
    },
    "max_score": 0.6925854,
    "hits": [
      {
        "_index": "universities",
        "_id": "1",
        "_score": 0.6925854,
        "_source": {
          "name": "London School of Economics and Political Science",
          "location": [
            -0.116422,
            51.5145976
          ]
        },
        "fields": {
          "distance": [
            3989
          ],
          "travel_time": [
            1107
          ]
        }
      },
      {
        "_index": "universities",
        "_id": "2",
        "_score": 0.32129964,
        "_source": {
          "name": "Imperial College London",
          "location": [
            -0.1756407,
            51.4989595
          ]
        },
        "fields": {
          "distance": [
            8957
          ],
          "travel_time": [
            2444
          ]
        }
      }
    ]
  }
}
```
