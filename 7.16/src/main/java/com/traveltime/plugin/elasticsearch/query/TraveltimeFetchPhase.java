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
      private final List<TraveltimeQueryParameters> paramList = new ArrayList<>();

      @Override
      public void visitLeaf(Query query) {
         if (query instanceof TraveltimeSearchQuery) {
            paramList.add(((TraveltimeSearchQuery) query).getParams());
         }
      }

      public TraveltimeQueryParameters getUniqueParam() {
         if (paramList.size() == 1) return paramList.get(0);
         else return null;
      }
   }

   @Override
   public FetchSubPhaseProcessor getProcessor(FetchContext fetchContext) {
      Query query = fetchContext.query();
      val finder = new ParamFinder();
      query.visit(finder);
      TraveltimeQueryParameters params = finder.getUniqueParam();
      if (params == null) return null;

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

            if (tt > 0) {
               hitContext.hit().setDocumentField("traveltime", new DocumentField("traveltime", List.of(tt)));
            }
         }
      };
   }
}
