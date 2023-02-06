package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest
import ru.yandex.market.logistics.cte.converters.SupplyDtoToSupplyConverter
import ru.yandex.market.logistics.cte.converters.SupplyItemDtoToSupplyItemConverter
import ru.yandex.market.logistics.cte.repo.SupplyItemRepository
import ru.yandex.market.mboc.http.MboMappingsForDelivery
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse

@ContextConfiguration(classes = [SupplyDtoToSupplyConverter::class, SupplyItemDtoToSupplyItemConverter::class])
class SupplyControllerResolveQualityAttributesTest(
    @Autowired private val supplyItemRepository: SupplyItemRepository
): MvcIntegrationTest() {
    @AfterEach
    fun invalidateCache() {
        Mockito.reset(deliveryParams)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml"),
    )

    fun successByCategoryId() {

        val vendorId = "14"
        val marketShopSku = "ff"
        val categoryId = 11

        val params = LinkedMultiValueMap<String, String>().apply{
            add("categoryId", categoryId.toString())
            add("vendorId", vendorId)
            add("marketShopSku", marketShopSku)
        }

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-supply_item",
            params,
            "controller/resolveQualityAttributes/by-categoryId_response.json", HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml"),
    )

    fun defaultGroupIfCategoryNotFound() {
        val vendorId = "14"
        val marketShopSku = "ff"
        val categoryId = "999"

        val params = LinkedMultiValueMap<String, String>().apply{
            add("categoryId", categoryId)
            add("vendorId", vendorId)
            add("marketShopSku", marketShopSku)
        }

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-supply_item",
            params,
            "controller/resolveQualityAttributes/default-group_response.json",
            HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml"),
    )
    fun defaultGroupIfCategoryEmpty() {
        val vendorId = "14"
        val marketShopSku = "ff"

        val params = LinkedMultiValueMap<String, String>().apply{
            add("vendorId", vendorId)
            add("marketShopSku", marketShopSku)
        }

        Mockito.`when`(deliveryParams.searchFulfilmentSskuParams(ArgumentMatchers.any()))
            .thenReturn(buildEmptyMappingResponse())

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-supply_item",
            params,
            "controller/resolveQualityAttributes/default-group_response.json",
            HttpStatus.OK,
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml"),
    )
    fun defaultGroupIfCategoryWithoutGroup() {
        val vendorId = "14"
        val marketShopSku = "ff"
        val categoryId = 4

        val params = LinkedMultiValueMap<String, String>().apply{
            add("vendorId", vendorId)
            add("marketShopSku", marketShopSku)
            add("categoryId", categoryId.toString())
        }

        Mockito.`when`(deliveryParams.searchFulfilmentSskuParams(ArgumentMatchers.any()))
            .thenReturn(buildEmptyMappingResponse())

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-supply_item",
            params,
            "controller/resolveQualityAttributes/default-group_response.json",
            HttpStatus.OK,
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml"),
        DatabaseSetup("classpath:repository/supply_item_attribute.xml")
    )
    fun successBySupplyItemCategoryWhenRequestCategoryEmptyAndMBOMappingNotFound() {
        val vendorId = "10264169"
        val marketShopSku = "shopSku311"

        val params = LinkedMultiValueMap<String, String>().apply{
            add("vendorId", vendorId)
            add("marketShopSku", marketShopSku)
        }

        Mockito.`when`(deliveryParams.searchFulfilmentSskuParams(ArgumentMatchers.any()))
            .thenReturn(buildEmptyMappingResponse())

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-supply_item",
            params,
            "controller/resolveQualityAttributes/default-group_response.json",
            HttpStatus.OK,
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml"),
        DatabaseSetup("classpath:repository/supply_item_attribute.xml")
    )
    fun defaultGroupIfCategoryEmptyAndMBOMappingNotFoundAndSupplyItemWithoutQualityGroup() {
        val vendorId = "10264169"
        val marketShopSku = "shopSku3"

        val params = LinkedMultiValueMap<String, String>().apply{
            add("vendorId", vendorId)
            add("marketShopSku", marketShopSku)
        }

        Mockito.`when`(deliveryParams.searchFulfilmentSskuParams(ArgumentMatchers.any()))
            .thenReturn(buildEmptyMappingResponse())

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-supply_item",
            params,
            "controller/resolveQualityAttributes/default-group_response.json",
            HttpStatus.OK,
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml"),
        DatabaseSetup("classpath:repository/supply_item_attribute.xml")
    )
    fun defaultGroupIfCategoryEmptyAndMBOMappingNotFound() {
        val vendorId = "10264169"
        val marketShopSku = "shopSku3"

        val params = LinkedMultiValueMap<String, String>().apply{
            add("vendorId", vendorId)
            add("marketShopSku", marketShopSku)
        }

        Mockito.`when`(deliveryParams.searchFulfilmentSskuParams(ArgumentMatchers.any()))
            .thenReturn(buildEmptyMappingResponse())

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-supply_item",
            params,
            "controller/resolveQualityAttributes/default-group_response.json",
            HttpStatus.OK,
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml"),
    )
    fun successWithEmptyCategoryId() {

        val vendorId = "14"
        val marketShopSku = "ff"
        val categoryId = 11

        val params = LinkedMultiValueMap<String, String>().apply{
            add("vendorId", vendorId)
            add("marketShopSku", marketShopSku)
        }

        Mockito.`when`(deliveryParams.searchFulfilmentSskuParams(ArgumentMatchers.any()))
            .thenReturn(buildMappingResponse(vendorId.toInt(), marketShopSku, categoryId))

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-supply_item",
            params,
            "controller/resolveQualityAttributes/group_1_response.json", HttpStatus.OK
        )
    }

    private fun buildMappingResponse(vendorId: Int, marketShopSku:String, categoryId: Int
    ): SearchFulfilmentSskuParamsResponse {
        return SearchFulfilmentSskuParamsResponse.newBuilder()
            .addFulfilmentInfo(
                    MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                        .setSupplierId(vendorId)
                        .setShopSku(marketShopSku)
                        .setMarketCategoryId(categoryId.toLong())
                ).build()
    }

    private fun buildMappingResponse(vendorId: Int, marketShopSku:String): SearchFulfilmentSskuParamsResponse {
        return SearchFulfilmentSskuParamsResponse.newBuilder()
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSupplierId(vendorId)
                    .setShopSku(marketShopSku)
            ).build()
    }

    private fun buildEmptyMappingResponse(): SearchFulfilmentSskuParamsResponse {
        return SearchFulfilmentSskuParamsResponse.newBuilder().build()
    }
}
