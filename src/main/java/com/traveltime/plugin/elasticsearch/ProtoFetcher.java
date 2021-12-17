package com.traveltime.plugin.elasticsearch;

import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.sdk.TravelTimeSDK;
import com.traveltime.sdk.auth.BaseAuth;
import com.traveltime.sdk.dto.requests.TimeFilterFastProtoRequest;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.OneToMany;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import com.traveltime.sdk.dto.responses.TimeFilterFastProtoResponse;
import com.traveltime.sdk.dto.responses.errors.IOError;
import com.traveltime.sdk.dto.responses.errors.ResponseError;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.geo.GeoPoint;

import java.net.URI;
import java.util.Arrays;
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

   public ProtoFetcher(URI uri, String id, String key) {
      val auth = new BaseAuth(id, key);
      val builder = TravelTimeSDK.builder().baseProtoUri(uri).credentials(auth);
      api = Util.elevate(builder::build);
   }

   public List<Integer> getTimes(GeoPoint origin, List<GeoPoint> destinations, int limit, Transportation mode, Country country) {
      val fastProto =
         TimeFilterFastProtoRequest
            .builder()
            .oneToMany(
               OneToMany
                  .builder()
                  .country(country)
                  .transportation(mode)
                  .originCoordinate(Util.toCoord(origin))
                  .destinationCoordinates(destinations.stream().map(Util::toCoord).collect(Collectors.toList()))
                  .travelTime(limit)
                  .build()
            )
            .build();


      log.info(String.format("Fetching %d destinations", destinations.size()));
      val result = Util.time(log, () -> Util.elevate(() -> api.sendProto(fastProto)));

      return result.fold(
         err -> {
            logError(err);
            throw new RuntimeException(err.getMessage());
         },
         TimeFilterFastProtoResponse::getTravelTimes
      );
   }

}
