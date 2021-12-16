package com.traveltime.plugin.elasticsearch.query;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.net.URI;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
public class TraveltimeSearchQuery extends Query {
   TraveltimeQueryParameters params;
   Query prefilter;
   URI appUri;
   String appId;
   String apiKey;

   @Override
   public String toString(String field) {
      return String.format("TraveltimeSearchQuery(params = %s, prefilter = %s)", params, prefilter);
   }

   @Override
   public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
      Weight prefilterWeight = null;
      if(prefilter != null) prefilterWeight = prefilter.createWeight(searcher, scoreMode, boost);
      return new TraveltimeWeight(this, prefilterWeight);
   }

   @Override
   public Query rewrite(IndexReader reader) throws IOException {
      if(prefilter != null) prefilter = prefilter.rewrite(reader);
      return this;
   }
}
