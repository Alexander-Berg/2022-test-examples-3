package ru.yandex.market.logistics.logistics4shops.factory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Component
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class LomFactory {
    private final LomClient lomClient;

    @Nonnull
    public AutoCloseable mockSearchWithDefaultOrders(
        PlatformClient platformClient,
        Set<String> externalIds
    ) {
        return mockSearchOrders(
            searchFilter(platformClient, externalIds),
            externalIds.stream().map(id -> defaultOrder(platformClient, id)).toList()
        );
    }

    @Nonnull
    public AutoCloseable mockSearchOrders(
        OrderSearchFilter orderSearchFilter,
        List<OrderDto> result
    ) {
        // Не паримся о том, как страница вернется
        when(lomClient.searchOrders(orderSearchFilter, Pageable.unpaged()))
            .thenReturn(new PageResult<OrderDto>().setData(result));
        return () -> verify(lomClient).searchOrders(orderSearchFilter, Pageable.unpaged());
    }

    @Nonnull
    public OrderDto defaultOrder(
        PlatformClient platformClient,
        String externalId,
        List<StorageUnitDto> storageUnits
    ) {
        return new OrderDto()
            .setId(NumberUtils.toLong(externalId, 1L))
            .setPlatformClientId(platformClient.getId())
            .setExternalId(externalId)
            .setUnits(storageUnits);
    }

    @Nonnull
    public OrderDto defaultOrder(
        PlatformClient platformClient,
        String externalId
    ) {
        int seed = NumberUtils.toInt(externalId, 1);
        return defaultOrder(
            platformClient,
            externalId,
            List.of(
                StorageUnitDto.builder()
                    .externalId("generated-root-" + externalId)
                    .type(StorageUnitType.ROOT)
                    .build(),
                StorageUnitDto.builder()
                    .externalId(externalId + "-1")
                    .type(StorageUnitType.PLACE)
                    .dimensions(
                        KorobyteDto.builder()
                            .weightGross(new BigDecimal(seed + ".123"))
                            .height(seed + 1)
                            .width(seed + 2)
                            .length(seed + 3)
                            .build()
                    )
                    .build()
            )
        );
    }

    @Nonnull
    public OrderSearchFilter searchFilter(PlatformClient platformClient, Set<String> externalIds) {
        return OrderSearchFilter.builder()
            .platformClientIds(Set.of(platformClient.getId()))
            .externalIds(externalIds)
            .build();
    }
}
