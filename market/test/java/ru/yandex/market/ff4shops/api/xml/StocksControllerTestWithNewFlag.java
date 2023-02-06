package ru.yandex.market.ff4shops.api.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStockInfo;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.ExpectedCount;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.AbstractJsonControllerFunctionalTest;
import ru.yandex.market.ff4shops.delivery.stocks.DatacampMessageLogbrokerEvent;
import ru.yandex.market.ff4shops.environment.EnvironmentService;
import ru.yandex.market.ff4shops.partner.service.StocksByPiExperiment;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FfAsserts;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link StocksController}.
 *
 *  - При выпиливании флага STOCKS_BY_NEW_FLAG_VAR удалить тест StocksControllerTest и переименовать
 *  этот тест в StocksControllerTest
 *  - С csv-файлами поступить аналогично
 * @author churlyaev-p
 */
@DbUnitDataSet(before = "StocksControllerTestWithNewFlag.csv")
class StocksControllerTestWithNewFlag extends AbstractJsonControllerFunctionalTest {


    @Autowired
    private DataCampClient dataCampShopClient;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private StocksByPiExperiment stocksByPiExperiment;

    @Autowired
    @Qualifier("logbrokerPartnerApiStockEventPublisher")
    private LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerPartnerApiStockEventPublisher;

    @BeforeEach
    public void init() {
        stocksByPiExperiment.resetCachingVariables();
        environmentService.setValue(StocksByPiExperiment.STOCKS_BY_NEW_FLAG_VAR, "true");
    }

