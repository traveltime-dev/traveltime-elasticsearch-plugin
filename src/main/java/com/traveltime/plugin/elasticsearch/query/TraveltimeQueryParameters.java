package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.*;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

@AllArgsConstructor
@Data
public class TraveltimeQueryParameters implements ToXContent {
   String field;
   GeoPoint origin;
   int limit;
   Transportation mode;
   Country country;

   public TraveltimeQueryParameters(StreamInput in) throws IOException {
      field = in.readString();
      origin = in.readGeoPoint();
      limit = in.readInt();
      mode = in.readEnum(Transportation.class);
      country = in.readEnum(Country.class);
   }

   public void writeTo(StreamOutput out) throws IOException {
      out.writeString(field);
      out.writeGeoPoint(origin);
      out.writeInt(limit);
      out.writeEnum(mode);
      out.writeEnum(country);
   }

   public void doWriteXContent(XContentBuilder builder) throws IOException {
      builder.field("field", field);
      builder.field("origin", origin);
      builder.field("limit", limit);
      builder.field("mode", mode);
      builder.field("country", country);
   }

   @Override
   public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
      builder.startObject();
      doWriteXContent(builder);
      builder.endObject();
      return builder;
   }
}
