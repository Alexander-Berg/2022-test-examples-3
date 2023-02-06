package ru.yandex.market.logistics.yard.facade

import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.PassNumberFacade

class PassNumberFacadeTest(@Autowired private val passNumberFacade: PassNumberFacade) :
    AbstractSecurityMockedContextualTest() {

    @Test
    fun getNextPassNumber() {
        assertions().assertThat(passNumberFacade.getNextPassNumber()).isEqualTo("00001")
        assertions().assertThat(passNumberFacade.getNextPassNumber()).isEqualTo("00012")
    }
}
