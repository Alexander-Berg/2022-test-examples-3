package ru.yandex.direct.mysql2grut.enummappers

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.bidmodifier.AgeType
import ru.yandex.direct.core.entity.bidmodifier.BannerType
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifier.GenderType
import ru.yandex.direct.core.entity.bidmodifier.InventoryType
import ru.yandex.direct.core.entity.bidmodifier.OperationType
import ru.yandex.direct.core.entity.bidmodifier.OsType
import ru.yandex.direct.core.entity.bidmodifier.TabletOsType
import ru.yandex.direct.core.entity.bidmodifier.TrafaretPosition
import ru.yandex.direct.core.entity.bidmodifier.WeatherType
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator
import ru.yandex.direct.dbschema.ppc.enums.HierarchicalMultipliersType
import ru.yandex.grut.auxiliary.proto.YabsOperation
import ru.yandex.grut.objects.proto.AgeGroup
import ru.yandex.grut.objects.proto.BidModifier
import ru.yandex.grut.objects.proto.Gender
import ru.yandex.grut.objects.proto.MobilePlatform

class BidModifierEnumMappersTest : EnumMappersTestBase() {

    @Test
    fun checkBidModifierTypeConverter() {
        testBase(
            HierarchicalMultipliersType.values(),
            BidModifierEnumMappers::toGrut,
            BidModifier.EBidModifierType.MLT_UNKNOWN,
            // Не нашел такие корректировки в проде и на тестинге, во внешнем апи их тоже нет
            setOf(
                HierarchicalMultipliersType.distance_multiplier,
                HierarchicalMultipliersType.traffic_multiplier,
                HierarchicalMultipliersType.express_traffic_multiplier,
                HierarchicalMultipliersType.express_content_duration_multiplier,
            )
        )
    }

    @Test
    fun checkBidModifierServiceTypeConverter() {
        val hierarchicalMultiplierTypes = BidModifierType.values()
            .map { BidModifierType.toSource(it)!! }
            .toTypedArray()

        testBase(
            hierarchicalMultiplierTypes,
            BidModifierEnumMappers::toGrut,
            BidModifier.EBidModifierType.MLT_UNKNOWN,
            // Не нашел такие корректировки в проде и на тестинге, во внешнем апи их тоже нет
            setOf(
                HierarchicalMultipliersType.distance_multiplier,
                HierarchicalMultipliersType.traffic_multiplier,
                HierarchicalMultipliersType.express_traffic_multiplier,
                HierarchicalMultipliersType.express_content_duration_multiplier,
            )
        )
    }

    @Test
    fun testOsTypeMapper() {
        testBase(
            OsType.values(),
            BidModifierEnumMappers::toGrut,
            MobilePlatform.EMobilePlatform.MP_UNKNOWN,
        )
    }

    @Test
    fun testTabletOsTypeMapper() {
        testBase(
            TabletOsType.values(),
            BidModifierEnumMappers::toGrut,
            MobilePlatform.EMobilePlatform.MP_UNKNOWN,
        )
    }

    @Test
    fun testGenderMapper() {
        testBase(
            GenderType.values(),
            BidModifierEnumMappers::toGrut,
            Gender.EGender.G_UNKNOWN,
        )
        val nullGender: GenderType? = null
        Assertions.assertEquals(BidModifierEnumMappers.toGrut(nullGender), Gender.EGender.G_UNKNOWN)
    }

    @Test
    fun testAgeType() {
        testBase(
            AgeType.values(),
            BidModifierEnumMappers::toGrut,
            AgeGroup.EAgeGroup.AG_UNKNOWN,
            skipValues = setOf(AgeType._45_, AgeType.UNKNOWN), // больше не используются https://st.yandex-team.ru/DIRECT-76518
        )
        val nullAge: AgeType? = null
        Assertions.assertEquals(BidModifierEnumMappers.toGrut(nullAge), AgeGroup.EAgeGroup.AG_UNKNOWN)
    }


    @Test
    fun testInventoryTypeMapper() {
        testBase(
            InventoryType.values(),
            BidModifierEnumMappers::toGrut,
            ru.yandex.grut.objects.proto.InventoryType.EInventoryType.IT_UNKNOWN,
            setOf(
                InventoryType.INROLL_OVERLAY,
                InventoryType.INROLL,
                InventoryType.INTERSTITIAL,
                InventoryType.PREROLL,
                InventoryType.OVERLAY,
                InventoryType.FULLSCREEN,
                InventoryType.PAUSEROLL,
                InventoryType.POSTROLL_OVERLAY,
                InventoryType.POSTROLL_WRAPPER,
                InventoryType.MIDROLL,
                InventoryType.POSTROLL,
            )
        )
    }

    @Test
    fun testOperationTypeMapper() {
        testBase(
            OperationType.values(),
            BidModifierEnumMappers::toGrut,
            YabsOperation.EYabsOperation.YO_UNKNOWN,
        )
    }

    @Test
    fun testBinaryOperator() {
        testBase(
            BidModifierExpressionOperator.values(),
            BidModifierEnumMappers::toGrut,
            YabsOperation.EYabsOperation.YO_UNKNOWN,
        )
    }

    @Test
    fun testTrafaretPoisition() {
        testBase(
            TrafaretPosition.values(),
            BidModifierEnumMappers::toGrut,
            BidModifier.TBidModifierSpec.EPosition.POS_UNKNOWN,
        )
    }

    @Test
    fun testWeatherParameterMapper() {
        testBase(
            WeatherType.values(),
            BidModifierEnumMappers::toGrut,
            BidModifier.TBidModifierSpec.EWeatherParameter.WT_UNKNOWN,
        )
    }

    @Test
    fun testBannerType() {
        testBase(
            BannerType.values(),
            BidModifierEnumMappers::toGrut,
            ru.yandex.grut.objects.proto.InventoryType.EInventoryType.IT_UNKNOWN,
            setOf(BannerType.CPM_OUTDOOR, BannerType.CPM_VIDEO),
        )
    }

}
