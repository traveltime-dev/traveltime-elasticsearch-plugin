package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.util.Util;
import lombok.AllArgsConstructor;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.elasticsearch.common.geo.GeoPoint;

import java.io.IOException;
import java.util.Map;

public class TraveltimeScorer extends Scorer {
   protected final TraveltimeWeight weight;
   Map<GeoPoint, Integer> pointToTime;
   SortedNumericDocValues docs;

   @AllArgsConstructor
   private class TraveltimeFilteredDocs extends SortedNumericDocValues {
      SortedNumericDocValues backing;

      @Override
      public long nextValue() throws IOException {
         return backing.nextValue();
      }

      @Override
      public int docValueCount() {
         return 1;
      }

      @Override
      public boolean advanceExact(int target) throws IOException {
         return backing.advanceExact(target) && pointToTime.containsKey(Util.decode(nextValue()));
      }

      @Override
      public int docID() {
         return backing.docID();
      }

      @Override
      public int nextDoc() throws IOException {
         int id = backing.nextDoc();
         while (id != DocIdSetIterator.NO_MORE_DOCS && !pointToTime.containsKey(Util.decode(nextValue()))) {
            id = backing.nextDoc();
         }
         return id;
      }

      @Override
      public int advance(int target) throws IOException {
         if (advanceExact(target)) {
            return target;
         } else {
            return nextDoc();
         }
      }

      @Override
      public long cost() {
         return backing.cost() * 1000;
      }
   }

   public TraveltimeScorer(TraveltimeWeight w, Map<GeoPoint, Integer> coordToTime, SortedNumericDocValues docs) {
      super(w);
      this.weight = w;
      this.pointToTime = coordToTime;
      this.docs = new TraveltimeFilteredDocs(docs);
   }

   @Override
   public DocIdSetIterator iterator() {
      return docs;
   }

   @Override
   public float getMaxScore(int upTo) {
      return 1;
   }

   @Override
   public float score() throws IOException {
      int limit = weight.getTtQuery().getParams().getLimit();
      int tt = pointToTime.getOrDefault(Util.decode(docs.nextValue()), limit + 1);
      return ((float)(limit - tt + 1)) / (limit + 1);

   }

   @Override
   public int docID() {
      return docs.docID();
   }
}
