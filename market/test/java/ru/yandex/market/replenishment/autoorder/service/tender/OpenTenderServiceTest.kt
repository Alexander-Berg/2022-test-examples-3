package ru.yandex.market.replenishment.autoorder.service.tender

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.ControllerTest
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import ru.yandex.market.replenishment.autoorder.service.client.Cabinet1PClient
import java.time.LocalDateTime

@MockBean(Cabinet1PClient::class)
@WithMockLogin
class OpenTenderServiceTest : ControllerTest() {
    companion object {
        val MOCKED_DATE_TIME: LocalDateTime = LocalDateTime.of(2020, 9, 6, 8, 0)
    }

    private val excelTestingHelper = ExcelTestingHelper(this)

    @Autowired
    private lateinit var cabinet1PClient: Cabinet1PClient

    @Before
    fun setUp() {
        setTestTime(MOCKED_DATE_TIME)
    }

    @Test
    @DbUnitDataSet(
        before = ["OpenTenderServiceTest.createOpenTender.before.csv"],
        after = ["OpenTenderServiceTest.createOpenTender.after.csv"]
    )
    fun uploadOpenTender() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/open/create",
            "OpenTenderServiceTest_uploadOpenTender.xlsx"
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$").value(1))
    }

    @Test
    @DbUnitDataSet(before = ["OpenTenderServiceTest.createOpenTender_noMskus.before.csv"])
    fun uploadOpenTender_noMskus() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/open/create",
            "OpenTenderServiceTest_uploadOpenTender.xlsx"
        ).andExpect(status().isIAmATeapot())
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Товара с msku = 1 нет в системе\\n" +
                            "Товара с msku = 2 нет в системе"
                    )
            );
    }

    @Test
    @DbUnitDataSet(before = ["OpenTenderServiceTest.createOpenTender.before.csv"])
    fun uploadOpenTender_noCatman() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/open/create",
            "OpenTenderServiceTest_uploadOpenTender_noCatman.xlsx"
        ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value("Не указан ответственный"))
    }

    @Test
    @DbUnitDataSet(before = ["OpenTenderServiceTest.createOpenTender.before.csv"])
    fun uploadOpenTender_noWh() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/open/create",
            "OpenTenderServiceTest_uploadOpenTender_noWh.xlsx"
        ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value("Неверно указан склад ''"));
    }

    @Test
    fun getOpenTenderTemplate() {
        mockMvc.perform(get("/api/v1/tender/open/template"))
            .andExpect(status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["OpenTenderServiceTest.syncAnonSuppliers.before.csv"],
        after = ["OpenTenderServiceTest.syncAnonSuppliers.after.csv"]
    )
    fun syncAnonSuppliers() {
        mockGetPartnerIdToRsId()
        mockMvc.perform(post("/api/v1/tender/open/1/sync-suppliers")).andExpect(status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["OpenTenderServiceTest.syncAnonSuppliers.before.csv"],
        after = ["OpenTenderServiceTest.syncAnonSuppliers.before.csv"]
    )
    fun syncAnonSuppliers_noMapping() {
        Mockito.`when`(cabinet1PClient.getPartnerIdToRsId(listOf(42, 43))).thenReturn(emptyMap())
        mockMvc.perform(post("/api/v1/tender/open/1/sync-suppliers")).andExpect(status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["OpenTenderServiceTest.syncAnonSuppliers.before.csv"],
        after = ["OpenTenderServiceTest.syncAnonSuppliers_oneMapping.after.csv"]
    )
    fun syncAnonSuppliers_oneMapping() {
        Mockito.`when`(cabinet1PClient.getPartnerIdToRsId(listOf(42, 43))).thenReturn(mapOf(42L to "000001"))
        mockMvc.perform(post("/api/v1/tender/open/1/sync-suppliers")).andExpect(status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["OpenTenderServiceTest.syncAnonSuppliers_noSuppliers.before.csv"],
        after = ["OpenTenderServiceTest.syncAnonSuppliers_noSuppliers.before.csv"]
    )
    fun syncAnonSuppliers_noSuppliers() {
        mockGetPartnerIdToRsId()
        mockMvc.perform(post("/api/v1/tender/open/1/sync-suppliers")).andExpect(status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["OpenTenderServiceTest.syncAnonSuppliers_oneSupplier.before.csv"],
        after = ["OpenTenderServiceTest.syncAnonSuppliers_oneMapping.after.csv"]
    )
    fun syncAnonSuppliers_oneSupplier() {
        mockGetPartnerIdToRsId()
        mockMvc.perform(post("/api/v1/tender/open/1/sync-suppliers")).andExpect(status().isOk)
    }

    private fun mockGetPartnerIdToRsId() {
        Mockito.`when`(cabinet1PClient.getPartnerIdToRsId(listOf(42, 43)))
            .thenReturn(mapOf(42L to "000001", 43L to "000002"))
    }
}