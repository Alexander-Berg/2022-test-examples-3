package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.adv.direct.expression.keywords.KeywordEnum
import ru.yandex.adv.direct.expression.operations.OperationEnum
import ru.yandex.adv.direct.expression2.TargetingExpression
import ru.yandex.adv.direct.expression2.TargetingExpressionAtom
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.additionaltargetings.repository.CampAdditionalTargetingsRepository
import ru.yandex.direct.core.entity.additionaltargetings.repository.ClientAdditionalTargetingsRepository
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings.timeTargetFromDb
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType
import ru.yandex.direct.core.entity.pricepackage.model.ViewType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository
import ru.yandex.direct.core.testing.steps.ClientSteps.Companion.DEFAULT_SHARD
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.utils.DisallowedTargetTypesCalculator
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration
import java.math.BigDecimal
import java.time.LocalDate

@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignShowConditionsHandlerTest @Autowired constructor(
    private val steps: Steps,
    private val campaignTypedRepository: CampaignTypedRepository,
    private val campAdditionalTargetingsRepository: CampAdditionalTargetingsRepository,
    private val clientAdditionalTargetingsRepository: ClientAdditionalTargetingsRepository,
    campaignRepository: CampaignRepository,
) {
    private val sspPlatformsRepository = mock<SspPlatformsRepository> {
        on(mock.sspTitlesToIds) doReturn mapOf("Google" to 52, "MoPub" to 30, "Unknown" to null)
    }
    private val cryptaSegmentRepository = mock<CryptaSegmentRepository> {
        on(mock.brandSafety) doReturn mapOf(42L to Goal().withKeywordValue("brand value") as Goal)
    }
    private val retargetingConditionRepository = mock<RetargetingConditionRepository>()

    private var ppcPropertiesSupport = mock<PpcPropertiesSupport> {
        val oldRfDisabledPlaceIdsProp = mock<PpcProperty<Set<Long>>> {
            on(mock.getOrDefault(any())) doReturn emptySet()
        }
        on(mock.get(
            eq(PpcPropertyNames.BS_EXPORT_OLD_RF_DISABLED_PLACE_IDS), any()
        )) doReturn oldRfDisabledPlaceIdsProp
    }

    private val disallowedTargetTypesCalculator = DisallowedTargetTypesCalculator()
    private val handler = CampaignShowConditionsHandler(
        disallowedTargetTypesCalculator,
        sspPlatformsRepository,
        cryptaSegmentRepository,
        retargetingConditionRepository,
        campAdditionalTargetingsRepository,
        clientAdditionalTargetingsRepository,
        campaignRepository,
        ppcPropertiesSupport
    )

    private val defaultUser = steps.userSteps().createDefaultUser()

    private val cpmDisabledContextTypeCondition = listOf(
        TargetingExpression.Disjunction.newBuilder().addOr(
            TargetingExpressionAtom.newBuilder()
                .setKeyword(KeywordEnum.PageTargetType.number)
                .setOperation(OperationEnum.NotEqual.number)
                .setValue("0")
        ).build(),
        TargetingExpression.Disjunction.newBuilder().addOr(
            TargetingExpressionAtom.newBuilder()
                .setKeyword(KeywordEnum.PageTargetType.number)
                .setOperation(OperationEnum.NotEqual.number)
                .setValue("1")
        ).build(),
        TargetingExpression.Disjunction.newBuilder().addOr(
            TargetingExpressionAtom.newBuilder()
                .setKeyword(KeywordEnum.PageTargetType.number)
                .setOperation(OperationEnum.NotEqual.number)
                .setValue("2")
        ).build(),
    )

    @Test
    fun `disabled ips condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = TextCampaign()
            .withDisabledIps(listOf("127.0.0.1", "8.8.8.8", "abcde", "257.1.1.1", "255.255.255.255")),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder().addAllAnd(
            listOf(
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.ClientIp.number)
                        .setOperation(OperationEnum.NotEqual.number)
                        .setValue("134744072")
                ).build(),
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.ClientIp.number)
                        .setOperation(OperationEnum.NotEqual.number)
                        .setValue("2130706433")
                ).build(),
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.ClientIp.number)
                        .setOperation(OperationEnum.NotEqual.number)
                        .setValue("4294967295")
                ).build(),
            )
        ).build()
        ).buildPartial())

    @Test
    fun `disabled ssp condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = TextCampaign()
            .withDisabledSsp(listOf("MoPub", " Google   ", "abcde", "Unknown")),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder().addAllAnd(
            listOf(
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.SspId.number)
                        .setOperation(OperationEnum.NotEqual.number)
                        .setValue("30")
                ).build(),
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.SspId.number)
                        .setOperation(OperationEnum.NotEqual.number)
                        .setValue("52")
                ).build(),
            )
        ).build()
        ).buildPartial())

    @Test
    fun `page ids condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = TextCampaign()
            .withAllowedPageIds(listOf(42, 4004)),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder().addAnd(
            TargetingExpression.Disjunction.newBuilder().addAllOr(
                listOf(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.PageId.number)
                        .setOperation(OperationEnum.Equal.number)
                        .setValue("4004").build(),
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.PageId.number)
                        .setOperation(OperationEnum.Equal.number)
                        .setValue("42").build(),
                )
            )
        ).build()
        ).buildPartial())

    @Test
    fun `page ids from type condition on CpmYndxFrontpageCampaign`() = assertCampaignHandledCorrectly(
        handler,
        campaign = CpmYndxFrontpageCampaign()
            .withAllowedFrontpageType(setOf(FrontpageCampaignShowType.FRONTPAGE, FrontpageCampaignShowType.FRONTPAGE_MOBILE)),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder()
            .addAllAnd(cpmDisabledContextTypeCondition)
            .addAnd(
                TargetingExpression.Disjunction.newBuilder().addAllOr(
                    listOf(345620, 674114, 1638690, 1638708, 1654537,
                        349254, 674124, 1638693, 1638711, 1654534, 1654540)
                        .sortedBy { it.toString() }.map {
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(KeywordEnum.PageId.number)
                            .setOperation(OperationEnum.Equal.number)
                            .setValue(it.toString()).build()
                    }
                )
            ).build()
        ).buildPartial())

    @Test
    fun `page ids from type condition on CpmPriceCampaign`() = assertCampaignHandledCorrectly(
        handler,
        campaign = CpmPriceCampaign().withFlightTargetingsSnapshot(PriceFlightTargetingsSnapshot()
            .withViewTypes(listOf(ViewType.DESKTOP, ViewType.MOBILE))),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder()
            .addAllAnd(cpmDisabledContextTypeCondition)
            .addAnd(
                TargetingExpression.Disjunction.newBuilder().addAllOr(
                    listOf(345620, 674114, 1638690, 1638708, 1654537,
                        349254, 674124, 1638693, 1638711, 1654534, 1654540)
                        .sortedBy { it.toString() }.map {
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(KeywordEnum.PageId.number)
                            .setOperation(OperationEnum.Equal.number)
                            .setValue(it.toString()).build()
                    }
                )
            ).build()
        ).buildPartial())

    @Test
    fun `target time condition`() {
        val timetable = "1Ab"
        val keyword = KeywordEnum.Timetable
        val operation = OperationEnum.TimeNotLike
        val values = listOf("234567BCDEFGHIJKLMNOPQRSTUVWX")

        val campaign = TextCampaign()
        campaign.timeTarget = timeTargetFromDb(timetable)
        val expectedResource = TargetingExpression.newBuilder()
            .addAnd(
                TargetingExpression.Disjunction.newBuilder().addAllOr(
                    values.map {
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(keyword.number)
                            .setOperation(operation.number)
                            .setValue(it).build()
                    }
                )
            )
        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = Campaign.newBuilder().setShowConditions(expectedResource.build()).buildPartial()
        )
    }

    @Test
    fun `production long target time condition`() {
        val timetable = "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOP" +
                "QRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX9"
        val campaign = TextCampaign()
        campaign.timeTarget = timeTargetFromDb(timetable)
        val expectedResource = TargetingExpression.newBuilder()
        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = Campaign.newBuilder().setShowConditions(expectedResource.build()).buildPartial()
        )
    }

    @Test
    fun `video domains condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = CpmBannerCampaign()
            .withDisabledDomains(listOf("yandex.ru", "  yandex.ru", " яндекс.ру    "))
            .withDisabledVideoPlacements(listOf("yndx.ru", "  yndx.ru", " я.ру    "))
            .withType(CampaignType.CPM_BANNER),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder()
            .addAllAnd(cpmDisabledContextTypeCondition)
            .addAllAnd(
                listOf(
                    TargetingExpression.Disjunction.newBuilder().addAllOr(
                        listOf(
                            TargetingExpressionAtom.newBuilder()
                                .setKeyword(KeywordEnum.Domain.number)
                                .setOperation(OperationEnum.DomainNotLike.number)
                                .setValue("xn--41a.xn--p1ag").build(),
                            TargetingExpressionAtom.newBuilder()
                                .setKeyword(KeywordEnum.AdType.number)
                                .setOperation(OperationEnum.NegationOfBitwiseAnd.number)
                                .setValue("184").build(),
                        )
                    ).build(),
                    TargetingExpression.Disjunction.newBuilder().addAllOr(
                        listOf(
                            TargetingExpressionAtom.newBuilder()
                                .setKeyword(KeywordEnum.Domain.number)
                                .setOperation(OperationEnum.DomainNotLike.number)
                                .setValue("xn--d1acpjx3f.xn--p1ag").build(),
                            TargetingExpressionAtom.newBuilder()
                                .setKeyword(KeywordEnum.AdType.number)
                                .setOperation(OperationEnum.NegationOfBitwiseAnd.number)
                                .setValue("6").build(),
                        )
                    ).build(),
                    TargetingExpression.Disjunction.newBuilder().addAllOr(
                        listOf(
                            TargetingExpressionAtom.newBuilder()
                                .setKeyword(KeywordEnum.Domain.number)
                                .setOperation(OperationEnum.DomainNotLike.number)
                                .setValue("yandex.ru").build(),
                            TargetingExpressionAtom.newBuilder()
                                .setKeyword(KeywordEnum.AdType.number)
                                .setOperation(OperationEnum.NegationOfBitwiseAnd.number)
                                .setValue("6").build(),
                        )
                    ).build(),
                    TargetingExpression.Disjunction.newBuilder().addAllOr(
                        listOf(
                            TargetingExpressionAtom.newBuilder()
                                .setKeyword(KeywordEnum.Domain.number)
                                .setOperation(OperationEnum.DomainNotLike.number)
                                .setValue("yndx.ru").build(),
                            TargetingExpressionAtom.newBuilder()
                                .setKeyword(KeywordEnum.AdType.number)
                                .setOperation(OperationEnum.NegationOfBitwiseAnd.number)
                                .setValue("184").build(),
                        )
                    ).build(),
                )
            ).build()
        ).buildPartial())

    @Test
    fun `domains condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = TextCampaign()
            .withDisabledDomains(listOf("yandex.ru", " yandex.ru", "  яндекс.ру   ", "notadomain123*\$\\_-+=  тричетыре", "",
                "comcountry11992-sayt-hartii-97-dobavlen-v-spisok-ogranichennogo-dostupa")),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder().addAllAnd(
            listOf(
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.Domain.number)
                        .setOperation(OperationEnum.DomainNotLike.number)
                        .setValue("xn--d1acpjx3f.xn--p1ag")
                ).build(),
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.Domain.number)
                        .setOperation(OperationEnum.DomainNotLike.number)
                        .setValue("yandex.ru")
                ).build(),
            )
        ).build()
        ).buildPartial())

    @ParameterizedTest
    @MethodSource("stopTimeCases")
    fun `stop time condition`(campaign: BaseCampaign, stopTime: Int?) {
        val resource = TargetingExpression.newBuilder()
        if (stopTime != null) {
            resource.addAnd(
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.Unixtime.number)
                        .setOperation(OperationEnum.Less.number)
                        .setValue(stopTime.toString())
                )
            )
        }
        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = Campaign.newBuilder().setShowConditions(resource.build()).buildPartial())
    }

    @Test
    fun `target type condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = CpmBannerCampaign(),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder()
            .addAllAnd(cpmDisabledContextTypeCondition).build()).buildPartial()
    )

    @ParameterizedTest
    @MethodSource("iosCases")
    fun `iOS condition`(campaign: MobileContentCampaign, match: Boolean, value: String?) {
        val expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder())
        if (value != null) {
            val operation = if (match) OperationEnum.MatchNameAndVersionRange.number
            else OperationEnum.NotMatchNameAndVersionRange.number
            expectedProto.showConditions = TargetingExpression.newBuilder().addAnd(
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.OsFamilyAndVersion.number)
                        .setOperation(operation)
                        .setValue(value).build()
                )
            ).build()
        }
        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = expectedProto.buildPartial()
        )
    }

    @Test
    fun `brand safety condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = TextCampaign().withBrandSafetyCategories(listOf(42)),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder().addAnd(
            TargetingExpression.Disjunction.newBuilder().addOr(
                TargetingExpressionAtom.newBuilder()
                    .setKeyword(KeywordEnum.BrandSafetyCategories.number)
                    .setOperation(OperationEnum.NotEqual.number)
                    .setValue("brand value").build()
            )
        ).build()).buildPartial()
    )

    @Test
    fun `disabled page id condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = CpmBannerCampaign().withDisallowedPageIds(listOf(1, 2, 42)),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder()
            .addAllAnd(
                listOf(1, 2, 42).map {
                    TargetingExpression.Disjunction.newBuilder().addOr(
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(KeywordEnum.PageId.number)
                            .setOperation(OperationEnum.NotEqual.number)
                            .setValue(it.toString()).build()
                    ).build()
                }
            )
            .addAllAnd(cpmDisabledContextTypeCondition)
            .build()
        ).buildPartial()
    )

    @Test
    fun `rf condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = InternalAutobudgetCampaign().withImpressionRateCount(2).withImpressionRateIntervalDays(10),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder()
            .addAnd(
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.FrequencyDay.number)
                        .setOperation(OperationEnum.Less.number)
                        .setValue("2")
                        .build()
                ).addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.FreqExpire.number)
                        .setOperation(OperationEnum.GreaterOrEqual.number)
                        .setValue("10")
                        .build()
                ).build()
            )
            .build()
        ).buildPartial()
    )

    @Test
    fun `internal pade ids condition`() = assertCampaignHandledCorrectly(
        handler,
        campaign = InternalAutobudgetCampaign().withPageId(listOf(101, 102, 10042)),
        expectedProto = Campaign.newBuilder().setShowConditions(TargetingExpression.newBuilder()
            .addAnd(
                TargetingExpression.Disjunction.newBuilder().addAllOr(
                    listOf(10042, 101, 102).map {
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(KeywordEnum.PageId.number)
                            .setOperation(OperationEnum.Equal.number)
                            .setValue(it.toString()).build()
                    }
                ).build()
            )
        ).buildPartial()
    )

    @Test
    fun `additional targeting condition`() {
        val campaignInfo = steps.campaignSteps().createActiveCampaign(defaultUser.clientInfo)
        val campaign =
            campaignTypedRepository.getTypedCampaigns(DEFAULT_SHARD, listOf(campaignInfo.campaignId))[0]
        campAdditionalTargetingsRepository.insertTargetingToCampaigns(
            DEFAULT_SHARD,
            listOf(campaign.id),
            """{"and": [{"or": [{"keyword": 1, "operation": 2, "value": "3"}]}]}""",
            null
        )
        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = Campaign.newBuilder().setShowConditions(
                TargetingExpression.newBuilder().addAnd(
                    TargetingExpression.Disjunction.newBuilder().addOr(
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(1)
                            .setOperation(2)
                            .setValue("3")
                            .build()
                    ).build()
                ).build()
            ).buildPartial()
        )
    }

    @Test
    fun `additional targeting incorrect condition`() {
        val campaignInfo = steps.campaignSteps().createActiveCampaign(defaultUser.clientInfo)
        val campaign =
            campaignTypedRepository.getTypedCampaigns(DEFAULT_SHARD, listOf(campaignInfo.campaignId))[0]
        campAdditionalTargetingsRepository.insertTargetingToCampaigns(DEFAULT_SHARD, listOf(campaign.id), "{\"qwerty\":123}", "")
        val emptyCondition = TargetingExpression.getDefaultInstance()
        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = Campaign.newBuilder().setShowConditions(emptyCondition).buildPartial()
        )
    }

    @Test
    fun `additional targeting complex condition`() {
        val campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(defaultUser.clientInfo)
        val campaign =
            campaignTypedRepository.getTypedCampaigns(DEFAULT_SHARD, listOf(campaignInfo.campaignId))[0]
        campAdditionalTargetingsRepository.insertTargetingToCampaigns(
            DEFAULT_SHARD,
            listOf(campaign.id),
            """{"and": [ {"or": [{"keyword": 1, "operation": 2, "value": "3"},{"keyword": 4, "operation": 5, "value": "qwerty"}]},
                               {"or": [{"keyword": 6, "operation": 7, "value": "ABC"}]}]}""",
            ""
        )
        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = Campaign.newBuilder().setShowConditions(
                TargetingExpression.newBuilder()
                    .addAnd(
                        TargetingExpression.Disjunction.newBuilder()
                            .addOr(
                                TargetingExpressionAtom.newBuilder()
                                    .setKeyword(6)
                                    .setOperation(7)
                                    .setValue("ABC")
                                    .build()
                            ).build()
                    )
                    .addAllAnd(cpmDisabledContextTypeCondition)
                    .addAnd(
                        TargetingExpression.Disjunction.newBuilder()
                            .addOr(
                                TargetingExpressionAtom.newBuilder()
                                    .setKeyword(1)
                                    .setOperation(2)
                                    .setValue("3")
                                    .build()
                            ).addOr(
                                TargetingExpressionAtom.newBuilder()
                                    .setKeyword(4)
                                    .setOperation(5)
                                    .setValue("qwerty")
                                    .build()
                            ).build()
                    ).build()
            ).buildPartial()
        )
    }

    @Test
    fun `client additional targeting condition`() {
        val campaignInfo = steps.campaignSteps().createActiveCampaign(defaultUser.clientInfo)
        val campaign =
            campaignTypedRepository.getTypedCampaigns(DEFAULT_SHARD, listOf(campaignInfo.campaignId))[0]
        clientAdditionalTargetingsRepository.insertTargetingToClients(
            DEFAULT_SHARD,
            listOf(defaultUser.clientId.asLong()),
            """{"and": [{"or": [{"keyword": 1, "operation": 2, "value": "3"}]}]}""",
            null
        )
        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = Campaign.newBuilder().setShowConditions(
                TargetingExpression.newBuilder().addAnd(
                    TargetingExpression.Disjunction.newBuilder().addOr(
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(1)
                            .setOperation(2)
                            .setValue("3")
                            .build()
                    ).build()
                ).build()
            ).buildPartial()
        )
    }

    @ParameterizedTest
    @MethodSource("domainToASCIICases")
    fun `test domainToASCII`(domainIn: String, domainOut: String?) {
        val actualOut = handler.domainToASCII(domainIn)
        Assertions.assertThat(actualOut).isEqualTo(domainOut)
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun domainToASCIICases() = listOf(
            Arguments.of("ya.ru", "ya.ru"),
            Arguments.of("https://ya.ru?qwerty=123", "https://ya.ru?qwerty=123"),
            Arguments.of("com.easybrain.sudoku.android", "com.easybrain.sudoku.android"),
            Arguments.of("*.pskovonline.ru", null),
            Arguments.of("cy\u00ADpr.com", null),
            Arguments.of("cy¬pr.com", null),
            Arguments.of("sport\u00ADexpress.ru", null),
            Arguments.of("сќрєр·рѕрѕ.сђс„", null),
            Arguments.of("с‡рёс‚р°с‚сњ-рѕрѕр»р°рnoрѕ.com.ua", null),
            Arguments.of("\u200B\u200Bcom.schibstedbelarus.kufar", null),
            Arguments.of("медлаб.рф", "xn--80achd5ad.xn--p1ai"),
            Arguments.of("com.mastercomlimited.cardriving_t", "com.mastercomlimited.cardriving_t"),
        )

        @JvmStatic
        @Suppress("unused")
        fun stopTimeCases() = listOf(
            // only endDate
            Arguments.of(TextCampaign().withEndDate(LocalDate.of(2021, 6, 1)), 1622581199),
            // only strategy finish
            Arguments.of(TextCampaign()
                .withStrategy(DbStrategy()
                    .withAutobudget(CampaignsAutobudget.YES)
                    .withStrategyData(StrategyData()
                        .withFinish(LocalDate.of(2021, 5, 1))
                        .withName(CampaignsStrategyName.autobudget_avg_cpv_custom_period.literal)
                    ) as DbStrategy),
                1619902799),
            // only strategy finish, period_fix_bid
            Arguments.of(TextCampaign()
                .withStrategy(DbStrategy()
                    .withAutobudget(CampaignsAutobudget.NO)
                    .withStrategyData(StrategyData()
                        .withFinish(LocalDate.of(2021, 5, 1))
                        .withName(CampaignsStrategyName.period_fix_bid.literal)
                    ) as DbStrategy),
                1619902799),
            // only strategy finish, period_fix_bid + dayBudget
            Arguments.of(TextCampaign()
                .withStrategy(DbStrategy()
                    .withAutobudget(CampaignsAutobudget.NO)
                    .withStrategyData(StrategyData()
                        .withFinish(LocalDate.of(2021, 5, 1))
                        .withName(CampaignsStrategyName.period_fix_bid.literal)
                    ) as DbStrategy)
                .withDayBudget(BigDecimal.ONE),
                null),
            // strategy finish < endDate
            Arguments.of(TextCampaign()
                .withEndDate(LocalDate.of(2021, 6, 1))
                .withStrategy(DbStrategy()
                    .withAutobudget(CampaignsAutobudget.YES)
                    .withStrategyData(StrategyData()
                        .withFinish(LocalDate.of(2021, 5, 1))
                        .withName(CampaignsStrategyName.autobudget_avg_cpv_custom_period.literal)
                    ) as DbStrategy),
                1619902799),
            // strategy finish > endDate
            Arguments.of(TextCampaign()
                .withEndDate(LocalDate.of(2021, 6, 1))
                .withStrategy(DbStrategy()
                    .withAutobudget(CampaignsAutobudget.YES)
                    .withStrategyData(StrategyData()
                        .withFinish(LocalDate.of(2021, 6, 2))
                        .withName(CampaignsStrategyName.autobudget_avg_cpv_custom_period.literal)
                    ) as DbStrategy),
                1622581199),
            // prolongation, ignore strategy finish
            Arguments.of(TextCampaign()
                .withEndDate(LocalDate.of(2021, 6, 1))
                .withStrategy(DbStrategy()
                    .withStrategyData(StrategyData()
                        .withFinish(LocalDate.of(2021, 5, 1))
                        .withName(CampaignsStrategyName.period_fix_bid.literal)
                        .withAutoProlongation(1)
                    ) as DbStrategy),
                1622753999),
            // prolongation, no endDate
            Arguments.of(TextCampaign()
                .withStrategy(DbStrategy()
                    .withStrategyData(StrategyData()
                        .withFinish(LocalDate.of(2021, 5, 1))
                        .withName(CampaignsStrategyName.period_fix_bid.literal)
                        .withAutoProlongation(1)
                    ) as DbStrategy),
                null),
        )

        @JvmStatic
        @Suppress("unused")
        fun iosCases() = listOf(
            Arguments.of(MobileContentCampaign(), false, "3:14005:"),

            Arguments.of(MobileContentCampaign()
                .withStrategy(DbStrategy().withStrategyName(StrategyName.AUTOBUDGET_AVG_CPI)
                    .withStrategyData(StrategyData().withGoalId(1)) as DbStrategy)
                .withIsSkadNetworkEnabled(true), false, "3:14005:"),
            Arguments.of(MobileContentCampaign()
                .withStrategy(DbStrategy().withStrategyName(StrategyName.AUTOBUDGET_AVG_CPI) as DbStrategy)
                .withIsSkadNetworkEnabled(true), false, "3:14005:"),
            Arguments.of(MobileContentCampaign()
                .withStrategy(DbStrategy().withStrategyName(StrategyName.AUTOBUDGET)
                    .withStrategyData(StrategyData().withGoalId(1)) as DbStrategy)
                .withIsSkadNetworkEnabled(true), false, "3:14005:"),
            Arguments.of(MobileContentCampaign()
                .withStrategy(DbStrategy().withStrategyName(StrategyName.AUTOBUDGET) as DbStrategy)
                .withIsSkadNetworkEnabled(true), true, "3:14000:"),

            Arguments.of(MobileContentCampaign().withIsSkadNetworkEnabled(true), true, "3:14000:"),
            Arguments.of(MobileContentCampaign()
                .withIsSkadNetworkEnabled(true)
                .withIsNewIosVersionEnabled(true), true, "3:14000:"),

            Arguments.of(MobileContentCampaign().withIsNewIosVersionEnabled(true), true, null),
        )
    }
}

