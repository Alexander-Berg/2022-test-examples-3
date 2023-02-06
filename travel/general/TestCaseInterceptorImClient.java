package ru.yandex.travel.train.partners.im;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import ru.yandex.travel.train.model.CustomerInfo;
import ru.yandex.travel.train.partners.im.model.AutoReturnRequest;
import ru.yandex.travel.train.partners.im.model.AutoReturnResponse;
import ru.yandex.travel.train.partners.im.model.ElectronicRegistrationRequest;
import ru.yandex.travel.train.partners.im.model.ElectronicRegistrationResponse;
import ru.yandex.travel.train.partners.im.model.OrderFullCustomerRequest;
import ru.yandex.travel.train.partners.im.model.OrderReservationBlankRequest;
import ru.yandex.travel.train.partners.im.model.OrderReservationTicketBarcodeRequest;
import ru.yandex.travel.train.partners.im.model.OrderReservationTicketBarcodeResponse;
import ru.yandex.travel.train.partners.im.model.ReservationConfirmResponse;
import ru.yandex.travel.train.partners.im.model.ReservationCreateRequest;
import ru.yandex.travel.train.partners.im.model.ReservationCreateResponse;
import ru.yandex.travel.train.partners.im.model.ReturnAmountRequest;
import ru.yandex.travel.train.partners.im.model.ReturnAmountResponse;
import ru.yandex.travel.train.partners.im.model.UpdateBlanksResponse;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceCheckoutRequest;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceCheckoutResponse;
import ru.yandex.travel.train.partners.im.model.insurance.InsurancePricingRequest;
import ru.yandex.travel.train.partners.im.model.insurance.InsurancePricingResponse;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceReturnRequest;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceReturnResponse;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationType;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOrderItemType;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderInfoResponse;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderItemResponse;
import ru.yandex.travel.train.partners.im.model.orderlist.OrderListRequest;
import ru.yandex.travel.train.partners.im.model.orderlist.OrderListResponse;

@Slf4j
public class TestCaseInterceptorImClient implements ImClient {
    private final ImClient defaultImClient;
    private final ImProperties imProperties;
    private final ImProperties.PointcutMiddleName pointcutMiddleName;
    private final ImProperties.ErrorsLastName errorsLastName;
    private final CustomerInfoRepository repository;

    public TestCaseInterceptorImClient(ImProperties imProperties, ImClient defaultImClient,
                                       CustomerInfoRepository repository) {
        this.defaultImClient = defaultImClient;
        this.imProperties = imProperties;
        this.pointcutMiddleName = imProperties.getTestCases().getPointcutMiddleName();
        this.errorsLastName = imProperties.getTestCases().getErrorsLastName();
        this.repository = repository;
    }

    @Override
    public AutoReturnResponse autoReturn(AutoReturnRequest request) {
        return defaultImClient.autoReturn(request);
    }

    @Override
    public ElectronicRegistrationResponse changeElectronicRegistration(ElectronicRegistrationRequest request) {
        return defaultImClient.changeElectronicRegistration(request);
    }

    @Override
    public InsuranceCheckoutResponse insuranceCheckout(InsuranceCheckoutRequest request) {
        checkCustomerAndThrow(pointcutMiddleName.getInsuranceCheckout(),
                request.getMainServiceReference().getOrderCustomerId());
        return defaultImClient.insuranceCheckout(request);
    }

    @Override
    public InsurancePricingResponse insurancePricing(InsurancePricingRequest request) {
        if (request.getProduct().getMainServiceReference().getOrderCustomerId() != null) {
            checkCustomerAndThrow(pointcutMiddleName.getInsurancePricing(),
                    request.getProduct().getMainServiceReference().getOrderCustomerId());
        } else {
            List<CustomerInfo> customers =
                    repository.getByBuyOperationId(request.getProduct().getMainServiceReference().getOrderItemId());
            for (CustomerInfo c : customers) {
                checkCustomerAndThrow(pointcutMiddleName.getInsurancePricing(), c);
            }
        }
        return defaultImClient.insurancePricing(request);
    }

    @Override
    public InsuranceReturnResponse insuranceReturn(InsuranceReturnRequest request) {
        return defaultImClient.insuranceReturn(request);
    }

