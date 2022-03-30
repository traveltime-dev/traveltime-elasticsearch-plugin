package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.Data;
import lombok.With;

@Data
public class TraveltimeQueryParameters {
   private final String field;
   private final Coordinates origin;
   private final int limit;
   @With private final Transportation mode;
   @With private final Country country;

}
