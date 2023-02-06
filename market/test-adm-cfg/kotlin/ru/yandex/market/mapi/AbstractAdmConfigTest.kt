package ru.yandex.market.mapi

import org.junit.jupiter.api.BeforeEach
import ru.yandex.market.experiments3.client.Experiments3Client
import ru.yandex.market.experiments3.client.Experiments3ClientConfig

abstract class AbstractAdmConfigTest {

    protected lateinit var client: Experiments3Client

    @BeforeEach
    fun init() {
        client = buildExp3Client()
    }

    private fun buildExp3Client(): Experiments3Client {
        val port = if (isRecipeUsed()) {
            System.getenv("RECIPE_EXP3_MATCHER_PORT")
        } else {
            // for local idea run using with docker
            "11920"
        }
        val config = Experiments3ClientConfig.Builder()
            .setConsumer("mapi")
            .setPort(port.toInt())
            .build()
        return Experiments3Client(config)
    }

    private fun isRecipeUsed(): Boolean {
        return System.getenv("market.mapi.use.exp3.recipe")
            ?.equals("true", ignoreCase = true) ?: false
    }
}
