package ru.yandex.market.contentmapping.utils

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Assert
import org.junit.Test
import ru.yandex.market.contentmapping.dto.model.CategoryRestriction
import ru.yandex.market.contentmapping.dto.model.CategoryRestrictionType
import ru.yandex.market.contentmapping.dto.model.Picture
import ru.yandex.market.contentmapping.dto.model.ValueSource
import ru.yandex.market.contentmapping.testdata.TestDataUtils.testShopModel
import ru.yandex.market.contentmapping.utils.MbocOfferToShopModelMerger.merge
import ru.yandex.market.mboc.http.SupplierOffer

class MbocOfferToShopModelMergerTest {
    val commonShopSku = "2"

    @Test
    fun noPicsInModel() {
        val offerPicUrl = "1"
        val offer = supplierOffer {
            it.addPictureUrl(offerPicUrl)
            it.shopSkuId = commonShopSku
        }
        val model = testShopModel(id = 1, shopSku = commonShopSku)
        val merged = merge(offer, model)
        Assert.assertEquals(1, merged.pictures.size.toLong())
        Assert.assertEquals(offerPicUrl, merged.pictures[0].url)
    }

    @Test
    fun `should also take pics from urls`() {
        val offerPicUrl = "1"
        val offer = supplierOffer {
            it.addPictureUrl(offerPicUrl)
            it.addUrls("http://somewhere/")
            it.addUrls("http://somewhere/pic.png")
            it.shopSkuId = commonShopSku
        }
        val model = testShopModel(id = 1, shopSku = commonShopSku)
        val merged = merge(offer, model)
        merged.asClue {
            it.pictures shouldHaveSize 2
            it.pictures[0].url shouldBe offerPicUrl
            it.pictures[1].url shouldBe "http://somewhere/pic.png"
        }
    }

    @Test
    fun hasPicsInModel() {
        val offerPicUrl = "1"
        val modelPics = listOf(Picture("2", ValueSource.MANUAL, 1, true))
        val offer = supplierOffer {
            it.shopSkuId = commonShopSku
            it.addPictureUrl(offerPicUrl)
        }
        val model = testShopModel(id = 1, shopSku = commonShopSku).copy(pictures = modelPics)
        val merged = merge(offer, model)
        Assert.assertEquals(modelPics, merged.pictures)
    }

    @Test
    fun `it should parse category restrictions`() {
        val offer = supplierOffer {
            it.shopSkuId = commonShopSku
            it.categoryRestriction = protoCategoryRestriction {
                type = SupplierOffer.Offer.CategoryRestriction.AllowedType.GROUP
                allowedGroupId = 42L
            }
        }
        val model = testShopModel(id = 1, shopSku = commonShopSku)
        val merged = merge(offer, model)

        merged.categoryRestriction shouldBe CategoryRestriction(CategoryRestrictionType.ALLOW_GROUP, goodsGroupId = 42L)
    }

    @Test
    fun `it should convert restrictions and fail on incorrect data`() {
        MbocOfferToShopModelMerger.convertCategoryRestriction(protoCategoryRestriction {
            type = SupplierOffer.Offer.CategoryRestriction.AllowedType.INDETERMINABLE
        }) shouldBe CategoryRestriction.ALLOW_NONE

        MbocOfferToShopModelMerger.convertCategoryRestriction(protoCategoryRestriction {
            type = SupplierOffer.Offer.CategoryRestriction.AllowedType.ANY
        }) shouldBe CategoryRestriction.ALLOW_ANY

        shouldThrow<IllegalArgumentException> {
            MbocOfferToShopModelMerger.convertCategoryRestriction(protoCategoryRestriction {
                type = SupplierOffer.Offer.CategoryRestriction.AllowedType.GROUP
            })
        }

        shouldThrow<IllegalArgumentException> {
            MbocOfferToShopModelMerger.convertCategoryRestriction(protoCategoryRestriction {
                type = SupplierOffer.Offer.CategoryRestriction.AllowedType.SINGLE
            })
        }

        MbocOfferToShopModelMerger.convertCategoryRestriction(protoCategoryRestriction {
            type = SupplierOffer.Offer.CategoryRestriction.AllowedType.GROUP
            allowedGroupId = 42L
        }) shouldBe CategoryRestriction(CategoryRestrictionType.ALLOW_GROUP, goodsGroupId = 42L)

        MbocOfferToShopModelMerger.convertCategoryRestriction(protoCategoryRestriction {
            type = SupplierOffer.Offer.CategoryRestriction.AllowedType.SINGLE
            allowedCategoryId = 42L
        }) shouldBe CategoryRestriction(CategoryRestrictionType.ALLOW_SINGLE, categoryId = 42L)
    }

    @Test
    fun `it should update category not matching restrictions`() {
        val offer = supplierOffer {
            it.shopSkuId = commonShopSku
            it.categoryRestriction = protoCategoryRestriction {
                type = SupplierOffer.Offer.CategoryRestriction.AllowedType.SINGLE
                allowedCategoryId = 12
            }
        }
        val model = testShopModel(id = 1, shopSku = commonShopSku, marketCategoryId = 1)
        val merged = merge(offer, model)

        merged.marketCategoryId shouldBe 12
    }

    @Test
    fun `it shouldn't update category matching restrictions`() {
        val offer = supplierOffer {
            it.shopSkuId = commonShopSku
            it.categoryRestriction = protoCategoryRestriction {
                type = SupplierOffer.Offer.CategoryRestriction.AllowedType.ANY
            }
        }
        val model = testShopModel(id = 1, shopSku = commonShopSku, marketCategoryId = 1)
        val merged = merge(offer, model)

        merged.marketCategoryId shouldBe 1
    }

    private fun protoCategoryRestriction(builder: SupplierOffer.Offer.CategoryRestriction.Builder.() -> Unit) =
            SupplierOffer.Offer.CategoryRestriction.newBuilder().apply(builder).build()
}
