import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.InventoryType
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacSearchLift
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbqueue.steps.DbQueueSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.service.emptyPatchRequest
import ru.yandex.direct.web.validation.model.WebValidationResult
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignGrutUpdateControllerTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var dbQueueSteps: DbQueueSteps

    @Autowired
    private lateinit var grutUacContentService: GrutUacContentService

    private lateinit var mockMvc: MockMvc
    private lateinit var uacCpmBannerCampaignId: String
    private lateinit var clientInfo: ClientInfo
    private lateinit var videoContentFirst: UacYdbContent

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        val userInfo: UserInfo = testAuthHelper.createDefaultUser()
        clientInfo = userInfo.clientInfo!!
        setFeatures()

        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        // Регистрируем типы джоб в dbqueue. Медийная кампания имеет брендлифты, надо ее также зарегистрировать.
        dbQueueSteps.registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)
        dbQueueSteps.registerJobType(DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS)

        grutSteps.createClient(clientInfo.clientId!!)

        uacCpmBannerCampaignId = grutSteps.createAndGetCpmBannerCampaign(clientInfo).id
        saveContent()
    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
    }

    @Test
    fun updateInventoryTest() {
        val newName = "Change Inventory Types"
        val uacYdbCampaignBeforeUpdate = getCpmBannerCampaign()
        SoftAssertions.assertSoftly {
            it.assertThat(uacYdbCampaignBeforeUpdate.inventoryTypes).hasSize(2)
            it.assertThat(uacYdbCampaignBeforeUpdate.inventoryTypes)
                .containsAll(setOf(InventoryType.INAPP, InventoryType.INSTREAM))
        }
        val newInventoryTypes = setOf(InventoryType.REWARDED)
        val request = updateCpmBannerCampaignRequest(name = newName, inventoryTypes = newInventoryTypes)
        doSuccessRequest(request, uacCpmBannerCampaignId)
        val uacYdbCampaignAfter = getCpmBannerCampaign()
        SoftAssertions.assertSoftly {
            it.assertThat(uacYdbCampaignAfter.inventoryTypes).hasSize(1)
            it.assertThat(uacYdbCampaignAfter.inventoryTypes).containsAll(newInventoryTypes)
        }
    }

    @Test
    fun updateInventoryToAllTypesTest() {
        val newName = "Change Inventory Types to all"
        val newInventoryTypes = setOf(InventoryType.INAPP,
            InventoryType.INSTREAM,
            InventoryType.INPAGE,
            InventoryType.REWARDED)

        val request = updateCpmBannerCampaignRequest(name = newName,
            inventoryTypes = newInventoryTypes)
        doSuccessRequest(request, uacCpmBannerCampaignId)
        val uacYdbCampaign = getCpmBannerCampaign()
        SoftAssertions.assertSoftly {
            it.assertThat(uacYdbCampaign.inventoryTypes).hasSize(4)
            it.assertThat(uacYdbCampaign.inventoryTypes).containsAll(newInventoryTypes)
        }
    }

    @Test
    fun updateDeviceTest() {
        val newName = "Change Device Types"
        val uacYdbCampaignBeforeUpdate = getCpmBannerCampaign()
        SoftAssertions.assertSoftly {
            it.assertThat(uacYdbCampaignBeforeUpdate.deviceTypes).hasSize(2)
            it.assertThat(uacYdbCampaignBeforeUpdate.deviceTypes)
                .containsAll(setOf(DeviceType.PHONE_ANDROID, DeviceType.DESKTOP))
        }
        val newDeviceTypes = setOf(DeviceType.PHONE_IOS)
        val request = updateCpmBannerCampaignRequest(name = newName, deviceTypes = newDeviceTypes)
        doSuccessRequest(request, uacCpmBannerCampaignId)
        val uacYdbCampaignAfter = getCpmBannerCampaign()
        SoftAssertions.assertSoftly {
            it.assertThat(uacYdbCampaignAfter.deviceTypes).hasSize(1)
            it.assertThat(uacYdbCampaignAfter.deviceTypes).containsAll(newDeviceTypes)
        }
    }

    @Test
    fun updateDeviceToAllTypesTest() {
        val newName = "Change Device Types to all"
        val newDeviceTypes = setOf(DeviceType.PHONE, DeviceType.DESKTOP, DeviceType.SMART_TV)
        val request = updateCpmBannerCampaignRequest(name = newName, deviceTypes = newDeviceTypes)
        doSuccessRequest(request, uacCpmBannerCampaignId)
        val uacYdbCampaignAfter = getCpmBannerCampaign()
        SoftAssertions.assertSoftly {
            it.assertThat(uacYdbCampaignAfter.deviceTypes).hasSize(3)
            it.assertThat(uacYdbCampaignAfter.deviceTypes).containsAll(newDeviceTypes)
        }
    }

    @Test
    fun updateSearchLiftTest() {
        val newName = "Change Device Types to all"
        val brands = listOf("brand1", "brand2")
        val searchObjects = listOf("Object1", "Object2")
        val request = updateCpmBannerCampaignRequest(name = newName, searchLift = UacSearchLift(brands, searchObjects))
        doSuccessRequest(request, uacCpmBannerCampaignId)
        val uacYdbCampaignAfter = getCpmBannerCampaign()
        SoftAssertions.assertSoftly {
            it.assertThat(uacYdbCampaignAfter.searchLift?.brands).containsAll(brands)
            it.assertThat(uacYdbCampaignAfter.searchLift?.searchObjects).containsAll(searchObjects)
        }
    }

    @Test
    fun updateSearchLiftTestWithoutFeature() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.SEARCH_LIFT, false)
        val newName = "Change Device Types to all"
        val brands = listOf("brand1", "brand2")
        val searchObjects = listOf("Object1", "Object2")
        val request = updateCpmBannerCampaignRequest(name = newName, searchLift = UacSearchLift(brands, searchObjects))
        checkBadRequest(request, uacCpmBannerCampaignId, PathHelper.path(PathHelper.field(CreateCampaignRequest::searchLift)), DefectIds.MUST_BE_NULL)
    }

    @Test
    fun updateSearchLiftTestWrongSize() {
        val newName = "Change Device Types to all"
        val brands = listOf("brand1", "brand2", "brand3", "brand4")
        val searchObjects = listOf("Object1", "Object2", "Object3", "Object4", "Object5", "Object6")
        val request = updateCpmBannerCampaignRequest(name = newName, searchLift = UacSearchLift(brands, searchObjects))
        checkBadRequest(request, uacCpmBannerCampaignId,
            PathHelper.path(
                PathHelper.field(CreateCampaignRequest::searchLift),
                PathHelper.field(UacSearchLift::brands)
            ), CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX)
        checkBadRequest(request, uacCpmBannerCampaignId,
            PathHelper.path(
                PathHelper.field(CreateCampaignRequest::searchLift),
                PathHelper.field(UacSearchLift::searchObjects)
            ), CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX)
    }

    @Test
    fun updateWithCpmBannerDisabledFeature() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true)
        val newName = "Change Device Types to all"
        val brands = listOf("brand1", "brand2")
        val searchObjects = listOf("Object1", "Object2")
        val request = updateCpmBannerCampaignRequest(name = newName, searchLift = UacSearchLift(brands, searchObjects))
        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/${uacCpmBannerCampaignId}?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    private fun getCpmBannerCampaign(): UacYdbCampaign {
        grutApiService.briefGrutApi.getBrief(uacCpmBannerCampaignId.toIdLong())
        val campaignResponse = grutApiService.briefGrutApi.getBrief(uacCpmBannerCampaignId.toIdLong())
        assertThat(campaignResponse).isNotNull
        return campaignResponse!!.toUacYdbCampaign()
    }

    private fun updateCpmBannerCampaignRequest(
        name: String,
        deviceTypes: Set<DeviceType> = setOf(DeviceType.DESKTOP, DeviceType.PHONE_IOS),
        inventoryTypes: Set<InventoryType> = setOf(InventoryType.INAPP, InventoryType.INSTREAM),
        socdem: Socdem = Socdem(listOf(Gender.FEMALE), AgePoint.AGE_45, AgePoint.AGE_INF, Socdem.IncomeGrade.LOW, Socdem.IncomeGrade.PREMIUM),
        searchLift: UacSearchLift? = null,
    ): PatchCampaignRequest {

        return emptyPatchRequest().copy(
            displayName = name,
            socdem = socdem,
            deviceTypes = deviceTypes,
            inventoryTypes = inventoryTypes,
            videosAreNonSkippable = true,
            href = "http://ya.ru",
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.valueOf(300),
                    LocalDate.of(2021, 10, 15),
                    LocalDate.of(2021, 4, 15),
                    BigDecimal.valueOf(2100),
                    null,
                    null,
                )
            ),
            contentIds = listOf(videoContentFirst.id),
            searchLift = searchLift,
        )
    }

    fun doSuccessRequest(
        request: PatchCampaignRequest,
        uacCampaignId: String,
    ): Long {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/${uacCampaignId}?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo { System.err.println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val directCampaignId = campaignId.toIdLong()
        val campaignResponse = grutApiService.briefGrutApi.getBrief(directCampaignId)
        assertThat(campaignResponse).isNotNull
        return directCampaignId
    }

    fun checkBadRequest(
        request: PatchCampaignRequest,
        uacCampaignId: String,
        path: Path?,
        defectId: DefectId<*>,
    ) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/${uacCampaignId}?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)
        SoftAssertions.assertSoftly {
            it.assertThat(validationResult.errors).isNotEmpty
            if (path != null) {
                it.assertThat(validationResult.errors.map { err -> err.path }).contains(path.toString())
            }
            it.assertThat(validationResult.errors.map { err -> err.code }).contains(defectId.code)
        }
    }

    private fun setFeatures() {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.DISABLE_BILLING_AGGREGATES, true)
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.UC_UAC_CREATE_MEDIA_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED, true)
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.SEARCH_LIFT, true)
    }

    private fun saveContent() {
        val creativeCanvasIdFirst = steps.creativeSteps()
            .addDefaultVideoAdditionCreative(clientInfo, steps.creativeSteps().nextCreativeId).creativeId
        videoContentFirst = createDefaultVideoContent(
            creativeId = creativeCanvasIdFirst,
            accountId = clientInfo.clientId!!.toString(),
        )
        grutUacContentService.insertContents(listOf(videoContentFirst))
    }
}
