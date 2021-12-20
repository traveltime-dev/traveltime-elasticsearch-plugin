package com.traveltime.plugin.elasticsearch;

import com.traveltime.plugin.elasticsearch.query.TraveltimeQueryParameters;
import com.traveltime.plugin.elasticsearch.query.TraveltimeSearchQuery;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.val;
import org.elasticsearch.common.geo.GeoPoint;

import java.util.Map;
import java.util.WeakHashMap;

public enum TraveltimeCache {
   INSTANCE;

   private final WeakHashMap<TraveltimeQueryParameters, Map<GeoPoint, Integer>> weakMap = new WeakHashMap<>();

   public Integer get(TraveltimeQueryParameters params, GeoPoint point) {
      if (!weakMap.containsKey(params)) {
         return -1;
      } else {
         return weakMap.get(params).getOrDefault(point, -1);
      }
   }

   public void add(TraveltimeSearchQuery query, Map<GeoPoint, Integer> results) {
      if (!weakMap.containsKey(query.getParams())) {
         weakMap.put(query.getParams(), new Object2IntOpenHashMap<>());
      }
      val cache = weakMap.get(query.getParams());
      cache.putAll(results);
   }
}
