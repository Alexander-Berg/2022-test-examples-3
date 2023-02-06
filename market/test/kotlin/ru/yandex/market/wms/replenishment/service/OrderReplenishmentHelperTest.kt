package ru.yandex.market.wms.replenishment.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class OrderReplenishmentHelperTest {
    @Test
    fun boxCalcTest() {
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(1, 3, 25)).isEqualTo(3)
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(1, 3, 35)).isEqualTo(1)
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(1, 5, 25)).isEqualTo(1)
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(2, 5, 25)).isEqualTo(5)
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(10, 100, 11)).isEqualTo(10)
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(10, 100, 9)).isEqualTo(100)
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(10, 0, 1)).isEqualTo(0)
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(0, 0, 0)).isEqualTo(0)
        Assertions.assertThat(OrderReplenishmentHelper.calcBoxTakeQty(10, 0, 111)).isEqualTo(0)
    }

    @Test
    fun remainToPickFromPalletCalcTest() {
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(1, 30, 10, 10)).isEqualTo(2)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(1, 30, 20, 10)).isEqualTo(5)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(3, 30, 10, 10)).isEqualTo(0)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(6, 30, 20, 10)).isEqualTo(0)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(5, 30, 20, 10)).isEqualTo(1)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(1, 30, 10, 95)).isEqualTo(29)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(33, 30, 10, 95)).isEqualTo(0)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(27, 30, 10, 95)).isEqualTo(3)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(1, 2, 10, 51)).isEqualTo(1)
        Assertions.assertThat(OrderReplenishmentHelper.calcRemainQtyToPick(1, 2, 10, 49)).isEqualTo(0)
    }
}
