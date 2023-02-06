package ru.yandex.direct.api.v5.entity.campaigns.service

import com.nhaarman.mockitokotlin2.mockingDetails
import com.nhaarman.mockitokotlin2.spy
import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignFieldEnum
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.api.v5.entity.campaigns.container.CampaignAnyFieldEnum
import ru.yandex.direct.api.v5.entity.campaigns.container.GetCampaignsContainer
import ru.yandex.direct.api.v5.entity.campaigns.container.toAnyFieldEnum
import ru.yandex.direct.api.v5.entity.campaigns.converter.CampaignsGetResponseConverter
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusWallet
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone
import ru.yandex.direct.core.entity.timetarget.model.GroupType
import ru.yandex.direct.core.testing.data.CLIENT_UID
import ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestMobileContentCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestSmartCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.libs.timetarget.TimeTargetUtils
import java.lang.reflect.Method
import java.math.BigDecimal
import java.time.ZoneId
import kotlin.reflect.KClass
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSuperclassOf

/**
 * Тест проверяет, что при конвертации полей
 * в [CampaignsGetResponseConverter.massConvertResponseItems]
 * используются данные только тех тайпсаппортов,
 * которые вернулись методом [CampaignDataFetcher.extractModelClassesToLoad] для этого поля
 *
 * Если тест падает - скорее всего нужно дополнить список тайпсаппортов
 * для поля в [CampaignDataFetcher.extractModelClassesToLoad]
 */
