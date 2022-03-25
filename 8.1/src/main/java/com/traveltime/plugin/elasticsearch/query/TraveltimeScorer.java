package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.sdk.dto.common.Coordinates;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;

import java.io.IOException;
import java.util.Map;

public class TraveltimeScorer extends Scorer {
   protected final TraveltimeWeight weight;
   private final Map<Coordinates, Integer> pointToTime;
   private final TraveltimeFilteredDocs docs;
   private final float boost;

   @RequiredArgsConstructor
   private class TraveltimeFilteredDocs extends SortedNumericDocValues {
      private final SortedNumericDocValues backing;

      @Getter
      private long currentValue = 0;
      private boolean currentValueDirty = true;
      private void invalidateCurrentValue() {
         currentValueDirty = true;
      }
      private void advanceValue() throws IOException {
         if(currentValueDirty) {
            currentValue = backing.nextValue();
            currentValueDirty = false;
         }
      }

      @Override
      public long nextValue() throws IOException {
         advanceValue();
         return currentValue;
      }

      @Override
      public int docValueCount() {
         return 1;
      }

      @Override
      public boolean advanceExact(int target) throws IOException {
         invalidateCurrentValue();
         return (target == DocIdSetIterator.NO_MORE_DOCS && backing.advanceExact(target)) ||
                 backing.advanceExact(target) && pointToTime.containsKey(Util.decode(nextValue()));
      }

      @Override
      public int docID() {
         return backing.docID();
      }

      @Override
      public int nextDoc() throws IOException {
         int id = backing.nextDoc();
         invalidateCurrentValue();
         while (id != DocIdSetIterator.NO_MORE_DOCS && !pointToTime.containsKey(Util.decode(nextValue()))) {
            id = backing.nextDoc();
            invalidateCurrentValue();
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

   public TraveltimeScorer(TraveltimeWeight w, Map<Coordinates, Integer> coordToTime, SortedNumericDocValues docs, float boost) {
      super(w);
      this.weight = w;
      this.pointToTime = coordToTime;
      this.docs = new TraveltimeFilteredDocs(docs);
      this.boost = boost;
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
      return (boost * (limit - tt + 1)) / (limit + 1);

   }

   @Override
   public int docID() {
      return docs.docID();
   }
}
