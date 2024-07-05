package com.traveltime.plugin.elasticsearch;

import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.sdk.TravelTimeSDK;
import com.traveltime.sdk.auth.TravelTimeCredentials;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.TimeFilterFastProtoRequest;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.RequestType;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import com.traveltime.sdk.dto.responses.TimeFilterFastProtoResponse;
import com.traveltime.sdk.dto.responses.errors.IOError;
import com.traveltime.sdk.dto.responses.errors.ResponseError;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ProtoFetcher {
   private final TravelTimeSDK api;
   private final Supplier<Permission> permissionSupplier;

   private final Logger log = LogManager.getLogger();

   private void logError(TravelTimeError left) {
      if (left instanceof IOError) {
         val ioerr = (IOError) left;
         log.warn(ioerr.getMessage());
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

   public ProtoFetcher(URI uri, String id, String key, Supplier<Permission> permissionSupplier) {
      val auth = TravelTimeCredentials.builder().appId(id).apiKey(key).build();
      val builder = TravelTimeSDK.builder().baseProtoUri(uri).credentials(auth);
      this.permissionSupplier = permissionSupplier;
      api = Util.elevate(builder::build, permissionSupplier);
   }

   public List<Integer> getTimes(Coordinates origin, List<Coordinates> destinations, int limit, Transportation mode, Country country, RequestType requestType) {
      if(destinations.isEmpty()) {
         return Collections.emptyList();
      }

      val fastProto =
         TimeFilterFastProtoRequest
            .builder()
            .country(country)
            .transportation(mode)
            .originCoordinate(origin)
            .destinationCoordinates(destinations)
            .travelTime(limit)
            .requestType(requestType)
            .build();


      log.info(String.format("Fetching %d destinations", destinations.size()));
      val result = Util.time(log, () -> Util.elevate(() -> api.sendProtoBatched(fastProto), permissionSupplier));

      return result.fold(
         err -> {
            logError(err);
            throw new RuntimeException(err.getMessage());
         },
         TimeFilterFastProtoResponse::getTravelTimes
      );
   }

}
