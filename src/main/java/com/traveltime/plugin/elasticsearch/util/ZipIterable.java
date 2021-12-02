package com.traveltime.plugin.elasticsearch.util;

import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.Iterator;

@AllArgsConstructor
public class ZipIterable<A, B> implements Iterable<Tuple2<A, B>> {
   private Iterable<A> iterableA;
   private Iterable<B> iterableB;

   @Override
   public Iterator<Tuple2<A, B>> iterator() {
      val iteratorA = iterableA.iterator();
      val iteratorB = iterableB.iterator();

      return new Iterator<>() {
         @Override
         public boolean hasNext() {
            return iteratorA.hasNext() && iteratorB.hasNext();
         }

         @Override
         public Tuple2<A, B> next() {
            return new Tuple2<>(iteratorA.next(), iteratorB.next());
         }
      };
   }
}
