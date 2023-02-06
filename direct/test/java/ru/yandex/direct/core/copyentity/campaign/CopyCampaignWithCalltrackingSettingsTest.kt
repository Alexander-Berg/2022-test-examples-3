package ru.yandex.direct.core.copyentity.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.assertThatKt
import ru.yandex.direct.core.entity.domain.model.Domain
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.rbac.RbacRole

@CoreTest
class CopyCampaignWithCalltrackingSettingsTest : BaseCopyCampaignTest() {

    private val PHONE_1 = "+79101112231"
    private val PHONE_2 = "+79101112232"
    private val DOMAIN_POSTFIX = ".com"
    private val PUNYCODE_DOMAIN = "xn--d1aqf.xn--p1ai"
    private val PUNYCODE_DOMAIN2 = "xn--d1a1qf.xn--p1ai"
    private val COUNTER_ID = 123456L

    @Autowired
    protected lateinit var metrikaClient: MetrikaClientStub

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        targetClient = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `copy campaign with calltracking to same client`() {
        val domainInfo = steps.domainSteps().createDomain(
            client.shard,
            Domain().withDomain(PUNYCODE_DOMAIN)
        )
        val calltrackingSettingsId = steps.calltrackingSettingsSteps().add(
            client.clientId,
            domainInfo!!.domainId,
            COUNTER_ID,
            listOf(PHONE_1)
        )
        val campaign = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withCalltrackingSettingsId(calltrackingSettingsId)
        )

        val copiedCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.calltrackingSettingsId).isEqualTo(calltrackingSettingsId)
    }

    @Test
    fun `copy one campaign to other client with existing calltracking with same domain`() {
        val domainInfo = steps.domainSteps().createDomain(
            client.shard,
            DOMAIN_POSTFIX,
            true
        )
        val fromCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
            client.clientId,
            domainInfo!!.domainId,
            COUNTER_ID,
            listOf(PHONE_1)
        )
        val toCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
            targetClient.clientId,
            domainInfo!!.domainId,
            COUNTER_ID,
            listOf(PHONE_2)
        )

        val campaign = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withCalltrackingSettingsId(fromCalltrackingSettingsId)
        )

        val copiedCampaign = copyValidCampaignBetweenClients(campaign)

        assertThat(copiedCampaign.calltrackingSettingsId).isEqualTo(toCalltrackingSettingsId)
    }

    @Test
    fun `copy one campaign to other client without existing calltracking with same domain`() {
        val domainInfo = steps.domainSteps().createDomain(
            client.shard,
            DOMAIN_POSTFIX,
            true
        )

        val domainInfo2 = steps.domainSteps().createDomain(
            client.shard,
            Domain().withDomain(PUNYCODE_DOMAIN2)
        )

        val fromCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
            client.clientId,
            domainInfo!!.domainId,
            COUNTER_ID,
            listOf(PHONE_1)
        )
        val toCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
            targetClient.clientId,
            domainInfo2!!.domainId,
            COUNTER_ID,
            listOf(PHONE_2)
        )

        val campaign = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withCalltrackingSettingsId(fromCalltrackingSettingsId)
        )

        val copiedCampaign = copyValidCampaignBetweenClients(campaign)

        assertThatKt(copiedCampaign.calltrackingSettingsId).isNull()
    }

    @Test
    fun `copy one campaign without calltrackingSettings to other client`() {
        val campaign = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
        )

        val copiedCampaign = copyValidCampaignBetweenClients(campaign)

        assertThatKt(copiedCampaign.calltrackingSettingsId).isNull()
    }
}
