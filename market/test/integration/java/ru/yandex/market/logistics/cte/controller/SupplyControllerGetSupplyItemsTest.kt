package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest

class SupplyControllerGetSupplyItemsTest: MvcIntegrationTest() {

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:controller/supply-controller/get-items/group.xml"),
        DatabaseSetup("classpath:repository/supply_item_attribute.xml")
    )
    fun getItems() {

        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "vendorId")
            add("offset", "0")
            add("limit", "10")
        }

        testGetEndpoint(
            "/logistic_services/supplies/item/list",
            params,
            "controller/supply-controller/get-items/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
            DatabaseSetup("classpath:repository/qattribute.xml"),
            DatabaseSetup("classpath:controller/supply-controller/get-items/group.xml"),
            DatabaseSetup(
                    "classpath:controller/supply-controller/get-items-grouped-by-uit/supply_item_attribute.xml"
            )
    )
    fun getItemsGroupedByUit() {

        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "vendorId")
            add("offset", "0")
            add("limit", "10")
        }

        testGetEndpoint(
                "/logistic_services/supplies/item/grouped-by-uit-list",
                params,
                "controller/supply-controller/get-items-grouped-by-uit/response.json",
                HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:controller/supply-controller/get-items/group.xml"),
        DatabaseSetup(
            "classpath:controller/supply-controller/get-items-grouped-by-uit/supply_item_attribute.xml"
        )
    )
    fun getItemsGroupedByUitWithFilterByAttribute() {

        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "vendorId")
            add("filter", "attributeName=='PACKAGE_HOLES'")
            add("offset", "0")
            add("limit", "10")
        }

        testGetEndpoint(
            "/logistic_services/supplies/item/grouped-by-uit-list",
            params,
            "controller/supply-controller/get-items-grouped-by-uit/response-with-filter.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
            DatabaseSetup("/controller/supply-controller/get-items-by-supply-id/qattribute.xml"),
            DatabaseSetup("/controller/supply-controller/get-items/group.xml"),
            DatabaseSetup("/controller/supply-controller/get-items-by-supply-id/supply_item_attribute.xml")
    )
    fun getItemsBySupplyId() {
        testGetEndpoint(
            "/logistic_services/supplies/10/item/list",
            LinkedMultiValueMap(),
            "controller/supply-controller/get-items-by-supply-id/response.json",
            HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetups(
            DatabaseSetup("classpath:repository/qattribute.xml"),
            DatabaseSetup("classpath:controller/supply-controller/get-items/group.xml"),
            DatabaseSetup("classpath:repository/supply_item_attribute.xml")
    )
    fun getItemsExcel() {

        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "vendorId")
        }

        testGetEndpointWithExcelFile(
                "/logistic_services/supplies/item/list/excel",
                params,
                HttpStatus.OK,
                "items_table.xlsx")
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:controller/supply-controller/get-items/group.xml"),
        DatabaseSetup("classpath:repository/supply_item_attribute.xml")
    )
    fun getItemsWithFilter() {

        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "vendorId")
            add("offset", "0")
            add("limit", "10")
            add("filter", "(boxId=='12341234')")
        }

        testGetEndpoint(
            "/logistic_services/supplies/item/list",
            params,
            "controller/supply-controller/get-items-with-filter/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:controller/supply-controller/get-items/group.xml"),
        DatabaseSetup("classpath:repository/supply_item_attribute.xml")
    )
    fun getItemsWithEnumFilter() {

        val params = LinkedMultiValueMap<String, String>().apply{
            add("sort", "vendorId")
            add("offset", "0")
            add("limit", "10")
            add("filter", "(stockType=='DAMAGE_RESELL')")
        }

        testGetEndpoint(
            "/logistic_services/supplies/item/list",
            params,
            "controller/supply-controller/get-items-with-enum-filter/response.json",
            HttpStatus.OK)
    }
}
