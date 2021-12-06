package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class TraveltimeQueryParameters implements ToXContent {
   String field;
   GeoPoint origin;
   int limit;
   Transportation mode;

   public TraveltimeQueryParameters(StreamInput in) throws IOException {
      field = in.readString();
      origin = in.readGeoPoint();
      limit = in.readInt();
      mode = in.readEnum(Transportation.class);
   }

   public void writeTo(StreamOutput out) throws IOException {
      out.writeString(field);
      out.writeGeoPoint(origin);
      out.writeInt(limit);
      out.writeEnum(mode);
   }

   public void doWriteXContent(XContentBuilder builder) throws IOException {
      builder.field("field", field);
      builder.field("origin", origin);
      builder.field("limit", limit);
      builder.field("mode", mode);
   }

   @Override
   public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
      builder.startObject();
      doWriteXContent(builder);
      builder.endObject();
      return builder;
   }
}
