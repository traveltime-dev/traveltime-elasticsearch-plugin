package com.traveltime.plugin.elasticsearch;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParameters;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public enum TraveltimeCache {
    INSTANCE;

    private static final class LockedMap {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final Long2IntMap map = new Long2IntOpenHashMap();

        public LockedMap() {
            map.defaultReturnValue(-1);
        }

        public int get(long key) {
            lock.readLock().lock();
            try {
                return map.get(key);
            } finally {
                lock.readLock().unlock();
            }
        }

        public void putAll(Map<Long, Integer> other) {
            lock.writeLock().lock();
            try {
                map.putAll(other);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private final LoadingCache<TraveltimeQueryParameters, LockedMap> lockedMap =
            CacheBuilder
                    .newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(new CacheLoader<TraveltimeQueryParameters, LockedMap>() {
                        @NotNull
                        @Override
                        public LockedMap load(@NotNull TraveltimeQueryParameters key) {
                            return new LockedMap();
                        }
                    });

    public Integer get(TraveltimeQueryParameters params, long point) {
        return lockedMap.getUnchecked(params).get(point);
    }

    public void add(TraveltimeQueryParameters params, Map<Long, Integer> results) {
        lockedMap.getUnchecked(params).putAll(results);
    }
}
