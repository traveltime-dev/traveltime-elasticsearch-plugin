package com.traveltime.plugin.elasticsearch.query;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.net.URI;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
public class TraveltimeSearchQuery extends Query {
   private final TraveltimeQueryParameters params;
   private final Query prefilter;
   private final String output;
   private final URI appUri;
   private final String appId;
   private final String apiKey;

   @Override
   public void visit(QueryVisitor visitor) {
      if (prefilter != null) {
         prefilter.visit(visitor);
      }
      visitor.visitLeaf(this);
   }

   @Override
   public String toString(String field) {
      return String.format("TraveltimeSearchQuery(params = %s, prefilter = %s)", params, prefilter);
   }

   @Override
   public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
      Weight prefilterWeight = prefilter != null ? prefilter.createWeight(searcher, scoreMode, boost) : null;
      return new TraveltimeWeight(this, prefilterWeight, !output.isEmpty(), boost);
   }

   @Override
   public Query rewrite(IndexSearcher reader) throws IOException {
      Query newPrefilter = prefilter != null ? prefilter.rewrite(reader) : null;
      if (newPrefilter == prefilter) {
         return super.rewrite(reader);
      } else {
         return new TraveltimeSearchQuery(params, newPrefilter, output, appUri, appId, apiKey);
      }
   }
}
