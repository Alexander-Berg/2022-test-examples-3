package ru.yandex.market.logistics.nesu.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics4shops.client.model.MdsFilePath;
import ru.yandex.market.logistics4shops.client.model.Outbound;
import ru.yandex.market.logistics4shops.client.model.OutboundsSearchRequest;

@ParametersAreNonnullByDefault
public final class L4ShopsFactory {
    public static final OutboundsSearchRequest SEARCH_OUTBOUND_REQUEST = new OutboundsSearchRequest()
        .yandexIds(List.of(TMFactory.outboundId()));
    public static final String CONFIRMED = "2021-05-01T02:00:00Z";

    private L4ShopsFactory() {
        throw new UnsupportedOperationException();
    }

    public static Outbound outbound(@Nullable List<String> orderIds) {
        return outbound(TMFactory.outboundId(), orderIds, CONFIRMED);
    }

    @Nonnull
    public static Outbound outbound(@Nullable String id, @Nullable List<String> orderIds, @Nullable String confirmed) {
        var outbound = new Outbound()
            .id(1L)
            .yandexId(Optional.ofNullable(id).orElse("-1L"))
            .orderIds(orderIds);
        Optional.ofNullable(confirmed)
            .map(Instant::parse)
            .ifPresent(outbound::setConfirmed);
        return outbound;
    }

    @Nonnull
    public static OutboundsSearchRequest outboundIds(int size) {
        return new OutboundsSearchRequest().yandexIds(
            IntStream.range(0, size)
                .mapToObj(TMFactory::outboundId)
                .collect(Collectors.toList())
        );
    }

    @Nonnull
    public static MdsFilePath mdsFilePath() {
        return new MdsFilePath()
            .bucket("bucket")
            .filename("filename");
    }
}
