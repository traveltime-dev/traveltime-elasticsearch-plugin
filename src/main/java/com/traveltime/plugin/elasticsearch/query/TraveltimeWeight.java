package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.ProtoFetcher;
import com.traveltime.plugin.elasticsearch.TraveltimeCache;
import com.traveltime.plugin.elasticsearch.util.Util;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.elasticsearch.common.geo.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@EqualsAndHashCode(callSuper = false)
public class TraveltimeWeight extends Weight {
   @Getter
   private final TraveltimeSearchQuery ttQuery;

   private final Weight prefilter;

   private final float boost;

   private final Logger log = LogManager.getLogger();

   @EqualsAndHashCode.Exclude
   private final ProtoFetcher protoFetcher;

   @EqualsAndHashCode.Exclude
   private final ExecutorService executor;

   public TraveltimeWeight(TraveltimeSearchQuery q, Weight prefilter, float boost) {
      super(q);
      ttQuery = q;
      this.prefilter = prefilter;
      this.boost = boost;
      protoFetcher = new ProtoFetcher(q.getAppUri(), q.getAppId(), q.getApiKey());
      executor = Executors.newFixedThreadPool(4);
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

      val valueArray = new ArrayList<GeoPoint>();

      while (finalIterator.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
         valueArray.add(Util.decode(backing.nextValue()));
      }

      val pointToTime = new Object2IntOpenHashMap<GeoPoint>(valueArray.size());

      int batchSize = ttQuery.getBatchSize();
      if (valueArray.size() > batchSize && valueArray.size() % batchSize < batchSize * 0.5) {
         val batchCount = Math.floor(((float) valueArray.size()) / batchSize);
         batchSize = (int) Math.ceil(valueArray.size() / batchCount);
      }

      val futureResults = new ArrayList<Future<List<Integer>>>();

      for (int offset = 0; offset < valueArray.size(); offset += batchSize) {
         val batch = valueArray.subList(offset, Math.min(offset + batchSize, valueArray.size()));
         val batchResult = new FutureTask<>(() -> protoFetcher.getTimes(
             ttQuery.getParams().getOrigin(),
             batch,
             ttQuery.getParams().getLimit(),
             ttQuery.getParams().getMode(),
             ttQuery.getParams().getCountry()
         ));
         executor.submit(batchResult);
         futureResults.add(batchResult);
      }

      int index = 0;
      for (Future<List<Integer>> future : futureResults) {
         try {
            for (Integer time : future.get()) {
               if(time > 0) {
                  pointToTime.put(valueArray.get(index), time.intValue());
               }
               index += 1;
            }
         } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
         }
      }

      TraveltimeCache.INSTANCE.add(ttQuery, pointToTime);

      return new TraveltimeScorer(this, pointToTime, reader.getSortedNumericDocValues(ttQuery.getParams().getField()), boost);
   }

   @Override
   public boolean isCacheable(LeafReaderContext ctx) {
      return true;
   }
}
