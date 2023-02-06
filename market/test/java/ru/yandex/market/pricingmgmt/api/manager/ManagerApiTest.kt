package ru.yandex.market.pricingmgmt.api.manager

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.exception.ApiErrorException
import ru.yandex.market.pricingmgmt.model.dto.Employee
import ru.yandex.market.pricingmgmt.service.EmployeeSearchService
import ru.yandex.mj.generated.server.model.ManagerDto

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory", value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class ManagerApiTest : ControllerTest() {

    @MockBean
    @Qualifier("cacheable")
    private lateinit var employeeSearchService: EmployeeSearchService

    @BeforeEach
    fun beforeEach() {
        reset(employeeSearchService)
    }

    @DbUnitDataSet(before = ["ManagerApiTest.csv"])
    @Test
    fun getMarkomsTest() {
        val expectedResult = listOf(
            ManagerDto().login("catManager").department("HOME").firstName("Иван").lastName("Иванов"),
            ManagerDto().login("catManager1").department("FASHION").firstName("Петр").lastName("Петров")
        )

        val searchEmployeesResult = listOf(
            Employee(id = 1L, login = "catManager", firstName = "Иван", lastName = "Иванов"),
            Employee(id = 2L, login = "catManager1", firstName = "Петр", lastName = "Петров")
        )
        `when`(employeeSearchService.searchEmployees(listOf("catManager", "catManager1"))).thenReturn(
            searchEmployeesResult
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/markoms").contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResult)))
    }

    @DbUnitDataSet(before = ["ManagerApiTest.csv"])
    @Test
    fun getMarkoms_employeeSearchApiFailedTest() {
        `when`(
            employeeSearchService.searchEmployees(
                listOf(
                    "catManager",
                    "catManager1"
                )
            )
        ).thenThrow(ApiErrorException("error"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/markoms").contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is5xxServerError)
    }

    @DbUnitDataSet(before = ["ManagerApiTest.csv"])
    @Test
    fun getTradesTest() {
        val expectedResult = listOf(
            ManagerDto().login("tradeManager").department("DIY").firstName("Иван").lastName("Иванов"),
            ManagerDto().login("tradeManager1").department("HOME").firstName("Петр").lastName("Петров")
        )

        val searchEmployeesResult = listOf(
            Employee(id = 1L, login = "tradeManager", firstName = "Иван", lastName = "Иванов"),
            Employee(id = 2L, login = "tradeManager1", firstName = "Петр", lastName = "Петров")
        )
        `when`(employeeSearchService.searchEmployees(listOf("tradeManager", "tradeManager1"))).thenReturn(
            searchEmployeesResult
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/trade-managers").contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResult)))
    }

    @DbUnitDataSet(before = ["ManagerApiTest.csv"])
    @Test
    fun getTrades_employeeSearchApiFailedTest() {
        `when`(
            employeeSearchService.searchEmployees(
                listOf(
                    "tradeManager",
                    "tradeManager1"
                )
            )
        ).thenThrow(ApiErrorException("error"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/trade-managers").contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is5xxServerError)
    }
}
