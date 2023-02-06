package ru.yandex.direct.core.entity.campaign.service

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
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup
import ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns.fullContentPromotionCampaign
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.testing.matchers.hasError
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CampaignServiceUnarchiveContentPromotionNegativeTest {

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    private lateinit var campaignService: CampaignService

    private lateinit var campaignInfo: ContentPromotionCampaignInfo
    private var campaignId: Long = 0
    private lateinit var lastChange: LocalDateTime

    fun parametrizedTestData(): List<List<Any?>> = listOf(
        listOf(ContentPromotionAdgroupType.VIDEO),
        listOf(ContentPromotionAdgroupType.COLLECTION)
    )

    @Before
    fun setUp() {
    }

    private fun createArchivedContentPromotionCampaign() {
        lastChange = LocalDateTime.now().minus(1, ChronoUnit.MINUTES)
        campaignInfo = steps.contentPromotionCampaignSteps().createCampaign(
            fullContentPromotionCampaign()
                .withStatusShow(false)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusArchived(true)
                .withLastChange(lastChange)
                .withStatusActive(false)
        )
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("contentPromotionType = {0}")
    fun `unarchive content promotion campaign without feature fail`(
        contentPromotionType: ContentPromotionAdgroupType
    ) {
        createArchivedContentPromotionCampaign()
        steps.contentPromotionAdGroupSteps()
            .createAdGroup(
                campaignInfo, fullContentPromotionAdGroup(contentPromotionType)
                    .withCampaignId(campaignId)
            )
        val result: MassResult<Long> = campaignService.unarchiveCampaigns(
            listOf(campaignId), campaignInfo.getUid(),
            UidAndClientId.of(campaignInfo.getUid(), campaignInfo.clientId)
        )
        assertThat(result.validationResult).hasError(
            path(index(0)), CampaignDefects.campaignIsInSpecialArchivedState()
        )
    }

}
