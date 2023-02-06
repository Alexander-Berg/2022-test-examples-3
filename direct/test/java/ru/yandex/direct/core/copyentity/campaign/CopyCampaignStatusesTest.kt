package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignStatusesTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `statusModerate is reset to draft if copyCampaignStatuses is not specified`() {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withStatusModerate(CampaignStatusModerate.YES))

        val copiedCampaign =
            copyValidCampaign(campaign, flags = CopyCampaignFlags(isCopyCampaignStatuses = false))

        assertThat(copiedCampaign.statusModerate).isEqualTo(CampaignStatusModerate.NEW)
    }

    @Test
    fun `statusShow is reset to yes if copyCampaignStatuses is not specified`() {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withStatusShow(false))

        val copiedCampaign =
            copyValidCampaign(campaign, flags = CopyCampaignFlags(isCopyCampaignStatuses = false))

        assertThat(copiedCampaign.statusShow).isTrue
    }

    fun campaignStatusParameters() = arrayOf(
        arrayOf(
            "draft",
            CampaignStatusModerate.NEW, CampaignStatusPostmoderate.NEW, true,
            CampaignStatusModerate.NEW, CampaignStatusPostmoderate.NEW, true,
        ),

        arrayOf(
            "stopped draft",
            CampaignStatusModerate.NEW, CampaignStatusPostmoderate.NEW, false,
            CampaignStatusModerate.NEW, CampaignStatusPostmoderate.NEW, false,
        ),

        arrayOf(
            "ready",
            CampaignStatusModerate.READY, CampaignStatusPostmoderate.NEW, true,
            CampaignStatusModerate.READY, CampaignStatusPostmoderate.NEW, true,
        ),

        arrayOf(
            "ready after quick-moderate rejection",
            CampaignStatusModerate.READY, CampaignStatusPostmoderate.NO, true,
            CampaignStatusModerate.READY, CampaignStatusPostmoderate.NEW, true,
        ),

        arrayOf(
            "sent",
            CampaignStatusModerate.SENT, CampaignStatusPostmoderate.NEW, true,
            CampaignStatusModerate.READY, CampaignStatusPostmoderate.NEW, true,
        ),

        arrayOf(
            "sent but stopped",
            CampaignStatusModerate.SENT, CampaignStatusPostmoderate.NEW, false,
            CampaignStatusModerate.READY, CampaignStatusPostmoderate.NEW, false,
        ),

        arrayOf(
            "accepted",
            CampaignStatusModerate.YES, CampaignStatusPostmoderate.YES, true,
            CampaignStatusModerate.READY, CampaignStatusPostmoderate.NEW, true,
        ),

        arrayOf(
            "rejected",
            CampaignStatusModerate.NO, CampaignStatusPostmoderate.NO, true,
            CampaignStatusModerate.READY, CampaignStatusPostmoderate.NEW, false,
        ),
    )

    @Test
    @Parameters(method = "campaignStatusParameters")
    @TestCaseName("{method}({0})")
    fun `campaign status is copied if copyCampaignStatuses is specified`(
        caseName: String,

        statusModerate: CampaignStatusModerate,
        statusPostModerate: CampaignStatusPostmoderate,
        statusShow: Boolean,

        expectedStatusModerate: CampaignStatusModerate,
        expectedStatusPostModerate: CampaignStatusPostmoderate,
        expectedStatusShow: Boolean,
    ) {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withStatusModerate(statusModerate)
            .withStatusPostModerate(statusPostModerate)
            .withStatusShow(statusShow))

        val copiedCampaign = copyValidCampaign(campaign, flags = CopyCampaignFlags(isCopyCampaignStatuses = true))

        softly {
            assertThat(copiedCampaign.statusModerate).isEqualTo(expectedStatusModerate)
            assertThat(copiedCampaign.statusPostModerate).isEqualTo(expectedStatusPostModerate)
            assertThat(copiedCampaign.statusShow).isEqualTo(expectedStatusShow)
        }
    }

    @Test
    fun `campaign is stopped if stopCopiedCampaigns flag is specified`() {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign())

        val copiedCampaign = copyValidCampaign(campaign, flags = CopyCampaignFlags(isStopCopiedCampaigns = true))

        assertThat(copiedCampaign.statusShow).isFalse
    }
}
