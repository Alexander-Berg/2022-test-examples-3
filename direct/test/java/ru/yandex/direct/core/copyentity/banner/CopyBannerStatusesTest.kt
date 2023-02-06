package ru.yandex.direct.core.copyentity.banner

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.assumeCopyResultIsSuccessful
import ru.yandex.direct.core.copyentity.CopyResult
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.NEW
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.NO
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.READY
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.SENT
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.YES
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.service.BannerService
import ru.yandex.direct.core.entity.campaign.service.CopyCampaignService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.steps.Steps

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyBannerStatusesTest : AbstractSpringTest() {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var bannerService: BannerService

    @Autowired
    private lateinit var copyCampaignService: CopyCampaignService

    private lateinit var adGroup: AdGroupInfo

    @Before
    fun before() {
        val client = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(client.clientId)
        val campaign = steps.campaignSteps().createActiveCampaign(client)
        adGroup = steps.adGroupSteps().createDefaultAdGroup(campaign)

        // баннер не может быть отправлен на модерацию, если у группы нет условия показа
        steps.keywordSteps().createKeyword(adGroup)
    }

    data class WithoutFlagParams(
        val statusShow: Boolean,
        val statusArchived: Boolean,
        val statusModerate: BannerStatusModerate,
    )

    fun withoutFlagParams() = arrayOf(
        WithoutFlagParams(statusShow = true, statusArchived = false, statusModerate = NEW),
        WithoutFlagParams(statusShow = true, statusArchived = false, statusModerate = YES),
        WithoutFlagParams(statusShow = false, statusArchived = true, statusModerate = READY),
    )

    @Test
    @Parameters(method = "withoutFlagParams")
    fun `banner statuses are reset to default without copyBannerStatuses flag`(params: WithoutFlagParams) {
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withAdGroupInfo(adGroup)
            .withBanner(fullTextBanner()
                .withStatusShow(params.statusShow)
                .withStatusArchived(params.statusArchived)
                .withStatusModerate(params.statusModerate)))

        val copyResult = copyCampaignWithStatuses(isCopyBannerStatuses = false)
        val copiedBanner: TextBanner = getCopiedBanner(copyResult)

        softly {
            assertThat(copiedBanner.statusShow).isTrue
            assertThat(copiedBanner.statusArchived).isFalse
            assertThat(copiedBanner.statusModerate).isEqualTo(NEW)
        }
    }

    data class WithFlagParams(
        val statusShow: Boolean,
        val statusArchived: Boolean,
        val statusModerate: BannerStatusModerate,

        val expectedStatusShow: Boolean,
        val expectedStatusArchived: Boolean,
        val expectedStatusModerate: BannerStatusModerate,
    )

    fun withFlagParams() = arrayOf(
        // черновик остается черновиком
        WithFlagParams(
            statusShow = true, statusArchived = false, statusModerate = NEW,
            expectedStatusShow = true, expectedStatusArchived = false, expectedStatusModerate = NEW,
        ),

        // принятый баннер отправляется на модерацию
        WithFlagParams(
            statusShow = true, statusArchived = false, statusModerate = YES,
            expectedStatusShow = true, expectedStatusArchived = false, expectedStatusModerate = READY,
        ),

        // отправленный на модерацию баннер отправляется на модерацию
        WithFlagParams(
            statusShow = true, statusArchived = false, statusModerate = SENT,
            expectedStatusShow = true, expectedStatusArchived = false, expectedStatusModerate = READY,
        ),

        // отклоненный баннер отправляется на модерацию, но останавливается
        WithFlagParams(
            statusShow = true, statusArchived = false, statusModerate = NO,
            expectedStatusShow = false, expectedStatusArchived = false, expectedStatusModerate = READY,
        ),

        // архивный баннер остается в архиве
        WithFlagParams(
            statusShow = true, statusArchived = true, statusModerate = YES,
            expectedStatusShow = true, expectedStatusArchived = true, expectedStatusModerate = READY,
        ),

        // остановленный баннер остается остановленным
        WithFlagParams(
            statusShow = false, statusArchived = true, statusModerate = YES,
            expectedStatusShow = false, expectedStatusArchived = true, expectedStatusModerate = READY,
        ),
    )

    @Test
    @Parameters(method = "withFlagParams")
    fun `banner statuses are copied with copyBannerStatuses flag`(params: WithFlagParams) {
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withAdGroupInfo(adGroup)
            .withBanner(fullTextBanner()
                .withStatusShow(params.statusShow)
                .withStatusArchived(params.statusArchived)
                .withStatusModerate(params.statusModerate)))

        val copyResult = copyCampaignWithStatuses(isCopyBannerStatuses = true)
        val copiedBanner: TextBanner = getCopiedBanner(copyResult)

        softly {
            assertThat(copiedBanner.statusShow).`as`("statusShow")
                .isEqualTo(params.expectedStatusShow)
            assertThat(copiedBanner.statusArchived).`as`("statusArchived")
                .isEqualTo(params.expectedStatusArchived)
            assertThat(copiedBanner.statusModerate).`as`("statusModerate")
                .isEqualTo(params.expectedStatusModerate)
        }
    }

    private fun copyCampaignWithStatuses(
        isCopyBannerStatuses: Boolean,
    ): CopyResult<Long> = copyCampaignService.copyCampaigns(
        adGroup.clientId, adGroup.clientId, operatorUid = adGroup.uid,
        campaignIds = listOf(adGroup.campaignId),
        flags = CopyCampaignFlags(
            isCopyStopped = true,
            isCopyArchived = true,
            isCopyBannerStatuses = isCopyBannerStatuses,
        ),
    )

    private fun <T : BannerWithAdGroupId> getCopiedBanner(result: CopyResult<Long>): T {
        assumeCopyResultIsSuccessful(result)
        val copiedBannerIds: List<Long> = result.getEntityMappings(BannerWithAdGroupId::class.java).values.toList()
        @Suppress("UNCHECKED_CAST")
        return bannerService.getBannersByIds(copiedBannerIds).first() as T
    }
}
