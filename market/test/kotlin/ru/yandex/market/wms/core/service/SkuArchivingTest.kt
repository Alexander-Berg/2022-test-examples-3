package ru.yandex.market.wms.core.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class SkuArchivingTest: IntegrationTest() {
    @Autowired
    private lateinit var skuArchiveService: SkuArchiveService

    @Test
    @DisplayName("SKU для удаления успешно добавляются в таблицу заданий")
    @ExpectedDatabase(
        value = "/service/sku-archive/insert-tasks-test/db-expected.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun insertTasksTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/markForArchiving")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("service/sku-archive/insert-tasks-test/request.json"))
        )
        .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/service/sku-archive/sku-correctly-archives/db-setup.xml")
    @ExpectedDatabase("/service/sku-archive/sku-correctly-archives/db-expected.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("SKU для удаления корректно обрабатываются")
    fun skuCorrectlyArchivesTest() {
        skuArchiveService.runArchiving("TEST_RUN")
    }
}
