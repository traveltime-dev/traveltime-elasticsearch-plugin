package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.ProtoFetcher;
import com.traveltime.plugin.elasticsearch.util.Util;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.elasticsearch.common.geo.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ExtensionMethod(Util.class)
@EqualsAndHashCode(callSuper = false)
public class TraveltimeWeight extends Weight {
   private final TraveltimeSearchQuery ttQuery;

   @EqualsAndHashCode.Exclude
   private final ProtoFetcher protoFetcher;

   public TraveltimeWeight(TraveltimeSearchQuery q) {
      super(q);
      ttQuery = q;
      protoFetcher = new ProtoFetcher(q.apiKey);
   }

   @Override
   public void extractTerms(Set<Term> terms) {
   }

   @Override
   public Explanation explain(LeafReaderContext context, int doc) {
      return Explanation.noMatch("Cannot provide explanation for traveltime matches");
   }

   @Override
   public Scorer scorer(LeafReaderContext context) throws IOException {
      val reader = context.reader();

      val backing = reader.getSortedNumericDocValues(ttQuery.getParams().field);

      val valueArray = new ArrayList<GeoPoint>();

      while (backing.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
         valueArray.add(Util.decode(backing.nextValue()));
      }

      final Map<GeoPoint, Integer> pointToTime = valueArray
         .stream()
         .distinct()
         .collect(Collectors.toList())
         .grouped(100_000)
         .stream()
         .flatMap(locs -> protoFetcher.getTimes(ttQuery.getParams().origin, locs, ttQuery.getParams().limit).toStream())
         .filter(kv -> kv._2 > 0)
         .toMap();

      return new TraveltimeScorer(this, pointToTime, reader.getSortedNumericDocValues(ttQuery.getParams().field));
   }

   @Override
   public boolean isCacheable(LeafReaderContext ctx) {
      return true;
   }
}
