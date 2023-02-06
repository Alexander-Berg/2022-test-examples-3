package ru.yandex.direct.jobs.marketfeeds

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.rbac.RbacRole

@GrutJobsTest
@ExtendWith(SpringExtension::class)
class SyncMarketFeedsJobClientUnderAgencyTest : SyncMarketFeedsJobTestBase() {
    override fun getClientInfo(): ClientInfo {
        val agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY)
        return steps.clientSteps().createClientUnderAgency(agencyClientInfo)
    }
}
