package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.client.repository.ClientRepository
import ru.yandex.direct.core.entity.uac.grut.GrutTransactionProvider
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.UacBannerService
import ru.yandex.direct.core.entity.uac.service.appinfo.ParseAppStoreUrlService
import ru.yandex.direct.core.entity.uac.service.trackingurl.TrackingUrlParseService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtOperator

abstract class BaseFixBrokenImpressionUrlOneshotTest {
    @Autowired
    protected lateinit var grutSteps: GrutSteps

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var grutUacCampaignService: GrutUacCampaignService

    @Autowired
    protected lateinit var trackingUrlParseService: TrackingUrlParseService

    @Autowired
    protected lateinit var shardHelper: ShardHelper

    @Autowired
    protected lateinit var clientRepository: ClientRepository

    @Autowired
    protected lateinit var parseAppStoreUrlService: ParseAppStoreUrlService

    @Autowired
    protected lateinit var grutApiService: GrutApiService

    @Autowired
    protected lateinit var grutTransactionProvider: GrutTransactionProvider

    protected lateinit var uacBannerService: UacBannerService
    protected lateinit var ydbGrutConverterYtRepository: YdbGrutConverterYtRepository
    protected lateinit var ytProvider: YtProvider
    protected lateinit var operator: YtOperator

    protected lateinit var clientId: ClientId
    protected lateinit var clientInfo: ClientInfo

    @BeforeEach
    fun initBase() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        grutSteps.createClient(clientInfo)

        ytProvider = mock()
        operator = mock()
        ydbGrutConverterYtRepository = mock()
        uacBannerService = mock()

        whenever(ytProvider.getOperator(any())).thenReturn(operator)
        whenever(operator.exists(any())).thenReturn(true)
    }

    protected fun createCampaign(appId: String, impressionUrl: String?, trackingUrl: String): Long {
        return grutSteps.createMobileAppCampaign(
            clientInfo,
            trackingUrl = trackingUrl,
            impressionUrl = impressionUrl,
            appId = appId
        )
    }
}
