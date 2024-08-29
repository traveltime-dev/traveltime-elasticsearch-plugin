package com.traveltime.plugin.elasticsearch.query;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;

public class TraveltimeScorer extends Scorer {
  protected final TraveltimeWeight weight;
  private final Long2IntMap pointToTime;
  private final TraveltimeFilteredDocs docs;
  private final float boost;

  @RequiredArgsConstructor
  private class TraveltimeFilteredDocs extends DocIdSetIterator {
    private final TraveltimeWeight.FilteredIterator backing;

    @Getter private long currentValue = 0;
    private boolean currentValueDirty = true;

    private void invalidateCurrentValue() {
      currentValueDirty = true;
    }

    private void advanceValue() throws IOException {
      if (currentValueDirty) {
        currentValue = backing.nextValue();
        currentValueDirty = false;
      }
    }

    public long nextValue() throws IOException {
      advanceValue();
      return currentValue;
    }

    @Override
    public int docID() {
      return backing.docID();
    }

    @Override
    public int nextDoc() throws IOException {
      int id = backing.nextDoc();
      invalidateCurrentValue();
      while (id != DocIdSetIterator.NO_MORE_DOCS && !pointToTime.containsKey(nextValue())) {
        id = backing.nextDoc();
        invalidateCurrentValue();
      }
      return id;
    }

    @Override
    public int advance(int target) throws IOException {
      int id = backing.advance(target);
      invalidateCurrentValue();
      if (id != DocIdSetIterator.NO_MORE_DOCS && !pointToTime.containsKey(nextValue())) {
        id = nextDoc();
      }
      return id;
    }

    @Override
    public long cost() {
      return backing.cost() * 1000;
    }
  }

  public TraveltimeScorer(
      TraveltimeWeight w,
      Long2IntMap coordToTime,
      TraveltimeWeight.FilteredIterator docs,
      float boost) {
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
    int tt = pointToTime.getOrDefault(docs.nextValue(), limit + 1);
    return (boost * (limit - tt + 1)) / (limit + 1);
  }

  @Override
  public int docID() {
    return docs.docID();
  }
}
