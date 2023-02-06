package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.google.common.collect.Lists
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.config.idm.IdmRoleSecurityConfigurationAdapter
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.nio.charset.StandardCharsets

class
CalendaringInfoControllerWarehousesTest : AbstractContextualTest() {

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml")
    fun testSuccessful() {

        Mockito.`when`(lmsClient!!.searchPartners(ArgumentMatchers.any()))
            .thenReturn(
                listOf(
                    createPartnerResponse(145, "Маршрут ФФ", false),
                    createPartnerResponse(147, "Яндекс.Маркет Ростов", true),
                    createPartnerResponse(172, "Яндекс Маркет Софьино", false),
                    createPartnerResponse(171, "Яндекс Маркет Томилино", false),
                    createPartnerResponse(1337, "Какой-то ещё", false)
                )
            )

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/warehouses")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .header(IdmRoleSecurityConfigurationAdapter.USER_LOGIN_HEADER, "test_login")
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent("fixtures/controller/calendaring-info/warehouses/get-successfully/response.json")
        JSONAssert.assertEquals(
            expectedJson,
            result.response.getContentAsString(StandardCharsets.UTF_8),
            JSONCompareMode.NON_EXTENSIBLE
        )
    }

    private fun createPartnerResponse(id: Long, name: String, calendaringEnabled: Boolean): PartnerResponse? {
        return PartnerResponse.newBuilder().id(id).readableName(name)
            .partnerType(PartnerType.FULFILLMENT)
            .params(
                Lists.newArrayList(
                    PartnerExternalParam(
                        PartnerExternalParamType.IS_CALENDARING_ENABLED.name,
                        "descr", calendaringEnabled.toString()
                    )
                )
            ).build()
    }

}
