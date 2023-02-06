package ru.yandex.direct.antifraud.client

import org.assertj.core.api.Assertions
import org.asynchttpclient.DefaultAsyncHttpClient
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import ru.yandex.direct.antifraud.client.model.Action
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import ru.yandex.direct.config.DirectConfigFactory
import ru.yandex.direct.env.EnvironmentType
import ru.yandex.direct.test.utils.TestUtils
import ru.yandex.direct.tvm.TvmIntegration
import ru.yandex.direct.tvm.TvmIntegrationImpl
import ru.yandex.direct.tvm.TvmService
import java.io.IOException

class AntifraudClientManualTest {
    private lateinit var antifraudClient: AntifraudClient

    companion object {
        private const val ANTIFRAUD_TEST_URL = "https://fraud-test.so.yandex-team.ru"
    }

    @Before
    @Throws(IOException::class)
    fun before() {
        //TVM
        val conf: MutableMap<String, Any> = HashMap()
        conf["tvm.enabled"] = true
        conf["tvm.app_id"] = TvmService.DIRECT_WEB_TEST.id
        conf["tvm.api.url"] = "https://tvm-api.yandex.net"
        conf["tvm.api.error_delay"] = "5s"
        conf["tvm.secret"] = "file://~/.direct-tokens/tvm2_direct-web-test"
        val directConfig = DirectConfigFactory.getConfig(EnvironmentType.TESTING, conf)
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.initialize()
        val tvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler)
        TestUtils.assumeThat {
            Assertions.assertThat(
                tvmIntegration.getTicket(TvmService.PASSPORT_ANTIFRAUD_API_TEST)
            ).isNotNull()
        }

        antifraudClient = createClient(tvmIntegration)
    }

    @Ignore
    @Test
    fun challengeReturned() {
        val verdict = antifraudClient.getVerdict(1L, "loginId", "https://direct.yandex.ru/", true)
        assertThat(verdict.status, `is`(Action.ALLOW))
        assertThat(verdict.challenge, notNullValue())
    }

    private fun createClient(tvmIntegration: TvmIntegration): AntifraudClient {
        val fetcherFactory = ParallelFetcherFactory(DefaultAsyncHttpClient(), FetcherSettings())
        return AntifraudClient(fetcherFactory, tvmIntegration, ANTIFRAUD_TEST_URL, false)
    }
}
