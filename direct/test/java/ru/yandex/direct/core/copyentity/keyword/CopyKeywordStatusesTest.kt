package ru.yandex.direct.core.copyentity.keyword

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.assumeCopyResultIsSuccessful
import ru.yandex.direct.core.copyentity.CopyResult
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.service.CopyCampaignService
import ru.yandex.direct.core.entity.keyword.model.Keyword
import ru.yandex.direct.core.entity.keyword.service.KeywordService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyKeywordStatusesTest : AbstractSpringTest() {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var copyCampaignService: CopyCampaignService

    @Autowired
    private lateinit var keywordService: KeywordService

    private lateinit var client: ClientInfo
    private lateinit var adGroup: AdGroupInfo

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        adGroup = steps.adGroupSteps().createDefaultAdGroup(client)
    }

    fun isSuspendedParams() = arrayOf(
        arrayOf(false),
        arrayOf(true),
    )

    @Test
    @Parameters(method = "isSuspendedParams")
    fun `keyword isSuspended is not copied without copyKeywordStatuses flag`(
        isSuspended: Boolean,
    ) {
        steps.keywordSteps().createKeyword(adGroup, defaultKeyword()
            .withIsSuspended(isSuspended))

        val result = copyCampaignWithStatuses(isCopyKeywordStatuses = false)
        val copiedKeyword = getCopiedKeyword(result)

        assertThat(copiedKeyword.isSuspended).isFalse
    }

    @Test
    @Parameters(method = "isSuspendedParams")
    fun `keyword isSuspended is copied with copyKeywordStatuses flag`(
        isSuspended: Boolean,
    ) {
        steps.keywordSteps().createKeyword(adGroup, defaultKeyword()
            .withIsSuspended(isSuspended))

        val result = copyCampaignWithStatuses(isCopyKeywordStatuses = true)
        val copiedKeyword = getCopiedKeyword(result)

        assertThat(copiedKeyword.isSuspended).isEqualTo(isSuspended)
    }

    private fun copyCampaignWithStatuses(
        isCopyKeywordStatuses: Boolean,
    ): CopyResult<Long> = copyCampaignService.copyCampaigns(
        client.clientId!!, client.clientId!!, operatorUid = client.uid,
        campaignIds = listOf(adGroup.campaignId),
        flags = CopyCampaignFlags(
            isCopyKeywordStatuses = isCopyKeywordStatuses,
        ),
    )

    private fun getCopiedKeyword(result: CopyResult<Long>): Keyword {
        assumeCopyResultIsSuccessful(result)
        val copiedKeywordIds: List<Long> = result.getEntityMappings(Keyword::class.java).values.toList()
        return keywordService.getKeywords(client.clientId!!, copiedKeywordIds).first()
    }
}
