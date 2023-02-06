package ru.yandex.direct.web.entity.uac.service

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.controller.BaseGrutCreateCampaignTest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest

@GrutDirectWebTest
@ExtendWith(SpringExtension::class)
class GrutUacCampaignServiceGoalsUpdateTest : BaseGrutCreateCampaignTest() {

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    companion object {
        @JvmStatic
        fun params(): Array<Array<Any>> {
            val firstGoal = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
            val anotherGoal = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
            return arrayOf(
                arrayOf(
                    "Replace goal, duplicates are not provided",
                    listOf(firstGoal),
                    listOf(anotherGoal),
                    1,
                    listOf(anotherGoal.goalId)
                ),
                arrayOf(
                    "Replace goal, duplicates are provided",
                    listOf(firstGoal),
                    listOf(firstGoal, firstGoal, anotherGoal),
                    2,
                    listOf(firstGoal.goalId, anotherGoal.goalId)
                )
            )
        }
    }

    @BeforeEach
    fun init() {
        super.before()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    fun updateGoalsCheck(
        testName: String,
        goalsOnCreate: List<UacGoal>,
        goalsOnUpdate: List<UacGoal>,
        expectedSize: Int,
        expectedIds: List<Long>
    ) {
        val campaignAppInfo = defaultAppInfo(
            appId = "com.yandex.browser",
            bundleId = "com.yandex.browser",
        )
        uacAppInfoRepository.saveAppInfo(campaignAppInfo)
        val campaignStoreUrl = googlePlayAppInfoGetter.appPageUrl(
            campaignAppInfo.appId,
            campaignAppInfo.region,
            campaignAppInfo.language
        )
        steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, campaignStoreUrl)

        val request = createUacCampaignRequest(
            contentIds = listOf(imageContentId),
            app = campaignAppInfo,
            goals = goalsOnCreate,
        )

        val campaignId1 = createCampaign(request)
        val campaignId2 = createCampaign(request)

        updateCampaigns(campaignId1.toIdLong(), campaignId2.toIdLong(), goalsOnUpdate)

        val firstCampaignAfter = getGoalIds(campaignId1)
        val secondCampaignAfter = getGoalIds(campaignId2)

        SoftAssertions().apply {
            assertThat(firstCampaignAfter)
                .hasSize(goalsOnUpdate.size)
                .containsExactlyInAnyOrderElementsOf(goalsOnUpdate.map { it.goalId })
                .containsAll(secondCampaignAfter)
            assertThat(secondCampaignAfter)
                .hasSize(expectedSize)
                .containsExactlyInAnyOrderElementsOf(expectedIds)
        }.assertAll()
    }

    private fun createCampaign(request: CreateCampaignRequest): String {
        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns")
                .param("ulogin", clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return JsonUtils.fromJson(response)["result"]["id"].asText()
    }

    private fun updateCampaigns(campaignId1: Long, campaignId2: Long, goalsOnUpdate: List<UacGoal>) {
        val patch = emptyPatchRequest().copy(
            goals = goalsOnUpdate
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$campaignId1")
                .param("ulogin", clientInfo.login)
                .content(JsonUtils.toJson(patch))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)

        grutUacCampaignService.updateCampaignGoals(
            listOf(campaignId2),
            goalsOnUpdate.map { it.goalId }.toSet()
        )
    }

    private fun getGoalIds(campaignId: String): List<Long>? {
        return grutUacCampaignService.getCampaignById(campaignId)!!.goals?.map { it.goalId }
    }
}
