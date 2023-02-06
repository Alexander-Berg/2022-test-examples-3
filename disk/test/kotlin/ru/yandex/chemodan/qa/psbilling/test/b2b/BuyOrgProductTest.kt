package ru.yandex.chemodan.qa.psbilling.test.b2b

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.chemodan.qa.psbilling.client.PsBillingClient
import ru.yandex.chemodan.qa.psbilling.model.psbilling.V3ProductSetKeyProductRs
import ru.yandex.chemodan.qa.psbilling.model.tus.TusUserRs
import ru.yandex.chemodan.qa.psbilling.util.randomINNFiz
import ru.yandex.chemodan.qa.psbilling.util.randomKPP

@DisplayName("Покупка продукт у организации")
class BuyOrgProductTest {
    @Test
    fun `Покупка продукта организация`() {
        //val user = TusClient.createUser()

        // /available-payment-data
        //
        // Смотрим есть ли у нас платёжные данные.
        // Если нету -> новый договор
        // Если есть -> ничего не делаем (используем старые данные)
        //
        // Создается орга в директории
        // /directory/create-organization-without-domain
        // {name: "Организация" | source: "DiskForBusiness"}
        //
        // Цепляем
        // /accept_agreement
        // {orgId: 109932 | productOwnerCode: "yandex_mail"}
        //
        // Получаем продукты
        // /get-products
        // {orgId: 109932}
        //
        // Подписываемся
        // /subscribe-with-payment-data
        // {orgId: 109932 | productId: "prepaid_org_mail_pro_standard100_late_april_2022"}
        // Еще отсылается 1.  requisitesId: "4093541324_1357801145_21064383", но думаю это лишнее.

        randomINNFiz()
        randomKPP()
    }

    private fun listProducts(user: TusUserRs) = PsBillingClient.v3ProductSetKeyProduct("mail_pro_b2c", user.uid)
        .`as`(V3ProductSetKeyProductRs::class.java)
        .toProduct4Test()
}

