package ru.yandex.chemodan.qa.psbilling.config

import ru.yandex.chemodan.qa.psbilling.client.PaymentCard
import ru.yandex.qatools.properties.PropertyLoader
import ru.yandex.qatools.properties.annotations.Property
import ru.yandex.qatools.properties.annotations.Resource.Classpath
import ru.yandex.qatools.secrets.SecretWithKey
import ru.yandex.qatools.secrets.SecretsLoader

class TusSecret private constructor() {
    //lateinit не работает с аннотацией @SecretWithKey
    @SecretWithKey(secret = "sec-01g3g0r8f5yhxm51eg89q36c40", key = "tusOauth")
    val tvmSecret: String? = null

    init {
        SecretsLoader.populate(this)
    }

    companion object {
        val instance: TusSecret by lazy { TusSecret() }
    }
}


@Classpath("app.properties")
object GlobalTestData {
    init {
        PropertyLoader.populate(this)
        SecretsLoader.populate(this)
    }

    @Property("psbilling.uri")
    lateinit var PS_BILLING_URI: String

    @Property("pci.uri")
    lateinit var PCI_URI: String

    @Property("trust.uri")
    lateinit var TRUST_URI: String

    @Property("tus.uri")
    lateinit var TUS_URI: String

    @Property("card.success.number")
    private lateinit var cardSuccessNumber: String

    @Property("card.success.month")
    private lateinit var cardSuccessMonth: String

    @Property("card.success.year")
    private lateinit var cardSuccessYear: String

    @Property("card.success.cvv")
    private lateinit var cardSuccessCvv: String

    val cardSuccessPay = PaymentCard(
        number = cardSuccessNumber,
        month = cardSuccessMonth,
        year = cardSuccessYear,
        cvv = cardSuccessCvv
    )
}
