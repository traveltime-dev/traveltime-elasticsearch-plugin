#!/bin/bash

# install plugin
#TODO in case of bad path it does a rollback and returns success - check explicitly to fail step early
bin/elasticsearch-plugin install --batch  file:///plugin/distributions/traveltime-elasticsearch-plugin_7.16.0_v0.2-SNAPSHOT.zip

# run elastic

bin/elasticsearch