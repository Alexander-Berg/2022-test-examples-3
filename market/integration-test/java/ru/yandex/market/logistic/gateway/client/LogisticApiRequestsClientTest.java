package ru.yandex.market.logistic.gateway.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.CisHandleMode;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_XML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class LogisticApiRequestsClientTest extends AbstractRestTest {

    @Autowired
    private LogisticApiRequestsClient logisticApiRequestsClient;

    @Test
    public void pushStocks() throws IOException {
        mockLogisticApi("logistic_api/push_stocks_request.xml",
            "logistic_api/push_stocks_response.xml");

        logisticApiRequestsClient.pushStocks(Collections.singletonList(createItemStocks()));
    }

    @Test
    public void pushReferenceItems() throws IOException {
        mockLogisticApi("logistic_api/push_reference_items/push_reference_items_request.xml",
            "logistic_api/push_reference_items/push_reference_items_response.xml");

        logisticApiRequestsClient.pushReferenceItems(Collections.singletonList(createItem()));
    }

    @Test
    public void pushOrdersStatusesChanged() throws IOException {
        mockLogisticApi(
            "logistic_api/push_orders_statuses_changed.xml",
            null
        );

        logisticApiRequestsClient.pushOrdersStatusesChanged(
            Collections.singletonList(
                new ResourceId("145", "ff1")
            )
        );
    }

    private void mockLogisticApi(String request, String response) throws IOException {
        ResponseCreator respond = withStatus(OK).contentType(TEXT_XML);
        if (response != null) {
            respond = getTaskResponseCreator(response);
        }
        logisticApiMock.expect(requestTo(uri + "/" + "fulfillment/query-gateway"))
            .andExpect(content().string(
                CompareMatcher.isSimilarTo(getFileContent(request))
                    .ignoreWhitespace()
                    .normalizeWhitespace()
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))))
            .andRespond(respond);
    }

    private DefaultResponseCreator getTaskResponseCreator(String s) throws IOException {
        return withStatus(OK)
            .contentType(TEXT_XML)
            .body(getFileContent(s));
    }

    private ItemStocks createItemStocks() {
        return new ItemStocks(
            new UnitId("111", 222L, "333"),
            new ResourceId("145", "ff1"),
            Collections.singletonList(createStock())
        );
    }

    private Item createItem() {
        return new Item.ItemBuilder("Name", 10, BigDecimal.valueOf(130.5))
            .setUnitId(new UnitId("1", 2L, "art"))
            .setArticle("Article")
            .setBarcodes(Collections.singletonList(new Barcode("code", "type", BarcodeSource.UNKNOWN)))
            .setDescription("description")
            .setUntaxedPrice(BigDecimal.valueOf(120))
            .setCargoType(CargoType.PERISHABLE_CARGO)
            .setCargoTypes(new CargoTypes(Collections.singletonList(CargoType.ART)))
            .setKorobyte(new Korobyte(100, 101, 102,
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(100.1), BigDecimal.valueOf(100.2)))
            .setHasLifeTime(true)
            .setLifeTime(15)
            .setBoxCount(150)
            .setBoxCapacity(50)
            .setTax(new Tax(TaxType.VAT, VatValue.TEN))
            .setComment("Hello world")
            .setRemainingLifetimes(new RemainingLifetimes(getShelfLives(1, 2), getShelfLives(3, 4)))
            .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
            .build();
    }

    private ShelfLives getShelfLives(int days, int percentage) {
        return new ShelfLives(new ShelfLife(days), new ShelfLife(percentage));
    }

    private Stock createStock() {
        return new Stock(StockType.QUARANTINE, 3, new DateTime("2016-03-21T12:34:56+03:00"));
    }
}
