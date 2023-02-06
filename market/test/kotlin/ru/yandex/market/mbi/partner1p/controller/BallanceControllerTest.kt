package ru.yandex.market.mbi.partner1p.controller

import net.javacrumbs.jsonunit.JsonAssert
import net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals
import net.javacrumbs.jsonunit.core.Option
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.util.UriComponentsBuilder
import ru.yandex.market.mbi.balance.model.ClientContractInfo
import ru.yandex.market.mbi.balance.model.currency.Currency
import ru.yandex.market.mbi.balance.service.BalanceService
import ru.yandex.market.mbi.balance.xmlrps.model.ContractType
import ru.yandex.market.mbi.partner1p.FunctionalTest
import ru.yandex.market.mbi.partner1p.FunctionalTestHelper
import java.time.LocalDate

/**
 *
 * @author lozovskii@yandex-team.ru
 */
open class BallanceControllerTest : FunctionalTest() {

    @Autowired
    lateinit var balanceService: BalanceService

    @BeforeEach
    open fun setup() {
        Mockito.`when`(balanceService.getClientContracts(1, ContractType.GENERAL, null, null, null, null, null))
            .thenReturn(
                listOf(
                    ClientContractInfo.ClientContractInfoBuilder()
                        .withCurrency(Currency.RUR)
                        .withDt(LocalDate.of(2021, 6, 23))
                        .withExternalId("123456789")
                        .withId(1)
                        .withPersonId(11)
                        .withServices(IntArray(3) { i -> i })
                        .isActive(true)
                        .withOfferAccepted(true)
                        .isCancelled(false)
                        .isFaxed(false)
                        .isSigned(true)
                        .isSuspended(false)
                        .build(),
                    ClientContractInfo.ClientContractInfoBuilder()
                        .withCurrency(Currency.RUR)
                        .withDt(LocalDate.of(2021, 6, 23))
                        .withExternalId("123456789")
                        .withId(1)
                        .withPersonId(11)
                        .withServices(arrayOf(0, 1, 1126).toIntArray())
                        .isActive(true)
                        .withOfferAccepted(false)
                        .isCancelled(false)
                        .isFaxed(false)
                        .isSigned(true)
                        .isSuspended(false)
                        .build(),
                    ClientContractInfo.ClientContractInfoBuilder()
                        .withCurrency(Currency.RUR)
                        .withDt(LocalDate.of(2021, 6, 23))
                        .withExternalId("123456789")
                        .withId(1)
                        .withPersonId(11)
                        .isActive(true)
                        .isCancelled(false)
                        .isFaxed(false)
                        .isSigned(true)
                        .isSuspended(false)
                        .build()
                )
            )
    }

    @Test
    fun `test get all partner contract offer list`() {

        val query = UriComponentsBuilder.fromUriString("${baseUrl()}/api/v1/partners/1/balance/contractOffer/list")
            .build()
            .toUriString()

        val expected = getStringResource("/test_get_all_partner_contract_offer_list/expected.json")
        val response = FunctionalTestHelper.get(query)

        assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }
}
