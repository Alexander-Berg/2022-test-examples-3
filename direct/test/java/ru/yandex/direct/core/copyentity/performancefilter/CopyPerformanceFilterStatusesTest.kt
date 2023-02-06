package ru.yandex.direct.core.copyentity.performancefilter

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.assumeCopyResultIsSuccessful
import ru.yandex.direct.core.copyentity.CopyResult
import ru.yandex.direct.core.copyentity.adgroup.BaseCopyAdGroupTest
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.testing.data.campaign.TestSmartCampaigns.fullSmartCampaign
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.test.utils.randomPositiveInt

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyPerformanceFilterStatusesTest : BaseCopyAdGroupTest() {

    @Autowired
    private lateinit var performanceFilterService: PerformanceFilterService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var campaign: CampaignInfo

    private val counterId: Int = randomPositiveInt()

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        campaign = steps.smartCampaignSteps()
            .createCampaign(client, fullSmartCampaign()
                .withStrategy(defaultAutobudgetAvgCpcPerCamp())
                .withMetrikaCounters(listOf(counterId.toLong())))
        metrikaClientStub.addUserCounter(client.uid, counterId)
    }

    data class IsSuspendedParams(
        val copyKeywordStatuses: Boolean,
        val isSuspended: Boolean,
        val expectedIsSuspended: Boolean,
    )

    fun isSuspendedParams() = arrayOf(
        // isSuspended is not copied without copyKeywordStatuses
        IsSuspendedParams(copyKeywordStatuses = false, isSuspended = false, expectedIsSuspended = false),
        IsSuspendedParams(copyKeywordStatuses = false, isSuspended = true, expectedIsSuspended = false),

        // isSuspended is copied with copyKeywordStatuses
        IsSuspendedParams(copyKeywordStatuses = true, isSuspended = false, expectedIsSuspended = false),
        IsSuspendedParams(copyKeywordStatuses = true, isSuspended = true, expectedIsSuspended = true),
    )

    @Test
    @Parameters(method = "isSuspendedParams")
    fun `isSuspended is only copied if copyKeywordStatuses is set`(params: IsSuspendedParams) {
        val performanceFilter = steps.performanceFilterSteps()
            .createPerformanceFilter(campaign, defaultPerformanceFilter()
                .withIsSuspended(params.isSuspended))

        val operation = sameCampaignAdGroupCopyOperation(
            performanceFilter.adGroupInfo,
            flags = CopyCampaignFlags(isCopyKeywordStatuses = params.copyKeywordStatuses),
        )
        val result = operation.copy()
        val copiedPerformanceFilter = getCopiedPerformanceFilter(result)

        assertThat(copiedPerformanceFilter.isSuspended)
            .isEqualTo(params.expectedIsSuspended)
    }

    private fun getCopiedPerformanceFilter(result: CopyResult<*>): PerformanceFilter {
        assumeCopyResultIsSuccessful(result)
        val copiedPerformanceFilterIds: List<Long> =
            result.getEntityMappings(PerformanceFilter::class.java).values.toList()
        return performanceFilterService.get(client.clientId!!, client.uid, copiedPerformanceFilterIds).first()
    }
}
