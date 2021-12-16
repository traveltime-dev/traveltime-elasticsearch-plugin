package com.traveltime.plugin.elasticsearch.query;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;

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
      return "TraveltimeSearchQuery(params = " + params.toString() + ")";
   }

   @Override
   public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
      Weight prefilterWeight = null;
      if(prefilter != null) prefilterWeight = prefilter.createWeight(searcher, scoreMode, boost);
      return new TraveltimeWeight(this, prefilterWeight);
   }
}
