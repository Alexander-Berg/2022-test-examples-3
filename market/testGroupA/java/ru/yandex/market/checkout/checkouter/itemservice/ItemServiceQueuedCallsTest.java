package ru.yandex.market.checkout.checkouter.itemservice;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceFetcher;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCObjectType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ITEM_SERVICE_DATE_CHANGE_NOTIFICATION;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ITEM_SERVICE_REGISTRATION;

public class ItemServiceQueuedCallsTest extends AbstractWebTestBase {

    private static final DateFormat DATE_FORMAT =
            new SimpleDateFormat(CheckouterDateFormats.DATE_FORMAT);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");


    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private CheckouterClient client;

    @Autowired
    private ItemServiceFetcher itemServiceFetcher;

    @Autowired
    private WireMockServer yaUslugiMock;

    @AfterEach
    public void resetMocks() {
        yaUslugiMock.resetAll();
    }

    @Test
    public void createItemServiceTest() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, false);
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/market_partner_orders/services"))
                        .withRequestBody(containing("\"count\":\"5\""))
                        .willReturn(okJson("{}")));

        LocalTime fromTime = LocalTime.of(12, 0);
        LocalTime toTime = LocalTime.of(13, 0);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().getItems().iterator().next().setCount(5); // специально для теста (реально 2 элемента)
        parameters.addItemService();
        parameters.getReportParameters().getActualDelivery()
                .getResults().get(0)
                .getDelivery().get(0)
                .setTimeIntervals(Collections.singletonList(
                        new DeliveryTimeInterval(fromTime, toTime)));
        String reqId = "someReqId";
        Order order = orderCreateHelper.createOrder(parameters, multiCart ->
                multiCart.getCarts()
                        .get(0)
                        .getItemServices()
                        .iterator()
                        .next()
                        .setYaUslugiTimeslotReqId(reqId)
        );

        Collection<Long> serviceIds = extractServiceIds(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds);
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(it -> it.getCallType() == ITEM_SERVICE_REGISTRATION));

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        assertTrue(queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds).isEmpty());


        List<LoggedRequest> createRequests = yaUslugiMock.findRequestsMatching(
                 postRequestedFor(
                        urlEqualTo("/ydo/api/market_partner_orders/services")).build())
                .getRequests();

        assertEquals(1, createRequests.size());

        var createBody = createRequests.get(0).getBodyAsString();
        DeliveryDates expectedDates = order.getDelivery().getDeliveryDates();
        JsonTest.checkJsonMatcher(createBody, "$.order.item.title",
                equalTo(order.getItems().iterator().next().getOfferName()));
        JsonTest.checkJsonMatcher(createBody, "$.order.delivery.dates.fromDate",
                equalTo(DATE_FORMAT.format(expectedDates.getFromDate())));
        JsonTest.checkJsonMatcher(createBody, "$.order.delivery.dates.toDate",
                equalTo(DATE_FORMAT.format(expectedDates.getToDate())));
        JsonTest.checkJsonMatcher(createBody, "$.order.delivery.dates.fromTime",
                equalTo(expectedDates.getFromTime().format(TIME_FORMAT)));
        JsonTest.checkJsonMatcher(createBody, "$.order.delivery.dates.toTime",
                equalTo(expectedDates.getToTime().format(TIME_FORMAT)));
        JsonTest.checkJsonMatcher(createBody, "$.yaUslugiTimeslotReqId",
                equalTo(reqId));
    }

    @Test
    public void createItemServiceTestWithSingleServicePerItems() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, true);
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/market_partner_orders/services"))
                        .withRequestBody(containing("\"count\":\"1\""))
                        .willReturn(okJson("{}")));

        LocalTime fromTime = LocalTime.of(12, 0);
        LocalTime toTime = LocalTime.of(13, 0);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().getItems().iterator().next().setCount(5); // специально для теста (реально 2 элемента)
        parameters.addItemService();
        parameters.getReportParameters().getActualDelivery()
                .getResults().get(0)
                .getDelivery().get(0)
                .setTimeIntervals(Collections.singletonList(
                        new DeliveryTimeInterval(fromTime, toTime)));
        Order order = orderCreateHelper.createOrder(parameters);

        Collection<Long> serviceIds = extractServiceIds(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds);
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(it -> it.getCallType() == ITEM_SERVICE_REGISTRATION));

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        assertTrue(queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds).isEmpty());


        List<LoggedRequest> createRequests = yaUslugiMock.findRequestsMatching(
                 postRequestedFor(
                        urlEqualTo("/ydo/api/market_partner_orders/services")).build())
                .getRequests();

        assertEquals(1, createRequests.size());

        var createBody = createRequests.get(0).getBodyAsString();
        DeliveryDates expectedDates = order.getDelivery().getDeliveryDates();
        JsonTest.checkJsonMatcher(createBody, "$.order.item.title",
                equalTo(order.getItems().iterator().next().getOfferName()));
        JsonTest.checkJsonMatcher(createBody, "$.order.delivery.dates.fromDate",
                equalTo(DATE_FORMAT.format(expectedDates.getFromDate())));
        JsonTest.checkJsonMatcher(createBody, "$.order.delivery.dates.toDate",
                equalTo(DATE_FORMAT.format(expectedDates.getToDate())));
        JsonTest.checkJsonMatcher(createBody, "$.order.delivery.dates.fromTime",
                equalTo(expectedDates.getFromTime().format(TIME_FORMAT)));
        JsonTest.checkJsonMatcher(createBody, "$.order.delivery.dates.toTime",
                equalTo(expectedDates.getToTime().format(TIME_FORMAT)));
    }

    @Test
    public void createItemServiceWithPersonalInformationTest() {
        String createItemUrl = "/ydo/api/market_partner_orders/services";

        yaUslugiMock.stubFor(
                post(urlPathEqualTo(createItemUrl))
                        .withRequestBody(containing("\"count\":\"1\""))
                        .willReturn(okJson("{}")));

        personalMockConfigurer.mockV1MultiTypesRetrieve();

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();

        Order order = orderCreateHelper.createOrder(parameters);

        Collection<Long> serviceIds = extractServiceIds(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds);
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(it -> it.getCallType() == ITEM_SERVICE_REGISTRATION));

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        assertTrue(queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds).isEmpty());

        List<LoggedRequest> changeRequests = yaUslugiMock.findRequestsMatching(
                postRequestedFor(urlEqualTo(createItemUrl)).build()).getRequests();

        assertEquals(1, changeRequests.size());

        var changeBody = changeRequests.get(0).getBodyAsString();
        JsonTest.checkJsonMatcher(changeBody, "$.client.firstName", equalTo("Leo"));
        JsonTest.checkJsonMatcher(changeBody, "$.client.lastName", equalTo("Tolstoy"));
    }

    @Test
    public void changeDeliveryDateTest() {
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/market_partner_orders/services"))
                        .withRequestBody(containing("\"count\":\"5\""))
                        .willReturn(okJson("{}")));

        yaUslugiMock.stubFor(
                put(urlPathMatching("/ydo/api/market_partner_orders/services/.*")));

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().getItems().iterator().next().setCount(5); // специально для теста (реально 2 элемента)
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        DeliveryDates deliveryDates = order.getDelivery().getDeliveryDates();
        Date prevDate = deliveryDates.getToDate();
        Date futureDate = addTwoDays(prevDate);

        // меняем дату доставки
        deliveryDates.setToDate(futureDate);
        deliveryDates.setFromTime(LocalTime.of(12, 0));
        deliveryDates.setToTime(LocalTime.of(13, 0));

        // регестрируем нотификатор (без вызова)
        client.updateOrderDelivery(order.getId(), SYSTEM, -1L, order.getDelivery());

        Long serviceId = extractServiceIds(order).iterator().next();
        ItemService itemService = itemServiceFetcher.fetchById(serviceId);

        assertEquals(1, queuedCallService.findQueuedCalls(
                ITEM_SERVICE_DATE_CHANGE_NOTIFICATION,
                serviceId).size());

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_DATE_CHANGE_NOTIFICATION);

        assertTrue(queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceId).stream()
                .noneMatch(it -> it.getCallType() == ITEM_SERVICE_DATE_CHANGE_NOTIFICATION));

        assertNotEquals(futureDate, itemService.getDate());
        assertEquals(prevDate, itemService.getDate());

        var expectedChangeUrl = "/ydo/api/market_partner_orders/services/" + itemService.getId();

        List<LoggedRequest> changeRequests = yaUslugiMock.findRequestsMatching(
                putRequestedFor(urlEqualTo(expectedChangeUrl)).build()).getRequests();

        assertEquals(1, changeRequests.size());
        var changeBody = changeRequests.get(0).getBodyAsString();
        JsonTest.checkJsonMatcher(changeBody, "$.order.delivery.dates.fromDate",
                equalTo(DATE_FORMAT.format(deliveryDates.getFromDate())));
        JsonTest.checkJsonMatcher(changeBody, "$.order.delivery.dates.toDate",
                equalTo(DATE_FORMAT.format(deliveryDates.getToDate())));
        JsonTest.checkJsonMatcher(changeBody, "$.order.delivery.dates.fromTime",
                equalTo(deliveryDates.getFromTime().format(TIME_FORMAT)));
        JsonTest.checkJsonMatcher(changeBody, "$.order.delivery.dates.toTime",
                equalTo(deliveryDates.getToTime().format(TIME_FORMAT)));
    }

    private Date addTwoDays(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 2);
        return c.getTime();
    }

    private Collection<Long> extractServiceIds(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getServices)
                .flatMap(Collection::stream)
                .map(ItemService::getId)
                .collect(Collectors.toSet());
    }
}
