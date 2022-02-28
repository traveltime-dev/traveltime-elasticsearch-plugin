package com.traveltime.plugin.elasticsearch;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParameters;
import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.val;
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

   private final LoadingCache<TraveltimeQueryParameters, Map<Coordinates, Integer>> cache =
      CacheBuilder
         .newBuilder()
         .maximumSize(1000)
         .expireAfterAccess(1, TimeUnit.MINUTES)
         .build(new CacheLoader<>() {
            @NotNull
            @Override
            public Map<Coordinates, Integer> load(@NotNull TraveltimeQueryParameters key) {
               val res = new Object2IntOpenHashMap<Coordinates>();
               res.defaultReturnValue(-1);
               return res;
            }
         });

   public Integer get(TraveltimeQueryParameters params, Coordinates point) {
      val results = cache.getUnchecked(params);
      val lock = locks.getUnchecked(params);
      lock.readLock().lock();
      val res = results.get(point);
      lock.readLock().unlock();
      return res;
   }

   public void add(TraveltimeQueryParameters params, Map<Coordinates, Integer> results) {
      val map = cache.getUnchecked(params);
      val lock = locks.getUnchecked(params);

      lock.writeLock().lock();
      map.putAll(results);
      lock.writeLock().unlock();

   }
}
