package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.adv.direct.campaign.CampaignMetatype
import ru.yandex.adv.direct.campaign.CampaignSource
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype as CoreCampaignMetatype
import ru.yandex.direct.core.entity.campaign.model.CampaignSource as CoreCampaignSource
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.container.CampaignCommonFields
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertProtoFilledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.utils.CampaignNameTransliterator

class CampaignCommonFieldsHandlerTest {
    private val transliterator = CampaignNameTransliterator()
    private val handler = CampaignCommonFieldsHandler(transliterator)

    @Test
    fun `resource is mapped to proto correctly`() = assertProtoFilledCorrectly(
        handler,
        resource = CampaignCommonFields.Builder()
            .withType("text")
            .withName("Имя")
            .withLatName("Imya")
            .withStop(false)
            .withArchive(true)
            .withClientId(1234L)
            .withSource(CampaignSource.CAMPAIGN_SOURCE_DIRECT)
            .withMetatype(CampaignMetatype.CAMPAIGN_METATYPE_DEFAULT)
            .build(),
        expectedProto = Campaign.newBuilder()
            .setType("text")
            .setName("Имя")
            .setLatName("Imya")
            .setStop(false)
            .setArchive(true)
            .setClientId(1234L)
            .setSource(CampaignSource.CAMPAIGN_SOURCE_DIRECT_VALUE)
            .setMetatype(CampaignMetatype.CAMPAIGN_METATYPE_DEFAULT_VALUE)
            .buildPartial()
    )

    @MethodSource
    @ParameterizedTest(name = "{0} is converted correctly")
    fun `campaign name is converted correctly`(
        campaignNameDescription: String,
        campaignName: String,
        expectedCampaignName: String,
        expectedCampaignLatName: String
    ) = assertCampaignHandledCorrectly(
        handler,
        campaign = DynamicCampaign()
            .withId(1)
            .withName(campaignName)
            .withMinusKeywords(listOf())
            .withType(CampaignType.DYNAMIC)
            .withStatusArchived(false)
            .withStatusShow(true)
            .withClientId(15L)
            .withSource(CoreCampaignSource.UAC)
            .withMetatype(CoreCampaignMetatype.ECOM),
        expectedResource = CampaignCommonFields.builder()
            .withName(expectedCampaignName)
            .withLatName(expectedCampaignLatName)
            .withArchive(false)
            .withStop(false)
            .withClientId(15L)
            .withType(CampaignsType.dynamic.literal)
            .withSource(CampaignSource.CAMPAIGN_SOURCE_UAC)
            .withMetatype(CampaignMetatype.CAMPAIGN_METATYPE_ECOM)
            .build()
    )

    @MethodSource
    @ParameterizedTest(name = "{0} is handled correctly")
    fun `campaign is handled correctly`(
        campaignDescription: String,
        campaign: BaseCampaign,
        expectedResource: CampaignCommonFields,
    ) = assertCampaignHandledCorrectly(handler, campaign, expectedResource)

    @EnumSource(CoreCampaignSource::class)
    @ParameterizedTest(name = "{0} campaign source is known")
    fun `all campaign sources are known`(source: CoreCampaignSource) =
            assertThatCode { CampaignSourceConverter.toExportSource(source) }.doesNotThrowAnyException()

    @EnumSource(CoreCampaignMetatype::class)
    @ParameterizedTest(name = "{0} campaign metatype is known")
    fun `all campaign metatypes are known`(metatype: CoreCampaignMetatype) =
            assertThatCode { CampaignMetatypeConverter.toExportMetatype(metatype) }.doesNotThrowAnyException()

