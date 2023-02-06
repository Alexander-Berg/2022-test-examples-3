package ru.yandex.market.ff.service.util;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.NonconformityType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.dto.registry.RegistryUnit;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitInfo;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;
import ru.yandex.market.ff.service.implementation.utils.RegistryUnitUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.ff.service.implementation.utils.RegistryUnitPredicates.UNITS_WITHOUT_SKU;

public class RegistryUnitUtilsTest {

    private static final RegistryUnitId BOX_UNIT_ID =
            new RegistryUnitId(Set.of(new UnitPartialId(RegistryUnitIdType.BOX_ID, "box1")));


    @Test
    void predicateTest() {
        Assertions.assertTrue(
                UNITS_WITHOUT_SKU.test(UnitCount.builder()
                        .type(UnitCountType.NON_COMPLIENT)
                        .nonconformityAttribute(NonconformityType.DEFECT)
                        .nonconformityAttribute(NonconformityType.UNKNOWN_SKU)
                        .build()));

        Assertions.assertFalse(
                UNITS_WITHOUT_SKU.test(UnitCount.builder()
                        .type(UnitCountType.NON_COMPLIENT)
                        .build()));

        Assertions.assertFalse(
                UNITS_WITHOUT_SKU.test(UnitCount.builder()
                        .type(UnitCountType.FIT)
                        .build()));
    }


    @Test
    void getParentCargoIdsSetTest() {

        RegistryUnitEntity registryUnitEntity = new RegistryUnitEntity();
        registryUnitEntity.setParentIds(List.of(BOX_UNIT_ID));

        Set<String> idsSet = RegistryUnitUtils.getParentCargoIdsSet(List.of(registryUnitEntity));
        assertTrue(idsSet.contains("box1"));

    }

    @Test
    void getParentCargoIdsSetWhenParentIdNullTest() {
        RegistryUnitEntity registryUnitEntity = new RegistryUnitEntity();
        registryUnitEntity.setParentIds(null);
        Set<String> idsSet = RegistryUnitUtils.getParentCargoIdsSet(List.of(registryUnitEntity));
        assertNotNull(idsSet);
    }

    @Test
    void getParentCargoIdsSetWhenParentIdEmptyTest() {
        RegistryUnitEntity registryUnitEntity = new RegistryUnitEntity();
        registryUnitEntity.setParentIds(List.of());
        Set<String> idsSet = RegistryUnitUtils.getParentCargoIdsSet(List.of(registryUnitEntity));
        assertNotNull(idsSet);
    }

    @Test
    void getParentCargoIdsSetWhenEmptySetTest() {
        Set<String> idsSet = RegistryUnitUtils.getParentCargoIdsSet(List.of());
        assertNotNull(idsSet);
    }

    @Test
    void getUndefinedItemSkuFullTest() {

        Supplier supplier = new Supplier();
        supplier.setId(10L);

        ShopRequest request = new ShopRequest();
        request.setSupplier(supplier);
        request.setServiceRequestId("555");

        RegistryUnitId unitId =
                new RegistryUnitId(Set.of(new UnitPartialId(RegistryUnitIdType.VIRTUAL_ID, "something")));

        RegistryUnitEntity registryUnitEntity = new RegistryUnitEntity();

        registryUnitEntity.setIdentifiers(unitId);

        registryUnitEntity.setParentIds(List.of(BOX_UNIT_ID));

        String itemSku = RegistryUnitUtils.getUndefinedItemSku(registryUnitEntity, request);

        assertEquals("10.555.box1.something", itemSku);
    }


    @Test
    void getUndefinedItemSkuEmptyIdentifiersTest() {

        Supplier supplier = new Supplier();
        supplier.setId(10L);

        ShopRequest request = new ShopRequest();
        request.setSupplier(supplier);
        request.setServiceRequestId("555");

        RegistryUnitId unitId =
                new RegistryUnitId(Set.of());

        RegistryUnitEntity registryUnitEntity = new RegistryUnitEntity();

        registryUnitEntity.setIdentifiers(unitId);

        registryUnitEntity.setParentIds(List.of(BOX_UNIT_ID));

        String itemSku = RegistryUnitUtils.getUndefinedItemSku(registryUnitEntity, request);

        assertEquals("10.555.box1", itemSku);
    }

