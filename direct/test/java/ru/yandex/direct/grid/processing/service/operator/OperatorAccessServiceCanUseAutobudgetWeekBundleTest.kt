package ru.yandex.direct.grid.processing.service.operator

import com.google.common.collect.ImmutableList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.model.campaign.GdiCampaignStrategyName
import ru.yandex.direct.grid.processing.model.client.GdClientInfo
import ru.yandex.direct.test.utils.RandomNumberUtils
import java.time.Instant
import ru.yandex.direct.grid.model.campaign.GdiBaseCampaign

@RunWith(Parameterized::class)
class OperatorAccessServiceCanUseAutobudgetWeekBundleTest : OperatorAccessServiceBaseTest() {

    companion object {

        data class TestData(val disableAutoBudgetWeekBundle: Boolean,
                            val hideAutobudgetWeekBundle: Boolean,
                            val hasAutoBudgetWeekStrategy: Boolean,
                            val isArchived: Boolean,
                            val result: Boolean)

        @JvmStatic
        @Parameterized.Parameters(name = "DISABLE_AUTOBUDGET_WEEK_BUNDLE = {0}, HIDE_AUTOBUDGET_WEEK_BUNDLE = {1}, HAS_AUTOBUDGET_WEEK_BUNDLE = {2}, IS_ARCHIVED = {3} Result = {4}")
        fun testData() =
            listOf(
                TestData(false, false, false, false, true),
                TestData(false, false, false, true, true),
                TestData(false, false, true, false, true),
                TestData(false, false, true, true, true),
                TestData(false, true, false, false, false),
                TestData(false, true, false, true, false),
                TestData(false, true, true, false, true),
                TestData(false, true, true, true, false),

                TestData(true, false, false, false, false),
                TestData(true, false, false, true, false),
                TestData(true, false, true, false, false),
                TestData(true, false, true, true, false),
                TestData(true, true, false, false, false),
                TestData(true, true, false, true, false),
                TestData(true, true, true, false, false),
                TestData(true, true, true, true, false)
            )
    }

    private val OPERATOR_UID = RandomNumberUtils.nextPositiveLong()

    @Parameterized.Parameter(0)
    lateinit var data: TestData

    private val hideBundleFeature = FeatureName.HIDE_AUTOBUDGET_WEEK_BUNDLE
    private val disableBundleFeature = FeatureName.DISABLE_AUTOBUDGET_WEEK_BUNDLE

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testCanUseAutobudgetBundleStrategy() {
        val operator = TestUsers.defaultUser()
            .withUid(OPERATOR_UID)
            .withClientId(ClientId.fromLong(OPERATOR_UID))
            .withClientId(ClientId.fromLong(RandomNumberUtils.nextPositiveInteger().toLong()))

        val clientInfo = GdClientInfo()
            .withId(1L)
            .withChiefUserId(OPERATOR_UID)
            .withShard(RandomNumberUtils.nextPositiveInteger(22))
            .withCountryRegionId(RandomNumberUtils.nextPositiveLong())
            .withManagersInfo(listOf());

        val clientId = ClientId.fromLong(clientInfo.id)

        `when`(featureService.isEnabledForClientId(eq(clientId), eq(hideBundleFeature)))
            .thenReturn(data.hideAutobudgetWeekBundle)
        `when`(featureService.isEnabledForClientId(eq(clientId), eq(disableBundleFeature)))
            .thenReturn(data.disableAutoBudgetWeekBundle)
        `when`(campaignInfoService.getAllBaseCampaigns(ArgumentMatchers.any(ClientId::class.java)))
            .thenReturn(ImmutableList.of(createCampaign(data.hasAutoBudgetWeekStrategy, data.isArchived)))

        val result = operatorAccessService.getAccess(operator, clientInfo, TestUsers.defaultUser().withUid(123456L), Instant.now())
        assertThat(result.canUseAutobudgetWeekBundle).isEqualTo(data.result)
    }

    private fun createCampaign(hasAutoBudgetWeekStrategy: Boolean, isArchived: Boolean) =
        GdiBaseCampaign()
            .withArchived(isArchived)
            .withStrategyName(
                if (hasAutoBudgetWeekStrategy) GdiCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE else GdiCampaignStrategyName.AUTOBUDGET_AVG_CPA
            )
}
