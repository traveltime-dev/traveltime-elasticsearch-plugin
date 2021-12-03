package com.traveltime.plugin.elasticsearch.aggregation;

import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParameters;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParser;
import lombok.val;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ContextParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.AggregatorFactory;

import java.io.IOException;
import java.util.Map;

public class TraveltimeAggregatorBuilder extends AbstractAggregationBuilder<TraveltimeAggregatorBuilder> {
   TraveltimeQueryParameters filter;

   public TraveltimeAggregatorBuilder(String name, TraveltimeQueryParameters filter) {
      super(name);
      this.filter = filter;
   }

   public TraveltimeAggregatorBuilder(StreamInput in) throws IOException {
      super(in);
      filter = new TraveltimeQueryParameters(in);
   }

   @Override
   protected void doWriteTo(StreamOutput out) throws IOException {
      filter.writeTo(out);
   }

   @Override
   protected AggregatorFactory doBuild(QueryShardContext queryShardContext, AggregatorFactory parent, AggregatorFactories.Builder subfactoriesBuilder) throws IOException {
      return new TraveltimeAggregatorFactory(
         name,
         filter,
         queryShardContext,
         parent,
         subfactoriesBuilder,
         metadata
      );
   }

   @Override
   protected XContentBuilder internalXContent(XContentBuilder builder, Params params) {
      return null;
   }

   @Override
   protected AggregationBuilder shallowCopy(AggregatorFactories.Builder factoriesBuilder, Map<String, Object> metadata) {
      val copy = new TraveltimeAggregatorBuilder(name, filter);
      copy.factoriesBuilder = factoriesBuilder;
      copy.metadata = metadata;
      return copy;
   }

   @Override
   public BucketCardinality bucketCardinality() {
      return BucketCardinality.ONE;
   }

   @Override
   public String getType() {
      return TraveltimeAggregatorBuilder.NAME;
   }

   public static String NAME = "traveltime";

   public static ContextParser<String, TraveltimeAggregatorBuilder> PARSER = (p, c) -> new TraveltimeAggregatorBuilder(c, new TraveltimeQueryParser().fromXContent(p).params());
}
