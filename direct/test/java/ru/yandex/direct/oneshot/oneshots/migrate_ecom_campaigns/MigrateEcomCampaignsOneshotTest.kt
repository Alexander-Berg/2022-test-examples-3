package ru.yandex.direct.oneshot.oneshots.migrate_ecom_campaigns

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.service.BaseUacBannerService
import ru.yandex.direct.core.entity.uac.service.UacBannerService
import ru.yandex.direct.core.entity.uac.service.UacCampaignServiceHolder
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.ClientsRole.client
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.ytwrapper.model.YtCluster.YT_LOCAL

@OneshotTest
@RunWith(JUnitParamsRunner::class)
class MigrateUacCampaignsOneshotTest {
    @Rule
    @JvmField
    val springMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var featureManagingService: FeatureManagingService

    @Autowired
    private lateinit var featureService: FeatureService

    private val uacBannerService: UacBannerService = mock()

    private val baseUacBannerService: BaseUacBannerService = mock()

    private val uacCampaignServiceHolder: UacCampaignServiceHolder = mock(
        defaultAnswer = { answer ->
            when (answer.method.name) {
                "getUacBannerService" -> baseUacBannerService
                else -> Mockito.RETURNS_DEFAULTS.answer(answer)
            }
        }
    )

    private val uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository = mock()

    private lateinit var oneshot: MigrateEcomCampaignsOneshot

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var operatorUid: Long = 0L

    @Autowired
    private lateinit var steps: Steps

    private val migratingTextId = FeatureName.MIGRATING_ECOM_CAMPAIGNS_TO_NEW_BACKEND.getName()
    private val newBackendTextId = FeatureName.ECOM_UC_NEW_BACKEND_ENABLED.getName()

    @Before
    fun before() {
        oneshot = spy(
            MigrateEcomCampaignsOneshot(
                uacBannerService = uacBannerService,
                featureManagingService = featureManagingService,
                uacDbDefineService = mock(),
                uacCampaignServiceHolder = uacCampaignServiceHolder,
                uacYdbDirectCampaignRepository = uacYdbDirectCampaignRepository,
                ytProvider = mock()
            )
        )
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        operatorUid = clientInfo.chiefUserInfo!!.uid

        val featureSteps = steps.featureSteps()
        val featureSettings = featureSteps.defaultSettings
            .withCanEnable(setOf(client.literal))
            .withCanDisable(setOf(client.literal))
        featureSteps.addFeature(migratingTextId, featureSettings)
        featureSteps.addFeature(newBackendTextId, featureSettings)
    }

    @Test
    @Parameters(method = "data")
    @TestCaseName("testExecute: revert = {0}, dryRun = {1}, campaignIds = {2}")
    fun testExecute(revert: Boolean, dryRun: Boolean, campaignIds: List<Long>) {
        doReturn(listOf(DataRow(clientId, campaignIds))).whenever(oneshot).readChunk(any(), any(), any(), any())
        whenever(uacYdbDirectCampaignRepository.getCampaignIdsByDirectCampaignIds(any())).doReturn(mapOf())
        val inputData = InputData(YT_LOCAL, "", operatorUid, dryRun, revert, 1)
        val state = oneshot.execute(inputData, null)
        assertThat(state?.lastRow).isEqualTo(1)
        val enabledFeatures = featureService.getEnabledForClientId(clientId)

        if (dryRun) {
            assertThat(enabledFeatures).doesNotContainSequence(migratingTextId, newBackendTextId)
        } else {
            assertThat(enabledFeatures).contains(migratingTextId)
            assertThat(enabledFeatures).apply {
                if (revert) doesNotContain(newBackendTextId) else contains(newBackendTextId)
            }
        }
        val times = times(if (dryRun) 0 else campaignIds.size)
        verify(baseUacBannerService, times)
            .updateBriefSynced(argThat { id -> id.toLong() in campaignIds }, eq(false))
        verify(uacBannerService, times)
            .updateAdsDeferred(eq(clientId), eq(operatorUid), argThat { id -> id.toLong() in campaignIds })
    }

    fun data() = listOf(
        arrayOf(false, false, listOf(1L, 2L, 3L)),
        arrayOf(false, false, listOf<Long>()),
        arrayOf(false, false, listOf(1L)),
        arrayOf(false, true, listOf(1L, 2L, 3L)),
        arrayOf(true, false, listOf(1L, 2L, 3L)),
        arrayOf(true, true, listOf(1L, 2L, 3L)),
    )
}

