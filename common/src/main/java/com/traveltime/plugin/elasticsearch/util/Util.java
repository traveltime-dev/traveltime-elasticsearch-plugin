package com.traveltime.plugin.elasticsearch.util;

import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.RequestType;
import lombok.val;
import org.apache.logging.log4j.Logger;

import java.security.Permission;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public final class Util {
   private Util() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static Optional<com.traveltime.sdk.dto.requests.proto.Transportation.Modes> findModeByName(String name) {
      return Arrays.stream(com.traveltime.sdk.dto.requests.proto.Transportation.Modes.values()).filter(it -> it.getValue().equals(name)).findFirst();
   }

   public static Optional<Country> findCountryByName(String name) {
      return Arrays.stream(Country.values()).filter(it -> it.getValue().equals(name)).findFirst();
   }

   public static Optional<RequestType> findRequestTypeByName(String name) {
      return Arrays.stream(RequestType.values()).filter(it -> it.name().equals(name)).findFirst();
   }

   public static boolean canUseDistance(com.traveltime.sdk.dto.requests.proto.Transportation transportation) {
      return transportation != com.traveltime.sdk.dto.requests.proto.Transportation.Modes.PUBLIC_TRANSPORT;
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

   public static <A> A elevate(Supplier<A> expr, Supplier<Permission> permissionSupplier) {
      Permission requiredPermission = permissionSupplier.get();
      if (requiredPermission != null) {
         System.out.println("Permission check: " + requiredPermission.getName());
      }

      return expr.get();
   }

   public static final short BITS = 32;

   private static final double LAT_SCALE = (0x1L << BITS) / 180.0D;
   private static final double LAT_DECODE = 1 / LAT_SCALE;
   private static final double LON_SCALE = (0x1L << BITS) / 360.0D;
   private static final double LON_DECODE = 1 / LON_SCALE;

   public static double decodeLatitude(int encoded) {
      return encoded * LAT_DECODE;
   }

   public static double decodeLongitude(int encoded) {
      return encoded * LON_DECODE;
   }

   public static Coordinates decode(long value) {
      double lat = decodeLatitude((int) (value >> 32));
      double lon = decodeLongitude((int) value);
      return new Coordinates(lat, lon);
   }
}
