package com.traveltime.plugin.elasticsearch;

import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryBuilder;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParser;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.util.List;

public class TraveltimePlugin extends Plugin implements SearchPlugin {
    public static Setting<String> API_KEY = Setting.simpleString("traveltime.api.key", Setting.Property.NodeScope);

    @Override
    public List<Setting<?>> getSettings() { return List.of(API_KEY); }

    @Override
    public List<QuerySpec<?>> getQueries() {
        return List.of(
           new QuerySpec<>(TraveltimeQueryParser.NAME, TraveltimeQueryBuilder::new, new TraveltimeQueryParser())
        );
    }
}
