package ru.yandex.direct.jobs.marketfeeds

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.jobs.configuration.GrutJobsTest

@GrutJobsTest
@ExtendWith(SpringExtension::class)
class SyncMarketFeedsJobDefaultClientTest : SyncMarketFeedsJobTestBase() {

    override fun getClientInfo(): ClientInfo {
        return steps.clientSteps().createDefaultClient()
    }
}
