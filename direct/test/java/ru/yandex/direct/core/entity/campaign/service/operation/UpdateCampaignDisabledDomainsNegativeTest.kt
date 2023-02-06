package ru.yandex.direct.core.entity.campaign.service.operation

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.hasError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(JUnitParamsRunner::class)
class UpdateCampaignDisabledDomainsNegativeTest {

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var campaignOperationService: CampaignOperationService

    fun parametrizedTestData(): List<List<Any?>> = listOf(
        listOf(
            listOf("yahoo.com", "yahoo.com"),
            path(index(0)),
            CampaignDefects.duplicatedStrings(listOf("yahoo.com")),
        ),
        listOf(
            listOf("yahoo.com", "abc"),
            path(index(0), field(TextCampaign.DISABLED_DOMAINS), index(1)),
            CampaignDefects.invalidDisabledDomain("abc"),
        ),
    )

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("requestedDisabledDomains = {0}")
    fun `DisabledDomains are validated`(
        requestedDisabledDomains: List<String>,
        expectedPath: Path,
        expectedDefect: Defect<*>
    ) {
        val campaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign(null, null))

        val modelChanges = ModelChanges(campaignInfo.campaign.id, TextCampaign::class.java)
        modelChanges.process(requestedDisabledDomains, TextCampaign.DISABLED_DOMAINS)
        val result = createUpdateOperation(modelChanges, campaignInfo.uid, campaignInfo.clientId!!)
            .apply()

        assertThat(result.validationResult).hasError(expectedPath, expectedDefect)
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
}


