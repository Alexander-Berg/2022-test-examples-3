package ru.yandex.direct.core.entity.campaign.service.operation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.vcard.model.Vcard
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestVcards
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.testing.matchers.hasError
import ru.yandex.direct.validation.defect.ids.StringDefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringRunner::class)
class AddCampaignContactInfoNegativeTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignOperationService: CampaignOperationService

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var shard: Int = 0

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT)
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard
    }

    @Test
    fun `contactInfo is validated`() {
        val campaign = TestCampaigns.defaultTextCampaign()
            .withContactInfo(
                TestVcards.fullVcard()
                    .withContactPerson(
                        "Too Long Contact Person Too Long Contact " +
                            "Person Too Long Contact Person Too Long Contact Person Too Long Contact Person Too Long" +
                            " Contact Person Too Long Contact Person Too Long Contact Person Too Long Contact Person " +
                            "Too Long Contact Person Too Long Contact Person Too Long Contact Person Too Long " +
                            "Contact Person Too Long Contact Person"
                    )
            )

        val addOperation = campaignOperationService.createRestrictedCampaignAddOperation(
            listOf(campaign),
            clientInfo.uid,
            UidAndClientId.of(clientInfo.uid, clientInfo.clientId!!),
            CampaignOptions.Builder().build()
        )

        val result = addOperation.prepareAndApply()

        assertThat(result.validationResult).hasError(
            path(index(0), field(TextCampaign.CONTACT_INFO), field(Vcard.CONTACT_PERSON)),
            StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX
        )
    }
}


