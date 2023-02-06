package ru.yandex.market.partner.placement.wizard

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.partner.placement.AbstractFunctionalTest

class PartnerPlacementWizardControllerTest: AbstractFunctionalTest() {

    @Test
    fun testOk() {
        val result = partnerPlacementWizardApiClient.getPartnerSteps(1L).schedule().join()
        Assertions.assertNotNull(result)
    }
}
