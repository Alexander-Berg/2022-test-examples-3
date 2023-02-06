package ru.yandex.market.abo.core.billing.calc.calculator

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.core.billing.calc.calculator.type.BillingReportCalculatorTest
import ru.yandex.market.abo.core.shopdata.ManualShopDataService
import ru.yandex.market.abo.core.shopdata.model.ShopData
import ru.yandex.market.abo.core.shopdata.model.ShopDataType

/**
 * @author antipov93.
 * @date 30.06.18.
 */
class ShopDataBillingTest @Autowired constructor(
    val manualShopDataService: ManualShopDataService
) : BillingReportCalculatorTest(2) {

    override fun populateData() {
        assessorIds.forEach { userId: Long ->
            val shopIds = nextIds()
            shopIds
                .map { ShopData(it, it.toString(), ShopDataType.JUR_NAME) }
                .forEach { manualShopDataService.addNewData(it, userId) }

            addItemsToUser(userId, 22, shopIds.size)
        }
    }

}
