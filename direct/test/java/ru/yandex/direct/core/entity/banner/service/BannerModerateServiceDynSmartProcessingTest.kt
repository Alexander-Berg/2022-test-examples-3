package ru.yandex.direct.core.entity.banner.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.repository.BannerRepository
import ru.yandex.direct.core.entity.banner.service.moderation.BannerModerateService
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewBannerInfo
import ru.yandex.direct.core.testing.steps.Steps

class BannerModerateServiceDynSmartProcessingTest(
    description: String,
    createAdGroup: (steps: Steps) -> AdGroupInfo,
    createBanner: (steps: Steps, adGroupInfo: AdGroupInfo) -> NewBannerInfo,
    prepareBanner: (steps: Steps, adGroupInfo: AdGroupInfo) -> BannerWithAdGroupId
) : BannerDynSmartProcessingTestBase(description, createAdGroup, createBanner, prepareBanner) {
    @Autowired
    private lateinit var bannerRepository: BannerRepository

    @Autowired
    private lateinit var service: BannerModerateService

    private lateinit var bannerInfo: NewBannerInfo

    @Before
    fun createBanner() {
        bannerInfo = createBanner(steps, adGroupInfo)
        bannerRepository.moderation
            .updateStatusModerate(bannerInfo.shard, listOf(bannerInfo.bannerId), BannerStatusModerate.NEW)
    }

    @Test
    fun testStatus_Draft() {
        updateAdGroup(StatusModerate.NEW, StatusBLGenerated.NO)

        moderateBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isNotEqualTo(StatusModerate.NEW)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_ModerateNo() {
        updateAdGroup(StatusModerate.NO, StatusBLGenerated.NO)

        moderateBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isNotEqualTo(StatusModerate.NO)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_ModerateYes_GeneratedNo() {
        updateAdGroup(StatusModerate.YES, StatusBLGenerated.NO)

        moderateBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.YES)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_ModerateYes_GeneratedYes() {
        updateAdGroup(StatusModerate.YES, StatusBLGenerated.YES)

        moderateBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.YES)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    private fun moderateBanner() {
        val result = service.moderateBanners(bannerInfo.clientId, bannerInfo.uid, listOf(bannerInfo.bannerId))

        assertThat(result.validationResult.flattenErrors()).isEmpty()
    }
}
