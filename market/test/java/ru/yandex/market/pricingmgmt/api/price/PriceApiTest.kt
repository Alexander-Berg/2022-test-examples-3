package ru.yandex.market.pricingmgmt.api.price

import org.apache.commons.httpclient.HttpStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.TestUtils
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.repository.postgres.SalesRepository
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import ru.yandex.market.pricingmgmt.util.configurers.DataCampConfigurer

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class PriceApiTest : ControllerTest() {

    companion object {
        private val JSON_DATE_TIME_1 = DateTimeTestingUtil.createJsonDateTime(2021, 12, 23, 8, 0, 0)
        private val JSON_DATE_TIME_2 = DateTimeTestingUtil.createJsonDateTime(2022, 1, 23, 12, 0, 0)
        private val JSON_DATE_TIME_LAST_SENT_TO_PAPI = DateTimeTestingUtil.createJsonDateTime(2022, 3, 20, 12, 34, 56)

        private val JSON_DATACAMP_SUCCESS = TestUtils.readResourceFile("/datacamp/partners_success.json")
    }

    @Autowired
    private val salesRepository: SalesRepository? = null

    @Autowired
    private lateinit var dataCampConfigurer: DataCampConfigurer

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPrices() {
        salesRepository?.refreshMskuSalesView()
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/")
                .contentType("application/json")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(6))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].ssku").value("111"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].msku").value(111L))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].warehouseId").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].isCoreFix").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].manufacturer").value("sony"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].title").value("title1 and"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].purchasePrice").value(123))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].cost").value(23.1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].sales28d").value(7))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].refminPrice").value(11))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].refminDomain").value("domain1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].refminUrl").value("url1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].refminSoft").value(22))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].refminDomainSoft").value("domain_soft1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].refminUrlSoft").value("url_soft1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].vendor").value("Луч"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].min3pPrice").value(123))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].supplier.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].supplier.name").value("supplier1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].price").value(20))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].crossedPrice").value(30))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].stock").value(20))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[0].competitorUrl")
                    .value("https://www.wildberries.ru/catalog/11900005/detail.aspx")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].lowerBound").value(18789))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].upperBound").isEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].rule").value("ref_min_price"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[0].dcoAlerts").value("[\"Цена меньше нижней границы правила\"]")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[0].priceTypeName")
                    .value("Нижняя цена для скидки с фиксированной продажной ценой")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].crossedPriceTypeName").value("Зачеркнутая ДЦО"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].journal.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].journal.priceType").value("PRIORITY_PRICE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].journal.startAt").value(JSON_DATE_TIME_1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].journal.createdAt").value(JSON_DATE_TIME_1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].journal.endAt").value(JSON_DATE_TIME_2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].journal.comment").value("The Comment"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].journal.login").value("vasya"))
            // https://st.yandex-team.ru/PRICINGMNGMT-383
            // vat_value == 0, поэтому min3pPrice = (45 * (1 + 0 / 100.0)) = 45
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].purchasePrice").value(45.0))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].min3pPrice").value(45.0))
            // vat_value == 20, поэтому min3pPrice = (100 * (1 + 20 / 100.0)) = 120
            .andExpect(MockMvcResultMatchers.jsonPath("$[5].purchasePrice").value(100.0))
            .andExpect(MockMvcResultMatchers.jsonPath("$[5].min3pPrice").value(120.0))
            // ни разу не отправлялась в PAPI, отдает null
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].lastSentToPapi").isEmpty)
            // была отправка в PAPI
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[5].lastSentToPapi").value(JSON_DATE_TIME_LAST_SENT_TO_PAPI)
            )
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesFilteredByDepartment() {

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?departmentId=2")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesFilteredByVendor() {

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?vendorId=2")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesFilteredByMsku() {

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?warehouseId=1&departmentId=1&msku=444")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesFilteredBySsku() {

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?ssku=115")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesFilteredBySupplierId() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?supplierId=2")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].supplier.id").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].ssku").value("112"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].ssku").value("115"))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesFilteredByTitle() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?title=title1")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesFilteredWarehouseId() {
        salesRepository?.refreshMskuSalesView()
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?warehouseId=1")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(5))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].sales28d").value(4))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesPagination() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?page=1&count=2")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesCount() {
        salesRepository?.refreshMskuSalesView()
        val count = 6
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(count))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/count/")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").value(count))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/count/?page=0&count=2")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").value(count))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/count/?journalId=3")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").value(3))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetPricesFilteredByJournal() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/?journalId=3")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].ssku").value("113"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].ssku").value("114"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].ssku").value("115"))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetOfferUrlInfo_withWarehouse_success() {
        dataCampConfigurer.mockDataCampResponse(HttpStatus.SC_OK, JSON_DATACAMP_SUCCESS)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/get-offer-url-info?ssku=123.abc&warehouseId=147")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.ssku").value("123.abc"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.modelId").value("1133"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msku").value("1234"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.warehouseId").value("147"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.wareMd5").value("test_md5_hash"))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetOfferUrlInfo_withoutWarehouse_success() {
        dataCampConfigurer.mockDataCampResponse(HttpStatus.SC_OK, JSON_DATACAMP_SUCCESS)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/get-offer-url-info?ssku=123.abc")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.ssku").value("123.abc"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.modelId").value("1133"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msku").value("1234"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.warehouseId").value("147"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.wareMd5").value("test_md5_hash"))
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetOfferUrlInfo_dataCamp_error() {
        dataCampConfigurer.mockDataCampResponse(HttpStatus.SC_BAD_REQUEST, "")

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/get-offer-url-info?ssku=123.abc")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    @DbUnitDataSet(before = ["PriceApiTest.getPrices.before.csv"])
    @Test
    fun testGetOfferUrlInfo_sskuIsEmpty_error() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/prices/get-offer-url-info?ssku=")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}
