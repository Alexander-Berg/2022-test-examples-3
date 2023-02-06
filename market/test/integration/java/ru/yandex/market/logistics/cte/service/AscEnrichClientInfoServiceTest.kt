package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto
import ru.yandex.market.logistics.lom.model.dto.OrderDto
import ru.yandex.market.logistics.lom.model.enums.ContactType
import ru.yandex.market.logistics.lom.model.page.PageResult

class AscEnrichClientInfoServiceTest(
    @Autowired private val ascEnrichClientInfoService: AscEnrichClientInfoService,
    @Autowired private val lomClient: LomClient

) : IntegrationTest() {


    @Test
    @DatabaseSetup("classpath:service/asc-enrich-client-info/before.xml")
    @ExpectedDatabase(
        value="classpath:service/asc-enrich-client-info/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun testClientInfoEnrichSuccessfully() {

        val contacts1 = OrderContactDto.builder().contactType(ContactType.RECIPIENT)
            .personalFullnameId("name1")
            .personalPhoneId("phone1")
            .build()

        val contacts2 = OrderContactDto.builder().contactType(ContactType.CONTACT)
            .personalFullnameId("name2")
            .personalPhoneId("phone2")
            .build()

        val contacts3 = OrderContactDto.builder().contactType(ContactType.RECIPIENT)
            .personalFullnameId("name3")
            .personalPhoneId("phone3")
            .build()

        val contacts4 = OrderContactDto.builder().contactType(ContactType.CONTACT)
            .personalFullnameId("name4")
            .personalPhoneId("phone4")
            .build()

        val orderDto1 = OrderDto()
        orderDto1.id = 10
        orderDto1.externalId = "1"
        orderDto1.contacts = listOf(contacts1, contacts2)

        val orderDto2 = OrderDto()
        orderDto2.id = 20
        orderDto2.externalId = "2"
        orderDto2.contacts = listOf(contacts3, contacts4)

        val pageResult = PageResult<OrderDto>()
        pageResult.data = listOf(orderDto1, orderDto2)

        whenever(lomClient.searchOrders(any(), any())).thenReturn(pageResult)

        ascEnrichClientInfoService.enrich(listOf(1, 2))

    }


}
