package ru.yandex.direct.mysql2grut.enummappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.autobudget.restart.service.Reason
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType
import ru.yandex.direct.core.entity.campaign.model.InternalCampaignRestrictionType
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.product.model.ProductType
import ru.yandex.direct.mysql2grut.enummappers.CampaignEnumMappers.Companion.autobudgetRestartReasonFromGrut
import ru.yandex.direct.mysql2grut.enummappers.CampaignEnumMappers.Companion.autobudgetRestartReasonToGrut
import ru.yandex.grut.objects.proto.CampaignPlatform
import ru.yandex.grut.objects.proto.CampaignV2
import ru.yandex.grut.objects.proto.CampaignV2.ERestrictionType
import ru.yandex.grut.objects.proto.CampaignV2.TCampaignV2Spec.TAutoBudgetRestart.ERestartReason
import ru.yandex.grut.objects.proto.Client.EProductType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CampaignEnumMappersTest : EnumMappersTestBase() {
    @Test
    fun checkCampaignTypeMapping() {
        testBase(
            CampaignType.values(),
            CampaignEnumMappers::toGrutCampaignType,
            CampaignV2.ECampaignType.CT_UNKNOWN,
            CampaignEnumMappers.CAMPAIGN_TYPE_BLACKLIST,
        )
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun grutRestartReasonValues() = ERestartReason.values()
            .filter { it != ERestartReason.RR_UNKNOWN }
            .toList()

        @JvmStatic
        @Suppress("unused")
        fun restartReasonValues() = Reason.values()
            // это значение нигде не используется
            .filter { it != Reason.CHANGED_STRATEGY_NAME }
            .toList()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("grutRestartReasonValues")
    fun autobudgetRestartReasonFromGrutTest(grutRestartReason: ERestartReason) {
        assertThat(autobudgetRestartReasonFromGrut(grutRestartReason.number)).isNotNull
    }

    @Test
    fun autobudgetRestartReasonFromGrutUnknownTest() {
        assertThat(autobudgetRestartReasonFromGrut(ERestartReason.RR_UNKNOWN.number)).isNull()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("restartReasonValues")
    fun autobudgetRestartReasonToGrutTest(reason: Reason) {
        assertThat(autobudgetRestartReasonToGrut(reason)).isNotEqualTo(ERestartReason.RR_UNKNOWN)
    }

    @Test
    fun checkProductTypeMapping() {
        testBase(
            ProductType.values(),
            CampaignEnumMappers::toGrutProductType,
            EProductType.PT_UNKNOWN,
            // TODO DIRECT-167882: вынести EProductType из клиента
            setOf(ProductType.AUTO_IMPORT),
        )
    }

    @Test
    fun checkRestrictionTypeToGrut() {
        testBase(
            InternalCampaignRestrictionType.values(),
            CampaignEnumMappers::restrictionTypeToGrut,
            ERestrictionType.RT_UNKNOWN
        )
    }

    @Test
    fun checkStrategyNameToGrut() {
        testBase(
            StrategyName.values(),
            CampaignEnumMappers::toGrutStrategyType,
            CampaignV2.TCampaignV2Spec.TStrategy.EStrategyType.ST_UNKNOWN,
            setOf(
                StrategyName.NO_PREMIUM,
                StrategyName.MIN_PRICE,
                StrategyName.AUTOBUDGET_MEDIA,
                StrategyName.AUTOBUDGET_WEEK_BUNDLE
            )
        )
    }

    @Test
    fun checkEshowsVideoTypeToGrut() {
        testBase(
            EshowsVideoType.values(),
            CampaignEnumMappers::eshowsVideoTypeToGrut,
            CampaignV2.EShowVideoType.EST_UNKNOWN,
        )
    }

    @Test
    fun checkCampaignPlatformToGrutMapping() {
        testBase(
            CampaignsPlatform.values(),
            CampaignEnumMappers::toGrutPlatform,
            CampaignPlatform.ECampaignPlatform.CP_UNKNOWN,
        )
    }
}
