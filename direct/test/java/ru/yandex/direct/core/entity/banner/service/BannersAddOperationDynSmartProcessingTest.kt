package ru.yandex.direct.core.entity.banner.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewBannerInfo
import ru.yandex.direct.core.testing.steps.Steps

class BannersAddOperationDynSmartProcessingTest(
    description: String,
    createAdGroup: (steps: Steps) -> AdGroupInfo,
    createBanner: (steps: Steps, adGroupInfo: AdGroupInfo) -> NewBannerInfo,
    prepareBanner: (steps: Steps, adGroupInfo: AdGroupInfo) -> BannerWithAdGroupId
) : BannerDynSmartProcessingTestBase(description, createAdGroup, createBanner, prepareBanner) {
    @Autowired
    private lateinit var addOperationFactory: BannersAddOperationFactory

    private lateinit var banner: BannerWithAdGroupId

    @Before
    fun prepareBanner() {
        banner = prepareBanner(steps, adGroupInfo)
            .withAdGroupId(adGroupInfo.adGroupId)
    }

    @Test
    fun testStatus_Draft() {
        updateAdGroup(StatusModerate.NEW, StatusBLGenerated.NO)

        addBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isNotEqualTo(StatusModerate.NEW)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_Draft_SaveDraft() {
        updateAdGroup(StatusModerate.NEW, StatusBLGenerated.NO)

        addBanner(true)

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.NEW)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.NO)
        }
    }

    @Test
    fun testStatus_ModerateNo() {
        updateAdGroup(StatusModerate.NO, StatusBLGenerated.NO)

        addBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            if (banner is PerformanceBanner) {
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

        addBanner(true)

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.NO)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.NO)
        }
    }

    @Test
    fun testStatus_ModerateYes_GeneratedNo() {
        updateAdGroup(StatusModerate.YES, StatusBLGenerated.NO)

        addBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.YES)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_ModerateYes_GeneratedYes() {
        updateAdGroup(StatusModerate.YES, StatusBLGenerated.YES)

        addBanner()

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.YES)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.PROCESSING)
        }
    }

    @Test
    fun testStatus_ModerateYes_GeneratedYes_SaveDraft() {
        updateAdGroup(StatusModerate.YES, StatusBLGenerated.YES)

        addBanner(true)

        val adGroup = getAdGroup()
        assertSoftly { softly ->
            softly.assertThat(adGroup.statusModerate).isEqualTo(StatusModerate.YES)
            softly.assertThat(adGroup.statusBLGenerated).isEqualTo(StatusBLGenerated.YES)
        }
    }

    private fun addBanner(saveDraft: Boolean = false) {
        val operation = addOperationFactory
            .createFullAddOperation(listOf(banner), adGroupInfo.clientId, adGroupInfo.uid, saveDraft)

        val result = operation.prepareAndApply()

        assertThat(result.validationResult.flattenErrors()).isEmpty()
    }
}
