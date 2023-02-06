package ru.yandex.direct.core.entity.banner.type.multicard

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.TestContextManager
import ru.yandex.direct.core.entity.banner.model.BannerMulticard
import ru.yandex.direct.core.entity.banner.model.BannerMulticardSetStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerMulticardsCurrency
import ru.yandex.direct.core.entity.banner.model.BannerWithMulticardSet
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewCpmBanners
import ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CreativeInfo
import ru.yandex.direct.feature.FeatureName

@CoreTest
@RunWith(JUnitParamsRunner::class)
class BannerWithMulticardSetAddPositiveTest : BannerClientInfoAddOperationTestBase() {

    private val text1 = "Текст первой карточки"
    private val text2 = "Текст второй карточки"

    private val href1 = "https://yandex.ru"
    private val href2 = "http://google.com"

    private lateinit var imageHash1: String
    private lateinit var imageHash2: String

    private lateinit var textAdGroupInfo: AdGroupInfo
    private lateinit var cpmVideoAdGroupInfo: AdGroupInfo

    private lateinit var creativeInfo: CreativeInfo

    @Before
    fun before() {
        TestContextManager(this::class.java).prepareTestInstance(this)
        clientInfo = steps.clientSteps().createDefaultClient()

        textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo)
        cpmVideoAdGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(clientInfo)

        imageHash1 = steps.bannerSteps().createRegularImageFormat(clientInfo).imageHash
        imageHash2 = steps.bannerSteps().createRegularImageFormat(clientInfo).imageHash

        val creativeId = steps.creativeSteps().nextCreativeId
        creativeInfo = steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, creativeId)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_CPM_VIDEO_MULTICARD, true)
    }

    @Test
    fun withoutMulticards() {
        val textBanner = clientTextBanner()
            .withAdGroupId(textAdGroupInfo.adGroupId)
            .withMulticards(null)

        val id = prepareAndApplyValid(textBanner)

        val actualBanner: BannerWithMulticardSet = getBanner(id)
        assertThat(actualBanner.multicards).isNull()
        assertThat(actualBanner.multicardSetStatusModerate).isNull()
    }

    @Test
    fun withMulticards() {
        val multicards = defaultMulticards()
        val textBanner = clientTextBanner()
            .withAdGroupId(textAdGroupInfo.adGroupId)
            .withImageHash(imageHash1)
            .withMulticards(multicards)

        val id = prepareAndApplyValid(textBanner)

        val actualBanner: BannerWithMulticardSet = getBanner(id)

        assertThat(actualBanner.multicards).hasSize(3)
        assertThat(actualBanner.multicards[0])
            .isEqualToIgnoringGivenFields(multicards[0], BannerMulticard.MULTICARD_ID.name())
        assertThat(actualBanner.multicards[1])
            .isEqualToIgnoringGivenFields(multicards[1], BannerMulticard.MULTICARD_ID.name())
        assertThat(actualBanner.multicards[2])
            .isEqualToIgnoringGivenFields(multicards[2], BannerMulticard.MULTICARD_ID.name())
    }

    @Test
    fun withCpmVideoMulticards() {
        val multicards = cpmVideoMulticards()
        val cpmVideoBanner = TestNewCpmBanners.clientCpmBanner(creativeInfo.creativeId)
            .withAdGroupId(cpmVideoAdGroupInfo.adGroupId)
            .withMulticards(multicards)

        val id = prepareAndApplyValid(cpmVideoBanner)
        val actualBanner: BannerWithMulticardSet = getBanner(id)

        assertThat(actualBanner.multicards).hasSize(3)
        assertThat(actualBanner.multicards[0])
            .isEqualToIgnoringGivenFields(multicards[0], BannerMulticard.MULTICARD_ID.name())
        assertThat(actualBanner.multicards[1])
            .isEqualToIgnoringGivenFields(multicards[1], BannerMulticard.MULTICARD_ID.name())
        assertThat(actualBanner.multicards[2])
            .isEqualToIgnoringGivenFields(multicards[2], BannerMulticard.MULTICARD_ID.name())
    }

    fun statusModerateParams() = arrayOf(
        arrayOf(true, BannerMulticardSetStatusModerate.NEW),
        arrayOf(false, BannerMulticardSetStatusModerate.READY),
    )

    @Test
    @Parameters(method = "statusModerateParams")
    fun testStatusModerate(saveDraft: Boolean, expectedStatusModerate: BannerMulticardSetStatusModerate) {
        val multicards = defaultMulticards()
        val textBanner = clientTextBanner()
            .withAdGroupId(textAdGroupInfo.adGroupId)
            .withImageHash(imageHash1)
            .withMulticards(multicards)

        val id = prepareAndApplyValid(textBanner, saveDraft)

        val actualBanner: BannerWithMulticardSet = getBanner(id)

        assertThat(actualBanner.multicardSetStatusModerate).isEqualTo(expectedStatusModerate)
    }

    private fun defaultMulticards() = listOf(
        BannerMulticard()
            .withImageHash(imageHash1)
            .withText(text1),
        BannerMulticard()
            .withImageHash(imageHash1),
        BannerMulticard()
            .withImageHash(imageHash2)
            .withText(text2),
    )

    private fun cpmVideoMulticards() = listOf(
        BannerMulticard()
            .withImageHash(imageHash1)
            .withText(text1)
            .withHref(href1)
            .withPrice(111)
            .withPriceOld(222)
            .withCurrency(BannerMulticardsCurrency.USD),
        BannerMulticard()
            .withImageHash(imageHash2)
            .withText(text2)
            .withHref(href1)
            .withPrice(1)
            .withPriceOld(2)
            .withCurrency(BannerMulticardsCurrency.RUB),
        BannerMulticard()
            .withImageHash(imageHash2)
            .withText(text2)
            .withHref(href2)
            .withPrice(1)
            .withPriceOld(2)
            .withCurrency(BannerMulticardsCurrency.BYN),
    )
}
