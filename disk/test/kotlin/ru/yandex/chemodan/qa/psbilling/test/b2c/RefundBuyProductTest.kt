package ru.yandex.chemodan.qa.psbilling.test.b2c

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.chemodan.qa.psbilling.client.PsBillingClient
import ru.yandex.chemodan.qa.psbilling.client.TusClient
import ru.yandex.chemodan.qa.psbilling.extensions.assert
import ru.yandex.chemodan.qa.psbilling.model.psbilling.V3ProductSetKeyProductRs
import ru.yandex.chemodan.qa.psbilling.model.tus.TusUserRs
import ru.yandex.chemodan.qa.psbilling.steps.V3ProductsetKeyProductStep
import ru.yandex.chemodan.qa.psbilling.steps.assertUserServiceActualization
import ru.yandex.chemodan.qa.psbilling.steps.buyProduct

@DisplayName("Рефанд покупки продукт сета mail_pro_b2c имея самый дорогой продукт")
class RefundBuyProductTest {

    @Test
    fun `Попытка покупки продукта имея самую дорогую`() {
        val user = TusClient.createUser()
        val products = listProducts(user)

        val major = products.first()
        buyProduct(user, major)

        products
            .drop(1)
            .map { product ->
                assertEmptyProductSet(user)

                buyProduct(user, product)
                assertUserServiceActualization(user, major)
            }
    }

    private fun listProducts(user: TusUserRs) = PsBillingClient.v3ProductSetKeyProduct("mail_pro_b2c", user.uid)
        .`as`(V3ProductSetKeyProductRs::class.java)
        .toProduct4Test()
        .reversed()

    private fun assertEmptyProductSet(user: TusUserRs) {
        PsBillingClient.v3ProductSetKeyProduct("mail_pro_b2c", user.uid) assert {
            V3ProductsetKeyProductStep.emptyProducts(this)
        }
    }
}
