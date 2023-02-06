package ru.yandex.market.logistics.tarifficator.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.jobs.model.GenerateRevisionPayload;


@ParametersAreNonnullByDefault
public final class PayloadFactory {
    private PayloadFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static GenerateRevisionPayload createGenerateRevisionPayload(
        Collection<Long> tariffIds,
        Collection<Long> priceListIds,
        long... subRequestIds
    ) {
        return new GenerateRevisionPayload(computeRequestId(subRequestIds), tariffIds, priceListIds);
    }

    @Nonnull
    private static String computeRequestId(long... subRequestIds) {
        return StreamEx.of(AbstractContextualTest.REQUEST_ID)
            .append(Arrays.stream(subRequestIds).mapToObj(Long::toString))
            .collect(Collectors.joining("/"));
    }
}
