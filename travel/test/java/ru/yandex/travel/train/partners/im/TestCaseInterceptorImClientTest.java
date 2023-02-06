package ru.yandex.travel.train.partners.im;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.train.partners.im.model.OrderCreateReservationCustomerResponse;
import ru.yandex.travel.train.partners.im.model.OrderFullCustomerRequest;
import ru.yandex.travel.train.partners.im.model.RailwayPassengerResponse;
import ru.yandex.travel.train.partners.im.model.RailwayReservationRequest;
import ru.yandex.travel.train.partners.im.model.RailwayReservationResponse;
import ru.yandex.travel.train.partners.im.model.ReservationCreateRequest;
import ru.yandex.travel.train.partners.im.model.ReservationCreateResponse;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceCheckoutRequest;
import ru.yandex.travel.train.partners.im.model.insurance.MainServiceReferenceInternal;
import ru.yandex.travel.train.partners.im.model.insurance.RailwayInsuranceTravelCheckoutRequest;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationType;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOrderItemType;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderInfoResponse;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderItemCustomer;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderItemResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCaseInterceptorImClientTest {
    private TestCaseInterceptorImClient testCaseInterceptorImClient;
    private ImClient originalImClient;
    private ImProperties imProperties;
    private CustomerInfoRepository customerInfoRepository;

    private Integer orderIdCounter = 10000;
    private Integer orderItemIdCounter = 20000;
    private Integer orderCustomerIdCounter = 30000;

    @Before
    public void setUp() {
        imProperties = new ImProperties();
        imProperties.setHttpReadTimeout(Duration.ofMillis(1));
        var testCases = new ImProperties.TestCases();
        imProperties.setTestCases(testCases);
        testCases.setEnabled(true);
        testCases.setMockImFirstName("Ошибкаим");
        testCases.setPointcutMiddleName(new ImProperties.PointcutMiddleName());
        testCases.getPointcutMiddleName().setReservationCreate("Бронирование");
        testCases.getPointcutMiddleName().setReservationConfirm("Подтверждение");
        testCases.getPointcutMiddleName().setInsuranceStatus("Статусстраховки");
        testCases.getPointcutMiddleName().setInsurancePricing("Ценастраховки");
        testCases.getPointcutMiddleName().setInsuranceCheckout("Выкупстраховки");
        testCases.setErrorsLastName(new ImProperties.ErrorsLastName());
        testCases.getErrorsLastName().setThrowTimeout("Таймаут");
        testCases.getErrorsLastName().setThrowCode2("Коддва");
        testCases.getErrorsLastName().setThrowCode3("Кодтри");
        testCases.getErrorsLastName().setThrowNoSeats("Нетмест");
        testCases.getErrorsLastName().setStatusError("Ошибка");
        testCases.getErrorsLastName().setStatusInProgress("Впроцессе");
        originalImClient = mock(ImClient.class);
        customerInfoRepository = new InMemoryCustomerInfoRepository();
        testCaseInterceptorImClient = new TestCaseInterceptorImClient(imProperties, originalImClient, customerInfoRepository);
    }

    @Test
    public void testInsuranceCheckout() {
        var reservationCreateRequest = reservationCreateRequest("Ошибкаим", "Выкупстраховки", "Кодтри");
        var reservationCreateResponse = reservationCreateResponse("Ошибкаим", "выкупстраховки", "Кодтри");
        when(originalImClient.reservationCreate(any(), any())).thenReturn(reservationCreateResponse);
        testCaseInterceptorImClient.reservationCreate(reservationCreateRequest, null);

        var request = new InsuranceCheckoutRequest(new MainServiceReferenceInternal(reservationCreateResponse.getReservationResults().get(0).getOrderItemId()),
                new RailwayInsuranceTravelCheckoutRequest(""), "", "");
        request.getMainServiceReference().setOrderCustomerId(reservationCreateResponse.getCustomers().get(0).getOrderCustomerId());

        assertThatThrownBy(() -> testCaseInterceptorImClient.insuranceCheckout(request))
                .isInstanceOf(ImClientException.class)
                .hasMessage("Операция завершилась неуспешно на стороне поставщика услуг");
        verify(originalImClient, times(0)).insuranceCheckout(any());
    }

    @Test
    public void testReplaceInsuranceStatus() {
        var reservationCreateRequest = reservationCreateRequest("Ошибкаим", "Статусстраховки", "Ошибка");
        var reservationCreateResponse = reservationCreateResponse("Ошибкаим", "Статусстраховки", "Ошибка");
        var orderInfoResponse = orderInfoResponse(reservationCreateResponse.getCustomers().get(0).getOrderCustomerId());
        when(originalImClient.reservationCreate(any(), any())).thenReturn(reservationCreateResponse);
        when(originalImClient.orderInfoAsync(anyInt())).thenReturn(CompletableFuture.completedFuture(orderInfoResponse));
        testCaseInterceptorImClient.reservationCreate(reservationCreateRequest, null);

        var orderInfo = testCaseInterceptorImClient.orderInfo(reservationCreateResponse.getOrderId());
        verify(originalImClient).orderInfoAsync(reservationCreateResponse.getOrderId());
        var insuranceBuy = orderInfo.getOrderItems().stream().filter(x ->
                x.getOperationType() == ImOperationType.BUY && x.getType() == ImOrderItemType.INSURANCE)
                .findFirst().get();
        assertThat(insuranceBuy.getSimpleOperationStatus()).isEqualTo(ImOperationStatus.FAILED);
        var ticketBuy = orderInfo.getOrderItems().stream().filter(x ->
                x.getOperationType() == ImOperationType.BUY && x.getType() == ImOrderItemType.RAILWAY)
                .findFirst().get();
        assertThat(ticketBuy.getSimpleOperationStatus()).isEqualTo(ImOperationStatus.OK);
    }

    @Test
    public void testReservationCreate() {
        var reservationCreateRequest = reservationCreateRequest("Ошибкаим", "Бронирование", "Нетмест");

        assertThatThrownBy(() -> testCaseInterceptorImClient.reservationCreate(reservationCreateRequest, null))
                .isInstanceOf(ImClientException.class)
                .hasMessage("Нет мест по указанным требованиям");
        verify(originalImClient, times(0)).reservationCreate(any(), any());
    }

    @Test
    public void testReservationConfirm() {
        var reservationCreateRequest = reservationCreateRequest("Ошибкаим", "Подтверждение", "Кодтри");
        var reservationCreateResponse = reservationCreateResponse("Ошибкаим", "Подтверждение", "Кодтри");
        when(originalImClient.reservationCreate(any(), any())).thenReturn(reservationCreateResponse);
        testCaseInterceptorImClient.reservationCreate(reservationCreateRequest, null);

        assertThatThrownBy(() -> testCaseInterceptorImClient.reservationConfirm(reservationCreateResponse.getOrderId()))
                .isInstanceOf(ImClientException.class);
        verify(originalImClient, times(0)).reservationConfirm(anyInt());
        verify(originalImClient).reservationCancel(reservationCreateResponse.getOrderId());
    }

    private ReservationCreateRequest reservationCreateRequest(String firstName, String middleName, String lastName) {
        var request = new ReservationCreateRequest();
        var customer = new OrderFullCustomerRequest();
        request.setCustomers(List.of(customer));
        customer.setFirstName(firstName);
        customer.setMiddleName(middleName);
        customer.setLastName(lastName);
        var reservation = new RailwayReservationRequest();
        request.setReservationItems(List.of(reservation));
        return request;
    }

    private ReservationCreateResponse reservationCreateResponse(String firstName, String middleName, String lastName) {
        Integer orderId = ++orderIdCounter;
        Integer orderItemId = ++orderItemIdCounter;
        Integer orderCustomerId = ++orderCustomerIdCounter;
        var response = new ReservationCreateResponse();
        response.setOrderId(orderId);
        var customer = new OrderCreateReservationCustomerResponse();
        response.setCustomers(List.of(customer));
        customer.setOrderCustomerId(orderCustomerId);
        customer.setFirstName(firstName);
        customer.setMiddleName(middleName);
        customer.setLastName(lastName);
        var reservation = new RailwayReservationResponse();
        response.setReservationResults(List.of(reservation));
        reservation.setOrderItemId(orderItemId);
        var passenger = new RailwayPassengerResponse();
        passenger.setOrderCustomerId(orderCustomerId);
        reservation.setPassengers(List.of(passenger));
        return response;
    }

    private OrderInfoResponse orderInfoResponse(Integer orderCustomerId) {
        var response = new OrderInfoResponse();
        var ticketBuy = new OrderItemResponse();
        var insuranceBuy = new OrderItemResponse();
        response.setOrderItems(List.of(ticketBuy, insuranceBuy));
        var customer = new OrderItemCustomer();
        customer.setOrderCustomerId(orderCustomerId);
        ticketBuy.setOrderItemCustomers(List.of(customer));
        ticketBuy.setSimpleOperationStatus(ImOperationStatus.OK);
        ticketBuy.setType(ImOrderItemType.RAILWAY);
        ticketBuy.setOperationType(ImOperationType.BUY);
        customer = new OrderItemCustomer();
        customer.setOrderCustomerId(orderCustomerId);
        insuranceBuy.setOrderItemCustomers(List.of(customer));
        insuranceBuy.setSimpleOperationStatus(ImOperationStatus.OK);
        insuranceBuy.setType(ImOrderItemType.INSURANCE);
        insuranceBuy.setOperationType(ImOperationType.BUY);
        return response;
    }
}
