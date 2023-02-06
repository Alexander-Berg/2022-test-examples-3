package ru.yandex.travel.api.endpoints.test_context;

import com.google.common.base.Preconditions;
import io.grpc.StatusRuntimeException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import ru.yandex.travel.api.endpoints.test_context.req_rsp.AviaTestContextReqV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.AviaTestContextRspV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.BusTestContextReqV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.BusTestContextRspV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.PaymentTestContextReqV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.PaymentTestContextRspV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.SuburbanTestContextReqV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.SuburbanTestContextRspV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.TestContextReqV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.TestContextRspV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.TrainTestContextReqV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.TrainTestContextRspV1;
import ru.yandex.travel.api.exceptions.GrpcError;
import ru.yandex.travel.api.infrastucture.BindFromQuery;
import ru.yandex.travel.api.infrastucture.ResponseProcessor;
import ru.yandex.travel.hotels.proto.EHotelCancellation;
import ru.yandex.travel.hotels.proto.EHotelConfirmationOutcome;
import ru.yandex.travel.hotels.proto.EHotelDataLookupOutcome;
import ru.yandex.travel.hotels.proto.EHotelOfferOutcome;
import ru.yandex.travel.hotels.proto.EHotelReservationOutcome;
import ru.yandex.travel.hotels.proto.EPansionType;
import ru.yandex.travel.hotels.proto.EPartnerId;

@RestController
@RequestMapping("/api/test_context")
@Api(value = "Test Context", description = "Helper method to generate test contexts")
@RequiredArgsConstructor
@ConditionalOnProperty("test-context.enabled")
public class TestContextController {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
    }

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<GrpcError> handleGrpcErrors(StatusRuntimeException ex) {
        GrpcError error = GrpcError.fromGrpcStatusRuntimeException(ex);
        return ResponseEntity.status(error.getStatus()).contentType(MediaType.APPLICATION_JSON).body(error);
    }

    private final ResponseProcessor responseProcessor;
    private final TestContextImpl impl;

    @RequestMapping(value = "/v1/tokens", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation("Генерирует Hotel-BoY-токен для тестирования")
    public DeferredResult<TestContextRspV1> getTokens(@Validated @BindFromQuery TestContextReqV1 request) {
        validateRequest(request);
        return responseProcessor.replyWithFuture("TestContextGetTokens", () -> impl.generateTestOffers(request));
    }

    @RequestMapping(value = "/v1/avia_token", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation("Генерирует Aeroflot-BoY токен для тестирования")
    public DeferredResult<AviaTestContextRspV1> getAviaToken(@Validated @BindFromQuery AviaTestContextReqV1 request) {
        return responseProcessor.replyWithFuture("AviaTestContextGetToken", () -> impl.generateAviaToken(request));
    }

    @RequestMapping(value = "/v1/train_token", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation("Генерирует ЖД-BoY-токен для тестирования")
    public DeferredResult<TrainTestContextRspV1> getTrainToken(@Validated @BindFromQuery TrainTestContextReqV1 request) {
        return responseProcessor.replyWithFuture("TrainTestContextGetToken", () -> impl.generateTrainToken(request));
    }

    @RequestMapping(value = "/v1/bus_token", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation("Генерирует Bus-BoY-токен для тестирования")
    public DeferredResult<BusTestContextRspV1> getBusToken(@Validated @BindFromQuery BusTestContextReqV1 request) {
        return responseProcessor.replyWithFuture("BusTestContextGetToken", () -> impl.generateBusToken(request));
    }

    @RequestMapping(value = "/v1/suburban_token", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation("Генерирует Suburban-BoY-токен для тестирования")
    public DeferredResult<SuburbanTestContextRspV1> getSuburbanToken(@Validated @BindFromQuery SuburbanTestContextReqV1 request) {
        return responseProcessor.replyWithFuture("SuburbanTestContextGetToken",
                () -> impl.generateSuburbanToken(request));
    }

    @RequestMapping(value = "/v1/payment_token", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation("Генерирует тестовый платежный контекст")
    public DeferredResult<PaymentTestContextRspV1> getPaymentToken(@Validated @BindFromQuery PaymentTestContextReqV1 request) {
        return responseProcessor.replyWithFuture("PaymentTestContextGetToken",
                () -> impl.generatePaymentToken(request));
    }

    private void validateRequest(TestContextReqV1 request) {
        if (request.isForceAvailability()) {
            Preconditions.checkArgument(request.getCancellation() != null, "Cancellation Policy should be set");
            Preconditions.checkArgument(request.getPansionType() != null, "Pansion should be set");
            Preconditions.checkArgument(request.getPansionType() != EPansionType.PT_UNKNOWN, "Pansion should be set");
            Preconditions.checkArgument(Strings.isNotBlank(request.getOfferName()), "Offer name should be defined");
            Preconditions.checkArgument(request.getPriceAmount() != null && request.getPriceAmount() > 0,
                    "Price should be specified");
            if (request.getCancellation() == EHotelCancellation.CR_CUSTOM || request.getCancellation() == EHotelCancellation.CR_PARTIALLY_REFUNDABLE) {
                Preconditions.checkArgument(request.getPartiallyRefundRate() != null && request.getPartiallyRefundRate() > 0,
                        "Partially Refundable Rate must be specified");
            }
            if (request.getCancellation() == EHotelCancellation.CR_CUSTOM) {
                Preconditions.checkArgument(request.getPartiallyRefundableInMinutes() != null &&
                                request.getPartiallyRefundableInMinutes() > 0,
                        "Partially Refundable in minutes must be specified");
                Preconditions.checkArgument(request.getNonRefundableInMinutes() != null &&
                        request.getNonRefundableInMinutes() > 0, "Non refundable in minutes must be specified");
            }
        }
        if (request.getPartnerId() != EPartnerId.PI_TRAVELLINE) {
            Preconditions.checkArgument(request.getHotelDataLookupOutcome() != EHotelDataLookupOutcome.HO_DISCONNECTED,
                    "Disconnected outcomes are supported only for Travelline");
            Preconditions.checkArgument(request.getGetOfferOutcome() != EHotelOfferOutcome.OO_DISCONNECTED,
                    "Disconnected outcomes are supported only for Travelline");
            Preconditions.checkArgument(request.getReservationOutcome() != EHotelReservationOutcome.RO_DISCONNECTED,
                    "Disconnected outcomes are supported only for Travelline");
            Preconditions.checkArgument(request.getConfirmationOutcome() != EHotelConfirmationOutcome.CO_DISCONNECTED,
                    "Disconnected outcomes are supported only for Travelline");
        }
    }
}
