package ru.yandex.direct.jobs.uac.converter

import NBannerLand.Banner
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.feed.service.MbiService
import ru.yandex.direct.core.entity.feed.model.FeedOfferExamples
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.grut.experimental.banner.BannerResources

internal class TBannerConverterTest {

    companion object {
        val ALL_FORMATS = listOf("small", "big", "huge", "orig")
    }

    @Test
    fun mergeWithNewOffers_withNullExistingOffers_success() {
        val ids = listOf<Long>(1, 7, 4312)
        val tBanners = createTBanners(ids)
        val expectedResult = createExpectedOffers(ids)

        val convertedOffers = TBannerConverter.mergeWithNewOffers(null, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    @Test
    fun mergeWithNewOffers_withExistingOffers_success() {
        val existingOfferIds = listOf<Long>(55, 5, 717)
        val existingOffers = JsonUtils.toJson(createExpectedOffers(existingOfferIds))

        val ids = listOf<Long>(7, 1, 4312)
        val tBanners = createTBanners(ids)
        val expectedResult = createExpectedOffers(ids + existingOfferIds)

        val convertedOffers = TBannerConverter.mergeWithNewOffers(existingOffers, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    @Test
    fun mergeWithNewOffers_withEmptyExistingOffers_success() {
        val existingOffers = "{\"data_params\":{}}"

        val ids = listOf<Long>(1, 7, 4312)
        val tBanners = createTBanners(ids)
        val expectedResult = createExpectedOffers(ids)

        val convertedOffers = TBannerConverter.mergeWithNewOffers(existingOffers, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    @Test
    fun mergeWithNewOffers_withEmptyNewOffers_success() {
        val existingOfferIds = listOf<Long>(5, 55, 717)
        val existingOffers = JsonUtils.toJson(createExpectedOffers(existingOfferIds))

        val tBanners = createTBanners(listOf())

        val convertedOffers = TBannerConverter.mergeWithNewOffers(existingOffers, tBanners)

        assertThat(convertedOffers).isNull()
    }

    @Test
    fun mergeWithNewOffers_withoutOptionalFields_success() {
        val ids = listOf<Long>(1, 7, 4312)
        val tBanners = createTBanners(ids, false, false)
        val expectedResult = createExpectedOffers(ids, false, false)

        val convertedOffers = TBannerConverter.mergeWithNewOffers(null, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    @Test
    fun mergeWithNewOffers_withInvalidOffers_success() {
        val ids = listOf<Long>(1, 7, 4312)
        val tBanners = createTBanners(ids).toMutableList()
        tBanners.add(createTBanner(25, "title", null,
            createOfferInfo(), createBannerPrice("10", null, "RUB"),
            listOf(createTImageInfo("imageName"))))
        val expectedResult = createExpectedOffers(ids)

        val convertedOffers = TBannerConverter.mergeWithNewOffers(null, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    @Test
    fun mergeWithNewOffers_withEmptyOldPrice_success() {
        val ids = listOf<Long>(1, 7, 4312)
        val tBanners = createTBanners(ids).toMutableList()
        tBanners.add(createTBanner(25, "title", "href",
            createOfferInfo(), createBannerPrice("10", "", "RUB"),
            listOf(createTImageInfo("imageName"))))

        val convertedOffers = TBannerConverter.mergeWithNewOffers(null, tBanners)

        assertThat(convertedOffers?.offers).hasSize(4)
        assertThat(convertedOffers!!.offers!!["25"]!!.price.old).isNull()
    }

    @Test
    fun mergeWithNewOffers_withManyImages_success() {
        val ids = listOf<Long>(1, 7, 4312)
        val tBanners = createTBanners(ids, imageCount = 10)

        val convertedOffers = TBannerConverter.mergeWithNewOffers(null, tBanners)

        assertThat(convertedOffers?.offers?.size).isEqualTo(3)
    }

    @Test
    fun mergeWithNewOffers_withoutImageFormats_success() {
        val ids = listOf<Long>(1, 7, 4312)
        val tBanners = createTBanners(ids).toMutableList()
        tBanners.addAll(createTBanners(listOf(17, 42), formats = listOf("small", "big", "huge", "orig", "hugeX")))
        tBanners.addAll(createTBanners(listOf(18, 43), formats = listOf("small", "big", "huge", "hugeX")))
        tBanners.addAll(createTBanners(listOf(19, 44), formats = listOf()))

        val expectedIds = ids.toMutableList()
        expectedIds.addAll(listOf(17, 42))
        val expectedResult = createExpectedOffers(expectedIds)

        val convertedOffers = TBannerConverter.mergeWithNewOffers(null, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    @Test
    fun mergeWithNewOffers_withoutImages_success() {
        val ids = listOf<Long>(1, 7, 4312)
        val tBanners = createTBanners(ids, imageCount = 0)

        val convertedOffers = TBannerConverter.mergeWithNewOffers(null, tBanners)

        assertThat(convertedOffers).isNull()
    }

    @Test
    fun mergeWithNewOffers_withDuplicatedOffers_success() {
        val existingOfferIds = listOf<Long>(5, 55, 717, 5005)
        val existingOffers = JsonUtils.toJson(createExpectedOffers(existingOfferIds))

        val ids = listOf<Long>(1, 55, 4312, 5005)
        val tBanners = createTBanners(ids)
        val expectedResult = createExpectedOffers(ids + listOf(5, 717))

        val convertedOffers = TBannerConverter.mergeWithNewOffers(existingOffers, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    @Test
    fun mergeWithNewOffers_withNewOffersCountMoreThanMax_success() {
        val existingOfferIds = listOf<Long>(5, 55, 717, 5005)
        val existingOffers = JsonUtils.toJson(createExpectedOffers(existingOfferIds))

        val ids = (1L..MbiService.OFFERS_COUNT_FOR_PREVIEW + 5L).toList()
        val tBanners = createTBanners(ids)
        val expectedResult = createExpectedOffers(ids.take(MbiService.OFFERS_COUNT_FOR_PREVIEW))

        val convertedOffers = TBannerConverter.mergeWithNewOffers(existingOffers, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    @Test
    fun mergeWithNewOffers_withOffersCountMoreThanMax_success() {
        val existingOfferIds = (1L..MbiService.OFFERS_COUNT_FOR_PREVIEW + 5L).toList()
        val existingOffers = JsonUtils.toJson(createExpectedOffers(existingOfferIds))

        val ids = listOf<Long>(1, 55, 4312, 5005)
        val tBanners = createTBanners(ids)
        val expectedResult = createExpectedOffers(ids.plus(existingOfferIds.drop(1)).take(MbiService.OFFERS_COUNT_FOR_PREVIEW))

        val convertedOffers = TBannerConverter.mergeWithNewOffers(existingOffers, tBanners)

        checkOffersEquals(convertedOffers, expectedResult)
    }

    fun createTBanners(ids: List<Long>, withDescription: Boolean = true, withOldPrice: Boolean = true,
                       imageCount: Int = 1, formats: List<String> = ALL_FORMATS): List<Banner.TBanner> {
        return ids.map { id ->
            val description = if (withDescription) "description-$id" else null
            val oldPrice = if (withOldPrice) "${id * 1000}" else null
            val images = IntRange(1, imageCount)
                .map { createTImageInfo("image-$id-$it", formats) }

            createTBanner(
                id, "banner-$id", "https://banner-$id.html",
                createOfferInfo(description),
                createBannerPrice("${id * 123}", oldPrice, "RUR"),
                images
            )
        }
    }

    fun createExpectedOffers(ids: List<Long>, withDescription: Boolean = true, withOldPrice: Boolean = true): FeedOfferExamples {
        var index: Long = 1_000_000_000
        val offers = ids.associateBy({ it.toString() }) { id ->
            val description = if (withDescription) "description-$id" else null
            val oldPrice = if (withOldPrice) "${id * 1000}" else null
            val href = "https://banner-$id.html"

            FeedOfferExamples.Offer(
                href,
                FeedOfferExamples.Price("${id * 123}", oldPrice),
                FeedOfferExamples.Text(description, null, "banner-$id", "RUB"),
                FeedOfferExamples.ClickUrls(href, null, null, null, null, null, null, null),
                FeedOfferExamples.ImageInfo(
                    FeedOfferExamples.Image(200, 100, "//avatars.mds.yandex.net/get-namespace/group/image-$id-1/small", null),
                    FeedOfferExamples.Image(200, 100, "//avatars.mds.yandex.net/get-namespace/group/image-$id-1/orig", null),
                    FeedOfferExamples.Image(200, 100, "//avatars.mds.yandex.net/get-namespace/group/image-$id-1/big", null),
                    FeedOfferExamples.Image(200, 100, "//avatars.mds.yandex.net/get-namespace/group/image-$id-1/huge", null)
                ),
                LocalDateTime.ofEpochSecond(index--, 0, ZoneOffset.UTC)
            )
        }

        return FeedOfferExamples(offers)
    }

    fun createTBanner(id: Long, title: String, href: String?,
                      offerInfo: Banner.TBanner.TOfferInfo?,
                      bannerPrice: BannerResources.TBannerPrice?,
                      imagesInfo: List<BannerResources.TImagesInfo>?): Banner.TBanner {
        val builder = Banner.TBanner.newBuilder()
            .setTitle(title)
            .setOfferYabsId(id)

        if (href != null) {
            builder.setHref(href)
        }

        if (offerInfo != null) {
            builder.setOfferInfo(offerInfo)
        }

        if (bannerPrice != null) {
            builder.setBannerPrice(bannerPrice)
        }

        imagesInfo?.forEach { imageInfo ->
            builder.addImagesInfo(imageInfo)
        }

        return builder.build()
    }

    fun createOfferInfo(description: String? = null): Banner.TBanner.TOfferInfo {
        val builder = Banner.TBanner.TOfferInfo.newBuilder()

        if (description != null) {
            builder.setDescriptionForDirect(description)
        }

        return builder.build()
    }

    fun createBannerPrice(currentPrice: String, oldPrice: String?, currency: String): BannerResources.TBannerPrice {
        val builder = BannerResources.TBannerPrice.newBuilder()

        if (oldPrice != null) {
            builder.setOldPrice(oldPrice)
        }

        return builder
            .setPrice(currentPrice)
            .setCurrency(currency)
            .build()
    }

    fun createTImageInfo(imageName: String, formats: List<String> = ALL_FORMATS): BannerResources.TImagesInfo {
        val builder = BannerResources.TImagesInfo.newBuilder()
            .setAvatarsImage(createAvatarsImage(imageName))

        formats.forEach { format ->
            builder.addImages(createTImage(format))
        }

        return builder.build()
    }

    fun createTImage(format: String): BannerResources.TImagesInfo.TImage {
        return BannerResources.TImagesInfo.TImage.newBuilder()
            .setFormat(format)
            .setWidth(200)
            .setHeight(100)
            .build()
    }

    fun createAvatarsImage(imageName: String): BannerResources.TImagesInfo.TAvatarsImage {
        return BannerResources.TImagesInfo.TAvatarsImage.newBuilder()
            .setNamespace("namespace")
            .setImageGroupId("group")
            .setImageName(imageName)
            .build()
    }

    fun checkOffersEquals(convertedOffers: FeedOfferExamples?, expectedResult: FeedOfferExamples) {
        convertedOffers?.offers?.forEach { (_, value) -> value.updateDate = null }
        expectedResult.offers?.forEach { (_, value) -> value.updateDate = null }
        assertThat(convertedOffers).isEqualTo(expectedResult)
    }
}

