package ru.yandex.direct.web.entity.uac.controller

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore
import ru.yandex.direct.core.entity.uac.model.AltAppStore
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerGetAltAppStoresTest {
    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    private lateinit var mockMvc: MockMvc
    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Suppress("unused")
    private fun testData() = listOf(
        listOf(
            setOf(
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY, MobileAppAlternativeStore.VIVO_APP_STORE,
                MobileAppAlternativeStore.SAMSUNG_GALAXY_STORE, MobileAppAlternativeStore.XIAOMI_GET_APPS
            ),
            setOf(
                AltAppStore.HUAWEI_APP_GALLERY, AltAppStore.VIVO_APP_STORE,
                AltAppStore.SAMSUNG_GALAXY_STORE, AltAppStore.XIAOMI_GET_APPS
            )
        ),
        listOf(
            setOf(MobileAppAlternativeStore.XIAOMI_GET_APPS),
            setOf(AltAppStore.XIAOMI_GET_APPS)
        ),
        listOf(null, null),
        listOf(
            setOf(MobileAppAlternativeStore.HUAWEI_APP_GALLERY, MobileAppAlternativeStore.VIVO_APP_STORE),
            setOf(AltAppStore.HUAWEI_APP_GALLERY, AltAppStore.VIVO_APP_STORE)
        ),
    )

    @Test
    @TestCaseName("AltAppStores {0}, expected altAppStores {1}")
    @Parameters(method = "testData")
    fun testGetCampaignsWithAltAppStores(
        altAppStores: Set<MobileAppAlternativeStore>?,
        expectedAltAppStores: Set<AltAppStore>?
    ) {
        val uacCampaignInfo = uacCampaignSteps.createMobileAppCampaign(clientInfo, altAppStores = altAppStores)

        val alternativeStores = sendRequestAndGetAltAppStores(uacCampaignInfo.uacCampaign.id)

        assertThat(alternativeStores).isEqualTo(expectedAltAppStores)
    }

    private fun sendRequestAndGetAltAppStores(uacCampaignId: String): Set<*>? {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/${uacCampaignId}?ulogin=${clientInfo.login}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val altAppStoresNode = resultJsonTree["result"]["alt_app_stores"]
        return try {
            JsonUtils.MAPPER.treeToValue(altAppStoresNode, List::class.java)
                .map { AltAppStore.valueOf(it.toString()) }
                .toSet()
        } catch (e: NullPointerException) {
            null
        }
    }
}
