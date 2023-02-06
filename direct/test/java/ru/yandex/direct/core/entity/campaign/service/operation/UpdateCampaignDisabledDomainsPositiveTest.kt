package ru.yandex.direct.core.entity.campaign.service.operation

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.testing.matchers.hasNoErrorsOrWarnings

@CoreTest
@RunWith(JUnitParamsRunner::class)
class UpdateCampaignDisabledDomainsPositiveTest {

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var campaignOperationService: CampaignOperationService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    private val SSP_PLATFORM_1 = "Rubicon"
    private val SSP_PLATFORM_2 = "sspplatform.ru"

    fun parametrizedTestData(): List<List<Any?>> = listOf(
        listOf(
            listOf("yahoo.com", "google.com", "www.ixbt.ru"),
            listOf(SSP_PLATFORM_1, SSP_PLATFORM_2),

            listOf("google.com", "ixbt.ru", SSP_PLATFORM_2, "yahoo.com"), // сортируются по алфавиту
            listOf(SSP_PLATFORM_1, SSP_PLATFORM_2),
        ),
    )

    @Before
    fun before() {
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2));
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("requestedDisabledDomains = {0}, requestedSsps = {1}")
    fun `DisabledDomains are updated`(
        requestedDisabledDomains: List<String>,
        requestedSsps: List<String>,
        expectedDisabledDomains: List<String>,
        expectedSsps: List<String>,
    ) {
        val campaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign(null, null))

        val modelChanges = ModelChanges(campaignInfo.campaign.id, TextCampaign::class.java)
        modelChanges.process(requestedDisabledDomains, TextCampaign.DISABLED_DOMAINS)
        modelChanges.process(requestedSsps, TextCampaign.DISABLED_SSP)
        val result = createUpdateOperation(modelChanges, campaignInfo.uid, campaignInfo.clientId!!)
            .apply()

        assertThat(result.validationResult).hasNoErrorsOrWarnings()
        val actualCampaign = getCampaignFromResult(result, campaignInfo.shard)
        assertThat(actualCampaign.disabledDomains).isEqualTo(expectedDisabledDomains)
        assertThat(actualCampaign.disabledSsp).isEqualTo(expectedSsps)
    }

    /**
     * DIRECT-173297
     */
    @Test
    fun `existent duplicates in DisabledDomains are removed on any campaign update`() {
        // create campaign with duplicates in disabledDomain (this could happen in perl)
        val campaignInfo = steps.campaignSteps().createCampaign(
            activeTextCampaign(null, null)
        )
        dslContextProvider.ppc(campaignInfo.shard)
            .update(Tables.CAMPAIGNS)
            .set(Tables.CAMPAIGNS.DONT_SHOW, "google.com,google.com")
            .where(Tables.CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        // update something, for example 'name'
        val modelChanges = ModelChanges(campaignInfo.campaign.id, TextCampaign::class.java)
        modelChanges.process(campaignInfo.campaign.name + " updated", TextCampaign.NAME)
        val result = createUpdateOperation(modelChanges, campaignInfo.uid, campaignInfo.clientId!!)
            .apply()

        // duplicates in DisabledDomains are removed
        assertThat(result.validationResult).hasNoErrorsOrWarnings()
        val actualCampaign = getCampaignFromResult(result, campaignInfo.shard)
        assertThat(actualCampaign.disabledDomains).isEqualTo(listOf("google.com"))
    }

    private fun createUpdateOperation(
        modelChanges: ModelChanges<out BaseCampaign>,
        uid: Long,
        clientId: ClientId
    ) =
        campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(modelChanges),
            uid,
            UidAndClientId.of(uid, clientId),
            CampaignOptions(),
        )

    private fun getCampaignFromResult(
        result: MassResult<Long>,
        shard: Int
    ): TextCampaign =
        campaignTypedRepository.getTypedCampaigns(shard, listOf(result[0].result)).get(0) as TextCampaign
}


