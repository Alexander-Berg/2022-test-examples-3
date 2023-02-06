package ru.yandex.travel.orders.workflows.orderitem.train;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limit.FixedLimit;
import com.netflix.concurrency.limits.limiter.SimpleLimiter;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.factories.ImReservationCreateResponseFactory;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.services.train.TrainDiscountService;
import ru.yandex.travel.orders.services.train.tariffinfo.TrainTariffInfoDataProvider;
import ru.yandex.travel.orders.workflow.order.proto.TServiceCancelled;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TPartnerReservationResponse;
import ru.yandex.travel.orders.workflow.train.proto.TReservationCommit;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.ReservingStateHandler;
import ru.yandex.travel.train.model.CarType;
import ru.yandex.travel.train.model.PassengerCategory;
import ru.yandex.travel.train.model.ReservationPlaceType;
import ru.yandex.travel.train.model.Sex;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.train.model.TrainTicket;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.ImClientException;
import ru.yandex.travel.train.partners.im.ImClientInvalidPassengerEmailException;
import ru.yandex.travel.train.partners.im.ImClientRetryableException;
import ru.yandex.travel.train.partners.im.model.OrderCreateReservationCustomerResponse;
import ru.yandex.travel.train.partners.im.model.RailwayPassengerResponse;
import ru.yandex.travel.train.partners.im.model.ReservationCreateResponse;
import ru.yandex.travel.workflow.exceptions.RetryableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class ReservingStateHandlerTest {

    private ImClient imClient;
    private ImClientProvider imClientProvider;
    private ReservingStateHandler handler;
    private TrainWorkflowProperties trainWorkflowProperties;
    private TrainTariffInfoDataProvider trainTariffInfoDataProvider;
    private Limiter<Void> limiter;

    @Before
    public void setUp() {
        imClient = mock(ImClient.class);
        imClientProvider = mock(ImClientProvider.class);
        when(imClientProvider.getImClientForOrderItem(any())).thenReturn(imClient);
        trainWorkflowProperties = new TrainWorkflowProperties();
        trainWorkflowProperties.setReservationMaxTries(1);
        trainWorkflowProperties.setReservationRetryDelay(Duration.ZERO);
        trainTariffInfoDataProvider = HandlerTestHelper.createTrainTariffInfoDataProvider();
        limiter = SimpleLimiter.newBuilder().limit(FixedLimit.of(Integer.MAX_VALUE)).build();
        handler = new ReservingStateHandler(imClientProvider, trainWorkflowProperties, trainTariffInfoDataProvider,
                mock(TrainDiscountService.class), limiter);
    }

    @Test
    public void testOrderReserved() {
        var factory = new TrainOrderItemFactory();
        var trainOrderItem = factory.createTrainOrderItem();
        trainOrderItem.setState(EOrderItemState.IS_RESERVING);
        var reservationResponseFactory = new ImReservationCreateResponseFactory(trainOrderItem);
        var response = reservationResponseFactory.createReservationCreateResponse();
        when(imClient.reservationCreate(any(), any())).thenReturn(response);

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TReservationCommit.getDefaultInstance(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_RESERVING);
        verify(imClient).reservationCreate(any(), any());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TPartnerReservationResponse.class);

        var ctx2 = testMessagingContext(trainOrderItem);
        handler.handleEvent(ctx.getScheduledEvents().get(0).getMessage(), ctx2);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_INSURANCE_PRICING_TRAINS);
        assertThat(trainOrderItem.getPayload().getPartnerOrderId()).isNotEqualTo(0);
        assertThat(trainOrderItem.getPayload().getPassengers().get(0).getTicket().getRawTariffType()).isEqualTo("Full");
        assertThat(trainOrderItem.getPayload().getPassengers().get(0).getTicket().getBookedTariffCode()).isEqualTo(
                "full");
        assertThat(trainOrderItem.getPayload().getPassengers().get(0).getTicket().getRawTariffName()).isEqualTo(
                "Полный");
    }

    @Test
    public void testReservationRetryableError() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_NEW);
        var trainOrderItem = factory.createTrainOrderItem();
        var originalError = new ImClientRetryableException(1, "Rzd unavailable");
        when(imClient.reservationCreate(any(), any())).thenThrow(originalError);

        var ctx = testMessagingContext(trainOrderItem);

        assertThatThrownBy(() -> handler.handleEvent(TReservationCommit.getDefaultInstance(), ctx))
                .isInstanceOf(RetryableException.class)
                .hasCause(originalError);
        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_NEW);
        assertThat(trainOrderItem.getPayload().getPartnerOrderId()).isEqualTo(0);
        verify(imClient).reservationCreate(any(), any());
    }

    @Test
    public void testReservationValidationError() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_NEW);
        var trainOrderItem = factory.createTrainOrderItem();
        var originalError = new ImClientInvalidPassengerEmailException(1386, "Invalid passenger's email", null, 0);
        when(imClient.reservationCreate(any(), any())).thenThrow(originalError);

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TReservationCommit.getDefaultInstance(), ctx);

        assertThat(trainOrderItem.getPayload().getPassengers().get(0).isUseEmailForReservation()).isFalse();
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TReservationCommit.class);
    }

    @Test
    public void testReservationCancelled() {
        var factory = new TrainOrderItemFactory();
        var trainOrderItem = factory.createTrainOrderItem();
        trainOrderItem.setState(EOrderItemState.IS_RESERVING);
        var originalError = new ImClientException(42, "Нет мест");
        when(imClient.reservationCreate(any(), any())).thenThrow(originalError);

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TReservationCommit.getDefaultInstance(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_RESERVING);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TPartnerReservationResponse.class);

        var ctx2 = testMessagingContext(trainOrderItem);
        handler.handleEvent(ctx.getScheduledEvents().get(0).getMessage(), ctx2);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        assertThat(trainOrderItem.getPayload().getPartnerOrderId()).isEqualTo(0);
        verify(imClient).reservationCreate(any(), any());
        assertThat(ctx2.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceCancelled.class);
    }

    @Test
    public void testSaveResponse() {
        var factory = new TrainOrderItemFactory();
        factory.setPassengers(List.of(factory.createTrainPassenger(), factory.createTrainPassenger()));
        var trainOrderItem = factory.createTrainOrderItem();
        var reservationResponseFactory = new ImReservationCreateResponseFactory(trainOrderItem);
        reservationResponseFactory.setOrderId(333333333);
        reservationResponseFactory.setOnlyFullReturnPossible(true);
        ReservationCreateResponse response = reservationResponseFactory.createReservationCreateResponse();

        handler.saveResponse(trainOrderItem, response);

        TrainReservation payload = trainOrderItem.getPayload();
        assertThat(payload.isOnlyFullReturnPossible()).isTrue();
        assertThat(payload.getPartnerOrderId()).isEqualTo(333333333);
        assertThat(payload.getReservationNumber()).isNull();
        assertThat(payload.getArrivalTime()).isEqualTo(LocalDateTime.parse("2019-07-10T16:55:00").toInstant(ZoneOffset.UTC));
        assertThat(payload.getDepartureTime()).isEqualTo(LocalDateTime.parse("2019-07-09T16:55:00").toInstant(ZoneOffset.UTC));
        assertThat(payload.getCarNumber()).isEqualTo("02");
        assertThat(payload.getCarType()).isEqualTo(CarType.SEDENTARY);
        assertThat(payload.getCarrier()).isEqualTo("ФПК");
        assertThat(payload.getPartnerDescription()).isEqualTo("ПРЕДВАРИТЕЛЬНЫЙ ДОСМОТР НА ВОКЗАЛЕ.");
        assertThat(payload.getStationFromCode()).isEqualTo("2064110");
        assertThat(payload.getStationToCode()).isEqualTo("2064001");
        assertThat(payload.getTrainNumber()).isEqualTo("820Э");
        assertThat(payload.getPassengers().get(0).getCustomerId()).isNotNull();
        var ticket = payload.getPassengers().get(0).getTicket();
        assertThat(ticket.getBlankId()).isEqualTo(20000001);
        assertThat(ticket.getCarrierInn()).isEqualTo("77777777");
        assertThat(ticket.getServiceAmount()).isEqualByComparingTo(Money.of(BigDecimal.valueOf(156),
                ProtoCurrencyUnit.RUB));
        assertThat(ticket.getServiceVatAmount()).isEqualByComparingTo(Money.of(BigDecimal.valueOf(26),
                ProtoCurrencyUnit.RUB));
        assertThat(ticket.getServiceVatRate()).isEqualByComparingTo(20.0D);
        assertThat(ticket.getTariffAmount()).isEqualByComparingTo(Money.of(BigDecimal.valueOf(3000.0),
                ProtoCurrencyUnit.RUB));
        assertThat(ticket.getTariffVatAmount()).isEqualByComparingTo(Money.of(BigDecimal.valueOf(0),
                ProtoCurrencyUnit.RUB));
        assertThat(ticket.getTariffVatRate()).isEqualByComparingTo(0.0D);
        assertThat(ticket.getPlaces().size()).isEqualTo(1);
        assertThat(ticket.getPlaces().get(0).getNumber()).isEqualTo("010");
        assertThat(ticket.getPlaces().get(0).getType()).isEqualTo(ReservationPlaceType.NEAR_TABLE_FORWARD);
    }

    @Test
    public void testGetNotice() {
        var notice = ReservingStateHandler.getPartnerNotice(null);
        assertThat(notice.getTimeNotice()).isNull();
        assertThat(notice.getSpecialNotice()).isNull();

        notice = ReservingStateHandler.getPartnerNotice("");
        assertThat(notice.getTimeNotice()).isNull();
        assertThat(notice.getSpecialNotice()).isNull();

        notice = ReservingStateHandler.getPartnerNotice("ВРЕМЯ МСК.   ДОСМОТР   НА ВОКЗАЛЕ.  УДАЧИ");
        assertThat(notice.getTimeNotice()).isEqualTo("ВРЕМЯ МСК");
        assertThat(notice.getSpecialNotice()).isEqualTo("ДОСМОТР НА ВОКЗАЛЕ. УДАЧИ");
    }


    @Test
    public void testBabyWithoutPlace() {
        var factory = new TrainOrderItemFactory();
        TrainOrderItem trainOrderItem = factory.createTrainOrderItem();
        trainOrderItem.setState(EOrderItemState.IS_RESERVING);
        var reservationResponseFactory = new ImReservationCreateResponseFactory(trainOrderItem);
        reservationResponseFactory.setOrderId(333333333);
        ReservationCreateResponse response = reservationResponseFactory.createReservationCreateResponse();
        var babyCustomer = new OrderCreateReservationCustomerResponse();
        babyCustomer.setOrderCustomerId(107);
        babyCustomer.setIndex(1);
        babyCustomer.setBirthday(LocalDate.now().atTime(0, 0));
        babyCustomer.setSex(Sex.MALE);
        response.getCustomers().add(babyCustomer);
        RailwayPassengerResponse babyPassenger = new RailwayPassengerResponse();
        babyPassenger.setOrderCustomerId(107);
        babyPassenger.setCategory(PassengerCategory.BABY);
        babyPassenger.setAmount(BigDecimal.valueOf(0.0));
        babyPassenger.setOrderCustomerReferenceIndex(1);
        babyPassenger.setOrderItemBlankId(response.getReservationResults().get(0).getBlanks().get(0).getOrderItemBlankId());
        babyPassenger.setPlacesWithType(new ArrayList<>());
        response.getReservationResults().get(0).getPassengers().add(babyPassenger);
        TrainPassenger baby = factory.createTrainPassenger();
        baby.setTariffCode("full");
        baby.setCategory(PassengerCategory.BABY);
        baby.setBirthday(LocalDate.now());
        trainOrderItem.getPayload().getPassengers().add(baby);
        when(imClient.reservationCreate(any(), any())).thenReturn(response);

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TReservationCommit.getDefaultInstance(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_RESERVING);
        verify(imClient).reservationCreate(any(), any());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TPartnerReservationResponse.class);

        var ctx2 = testMessagingContext(trainOrderItem);
        handler.handleEvent(ctx.getScheduledEvents().get(0).getMessage(), ctx2);

        TrainReservation payload = trainOrderItem.getPayload();
        TrainTicket babyTicket = payload.getPassengers().get(1).getTicket();
        assertThat(babyTicket).isNotNull();
        assertThat(babyTicket.getTariffAmount().isZero()).isTrue();
        assertThat(babyTicket.getServiceAmount().isZero()).isTrue();
        assertThat(babyTicket.getFeeAmount().isZero()).isTrue();
    }
}
