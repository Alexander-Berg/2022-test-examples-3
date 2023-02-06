package ru.yandex.direct.core.copyentity.preprocessors.campaign

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.defaultCopyContainer
import ru.yandex.direct.core.entity.client.model.ClientLimits
import ru.yandex.direct.core.entity.client.service.ClientLimitsService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CampaignWithDisabledDomainsAndSspCopyPreprocessorTest {

    private lateinit var preprocessor: CampaignWithDisabledDomainsAndSspCopyPreprocessor

    private lateinit var clientLimitsService: ClientLimitsService

    @Before
    fun before() {
        val hostingsHandler = mock<HostingsHandler>()
        clientLimitsService = mock<ClientLimitsService>()


        whenever(hostingsHandler.stripWww(any())).then {
            it.arguments[0]
        }

        preprocessor = CampaignWithDisabledDomainsAndSspCopyPreprocessor(hostingsHandler, clientLimitsService)
    }

    fun limitParams(): Array<Array<out Any?>> {
        val elements = (1..500).map { x -> x.toString() }
        val elements2 = (501..1000).map { x -> x.toString() }
        return arrayOf(
            arrayOf(
                "successful copy of small amount, only domains",
                listOf("12", "34", "56"),
                null,
                3L,
                listOf("12", "34", "56"),
                null
            ),
            arrayOf("failed copy of small amount, only domains", listOf("12", "34", "56"), null, 1L, null, null),
            arrayOf(
                "successful copy of small amount, only ssp",
                null,
                listOf("12", "34", "56"),
                3L,
                null,
                listOf("12", "34", "56")
            ),
            arrayOf("failed copy of small amount, only ssp", null, listOf("12", "34", "56"), 1L, null, null),
            arrayOf(
                "successful copy of small amount, ssp and domains",
                listOf("12", "34", "56"),
                listOf("21", "43", "65"),
                6L,
                listOf("12", "34", "56"),
                listOf("21", "43", "65")
            ),
            arrayOf(
                "failed copy of small amount, domains and ssp",
                listOf("12", "34", "56"),
                listOf("21", "43", "65"),
                5L,
                null,
                null
            ),
            arrayOf("failed copy of big amount, domains and ssp", elements, elements2, 999L, null, null),
            arrayOf("successful copy of small amount, domains and ssp", elements, elements2, 1000L, elements, elements2),
            arrayOf(
                "successful copy of small amount, domains and ssp, default limit ",
                elements,
                elements,
                0L,
                elements,
                elements
            ),
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
        val campaign = TestTextCampaigns.fullTextCampaign()
            .withDisabledDomains(disabledDomains)
            .withDisabledSsp(disabledSsp)

        val clientLimits = ClientLimits()

        clientLimits.generalBlacklistSizeLimit = generalBlackListSizeLimit

        whenever(clientLimitsService.getClientLimits(any())).then {
            clientLimits
        }
        preprocessor.preprocess(campaign, defaultCopyContainer())
        softly {
            assertThat(campaign.disabledDomains).isEqualTo(expectedDisabledDomains)
            assertThat(campaign.disabledSsp).isEqualTo(expectedDisabledSsp)
        }
    }
}