    @Override
    public CompletableFuture<OrderInfoResponse> orderInfoAsync(int orderId, Duration timeout) {
        CompletableFuture<OrderInfoResponse> fResult = defaultImClient.orderInfoAsync(orderId);

        return fResult.thenApply(result -> {
            for (OrderItemResponse i : result.getOrderItems()) {
                if (i.getType() == ImOrderItemType.INSURANCE && i.getOperationType() == ImOperationType.BUY) {
                    CustomerInfo customer = repository.getOne(i.getOrderItemCustomers().get(0).getOrderCustomerId());
                    if (checkMocked(pointcutMiddleName.getInsuranceStatus(), customer)) {
                        if (equalsIgnoreCase(errorsLastName.getStatusInProgress(), customer.getLastName())) {
                            i.setSimpleOperationStatus(ImOperationStatus.IN_PROCESS);
                        } else if (equalsIgnoreCase(errorsLastName.getStatusError(), customer.getLastName())) {
                            i.setSimpleOperationStatus(ImOperationStatus.FAILED);
                        }
                    }
                }
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<byte[]> orderReservationBlankAsync(OrderReservationBlankRequest request,
                                                                Duration timeout) {
        return defaultImClient.orderReservationBlankAsync(request);
    }

    @Override
    public void reservationCancel(int orderId) {
        defaultImClient.reservationCancel(orderId);
    }

    @Override
    public ReservationConfirmResponse reservationConfirm(int orderId) {
        List<CustomerInfo> customers = repository.getByOrderId(orderId);
        for (CustomerInfo c : customers) {
            try {
                checkCustomerAndThrow(pointcutMiddleName.getReservationConfirm(), c);
            } catch (ImClientRetryableException ex) {
                throw ex;
            } catch (ImClientException ex) {
                defaultImClient.reservationCancel(orderId);
                throw ex;
            }
        }
        return defaultImClient.reservationConfirm(orderId);
    }

    @Override
    public ReservationCreateResponse reservationCreate(ReservationCreateRequest request, Object data) {
        boolean mockIm = request.getCustomers().stream().anyMatch(x ->
                equalsIgnoreCase(imProperties.getTestCases().getMockImFirstName(), x.getFirstName()));
        if (mockIm) {
            String mockReservationCreateLastName = request.getCustomers().stream()
                    .filter(x -> equalsIgnoreCase(imProperties.getTestCases().getMockImFirstName(), x.getFirstName())
                            && equalsIgnoreCase(pointcutMiddleName.getReservationCreate(), x.getMiddleName()))
                    .findAny()
                    .map(OrderFullCustomerRequest::getLastName)
                    .orElse(null);
            throwErrorByLastName(mockReservationCreateLastName);
        }

        ReservationCreateResponse result = defaultImClient.reservationCreate(request, null);

        if (mockIm) {
            repository.save(result.getCustomers().stream()
                    .map(x -> new CustomerInfo(x.getOrderCustomerId(), result.getOrderId(),
                            result.getReservationResults().get(0).getOrderItemId(),
                            x.getFirstName(), x.getMiddleName(), x.getLastName()))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public UpdateBlanksResponse updateBlanks(int orderItemId, Duration timeout) {
        return defaultImClient.updateBlanks(orderItemId);
    }

    @Override
    public ReturnAmountResponse getReturnAmount(ReturnAmountRequest request) {
        return defaultImClient.getReturnAmount(request);
    }

    @Override
    public OrderListResponse orderList(OrderListRequest request) {
        return defaultImClient.orderList(request);
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        } else {
            return a.equalsIgnoreCase(b);
        }
    }

    private boolean checkMocked(String middle, CustomerInfo customer) {
        return customer != null && equalsIgnoreCase(imProperties.getTestCases().getMockImFirstName(),
                customer.getFirstName())
                && equalsIgnoreCase(middle, customer.getMiddleName());
    }

    private void checkCustomerAndThrow(String middle, Integer customerId) {
        CustomerInfo customer = repository.getOne(customerId);
        checkCustomerAndThrow(middle, customer);
    }

    private void checkCustomerAndThrow(String middle, CustomerInfo customer) {
        if (checkMocked(middle, customer)) {
            throwErrorByLastName(customer.getLastName());
        }
    }

    private void throwErrorByLastName(String lastName) {
        if (equalsIgnoreCase(errorsLastName.getThrowCode2(), lastName)) {
            throw new ImClientRetryableException(2, "Не удалось получить ответ от поставщика услуг. Попробуйте " +
                    "обратиться позже");
        } else if (equalsIgnoreCase(errorsLastName.getThrowCode3(), lastName)) {
            throw new ImClientException(3, "Операция завершилась неуспешно на стороне поставщика услуг");
        } else if (equalsIgnoreCase(errorsLastName.getThrowNoSeats(), lastName)) {
            throw new ImClientException(345, "Нет мест по указанным требованиям");
        } else if (equalsIgnoreCase(errorsLastName.getThrowTimeout(), lastName)) {
            try {
                Thread.sleep(imProperties.getHttpReadTimeout().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new ImClientIOException("IM call timeout");
        }
    }

    @Override
    public CompletableFuture<OrderReservationTicketBarcodeResponse> orderReservationTicketBarcodeAsync(OrderReservationTicketBarcodeRequest request,
                                                                                                       Duration timeout) {
        return defaultImClient.orderReservationTicketBarcodeAsync(request, timeout);
    }
}
