package ru.yandex.market.ff.service.implementation.additionalSupplies;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.dto.CreateAnomalyShadowWithdrawRequestDTO;
import ru.yandex.market.ff.model.dto.PutWithdrawRequestWithRegistryDTO;
import ru.yandex.market.ff.model.dto.registry.RegistryUnit;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitInfo;
import ru.yandex.market.ff.model.dto.registry.UnitMeta;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.AdditionalSuppliesService;
import ru.yandex.market.ff.service.ItemNameService;
import ru.yandex.market.ff.service.RequestRelationService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.registry.RegistryChainStateService;
import ru.yandex.market.ff.service.registry.converter.DefaultRegistryUnitConverter;
import ru.yandex.market.ff.service.registry.converter.ff.FFOutboundRegistryEntityToRegistryConverter;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.common.model.common.RegistryType.PLANNED_ANOMALY_WITHDRAWAL;

class AdditionalSuppliesServiceImplTest {

    private RegistryUnit knownSkuAnomaly = RegistryUnit.builder()
            .type(RegistryUnitType.ITEM)
            .unitInfo(UnitInfo.builder()
                    .unitId(RegistryUnitId.of(
                            UnitPartialId.builder()
                                    .value("sku1")
                                    .type(RegistryUnitIdType.SHOP_SKU)
                                    .build(),
                            UnitPartialId.builder()
                                    .value("cons1")
                                    .type(RegistryUnitIdType.CONSIGNMENT_ID)
                                    .build(),
                            UnitPartialId.builder()
                                    .value("vend1")
                                    .type(RegistryUnitIdType.VENDOR_ID)
                                    .build()
                    ))
                    .unitCountsInfo(UnitCountsInfo.builder()
                            .unitCount(UnitCount.builder()
                                    .count(1)
                                    .type(UnitCountType.NON_COMPLIENT)
                                    .build())
                            .build())
                    .build())
            .unitMeta(UnitMeta.builder().build())
            .build();

    private RegistryUnit unknownSkuAnomaly = RegistryUnit.builder()
            .type(RegistryUnitType.ITEM)
            .unitInfo(UnitInfo.builder()
                    .unitId(RegistryUnitId.of(
                            UnitPartialId.builder()
                                    .value("cons2")
                                    .type(RegistryUnitIdType.CONSIGNMENT_ID)
                                    .build(),
                            UnitPartialId.builder()
                                    .value("vend1")
                                    .type(RegistryUnitIdType.VENDOR_ID)
                                    .build()
                    ))
                    .unitCountsInfo(UnitCountsInfo.builder()
                            .unitCount(UnitCount.builder()
                                    .count(1)
                                    .type(UnitCountType.NON_COMPLIENT)
                                    .build())
                            .build())
                    .build())
            .unitMeta(UnitMeta.builder().build())
            .build();
    private int myPublicSubtypeId = 123;


    @Test
    void generatePutWithdraw() {
        ShopRequestFetchingService shopRequestFetchingService = Mockito.mock(ShopRequestFetchingService.class);
        RegistryChainStateService registryChainStateService = Mockito.mock(RegistryChainStateService.class);
        RequestSubTypeService subTypeService = mock(RequestSubTypeService.class);
        ItemNameService itemNameService = mock(ItemNameService.class);

        AdditionalSuppliesService aSs = new AdditionalSuppliesServiceImpl(
                shopRequestFetchingService,
                null,
                null,
                registryChainStateService,
                null,
                null,
                new FFOutboundRegistryEntityToRegistryConverter(),
                new DefaultRegistryUnitConverter(),
                subTypeService,
                itemNameService,
                Mockito.mock(RequestRelationService.class)
        );

        AdditionalSuppliesService mock = Mockito.spy(aSs);

        doNothing().when(mock).doSomeSpecificAddSupplyValidation(anyLong());

        when(shopRequestFetchingService.getRequestOrThrow(anyLong())).thenReturn(new ShopRequest());
        RequestSubTypeEntity subTypeEntity = mock(RequestSubTypeEntity.class);
        when(subTypeEntity.getId()).thenReturn(myPublicSubtypeId);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(subTypeEntity);

        List<RegistryUnit> whatsLeftInAnomalies = List.of(
                knownSkuAnomaly,
                unknownSkuAnomaly
        );

        when(registryChainStateService.getFinalStateOfRegistryChain(anyLong(), any())).thenReturn(whatsLeftInAnomalies);

        CreateAnomalyShadowWithdrawRequestDTO request = new CreateAnomalyShadowWithdrawRequestDTO();
        request.setSupplyRequestId(123L);

        PutWithdrawRequestWithRegistryDTO result = mock.generatePutWithdraw(request);

        Assertions.assertEquals(result.getOutboundHeader().getRequestType(), myPublicSubtypeId);
        Assertions.assertEquals(result.getOutboundRegistry().getOutboundRegistry().getRegistryType(),
                PLANNED_ANOMALY_WITHDRAWAL);

        assertThat(result.getOutboundRegistry().getOutboundRegistry().getItems(),
                containsInAnyOrder(
                        getRegistryIdentifiedItemMatcher(),
                        getRegistryUnidentifiedItemMatcher()
                ));

    }

    @NotNull
    private Matcher<RegistryItem> getRegistryUnidentifiedItemMatcher() {
        return allOf(
                hasGraph("unitInfo.counts",
                        contains(allOf(
                                hasGraph("quantity", equalTo(1)),
                                hasGraph("countType.name", equalTo("NON_COMPLIENT"))
                        ))
                ),
                hasGraph("unitInfo.compositeId.partialIds",
                        containsInAnyOrder(
                                allOf(
                                        hasGraph("value", equalTo("cons2")),
                                        hasGraph("idType.name", equalTo("CONSIGNMENT_ID"))
                                ),
                                allOf(
                                        hasGraph("value", equalTo("vend1")),
                                        hasGraph("idType.name", equalTo("VENDOR_ID"))
                                )
                        )
                )
        );
    }

    @NotNull
    private Matcher<RegistryItem> getRegistryIdentifiedItemMatcher() {
        return allOf(
                hasGraph("unitInfo.counts",
                        contains(allOf(
                                hasGraph("quantity", equalTo(1)),
                                hasGraph("countType.name", equalTo("NON_COMPLIENT"))
                        ))
                ),
                hasGraph("unitInfo.compositeId.partialIds",
                        containsInAnyOrder(
                                allOf(hasGraph("value", equalTo("cons1")),
                                        hasGraph("idType.name", equalTo("CONSIGNMENT_ID"))
                                ),
                                allOf(hasGraph("value", equalTo("sku1")),
                                        hasGraph("idType.name", equalTo("ARTICLE"))
                                ),
                                allOf(hasGraph("value", equalTo("vend1")),
                                        hasGraph("idType.name", equalTo("VENDOR_ID"))
                                )
                        )
                )
        );
    }

    private static <T> Matcher<T> hasGraph(String graphPath, Matcher<?> matcher) {
        List<String> properties = Arrays.asList(graphPath.split("\\."));
        ListIterator<String> iterator =
                properties.listIterator(properties.size());

        Matcher<?> ret = matcher;
        Matcher<T> result = null;
        while (iterator.hasPrevious()) {
            String previous = iterator.previous();
            if (iterator.hasPrevious()) {
                ret = hasProperty(previous, ret);
            } else {
                result = hasProperty(previous, ret);
            }
        }
        return result;
    }
}
