package ru.yandex.market.pricingmgmt.api.price_log

import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationFilter

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
@WithMockUser(
    username = PassportAuthenticationFilter.LOCAL_DEV,
    roles = ["PRICING_MGMT_ACCESS", "VIEW_PRICES_JOURNALS_KOMANDA2"]
)
class PriceLogApiTest : ControllerTest() {

    @DbUnitDataSet(before = ["PriceLogApiTest.getLogs.before.csv"])
    @Test
    fun testGetLogs() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/price-logs/?msku=100&ssku=111")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(5))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].ssku").value("111"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].msku").value(100))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].title").value("title1"))
            //.andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].createdAt").value("2021-12-23T02:00:00Z"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].price").value(10.0))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].priceTypeName").value("priceType1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].crossedPrice").value(20.0))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].crossedPriceTypeName").value("priceType2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].rule").value("purchase_price"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[?(@.id==1)].dcoAlerts")
                    .value("[\"Перечеркнутая цена меньше нижней границы правила\", \"Цена меньше нижней границы правила\"]")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].vat").value("vat1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].availableForBusiness").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].prohibitedForPersons").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].warehouse.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].warehouse.name").value("test1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].journal.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].journal.comment").value("comment1"))
            //.andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].journal.createdAt").value("2021-12-23T02:00:00Z"))
            //.andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].journal.startAt").value("2021-12-23T02:00:00Z"))
            //.andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].journal.endAt").value("2022-02-23T06:00:00Z"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.id==1)].journal.login").value("localDev"))
    }

    @DbUnitDataSet(before = ["PriceLogApiTest.getLogs.before.csv"])
    @Test
    fun testGetLogsFilteredByDates() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/price-logs/?msku=100&ssku=111&startAt=2021-12-18T01:00:00Z&endAt=2021-12-22T01:00:00Z")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
    }

    @DbUnitDataSet(before = ["PriceLogApiTest.getLogs.before.csv"])
    @Test
    fun testGetLogsMissedRole() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/price-logs/?msku=200")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Не хватает роли для просмотра"))
    }

    @DbUnitDataSet(before = ["PriceLogApiTest.getLogs.before.csv"])
    @Test
    fun testGetLogsCount() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/price-logs/count?msku=100")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").value(7))
    }
}
