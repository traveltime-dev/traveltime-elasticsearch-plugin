package com.traveltime.plugin.elasticsearch.util;

import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.val;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.geo.GeoPoint;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Util {
   private Util() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static Coordinates toCoord(GeoPoint point) {
      return Coordinates.builder().lat(point.lat()).lng(point.getLon()).build();
   }

   public static GeoPoint decode(long value) {
      double lat = GeoEncodingUtils.decodeLatitude((int) (value >> 32));
      double lon = GeoEncodingUtils.decodeLongitude((int) value);
      return new GeoPoint().resetLat(lat).resetLon(lon);
   }

   public static <A> Stream<A> toStream(Iterable<A> iterable) {
      return StreamSupport.stream(iterable.spliterator(), false);
   }

   public static Transportation findModeByName(String name) {
      return Arrays.stream(Transportation.values()).filter(it -> it.getValue().equals(name)).findFirst().get();
   }

   public static Country findCountryByName(String name) {
      return Arrays.stream(Country.values()).filter(it -> it.getValue().equals(name)).findFirst().get();
   }

   public static <A> A elevate(PrivilegedAction<A> expr) {
      val sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new SpecialPermission());
      }
      return AccessController.doPrivileged(expr);
   }

   public static <A> A time(Logger logger, Supplier<A> expr) {
      val startTime = System.currentTimeMillis();
      val res = expr.get();
      val endTime = System.currentTimeMillis();
      val lastStack = Thread.currentThread().getStackTrace()[2].toString();
      val message = String.format("In %s took %d ms", lastStack, endTime - startTime);
      logger.info(message);
      return res;
   }
}
