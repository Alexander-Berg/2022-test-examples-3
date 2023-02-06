package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest

class AuctionControllerTest: MvcIntegrationTest() {

    @Test
    fun createAuctionWithEmptyItems() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/empty-items/request.json",
            "controller/auction/empty-items/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    fun createAuctionWithoutVendorIdInItem() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/no-vendor-id-in-item/request.json",
            "controller/auction/no-vendor-id-in-item/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    fun createAuctionWithoutSkuInItem() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/no-sku-in-item/request.json",
            "controller/auction/no-sku-in-item/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    fun createAuctionWithoutUitsInItem() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/no-uits-in-item/request.json",
            "controller/auction/no-uits-in-item/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    fun createAuctionWithEmptyUitsInItem() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/empty-uits/request.json",
            "controller/auction/empty-uits/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    fun createAuctionWithDoubleUits() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/double-uits/request.json",
            "controller/auction/double-uits/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/auction/not-existing-uits/before.xml")
    fun createAuctionForNotExistingUits() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/not-existing-uits/request.json",
            "controller/auction/not-existing-uits/response.json",
            HttpStatus.NOT_FOUND
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/auction/more-than-one-sku-for-uit/before.xml")
    fun createAuctionWhenMoreThanOneSkuForUit() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/more-than-one-sku-for-uit/request.json",
            "controller/auction/more-than-one-sku-for-uit/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/auction/different-sku-for-uit/before.xml")
    fun createAuctionWhenDifferentSkuForUit() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/different-sku-for-uit/request.json",
            "controller/auction/different-sku-for-uit/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/auction/auction-already-exists/before.xml")
    fun createAuctionWhenItAlreadyExists() {
        testEndpointPost(
            "/auction/create-auction/500/1",
            "controller/auction/auction-already-exists/request.json",
            "controller/auction/auction-already-exists/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/auction/create-success/before.xml")
    @ExpectedDatabase(value = "classpath:controller/auction/create-success/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun createAuctionSuccess() {
        testEndpointPostStatus(
            "/auction/create-auction/500/1",
            "controller/auction/create-success/request.json",
            HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/auction/cancel-not-existing/before.xml")
    @ExpectedDatabase(value = "classpath:controller/auction/cancel-not-existing/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun cancelNotExistingAuction() {
        testEndpointPut(
            "/auction/cancel-auction/2",
            "controller/auction/cancel-not-existing/response.json",
            HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/auction/cancel-success/before.xml")
    @ExpectedDatabase(value = "classpath:controller/auction/cancel-success/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun cancelAuctionSuccess() {
        testEndpointPutStatus(
            "/auction/cancel-auction/1",
            HttpStatus.OK
        )
    }
}
