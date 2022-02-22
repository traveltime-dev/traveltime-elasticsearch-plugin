FROM docker.elastic.co/elasticsearch/elasticsearch:7.16.0-SNAPSHOT

#COPY build/distributions/traveltime-elasticsearch-plugin_7.16.0_v0.2-SNAPSHOT.zip .

COPY build/distributions/* /distributions/


# RUN bin/elasticsearch-plugin install --batch file://build/distributions/traveltime-elasticsearch-plugin_7.16.0_v0.2-SNAPSHOT.zip
RUN bin/elasticsearch-plugin install --batch file:///distributions/traveltime-elasticsearch-plugin_7.16.0_v0.2-SNAPSHOT.zip
