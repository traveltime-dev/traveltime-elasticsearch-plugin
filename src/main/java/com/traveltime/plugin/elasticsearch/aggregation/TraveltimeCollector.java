package com.traveltime.plugin.elasticsearch.aggregation;

import lombok.AllArgsConstructor;
import lombok.val;
import org.apache.lucene.search.Scorable;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.lucene.ScorerAware;
import org.elasticsearch.index.fielddata.MultiGeoPointValues;
import org.elasticsearch.index.fielddata.SortedBinaryDocValues;
import org.elasticsearch.index.fielddata.SortedNumericDoubleValues;
import org.elasticsearch.search.aggregations.LeafBucketCollector;
import org.elasticsearch.search.lookup.SourceLookup;

import java.io.IOException;
import java.util.function.Function;

@AllArgsConstructor
public class TraveltimeCollector extends LeafBucketCollector {
   TraveltimeAggregator parent;
   LeafBucketCollector sub;
   SortedBinaryDocValues id;
   Function<Integer, SourceLookup> source;
   SortedNumericDoubleValues scores;
   MultiGeoPointValues coords;


   @Override
   public void collect(int doc, long owningBucketOrd) throws IOException {
      id.advanceExact(doc);
      scores.advanceExact(doc);
      coords.advanceExact(doc);

      val _source = source.apply(doc);

      val data = new TraveltimeAggregator.DocData(id.nextValue().utf8ToString(), scores.nextValue(), new GeoPoint(coords.nextValue()), _source.loadSourceIfNeeded());
      parent.collectDoc(sub, doc, owningBucketOrd, data);
   }

   @Override
   public void setScorer(Scorable scorer) throws IOException {
      super.setScorer(scorer);
      ((ScorerAware)scores).setScorer(scorer);
   }
}
