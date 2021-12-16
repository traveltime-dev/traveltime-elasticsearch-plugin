package com.traveltime.plugin.elasticsearch;


import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryBuilder;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParser;
import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class TraveltimePlugin extends Plugin implements SearchPlugin {
   public static Setting<String> APP_ID = Setting.simpleString("traveltime.app.id", Setting.Property.NodeScope);
   public static Setting<String> API_KEY = Setting.simpleString("traveltime.api.key", Setting.Property.NodeScope, Setting.Property.Filtered);
   public static Setting<Optional<Transportation>> DEFAULT_MODE = new Setting<>("traveltime.default.mode", s -> "", Util::findModeByName, Setting.Property.NodeScope);
   public static Setting<Optional<Country>> DEFAULT_COUNTRY = new Setting<>("traveltime.default.country", s -> "", Util::findCountryByName, Setting.Property.NodeScope);
   public static Setting<URI> API_URI = new Setting<>("traveltime.api.uri", s -> "https://proto.api.traveltimeapp.com/api/v2/", URI::create, Setting.Property.NodeScope);

   public static int BATCH_SIZE = 550_000;

   @Override
   public List<Setting<?>> getSettings() {
      return List.of(APP_ID, API_KEY, DEFAULT_MODE, DEFAULT_COUNTRY, API_URI);
   }

   @Override
   public List<QuerySpec<?>> getQueries() {
      return List.of(
         new QuerySpec<>(TraveltimeQueryParser.NAME, TraveltimeQueryBuilder::new, new TraveltimeQueryParser())
      );
   }
}
