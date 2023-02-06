package ru.yandex.direct.web.entity.uac.service

import org.assertj.core.api.SoftAssertions
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.*
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacAccountSteps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.FunctionalUtils.mapList
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignInternalRequest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignValidationServiceTest {
    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacAccountSteps: UacAccountSteps

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    @Autowired
    private lateinit var webYdbUacCampaignUpdateService: WebYdbUacCampaignUpdateService

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    private lateinit var uacCampaignInfo: UacCampaignSteps.UacCampaignInfo
    private lateinit var clientInfo: ClientInfo
    private lateinit var uacAccount: UacYdbAccount
    private lateinit var content: UacYdbContent
    private lateinit var defaultRequestWithoutButton: PatchCampaignInternalRequest
    private lateinit var defaultRequestWithButton: PatchCampaignInternalRequest

    @Before
    fun before() {
        content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        clientInfo = steps.clientSteps().createDefaultClient()
        uacAccount = uacAccountSteps.createAccount(clientInfo)
        uacCampaignInfo = uacCampaignSteps.createCpmCampaign(clientInfo)
        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)

        var cpmAssetButton = CpmAssetButton(action = UacButtonAction.CUSTOM_TEXT,
            href = "https://ya.ru",
            customText = "hh");

        val cpmAssetWithoutButton = mapOf(content.id to UacCpmAsset(
            title = "Asset title",
            titleExtension = "Asset title extension",
            body = "Asset body",
            button = null,
            logoImageHash = null,
            measurers = null,
            pixels = listOf("https:/asd/mc.yanasxasxsdex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
            bannerHref = "https://ya.ru")
        );

        val cpmAssetWithButton = mapOf(content.id to UacCpmAsset(
            title = "Asset title",
            titleExtension = "Asset title extension",
            body = "Asset body",
            button = cpmAssetButton,
            logoImageHash = null,
            measurers = null,
            pixels = listOf("https:/asd/mc.yanasxasxsdex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
            bannerHref = "https://ya.ru")
        );

        defaultRequestWithoutButton = emptyPatchInternalRequest().copy(
            displayName = "New display name",
            href = uacCampaignInfo.uacCampaign.storeUrl,
            contentIds = listOf(content.id),
            strategy = uacCampaignInfo.uacCampaign.strategy,
            videosAreNonSkippable = false,
            socdem = Socdem(
                genders = listOf(Gender.MALE),
                ageLower = AgePoint.AGE_25,
                ageUpper = AgePoint.AGE_55,
                incomeLower = Socdem.IncomeGrade.LOW,
                incomeUpper = Socdem.IncomeGrade.MIDDLE
            ),
            cpmAssets = cpmAssetWithoutButton
        )

        defaultRequestWithButton = emptyPatchInternalRequest().copy(
            displayName = "New display name",
            href = uacCampaignInfo.uacCampaign.storeUrl,
            contentIds = listOf(content.id),
            strategy = uacCampaignInfo.uacCampaign.strategy,
            videosAreNonSkippable = false,
            socdem = Socdem(
                genders = listOf(Gender.MALE),
                ageLower = AgePoint.AGE_25,
                ageUpper = AgePoint.AGE_55,
                incomeLower = Socdem.IncomeGrade.LOW,
                incomeUpper = Socdem.IncomeGrade.MIDDLE
            ),
            cpmAssets = cpmAssetWithButton
        )
    }

    @Test
    fun updateCampaignAssetsTest_BadProtocol_InvalidPixelFormat() {
        check(listOf("asdasdhttps:/asd/mc.yanasxasxsdex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
            listOf(BannerDefectIds.Gen.INVALID_PIXEL_FORMAT))
    }

    @Test
    fun updateCampaignAssetsTest_BadDomain_InvalidPixelFormat() {
        check(listOf("https://ads.adfoxfalse.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25"),
            listOf(BannerDefectIds.Gen.INVALID_PIXEL_FORMAT))
    }

    @Test
    fun updateCampaignAssetsTest_MaxForeignPixelsOnBanner() {
        check(listOf(
                "https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25",
                "https://amc.yandex.ru/show?cmn_id=4&plt_id=4&crv_id=4&evt_tp=impression&ad_type=banner&vv_crit=mrc&rnd=%Random%",
                "https://ads.adfox.ru/254366/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25",
                "https://ads.adfox.ru/254368/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25",
                "https://ads.adfox.ru/254335/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25",
            ),
            listOf(BannerDefectIds.Size.MAX_FOREIGN_PIXELS_ON_BANNER)
        )
    }

    @Test
    fun updateCampaignAssetsTest_MaxYaAudPixelsOnBanner() {
        check(listOf(
                "https://mc.yandex.ru/pixel/2555327861230035827?rnd=%aw_random%",
                "https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"
            ),
            listOf(BannerDefectIds.Size.MAX_YA_AUD_PIXELS_ON_BANNER)
        )
    }

    @Test
    fun updateCampaignAssetsTest_InvalidPixelFormat4() {
        check(listOf(
            "https://mc.yandex.ru/pixel/2555327861230035827?rnd=%aw_random%",
            "https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25",
            "https://ads.adhjkjhkjfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25",
            ),
            listOf(BannerDefectIds.Size.MAX_YA_AUD_PIXELS_ON_BANNER, BannerDefectIds.Gen.INVALID_PIXEL_FORMAT)
        )
    }

    @Test
    fun updateCampaignAssetsTest_InvalidPixelFormat5() {
        check(listOf(
            "https://ad.adriver.ru/cgi-bin/erle.cgi?sid=1&ad=605736&bt=43&bn=605736&rnd=%aw_random%"
            ),
            listOf(BannerDefectIds.PixelPermissions.NO_RIGHTS_TO_PIXEL)
        )
    }

    @Test
    fun updateCampaignAssetsTest_CustomTextNotNullWithoutFeature() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_BUTTON_CUSTOM_TEXT, false);
        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            defaultRequestWithButton,
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )
        Assert.assertThat(result.isSuccessful, Matchers.`is`(false))
        val resultDefects: List<DefectId<*>> = mapList(result.errors) {it.defect.defectId()}
        Assert.assertTrue(resultDefects.equals(listOf(DefectIds.INVALID_VALUE, BannerDefectIds.Gen.UNSUPPORTED_BUTTON_ACTION)))
    }

    @Test
    fun updateCampaignAssetsTest_CustomTextNotNullWithFeature() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_BUTTON_CUSTOM_TEXT, true);
        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            defaultRequestWithButton,
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )
        Assert.assertThat(result.isSuccessful, Matchers.`is`(false))
        val resultDefects: List<DefectId<*>> = mapList(result.errors) {it.defect.defectId()}
        Assert.assertTrue(resultDefects.containsAll(listOf(DefectIds.INVALID_VALUE)))
        Assert.assertTrue(listOf(DefectIds.INVALID_VALUE).equals(resultDefects))
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_BUTTON_CUSTOM_TEXT, false);
    }

    private fun check(pixels: List<String>, defects: List<DefectId<*>>) {
        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            getRequest(pixels),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        Assert.assertThat(result.isSuccessful, Matchers.`is`(false))
        Assert.assertNotNull(result.validationResult)
        Assert.assertEquals("has error error as expected", result.validationResult.hasAnyErrors(), true)
        Assert.assertEquals(result.errors.size, defects.size)
        val resultDefects: List<DefectId<*>> = mapList(result.errors) {it.defect.defectId()}
        SoftAssertions.assertSoftly {
            it.assertThat(resultDefects).hasSize(defects.count())
            it.assertThat(resultDefects).containsAnyElementsOf(defects)
        }
    }

    private fun getRequest(pixels: List<String>): PatchCampaignInternalRequest {
        val eee = defaultRequestWithoutButton.cpmAssets!!.get(content.id)!!.copy(pixels = pixels)
        return defaultRequestWithoutButton.copy(cpmAssets = mapOf(content.id to eee))
    }

}
