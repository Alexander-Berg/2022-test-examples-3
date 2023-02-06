package ru.yandex.market.pricingmgmt.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.config.cache.CacheNames
import ru.yandex.market.pricingmgmt.model.dto.Employee
import ru.yandex.market.pricingmgmt.model.dto.staff.PersonDto
import ru.yandex.market.pricingmgmt.model.dto.staff.PersonFields
import ru.yandex.market.pricingmgmt.model.dto.staff.PersonNameDto
import ru.yandex.market.pricingmgmt.model.dto.staff.TextDto
import ru.yandex.market.pricingmgmt.service.client.StaffClient

internal class CacheableEmployeeSearchServiceImplTest : AbstractFunctionalTest() {
    @Autowired
    private lateinit var cacheableEmployeeSearchServiceImpl: CacheableEmployeeSearchServiceImpl

    @Autowired
    private lateinit var cacheManager: CacheManager

    @MockBean
    private lateinit var staffClient: StaffClient

    @Test
    fun searchEmployeesWithCache() {
        // prepare
        reset(staffClient)
        val clientResponse = listOf(
            PersonDto(
                id = 1L,
                login = "ivanov",
                name = PersonNameDto(
                    first = TextDto(ru = "Иван", en = "Ivan"),
                    last = TextDto(ru = "Иванов", "Ivanov")
                )
            ),
            PersonDto(
                id = 2L,
                login = "petrov",
                name = PersonNameDto(
                    first = TextDto(ru = "Петр", en = "Petr"),
                    last = TextDto(ru = "Петров", "Petrov")
                )
            )
        )
        `when`(
            staffClient.getEmployees(
                listOf("ivanov", "petrov"),
                listOf(PersonFields.LOGIN, PersonFields.NAME)
            )
        ).thenReturn(clientResponse)

        // act
        val actualResult1 = cacheableEmployeeSearchServiceImpl.searchEmployees(listOf("ivanov", "petrov"))
        val actualResult2 = cacheableEmployeeSearchServiceImpl.searchEmployees(listOf("ivanov", "petrov"))

        // verify
        val expectedResult = listOf(
            Employee(id = 1L, login = "ivanov", firstName = "Иван", lastName = "Иванов"),
            Employee(id = 2L, login = "petrov", firstName = "Петр", lastName = "Петров")
        )
        assertEquals(expectedResult, actualResult1)
        assertEquals(expectedResult, actualResult2)

        assertNotNull(cacheManager.getCache(CacheNames.EMPLOYEE_SEARCH))

        verify(staffClient, times(1)).getEmployees(
            listOf("ivanov", "petrov"),
            listOf(PersonFields.LOGIN, PersonFields.NAME)
        )
    }
}
