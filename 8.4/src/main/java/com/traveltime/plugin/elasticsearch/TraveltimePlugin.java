package com.traveltime.plugin.elasticsearch;

import com.traveltime.plugin.elasticsearch.query.TraveltimeFetchPhase;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryBuilder;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParser;
import com.traveltime.plugin.elasticsearch.util.Util;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.RequestType;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.tracing.Tracer;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xcontent.NamedXContentRegistry;

public class TraveltimePlugin extends Plugin implements SearchPlugin {
  public static final Setting<String> APP_ID =
      Setting.simpleString("traveltime.app.id", Setting.Property.NodeScope);
  public static final Setting<String> API_KEY =
      Setting.simpleString(
          "traveltime.api.key", Setting.Property.NodeScope, Setting.Property.Filtered);
  public static final Setting<Optional<Transportation.Modes>> DEFAULT_MODE =
      new Setting<>(
          "traveltime.default.mode", s -> "", Util::findModeByName, Setting.Property.NodeScope);
  public static final Setting<Optional<Country>> DEFAULT_COUNTRY =
      new Setting<>(
          "traveltime.default.country",
          s -> "",
          Util::findCountryByName,
          Setting.Property.NodeScope);

  public static final Setting<Optional<RequestType>> DEFAULT_REQUEST_TYPE =
      new Setting<>(
          "traveltime.default.request_type",
          s -> RequestType.ONE_TO_MANY.name(),
          Util::findRequestTypeByName,
          Setting.Property.NodeScope);
  public static final Setting<URI> API_URI =
      new Setting<>(
          "traveltime.api.uri",
          s -> "https://proto.api.traveltimeapp.com/api/v2/",
          URI::create,
          Setting.Property.NodeScope);

  private static final Setting<Integer> CACHE_CLEANUP_INTERVAL =
      Setting.intSetting("traveltime.cache.cleanup.interval", 120, 0, Setting.Property.NodeScope);
  private static final Setting<Integer> CACHE_EXPIRY =
      Setting.intSetting("traveltime.cache.expiry", 60, 0, Setting.Property.NodeScope);
  private static final Setting<Integer> CACHE_SIZE =
      Setting.intSetting("traveltime.cache.size", 50, 0, Setting.Property.NodeScope);

  private void cleanUpAndReschedule(ThreadPool threadPool, TimeValue cleanupSeconds) {
    TraveltimeCache.INSTANCE.cleanUp();
    TraveltimeCache.DISTANCE.cleanUp();
    threadPool.scheduleUnlessShuttingDown(
        cleanupSeconds, "generic", () -> cleanUpAndReschedule(threadPool, cleanupSeconds));
  }

  @Override
  public Collection<Object> createComponents(
      Client client,
      ClusterService clusterService,
      ThreadPool threadPool,
      ResourceWatcherService resourceWatcherService,
      ScriptService scriptService,
      NamedXContentRegistry xContentRegistry,
      Environment environment,
      NodeEnvironment nodeEnvironment,
      NamedWriteableRegistry namedWriteableRegistry,
      IndexNameExpressionResolver indexNameExpressionResolver,
      Supplier<RepositoriesService> repositoriesServiceSupplier,
      Tracer tracer) {
    TimeValue cleanupSeconds =
        TimeValue.timeValueSeconds(CACHE_CLEANUP_INTERVAL.get(environment.settings()));
    Duration cacheExpiry = Duration.ofSeconds(CACHE_EXPIRY.get(environment.settings()));
    Integer cacheSize = CACHE_SIZE.get(environment.settings());

    TraveltimeCache.INSTANCE.setUp(cacheSize, cacheExpiry);
    TraveltimeCache.DISTANCE.setUp(cacheSize, cacheExpiry);
    cleanUpAndReschedule(threadPool, cleanupSeconds);

    return super.createComponents(
        client,
        clusterService,
        threadPool,
        resourceWatcherService,
        scriptService,
        xContentRegistry,
        environment,
        nodeEnvironment,
        namedWriteableRegistry,
        indexNameExpressionResolver,
        repositoriesServiceSupplier,
        tracer);
  }

  @Override
  public List<Setting<?>> getSettings() {
    return List.of(
        APP_ID,
        API_KEY,
        DEFAULT_MODE,
        DEFAULT_COUNTRY,
        DEFAULT_REQUEST_TYPE,
        API_URI,
        CACHE_SIZE,
        CACHE_EXPIRY,
        CACHE_CLEANUP_INTERVAL);
  }

  @Override
  public List<QuerySpec<?>> getQueries() {
    return List.of(
        new QuerySpec<>(
            TraveltimeQueryParser.NAME,
            TraveltimeQueryBuilder::new,
            TraveltimeQueryBuilder::fromXContent));
  }

  @Override
  public List<FetchSubPhase> getFetchSubPhases(FetchPhaseConstructionContext context) {
    return List.of(new TraveltimeFetchPhase());
  }
}
