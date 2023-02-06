package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.core.supplier.model.SupplierType
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi
import ru.yandex.market.ff.client.dto.ShopRequestYardDTO
import ru.yandex.market.ff.client.dto.ShopRequestYardDTOContainer
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.ShopRequestInfoFacade
import java.time.LocalDateTime

class ShopRequestInfoFacadeTest(
    @Autowired private val shopRequestInfoFacade: ShopRequestInfoFacade,
    @Autowired private val ffWorkflowApiClient: FulfillmentWorkflowClientApi,
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/shop-request-info-facade/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/shop-request-info-facade/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testLoadShopRequests() {
        Mockito.`when`(ffWorkflowApiClient.getRequestsForYard(any())).thenReturn(
            getTestShopRequestContainer()
        )
        shopRequestInfoFacade.loadShopRequests()
    }

    private fun getTestShopRequestContainer(): ShopRequestYardDTOContainer {
        val result = ShopRequestYardDTOContainer()

        result.addRequest(
            ShopRequestYardDTO().apply {
                id = 13014681
                serviceRequestId = "6936147_PARTNER_ID"
                externalRequestId = "TMU743928"
                totalItemsCount = 987
                totalPalletsCount = 1
                readableType = "Изъятие для межскладского перемещения"
                requestedDate = LocalDateTime.parse("2022-07-03T04:11:42")
                serviceId = 172
            })
        result.addRequest(
            ShopRequestYardDTO().apply {
                id = 13014682
                serviceRequestId = "0000929037"
                totalItemsCount = 10
                totalPalletsCount = 1
                supplierName = ""
                supplierType = SupplierType.THIRD_PARTY
                readableType = "Магистральный X-DOC"
                requestedDate = LocalDateTime.parse("2022-07-03T04:11:42")
                realSupplierId = "10264281"
                serviceId = 147
            })
        return result
    }
}
