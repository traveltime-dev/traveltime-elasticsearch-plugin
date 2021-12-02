package com.traveltime.plugin.elasticsearch;

import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.plugin.elasticsearch.util.ZipIterable;
import com.traveltime.sdk.TravelTimeSDK;
import com.traveltime.sdk.dto.requests.TimeFilterFastProtoRequest;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.OneToMany;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import com.traveltime.sdk.dto.responses.errors.IOError;
import com.traveltime.sdk.dto.responses.errors.ResponseError;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import io.vavr.Tuple2;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.geo.GeoPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ProtoFetcher {
   private final TravelTimeSDK api;

   private final Logger log = LogManager.getLogger();

   private void logError(TravelTimeError left) {
      if (left instanceof IOError) {
         val ioerr = (IOError) left;
         log.warn(ioerr.getCause().getMessage());
         log.warn(
            Arrays.stream(ioerr.getCause().getStackTrace())
               .map(StackTraceElement::toString)
               .reduce("", (a, b) -> a + "\n\t" + b)
         );
      } else if (left instanceof ResponseError) {
         val error = (ResponseError) left;
         log.warn(error.getDescription());
      }
   }

   public ProtoFetcher(String key) {
      val builder = TravelTimeSDK.builder().appId("crude-curl").apiKey(key);
      api = Util.elevate(builder::build);
   }

   public Iterable<Tuple2<GeoPoint, Integer>> getTimes(GeoPoint origin, List<GeoPoint> destinations, int limit) {
      val fastProto = TimeFilterFastProtoRequest
         .builder()
         .oneToMany(
            OneToMany
               .builder()
               .country(Country.UNITED_KINGDOM)
               .transportation(Transportation.DRIVING_FERRY)
               .originCoordinate(Util.toCoord(origin))
               .destinationCoordinates(destinations.stream().map(Util::toCoord).collect(Collectors.toList()))
               .travelTime(limit)
               .build()
         )
         .build();


      val result = Util.elevate(() -> api.sendProto(fastProto));

      return result.fold(
         err -> {
            logError(err);
            return new ZipIterable<>(destinations, Collections.nCopies(destinations.size(), -1));
         },
         response -> new ZipIterable<>(destinations, response.getTravelTimes())
      );
   }

}