    companion object {
        private fun campaignNameTestArgument(
            description: String,
            name: String,
            expectedName: String = name,
            expectedLatName: String,
        ) = Arguments.of(description, name, expectedName, expectedLatName)

        @JvmStatic
        @Suppress("unused")
        fun `campaign name is converted correctly`() = listOf(
            campaignNameTestArgument(
                description = "name with cyrillic letters",
                name = "Тестовая кампания API510:40 AddMediaplanAdNotRequired" +
                    "FieldsHrefAndContactInfoTest.initTestData",
                expectedLatName = "Testovaya_kampaniya_API510_40_AddMediaplanAdNotRequired" +
                    "FieldsHrefAndContactInfoTest_initTestData",
            ),
            campaignNameTestArgument(
                description = "name with digit and punctuation",
                name = " 5. RSA.  Дин_Поиск.  Очень полезное",
                expectedLatName = "_5__RSA__Din_Poisk__Ochen_poleznoe",
            ),
            campaignNameTestArgument(
                description = "name with non-latin letters",
                name = "API Test Kampanyası510: 40 AddMediaplanAdNotRequired" +
                    "FieldsHrefAndContactInfoTest.initTestData",
                expectedLatName = "API_Test_Kampanyas510__40_AddMediaplanAdNotRequired" +
                    "FieldsHrefAndContactInfoTest_initTestData",
            ),
            campaignNameTestArgument(
                description = "empty name",
                name = "",
                expectedLatName = "",
            ),
            campaignNameTestArgument(
                description = "name with hieroglyphs",
                name = "北京康鼎医疗科技有限公司 2部",
                expectedName = " 2",
                expectedLatName = "_2",
            )
        )

        private fun campaignTestArgument(
            campaignDescription: String,
            campaign: BaseCampaign,
            expectedResource: CampaignCommonFields,
        ) = Arguments.of(campaignDescription, campaign, expectedResource)

        @JvmStatic
        @Suppress("unused")
        fun `campaign is handled correctly`() = listOf(
            campaignTestArgument(
                campaignDescription = "campaign without minus keywords",
                campaign = InternalAutobudgetCampaign()
                    .withId(1)
                    .withName("")
                    .withStatusArchived(false)
                    .withStatusShow(true)
                    .withClientId(15L)
                    .withType(CampaignType.INTERNAL_AUTOBUDGET)
                    .withSource(CoreCampaignSource.DIRECT)
                    .withMetatype(CoreCampaignMetatype.DEFAULT_),
                expectedResource = CampaignCommonFields.builder()
                    .withName("")
                    .withLatName("")
                    .withArchive(false)
                    .withStop(false)
                    .withClientId(15L)
                    .withType(CampaignsType.internal_autobudget.literal)
                    .withSource(CampaignSource.CAMPAIGN_SOURCE_DIRECT)
                    .withMetatype(CampaignMetatype.CAMPAIGN_METATYPE_DEFAULT)
                    .build(),
            ),
            campaignTestArgument(
                campaignDescription = "campaign with minus keywords",
                campaign = DynamicCampaign()
                    .withId(1)
                    .withName("")
                    .withStatusArchived(false)
                    .withStatusShow(true)
                    .withClientId(15L)
                    .withType(CampaignType.DYNAMIC)
                    .withMinusKeywords(listOf("!своими", "асклепий", "бесплатно", "диплом", "коллаж"))
                    .withSource(CoreCampaignSource.UAC)
                    .withMetatype(CoreCampaignMetatype.DEFAULT_),
                expectedResource = CampaignCommonFields.builder()
                    .withName("")
                    .withLatName("")
                    .withArchive(false)
                    .withStop(false)
                    .withClientId(15L)
                    .withType(CampaignsType.dynamic.literal)
                    .withSource(CampaignSource.CAMPAIGN_SOURCE_UAC)
                    .withMetatype(CampaignMetatype.CAMPAIGN_METATYPE_DEFAULT)
                    .build(),
            ),
            campaignTestArgument(
                campaignDescription = "campaign with disallowed target types",
                campaign = DynamicCampaign()
                    .withId(1)
                    .withName("")
                    .withStatusArchived(false)
                    .withStatusShow(true)
                    .withClientId(15L)
                    .withMinusKeywords(listOf())
                    .withType(CampaignType.DYNAMIC)
                    .withSource(CoreCampaignSource.API)
                    .withMetatype(CoreCampaignMetatype.ECOM),
                expectedResource = CampaignCommonFields.builder()
                    .withName("")
                    .withLatName("")
                    .withArchive(false)
                    .withStop(false)
                    .withClientId(15L)
                    .withType(CampaignsType.dynamic.literal)
                    .withSource(CampaignSource.CAMPAIGN_SOURCE_API)
                    .withMetatype(CampaignMetatype.CAMPAIGN_METATYPE_ECOM)
                    .build(),
            ),
        )
    }
}
