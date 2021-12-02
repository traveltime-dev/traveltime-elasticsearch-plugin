package com.traveltime.plugin.elasticsearch.query;

import lombok.AllArgsConstructor;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;

@AllArgsConstructor
public class TraveltimeSearchQuery extends Query {
   TraveltimeQueryParameters params;
   String apiKey;

   @Override
   public String toString(String field) {
      return params.toString();
   }


   public TraveltimeQueryParameters getParams() {
      return this.params;
   }

   public String getApiKey() {
      return this.apiKey;
   }

   @Override
   public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) {
      return new TraveltimeWeight(this);
   }

   public boolean equals(final Object o) {
      if (o == this) return true;
      if (!(o instanceof TraveltimeSearchQuery)) return false;
      final TraveltimeSearchQuery other = (TraveltimeSearchQuery) o;
      if (!other.canEqual(this)) return false;
      if (this.getParams() == null ? other.getParams() != null : !(this.getParams()).equals(other.getParams()))
         return false;
      return this.getApiKey() == null ? other.getApiKey() == null : (this.getApiKey()).equals(other.getApiKey());
   }

   protected boolean canEqual(final Object other) {
      return other instanceof TraveltimeSearchQuery;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + (this.getParams() == null ? 43 : this.getParams().hashCode());
      result = result * PRIME + (this.getApiKey() == null ? 43 : this.getApiKey().hashCode());
      return result;
   }
}
