package ru.yandex.direct.mysql2grut.enummappers

import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory
import ru.yandex.direct.dbschema.ppc.enums.AdgroupAdditionalTargetingsTargetingType
import ru.yandex.grut.objects.proto.AdGroupAdditionalTargeting
import ru.yandex.grut.objects.proto.AdGroupV2
import ru.yandex.grut.objects.proto.RelevanceMatchCategory.ERelevanceMatchCategory

class AdGroupEnumMappersTest : EnumMappersTestBase() {

    @Test
    fun mapSomeRelevanceMatchCategories() {
        var enumValue = AdGroupEnumMappers.toGrutRelevanceMatchCategory(RelevanceMatchCategory.exact_mark)
        softly.assertThat(enumValue).isEqualTo(ERelevanceMatchCategory.RMC_EXACT_MARK)

        enumValue = AdGroupEnumMappers.toGrutRelevanceMatchCategory(RelevanceMatchCategory.alternative_mark)
        softly.assertThat(enumValue).isEqualTo(ERelevanceMatchCategory.RMC_ALTERNATIVE_MARK)

        enumValue = AdGroupEnumMappers.toGrutRelevanceMatchCategory(RelevanceMatchCategory.broader_mark)
        softly.assertThat(enumValue).isEqualTo(ERelevanceMatchCategory.RMC_BROADER_MARK)
    }

    @Test
    fun checkRelevanceMatchCategoryMapping() {
        testBase(
            RelevanceMatchCategory.values(),
            AdGroupEnumMappers::toGrutRelevanceMatchCategory,
            ERelevanceMatchCategory.RMC_UNKNOWN,
        )
    }

    @Test
    fun checkAdGroupTypeMapping() {
        testBase(
            AdGroupType.values(),
            AdGroupEnumMappers::toGrutAdGroupType,
            AdGroupV2.EAdGroupType.AGT_UNKNOWN,
        )
    }

    @Test
    fun checkTargetingModeMapping() {
        testBase(
            AdGroupAdditionalTargetingMode.values(),
            AdGroupEnumMappers::toGrutTargetingMode,
            AdGroupAdditionalTargeting.TAdGroupAdditionalTargeting.ETargetingMode.TM_UNKNOWN
        )
    }

    @Test
    fun checkJoinTypeMapping() {
        testBase(
            AdGroupAdditionalTargetingJoinType.values(),
            AdGroupEnumMappers::toGrutJoinType,
            AdGroupAdditionalTargeting.TAdGroupAdditionalTargeting.EValueJoinType.JT_UNKNOWN
        )
    }

    @Test
    fun checkTargetingTypeMapping() {
        testBase(
            AdgroupAdditionalTargetingsTargetingType.values(),
            AdGroupEnumMappers::toGrutTargetingType,
            AdGroupAdditionalTargeting.TAdGroupAdditionalTargeting.ETargetingType.TT_UNKNOWN,
            setOf(AdgroupAdditionalTargetingsTargetingType.is_browser) // deprecated
        )
    }

    @Test
    fun checkInterfaceLanguageMapping() {
        testBase(
            InterfaceLang.values(),
            AdGroupEnumMappers::toGrutInterfaceLanguage,
            AdGroupAdditionalTargeting.TAdGroupAdditionalTargeting.EInterfaceLanguage.IL_UNKNOWN,
        )
    }

    @Test
    fun checkMobileDeviceTypeMapping() {
        testBase(
            MobileContentAdGroupDeviceTypeTargeting.values(),
            AdGroupEnumMappers::toGrutMobileDeviceType,
            AdGroupV2.TAdGroupV2Spec.TMobileContentDetails.EDeviceType.DV_UNKNOWN
        )
    }

    @Test
    fun checkMobileNetworkTypeMapping() {
        testBase(
            MobileContentAdGroupNetworkTargeting.values(),
            AdGroupEnumMappers::toGrutMobileNetworkType,
            AdGroupV2.TAdGroupV2Spec.TMobileContentDetails.ENetworkType.NT_UNKNOWN
        )
    }
}
