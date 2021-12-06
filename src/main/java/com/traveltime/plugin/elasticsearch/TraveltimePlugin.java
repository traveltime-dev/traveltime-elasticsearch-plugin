package com.traveltime.plugin.elasticsearch;

import com.traveltime.plugin.elasticsearch.aggregation.TraveltimeAggregator;
import com.traveltime.plugin.elasticsearch.aggregation.TraveltimeAggregatorBuilder;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryBuilder;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParser;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.val;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.util.List;

public class TraveltimePlugin extends Plugin implements SearchPlugin {
   public static Setting<String> APP_ID = Setting.simpleString("traveltime.app.id", Setting.Property.NodeScope);
   public static Setting<String> API_KEY = Setting.simpleString("traveltime.api.key", Setting.Property.NodeScope, Setting.Property.Filtered);
   public static Setting<Transportation> DEFAULT_MODE = new Setting<>("traveltime.default.mode", s -> "DRIVING_FERRY", Transportation::valueOf, Setting.Property.NodeScope);
   public static Setting<Country> DEFAULT_COUNTRY = new Setting<>("traveltime.default.country", s -> "UNITED_KINGDOM", Country::valueOf, Setting.Property.NodeScope);

   @Override
   public List<Setting<?>> getSettings() {
      return List.of(APP_ID, API_KEY, DEFAULT_MODE, DEFAULT_COUNTRY);
   }

   @Override
   public List<QuerySpec<?>> getQueries() {
      return List.of(
         new QuerySpec<>(TraveltimeQueryParser.NAME, TraveltimeQueryBuilder::new, new TraveltimeQueryParser())
      );
   }

   @Override
   public List<AggregationSpec> getAggregations() {
      val spec = new AggregationSpec(TraveltimeAggregatorBuilder.NAME, TraveltimeAggregatorBuilder::new, TraveltimeAggregatorBuilder.PARSER);
      spec.addResultReader(TraveltimeAggregator.TraveltimeInternalAggregation::new);
      return List.of(spec);
   }

   @Override
   public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
      return List.of(
         new NamedWriteableRegistry.Entry(TraveltimeAggregatorBuilder.class, TraveltimeAggregatorBuilder.NAME, TraveltimeAggregatorBuilder::new)
      );
   }
}
