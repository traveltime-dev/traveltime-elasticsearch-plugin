package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.*;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

@Data
public class TraveltimeQueryParameters implements ToXContent {
   private final String field;
   private final GeoPoint origin;
   private final int limit;
   @With private final Transportation mode;
   @With private final Country country;

   @Override
   public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
      builder.startObject();
      builder.field("field", field);
      builder.field("origin", origin);
      builder.field("limit", limit);
      builder.field("mode", mode);
      builder.field("country", country);
      builder.endObject();
      return builder;
   }
}