    @Test
    void getUndefinedItemSkuEmptyParentIdsTest() {

        Supplier supplier = new Supplier();
        supplier.setId(10L);

        ShopRequest request = new ShopRequest();
        request.setSupplier(supplier);
        request.setServiceRequestId("555");

        RegistryUnitId unitId =
                new RegistryUnitId(Set.of());

        RegistryUnitEntity registryUnitEntity = new RegistryUnitEntity();

        registryUnitEntity.setIdentifiers(unitId);

        String itemSku = RegistryUnitUtils.getUndefinedItemSku(registryUnitEntity, request);

        assertEquals("10.555.-", itemSku);
    }

    @Test
    void getNonComplientCountWhenAcceptable() {

        UnitCount unitCount = UnitCount.builder().type(UnitCountType.NON_COMPLIENT)
                .nonconformityAttributes(List.of(
                        NonconformityType.MISMATCHING_DESCRIPTION,
                        NonconformityType.UNKNOWN_SKU,
                        NonconformityType.MISGRADING))
                .count(10)
                .build();

        assertEquals(10, RegistryUnitUtils.getNonComplientCount(List.of(unitCount)));
    }

    @Test
    void getNonComplientCountWhenUnacceptable() {

        UnitCount unitCount = UnitCount.builder().type(UnitCountType.NON_COMPLIENT)
                .nonconformityAttributes(List.of(
                        NonconformityType.MISMATCHING_DESCRIPTION,
                        NonconformityType.MISGRADING,
                        NonconformityType.NO_BARCODE))
                .count(10)
                .build();

        assertEquals(10, RegistryUnitUtils.getNonComplientCount(List.of(unitCount)));
    }

    @Test
    void getAcceptableCountWhenAllAcceptable() {

        UnitCount unitCount = UnitCount.builder().type(UnitCountType.NON_COMPLIENT)
                .nonconformityAttributes(List.of(
                        NonconformityType.MISMATCHING_DESCRIPTION,
                        NonconformityType.UNKNOWN_SKU,
                        NonconformityType.MISGRADING))
                .count(10)
                .build();

        assertEquals(10, RegistryUnitUtils.getAcceptableCount(List.of(unitCount)));
    }

    @Test
    void getAcceptableCountWhenOneAcceptable() {

        UnitCount unitCount = UnitCount.builder().type(UnitCountType.NON_COMPLIENT)
                .nonconformityAttributes(List.of(NonconformityType.MISGRADING))
                .count(10)
                .build();

        assertEquals(10, RegistryUnitUtils.getAcceptableCount(List.of(unitCount)));
    }

    @Test
    void getAcceptableCountWhenMixed() {

        UnitCount unitCount = UnitCount.builder().type(UnitCountType.NON_COMPLIENT)
                .nonconformityAttributes(List.of(
                        NonconformityType.MISMATCHING_DESCRIPTION,
                        NonconformityType.UNKNOWN_SKU,
                        NonconformityType.NO_BARCODE))
                .count(10)
                .build();

        assertEquals(0, RegistryUnitUtils.getAcceptableCount(List.of(unitCount)));
    }

    @Test
    void getAcceptableCountWhenOnlyUnacceptable() {

        UnitCount unitCount = UnitCount.builder().type(UnitCountType.NON_COMPLIENT)
                .nonconformityAttributes(List.of(
                        NonconformityType.INCORRECT_SERIAL_NUMBER,
                        NonconformityType.NO_LIFE_TIME,
                        NonconformityType.NO_BARCODE))
                .count(10)
                .build();

        assertEquals(0, RegistryUnitUtils.getAcceptableCount(List.of(unitCount)));
    }


