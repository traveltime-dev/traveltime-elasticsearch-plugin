package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.FetcherSingleton;
import com.traveltime.plugin.elasticsearch.ProtoFetcher;
import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.elasticsearch.SpecialPermission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
public class TraveltimeWeight extends Weight {
   @Getter
   private final TraveltimeSearchQuery ttQuery;

   private final Weight prefilter;

   private final Logger log = LogManager.getLogger();

   private float boost = 1.0f;

   @EqualsAndHashCode.Exclude
   private final ProtoFetcher protoFetcher;

   public TraveltimeWeight(TraveltimeSearchQuery q, Weight prefilter) {
      super(q);
      ttQuery = q;
      this.prefilter = prefilter;
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
   public float getValueForNormalization() throws IOException {
      if (prefilter != null) {
         return prefilter.getValueForNormalization();
      } else {
         return 1.0f;
      }
   }

   @Override
   public void normalize(float norm, float boost) {
      if (prefilter != null) {
         prefilter.normalize(norm, boost);
      }
      this.boost = boost;
   }

   @Override
   public Scorer scorer(LeafReaderContext context) throws IOException {
      val reader = context.reader();
      DocIdSetIterator docs;
      val coords = reader.getSortedNumericDocValues(ttQuery.getParams().getField());

      if (prefilter != null) {
         val preScorer = prefilter.scorer(context);
         if (preScorer == null) return null;
         docs = preScorer.iterator();
      } else {
         docs = DocIdSetIterator.all(reader.maxDoc());
      }
      Bits live = reader.getLiveDocs();

      val valueArray = new ArrayList<Coordinates>();
      val valueSet = new LongOpenHashSet();

      while (docs.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
         if (live != null && !live.get(docs.docID())) continue;
         coords.setDocument(docs.docID());
         if (coords.count() > 0 && valueSet.add(coords.valueAt(0))) {
            valueArray.add(Util.decode(coords.valueAt(0)));
         }
      }

      val pointToTime = new Object2IntOpenHashMap<Coordinates>(valueArray.size());

      val results = protoFetcher.getTimes(
          ttQuery.getParams().getOrigin(),
          valueArray,
          ttQuery.getParams().getLimit(),
          ttQuery.getParams().getMode(),
          ttQuery.getParams().getCountry()
      );

      for (int index = 0; index < results.size(); index++) {
         if(results.get(index) > 0) {
            pointToTime.put(valueArray.get(index), results.get(index).intValue());
         }
      }

      return new TraveltimeScorer(this, pointToTime, DocIdSetIterator.all(reader.maxDoc()), reader.getSortedNumericDocValues(ttQuery.getParams().getField()), boost);
   }
}
