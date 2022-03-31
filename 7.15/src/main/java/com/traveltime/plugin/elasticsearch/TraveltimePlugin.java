package com.traveltime.plugin.elasticsearch;


import com.traveltime.plugin.elasticsearch.query.TraveltimeFetchPhase;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryBuilder;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParser;
import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TraveltimePlugin extends Plugin implements SearchPlugin {
   public static Setting<String> APP_ID = Setting.simpleString("traveltime.app.id", Setting.Property.NodeScope);
   public static Setting<String> API_KEY = Setting.simpleString("traveltime.api.key", Setting.Property.NodeScope, Setting.Property.Filtered);
   public static Setting<Optional<Transportation>> DEFAULT_MODE = new Setting<>("traveltime.default.mode", s -> "", Util::findModeByName, Setting.Property.NodeScope);
   public static Setting<Optional<Country>> DEFAULT_COUNTRY = new Setting<>("traveltime.default.country", s -> "", Util::findCountryByName, Setting.Property.NodeScope);
   public static Setting<URI> API_URI = new Setting<>("traveltime.api.uri", s -> "https://proto.api.traveltimeapp.com/api/v2/", URI::create, Setting.Property.NodeScope);

   private void cleanUpAndReschedule(ThreadPool threadPool) {
      TraveltimeCache.INSTANCE.cleanUp();
      threadPool.scheduleUnlessShuttingDown(TimeValue.timeValueSeconds(120), "generic", () -> cleanUpAndReschedule(threadPool));
   }

   @Override
   public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool, ResourceWatcherService resourceWatcherService, ScriptService scriptService, NamedXContentRegistry xContentRegistry, Environment environment, NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry, IndexNameExpressionResolver indexNameExpressionResolver, Supplier<RepositoriesService> repositoriesServiceSupplier) {
      cleanUpAndReschedule(threadPool);
      return super.createComponents(client, clusterService, threadPool, resourceWatcherService, scriptService, xContentRegistry, environment, nodeEnvironment, namedWriteableRegistry, indexNameExpressionResolver, repositoriesServiceSupplier);

   }

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

   @Override
   public List<FetchSubPhase> getFetchSubPhases(FetchPhaseConstructionContext context) {
      return List.of(new TraveltimeFetchPhase());
   }
}
