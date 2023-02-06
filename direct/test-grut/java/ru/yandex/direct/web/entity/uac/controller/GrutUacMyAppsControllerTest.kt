package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA_2
import ru.yandex.direct.core.entity.uac.service.GrutUacClientService
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutUacMyAppsControllerTest : UacMyAppsControllerTestBase() {

    @Autowired
    private lateinit var grutUacClientService: GrutUacClientService

    @Autowired
    private lateinit var grutSteps: GrutSteps

    protected lateinit var uacAppInfo3: UacYdbAppInfo

    @Before
    fun grutBefore() {
        val operator = userInfo.clientInfo?.chiefUserInfo?.user!!
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        accountId = grutUacClientService.getOrCreateClient(operator, subjectUser)
    }

    @Test
    fun myAppsWithGrutTest() {
        uacAppInfo3 = defaultAppInfo(platform = Platform.IOS, source = Store.ITUNES, data = IOS_APP_INFO_DATA_2)
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo3)

        grutSteps.createMobileAppCampaign(userInfo.clientInfo!!, appId = uacAppInfo3.id)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/app_info/my_apps")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val expectedAppInfos = listOf(uacAppInfo1, uacAppInfo2, uacAppInfo3)
            .map { uacAppInfoService.getAppInfo(it) }
            .sortedBy { it.title?.lowercase() ?: "" }

        assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(
                    expectedAppInfos
                )
            )
        )
    }
}
