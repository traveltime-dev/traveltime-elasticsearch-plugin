package com.traveltime.plugin.elasticsearch.query;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParser;

import java.io.IOException;

public class TraveltimeQueryParser implements QueryParser<TraveltimeQueryBuilder> {
   public static String NAME = "traveltime";
   ParseField field = new ParseField("field");
   ParseField origin = new ParseField("origin");
   ParseField limit = new ParseField("limit");

   ObjectParser<GeoPoint, Void> pointParser = new ObjectParser<>("origin", () -> new GeoPoint(Double.NaN, Double.NaN));

   {
      pointParser.declareDouble(GeoPoint::resetLat, new ParseField("lat"));
      pointParser.declareDouble(GeoPoint::resetLon, new ParseField("lon"));
   }

   ObjectParser<TraveltimeQueryBuilder, Void> queryParser = new ObjectParser<>(NAME, TraveltimeQueryBuilder::new);

   {
      queryParser.declareString(TraveltimeQueryBuilder::setField, field);
      queryParser.declareObject(TraveltimeQueryBuilder::setOrigin, pointParser, origin);
      queryParser.declareInt(TraveltimeQueryBuilder::setLimit, limit);
   }


   @Override
   public TraveltimeQueryBuilder fromXContent(XContentParser parser) throws IOException {
      try {
         return queryParser.parse(parser, null);
      } catch (IllegalArgumentException iae) {
         throw new ParsingException(parser.getTokenLocation(), iae.getMessage(), iae);
      }
   }
}
