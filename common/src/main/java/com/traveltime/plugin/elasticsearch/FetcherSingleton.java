package com.traveltime.plugin.elasticsearch;

import java.net.URI;
import java.security.Permission;
import java.util.function.Supplier;

public enum FetcherSingleton {
   INSTANCE;

   private ProtoFetcher underlying = null;
   private final Object[] lock = new Object[0];

   public ProtoFetcher getFetcher(URI uri, String id, String key, Supplier<Permission> permissionSupplier) {
      if(underlying != null) return underlying;
      synchronized (lock) {
         if(underlying != null) return underlying;
         underlying = new ProtoFetcher(uri, id, key, permissionSupplier);
         return underlying;
      }
   }
}
