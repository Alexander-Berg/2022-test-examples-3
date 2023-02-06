package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class OrderControllerTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml"
    )
    fun `get orders ready for shipment with no orders`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders"),
            responseFile = "controller/shipping/orders/orders_empty_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/before.xml"
    )
    fun `get orders ready for shipment`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders"),
            responseFile = "controller/shipping/orders/orders_without_filters_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/before.xml"
    )
    fun `get orders ready for shipment with ASC order by status and limit`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?order=ASC&orderKey=status&limit=2"),
            responseFile = "controller/shipping/orders/orders_with_order_by_status_and_limit.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/before.xml"
    )
    fun `get orders ready for shipment with desc order, limit and offset`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?order=DESC&limit=3&offset=1"),
            responseFile = "controller/shipping/orders/orders_with_desc_order_limit_and_offset.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/before.xml"
    )
    fun `get orders ready for shipment with filter by carrierName`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?filter=(carrierName=='Test carrier 2')"),
            responseFile = "controller/shipping/orders/orders_with_filter_by_carrierName.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/order_with_null_shipment_date_time.xml"
    )
    fun `get orders with null shipmentDateTime field`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders"),
            responseFile = "controller/shipping/orders/orders_with_null_shipment_date_time.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/orders_with_unprocessable_types.xml"
    )
    fun `get orders with unprocessable types`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders"),
            responseFile = "controller/shipping/orders/orders_empty_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/orders_with_unprocessable_statuses.xml"
    )
    fun `get orders with unprocessable statuses`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders"),
            responseFile = "controller/shipping/orders/orders_empty_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/orders_with_sorting_by_external_order_key.xml"
    )
    fun `get orders with sorting by externalOrderKey`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders"),
            responseFile = "controller/shipping/orders/orders_with_soring_by_external_order_key.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/before.xml"
    )
    fun `get orders with filter by order keys`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?" +
                "filter=(orderKey=='ORD0002',orderKey=='ORD0007',orderKey=='ORD0005',orderKey=='ORD0001',orderKey=='')"),
            responseFile = "controller/shipping/orders/orders_with_filter_by_order_keys.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/before.xml"
    )
    fun `try to get orders using wrong filter`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?filter=(absolutelyWrongField=='ORD0001')"),
            status = MockMvcResultMatchers.status().isBadRequest,
            responseFile = "controller/shipping/orders/wrong_filter_format.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/before.xml"
    )
    fun `get orders with filter by statuses`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?" +
                "filter=(status=='PART_SHIPPED',status=='PICKED_PART_SHIPPED',status=='IMPOSSIBLE_STATUS')"),
            responseFile = "controller/shipping/orders/orders_with_filter_statuses.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/before.xml"
    )
    fun `get orders with filter by types`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?" +
                "filter=(type=='OUTBOUND_FIT',type=='OUTBOUND_SURPLUS',type=='IMPOSSIBLE_TYPE')"),
            responseFile = "controller/shipping/orders/orders_with_filter_types.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/orders_with_for_filter_by_external_order_key.xml"
    )
    fun `get orders with filter by external order key`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?" +
                "filter=(externalOrderKey=='%ium',externalOrderKey=='ORD2',externalOrderKey=='8-800-555-35-35')"),
            responseFile = "controller/shipping/orders/orders_with_for_filter_by_external_order_key.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/orders/db/common.xml",
        "/controller/shipping/orders/db/orders_with_for_filter_by_external_order_key.xml"
    )
    fun `get orders with filter by withdrawal external order key`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/orders?" +
                "filter=((externalOrderKey=='ORD0006',externalOrderKey=='outbound-ORD0007',externalOrderKey=='ORD0008');" +
                "status=='PART_SHIPPED')"),
            responseFile = "controller/shipping/orders/orders_with_filter_by_withdrawal_external_order_key.json"
        )
    }
}
