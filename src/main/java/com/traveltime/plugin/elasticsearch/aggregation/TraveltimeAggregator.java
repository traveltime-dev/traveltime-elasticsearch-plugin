package com.traveltime.plugin.elasticsearch.aggregation;

import com.traveltime.plugin.elasticsearch.ProtoFetcher;
import com.traveltime.plugin.elasticsearch.TraveltimePlugin;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParameters;
import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.plugin.elasticsearch.util.ZipIterable;
import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.BucketsAggregator;
import org.elasticsearch.search.aggregations.bucket.InternalSingleBucketAggregation;
import org.elasticsearch.search.aggregations.support.CoreValuesSourceType;
import org.elasticsearch.search.aggregations.support.ValuesSource;
import org.elasticsearch.search.aggregations.support.ValuesSourceConfig;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.lookup.SourceLookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ExtensionMethod(Util.class)
public class TraveltimeAggregator extends BucketsAggregator {

   private final ValuesSourceConfig score = ValuesSourceConfig.resolve(context().getQueryShardContext(), null, null, new Script("_score"), null, null, null, CoreValuesSourceType.NUMERIC);
   private final ValuesSourceConfig id = ValuesSourceConfig.resolve(context().getQueryShardContext(), null, "_id", null, null, null, null, CoreValuesSourceType.BYTES);
   private final ValuesSourceConfig coords = ValuesSourceConfig.resolve(context().getQueryShardContext(), null, "coords", null, null, null, null, CoreValuesSourceType.BYTES);

   private final String apiKey = TraveltimePlugin.API_KEY.get(context().getQueryShardContext().getIndexSettings().getSettings());

   TraveltimeQueryParameters params;

   private final ArrayList<DocData> docs = new ArrayList<>();

   public TraveltimeAggregator(String name, TraveltimeQueryParameters params, AggregatorFactories factories, SearchContext context, Aggregator parent, CardinalityUpperBound bucketCardinality, Map<String, Object> metadata) throws IOException {
      super(name, factories, context, parent, bucketCardinality, metadata);
      this.params = params;
   }

   void collectDoc(LeafBucketCollector sub, int doc, long bucket, DocData data) throws IOException {
      this.collectBucket(sub, doc, bucket);
      docs.add(data);
   }

   @Override
   protected LeafBucketCollector getLeafCollector(LeafReaderContext ctx, LeafBucketCollector sub) throws IOException {
      val idValues = id.getValuesSource().bytesValues(ctx);
      val sourceLookup = new SourceLookup();
      val scoreValues = ((ValuesSource.Numeric) score.getValuesSource()).doubleValues(ctx);
      val coordValues = ((ValuesSource.GeoPoint) coords.getValuesSource()).geoPointValues(ctx);
      return new TraveltimeCollector(this, sub, idValues, doc -> {
         sourceLookup.setSegmentAndDocument(ctx, doc);
         return sourceLookup;
      }, scoreValues, coordValues);
   }

   @Override
   public InternalAggregation[] buildAggregations(long[] owningBucketOrds) throws IOException {
      return buildAggregationsForSingleBucket(owningBucketOrds, (ord, sub) -> new TraveltimeInternalAggregation(name, apiKey, params, bucketDocCount(ord), sub, metadata(), docs));
   }

   @Override
   public InternalAggregation buildEmptyAggregation() {
      return new TraveltimeInternalAggregation(name, apiKey, params, 0, null, null, docs);
   }

   public static class TraveltimeInternalAggregation extends InternalSingleBucketAggregation {
      @Getter
      final List<DocData> internalDocs;

      final TraveltimeQueryParameters params;

      private final String apiKey;

      private final ProtoFetcher fetcher;

      public TraveltimeInternalAggregation(String name, String apiKey, TraveltimeQueryParameters params, long docCount, InternalAggregations aggregations, Map<String, Object> metadata, List<DocData> internalDocs) {
         super(name, docCount, aggregations, metadata);
         this.internalDocs = internalDocs;
         this.params = params;
         this.apiKey = apiKey;
         fetcher = new ProtoFetcher(apiKey);
      }

      public TraveltimeInternalAggregation(StreamInput in) throws IOException {
         super(in);
         internalDocs = in.readList(DocData::new);
         params = new TraveltimeQueryParameters(in);
         apiKey = in.readString();
         fetcher = new ProtoFetcher(apiKey);
      }

      @Override
      protected void doWriteTo(StreamOutput out) throws IOException {
         super.doWriteTo(out);
         out.writeList(internalDocs);
         params.writeTo(out);
         out.writeString(apiKey);
      }

      @Override
      protected InternalSingleBucketAggregation newAggregation(String name, long docCount, InternalAggregations subAggregations) {
         return new TraveltimeInternalAggregation(name, apiKey, params, docCount, subAggregations, metadata, internalDocs);
      }

      @Override
      public InternalAggregation reduce(List<InternalAggregation> aggregations, ReduceContext reduceContext) {
         List<InternalAggregations> subAggregationsList = new ArrayList<>(aggregations.size());
         ArrayList<DocData> newDocs = new ArrayList<>();
         for (InternalAggregation aggregation : aggregations) {
            assert aggregation.getName().equals(getName());
            subAggregationsList.add(((TraveltimeInternalAggregation) aggregation).getAggregations());
            newDocs.addAll(((TraveltimeInternalAggregation) aggregation).getInternalDocs());
         }
         final InternalAggregations aggs = InternalAggregations.reduce(subAggregationsList, reduceContext);

         val points = newDocs.stream().map(DocData::getCoords).collect(Collectors.toList());
         val times = fetcher.getTimes(params.getOrigin(), points, params.getLimit());
         final List<DocData> filteredDocs =
            new ZipIterable<>(newDocs, times)
               .toStream()
               .filter(dt -> dt._2._2 > 0)
               .map(Tuple2::_1)
               .collect(Collectors.toList());

         return new TraveltimeInternalAggregation(name, apiKey, params, filteredDocs.size(), aggs, metadata, filteredDocs);
      }

      @Override
      public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
         builder.array("hits", internalDocs.toArray());
         builder.field(CommonFields.DOC_COUNT.getPreferredName(), getDocCount());
         getAggregations().toXContentInternal(builder, params);
         return builder;
      }

      @Override
      public String getWriteableName() {
         return TraveltimeAggregatorBuilder.NAME;
      }
   }

   @Data
   @AllArgsConstructor
   static class DocData implements ToXContent, Writeable {
      String id;
      double score;
      GeoPoint coords;
      Map<String, Object> source;

      public DocData(StreamInput in) throws IOException {
         id = in.readString();
         score = in.readDouble();
         coords = in.readGeoPoint();
         source = in.readMap();
      }

      @Override
      public void writeTo(StreamOutput out) throws IOException {
         out.writeString(id);
         out.writeDouble(score);
         out.writeGeoPoint(coords);
         out.writeMap(source);
      }

      @Override
      public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
         builder.startObject();
         builder.field("_id", id);
         builder.field("_score", score);
         builder.field("_coords", coords);
         builder.field("_source", source);
         return builder.endObject();
      }
   }
}
