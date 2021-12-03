package com.traveltime.plugin.elasticsearch.aggregation;

import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParameters;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.AggregatorFactory;
import org.elasticsearch.search.aggregations.CardinalityUpperBound;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;

public class TraveltimeAggregatorFactory extends AggregatorFactory {
   TraveltimeQueryParameters filter;

   public TraveltimeAggregatorFactory(String name, TraveltimeQueryParameters filter, QueryShardContext queryShardContext, AggregatorFactory parent, AggregatorFactories.Builder subFactoriesBuilder, Map<String, Object> metadata) throws IOException {
      super(name, queryShardContext, parent, subFactoriesBuilder, metadata);
      this.filter = filter;
   }

   @Override
   protected Aggregator createInternal(SearchContext searchContext, Aggregator parent, CardinalityUpperBound cardinality, Map<String, Object> metadata) throws IOException {
      return new TraveltimeAggregator(name, filter, factories, searchContext, parent, cardinality, metadata);
   }
}
