package com.traveltime.plugin.elasticsearch.query;

import lombok.AllArgsConstructor;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

@AllArgsConstructor
public class TraveltimeQueryParameters implements ToXContent {
   String field;
   GeoPoint origin;
   int limit;

   public TraveltimeQueryParameters(StreamInput in) throws IOException {
      field = in.readString();
      origin = in.readGeoPoint();
      limit = in.readInt();
   }

   public void writeTo(StreamOutput out) throws IOException {
      out.writeString(field);
      out.writeGeoPoint(origin);
      out.writeInt(limit);
   }

   public void doWriteXContent(XContentBuilder builder) throws IOException {
      builder.field("field", field);
      builder.field("origin", origin);
      builder.field("limit", limit);
   }

   @Override
   public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
      builder.startObject();
      doWriteXContent(builder);
      builder.endObject();
      return builder;
   }

   public String getField() {
      return this.field;
   }

   public GeoPoint getOrigin() {
      return this.origin;
   }

   public int getLimit() {
      return this.limit;
   }

   public boolean equals(final Object o) {
      if (o == this) return true;
      if (!(o instanceof TraveltimeQueryParameters)) return false;
      final TraveltimeQueryParameters other = (TraveltimeQueryParameters) o;
      if (!other.canEqual(this)) return false;
      if (this.getField() == null ? other.getField() != null : !((Object) this.getField()).equals(other.getField()))
         return false;
      if (this.getOrigin() == null ? other.getOrigin() != null : !((Object) this.getOrigin()).equals(other.getOrigin()))
         return false;
      return this.getLimit() == other.getLimit();
   }

   protected boolean canEqual(final Object other) {
      return other instanceof TraveltimeQueryParameters;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + (this.getField() == null ? 43 : ((Object) this.getField()).hashCode());
      result = result * PRIME + (this.getOrigin() == null ? 43 : ((Object) this.getOrigin()).hashCode());
      result = result * PRIME + this.getLimit();
      return result;
   }

   public String toString() {
      return "QueryParameters(field=" + this.getField() + ", origin=" + this.getOrigin() + ", limit=" + this.getLimit() + ")";
   }
}
