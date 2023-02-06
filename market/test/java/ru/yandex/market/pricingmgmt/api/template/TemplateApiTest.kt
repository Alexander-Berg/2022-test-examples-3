package ru.yandex.market.pricingmgmt.api.template

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.model.dto.PriceExcelDTO
import ru.yandex.market.pricingmgmt.service.excel.core.ExcelHelper
import ru.yandex.market.pricingmgmt.service.excel.meta.PriceExcelMetaData
import ru.yandex.mj.generated.server.model.PriceType

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class TemplateApiTest : ControllerTest() {

    @Autowired
    var excelHelper: ExcelHelper<PriceExcelDTO>? = null

    @Test
    fun testDownloadTemplates() {
        val priceTypes = listOf(
            PriceType.RRP,
            PriceType.CROSSED_OUT_AND_SALE_PRICE,
            PriceType.CROSSED_OUT_AND_BOUNDS_FOR_DCO,
            PriceType.PRIORITY_PRICE,
            PriceType.DCO_UPPER_BOUND,
            PriceType.DCO_LOWER_BOUND
        )
        for (type in priceTypes) {
            testDownloadTemplate(type)
        }
    }

    private fun testDownloadTemplate(priceType: PriceType) {
        val excelData = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/templates/${priceType}")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsByteArray

        val headers = PriceExcelMetaData.getColumnsForImportPrices(priceType)
        val prices = excelHelper!!.read(excelData.inputStream(), headers)
        val expectedData = listOf<PriceExcelDTO>()
        Assertions.assertThat(prices).isEqualTo(expectedData)
    }
}
