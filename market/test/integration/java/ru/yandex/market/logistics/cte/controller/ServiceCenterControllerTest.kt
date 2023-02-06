package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest
import ru.yandex.market.logistics.cte.util.ResettableSequenceStyleGenerator.Companion.resetAllInstances

class ServiceCenterControllerTest : MvcIntegrationTest() {

    @BeforeEach
    fun resetSequences() {
        resetAllInstances()
    }

    @Test
    @DatabaseSetups(DatabaseSetup("classpath:service/empty.xml"))
    @ExpectedDatabases(ExpectedDatabase(value = "classpath:service/service_center/service_center_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT))
    @Throws(Exception::class)
    fun addServiceCenter() {

        testEndpointPostStatus(
                "/service_center/asc", "service/service_center/request.json", HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(DatabaseSetup("classpath:service/service_center/service_center_before.xml"))
    @ExpectedDatabases(ExpectedDatabase(value = "classpath:service/service_center/service_center_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT))
    @Throws(Exception::class)
    fun addServiceCenterWithDuplicateName() {
        testEndpointPostStatus(
                "/service_center/asc", "service/service_center/request.json", HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
            DatabaseSetup("classpath:service/service_center/service_centers_get_all_in_alphabetical_order.xml"))
    @ExpectedDatabases(
            ExpectedDatabase(
                    value = "classpath:service/service_center/service_centers_get_all_in_alphabetical_order.xml",
                    assertionMode = DatabaseAssertionMode.NON_STRICT
            )
    )
    @Throws(Exception::class)
    fun getAllServiceCentersInAlphabeticalOrder() {
        testGetEndpoint(
                "/service_center/asc/list",
                LinkedMultiValueMap(),
                "controller/service-center/get_service_centers_response.json", HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/service-center/get-items-to-send/before.xml"))
    fun getServiceCenterItemsToSendTest() {
        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "uit")
            add("offset", "0")
            add("limit", "10")
        }

        testGetEndpoint(
            "/service_center/item/list",
            params,
            "controller/service-center/get-items-to-send/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/service-center/get-items-to-send-with-limit/before.xml"))
    fun getServiceCenterItemsToSendWithLimitTest() {
        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "id")
            add("offset", "1")
            add("limit", "2")
            add("order", "DESC")
        }

        testGetEndpoint(
            "/service_center/item/list",
            params,
            "controller/service-center/get-items-to-send-with-limit/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/service-center/get-items-to-send-with-filter/before.xml"))
    fun getServiceCenterItemsToSendWithFilterTest() {
        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "uit")
            add("offset", "0")
            add("limit", "10")
            add("filter", "id=='1'; name=='Стремянка Nika СМ4'; " +
                "uit=='7741639862'; categoryId=='1'; marketShopSku=='1234'; ownerId=='111'; " +
                "soldAt=='2022-05-31 12:00:00'; serviceCenterSentAt=='2022-01-01 12:00:00' ")
        }

        testGetEndpoint(
            "/service_center/item/list",
            params,
            "controller/service-center/get-items-to-send-with-filter/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/service-center/get-items-to-send-with-filter/before.xml"))
    fun getServiceCenterItemsToSendWithAscFilterTest() {
        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "uit")
            add("offset", "0")
            add("limit", "10")
            add("filter", "serviceCenterName=='Рубин'")
        }

        testGetEndpoint(
            "/service_center/item/list",
            params,
            "controller/service-center/get-items-to-send-with-filter/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/service-center/get-items-to-send-without-deleted/before.xml"))
    fun getServiceCenterItemsToSendWithoutDeletedItemsTest() {
        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "uit")
            add("offset", "0")
            add("limit", "10")
        }

        testGetEndpoint(
            "/service_center/item/list",
            params,
            "controller/service-center/get-items-to-send-without-deleted/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
            DatabaseSetup(
                    "classpath:controller/service-center/create-service-center-items-to-send/qattribute.xml"),
            DatabaseSetup(
                    "classpath:controller/service-center/create-service-center-items-to-send/supply_items.xml")
    )
    @ExpectedDatabases(
            ExpectedDatabase(
                    value = "classpath:" +
                        "controller/service-center/create-service-center-items-to-send/after_service_center_items_to_send.xml",
                    assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
            )
    )
    fun createServiceCenterItemsToSend() {
        testEndpointPostStatus(
                "/service_center/item/create-from-supply-item",
                "controller/service-center/" +
                        "create-service-center-items-to-send/create-service-center-items-to-send.json",
                 HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(
            "classpath:controller/service-center/create-service-center-items-to-send/qattribute.xml"),
        DatabaseSetup(
            "classpath:" +
                "controller/service-center/do-not-create-exists-service-center-items-to-send/before.xml")
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:" +
                "controller/service-center/do-not-create-exists-service-center-items-to-send/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    fun doNotCreateAlreadyExistsServiceCenterItemsToSend() {
        testEndpointPostStatus(
            "/service_center/item/create-from-supply-item",
            "controller/service-center/" +
                "do-not-create-exists-service-center-items-to-send/request.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(
            "classpath:controller/service-center/create-service-center-items-to-send/qattribute.xml"),
        DatabaseSetup(
            "classpath:" +
                "controller/service-center/create-exists-service-center-items-to-send/before.xml")
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:" +
                "controller/service-center/create-exists-service-center-items-to-send/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
        )
    )
    fun createAlreadyExistsServiceCenterItemsToSendInFinishedStatus() {
        testEndpointPostStatus(
            "/service_center/item/create-from-supply-item",
            "controller/service-center/" +
                "create-exists-service-center-items-to-send/request.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/service-center/get-count-by-status/before.xml")
    )
    fun getItemsCountsByStatus() {

        val params = LinkedMultiValueMap<String, String>()

        testGetEndpoint(
            "/service_center/item/status-count",
            params,
            "controller/service-center/get-count-by-status/response.json",
            HttpStatus.OK)
    }

}
