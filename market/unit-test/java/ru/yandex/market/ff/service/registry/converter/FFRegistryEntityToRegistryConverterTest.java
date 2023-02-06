package ru.yandex.market.ff.service.registry.converter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.RegistryFlowType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.dto.KorobyteDto;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.RelatedUnitIds;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitMeta;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.registry.RegistryEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;
import ru.yandex.market.ff.service.registry.converter.ff.FFInboundRegistryEntityToRegistryConverter;
import ru.yandex.market.ff.service.registry.converter.ff.FFOutboundRegistryEntityToRegistryConverter;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryBox;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryPallet;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.UnitRelation;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.IdTemplate;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.ff.model.dto.registry.RegistryUnitId.of;

class FFRegistryEntityToRegistryConverterTest {

    @Test
    void convertToInboundRegistry() {
        FFInboundRegistryEntityToRegistryConverter ffInboundRegistryEntityToRegistryConverter =
                new FFInboundRegistryEntityToRegistryConverter();
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);

        RegistryEntity entity = getValidEntity();

        InboundRegistry inboundRegistry = ffInboundRegistryEntityToRegistryConverter.convertToRegistry(
                shopRequest,
                entity,
                entity.getRegistryUnits(),
                false
        );

        InboundRegistry inboundRegistryValid = getValidInbound();

