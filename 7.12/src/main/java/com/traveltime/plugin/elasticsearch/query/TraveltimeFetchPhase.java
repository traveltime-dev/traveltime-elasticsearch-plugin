package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.TraveltimeCache;
import lombok.val;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.fetch.FetchContext;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.FetchSubPhaseProcessor;
import org.elasticsearch.search.fetch.subphase.FieldAndFormat;
import org.elasticsearch.search.fetch.subphase.FieldFetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TraveltimeFetchPhase implements FetchSubPhase {

   private static class ParamFinder extends QueryVisitor {
      private final List<TraveltimeSearchQuery> paramList = new ArrayList<>();

      @Override
      public void visitLeaf(Query query) {
         if (query instanceof TraveltimeSearchQuery) {
            if (!((TraveltimeSearchQuery) query).getOutput().isEmpty()) {
               paramList.add(((TraveltimeSearchQuery) query));
            }
         }
      }

      public TraveltimeSearchQuery getQuery() {
         if (paramList.size() == 1) return paramList.get(0);
         else return null;
      }
   }

   @Override
   public FetchSubPhaseProcessor getProcessor(FetchContext fetchContext) {
      Query query = fetchContext.query();
      val finder = new ParamFinder();
      query.visit(finder);
      TraveltimeSearchQuery traveltimeQuery = finder.getQuery();
      if (traveltimeQuery == null) return null;
      TraveltimeQueryParameters params = traveltimeQuery.getParams();
      final String output = traveltimeQuery.getOutput();

      FieldFetcher fieldFetcher = FieldFetcher.create(fetchContext.getSearchExecutionContext(), List.of(new FieldAndFormat(params.getField(), null)));

      return new FetchSubPhaseProcessor() {

         @Override
         public void setNextReader(LeafReaderContext readerContext) {
            fieldFetcher.setNextReader(readerContext);
         }

         @Override
         public void process(HitContext hitContext) throws IOException {
            val docValues = hitContext.reader().getSortedNumericDocValues(params.getField());
            docValues.advance(hitContext.docId());
            Integer tt = TraveltimeCache.INSTANCE.get(params, docValues.nextValue());

            if (tt >= 0) {
               hitContext.hit().setDocumentField(output, new DocumentField(output, List.of(tt)));
            }
         }
      };
   }
}
