package ru.yandex.market.contentmapping.services.category.info

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.services.shop.ShopService
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.MboCategory.ForceOfferCategoryResponse
import ru.yandex.market.mboc.http.MboCategory.ForceOfferReclassificationResponse
import ru.yandex.market.mboc.http.MboCategoryService
import ru.yandex.market.mboc.http.SupplierOffer.OperationResult
import ru.yandex.market.mboc.http.SupplierOffer.OperationStatus

class CategoryControlServiceImplTest {
    private val shopId1 = 121212L
    private val businessId1 = 232323L
    private val businessId2 = 364462L
    private val shopSku1 = "sku1"
    private val shopSku2 = "sku2"
    private val shopSkus = listOf(shopSku1, shopSku2)
    private val staffLogin = "shimmermare"

    private lateinit var shopServiceMock: ShopService
    private lateinit var mboCategoryServiceMock: MboCategoryService

    @Before
    fun setup() {
        shopServiceMock = mock {
            doReturn(Shop(id = shopId1, name = "Shop", businessId = businessId1))
                    .`when`(it).findById(eq(shopId1))
            doReturn(Shop(id = businessId2, name = "Shop", businessId = businessId2))
                    .`when`(it).findById(eq(businessId2))
        }
        mboCategoryServiceMock = mock {
            doReturn(ForceOfferCategoryResponse.newBuilder()
                    .setResult(
                            OperationResult.newBuilder()
                                    .setStatus(OperationStatus.SUCCESS)
                                    .setMessage("Success")
                                    .build()
                    ).build()
            ).`when`(it).forceOfferCategory(Mockito.any())
            doReturn(
                    ForceOfferReclassificationResponse.newBuilder().setResult(
                            OperationResult.newBuilder()
                                    .setStatus(OperationStatus.SUCCESS)
                                    .setMessage("Success")
                                    .build()
                    ).build()
            ).`when`(it).forceOfferReclassification(any())
        }
    }

    @Test
    fun `Correctly builds force category request`() {
        val categoryId = 565656L

        val categoryControlService = CategoryControlServiceImpl(shopServiceMock, mboCategoryServiceMock)
        val success = categoryControlService.forceOffersCategory(shopId1, shopSkus, categoryId, staffLogin)

        success shouldBe true

        val requestCaptor = argumentCaptor<MboCategory.ForceOfferCategoryRequest>()
        verify(mboCategoryServiceMock).forceOfferCategory(requestCaptor.capture())
        val operationsBySku = requestCaptor.firstValue.operationsList.associateBy { it.shopSku }

        operationsBySku shouldHaveSize 2

        operationsBySku shouldContainKey shopSku1
        val test1 = operationsBySku[shopSku1]!!
        test1.businessId shouldBe shopId1
        test1.categoryId shouldBe categoryId
        test1.staffLogin shouldBe staffLogin

        operationsBySku shouldContainKey (shopSku2)
        val test2 = operationsBySku[shopSku2]!!
        test2.businessId shouldBe shopId1
        test2.categoryId shouldBe categoryId
        test2.staffLogin shouldBe staffLogin
    }


    @Test
    fun `Correctly builds force reclassification request`() {
        val classificationService = CategoryControlServiceImpl(shopServiceMock, mboCategoryServiceMock)
        val result = classificationService.forceReclassifficateOffers(businessId2, shopSkus, staffLogin)

        result shouldBe true

        val requestCaptor = argumentCaptor<MboCategory.ForceOfferReclassificationRequest>()
        verify(mboCategoryServiceMock).forceOfferReclassification(requestCaptor.capture())

        val operationsBySku = requestCaptor.firstValue.operationsList.associateBy { it.shopSku }
        operationsBySku shouldHaveSize 2

        operationsBySku shouldContainKey shopSku1
        val test1 = operationsBySku[shopSku1]!!
        test1.businessId shouldBe businessId2
        test1.staffLogin shouldBe staffLogin

        operationsBySku shouldContainKey shopSku2
        val test2 = operationsBySku[shopSku2]!!
        test2.businessId shouldBe businessId2
        test2.staffLogin shouldBe staffLogin
    }
}
