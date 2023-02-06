package ru.yandex.direct.core.entity.banner.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate
import ru.yandex.direct.core.entity.banner.model.Banner
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner
import ru.yandex.direct.core.entity.banner.repository.BannerRepository
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewBannerInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.model.ModelChanges

class BannersUpdateOperationDynSmartProcessingTest(
    description: String,
    createAdGroup: (steps: Steps) -> AdGroupInfo,
    createBanner: (steps: Steps, adGroupInfo: AdGroupInfo) -> NewBannerInfo,
    prepareBanner: (steps: Steps, adGroupInfo: AdGroupInfo) -> BannerWithAdGroupId
) : BannerDynSmartProcessingTestBase(description, createAdGroup, createBanner, prepareBanner) {
    @Autowired
    private lateinit var bannerRepository: BannerRepository

    @Autowired
    private lateinit var updateOperationFactory: BannersUpdateOperationFactory

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

        updateBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isNotEqualTo(StatusModerate.NEW)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_Draft_SaveDraft() {
        updateAdGroup(StatusModerate.NEW, StatusBLGenerated.NO)

        updateBanner(ModerationMode.FORCE_SAVE_DRAFT)

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.NEW)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.NO)
        }
    }

    @Test
    fun testStatus_ModerateNo() {
        updateAdGroup(StatusModerate.NO, StatusBLGenerated.NO)

        updateBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            if (bannerInfo.getBanner<Banner>() is PerformanceBanner) {
                softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.NO)
            } else {
                softly.assertThat(adGroup.statusModerate).isNotEqualTo(StatusModerate.NO)
            }
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_ModerateNo_SaveDraft() {
        updateAdGroup(StatusModerate.NO, StatusBLGenerated.NO)

        updateBanner(ModerationMode.FORCE_SAVE_DRAFT)

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.NO)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.NO)
        }
    }

    @Test
    fun testStatus_ModerateYes_GeneratedNo() {
        updateAdGroup(StatusModerate.YES, StatusBLGenerated.NO)

        updateBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.YES)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_ModerateYes_GeneratedYes() {
        updateAdGroup(StatusModerate.YES, StatusBLGenerated.YES)

        updateBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.YES)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_ModerateYes_GeneratedYes_SaveDraft() {
        updateAdGroup(StatusModerate.YES, StatusBLGenerated.YES)

        updateBanner(ModerationMode.FORCE_SAVE_DRAFT)

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.YES)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.YES)
        }
    }

    private fun updateBanner(moderationMode: ModerationMode = ModerationMode.FORCE_MODERATE) {
        val modelChanges = ModelChanges(bannerInfo.bannerId,
            bannerInfo.getBanner<BannerWithSystemFields>().javaClass)
        val operation = updateOperationFactory.createPartialUpdateOperation(moderationMode,
            listOf(modelChanges), adGroupInfo.uid, adGroupInfo.clientId)

        val result = operation.prepareAndApply()

        assertThat(result.validationResult.flattenErrors()).isEmpty()
    }
}
