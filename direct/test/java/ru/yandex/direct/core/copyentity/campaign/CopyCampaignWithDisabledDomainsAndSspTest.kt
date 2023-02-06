package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.client.model.ClientLimitsBase.GENERAL_BLACKLIST_SIZE_LIMIT
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.rbac.RbacRole

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithDisabledDomainsAndSspTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)

        targetClient = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `duplicate domains are removed after copying`() {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withDisabledDomains(listOf("google.com", "google.com")))

        val copiedCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.disabledDomains).containsExactly("google.com")
    }

    @Test
    fun `duplicate domains with www are removed after copying`() {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withDisabledDomains(listOf("google.com", "www.google.com")))

        val copiedCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.disabledDomains).containsExactly("google.com")
    }

    fun limitParams(): Array<Array<out Any?>> {
        val limit = 100L
        val ssp = listOf(
            "AdsNative",
            "BidSwitch",
            "ByteDance",
            "Google",
            "Inner-active",
            "Madgic",
            "MobFox",
            "MoPub",
            "Smaato" ,
        )

        val domains = (1..limit / 5 - ssp.size).map { x -> "google$x.com" }

        val domains2 = (1..limit - ssp.size).map { x -> "google$x.com" }

        val domains3 = (1..limit).map { x -> "google$x.com" }

        return arrayOf(
            arrayOf("domains ans ssp fields are copying successful if less than limit", domains, ssp, limit, domains, ssp),
            arrayOf("domains ans ssp fields are copying successful if equals to limit", domains2, ssp, limit, domains2, ssp),
            arrayOf("domains and ssp fields removed after copying if greater than limit", domains3, ssp, limit, null, null),
        )
    }

    @Test
    @TestCaseName("{method}, {0}")
    @Parameters(method = "limitParams")
    fun testGeneralBlackListSizeLimit(
        description: String,
        disabledDomains: List<String>?,
        disabledSsp: List<String>?,
        generalBlackListSizeLimit: Long?,
        expectedDisabledDomains: List<String>?,
        expectedDisabledSsp: List<String>?,
    ) {
        steps.sspPlatformsSteps().addSspPlatforms(disabledSsp)

        val campaign = steps.textCampaignSteps().createCampaign(
            client,
            fullTextCampaign()
                .withDisabledDomains(disabledDomains)
                .withDisabledSsp(disabledSsp)
        )

        steps.clientSteps().setClientLimit(targetClient, GENERAL_BLACKLIST_SIZE_LIMIT, generalBlackListSizeLimit)
        val copiedCampaign = copyValidCampaignBetweenClients(campaign)

        softly {
                assertThat(copiedCampaign.disabledDomains?.sorted()).isEqualTo(expectedDisabledDomains?.sorted())
                assertThat(copiedCampaign.disabledSsp?.sorted()).isEqualTo(expectedDisabledSsp?.sorted())
        }
    }
}
