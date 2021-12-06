package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.TraveltimePlugin;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.Setter;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;

import java.io.IOException;
import java.util.Objects;

@Setter
public class TraveltimeQueryBuilder extends AbstractQueryBuilder<TraveltimeQueryBuilder> {
   String field;
   GeoPoint origin;
   int limit;
   Transportation mode;
   Country country;

   public TraveltimeQueryBuilder() {
   }

   public TraveltimeQueryBuilder(StreamInput in) throws IOException {
      super(in);
      field = in.readString();
      origin = in.readGeoPoint();
      limit = in.readInt();
      mode = in.readEnum(Transportation.class);
      country = in.readEnum(Country.class);
   }

   @Override
   protected void doWriteTo(StreamOutput out) throws IOException {
      out.writeString(field);
      out.writeGeoPoint(origin);
      out.writeInt(limit);
      out.writeEnum(mode);
      out.writeEnum(country);
   }

   @Override
   protected void doXContent(XContentBuilder builder, Params params) {

   }

   public TraveltimeQueryParameters params() {
      return new TraveltimeQueryParameters(field, origin, limit, mode, country);
   }

   @Override
   protected Query doToQuery(QueryShardContext context) {
      String appId = TraveltimePlugin.APP_ID.get(context.getIndexSettings().getSettings());
      String apiKey = TraveltimePlugin.API_KEY.get(context.getIndexSettings().getSettings());
      Transportation defaultMode = TraveltimePlugin.DEFAULT_MODE.get(context.getIndexSettings().getSettings());
      TraveltimeQueryParameters params = params();
      if(params.getMode() == null) {
         params.setMode(defaultMode);
      }
      return new TraveltimeSearchQuery(params, appId, apiKey);
   }

   @Override
   protected boolean doEquals(TraveltimeQueryBuilder other) {
      if (!Objects.equals(this.field, other.field)) return false;
      if (!Objects.equals(this.origin, other.origin)) return false;
      if (!Objects.equals(this.mode, other.mode)) return false;
      if (!Objects.equals(this.country, other.country)) return false;
      return this.limit == other.limit;
   }

   @Override
   protected int doHashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.field.hashCode();
      result = result * PRIME + this.origin.hashCode();
      result = result * PRIME + this.mode.hashCode();
      result = result * PRIME + this.country.hashCode();
      result = result * PRIME + this.limit;
      return result;
   }

   @Override
   public String getWriteableName() {
      return TraveltimeQueryParser.NAME;
   }
}
