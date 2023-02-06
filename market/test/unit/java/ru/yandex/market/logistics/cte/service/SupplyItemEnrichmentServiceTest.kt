package ru.yandex.market.logistics.cte.service

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.any
import org.mockito.Mockito.clearAllCaches
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.yandex.market.logistics.cte.entity.supply.CompositeKey
import ru.yandex.market.logistics.cte.entity.supply.SupplyItem
import ru.yandex.market.logistics.cte.service.property.SystemPropertyBoolKey
import ru.yandex.market.mboc.http.DeliveryParams
import ru.yandex.market.mboc.http.DeliveryParamsStub
import ru.yandex.market.mboc.http.MboMappingsForDelivery.OfferFulfilmentInfo
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse

internal class SupplyItemEnrichmentServiceTestv2(
    private val deliveryParamsService: DeliveryParams = mock(DeliveryParamsStub::class.java),
    private val systemPropertyService: SystemPropertyService = mock(SystemPropertyService::class.java)
) {

    private var supplyItemEnrichmentService: SupplyItemEnrichmentService? = null

    @BeforeEach
    fun init() {
        clearAllCaches()
        val mappingResponse = SearchFulfilmentSskuParamsResponse.newBuilder()
            .addFulfilmentInfo(ITEM_INFO)
            .build()
        `when`(deliveryParamsService.searchFulfilmentSskuParams(any()))
            .thenReturn(mappingResponse)
        `when`(systemPropertyService.getBooleanProperty(SystemPropertyBoolKey.SEARCH_CATEGORY_ID_IN_MBO))
            .thenReturn(true)
        supplyItemEnrichmentService = SupplyItemEnrichmentService(deliveryParamsService, systemPropertyService)
    }

    @Test
    fun shouldEnrichSupplyItem() {
        val supplyItem = SupplyItem(
            compositeKey = CompositeKey(vendorId = SUPPLIER_ID, marketShopSku = "sku.sku"),
            attributes = setOf()
        )
        supplyItemEnrichmentService!!.enrichWithMboData(supplyItem)
        MatcherAssert.assertThat(supplyItem.categoryId, Matchers.equalTo(MARKET_CATEGORY_1))
        verify(deliveryParamsService, times(1))
            .searchFulfilmentSskuParams(ArgumentMatchers.any())
    }

    companion object {
        private const val SUPPLIER_ID = 100500L
        private const val SUPPLIER_SKU_1 = "sku.sku"
        private const val MARKET_CATEGORY_1 = 13
        private val ITEM_INFO = createItemInfo()
        private fun createItemInfo(): OfferFulfilmentInfo {
            return OfferFulfilmentInfo.newBuilder()
                .setSupplierId(SUPPLIER_ID.toInt())
                .setShopSku(SUPPLIER_SKU_1)
                .setMarketCategoryId(MARKET_CATEGORY_1.toLong())
                .build()
        }
    }
}