    @Test
    void getRegistryUnitIdentifierValueOrThrow() {
        assertEquals("123", RegistryUnitUtils.getRegistryUnitIdentifierValueOrThrow(
                Set.of(UnitPartialId.builder()
                        .type(RegistryUnitIdType.SHOP_SKU)
                        .value("123")
                        .build()),
                RegistryUnitIdType.SHOP_SKU));

        assertThrows(RuntimeException.class, () -> RegistryUnitUtils.getRegistryUnitIdentifierValueOrThrow(
                Set.of(UnitPartialId.builder()
                        .type(RegistryUnitIdType.BOX_ID)
                        .value("123")
                        .build()),
                RegistryUnitIdType.SHOP_SKU));

    }

    @Test
    void getRegistryUnitIdentifierValue() {
        assertEquals(Optional.of("123"), RegistryUnitUtils.getRegistryUnitIdentifierValue(
                Set.of(UnitPartialId.builder()
                        .type(RegistryUnitIdType.SHOP_SKU)
                        .value("123")
                        .build()),
                RegistryUnitIdType.SHOP_SKU));

        assertEquals(Optional.empty(), RegistryUnitUtils.getRegistryUnitIdentifierValue(
                Set.of(UnitPartialId.builder()
                        .type(RegistryUnitIdType.BOX_ID)
                        .value("123")
                        .build()),
                RegistryUnitIdType.SHOP_SKU));
    }

    @Test
    void convertToSupplierSkuKey() {
        assertEquals(new SupplierSkuKey(123, "321"), RegistryUnitUtils.convertToSupplierSkuKey(
                RegistryUnit.builder()
                        .unitInfo(UnitInfo.builder()
                                .unitId(RegistryUnitId.builder()
                                        .part(UnitPartialId.builder()
                                                .type(RegistryUnitIdType.SHOP_SKU)
                                                .value("321")
                                                .build())
                                        .part(UnitPartialId.builder()
                                                .type(RegistryUnitIdType.VENDOR_ID)
                                                .value("123")
                                                .build())
                                        .build())
                                .build())
                        .build()));
    }

    @Test
    void getSumTest() {
        assertEquals(13, RegistryUnitUtils.getItemsSum(List.of(
                RegistryUnit.builder()
                        .type(RegistryUnitType.ITEM)
                        .unitInfo(UnitInfo.builder()
                                .unitCountsInfo(UnitCountsInfo.builder()
                                        .unitCount(UnitCount.builder()
                                                .type(UnitCountType.NON_COMPLIENT)
                                                .count(12)
                                                .build())
                                        .unitCount(UnitCount.builder()
                                                .type(UnitCountType.FIT)
                                                .count(13)
                                                .build())
                                        .build())
                                .build()).build()), cUnit -> cUnit.getType().equals(UnitCountType.FIT)));

    }


    @Test
    void getFitCountTest() {
        assertEquals(13, RegistryUnitUtils.getFitCount(
                RegistryUnit.builder().unitInfo(UnitInfo.builder()
                        .unitCountsInfo(UnitCountsInfo.builder()
                                .unitCount(UnitCount.builder()
                                        .type(UnitCountType.NON_COMPLIENT)
                                        .count(12)
                                        .build())
                                .unitCount(UnitCount.builder()
                                        .type(UnitCountType.FIT)
                                        .count(13)
                                        .build())
                                .build())
                        .build()).build()
        ));
    }

    @Test
    void checkGetPalletIdSuccess() {
        var palletId = RegistryUnitUtils.getPartialIdWithPalletIdTypeOptional(RegistryUnitId.of(
                new UnitPartialId(RegistryUnitIdType.PALLET_ID, "PALLET1")));

        assertTrue(palletId.isPresent());
        assertEquals("PALLET1", palletId.get().getValue());
    }

    @Test
    void checkGetPalletIdReturnNone() {
        var palletId = RegistryUnitUtils.getPartialIdWithPalletIdTypeOptional(RegistryUnitId.of(
                new UnitPartialId(RegistryUnitIdType.ORDER_RETURN_ID, "PALLET1")));

        assertFalse(palletId.isPresent());
    }
}
