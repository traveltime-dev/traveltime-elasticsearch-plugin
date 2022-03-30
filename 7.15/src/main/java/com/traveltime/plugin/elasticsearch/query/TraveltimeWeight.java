package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.FetcherSingleton;
import com.traveltime.plugin.elasticsearch.ProtoFetcher;
import com.traveltime.plugin.elasticsearch.TraveltimeCache;
import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.elasticsearch.SpecialPermission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
public class TraveltimeWeight extends Weight {
   @Getter
   private final TraveltimeSearchQuery ttQuery;

   private final Weight prefilter;

   private final float boost;

   private final Logger log = LogManager.getLogger();

   @EqualsAndHashCode.Exclude
   private final ProtoFetcher protoFetcher;

   public TraveltimeWeight(TraveltimeSearchQuery q, Weight prefilter, float boost) {
      super(q);
      ttQuery = q;
      this.prefilter = prefilter;
      this.boost = boost;
      protoFetcher = FetcherSingleton.INSTANCE.getFetcher(q.getAppUri(), q.getAppId(), q.getApiKey(), SpecialPermission::new);
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
      val backing = reader.getSortedNumericDocValues(ttQuery.getParams().getField());

      DocIdSetIterator finalIterator;

      if (prefilter != null) {
         val preScorer = prefilter.scorer(context);
         if(preScorer == null) return null;
         val prefilterIterator = preScorer.iterator();
         finalIterator = ConjunctionDISI.intersectIterators(List.of(prefilterIterator, backing));
      } else {
         finalIterator = backing;
      }

      val valueArray = new LongArrayList();
      val decodedArray = new ArrayList<Coordinates>();
      val valueSet = new LongOpenHashSet();

      while (finalIterator.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
         long encodedCoords = backing.nextValue();
         if(valueSet.add(encodedCoords)) {
            valueArray.add(encodedCoords);
            decodedArray.add(Util.decode(encodedCoords));
         }
      }

      val pointToTime = new Long2IntOpenHashMap(valueArray.size());

      val results = protoFetcher.getTimes(
          ttQuery.getParams().getOrigin(),
          decodedArray,
          ttQuery.getParams().getLimit(),
          ttQuery.getParams().getMode(),
          ttQuery.getParams().getCountry()
      );

      for (int index = 0; index < results.size(); index++) {
         if(results.get(index) > 0) {
            pointToTime.put(valueArray.getLong(index), results.get(index).intValue());
         }
      }

      TraveltimeCache.INSTANCE.add(ttQuery.getParams(), pointToTime);

      return new TraveltimeScorer(this, pointToTime, reader.getSortedNumericDocValues(ttQuery.getParams().getField()), boost);
   }

   @Override
   public boolean isCacheable(LeafReaderContext ctx) {
      return true;
   }
}