@Api5Test
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CampaignDataFetcherConsistencyTest @Autowired constructor(
    private val steps: Steps,
    private val campaignsGetResponseConverter: CampaignsGetResponseConverter,
    private val campaignDataFetcher: CampaignDataFetcher,
) {

    private val contentPromotionCampaign = steps
        .contentPromotionCampaignSteps()
        .createCampaign(
            TestContentPromotionCampaigns
                .fullContentPromotionCampaign()
                .withTimeTarget(TimeTargetUtils.timeTarget24x7())
        )
        .typedCampaign

    private val cpmBannerCampaign = steps
        .cpmBannerCampaignSteps()
        .createCampaign(
            TestCpmBannerCampaigns
                .fullCpmBannerCampaign()
                .withTimeTarget(TimeTargetUtils.timeTarget24x7())
        )
        .typedCampaign

    private val dynamicCampaign = steps
        .dynamicCampaignSteps()
        .createCampaign(
            TestDynamicCampaigns
                .fullDynamicCampaign()
                .withTimeTarget(TimeTargetUtils.timeTarget24x7())
        )
        .typedCampaign

    private val mobileContentCampaign = steps
        .mobileContentCampaignSteps()
        .createCampaign(
            TestMobileContentCampaigns
                .fullMobileContentCampaign(0)
                .withTimeTarget(TimeTargetUtils.timeTarget24x7())
        )
        .typedCampaign

    private val smartCampaign = steps
        .smartCampaignSteps()
        .createCampaign(
            TestSmartCampaigns
                .fullSmartCampaign()
                .withTimeTarget(TimeTargetUtils.timeTarget24x7())
        )
        .typedCampaign

    private val textCampaign = steps
        .textCampaignSteps()
        .createCampaign(
            TestTextCampaigns
                .fullTextCampaign()
                .withTimeTarget(TimeTargetUtils.timeTarget24x7())
        )
        .typedCampaign

    @BeforeEach
    internal fun setUp() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(clientInfo.clientId)
    }

    @ParameterizedTest
    @EnumSource(ContentPromotionCampaignFieldEnum::class)
    fun `content promotion campaign fields are handled correctly`(fieldName: ContentPromotionCampaignFieldEnum) {
        doTest(
            campaign = contentPromotionCampaign,
            fieldName = fieldName.toAnyFieldEnum(),
        )
    }

    @ParameterizedTest
    @EnumSource(CpmBannerCampaignFieldEnum::class)
    fun `cpm banner campaign fields are handled correctly`(fieldName: CpmBannerCampaignFieldEnum) {
        doTest(
            campaign = cpmBannerCampaign,
            fieldName = fieldName.toAnyFieldEnum(),
        )
    }

    @ParameterizedTest
    @EnumSource(DynamicTextCampaignFieldEnum::class)
    fun `dynamic campaign fields are handled correctly`(fieldName: DynamicTextCampaignFieldEnum) {
        doTest(
            campaign = dynamicCampaign,
            fieldName = fieldName.toAnyFieldEnum(),
        )
    }

    @ParameterizedTest
    @EnumSource(MobileAppCampaignFieldEnum::class)
    fun `mobile app campaign fields are handled correctly`(fieldName: MobileAppCampaignFieldEnum) {
        doTest(
            campaign = mobileContentCampaign,
            fieldName = fieldName.toAnyFieldEnum(),
        )
    }

    @ParameterizedTest
    @EnumSource(SmartCampaignFieldEnum::class)
    fun `smart campaign fields are handled correctly`(fieldName: SmartCampaignFieldEnum) {
        doTest(
            campaign = smartCampaign,
            fieldName = fieldName.toAnyFieldEnum(),
        )
    }

    @ParameterizedTest
    @EnumSource(TextCampaignFieldEnum::class)
    fun `text campaign fields are handled correctly`(fieldName: TextCampaignFieldEnum) {
        doTest(
            campaign = textCampaign,
            fieldName = fieldName.toAnyFieldEnum(),
        )
    }

    @ParameterizedTest
    @EnumSource(CampaignFieldEnum::class)
    fun `common campaign fields are handled correctly`(fieldName: CampaignFieldEnum) {
        doTest(
            campaign = textCampaign,
            fieldName = fieldName.toAnyFieldEnum(),
        )
    }

    private inline fun <reified C : CommonCampaign> doTest(
        campaign: C,
        fieldName: CampaignAnyFieldEnum,
    ) {
        val availableTypeSupports: Collection<KClass<*>> = campaignDataFetcher
            .extractModelClassesToLoad(fieldName)
        val availableGetters = availableTypeSupports
            .filter { it.isSuperclassOf(C::class) }
            .flatMap { it.functions }
            .map { it.name }
            .filter { it.startsWith("get") }

        val campaignSpy = spy(campaign)
        val container = createContainer(campaignSpy, fieldName)
        campaignsGetResponseConverter
            .massConvertResponseItems(setOf(container))

        val usedGetters: Collection<Method> = mockingDetails(campaignSpy)
            .invocations
            .mapTo(mutableSetOf()) { it.method }
        softly {
            for (getter in usedGetters) {
                assertThat(getter.name)
                    .describedAs("Поле $getter не загружалось из БД, " +
                        "но при этом использовалось для вычисления поля $fieldName")
                    .isIn(availableGetters)
            }
        }
    }

    private fun createContainer(
        campaign: CommonCampaign,
        fieldName: CampaignAnyFieldEnum,
    ) = GetCampaignsContainer(
        campaign = campaign,
        requestedFields = setOf(fieldName),
        clientUid = CLIENT_UID,
        ndsRatioSupplier = { BigDecimal.ZERO },
        sumForTransferSupplier = { 300.toBigDecimal() },
        managerFioSupplier = { "A" },
        agencyNameSupplier = { "B" },
        timezoneSupplier = { AMSTERDAM_TIMEZONE },
        aggregatedStatusWalletSupplier = {
            AggregatedStatusWallet()
                .withSum(300.toBigDecimal())
        },
        walletSupplier = { Campaign().withSum(300.toBigDecimal()) },
        queueOperationsSupplier = { emptySet() },
        hasBannersSupplier = { true },
        hasActiveBannersSupplier = { true },
        advancedGeoTargetingSupplier = { true },
    )

    private companion object {
        private val AMSTERDAM_TIMEZONE = GeoTimezone()
            .withTimezone(ZoneId.of("Europe/Amsterdam"))
            .withTimezoneId(174L)
            .withGroupType(GroupType.WORLD)
    }
}
