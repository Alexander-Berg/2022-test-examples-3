package ru.yandex.direct.core.entity.banner.type.multicard

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.banner.model.BannerMulticard
import ru.yandex.direct.core.entity.banner.model.BannerMulticardsCurrency
import ru.yandex.direct.core.entity.banner.model.BannerWithMulticardSet
import ru.yandex.direct.core.entity.banner.model.BannerWithMulticardSetAndBannerImage
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewCpmBanners
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CreativeInfo
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.testing.matchers.hasErrorOrWarning
import ru.yandex.direct.validation.defect.CommonDefects.isNull
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringRunner::class)
class BannerWithMulticardSetAddNegativeTest : BannerClientInfoAddOperationTestBase() {

    private lateinit var textAdGroupInfo: AdGroupInfo
    private lateinit var cpmVideoAdGroupInfo: AdGroupInfo

    private lateinit var creativeInfo: CreativeInfo

    private lateinit var imageHash: String

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo)
        cpmVideoAdGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(clientInfo)
        imageHash = steps.bannerSteps().createRegularImageFormat(clientInfo).imageHash

        val creativeId = steps.creativeSteps().nextCreativeId
        creativeInfo = steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, creativeId)
    }

    @Test
    fun testWithoutTgoFeature() {
        val textBanner = TestNewTextBanners.clientTextBanner()
            .withAdGroupId(textAdGroupInfo.adGroupId)
            .withImageHash(imageHash)
            .withMulticards(listOf())

        val validationResult = prepareAndApplyInvalid(textBanner)

        assertThat(validationResult).hasErrorOrWarning(path(field(BannerWithMulticardSet.MULTICARDS)), isNull())
    }

    @Test
    fun testWithoutCpmVideoFeature() {
        val cpmVideoBanner = TestNewCpmBanners.clientCpmBanner(creativeInfo.creativeId)
            .withAdGroupId(cpmVideoAdGroupInfo.adGroupId)
            .withMulticards(listOf())

        val validationResult = prepareAndApplyInvalid(cpmVideoBanner)

        assertThat(validationResult).hasErrorOrWarning(path(field(BannerWithMulticardSet.MULTICARDS)), isNull())
    }

    @Test
    fun testEmpty() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)

        val textBanner = TestNewTextBanners.clientTextBanner()
            .withAdGroupId(textAdGroupInfo.adGroupId)
            .withImageHash(imageHash)
            .withMulticards(listOf())

        val validationResult = prepareAndApplyInvalid(textBanner)

        assertThat(validationResult).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS)),
            CollectionDefectIds.Size.SIZE_MUST_BE_IN_INTERVAL
        )
    }

    @Test
    fun testHasId() {
        steps.featureSteps().addClientFeature(textAdGroupInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)

        val multicards = listOf(
            BannerMulticard()
                .withMulticardId(1)
                .withImageHash(imageHash),
            BannerMulticard()
                .withImageHash(imageHash)
        )
        val textBanner = TestNewTextBanners.clientTextBanner()
            .withAdGroupId(textAdGroupInfo.adGroupId)
            .withImageHash(imageHash)
            .withMulticards(multicards)

        val validationResult = prepareAndApplyInvalid(textBanner)

        assertThat(validationResult).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS), index(0), field(BannerMulticard.MULTICARD_ID)),
            isNull()
        )
    }

    @Test
    fun testWithoutBannerImage() {
        val multicards = listOf(
            BannerMulticard()
                .withImageHash(imageHash),
            BannerMulticard()
                .withImageHash(imageHash)
        )
        val textBanner = TestNewTextBanners.clientTextBanner()
            .withAdGroupId(textAdGroupInfo.adGroupId)
            .withMulticards(multicards)

        val validationResult = prepareAndApplyInvalid(textBanner)

        assertThat(validationResult).hasErrorOrWarning(
            path(field(BannerWithMulticardSetAndBannerImage.IMAGE_HASH)), notNull()
        )
    }

    @Test
    fun testCpmBannerPricesValidation() {
        val multicards = listOf(
            BannerMulticard()
                .withImageHash(imageHash)
                .withPrice(1111),
            BannerMulticard()
                .withImageHash(imageHash)
                .withCurrency(BannerMulticardsCurrency.RUB),
            BannerMulticard()
                .withImageHash(imageHash)
                .withPriceOld(11111)
                .withCurrency(BannerMulticardsCurrency.RUB)
        )
        val cpmVideoBanner = TestNewCpmBanners.clientCpmBanner(creativeInfo.creativeId)
            .withAdGroupId(cpmVideoAdGroupInfo.adGroupId)
            .withMulticards(multicards)

        val validationResult = prepareAndApplyInvalid(cpmVideoBanner)

//        assertThat(validationResult).hasErrorOrWarning()
        assertThat(validationResult) .hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS), index(0), field(BannerMulticard.CURRENCY)),
            notNull()
        )
        assertThat(validationResult).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS), index(1), field(BannerMulticard.PRICE)),
            notNull()
        )
        assertThat(validationResult).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS), index(2), field(BannerMulticard.PRICE_OLD)),
            isNull()
        )
    }

}
