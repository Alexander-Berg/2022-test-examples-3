package ru.yandex.market.checkout.checkouter.pay;


import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.EditPossibilityWrapper;
import ru.yandex.market.checkout.checkouter.order.changerequest.OrderEditPossibility;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryRoute;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.PickupOption;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType.DELIVERY_DATES;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.PROCESS_UNPAID_WAITING_USER_DELIVERY_INPUT;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.POST_PRICE;

public class TinkoffCreditCourierDeliveryTest extends AbstractWebTestBase {

    public static final long ANOTHER_DELIVERY_SERVICE_ID = 777L;
    public static final long DEFAULT_DELIVERY_SERVICE_ID = BlueParametersProvider.DELIVERY_SERVICE_ID;
    public static final String ANOTHER_ROUTE = "{\"anotherRoute\":111,\"this\":\"is\"}";

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    private Order order;
    private Payment payment;

    private void initOrder(boolean combinator, DeliveryType deliveryType) {
        if (combinator) {
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                    Collections.singleton(Constants.COMBINATOR_EXPERIMENT)));
        }
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(DEFAULT_DELIVERY_SERVICE_ID)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addDelivery(actualDelivery(DEFAULT_DELIVERY_SERVICE_ID))
                        .addDelivery(actualDelivery(ANOTHER_DELIVERY_SERVICE_ID))
                        .addPost(createPickupOption(1, DEFAULT_DELIVERY_SERVICE_ID))
                        .addPost(createPickupOption(7, ANOTHER_DELIVERY_SERVICE_ID))
                        .withFreeDelivery()
                        .build())
                .withDeliveryType(deliveryType)
                .withColor(Color.BLUE)
                .withCombinator(combinator)
                .buildParameters();
        parameters.setExperiments(combinator ? Constants.COMBINATOR_EXPERIMENT : null);
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        order = orderCreateHelper.createOrder(parameters);
        payment = orderPayHelper.pay(order.getId());
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket(), null);

        orderPayHelper.notifyPayment(payment);

        reportMock.resetRequests();
    }

    @DisplayName("Должны давать выбрать опции доставки со сменой СД")
    @ParameterizedTest(name = "combinator {0}")
    @ValueSource(booleans = {true, false})
    void deliveryEditOptions(boolean combinator) {
        initOrder(combinator, DeliveryType.DELIVERY);
        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(DELIVERY_DATES));
        OrderEditOptions orderEditOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                order.getBuyer().getUid(), Collections.singletonList(Color.BLUE), request);

        // в actual_delivery запросили интервалы без фильтрации по СД
        ServeEvent actualDeliveryEvent = reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && (req.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery")))
                .findFirst().get();
        Map<String, QueryParameter> actualDeliveryParams = actualDeliveryEvent.getRequest().getQueryParams();
        assertThat(actualDeliveryParams.get("deliveryServiceId"), nullValue());

        if (combinator) {
            //в комбинаторном флоу не получаем deliveryService в ответе
            assertThat(orderEditOptions.getDeliveryOptions(), everyItem((hasProperty("deliveryServiceId",
                    nullValue()))));
        } else {
            // в ответе получаем интервалы обеих СД
            assertThat(orderEditOptions.getDeliveryOptions(), hasItems(
                    hasProperty("deliveryServiceId", is(DEFAULT_DELIVERY_SERVICE_ID)),
                    hasProperty("deliveryServiceId", is(ANOTHER_DELIVERY_SERVICE_ID))));
        }
    }

    @DisplayName("Должны создать и применить ChangeRequest с правильным типом при запросе на смену доставки " +
            "заказа с кредитом Тинькофф")
    @ParameterizedTest(name = "combinator {0}")
    @ValueSource(booleans = {true, false})
    void shouldCreateAndCompleteDeliveryOptionChangeRequest(boolean combinator) {
        initOrder(combinator, DeliveryType.DELIVERY);

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(1))
                .toDate(LocalDate.now(getClock()).plusDays(1))
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);

        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        assertThat(changeRequests.get(0), allOf(
                hasProperty("status", is(ChangeRequestStatus.APPLIED)),
                hasProperty("type", is(ChangeRequestType.DELIVERY_OPTION))));
    }

    @DisplayName("Актуальная доставка на момент клира платежа")
    @ParameterizedTest(name = "combinator {0}")
    @ValueSource(booleans = {true, false})
    void stillActualDeliveryOnClear(boolean combinator) {
        initOrder(combinator, DeliveryType.DELIVERY);

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);

        orderPayHelper.notifyPayment(payment);

        //доставка на следующий день все еще актуальна, заказ сразу уходит в PROCESSING
        Order orderAfterNotify = orderService.getOrder(order.getId());
        assertThat(orderAfterNotify.getStatus(), is(OrderStatus.PROCESSING));
    }

    @DisplayName("Протухшая доставка на момент клира платежа")
    @ParameterizedTest(name = "combinator {0}")
    @ValueSource(booleans = {true, false})
    void outdatedDeliveryOnClear(boolean combinator) {
        initOrder(combinator, DeliveryType.DELIVERY);

        Order unpaidOrder = orderService.getOrder(order.getId());
        assertThat(unpaidOrder.getStatus(), equalTo(UNPAID));
        assertThat(unpaidOrder.getSubstatus(), equalTo(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(unpaidOrder.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        checkOrderEditPossibility(DELIVERY_DATES, false);

        //прошло несколько дней с момента чекаута, выбранная опция доставки уже не актуальна
        setFixedTime(getClock().instant().plus(7, ChronoUnit.DAYS));
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPayment(payment);

        //запрашиваем у пользователя выбор новой опции доставки
        Order orderAfterNotify = orderService.getOrder(order.getId());
        assertThat(orderAfterNotify.getStatus(), is(OrderStatus.UNPAID));
        assertThat(orderAfterNotify.getSubstatus(), is(OrderSubstatus.WAITING_USER_DELIVERY_INPUT));
        checkOrderEditPossibility(DELIVERY_DATES, true);
    }

    @DisplayName("Обновляем shipment и delivery_route при переводе в processing заказа с измененной датой")
    @ParameterizedTest(name = "combinator {0}")
    @ValueSource(booleans = {true, false})
    void updateShipmentAndDeliveryRouteOnProcessing(boolean combinator) {
        initOrder(combinator, DeliveryType.DELIVERY);

        //прошло несколько дней с момента чекаута, выбранная опция доставки уже не актуальна, запросим новую.
        setFixedTime(getClock().instant().plus(7, ChronoUnit.DAYS));
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPayment(payment);

        //честный пользовательский флоу получения доступных опций доставки для редактирования, выбора опции и
        // применения.
        mockNewOption(combinator);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .build());
        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        DeliveryOption deliveryOption = orderEditOptions.getDeliveryOptions().iterator().next();
        TimeInterval timeInterval = deliveryOption.getTimeIntervalOptions().iterator().next();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(deliveryOption)
                .timeInterval(timeInterval)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        assertThat(changeRequests.get(0), hasProperty("status", is(ChangeRequestStatus.APPLIED)));

        order = orderService.getOrder(order.getId());
        assertThat("Заказ ушел в процессинг после применения новой актуальной опции доставки",
                order.getStatus(), is(OrderStatus.PROCESSING));
        assertThat("Даты и интервал доставки изменились на выбранные", order.getDelivery().getDeliveryDates(),
                allOf(
                        hasProperty("fromDate", is(Date.valueOf(deliveryOption.getFromDate()))),
                        hasProperty("toDate", is(Date.valueOf(deliveryOption.getToDate()))),
                        hasProperty("fromTime", is(timeInterval.getFromTime())),
                        hasProperty("toTime", is(timeInterval.getToTime()))
                ));
        assertThat("Дата отгрузки из опции в actualDelivery - в тот же день. " +
                        "Должны изменить ее на сегодня на парселе",
                order.getDelivery().getParcels().iterator().next().getShipmentDate(), is(LocalDate.now(getClock())));
        assertThat(order.getDelivery().getDeliveryServiceId(), is(ANOTHER_DELIVERY_SERVICE_ID));
        if (combinator) {
            JSONAssert.assertEquals("Для комбинатора заменили route на новый",
                    ANOTHER_ROUTE,
                    order.getDelivery().getParcels().get(0).getRoute().toString(),
                    JSONCompareMode.NON_EXTENSIBLE
            );
        }
    }

    @DisplayName("[QcCall]Обновляем shipment и delivery_route при переводе в processing заказа с измененной датой")
    @ParameterizedTest(name = "combinator {0}, deliveryType {1}")
    @CsvSource(
            value = {
                    "true:DELIVERY",
                    "false:DELIVERY",
                    "true:POST",
                    "false:POST"
            },
            delimiter = ':')
    void updateShipmentAndDeliveryRouteOnProcessingByQcCall(boolean combinator, DeliveryType deliveryType) {
        initOrder(combinator, deliveryType);

        //прошло несколько дней с момента чекаута, выбранная опция доставки уже не актуальна, запросим новую.
        setFixedTime(getClock().instant().plus(7, ChronoUnit.DAYS));
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPayment(payment);

        //честный пользовательский флоу получения доступных опций доставки для редактирования, выбора опции и
        // применения.
        mockNewOption(combinator);

        var orderInWaitingUserDeliveryInput = orderService.getOrder(order.getId());
        assertTrue(queuedCallService.existsQueuedCall(PROCESS_UNPAID_WAITING_USER_DELIVERY_INPUT, order.getId()));
        queuedCallService.executeQueuedCallBatch(PROCESS_UNPAID_WAITING_USER_DELIVERY_INPUT);
        // запуск откладывается на delayQcCallMinutes
        assertTrue(queuedCallService.existsQueuedCall(PROCESS_UNPAID_WAITING_USER_DELIVERY_INPUT, order.getId()));

        // спустя delayQcCallMinutes выбираем даты доставки автоматически
        setFixedTime(getClock().instant().plus(1, ChronoUnit.DAYS));

        assertTrue(queuedCallService.existsQueuedCall(PROCESS_UNPAID_WAITING_USER_DELIVERY_INPUT, order.getId()));
        queuedCallService.executeQueuedCallBatch(PROCESS_UNPAID_WAITING_USER_DELIVERY_INPUT);
        assertFalse(queuedCallService.existsQueuedCall(PROCESS_UNPAID_WAITING_USER_DELIVERY_INPUT, order.getId()));

        order = orderService.getOrder(order.getId());
        var oldDeliveryDates = orderInWaitingUserDeliveryInput.getDelivery().getDeliveryDates();
        var nowDate = Date.valueOf(LocalDate.now(getClock()));
        assertThat("Заказ ушел в процессинг после применения новой актуальной опции доставки",
                order.getStatus(), is(OrderStatus.PROCESSING));
        assertThat("Даты и интервал доставки изменились на выбранные", order.getDelivery().getDeliveryDates(),
                allOf(
                        hasProperty("fromDate", greaterThanOrEqualTo(nowDate)),
                        hasProperty("toDate", greaterThanOrEqualTo(nowDate)),
                        hasProperty("fromDate", not(oldDeliveryDates.getFromDate())),
                        hasProperty("toDate", not(oldDeliveryDates.getToDate()))
                ));
        if (deliveryType == DeliveryType.POST && !combinator) {
            assertThat("Дата отгрузки из опции в actualDelivery - дата в будущем.",
                    order.getDelivery().getParcels().iterator().next().getShipmentDate(),
                    greaterThanOrEqualTo(LocalDate.now(getClock())));
        } else {
            assertThat("Дата отгрузки из опции в actualDelivery - в тот же день." +
                            " Должны изменить ее на сегодня на парселе",
                    order.getDelivery().getParcels().iterator().next().getShipmentDate(),
                    is(LocalDate.now(getClock())));
        }
        assertThat(order.getDelivery().getDeliveryServiceId(), is(ANOTHER_DELIVERY_SERVICE_ID));
        if (combinator) {
            JSONAssert.assertEquals("Для комбинатора заменили route на новый",
                    ANOTHER_ROUTE,
                    order.getDelivery().getParcels().get(0).getRoute().toString(),
                    JSONCompareMode.NON_EXTENSIBLE
            );
        }
    }

    private void mockNewOption(boolean combinator) {
        ReportGeneratorParameters reportParameters = new Parameters(order).getReportParameters();
        //осталась одна СД, должны будем сменить доставку на нее
        ActualDelivery newActualDelivery = ActualDeliveryProvider.builder()
                .addDelivery(actualDelivery(ANOTHER_DELIVERY_SERVICE_ID))
                .addPost(createPickupOption(7, ANOTHER_DELIVERY_SERVICE_ID))
                .withFreeDelivery()
                .build();
        reportParameters.setActualDelivery(newActualDelivery);
        if (combinator) {
            DeliveryRoute deliveryRoute = DeliveryRouteProvider.fromActualDelivery(newActualDelivery,
                    DeliveryType.DELIVERY);
            DeliveryRouteProvider.cleanActualDelivery(newActualDelivery);
            //нужно заменить route, чтобы обновление запустилось в UpdateShipmentStatusAction.updateShipmentIfNotActual
            deliveryRoute.getResults().get(0).setRoute(ANOTHER_ROUTE);
            reportParameters.setDeliveryRoute(deliveryRoute);
            reportConfigurer.mockReportPlace(MarketReportPlace.DELIVERY_ROUTE, reportParameters);
        }
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, reportParameters);
    }

    private ActualDeliveryOption actualDelivery(Long serviceId) {
        ActualDeliveryOption option = new ActualDeliveryOption();
        option.setDayFrom(1);
        option.setDayTo(1);
        option.setTimeIntervals(List.of(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0)),
                new DeliveryTimeInterval(LocalTime.of(14, 0), LocalTime.of(18, 0))));
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        option.setDeliveryServiceId(serviceId);
        option.setShipmentDay(0);
        return option;
    }

    private void checkOrderEditPossibility(ChangeRequestType changeRequestType, boolean expectedResult) {
        var actualResult = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()),
                ClientRole.USER,
                BuyerProvider.UID,
                Collections.singletonList(Color.BLUE))
                .stream()
                .map(OrderEditPossibility::getEditPossibilities)
                .map(EditPossibilityWrapper::build)
                .anyMatch(editPossibilityWrapper -> editPossibilityWrapper.isPossible(changeRequestType));

        assertThat("Wrong order edit possibilities", actualResult, Matchers.equalTo(expectedResult));
    }

    public PickupOption createPickupOption(Integer shipmentDay, long deliveryServiceId) {
        PickupOption postOption = new PickupOption();
        postOption.setDeliveryServiceId(deliveryServiceId);
        postOption.setDayFrom(1);
        postOption.setDayTo(7);
        postOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        postOption.setPrice(POST_PRICE);
        postOption.setCurrency(Currency.RUR);
        postOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        postOption.setShipmentDay(shipmentDay);
        postOption.setOutletIds(Collections.singletonList(DeliveryProvider.POST_OUTLET_ID));
        postOption.setPackagingTime(Duration.ofHours(23));
        postOption.setPostCodes(Collections.singletonList(111222L));
        return postOption;
    }
}
