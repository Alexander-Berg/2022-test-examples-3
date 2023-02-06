package ru.yandex.direct.core.entity.banner.type.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow

@CoreTest
class InternalBannerModerationStatusesTest : BannerNewBannerInfoUpdateOperationTestBase() {

    companion object {

        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        private var moderatedAdGroupInfo: AdGroupInfo? = null
        private var notModeratedAdGroupInfo: AdGroupInfo? = null

        private val COMPARE_STRATEGY = DefaultCompareStrategies
                .allFields()
                .forFields(BeanFieldPath.newPath(InternalBanner.LAST_CHANGE.name())).useMatcher(approximatelyNow())
        private const val DESCRIPTIONS_UPDATE_TEXT = "update desc for internal banner"
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Before
    fun createBanner() {
        //TODO заменить на BeforeAll
        if (moderatedAdGroupInfo == null) {
            val clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct()
            val campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo)
            moderatedAdGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo)

            val campaignInfo2 = steps.campaignSteps().createActiveInternalAutobudgetCampaign(clientInfo)
            notModeratedAdGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo2)
        }
    }

    fun getDefaultModelChanges(): ModelChanges<InternalBanner> {
        return ModelChanges(bannerInfo.bannerId, InternalBanner::class.java)
                .process(DESCRIPTIONS_UPDATE_TEXT, InternalBanner.DESCRIPTION)
    }

    fun getDefaultExpectedBanner(): InternalBanner {
        return bannerInfo.getBanner<InternalBanner>()
                .withDescription(DESCRIPTIONS_UPDATE_TEXT)
    }


    @Test
    fun checkInternalBannerModerationStatuses_ModeratedBanner() {
        bannerInfo = steps.internalBannerSteps()
                .createModeratedInternalBanner(moderatedAdGroupInfo!!, BannerStatusModerate.YES)

        val expectedBanner = getDefaultExpectedBanner()
                .withStatusModerate(BannerStatusModerate.READY)
                .withStatusPostModerate(BannerStatusPostModerate.NO)

        checkInternalBannerModerationStatuses(getDefaultModelChanges(), expectedBanner)
    }

    @Test
    fun checkInternalBannerModerationStatuses_NotModeratedBanner() {
        bannerInfo = steps.internalBannerSteps().createInternalBanner(notModeratedAdGroupInfo!!)

        val expectedBanner = getDefaultExpectedBanner()
                .withStatusModerate(BannerStatusModerate.YES)
                .withStatusPostModerate(BannerStatusPostModerate.YES)

        checkInternalBannerModerationStatuses(getDefaultModelChanges(), expectedBanner)
    }

    @Test
    fun checkInternalBannerModerationStatuses_DraftModeratedBanner() {
        bannerInfo = steps.internalBannerSteps()
                .createModeratedInternalBanner(moderatedAdGroupInfo!!, BannerStatusModerate.NEW)

        val expectedBanner = getDefaultExpectedBanner()
                .withStatusModerate(BannerStatusModerate.READY)
                .withStatusPostModerate(BannerStatusPostModerate.NO)

        checkInternalBannerModerationStatuses(getDefaultModelChanges(), expectedBanner)
    }

    @Test
    fun checkInternalBannerModerationStatuses_ModeratedBannerToDraft() {
        bannerInfo = steps.internalBannerSteps()
                .createModeratedInternalBanner(moderatedAdGroupInfo!!, BannerStatusModerate.YES)

        val expectedBanner = bannerInfo.getBanner<InternalBanner>()
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NO)
        expectedBanner.moderationInfo.sendToModeration = false

        val modelChanges = ModelChanges(bannerInfo.bannerId, InternalBanner::class.java)
                .process(expectedBanner.moderationInfo, InternalBanner.MODERATION_INFO)

        checkInternalBannerModerationStatuses(modelChanges, expectedBanner)
    }

    @Test
    fun checkInternalBannerModerationStatuses_ModeratedBanner_NotChangedFieldsWhichSendingToModeration() {
        bannerInfo = steps.internalBannerSteps()
                .createModeratedInternalBanner(moderatedAdGroupInfo!!, BannerStatusModerate.YES)

        val expectedBanner = bannerInfo.getBanner<InternalBanner>()
                .withStatusBsSynced(StatusBsSynced.NO)
        expectedBanner.statusShow = !expectedBanner.statusShow
        expectedBanner.moderationInfo.statusShowAfterModeration = !expectedBanner.moderationInfo.statusShowAfterModeration

        // изменяем поля, которые не должны сбросить статус модерации
        val modelChanges = ModelChanges(bannerInfo.bannerId, InternalBanner::class.java)
                .process(expectedBanner.moderationInfo, InternalBanner.MODERATION_INFO)
                .process(expectedBanner.statusShow, InternalBanner.STATUS_SHOW)

        checkInternalBannerModerationStatuses(modelChanges, expectedBanner)
    }

    private fun checkInternalBannerModerationStatuses(modelChanges: ModelChanges<InternalBanner>,
                                                      expectedBanner: InternalBanner) {
        val id = prepareAndApplyValid(modelChanges)
        val actualBanner = getBanner(id, InternalBanner::class.java)

        assertThat(actualBanner)
                .`is`(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(COMPARE_STRATEGY)))
    }

}
