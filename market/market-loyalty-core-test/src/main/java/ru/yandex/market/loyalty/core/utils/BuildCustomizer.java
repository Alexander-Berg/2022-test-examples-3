package ru.yandex.market.loyalty.core.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface BuildCustomizer<T, B> {
    void change(B builder);

    class Util {
        @Nonnull
        @SafeVarargs
        public static <T, B> B customize(
                @Nonnull Supplier<B> supplier, BuildCustomizer<T, B>... customizers
        ) {
            return customize(supplier, List.of(customizers));
        }

        @Nonnull
        public static <T, B> B customize(
                @Nonnull Supplier<B> supplier, Collection<BuildCustomizer<T, B>> customizers
        ) {
            B builder = supplier.get();
            customizers.forEach(c -> c.change(builder));
            return builder;
        }

        @Nonnull
        public static <T, B> Supplier<B> same(@Nonnull B builder) {
            return () -> builder;
        }

        @Nonnull
        @SafeVarargs
        public static <T, B extends Builder<T>> Stream<T> generateWith(
                @Nonnull Supplier<B> supplier,
                int count,
                SequenceCustomizer<T, B>... customizers
        ) {
            return LongStream.rangeClosed(1, count).mapToObj(i -> {
                B builder = supplier.get();
                Arrays.stream(customizers).forEach(c -> c.change(i, builder));
                return builder.build();
            });
        }

        @Nonnull
        @SafeVarargs
        public static <T, B> BuildCustomizer<T, B> mixin(
                BuildCustomizer<T, B>... customizers
        ) {
            return b -> customize(same(b), customizers);
        }
    }
}
