package ru.yandex.market.fulfillment.wrap.marschroute.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.wrap.marschroute.api.ProductsClient;
import ru.yandex.market.fulfillment.wrap.marschroute.api.WaybillsClient;
import ru.yandex.market.fulfillment.wrap.marschroute.api.request.waybill.CreateInboundRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill.CreateWaybillResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybill;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybillItem;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.MarschrouteUpdateProductRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.MarschrouteUpdateProductsRequest;

class UpdateProductsTimeMeasureTest extends IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(UpdateProductsTimeMeasureTest.class);

    private static final int ITEMS_COUNT = 100;

    @Autowired
    private WaybillsClient waybillsClient;

    @Autowired
    private ProductsClient productsClient;

    /**
     * This test was used to measure how much the products update (using distinct product updates) duration
     * is longer than the duration of waybill with these products creation.
     * According to results the products update is around 5 times as much as the waybill creation.
     * <p>
     * If you unignore this test, don't forget to edit the application.properties file.
     */
    @Disabled("Unignore if you need to measure waybill creating and distinct products updatings durations.")
    @Test
    void measureDistinctProductUpdatesTime() {
        MarschrouteWaybill waybill = new MarschrouteWaybill()
            .setDate(LocalDateTime.now().plusDays(1));

        List<MarschrouteWaybillItem> items = generateTestItems();

        CreateInboundRequest createInboundRequest = new CreateInboundRequest()
            .setWaybill(waybill)
            .setItems(items);

        long createInboundBeginTs = new Date().getTime();
        waybillsClient.createWaybill(createInboundRequest);
        long createInboundEndTs = new Date().getTime();

        long createInboundTime = createInboundEndTs - createInboundBeginTs;

        long updateProductsBeginTs = new Date().getTime();
        for (MarschrouteWaybillItem item : items) {
            MarschrouteUpdateProductRequest updateProductRequest = new MarschrouteUpdateProductRequest()
                .setName(item.getName() + " Edited")
                .setLifetime(true);
            productsClient.updateProduct(item.getItemId(), updateProductRequest);
        }
        long updateProductsEndTs = new Date().getTime();

        long updateProductsTime = updateProductsEndTs - updateProductsBeginTs;

        logger.info("Create inbound time: {} ms", createInboundTime);
        logger.info("Update products time: {} ms", updateProductsTime);
    }

    /**
     * This test was used to measure how much the products update (using batch products update) duration
     * is longer than the duration of waybill with these products creation.
     * According to results the products update is around 1.5 times as much as the waybill creation.
     * <p>
     * If you unignore this test, don't forget to edit the application.properties file.
     */
    @Disabled("Unignore if you need to measure waybill creating and batch products updating duration.")
    @Test
    void measureBatchProductUpdatesTime() {
        MarschrouteWaybill waybill = new MarschrouteWaybill()
            .setDate(LocalDateTime.now().plusDays(1));

        List<MarschrouteWaybillItem> items = generateTestItems();

        CreateInboundRequest createInboundRequest = new CreateInboundRequest()
            .setWaybill(waybill)
            .setItems(items);

        long createInboundBeginTs = new Date().getTime();
        CreateWaybillResponse createWaybillResponse = waybillsClient.createWaybill(createInboundRequest);
        long createInboundEndTs = new Date().getTime();

        softly.assertThat(createWaybillResponse.isSuccessful())
            .as("Create inbound success")
            .isTrue();

        long createInboundTime = createInboundEndTs - createInboundBeginTs;

        long updateProductsBeginTs = new Date().getTime();

        List<MarschrouteUpdateProductRequest> updateProductRequests = items.stream()
            .map(item -> new MarschrouteUpdateProductRequest()
                .setItemId(item.getItemId())
                .setName(item.getName() + " Edited")
                .setLifetime(true))
            .collect(Collectors.toList());
        productsClient.updateProducts(new MarschrouteUpdateProductsRequest().setItems(updateProductRequests));
        long updateProductsEndTs = new Date().getTime();

        long updateProductsTime = updateProductsEndTs - updateProductsBeginTs;

        logger.info("Create inbound time: {} ms", createInboundTime);
        logger.info("Update products time: {} ms", updateProductsTime);
    }

    private List<MarschrouteWaybillItem> generateTestItems() {
        int randomNumber = new Random().nextInt();

        List<MarschrouteWaybillItem> items = new ArrayList<>();

        for (int i = 0; i < ITEMS_COUNT; i++) {
            MarschrouteWaybillItem item = new MarschrouteWaybillItem()
                .setItemId("UpdateProductsTimeMeasureTest.Item." + randomNumber + "." + i)
                .setName("Test Item #" + i)
                .setPriceNds(BigDecimal.ZERO)
                .setQuantity(1)
                .setSumNds(BigDecimal.ZERO);

            items.add(item);
        }

        return items;
    }
}
