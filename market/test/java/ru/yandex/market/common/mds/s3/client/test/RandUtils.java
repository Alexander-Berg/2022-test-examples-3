package ru.yandex.market.common.mds.s3.client.test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProvider;

/**
 * Утилитный класс для данных случайного содержимого.
 *
 * @author Vladislav Bauer
 */
public final class RandUtils {

    private RandUtils() {
        throw new UnsupportedOperationException();
    }


    @Nonnull
    public static TextContentProvider createText() {
        return new TextContentProvider(randomText());
    }

    @Nonnull
    public static List<TextContentProvider> createTexts(final int amount) {
        return IntStream.range(0, amount)
            .mapToObj(i -> RandUtils.createText())
            .collect(Collectors.toList());
    }

    @Nonnull
    public static String randomText() {
        return UUID.randomUUID().toString();
    }



}
