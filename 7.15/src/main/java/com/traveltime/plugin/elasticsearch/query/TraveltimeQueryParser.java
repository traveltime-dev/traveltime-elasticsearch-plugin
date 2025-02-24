package com.traveltime.plugin.elasticsearch.query;

import com.traveltime.plugin.elasticsearch.util.Util;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.geo.GeoUtils;
import org.elasticsearch.common.xcontent.ContextParser;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ParseField;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryParser;

public class TraveltimeQueryParser implements QueryParser<TraveltimeQueryBuilder> {
  public static String NAME = "traveltime";
  private final ParseField field = new ParseField("field");
  private final ParseField origin = new ParseField("origin");
  private final ParseField limit = new ParseField("limit");
  private final ParseField mode = new ParseField("mode");
  private final ParseField country = new ParseField("country");
  private final ParseField requestType = new ParseField("requestType");
  private final ParseField prefilter = new ParseField("prefilter");
  private final ParseField output = new ParseField("output");
  private final ParseField distanceOutput = new ParseField("distanceOutput");

  private final ContextParser<Void, QueryBuilder> prefilterParser =
      (p, c) -> AbstractQueryBuilder.parseInnerQueryBuilder(p);

  public final ObjectParser<TraveltimeQueryBuilder, Void> queryParser =
      new ObjectParser<>(NAME, TraveltimeQueryBuilder::new);

  {
    queryParser.declareString(TraveltimeQueryBuilder::setField, field);
    queryParser.declareField(
        TraveltimeQueryBuilder::setOrigin,
        (parser, c) -> GeoUtils.parseGeoPoint(parser),
        origin,
        ObjectParser.ValueType.VALUE_OBJECT_ARRAY);
    queryParser.declareInt(TraveltimeQueryBuilder::setLimit, limit);
    queryParser.declareString(
        (qb, s) -> qb.setMode(findByNameOrError("transportation mode", s, Util::findModeByName)),
        mode);
    queryParser.declareString(
        (qb, s) -> qb.setCountry(findByNameOrError("country", s, Util::findCountryByName)),
        country);
    queryParser.declareString(
        (qb, s) ->
            qb.setRequestType(findByNameOrError("request mode", s, Util::findRequestTypeByName)),
        requestType);
    queryParser.declareObject(TraveltimeQueryBuilder::setPrefilter, prefilterParser, prefilter);
    queryParser.declareString(TraveltimeQueryBuilder::setOutput, output);
    queryParser.declareString(TraveltimeQueryBuilder::setDistanceOutput, distanceOutput);

    queryParser.declareRequiredFieldSet(field.toString());
    queryParser.declareRequiredFieldSet(origin.toString());
    queryParser.declareRequiredFieldSet(limit.toString());
  }

  private static <T> T findByNameOrError(
      String what, String name, Function<String, Optional<T>> finder) {
    Optional<T> result = finder.apply(name);
    if (result.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Couldn't find a %s with the name %s", what, name));
    } else {
      return result.get();
    }
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
