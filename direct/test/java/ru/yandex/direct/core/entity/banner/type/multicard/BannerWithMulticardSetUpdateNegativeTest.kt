package ru.yandex.direct.core.entity.banner.type.multicard

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.banner.model.BannerMulticard
import ru.yandex.direct.core.entity.banner.model.BannerMulticardsCurrency
import ru.yandex.direct.core.entity.banner.model.BannerWithMulticardSet
import ru.yandex.direct.core.entity.banner.model.BannerWithMulticardSetAndBannerImage
import ru.yandex.direct.core.entity.banner.model.CpmBanner
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewCpmBanners
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.info.BannerImageFormatInfo
import ru.yandex.direct.core.testing.info.CreativeInfo
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.direct.testing.matchers.hasErrorOrWarning
import ru.yandex.direct.validation.defect.CommonDefects.isNull
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringRunner::class)
class BannerWithMulticardSetUpdateNegativeTest : BannerClientInfoUpdateOperationTestBase() {

    private lateinit var creativeInfo: CreativeInfo

    private lateinit var imageHash1: String
    private lateinit var imageHash2: String

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        imageHash1 = steps.bannerSteps().createRegularImageFormat(clientInfo).imageHash
        imageHash2 = steps.bannerSteps().createRegularImageFormat(clientInfo).imageHash

        val creativeId = steps.creativeSteps().nextCreativeId
        creativeInfo = steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, creativeId)
    }

    @Test
    fun testNoTgoFeature() {
        val bannerInfo = steps.textBannerSteps().createBanner(NewTextBannerInfo().withClientInfo(clientInfo))

        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, listOf<BannerMulticard>())
        val vr = prepareAndApplyInvalid(mc)

        assertThat(vr).hasErrorOrWarning(path(field(BannerWithMulticardSet.MULTICARDS)), isNull())
    }

    @Test
    fun testNoCpmVideoFeature() {
        val bannerInfo = steps.cpmBannerSteps().createCpmBanner(NewCpmBannerInfo().withClientInfo(clientInfo))

        val mc: ModelChanges<CpmBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, listOf<BannerMulticard>())
        val vr = prepareAndApplyInvalid(mc)

        assertThat(vr).hasErrorOrWarning(path(field(BannerWithMulticardSet.MULTICARDS)), isNull())
    }

    @Test
    fun testEmptyMulticards() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())

        val emptyMulticards: List<BannerMulticard> = listOf()
        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, emptyMulticards)
        val vr = prepareAndApplyInvalid(mc)

        assertThat(vr).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS)),
            CollectionDefectIds.Size.SIZE_MUST_BE_IN_INTERVAL
        )
    }

    @Test
    fun testAddWithoutImage() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(multicards = null, imageInfo = null)

        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, defaultMulticards())
        val vr = prepareAndApplyInvalid(mc)

        assertThat(vr).hasErrorOrWarning(
            path(field(BannerWithMulticardSetAndBannerImage.IMAGE_HASH)), notNull()
        )
    }

    @Test
    fun testRemoveImage() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())

        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSetAndBannerImage.IMAGE_HASH, null)
        val vr = prepareAndApplyInvalid(mc)

        assertThat(vr).hasErrorOrWarning(
            path(field(BannerWithMulticardSetAndBannerImage.IMAGE_HASH)), notNull()
        )
    }

    @Test
    fun testMinMulticards() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())
        val multicards = getBanner<TextBanner>(bannerInfo.bannerId).multicards

        val removed = multicards.dropLast(1)
        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, removed)
        val vr = prepareAndApplyInvalid(mc)

        assertThat(vr).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS)),
            CollectionDefectIds.Size.SIZE_MUST_BE_IN_INTERVAL
        )
    }

    @Test
    fun testNonExistentId() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())
        val multicards = getBanner<TextBanner>(bannerInfo.bannerId).multicards

        val changed = listOf(
            multicards[0],
            multicards[1]
                .withMulticardId(randomPositiveInt().toLong())
        )

        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, changed)
        val vr = prepareAndApplyInvalid(mc)

        assertThat(vr).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS), index(1), field(BannerMulticard.MULTICARD_ID)),
            CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION
        )
    }

    @Test
    fun testDuplicateIds() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())
        val multicards = getBanner<TextBanner>(bannerInfo.bannerId).multicards

        val changed = listOf(
            multicards[0],
            multicards[1]
                .withMulticardId(multicards[0].multicardId),
        )

        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, changed)
        val vr = prepareAndApplyInvalid(mc)

        softly {
            assertThat(vr).hasErrorOrWarning(
                path(field(BannerWithMulticardSet.MULTICARDS), index(0)),
                CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS
            )
            assertThat(vr).hasErrorOrWarning(
                path(field(BannerWithMulticardSet.MULTICARDS), index(1)),
                CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS
            )
        }
    }

    @Test
    fun testCpmBannerPricesValidation() {
        val bannerInfo = steps.cpmBannerSteps().createCpmBanner(NewCpmBannerInfo().withClientInfo(clientInfo).withBanner(TestNewCpmBanners
            .fullCpmBanner(clientInfo.clientId!!.asLong()).withMulticards(defaultCpmBannerMulticards())))

        val multicards = getBanner<CpmBanner>(bannerInfo.bannerId).multicards
        val changed = listOf(
            multicards[0].withCurrency(null),
            multicards[1].withPrice(null).withPriceOld(null),
            multicards[2].withPrice(null)
        )

        val mc: ModelChanges<CpmBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, changed)
        val vr = prepareAndApplyInvalid(mc)

        assertThat(vr).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS), index(0), field(BannerMulticard.CURRENCY)),
            notNull()
        )
        assertThat(vr).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS), index(1), field(BannerMulticard.PRICE)),
            notNull()
        )
        assertThat(vr).hasErrorOrWarning(
            path(field(BannerWithMulticardSet.MULTICARDS), index(2), field(BannerMulticard.PRICE_OLD)),
            isNull()
        )
    }

    private fun defaultMulticards() = listOf(
        BannerMulticard()
            .withImageHash(imageHash1),
        BannerMulticard()
            .withImageHash(imageHash2),
    )

    private fun defaultCpmBannerMulticards() = listOf(
        BannerMulticard()
            .withImageHash(imageHash1)
            .withPrice(111)
            .withPriceOld(222)
            .withCurrency(BannerMulticardsCurrency.RUB),
        BannerMulticard()
            .withImageHash(imageHash2)
            .withPrice(111)
            .withPriceOld(222)
            .withCurrency(BannerMulticardsCurrency.USD),
        BannerMulticard()
            .withImageHash(imageHash2)
            .withPrice(111)
            .withPriceOld(222)
            .withCurrency(BannerMulticardsCurrency.USD),
    )

    private fun createBanner(
        multicards: List<BannerMulticard>?,
        imageInfo: BannerImageFormatInfo? = BannerImageFormatInfo()
    ): NewTextBannerInfo {
        return steps.textBannerSteps().createBanner(
            NewTextBannerInfo()
                .withClientInfo(clientInfo)
                .withBanner(
                    TestNewTextBanners.fullTextBanner()
                        .withMulticards(multicards)
                )
                .withBannerImageFormatInfo(imageInfo)
        )
    }
}
