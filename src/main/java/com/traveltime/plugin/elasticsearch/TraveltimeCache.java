package com.traveltime.plugin.elasticsearch;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParameters;
import com.traveltime.plugin.elasticsearch.query.TraveltimeSearchQuery;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.val;
import org.elasticsearch.common.geo.GeoPoint;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public enum TraveltimeCache {
   INSTANCE;

   private final LoadingCache<TraveltimeQueryParameters, ReadWriteLock> locks =
      CacheBuilder
         .newBuilder()
         .maximumSize(1000)
         .expireAfterAccess(1, TimeUnit.MINUTES)
         .build(new CacheLoader<>() {
            @NotNull
            @Override
            public ReadWriteLock load(@NotNull TraveltimeQueryParameters key) {
               return new ReentrantReadWriteLock();
            }
         });

   private final LoadingCache<TraveltimeQueryParameters, Map<GeoPoint, Integer>> cache =
      CacheBuilder
         .newBuilder()
         .maximumSize(1000)
         .expireAfterAccess(1, TimeUnit.MINUTES)
         .build(new CacheLoader<>() {
            @NotNull
            @Override
            public Map<GeoPoint, Integer> load(@NotNull TraveltimeQueryParameters key) {
               val res = new Object2IntOpenHashMap<GeoPoint>();
               res.defaultReturnValue(-1);
               return res;
            }
         });

   public Integer get(TraveltimeQueryParameters params, GeoPoint point) {
      val results = cache.getUnchecked(params);
      val lock = locks.getUnchecked(params);
      lock.readLock().lock();
      val res = results.get(point);
      lock.readLock().unlock();
      return res;
   }

   public void add(TraveltimeSearchQuery query, Map<GeoPoint, Integer> results) {
      val map = cache.getUnchecked(query.getParams());
      val lock = locks.getUnchecked(query.getParams());

      lock.writeLock().lock();
      map.putAll(results);
      lock.writeLock().unlock();

   }
}
