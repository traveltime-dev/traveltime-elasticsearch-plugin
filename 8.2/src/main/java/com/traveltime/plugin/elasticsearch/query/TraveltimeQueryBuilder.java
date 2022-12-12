package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.TraveltimePlugin;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.NonNull;
import lombok.Setter;
import org.apache.lucene.search.Query;
import org.elasticsearch.Version;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.GeoUtils;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.mapper.GeoPointFieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

@Setter
public class TraveltimeQueryBuilder extends AbstractQueryBuilder<TraveltimeQueryBuilder> {
   @NonNull
   private String field;
   @NonNull
   private GeoPoint origin;
   private int limit;
   private Transportation mode;
   private Country country;
   private QueryBuilder prefilter;
   @NonNull
   private String output = "";

   public TraveltimeQueryBuilder() {
   }

   public TraveltimeQueryBuilder(StreamInput in) throws IOException {
      super(in);
      field = in.readString();
      origin = in.readGeoPoint();
      limit = in.readInt();
      mode = in.readOptionalEnum(Transportation.class);
      country = in.readOptionalEnum(Country.class);
      prefilter = in.readOptionalNamedWriteable(QueryBuilder.class);
      output = in.readString();
   }

   @Override
   protected void doWriteTo(StreamOutput out) throws IOException {
      out.writeString(field);
      out.writeGeoPoint(origin);
      out.writeInt(limit);
      out.writeOptionalEnum(mode);
      out.writeOptionalEnum(country);
      out.writeOptionalNamedWriteable(prefilter);
      out.writeString(output);
   }

   @Override
   protected void doXContent(XContentBuilder builder, Params params) throws IOException {
      builder.field("field", field);
      builder.field("origin", origin);
      builder.field("limit", limit);
      builder.field("mode", mode == null ? null : mode.getValue());
      builder.field("country", country == null ? null : country.getValue());
      builder.field("prefilter", prefilter);
      builder.field("output", output);
   }

   @Override
   protected QueryBuilder doRewrite(QueryRewriteContext queryRewriteContext) throws IOException {
      if (this.prefilter != null) this.prefilter = this.prefilter.rewrite(queryRewriteContext);
      return super.doRewrite(queryRewriteContext);
   }

   @Override
   protected Query doToQuery(SearchExecutionContext context) throws IOException {
      MappedFieldType originMapping = context.getFieldType(field);
      if (!(originMapping instanceof GeoPointFieldMapper.GeoPointFieldType)) {
         throw new QueryShardException(context, "field [" + field + "] is not a geo_point field");
      }

      GeoUtils.normalizePoint(origin);
      if (!GeoUtils.isValidLatitude(origin.getLat())) {
         throw new QueryShardException(context, "latitude invalid for origin " + origin);
      }
      if (!GeoUtils.isValidLongitude(origin.getLon())) {
         throw new QueryShardException(context, "longitude invalid for origin " + origin);
      }

      URI appUri = TraveltimePlugin.API_URI.get(context.getIndexSettings().getSettings());
      String appId = TraveltimePlugin.APP_ID.get(context.getIndexSettings().getSettings());
      String apiKey = TraveltimePlugin.API_KEY.get(context.getIndexSettings().getSettings());
      if (appId.isEmpty()) {
         throw new IllegalStateException("Traveltime app id must be set in the config");
      }
      if (apiKey.isEmpty()) {
         throw new IllegalStateException("Traveltime api key must be set in the config");
      }

      Optional<Transportation> defaultMode = TraveltimePlugin.DEFAULT_MODE.get(context.getIndexSettings().getSettings());
      Optional<Country> defaultCountry = TraveltimePlugin.DEFAULT_COUNTRY.get(context.getIndexSettings().getSettings());
      Coordinates originCoord = Coordinates.builder().lat(origin.lat()).lng(origin.getLon()).build();
      TraveltimeQueryParameters params = new TraveltimeQueryParameters(field, originCoord, limit, mode, country);
      if (params.getMode() == null) {
         if (defaultMode.isPresent()) {
            params = params.withMode(defaultMode.get());
         } else {
            throw new IllegalStateException("Traveltime query requires either 'mode' field to be present or a default mode to be set in the config");
         }
      }
      if (params.getCountry() == null) {
         if (defaultCountry.isPresent()) {
            params = params.withCountry(defaultCountry.get());
         } else {
            throw new IllegalStateException("Traveltime query requires either 'country' field to be present or a default country to be set in the config");
         }
      }
      if (params.getLimit() <= 0) {
         throw new IllegalStateException("Traveltime limit must be greater than zero");
      }

      Query prefilterQuery = prefilter != null ? prefilter.toQuery(context) : null;

      return new TraveltimeSearchQuery(params, prefilterQuery, output, appUri, appId, apiKey);
   }

   @Override
   protected boolean doEquals(TraveltimeQueryBuilder other) {
      if (!Objects.equals(this.field, other.field)) return false;
      if (!Objects.equals(this.origin, other.origin)) return false;
      if (!Objects.equals(this.mode, other.mode)) return false;
      if (!Objects.equals(this.country, other.country)) return false;
      if (!Objects.equals(this.prefilter, other.prefilter)) return false;
      if (!Objects.equals(this.output, other.output)) return false;
      return this.limit == other.limit;
   }

   @Override
   protected int doHashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.field.hashCode();
      result = result * PRIME + this.origin.hashCode();
      result = result * PRIME + Objects.hashCode(this.mode);
      result = result * PRIME + Objects.hashCode(this.country);
      result = result * PRIME + Objects.hashCode(this.prefilter);
      result = result * PRIME + Objects.hashCode(this.output);
      result = result * PRIME + this.limit;
      return result;
   }

   @Override
   public String getWriteableName() {
      return TraveltimeQueryParser.NAME;
   }

   @Override
   public Version getMinimalSupportedVersion() {
      return Version.V_8_2_0;
   }
}
