package ru.yandex.market.mbi.datacamp.saas.impl.util;

import java.util.List;

import Market.DataCamp.DataCampUnitedOffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.saas.impl.DataCampSaasConversions;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;

class SaasConverterTest {

    @Test
    void testConvertOfferInfo() {
        SaasOfferInfo saasOfferInfo = SaasOfferInfo
                .newBuilder()
                .addOfferId("vacuumCleanerLg")
                .addVendors(List.of("LG"))
                .addCategoryIds(List.of(4321L))
                .setName("пылесос")
                .setVariantId("123456")
                .setMbocConsistency(true)
                .build();

        DataCampUnitedOffer.UnitedOffer expectedUnitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/SaasConverterTest.filledUnitedOffer.json",
                getClass()
        );

        DataCampUnitedOffer.UnitedOffer convertedOffer = DataCampSaasConversions.toUnitedOffer(saasOfferInfo);
        ReflectionAssert.assertReflectionEquals(expectedUnitedOffer, convertedOffer);
    }

    @Test
    @DisplayName("При конвертации в единый оффер создаются только непустые поля")
    void testConvertAlmostEmptyOfferInfo() {
        SaasOfferInfo saasOfferInfo = SaasOfferInfo
                .newBuilder()
                .addOfferId("vacuumCleanerLg")
                .addCategoryIds(List.of(4321L))
                .build();

        DataCampUnitedOffer.UnitedOffer expectedUnitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/SaasConverterTest.almostEmptyUnitedOffer.json",
                getClass()
        );

        DataCampUnitedOffer.UnitedOffer convertedOffer = DataCampSaasConversions.toUnitedOffer(saasOfferInfo);
        ReflectionAssert.assertReflectionEquals(expectedUnitedOffer, convertedOffer);
    }
}
