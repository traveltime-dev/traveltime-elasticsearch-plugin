package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.util.Util;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.xcontent.ContextParser;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryParser;

import java.io.IOException;

public class TraveltimeQueryParser implements QueryParser<TraveltimeQueryBuilder> {
   public static String NAME = "traveltime";
   ParseField field = new ParseField("field");
   ParseField origin = new ParseField("origin");
   ParseField limit = new ParseField("limit");
   ParseField mode = new ParseField("mode");
   ParseField country = new ParseField("country");
   ParseField prefilter = new ParseField("prefilter");

   ObjectParser<GeoPoint, Void> pointParser = new ObjectParser<>("origin", () -> new GeoPoint(Double.NaN, Double.NaN));

   {
      pointParser.declareDouble(GeoPoint::resetLat, new ParseField("lat"));
      pointParser.declareDouble(GeoPoint::resetLon, new ParseField("lon"));
   }

   ObjectParser<TraveltimeQueryBuilder, Void> queryParser = new ObjectParser<>(NAME, TraveltimeQueryBuilder::new);

   ContextParser<Void, QueryBuilder> prefilterParser = (p, c) -> AbstractQueryBuilder.parseInnerQueryBuilder(p);

   {
      queryParser.declareString(TraveltimeQueryBuilder::setField, field);
      queryParser.declareObject(TraveltimeQueryBuilder::setOrigin, pointParser, origin);
      queryParser.declareInt(TraveltimeQueryBuilder::setLimit, limit);
      queryParser.declareString((qb, s) -> qb.setMode(Util.findModeByName(s)), mode);
      queryParser.declareString((qb, s) -> qb.setCountry(Util.findCountryByName(s)), country);
      queryParser.declareObject(TraveltimeQueryBuilder::setPrefilter, prefilterParser, prefilter);
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
