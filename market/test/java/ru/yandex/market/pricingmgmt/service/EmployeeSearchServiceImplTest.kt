package ru.yandex.market.pricingmgmt.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.exception.ApiErrorException
import ru.yandex.market.pricingmgmt.model.dto.Employee
import ru.yandex.market.pricingmgmt.model.dto.staff.PersonDto
import ru.yandex.market.pricingmgmt.model.dto.staff.PersonFields
import ru.yandex.market.pricingmgmt.model.dto.staff.PersonNameDto
import ru.yandex.market.pricingmgmt.model.dto.staff.TextDto
import ru.yandex.market.pricingmgmt.service.client.StaffClient
import java.util.*

internal class EmployeeSearchServiceImplTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var employeeSearchServiceImpl: EmployeeSearchServiceImpl

    @MockBean
    private lateinit var staffClient: StaffClient

    @BeforeEach
    fun beforeEach() {
        reset(staffClient)
    }

    @Test
    fun searchEmployees_ok() {
        // prepare
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
        val actualResult = employeeSearchServiceImpl.searchEmployees(listOf("ivanov", "petrov"))

        // verify
        val expectedResult = listOf(
            Employee(id = 1L, login = "ivanov", firstName = "Иван", lastName = "Иванов"),
            Employee(id = 2L, login = "petrov", firstName = "Петр", lastName = "Петров")
        )
        assertEquals(expectedResult, actualResult)

        verify(staffClient).getEmployees(
            listOf("ivanov", "petrov"),
            listOf(PersonFields.LOGIN, PersonFields.NAME)
        )
    }

    @Test
    fun searchEmployees_clientReturnedEmptyResult() {
        // prepare
        `when`(
            staffClient.getEmployees(
                listOf("ivanov", "petrov"),
                listOf(PersonFields.LOGIN, PersonFields.NAME)
            )
        ).thenReturn(Collections.emptyList())

        // act
        val actualResult = employeeSearchServiceImpl.searchEmployees(listOf("ivanov", "petrov"))

        // verify
        assertEquals(Collections.emptyList<Employee>(), actualResult)
        verify(staffClient).getEmployees(
            listOf("ivanov", "petrov"),
            listOf(PersonFields.LOGIN, PersonFields.NAME)
        )
    }

    @Test
    fun searchEmployees_clientError() {
        // prepare
        `when`(
            staffClient.getEmployees(
                listOf("ivanov", "petrov"),
                listOf(PersonFields.LOGIN, PersonFields.NAME)
            )
        ).thenThrow(ApiErrorException("Request failed"))

        // act
        assertThrows(ApiErrorException::class.java) {
            employeeSearchServiceImpl.searchEmployees(
                listOf(
                    "ivanov",
                    "petrov"
                )
            )
        }
        verify(staffClient).getEmployees(
            listOf("ivanov", "petrov"),
            listOf(PersonFields.LOGIN, PersonFields.NAME)
        )
    }
}
