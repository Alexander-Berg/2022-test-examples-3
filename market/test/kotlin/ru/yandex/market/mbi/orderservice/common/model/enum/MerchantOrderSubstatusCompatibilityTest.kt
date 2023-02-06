package ru.yandex.market.mbi.orderservice.common.model.enum

import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Test
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus

class MerchantOrderSubstatusCompatibilityTest {

    @Test
    fun `test compatibility with checkouter models`() {
        val checkouterSubstatuses = OrderSubstatus.values()

        for (checkouterSubstatus in checkouterSubstatuses) {
            assertThatNoException().isThrownBy {
                MerchantOrderSubstatus.fromCheckouterSubstatus(checkouterSubstatus)
            }
        }
    }
}
