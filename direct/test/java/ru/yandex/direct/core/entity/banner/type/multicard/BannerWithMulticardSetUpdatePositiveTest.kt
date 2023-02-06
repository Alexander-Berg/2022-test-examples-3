package ru.yandex.direct.core.entity.banner.type.multicard

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.banner.model.BannerMulticard
import ru.yandex.direct.core.entity.banner.model.BannerMulticardSetStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerMulticardsCurrency
import ru.yandex.direct.core.entity.banner.model.BannerWithMulticardSet
import ru.yandex.direct.core.entity.banner.model.BannerWithMulticardSetAndBannerImage
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner
import ru.yandex.direct.core.testing.info.BannerImageFormatInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges

@CoreTest
@RunWith(SpringRunner::class)
class BannerWithMulticardSetUpdatePositiveTest : BannerClientInfoUpdateOperationTestBase() {

    private val text1 = "Текст первой карточки"
    private val text2 = "Текст второй карточки"
    private val text3 = "Текст третьей карточки"

    private lateinit var imageHash1: String
    private lateinit var imageHash2: String
    private lateinit var imageHash3: String

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        imageHash1 = steps.bannerSteps().createRegularImageFormat(clientInfo).imageHash
        imageHash2 = steps.bannerSteps().createRegularImageFormat(clientInfo).imageHash
        imageHash3 = steps.bannerSteps().createRegularImageFormat(clientInfo).imageHash
    }

    @Test
    fun testNoFeature() {
        val bannerInfo = createBanner(defaultMulticards())

        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, null)
        val id = prepareAndApplyValid(mc)

        val actualBanner: TextBanner = getBanner(id)
        assertThat(actualBanner.multicards).isNull()
        assertThat(actualBanner.multicardSetStatusModerate).isNull()
    }

    @Test
    fun testAddMulticards() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(null)

        val newMulticards = defaultMulticards()
        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, newMulticards)
        val id = prepareAndApplyValid(mc)

        val actualBanner: TextBanner = getBanner(id)
        assertThat(actualBanner.multicards).hasSize(2)
        assertThat(actualBanner.multicardSetStatusModerate).isEqualTo(BannerMulticardSetStatusModerate.READY)
    }

    @Test
    fun testAddMulticardsWithImage() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(multicards = null, imageInfo = null)

        val newMulticards = defaultMulticards()
        val mc: ModelChanges<TextBanner> = ModelChanges(bannerInfo.bannerId, TextBanner::class.java)
            .process(newMulticards, BannerWithMulticardSet.MULTICARDS)
            .process(imageHash1, BannerWithMulticardSetAndBannerImage.IMAGE_HASH)
        val id = prepareAndApplyValid(mc)

        val actualBanner: TextBanner = getBanner(id)
        assertThat(actualBanner.imageHash).isEqualTo(imageHash1)
        assertThat(actualBanner.multicards).hasSize(2)
        assertThat(actualBanner.multicardSetStatusModerate).isEqualTo(BannerMulticardSetStatusModerate.READY)
    }

    @Test
    fun testRemoveMulticards() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())

        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, null)
        val id = prepareAndApplyValid(mc)

        val actualBanner: TextBanner = getBanner(id)
        assertThat(actualBanner.multicards).isNull()
        assertThat(actualBanner.multicardSetStatusModerate).isNull()
    }

    @Test
    fun testRemoveMulticardsAndImage() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())

        val mc: ModelChanges<TextBanner> = ModelChanges(bannerInfo.bannerId, TextBanner::class.java)
            .process(null, BannerWithMulticardSet.MULTICARDS)
            .process(null, BannerWithMulticardSetAndBannerImage.IMAGE_HASH)
        val id = prepareAndApplyValid(mc)

        val actualBanner: TextBanner = getBanner(id)
        assertThat(actualBanner.imageHash).isNull()
        assertThat(actualBanner.multicards).isNull()
        assertThat(actualBanner.multicardSetStatusModerate).isNull()
    }

    @Test
    fun testReorderMulticards() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())
        val multicards = getBanner<TextBanner>(bannerInfo.bannerId).multicards

        val reversed = multicards.reversed()
        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, reversed)
        val id = prepareAndApplyValid(mc)

        val actualBanner: TextBanner = getBanner(id)
        assertThat(actualBanner.multicards).hasSize(2)
        softly {
            assertThat(actualBanner.multicards[0].multicardId).isEqualTo(reversed[0].multicardId)
            assertThat(actualBanner.multicards[1].multicardId).isEqualTo(reversed[1].multicardId)
            assertThat(actualBanner.multicardSetStatusModerate).isEqualTo(BannerMulticardSetStatusModerate.YES)
        }
    }

    @Test
    fun testAppendMulticard() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val bannerInfo = createBanner(defaultMulticards())
        val multicards = getBanner<TextBanner>(bannerInfo.bannerId).multicards

        val modified = multicards + listOf(createMulticard(imageHash = imageHash3))
        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, modified)
        val id = prepareAndApplyValid(mc)

        val actualBanner: TextBanner = getBanner(id)
        assertThat(actualBanner.multicards).hasSize(3)
        softly {
            assertThat(actualBanner.multicards[0].multicardId).isEqualTo(multicards[0].multicardId)
            assertThat(actualBanner.multicards[1].multicardId).isEqualTo(multicards[1].multicardId)
            assertThat(actualBanner.multicardSetStatusModerate).isEqualTo(BannerMulticardSetStatusModerate.READY)
        }
    }

    @Test
    fun testComplex() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ASSET_TGO_MULTICARD, true)
        val beforeMulticards = listOf(
            createMulticard(imageHash = imageHash1, text = text1),
            createMulticard(imageHash = imageHash1, text = null),
            createMulticard(imageHash = imageHash2, text = text2),
        )
        val bannerInfo = createBanner(beforeMulticards)
        val multicards = getBanner<TextBanner>(bannerInfo.bannerId).multicards

        // Первую карточку удаляем
        // Вторую меняем с третьей местами, изменяем картинку
        // У третьей меняем текст
        // В середину добавляем новую карточку
        val modified = listOf(
            multicards[2]
                .withText(null),
            createMulticard(imageHash = imageHash3, text = text3),
            multicards[1]
                .withImageHash(imageHash3),
        )

        val mc: ModelChanges<TextBanner> =
            ModelChanges.build(bannerInfo.getBanner(), BannerWithMulticardSet.MULTICARDS, modified)
        val id = prepareAndApplyValid(mc)

        val actualBanner: TextBanner = getBanner(id)
        assertThat(actualBanner.multicards).hasSize(3)

        softly {
            assertThat(actualBanner.multicards[0]).isEqualTo(
                createMulticard(id = multicards[2].multicardId, imageHash = imageHash2, text = null)
            )
            val oldIds = multicards.map { it.multicardId }.toSet()
            assertThat(actualBanner.multicards[1].multicardId).isNotIn(oldIds)
            assertThat(actualBanner.multicards[1]).isEqualToIgnoringGivenFields(
                createMulticard(imageHash = imageHash3, text = text3),
                BannerMulticard.MULTICARD_ID.name()
            )
            assertThat(actualBanner.multicards[2]).isEqualTo(
                createMulticard(id = multicards[1].multicardId, imageHash = imageHash3, text = null)
            )
            assertThat(actualBanner.multicardSetStatusModerate).isEqualTo(BannerMulticardSetStatusModerate.READY)
        }
    }

    private fun defaultMulticards() = listOf(
        createMulticard(imageHash = imageHash1, text = text1),
        createMulticard(imageHash = imageHash2, text = text2),
    )

    private fun createMulticard(id: Long? = null, imageHash: String, text: String? = null, price: Long? = null,
                                priceOld: Long? = null, currency: BannerMulticardsCurrency? = null): BannerMulticard {
        return BannerMulticard()
            .withMulticardId(id)
            .withImageHash(imageHash)
            .withText(text)
            .withPrice(price)
            .withPriceOld(priceOld)
            .withCurrency(currency)
    }

    private fun createBanner(
        multicards: List<BannerMulticard>?,
        imageInfo: BannerImageFormatInfo? = BannerImageFormatInfo()
    ): NewTextBannerInfo {
        return steps.textBannerSteps().createBanner(
            NewTextBannerInfo()
                .withClientInfo(clientInfo)
                .withBanner(
                    fullTextBanner()
                        .withMulticardSetStatusModerate(BannerMulticardSetStatusModerate.YES)
                        .withMulticards(multicards)
                )
                .withBannerImageFormatInfo(imageInfo)
        )
    }
}
