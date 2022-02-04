package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.util.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.elasticsearch.common.geo.GeoPoint;

import java.io.IOException;
import java.util.Map;

public class TraveltimeScorer extends Scorer {
   protected final TraveltimeWeight weight;
   private final Map<GeoPoint, Integer> pointToTime;
   private final TraveltimeFilteredDocs docs;
   private final float boost;

   @AllArgsConstructor
   @Getter
   private class TraveltimeFilteredDocs extends DocIdSetIterator {
      private final DocIdSetIterator backing;
      SortedNumericDocValues backingCoords;

      @Override
      public int docID() {
         return backing.docID();
      }

      @Override
      public int nextDoc() throws IOException {
         int id = backing.nextDoc();
         while (id != DocIdSetIterator.NO_MORE_DOCS) {
            backingCoords.setDocument(id);
            if (backingCoords.count() > 0 && pointToTime.containsKey(Util.decode(backingCoords.valueAt(0)))) {
               return id;
            }
            id = backing.nextDoc();
         }
         return id;
      }

      @Override
      public int advance(int target) throws IOException {
         int id = backing.advance(target);
         backingCoords.setDocument(id);
         if (backingCoords.count() > 0 && pointToTime.containsKey(Util.decode(backingCoords.valueAt(0)))) {
            return id;
         } else {
            return nextDoc();
         }
      }

      @Override
      public long cost() {
         return backing.cost() * 100;
      }
   }

   public TraveltimeScorer(TraveltimeWeight w, Map<GeoPoint, Integer> coordToTime, DocIdSetIterator docs, SortedNumericDocValues coords, float boost) {
      super(w);
      this.weight = w;
      this.pointToTime = coordToTime;
      this.docs = new TraveltimeFilteredDocs(docs, coords);
      this.boost = boost;
   }

   @Override
   public DocIdSetIterator iterator() {
      return docs;
   }

   @Override
   public float score() {
      int limit = weight.getTtQuery().getParams().getLimit();
      int tt = pointToTime.getOrDefault(Util.decode(docs.backingCoords.valueAt(0)), limit + 1);
      return boost * (limit - tt + 1) / (limit + 1);
   }

   @Override
   public int freq() {
      return 0;
   }

   @Override
   public int docID() {
      return docs.docID();
   }
}