        assertEquals(inboundRegistry, inboundRegistryValid);
    }

    @Test
    void convertToOutboundRegistry() {
        FFOutboundRegistryEntityToRegistryConverter ffOutboundRegistryEntityToRegistryConverter =
                new FFOutboundRegistryEntityToRegistryConverter();
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);

        RegistryEntity entity = getValidEntity();

        OutboundRegistry outboundRegistry = ffOutboundRegistryEntityToRegistryConverter.convertToRegistry(
                shopRequest,
                entity,
                entity.getRegistryUnits(),
                false
        );

        OutboundRegistry outboundRegistryValid = getValidOutbound();

        assertEquals(outboundRegistry, outboundRegistryValid);
    }

    @Test
    void convertToInboundRegistryWithoutPallet() {
        FFInboundRegistryEntityToRegistryConverter ffInboundRegistryEntityToRegistryConverter =
                new FFInboundRegistryEntityToRegistryConverter();
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);

        RegistryEntity registryEntity = new RegistryEntity();
        registryEntity.setId(12L);
        registryEntity.setType(RegistryFlowType.PLANNED_RETURNS);

        RegistryUnitEntity registryUnitEntityBox = getBox();
        RegistryUnitEntity registryUnitEntityItem = getPallet();
        registryEntity.setRegistryUnits(List.of(
                registryUnitEntityBox,
                registryUnitEntityItem
        ));

        InboundRegistry inboundRegistry = ffInboundRegistryEntityToRegistryConverter.convertToRegistry(
                shopRequest,
                registryEntity,
                registryEntity.getRegistryUnits(),
                false
        );


        RegistryBox registryBox = getRegistryBox();

        RegistryPallet registryPallet = getRegistryPallet();
        InboundRegistry inboundRegistryValid = InboundRegistry.builder(
                ResourceId.builder().setYandexId("12").build(),
                ResourceId.builder().setYandexId("1").build(),
                RegistryType.PLANNED_RETURNS
        )
                .setPallets(List.of(registryPallet))
                .setBoxes(List.of(registryBox))
                .setItems(Collections.emptyList()).build();

        assertEquals(inboundRegistry, inboundRegistryValid);
    }

    @Test
    void convertToInboundRegistryWithoutItem() {
        FFInboundRegistryEntityToRegistryConverter ffInboundRegistryEntityToRegistryConverter =
                new FFInboundRegistryEntityToRegistryConverter();
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);

        RegistryEntity registryEntity = new RegistryEntity();
        registryEntity.setId(12L);
        registryEntity.setType(RegistryFlowType.PLANNED_RETURNS);

        RegistryUnitEntity registryUnitEntityBox = getBox();
        RegistryUnitEntity registryUnitEntityItem = getItem();
        registryEntity.setRegistryUnits(List.of(
                registryUnitEntityBox,
                registryUnitEntityItem
        ));

        InboundRegistry inboundRegistry = ffInboundRegistryEntityToRegistryConverter.convertToRegistry(
                shopRequest,
                registryEntity,
                registryEntity.getRegistryUnits(),
                false
        );


        RegistryBox registryBox = getRegistryBox();

        RegistryItem registryItem = getRegistryItem();

        InboundRegistry inboundRegistryValid = InboundRegistry.builder(
                ResourceId.builder().setYandexId("12").build(),
                ResourceId.builder().setYandexId("1").build(),
                RegistryType.PLANNED_RETURNS
        )
                .setBoxes(List.of(registryBox))
                .setItems(List.of(registryItem))
                .build();

        assertEquals(inboundRegistry, inboundRegistryValid);
    }


    private RegistryEntity getValidEntity() {
        RegistryEntity registryEntity = new RegistryEntity();
        registryEntity.setId(12L);
        registryEntity.setType(RegistryFlowType.PLANNED_RETURNS);

        RegistryUnitEntity registryUnitEntityPallet = getPallet();
        RegistryUnitEntity registryUnitEntityBox = getBox();
        RegistryUnitEntity registryUnitEntityItem = getItem();
        registryEntity.setRegistryUnits(List.of(
                registryUnitEntityBox,
                registryUnitEntityPallet,
                registryUnitEntityItem
        ));
        return registryEntity;
    }

    private RegistryUnitEntity getItem() {
        RegistryUnitEntity registryUnitEntityItem = RegistryUnitEntity.builder()
                .type(RegistryUnitType.ITEM)
                .registryId(1L)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.SHOP_SKU, "SHOP_SKU0002",
                        RegistryUnitIdType.VENDOR_ID, "456123"
                ))
                .parentIds(List.of(RegistryUnitId.of(RegistryUnitIdType.BOX_ID, "VOZRAT")))
                .unitCountsInfo(UnitCountsInfo.of(
                        UnitCount.of(UnitCountType.FIT, 1, RelatedUnitIds.asOrphan(
                                of(RegistryUnitIdType.CIS, "CIS0002", RegistryUnitIdType.CIS, "CIS0001")
                        ), null),
                        null
                ))
                .meta(UnitMeta.builder()
                        .description("Some item")
                        .snMask("SNMASK")
                        .imeiMask("IMEIMASK")
                        .checkImei(1)
                        .checkSn(1)
                        .supplyPrice(BigDecimal.TEN)
                        .korobyte(KorobyteDto.builder()
                                .height(BigDecimal.TEN)
                                .length(BigDecimal.ONE)
                                .width(BigDecimal.ONE)
                                .build()).build())
                .build();
        return registryUnitEntityItem;
    }

    private RegistryUnitEntity getBox() {
        RegistryUnitEntity registryUnitEntityBox = RegistryUnitEntity.builder()
                .type(RegistryUnitType.BOX)
                .meta(UnitMeta.builder().description("Some box").build())
                .registryId(1L)
                .identifiers(new RegistryUnitId(Set.of(new UnitPartialId(RegistryUnitIdType.BOX_ID, "VOZRAT"))))
                .unitCountsInfo(UnitCountsInfo.of(
                        UnitCount.of(UnitCountType.FIT, 1),
                        null
                ))
                .build();
        return registryUnitEntityBox;
    }

    private RegistryUnitEntity getPallet() {
        RegistryUnitEntity registryUnitEntityPallet = RegistryUnitEntity.builder()
                .type(RegistryUnitType.PALLET)
                .registryId(1L)
                .meta(UnitMeta.builder().description("Some pallet").build())
                .identifiers(new RegistryUnitId(Set.of(new UnitPartialId(RegistryUnitIdType.PALLET_ID, "PALLET"))))
                .unitCountsInfo(UnitCountsInfo.of(
                        UnitCount.of(UnitCountType.FIT, 1),
                        RelatedUnitIds.asOrphan(of(RegistryUnitIdType.BOX_ID, "VOZRAT"))
                ))
                .build();
        return registryUnitEntityPallet;
    }

    private InboundRegistry getValidInbound() {

        RegistryPallet registryPallet = getRegistryPallet();

        RegistryBox registryBox = getRegistryBox();

        RegistryItem registryItem = getRegistryItem();
        return InboundRegistry.builder(
                ResourceId.builder().setYandexId("12").build(),
                ResourceId.builder().setYandexId("1").build(),
                RegistryType.PLANNED_RETURNS
        )
                .setPallets(List.of(registryPallet))
                .setBoxes(List.of(registryBox))
                .setItems(List.of(registryItem)).build();
    }

    private OutboundRegistry getValidOutbound() {

        RegistryPallet registryPallet = getRegistryPallet();

        RegistryBox registryBox = getRegistryBox();

        RegistryItem registryItem = getRegistryItem();
        return OutboundRegistry.builder(
                ResourceId.builder().setYandexId("12").build(),
                ResourceId.builder().setYandexId("1").build(),
                RegistryType.PLANNED_RETURNS
        )
                .setPallets(List.of(registryPallet))
                .setBoxes(List.of(registryBox))
                .setItems(List.of(registryItem)).build();
    }

    private RegistryItem getRegistryItem() {
        ru.yandex.market.logistic.gateway.common.model.common.UnitCount buildItem =
                new ru.yandex.market.logistic.gateway.common.model.common.UnitCount.UnitCountBuilder().setCountType(
                        ru.yandex.market.logistic.gateway.common.model.common.UnitCountType.FIT)
                        .setUnitIds(List.of(new CompositeId(List.of(
                                new PartialId(PartialIdType.CIS, "CIS0002"),
                                new PartialId(PartialIdType.CIS, "CIS0001")
                                ))
                        ))
                        .setQuantity(1)
                        .setNonconformityAttributes(Collections.emptyList())
                        .build();

        ru.yandex.market.logistic.gateway.common.model.common.UnitInfo someItem =
                new ru.yandex.market.logistic.gateway.common.model.common.UnitInfo(
                        List.of(buildItem),
                        new CompositeId(List.of(
                                new PartialId(PartialIdType.ARTICLE, "SHOP_SKU0002"),
                                new PartialId(PartialIdType.VENDOR_ID, "456123")
                        )),
                        List.of(new UnitRelation(new CompositeId(List.of(new PartialId(
                                PartialIdType.BOX_ID,
                                "VOZRAT"
                        ))))),
                        new Korobyte(BigDecimal.ONE.intValue(),
                                BigDecimal.TEN.intValue(),
                                BigDecimal.ONE.intValue(),
                                null,
                                null,
                                null),
                        "Some item"
                );

        RegistryItem registryItem = RegistryItem.builder(someItem)
                .setSnTemplate(new IdTemplate(1, "SNMASK"))
                .setImeiTemplate(new IdTemplate(1, "IMEIMASK"))
                .setPrice(BigDecimal.TEN)
                .build();
        return registryItem;
    }

    @NotNull
    private RegistryBox getRegistryBox() {
        ru.yandex.market.logistic.gateway.common.model.common.UnitCount build =
                new ru.yandex.market.logistic.gateway.common.model.common.UnitCount.UnitCountBuilder().setCountType(
                        ru.yandex.market.logistic.gateway.common.model.common.UnitCountType.FIT)
                        .setQuantity(1)
                        .setUnitIds(Collections.emptyList())
                        .setNonconformityAttributes(Collections.emptyList())
                        .build();
        ru.yandex.market.logistic.gateway.common.model.common.UnitInfo someBox =
                new ru.yandex.market.logistic.gateway.common.model.common.UnitInfo(
                        List.of(build),
                        new CompositeId(List.of(
                                new PartialId(PartialIdType.BOX_ID, "VOZRAT")
                        )),
                        Collections.emptyList(),
                        null,
                        "Some box"
                );
        RegistryBox registryBox = new RegistryBox(someBox
        );
        return registryBox;
    }

    @NotNull
    private RegistryPallet getRegistryPallet() {
        ru.yandex.market.logistic.gateway.common.model.common.UnitCount build =
                new ru.yandex.market.logistic.gateway.common.model.common.UnitCount.UnitCountBuilder().setCountType(
                        ru.yandex.market.logistic.gateway.common.model.common.UnitCountType.FIT)
                        .setQuantity(1)
                        .setUnitIds(Collections.emptyList())
                        .setNonconformityAttributes(Collections.emptyList())
                        .build();
        ru.yandex.market.logistic.gateway.common.model.common.UnitInfo somePallet =
                new ru.yandex.market.logistic.gateway.common.model.common.UnitInfo(
                        List.of(build),
                        new CompositeId(List.of(
                                new PartialId(PartialIdType.PALLET_ID, "PALLET")
                        )),
                        Collections.emptyList(),
                        null,
                        "Some pallet"
                );
        RegistryPallet registryPallet = new RegistryPallet(somePallet
        );
        return registryPallet;
    }
}
