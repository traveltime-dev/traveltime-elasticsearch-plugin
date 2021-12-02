package com.traveltime.plugin.elasticsearch.util;

import com.traveltime.sdk.dto.common.Coordinates;
import io.vavr.Tuple2;
import lombok.val;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.geo.GeoPoint;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.stream.*;

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


   public static <A> List<List<A>> grouped(List<A> collection, int groupSize) {
      return IntStream.iterate(0, i -> i < collection.size(), i -> i + groupSize)
         .mapToObj(i -> collection.subList(i, Math.min(i + groupSize, collection.size())))
         .collect(Collectors.toList());
   }

   public static <K, V> Collector<Tuple2<K, V>, ?, Map<K, V>> toMap() {
      return Collectors.toMap(kvTuple2 -> kvTuple2._1, kvTuple21 -> kvTuple21._2);
   }

   public static <K, V> Map<K, V> toMap(Stream<Tuple2<K, V>> stream) {
      return stream.collect(Util.toMap());
   }

   public static <A> Stream<A> toStream(Iterable<A> iterable) {
      return StreamSupport.stream(iterable.spliterator(), false);
   }

   public static <A> A elevate(PrivilegedAction<A> expr) {
      val sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new SpecialPermission());
      }
      return AccessController.doPrivileged(expr);
   }
}