    @Test
    @DisplayName("Запрос по отсутствующим/архивным оферам поставщика, работающего по АПИ (ходит в push API)")
    void requestByArchivedItems() {
        pushApiMockRestServiceServer.expect(ExpectedCount.never(), anything());
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_1</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <unitIds>\n" +
                        "            <unitId>\n" +
                        "                <id>archived</id>\n" +
                        "                <vendorId>100</vendorId>\n" +
                        "                <article>archived</article>\n" +
                        "            </unitId>\n" +
                        "        </unitIds>\n" +
                        "    </request>\n" +
                        "</root>";
        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <requestState>\n" +
                        "        <isError>false</isError>\n" +
                        "    </requestState>\n" +
                        "    <response type=\"getStocks\">\n" +
                        "        <itemStocksList>\n" +
                        "            <itemStocks>\n" +
                        "                <unitId>\n" +
                        "                    <id>archived</id>\n" +
                        "                    <vendorId>100</vendorId>\n" +
                        "                    <article>archived</article>\n" +
                        "                </unitId>\n" +
                        "                <warehouseId/>\n" +
                        "                <stocks>\n" +
                        "                    <stock>\n" +
                        "                        <type>10</type>\n" +
                        "                        <count>0</count>\n" +
                        "                        <updated>2018-12-11T17:44:08+03:00</updated>\n" +
                        "                    </stock>\n" +
                        "                </stocks>\n" +
                        "            </itemStocks>\n" +
                        "      </itemStocksList>\n" +
                        "    </response>\n" +
                        "</root>";

        ResponseEntity<String> response = getStocks(request, 10, false);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody(), Collections.singleton("updated"));
    }

    @Test
    @DbUnitDataSet(after = "StocksControllerTest.status.empty.after.csv")
    @DisplayName("Запрос по определенным оферам поставщика, работающего по АПИ (ходит в push API)")
    void requestByItems() {
        expectRequestToPushApi(100, 95);
        expectRequestToPushApi(101, 96);
        requestByItems(100, 95, 10, false);
        requestByItems(101, 96, 12, true);
        verifyNoInteractions(logbrokerPartnerApiStockEventPublisher);
    }

    @Test
    @DbUnitDataSet(
            before = "StocksControllerTest.status.lb.enabled.csv",
            after = "StocksControllerTest.status.empty.after.csv")
    @DisplayName("Запрос по определенным оферам поставщика, работающего по АПИ (ходит в push API) с отправкой в ЛБ")
    void requestByItemsWithLb() {
        expectRequestToPushApi(100, 95);
        expectRequestToPushApi(101, 96);
        requestByItems(100, 95, 10, false);
        requestByItems(101, 96, 12, true);
        verify(logbrokerPartnerApiStockEventPublisher, times(4)).publishEventAsync(any());
    }

    void requestByItems(long partnerId1, long partnerId2, long serviceId, boolean isWhite) {
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_1</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <unitIds>\n" +
                        "            <unitId>\n" +
                        "                <id>AAA</id>\n" +
                        "                <vendorId>" + partnerId1 + "</vendorId>\n" +
                        "                <article>AAA</article>\n" +
                        "            </unitId>\n" +
                        "            <unitId>\n" +
                        "                <id>CCC</id>\n" +
                        "                <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "                <article>CCC</article>\n" +
                        "            </unitId>\n" +
                        "            <unitId>\n" +
                        "                <id>ZZZ</id>\n" +
                        "                <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "                <article>ZZZ</article>\n" +
                        "            </unitId>\n" +
                        "        </unitIds>\n" +
                        "    </request>\n" +
                        "</root>";
        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <requestState>\n" +
                        "        <isError>false</isError>\n" +
                        "    </requestState>\n" +
                        "    <response type=\"getStocks\">\n" +
                        "        <itemStocksList>\n" +
                        "            <itemStocks>\n" +
                        "                <unitId>\n" +
                        "                    <id>AAA</id>\n" +
                        "                    <vendorId>" + partnerId1 + "</vendorId>\n" +
                        "                    <article>AAA</article>\n" +
                        "                </unitId>\n" +
                        "                <warehouseId/>\n" +
                        "                <stocks>\n" +
                        "                    <stock>\n" +
                        "                        <type>10</type>\n" +
                        "                        <count>3</count>\n" +
                        "                        <updated>2018-12-11T17:44:08+03:00</updated>\n" +
                        "                    </stock>\n" +
                        "                </stocks>\n" +
                        "            </itemStocks>\n" +
                        "            <itemStocks>\n" +
                        "                <unitId>\n" +
                        "                    <id>CCC</id>\n" +
                        "                    <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "                    <article>CCC</article>\n" +
                        "                </unitId>\n" +
                        "                <warehouseId/>\n" +
                        "                <stocks>\n" +
                        "                    <stock>\n" +
                        "                        <type>10</type>\n" +
                        "                        <count>3</count>\n" +
                        "                        <updated>2018-12-11T17:44:08+03:00</updated>\n" +
                        "                    </stock>\n" +
                        "                </stocks>\n" +
                        "            </itemStocks>\n" +
                        "            <itemStocks>\n" +
                        "                <unitId>\n" +
                        "                    <id>ZZZ</id>\n" +
                        "                    <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "                    <article>ZZZ</article>\n" +
                        "                </unitId>\n" +
                        "                <warehouseId/>\n" +
                        "                <stocks>\n" +
                        "                    <stock>\n" +
                        "                        <type>10</type>\n" +
                        "                        <count>0</count>\n" +
                        "                        <updated>2020-02-11T16:28:46+03:00</updated>\n" +
                        "                    </stock>\n" +
                        "                </stocks>\n" +
                        "            </itemStocks>\n" +
                        "      </itemStocksList>\n" +
                        "    </response>\n" +
                        "</root>";

        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody(), Collections.singleton("updated"));
    }

    @Test
    @DbUnitDataSet(after = "StocksControllerTest.status.empty.after.csv")
    @DisplayName("Запрос по определенным оферам поставщика, работающего по АПИ (ходит в push API)")
    void requestByItemsWithErrorInPushApi() {
        errorToPushApi(100, 95);
        errorToPushApi(101, 96);
        requestByItemsWithErrorInPushApi(100, 95, 10, false);
        requestByItemsWithErrorInPushApi(101, 96, 12, true);
    }

    void requestByItemsWithErrorInPushApi(long partnerId1, long partnerId2, long serviceId, boolean isWhite) {
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_1</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <unitIds>\n" +
                        "            <unitId>\n" +
                        "                <id>AAA</id>\n" +
                        "                <vendorId>" + partnerId1 + "</vendorId>\n" +
                        "                <article>AAA</article>\n" +
                        "            </unitId>\n" +
                        "            <unitId>\n" +
                        "                <id>CCC</id>\n" +
                        "                <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "                <article>CCC</article>\n" +
                        "            </unitId>\n" +
                        "        </unitIds>\n" +
                        "    </request>\n" +
                        "</root>";
        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "   <requestState>\n" +
                        "      <isError>false</isError>\n" +
                        "   </requestState>\n" +
                        "   <response type=\"getStocks\">\n" +
                        "      <itemStocksList>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>AAA</id>\n" +
                        "               <vendorId>" + partnerId1 + "</vendorId>\n" +
                        "               <article>AAA</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId />\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>0</count>\n" +
                        "                  <updated>1970-01-01T03:00:00+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>CCC</id>\n" +
                        "               <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "               <article>CCC</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId />\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>0</count>\n" +
                        "                  <updated>1970-01-01T03:00:00+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "      </itemStocksList>\n" +
                        "   </response>\n" +
                        "</root>";

        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody(), Collections.singleton("updated"));
    }

    private void errorToPushApi(long partnerId1, long partnerId2) {
        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId1 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ERROR", MediaType.APPLICATION_XML));

        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId2 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ERROR", MediaType.APPLICATION_XML));
    }

    private void expectRequestToPushApi(long partnerId1, long partnerId2) {
        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId1 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess( //language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"AAA\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));

        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId2 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(//language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"CCC\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));
    }

    @Test
    @DbUnitDataSet(after = "StocksControllerTest.api.status.after.csv")
    @DisplayName("Тест offset запроса по limit и offset поставщика, работающего по АПИ (ходит в push API)")
    void requestByLimit() {
        expectRequestToPushApiByLimit(100, 95);
        expectRequestToPushApiByLimit(101, 96);
        requestByLimit(100, 95, 10, false);
        requestByLimit(101, 96, 12, true);
    }

    void requestByLimit(long partnerId1, long partnerId2, long serviceId, boolean isWhite) {
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_1</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <limit>2</limit>\n" +
                        "        <offset>1</offset>\n" +
                        "    </request>\n" +
                        "</root>";
        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "   <requestState>\n" +
                        "      <isError>false</isError>\n" +
                        "   </requestState>\n" +
                        "   <response type=\"getStocks\">\n" +
                        "      <itemStocksList>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>AAA</id>\n" +
                        "               <vendorId>" + partnerId1 + "</vendorId>\n" +
                        "               <article>AAA</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId />\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>3</count>\n" +
                        "                  <updated>2018-12-11T17:44:08+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>DDD</id>\n" +
                        "               <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "               <article>DDD</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId />\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>3</count>\n" +
                        "                  <updated>2018-12-11T17:44:08+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "      </itemStocksList>\n" +
                        "   </response>\n" +
                        "</root>";

        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody(), Collections.singleton("updated"));
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Тест offset заапроса по limit и offset поставщика, работающего по АПИ (ходит в push API)," +
            "push api не вернул stock item")
    void requestByLimitPushApiNotItem() {
        expectPushApiNoItems(100, 95);
        expectPushApiNoItems(101, 96);
        requestByLimitPushApiNotItem(100, 95, 10, false);
        requestByLimitPushApiNotItem(101, 96, 12, true);
    }

    void requestByLimitPushApiNotItem(long partnerId1, long partnerId2, long serviceId, boolean isWhite) {
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_1</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <limit>2</limit>\n" +
                        "        <offset>1</offset>\n" +
                        "    </request>\n" +
                        "</root>";
        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <requestState>\n" +
                        "        <isError>false</isError>\n" +
                        "    </requestState>\n" +
                        "    <response type=\"getStocks\">\n" +
                        "        <itemStocksList>\n" +
                        "            <itemStocks>\n" +
                        "                <unitId>\n" +
                        "                    <id>AAA</id>\n" +
                        "                    <vendorId>" + partnerId1 + "</vendorId>\n" +
                        "                    <article>AAA</article>\n" +
                        "                </unitId>\n" +
                        "                <warehouseId/>\n" +
                        "                <stocks>\n" +
                        "                    <stock>\n" +
                        "                        <type>10</type>\n" +
                        "                        <count>0</count>\n" +
                        "                        <updated>2019-12-20T16:04:52+03:00</updated>\n" +
                        "                    </stock>\n" +
                        "                </stocks>\n" +
                        "            </itemStocks>\n" +
                        "            <itemStocks>\n" +
                        "                <unitId>\n" +
                        "                    <id>DDD</id>\n" +
                        "                    <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "                    <article>DDD</article>\n" +
                        "                </unitId>\n" +
                        "                <warehouseId/>\n" +
                        "                <stocks>\n" +
                        "                    <stock>\n" +
                        "                        <type>10</type>\n" +
                        "                        <count>0</count>\n" +
                        "                        <updated>2019-12-20T16:04:52+03:00</updated>\n" +
                        "                    </stock>\n" +
                        "                </stocks>\n" +
                        "            </itemStocks>\n" +
                        "        </itemStocksList>\n" +
                        "    </response>\n" +
                        "</root>";

        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody(), Collections.singleton("updated"));
    }

    private void expectPushApiNoItems(long partnerId1, long partnerId2) {
        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId1 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(//language=xml
                        "<stocksResponse>" +
                                "  <stocks>" +
                                "    <stock sku=\"AAA\" warehouseId=\"1\">" +
                                "       <items></items>" +
                                "    </stock>" +
                                "  </stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));

        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId2 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(//language=xml
                        "" +
                                "<stocksResponse>" +
                                "  <stocks>" +
                                "    <stock sku=\"DDD\" warehouseId=\"1\">" +
                                "      <items>null</items>" +
                                "    </stock>" +
                                "  </stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));
    }

    @Test
    @DisplayName("Тест запроса по limit и offset поставщика, работающего по АПИ (ходит в push API)")
    void requestByLimitWithIncompleteResponse() {
        expectRequestToPushApiByLimitWithIncompleteResponse(95);
        expectRequestToPushApiByLimitWithIncompleteResponse(96);
        requestByLimitWithIncompleteResponse(95, 10, false);
        requestByLimitWithIncompleteResponse(96, 12, true);
    }

    void requestByLimitWithIncompleteResponse(long partnerId, long serviceId, boolean isWhite) {
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_1</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <limit>2</limit>\n" +
                        "        <offset>0</offset>\n" +
                        "    </request>\n" +
                        "</root>";

        String expectedResponse =  //language=xml
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "   <requestState>\n" +
                        "      <isError>false</isError>\n" +
                        "   </requestState>\n" +
                        "   <response type=\"getStocks\">\n" +
                        "      <itemStocksList>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>CCC</id>\n" +
                        "               <vendorId>" + partnerId + "</vendorId>\n" +
                        "               <article>CCC</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId />\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>0</count>\n" +
                        "                  <updated>2018-12-11T17:44:08+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>DDD</id>\n" +
                        "               <vendorId>" + partnerId + "</vendorId>\n" +
                        "               <article>DDD</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId />\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>3</count>\n" +
                        "                  <updated>2018-12-11T17:44:08+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "      </itemStocksList>\n" +
                        "   </response>\n" +
                        "</root>";
        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody(), Collections.singleton("updated"));
    }

    private void expectRequestToPushApiByLimit(long partnerId1, long partnerId2) {
        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId1 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(//language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"AAA\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));

        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId2 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(//language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"DDD\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));

    }

    private void expectRequestToPushApiByLimitWithIncompleteResponse(long partnerId) {
        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(//language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"DDD\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));

    }

    @Test
    @DbUnitDataSet(after = "StocksControllerTest.status.empty.after.csv")
    @DisplayName("Запрос по определенным оферам поставщиков, работающих через парнтнерский интерфейс (ходит в " +
            "datacamp-stroller API)")
    void requestByItemsIndexer() {
        when(dataCampShopClient.getBusinessOfferStocks(34, 1000, 11, List.of("FFF")))
                .thenReturn(List.of(
                        getDatacampStockInfo(34, 1000, 11, 600, 10000000000L, "FFF", 1562716800000L)
                ));
        when(dataCampShopClient.getBusinessOfferStocks(45, 1001, 11, List.of("RRR")))
                .thenReturn(List.of(
                        getDatacampStockInfo(45, 1001, 11, 700, 5, "RRR", 1562716800000L)
                ));
        when(dataCampShopClient.getBusinessOfferStocks(34, 1003, 13, List.of("FFF")))
                .thenReturn(List.of(
                        getDatacampStockInfo(34, 1003, 13, 600, 10000000000L, "FFF", 1562716800000L)
                ));
        when(dataCampShopClient.getBusinessOfferStocks(45, 1004, 13, List.of("RRR")))
                .thenReturn(List.of(
                        getDatacampStockInfo(45, 1004, 13, 700, 5, "RRR", 1562716800000L)
                ));

        expectPushApiSingleItem(1002);
        expectPushApiSingleItem(1005);

        requestByItemsIndexer(1001, 1000, 1002, 11, false);
        requestByItemsIndexer(1004, 1003, 1005, 13, true);
    }

    void requestByItemsIndexer(long partnerId1, long partnerId2, long partnerId3, long serviceId, boolean isWhite) {
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_2</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <unitIds>\n" +
                        "            <unitId>\n" +
                        "                <id>RRR</id>\n" +
                        "                <vendorId>" + partnerId1 + "</vendorId>\n" +
                        "                <article>RRR</article>\n" +
                        "            </unitId>\n" +
                        "            <unitId>\n" +
                        "                <id>FFF</id>\n" +
                        "                <vendorId>" + partnerId2 + "</vendorId>\n" +
                        "                <article>FFF</article>\n" +
                        "            </unitId>\n" +
                        "            <unitId>\n" +
                        "                <id>HHH</id>\n" +
                        "                <vendorId>" + partnerId3 + "</vendorId>\n" +
                        "                <article>HHH</article>\n" +
                        "            </unitId>\n" +
                        "        </unitIds>\n" +
                        "    </request>\n" +
                        "</root>";

        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>" +
                        "  <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>" +
                        "  <hash>36fc8f6373206300cd2d3350611cc50c</hash>" +
                        "  <requestState><isError>false</isError></requestState>" +
                        "  <response type=\"getStocks\">" +
                        "    <itemStocksList>" +
                        "      <itemStocks>" +
                        "        <unitId><id>FFF</id><vendorId>" + partnerId2 + "</vendorId><article>FFF</article" +
                        "></unitId>" +
                        "        <warehouseId/>" +
                        "        <stocks>" +
                        "          <stock><type>10</type><count>2147483647</count><updated>2019-07-10T03:00:00+03:00" +
                        "</updated" +
                        "></stock>" +
                        "        </stocks>" +
                        "      </itemStocks>" +
                        "      <itemStocks>" +
                        "        <unitId><id>HHH</id><vendorId>" + partnerId3 + "</vendorId><article>HHH</article" +
                        "></unitId>" +
                        "        <warehouseId/>" +
                        "        <stocks>" +
                        "          <stock><type>10</type><count>3</count><updated>2018-12-11T17:44:08+03:00</updated" +
                        "></stock>" +
                        "        </stocks>" +
                        "      </itemStocks>" +
                        "      <itemStocks>" +
                        "        <unitId><id>RRR</id><vendorId>" + partnerId1 + "</vendorId><article>RRR</article" +
                        "></unitId>" +
                        "        <warehouseId/>" +
                        "        <stocks>" +
                        "          <stock><type>10</type><count>5</count><updated>2019-07-10T03:00:00+03:00</updated" +
                        "></stock>" +
                        "        </stocks>" +
                        "      </itemStocks>" +
                        "    </itemStocksList>" +
                        "  </response>" +
                        "</root>";
        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody());
    }

    private void expectPushApiSingleItem(long partnerId3) {
        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/shops/" + partnerId3 + "/stocks?context=MARKET&apiSettings" +
                                        "=PRODUCTION&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess( //language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"HHH\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));
    }


    @Test
    @DbUnitDataSet(after = "StocksControllerTest.status.after.csv")
    @DisplayName("Тест запроса limit поставщика, работающего через парнтнерский интерфейс (ходит в datacamp-stroller " +
            "API)")
    void requestByLimitIndexer() {
        when(dataCampShopClient.getBusinessOfferStocks(34, 1000, 11, List.of("FFF")))
                .thenReturn(List.of(
                        getDatacampStockInfo(34, 1000, 11, 600, 12, "FFF", 1562716800000L)
                ));
        when(dataCampShopClient.getBusinessOfferStocks(34, 1003, 13, List.of("FFF")))
                .thenReturn(List.of(
                        getDatacampStockInfo(34, 1003, 13, 600, 12, "FFF", 1562716800000L)
                ));

        requestByLimitIndexer(1000L, 11, false);
        requestByLimitIndexer(1003L, 13, true);
    }

    void requestByLimitIndexer(long partnerId, long serviceId, boolean isWhite) {
        String request =  //language=xml
                "<root>\n" +
                        "    <token>ff_token_2</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <limit>1</limit>\n" +
                        "    </request>\n" +
                        "</root>";


        String expectedResponse =  //language=xml
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "   <requestState>\n" +
                        "      <isError>false</isError>\n" +
                        "   </requestState>\n" +
                        "   <response type=\"getStocks\">\n" +
                        "      <itemStocksList>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>FFF</id>\n" +
                        "             <vendorId>" + partnerId + "</vendorId>\n" +
                        "               <article>FFF</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId/>\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>12</count>\n" +
                        "                  <updated>2019-07-10T03:00:00+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "      </itemStocksList>\n" +
                        "   </response>\n" +
                        "</root>";
        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody());
    }

    @Test
    @DbUnitDataSet(after = "StocksControllerTest.status.after.csv")
    @DisplayName("Тест запроса offset поставщика, работающего через партнерский интерфейс (ходит в datacamp-stroller " +
            "API)")
    void requestByOffsetIndexer() {
        when(dataCampShopClient.getBusinessOfferStocks(34, 1000, 11, List.of("GGG", "SSS")))
                .thenReturn(List.of(
                        getDatacampStockInfo(34, 1000, 11, 600, 12, "GGG", 1562716800000L),
                        getDatacampStockInfo(34, 1000, 11, 600, 20, "SSS", 1592331902000L)
                ));
        when(dataCampShopClient.getBusinessOfferStocks(34, 1003, 13, List.of("GGG", "SSS")))
                .thenReturn(List.of(
                        getDatacampStockInfo(34, 1003, 13, 600, 12, "GGG", 1562716800000L),
                        getDatacampStockInfo(34, 1003, 13, 600, 20, "SSS", 1592331902000L)
                ));

        requestByOffsetIndexer(1000, 11, false);
        requestByOffsetIndexer(1003, 13, true);
    }

    @Test
    @DbUnitDataSet()
    @DisplayName("Возвращаем нулевые остатки если запрос пришел не на главный склад группы")
    void zeroStocksWithNonMainWarehouse() {
        var partnerId = 2101L;
        var warehouseId = 16L;
        var shopSku1 = "GGG2";
        var shopSku2 = "GGG3";
        requestByOffsetIndexer(partnerId, warehouseId, false, List.of(
                new ItemStocks(
                        new UnitId(shopSku1, partnerId, shopSku1),
                        null,
                        List.of(new Stock(StockType.FIT, 0, new DateTime("2019-07-10T03:00:00+03:00")))),
                new ItemStocks(
                        new UnitId(shopSku2, partnerId, shopSku2),
                        null,
                        List.of(new Stock(StockType.FIT, 0, new DateTime("2019-07-10T03:00:00+03:00"))))
                ), true);
    }

    void requestByOffsetIndexer(long partnerId, long serviceId, boolean isWhite) {
        requestByOffsetIndexer(partnerId, serviceId, isWhite, List.of(
                new ItemStocks(
                        new UnitId("GGG", partnerId, "GGG"),
                        null,
                        List.of(new Stock(StockType.FIT, 12, new DateTime("2019-07-10T03:00:00+03:00")))),
                new ItemStocks(
                        new UnitId("SSS", partnerId, "SSS"),
                        null,
                        List.of(new Stock(StockType.FIT, 20, new DateTime("2020-06-16T21:25:02+03:00"))))
                ), false);
    }

    void requestByOffsetIndexer(long partnerId, long serviceId, boolean isWhite, Collection<ItemStocks> skuCount,
                                boolean ignoreDateTime) {
        String request =  //language=xml
                "<root>\n" +
                        "    <token>ff_token_2</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <limit>2</limit>\n" +
                        "        <offset>1</offset>\n" +
                        "    </request>\n" +
                        "</root>";

        String items = skuCount.stream().map(sku ->
                "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>" + sku.getUnitId().getId() + "</id>\n" +
                        "               <vendorId>" + partnerId + "</vendorId>\n" +
                        "               <article>" + sku.getUnitId().getId() + "</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId/>\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>" + sku.getStocks().stream()
                                            .findFirst().map(Stock::getCount).orElse(-1) + "</count>\n" +
                        "                  <updated>" + sku.getStocks().stream()
                                            .findFirst().map(Stock::getUpdated).orElse(null).getFormattedDate()
                                            + "</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n").collect(Collectors.joining());

        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "   <requestState>\n" +
                        "      <isError>false</isError>\n" +
                        "   </requestState>\n" +
                        "   <response type=\"getStocks\">\n" +
                        "      <itemStocksList>\n" +
                        items +
                        "      </itemStocksList>\n" +
                        "   </response>\n" +
                        "</root>";

        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        var ignoreNode = new HashSet<String>();
        if (ignoreDateTime) {
            ignoreNode.add("updated");
        }
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody(), ignoreNode);
    }

    @Test
    @DisplayName("Запрос по всем оферам поставщиков, работающего через партнерский интерфейс (ходит в " +
            "datacamp-stroller API)")
    @DbUnitDataSet(after = "StocksControllerTest.status.after.csv")
    void requestByLimitWithMissingItems() {
        when(dataCampShopClient.getBusinessOfferStocks(34, 1000, 11, List.of("FFF", "GGG", "SSS", "TESTTEST")))
                .thenReturn(List.of(
                        getDatacampStockInfo(34, 1000, 11, 600, 12, "FFF", 1562716800000L),
                        getDatacampStockInfo(34, 1000, 11, 600, 20, "SSS", 1592331902000L),
                        getDatacampStockInfo(34, 1000, 11, 600, 25, "TESTTEST", 1592331902000L)
                ));
        when(dataCampShopClient.getBusinessOfferStocks(34, 1003, 13, List.of("FFF", "GGG", "SSS", "TESTTEST")))
                .thenReturn(List.of(
                        getDatacampStockInfo(34, 1003, 13, 600, 12, "FFF", 1562716800000L),
                        getDatacampStockInfo(34, 1003, 13, 600, 20, "SSS", 1592331902000L),
                        getDatacampStockInfo(34, 1003, 13, 600, 25, "TESTTEST", 1592331902000L)
                ));

        requestByLimitWithMissingItems(1000, 11, false);
        requestByLimitWithMissingItems(1003, 13, true);
    }

    void requestByLimitWithMissingItems(long partnerId, long serviceId, boolean isWhite) {
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_2</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <limit>4</limit>\n" +
                        "        <offset>0</offset>\n" +
                        "    </request>\n" +
                        "</root>";


        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root>\n" +
                        "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "   <requestState>\n" +
                        "      <isError>false</isError>\n" +
                        "   </requestState>\n" +
                        "   <response type=\"getStocks\">\n" +
                        "      <itemStocksList>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>FFF</id>\n" +
                        "               <vendorId>" + partnerId + "</vendorId>\n" +
                        "               <article>FFF</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId/>\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>12</count>\n" +
                        "                  <updated>2019-07-10T03:00:00+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>GGG</id>\n" +
                        "               <vendorId>" + partnerId + "</vendorId>\n" +
                        "               <article>GGG</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId/>\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>0</count>\n" +
                        "                  <updated>1970-01-01T03:00:00+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>SSS</id>\n" +
                        "               <vendorId>" + partnerId + "</vendorId>\n" +
                        "               <article>SSS</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId/>\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>20</count>\n" +
                        "                  <updated>2020-06-16T21:25:02+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "         <itemStocks>\n" +
                        "            <unitId>\n" +
                        "               <id>TESTTEST</id>\n" +
                        "               <vendorId>" + partnerId + "</vendorId>\n" +
                        "               <article>TESTTEST</article>\n" +
                        "            </unitId>\n" +
                        "            <warehouseId/>\n" +
                        "            <stocks>\n" +
                        "               <stock>\n" +
                        "                  <type>10</type>\n" +
                        "                  <count>25</count>\n" +
                        "                  <updated>2020-06-16T21:25:02+03:00</updated>\n" +
                        "               </stock>\n" +
                        "            </stocks>\n" +
                        "         </itemStocks>\n" +
                        "      </itemStocksList>\n" +
                        "   </response>\n" +
                        "</root>";
        ResponseEntity<String> response = getStocks(request, serviceId, isWhite);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody());
    }

    private ResponseEntity<String> getStocks(String request, long serviceId, boolean isWhite) {
        String stocksUrl = FF4ShopsUrlBuilder.getStocksUrl(randomServerPort, serviceId, isWhite);
        return FunctionalTestHelper.postForXml(stocksUrl, request);
    }

    @Nonnull
    private DataCampOffer.Offer getDatacampStockInfo(int businessId, int partnerId, int warehouseId, int feedId,
                                                     long count, String offerId, long timestamp) {
        return DataCampOffer.Offer.newBuilder()
                .setStockInfo(
                        DataCampOfferStockInfo.OfferStockInfo.newBuilder()
                                .setPartnerStocks(
                                        DataCampOfferStockInfo.OfferStocks.newBuilder()
                                                .setCount(count)
                                                .setMeta(
                                                        DataCampOfferMeta.UpdateMeta.newBuilder()
                                                                .setTimestamp(
                                                                        Timestamp.newBuilder()
                                                                                .setSeconds(timestamp / 1000)
                                                                                .setNanos((int) (timestamp % 1000) * 1000000)
                                                                                .build()
                                                                )
                                                )
                                )
                )
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId(offerId)
                                .setBusinessId(businessId)
                                .setShopId(partnerId)
                                .setWarehouseId(warehouseId)
                                .setFeedId(feedId)
                ).build();
    }

    @ParameterizedTest
    @CsvSource(value = {"true", "false"})
    @DisplayName("Проверка ошибки несуществующего склада")
    void testGetStocksNoFfLink(boolean isWhite) {
        String request = //language=xml
                "<root>\n" +
                        "    <token>ff_token_1</token>\n" +
                        "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                        "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                        "    <request type=\"getStocks\">\n" +
                        "        <limit>2</limit>\n" +
                        "        <offset>1</offset>\n" +
                        "    </request>\n" +
                        "</root>";

        String expectedResponse = "<root>\n" +
                "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "    <requestState>\n" +
                "        <isError>true</isError>\n" +
                "        <errorCodes>\n" +
                "            <errorCode>\n" +
                "                <code>9404</code>\n" +
                "                <message>No partners linked to warehouse: 1</message>\n" +
                "                <description/>\n" +
                "            </errorCode>\n" +
                "        </errorCodes>\n" +
                "    </requestState>\n" +
                "    <response type=\"%s\"/>\n" +
                "</root>";

        ResponseEntity<String> response = getStocks(request, 1, isWhite);
        FfAsserts.assertXmlEquals(String.format(expectedResponse, isWhite ? "getWhiteStocks" : "getStocks"),
                response.getBody());
    }
}
