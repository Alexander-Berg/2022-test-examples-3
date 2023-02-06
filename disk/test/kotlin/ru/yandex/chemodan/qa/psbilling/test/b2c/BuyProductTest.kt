package ru.yandex.chemodan.qa.psbilling.test.b2c

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import ru.yandex.chemodan.qa.psbilling.client.PsBillingClient
import ru.yandex.chemodan.qa.psbilling.client.TusClient
import ru.yandex.chemodan.qa.psbilling.extensions.AnonymousProductSetTestTemplate
import ru.yandex.chemodan.qa.psbilling.model.psbilling.V3ProductSetKeyProductRs
import ru.yandex.chemodan.qa.psbilling.model.psbilling.utils.Product
import ru.yandex.chemodan.qa.psbilling.model.tus.TusUserRs
import ru.yandex.chemodan.qa.psbilling.steps.assertUserServiceActualization
import ru.yandex.chemodan.qa.psbilling.steps.buyProduct

@DisplayName("Покупка продукт сета mail_pro_b2c")
class BuyProductTest {

    @TestTemplate
    @ExtendWith(AnonymousProductSetTestTemplate::class)
    fun `Покупка продукта - Каждый продукт новому пользователю`(product: Product) {
        val user = TusClient.createUser()

        buyProduct(user, product)
        assertUserServiceActualization(user, product)
    }

    @Test
    fun `Покупка продукта - Каждый продукт одному пользователю (апгрейд)`() {
        val user = TusClient.createUser()
        var product = listProducts(user).firstOrNull()

        while (product != null) {
            buyProduct(user, product)
            assertUserServiceActualization(user, product)

            product = listProducts(user).firstOrNull()
        }
    }

    private fun listProducts(user: TusUserRs) = PsBillingClient.v3ProductSetKeyProduct("mail_pro_b2c", user.uid)
        .`as`(V3ProductSetKeyProductRs::class.java)
        .toProduct4Test()
}
