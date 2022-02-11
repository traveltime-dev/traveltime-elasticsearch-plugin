#!/bin/bash

# install plugin

echo y | bin/elasticsearch-plugin install --batch  file:///plugin/distributions/traveltime-elasticsearch-plugin_7.16.3-SNAPSHOT_7.16.zip

# run elastic

bin/elasticsearch