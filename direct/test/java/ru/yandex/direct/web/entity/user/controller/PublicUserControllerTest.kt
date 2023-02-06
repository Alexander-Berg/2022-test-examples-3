package ru.yandex.direct.web.entity.user.controller

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.service.BlackboxUserService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.BlackboxUserInfo
import ru.yandex.direct.core.testing.steps.FeatureSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.i18n.Language
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.entity.uac.controller.BaseMvcTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
internal class PublicUserControllerTest : BaseMvcTest() {
    @Autowired
    private lateinit var featureSteps: FeatureSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var blackboxUserService: BlackboxUserService

    private lateinit var userInfo: UserInfo

    private lateinit var blackboxUserInfo: BlackboxUserInfo

    @Before
    fun init() {
        userInfo = testAuthHelper.createDefaultUser()
        blackboxUserInfo = testAuthHelper.createDefaultBlackboxUser()

        Mockito.`when`(blackboxUserService.getUserInfo(blackboxUserInfo.uid))
            .thenReturn(blackboxUserInfo.user)
    }

    @Test
    fun getOperatorInfo_success() {
        doRequest(
            "/public/user/operator_info",
            HttpMethod.GET,
            200
        ).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.login").value(userInfo.login))
            .andExpect(jsonPath("$.result.direct_user").value(true))
    }

    @Test
    fun getOperatorInfo_agency_success() {
        val agencyClientInfo: ClientInfo = steps.clientSteps().createDefaultAgency()
        val agencyOperator = agencyClientInfo.chiefUserInfo!!.user
        val clientInfo: ClientInfo = steps.clientSteps().createClientUnderAgency(agencyClientInfo)
        testAuthHelper.setSubjectUser(clientInfo.uid)
        testAuthHelper.setOperator(agencyOperator!!.uid)
        testAuthHelper.setSecurityContext()
        doRequest(
            "/public/user/operator_info",
            HttpMethod.GET,
            200
        ).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.login").value(agencyOperator.login))
            .andExpect(jsonPath("$.result.direct_user").value(true))
            .andExpect(jsonPath("$.result.role").value("AGENCY"))
    }

    @Test
    fun getOperatorInfo_blocked_success() {
        steps.userSteps().setUserProperty(userInfo, User.STATUS_BLOCKED, true)
        testAuthHelper.setOperatorAndSubjectUser(userInfo.uid)

        doRequest(
            "/public/user/operator_info",
            HttpMethod.GET,
            200
        ).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.direct_user").value(true))
            .andExpect(jsonPath("$.result.blocked").value(true))
    }

    @Test
    fun getOperatorInfo_language_success() {
        steps.userSteps().setUserProperty(userInfo, User.LANG, Language.TR)
        testAuthHelper.setSecurityContext()
        doRequest(
            "/public/user/operator_info",
            HttpMethod.GET,
            200
        ).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.lang").value("ru"))
    }

    @Test
    fun getOperatorInfo_languageDefault_success() {
        doRequest(
            "/public/user/operator_info",
            HttpMethod.GET,
            200
        ).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.lang").value("ru"))
    }

    @Test
    fun getFeature_enabled() {
        featureSteps.enableClientFeature(FeatureName.USE_KOTLIN_FOR_V1_USER)
        doRequest(
            "/public/user/features",
            HttpMethod.GET,
            200
        ).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.use_kotlin_for_v1_user").value(true))
    }

    @Test
    fun getFeature_disable() {
        doRequest(
            "/public/user/features",
            HttpMethod.GET,
            200
        ).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.use_kotlin_for_v1_user").value(false))
    }
}
