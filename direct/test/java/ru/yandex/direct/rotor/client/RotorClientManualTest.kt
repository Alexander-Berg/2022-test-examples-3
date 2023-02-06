package ru.yandex.direct.rotor.client

import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.Ignore
import org.junit.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import ru.yandex.direct.config.DirectConfig
import ru.yandex.direct.config.DirectConfigFactory
import ru.yandex.direct.env.EnvironmentType
import ru.yandex.direct.tvm.TvmIntegration
import ru.yandex.direct.tvm.TvmIntegrationImpl
import ru.yandex.direct.tvm.TvmService

@Ignore("Uses external service and local Direct TVM secret")
class RotorClientManualTest {
    private val fetcherFactory = ParallelFetcherFactory(DefaultAsyncHttpClient(), FetcherSettings())

    val scheduler = ThreadPoolTaskScheduler()
    init {
        scheduler.initialize()
    }

    val tvmIntegration: TvmIntegration = TvmIntegrationImpl.create(getDirectConfig(), scheduler)

    val rotorClient = RotorClient(
        "rotor_tracking_url_android",
        "http://gorotor.zora.yandex.net:23555",
        fetcherFactory,
        tvmIntegration,
        TvmService.ZORA_GO,
        "rmp"
    )

    private fun getDirectConfig(): DirectConfig {
        val conf: MutableMap<String, Any> = HashMap()
        conf["tvm.enabled"] = true
        conf["tvm.app_id"] = TvmService.DIRECT_WEB_TEST.id
        conf["tvm.api.url"] = "https://tvm-api.yandex.net"
        conf["tvm.api.error_delay"] = "5s"
        conf["tvm.secret"] = "file://~/.direct-tokens/tvm2_direct-web-test"
        return DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING, conf)
    }

    @Test
    fun get() {
        val url = "https://lr.app.link/JPOGAu4zWeb?%243p=a_yandex_direct&~click_id=test&~agency=GoMobile&~campaign=test&~ad_set_id=test&~ad_id=test&~keyword=tet&%24aaid=&%24idfa=&~campaign_id=test"
        val response = rotorClient.get(url)
    }
}
