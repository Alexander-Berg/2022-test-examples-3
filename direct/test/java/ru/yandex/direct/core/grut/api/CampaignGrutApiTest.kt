package ru.yandex.direct.core.grut.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.PlacementType
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType
import ru.yandex.direct.core.entity.uac.grut.ThreadLocalGrutContext
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.grut.client.GrutClient
import ru.yandex.grut.object_api.proto.ObjectApiServiceOuterClass
import ru.yandex.grut.objects.proto.CampaignV2
import java.math.BigDecimal

class CampaignGrutApiTest {

    private lateinit var campaignGrutApi: CampaignGrutApi

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    @Before
    fun before() {
        val grutClient = mock(GrutClient::class.java)
        val grutContext = ThreadLocalGrutContext(grutClient)
        `when`(grutClient.updateObjects(any(ObjectApiServiceOuterClass.TReqUpdateObjects::class.java)))
            .thenReturn(ObjectApiServiceOuterClass.TRspUpdateObjects.getDefaultInstance())
        campaignGrutApi = CampaignGrutApi(grutContext)
    }

    @Test
    fun trySerializeCampaignWithNullCreateDate() {
        //arrange

        val campaign = TextCampaign()
            .withId(123L)
            .withClientId(1L)
            .withOrderId(256L)
            .withName("name")
            .withType(CampaignType.TEXT)
            .withCreateTime(null)
            .withCurrency(CurrencyCode.RUB)
            .withStrategy(defaultStrategy())
            .withSource(CampaignSource.DIRECT)
            .withMetatype(CampaignMetatype.DEFAULT_)
            .withSum(BigDecimal.valueOf(0))
            .withSumSpent(BigDecimal.valueOf(0))
        //act & assert
        assertThatCode { campaignGrutApi.createOrUpdateCampaign(CampaignGrutModel(campaign, orderType = 1)) }
            .doesNotThrowAnyException()
    }

    @Test
    fun campaignStatusArchivedTest() {
        val campaign = TextCampaign()
            .withStatusArchived(true)
            .withStatusModerate(CampaignStatusModerate.NEW)
            .withStatusShow(false)
        val gotStatus = campaignGrutApi.getCampaignStatus(campaign)
        assertThat(gotStatus).isEqualTo(CampaignV2.ECampaignStatus.CST_ARCHIVED)
    }

    @Test
    fun campaignStatusDraftTest() {
        val campaign = TextCampaign()
            .withStatusArchived(false)
            .withStatusModerate(CampaignStatusModerate.NEW)
            .withStatusShow(false)
        val gotStatus = campaignGrutApi.getCampaignStatus(campaign)
        assertThat(gotStatus).isEqualTo(CampaignV2.ECampaignStatus.CST_DRAFT)
    }

    @Test
    fun campaignStatusStoppedTest() {
        val campaign = TextCampaign()
            .withStatusArchived(false)
            .withStatusModerate(CampaignStatusModerate.READY)
            .withStatusShow(false)
        val gotStatus = campaignGrutApi.getCampaignStatus(campaign)
        assertThat(gotStatus).isEqualTo(CampaignV2.ECampaignStatus.CST_STOPPED)
    }

    @Test
    fun campaignStatusActiveTest() {
        val campaign = TextCampaign()
            .withStatusArchived(false)
            .withStatusModerate(CampaignStatusModerate.READY)
            .withStatusShow(true)
        val gotStatus = campaignGrutApi.getCampaignStatus(campaign)
        assertThat(gotStatus).isEqualTo(CampaignV2.ECampaignStatus.CST_ACTIVE)
    }


    /**
     * Тест проверяет, что для каждого значение енума из PlacementType есть значение в TPlacementTypes
     */
    @Test
    fun placementTypesTest() {
        val softAssertions = SoftAssertions()
        for (placementType in PlacementType.values()) {
            val grutPlacementTypes = campaignGrutApi.getPlacementTypes(DynamicCampaign().withPlacementTypes(setOf(placementType)))
            softAssertions.assertThat(grutPlacementTypes).isNotNull
            softAssertions.assertThat(grutPlacementTypes!!.allFields.values.filter { it == true }).hasSize(1)
        }
        softAssertions.assertAll()
    }

    /**
     * Тест проверяет, что если указано больше одного значения в PlacementType, то они все будут смапплены в GRuT
     */
    @Test
    fun placementTypes_TwoValuesSetTest() {
        val grutPlacementTypes = campaignGrutApi.getPlacementTypes(DynamicCampaign().withPlacementTypes(setOf(PlacementType.SEARCH_PAGE, PlacementType.ADV_GALLERY)))
        assertThat(grutPlacementTypes).isNotNull
        assertThat(grutPlacementTypes!!.advGallery).isTrue
        assertThat(grutPlacementTypes.searchPage).isTrue
    }

    @Test
    fun placementTypes_AllTypesTest() {
        // отсутствие placement types равносильно - показывать везде
        val grutPlacementTypes = campaignGrutApi.getPlacementTypes(DynamicCampaign())
        assertThat(grutPlacementTypes).isNull()
    }

    /**
     * Тест проверяет, что для всех возможных значений FrontpageCampaignShowType есть заполняется соответствующее поле в GrUT
     */
    @Test
    fun getFrontPagePlacementsTest() {
        val softAssertions = SoftAssertions()
        for (frontpageCampaignShowType in FrontpageCampaignShowType.values()) {
            val grutFrontPagePlacements = campaignGrutApi.getFrontPagePlacements(CpmYndxFrontpageCampaign().withAllowedFrontpageType(setOf(frontpageCampaignShowType)))
            softAssertions.assertThat(grutFrontPagePlacements).isNotNull
            softAssertions.assertThat(grutFrontPagePlacements!!.allFields.values.filter { it == true }).hasSize(1)
        }
        softAssertions.assertAll()
    }
}
