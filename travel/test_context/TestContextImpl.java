package ru.yandex.travel.api.endpoints.test_context;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import ru.yandex.misc.lang.StringUtils;
import ru.yandex.travel.api.config.common.TestContextConfigurationProperties;
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
import ru.yandex.travel.api.infrastucture.ApiTokenEncrypter;
import ru.yandex.travel.api.models.hotels.TestOffer;
import ru.yandex.travel.api.services.hotels.searcher.SearcherClient;
import ru.yandex.travel.commons.lang.ComparatorUtils;
import ru.yandex.travel.hotels.proto.THotelTestContext;
import ru.yandex.travel.orders.commons.proto.EPaymentOutcome;
import ru.yandex.travel.orders.commons.proto.TAviaTestContext;
import ru.yandex.travel.orders.commons.proto.TAviaVariant;
import ru.yandex.travel.orders.commons.proto.TBusTestContext;
import ru.yandex.travel.orders.commons.proto.TDelayInterval;
import ru.yandex.travel.orders.commons.proto.TPaymentTestContext;
import ru.yandex.travel.orders.commons.proto.TSuburbanTestContext;
import ru.yandex.travel.orders.commons.proto.TSuburbanTestContextHandlerError;
import ru.yandex.travel.orders.commons.proto.TTrainOfficeAction;
import ru.yandex.travel.orders.commons.proto.TTrainOfficeActions;
import ru.yandex.travel.orders.commons.proto.TTrainOfficeRefundAction;
import ru.yandex.travel.orders.commons.proto.TTrainTestContext;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty("test-context.enabled")
@EnableConfigurationProperties(TestContextConfigurationProperties.class)
public class TestContextImpl {
    private final static long PERMALINK = 42;

    private final TestContextConfigurationProperties properties;
    private final SearcherClient client;
    private final ApiTokenEncrypter tokenEncrypter;

    public CompletableFuture<TestContextRspV1> generateTestOffers(TestContextReqV1 request) {
        LocalDate checkin = request.getCheckinDate();
        if (checkin == null) {
            checkin = LocalDate.now().plusDays(1);
        }
        LocalDate checkout = request.getCheckoutDate();
        if (checkout == null) {
            checkout = checkin.plusDays(1);
        }
        return client.findOffers(request.getPartnerId(), request.getOriginalId(), PERMALINK, checkin, checkout,
                request.getOccupancy(), buildTestContext(request)).thenApply(result -> {
            var offerList = result.get(0).getOffers().getOfferList().stream().map(offer -> {
                TestOffer testOffer = new TestOffer();
                testOffer.setOfferName(offer.getDisplayedTitle().getValue());
                testOffer.setPansionType(offer.getPansion());
                testOffer.setHasFreeCancellation(offer.getFreeCancellation().getValue());
                testOffer.setPrice(offer.getPrice().getAmount());
                testOffer.setToken(offer.getLandingInfo().getLandingTravelToken());
                if (!Strings.isNullOrEmpty(properties.getUrlPrefix())) {
                    testOffer.setBookingPageUrl(properties.getUrlPrefix() + "/?token=" + offer.getLandingInfo().getLandingTravelToken());
                }
                return testOffer;
            }).collect(Collectors.toList());
            TestContextRspV1 response = new TestContextRspV1();
            response.setOfferTokens(offerList);
            return response;
        });
    }


    private THotelTestContext buildTestContext(TestContextReqV1 request) {
        var builder =
                THotelTestContext.newBuilder()
                        .setForceAvailability(request.isForceAvailability())
                        .setGetOfferOutcome(request.getGetOfferOutcome())
                        .setCreateOrderOutcome(request.getCreateOrderOutcome())
                        .setHotelDataLookupOutcome(request.getHotelDataLookupOutcome())
                        .setReservationOutcome(request.getReservationOutcome())
                        .setConfirmationOutcome(request.getConfirmationOutcome())
                        .setRefundOutcome(request.getRefundOutcome())
                        .setPriceMismatchRate(request.getPriceMismatchRate())
                        .setIgnorePaymentScheduleRestrictions(request.isIgnorePaymentScheduleRestrictions())
                        .setIsPostPay(request.isPostPay());
        if (request.getCancellation() != null) {
            builder.setCancellation(request.getCancellation());
        }
        if (request.getPansionType() != null) {
            builder.setPansionType(request.getPansionType());
        }
        if (!Strings.isNullOrEmpty(request.getOfferName())) {
            builder.setOfferName(request.getOfferName());
        }
        if (request.getPriceAmount() != null) {
            builder.setPriceAmount(request.getPriceAmount());
        }
        if (request.getPartiallyRefundRate() != null) {
            builder.setPartiallyRefundableRate(request.getPartiallyRefundRate());
        }
        if (request.getPartiallyRefundableInMinutes() != null) {
            builder.setPartiallyRefundableInMinutes(request.getPartiallyRefundableInMinutes());
        }
        if (request.getNonRefundableInMinutes() != null) {
            builder.setNonRefundableInMinutes(request.getNonRefundableInMinutes());
        }
        if (!Strings.isNullOrEmpty(request.getExistingDolphinOrder())) {
            builder.setDolphinExistingOrderId(request.getExistingDolphinOrder());
        }
        if (request.getDiscountAmount() != null && request.getDiscountAmount() > 0) {
            builder.setDiscountAmount(Int32Value.newBuilder().setValue(request.getDiscountAmount()).build());
        }
        if (request.getMealPrice() != null && request.getMealPrice() > 0) {
            builder.setMealPrice(Int32Value.newBuilder().setValue(request.getMealPrice()).build());
        }
        return builder.build();
    }

    public CompletableFuture<AviaTestContextRspV1> generateAviaToken(AviaTestContextReqV1 request) {
        // for performing "camelCase" -> "CamelCase"
        SimpleModule simpleModule = new SimpleModule().addKeySerializer(String.class, new JsonSerializer<>() {
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeFieldName(StringUtils.capitalize(value));
            }
        });
        ObjectMapper objectMapper = new ObjectMapper().registerModule(simpleModule);

        List<TAviaVariant> aviaVariants = List.of();
        var variantsString = request.getAviaVariants();
        if (!Strings.isNullOrEmpty(variantsString)) {
            List<Object> aviaVariantsAsJsonNodes;

            try {
                aviaVariantsAsJsonNodes = objectMapper.readValue(variantsString, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                return CompletableFuture.failedFuture(e);
            }
            try {
                aviaVariants = aviaVariantsAsJsonNodes
                        .stream()
                        .map(jsonNode -> {
                            var aviaVariant = TAviaVariant.newBuilder();
                            try {
                                var textValue =
                                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                                JsonFormat.parser().merge(textValue, aviaVariant);
                            } catch (InvalidProtocolBufferException | JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                            return aviaVariant.build();
                        })
                        .collect(Collectors.toList());
            } catch (RuntimeException e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        TAviaTestContext aviaTestContext = TAviaTestContext.newBuilder()
                .setCheckAvailabilityOnRedirOutcome(request.getCheckAvailabilityOnRedirOutcome())
                .setCheckAvailabilityOutcome(request.getCheckAvailabilityBeforeBookingOutcome())
                .setConfirmationOutcome(request.getConfirmationOutcome())
                .setMqEventOutcome(request.getMqEventOutcome())
                .setMockAviaVariants(request.isMockAviaVariants())
                .addAllAviaVariants(aviaVariants)
                .build();

        EPaymentOutcome paymentOutcome = EPaymentOutcome.PO_UNKNOWN;
        switch (request.getTokenizationOutcome()) {
            case TO_SUCCESS:
                paymentOutcome = EPaymentOutcome.PO_SUCCESS;
                break;
            case TO_FAILURE:
                paymentOutcome = EPaymentOutcome.PO_FAILURE;
                break;
        }

        try {
            AviaTestContextRspV1 rsp = new AviaTestContextRspV1();
            rsp.setToken(tokenEncrypter.toAviaTestContextToken(aviaTestContext));
            rsp.setPaymentToken(tokenEncrypter.toPaymentTestContextToken(TPaymentTestContext.newBuilder()
                    .setPaymentOutcome(paymentOutcome).build()));
            return CompletableFuture.completedFuture(rsp);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<TrainTestContextRspV1> generateTrainToken(TrainTestContextReqV1 request) {
        var officeActionsBuilder = TTrainOfficeActions.newBuilder();
        if (nullToZero(request.getOfficeAcquireDelayInSeconds()) > 0) {
            officeActionsBuilder.setAcquire(
                    TTrainOfficeAction.newBuilder().setDelayInSeconds(request.getOfficeAcquireDelayInSeconds()));
        }
        if (nullToZero(request.getOfficeReturnDelayInSeconds()) > 0) {
            officeActionsBuilder.addRefunds(
                    TTrainOfficeRefundAction.newBuilder().setDelayInSeconds(request.getOfficeReturnDelayInSeconds()));
        }
        officeActionsBuilder.addAllRefunds(generateTrainOfficeRefundActions(request.getOfficeReturns()));
        var trainTestContext = TTrainTestContext.newBuilder()
                .setInsurancePricingOutcome(request.getInsurancePricingOutcome())
                .setInsuranceCheckoutOutcome(request.getInsuranceCheckoutOutcome())
                .setInsuranceCheckoutConfirmOutcome(request.getInsuranceCheckoutConfirmOutcome())
                .setRefundPricingOutcome(request.getRefundPricingOutcome())
                .setRefundCheckoutOutcome(request.getRefundCheckoutOutcome())
                .setReservationCreateOutcome(request.getCreateReservationOutcome())
                .setReservationConfirmOutcome(request.getConfirmReservationOutcome())
                .setOfficeActions(officeActionsBuilder)
                .setAlwaysTimeoutAfterConfirmDelayInSeconds(nullToZero(request.getAlwaysTimeoutAfterConfirmingInSeconds()))
                .build();
        try {
            TrainTestContextRspV1 rsp = new TrainTestContextRspV1();
            rsp.setTestContextToken(tokenEncrypter.toTrainTestContextToken(trainTestContext));
            return CompletableFuture.completedFuture(rsp);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<TTrainOfficeRefundAction> generateTrainOfficeRefundActions(List<String> rawOfficeReturns) {
        var result = new ArrayList<TTrainOfficeRefundAction>();
        if (rawOfficeReturns == null) {
            return result;
        }
        for (String rawOfficeReturn : rawOfficeReturns) {
            var parts = Stream.of(rawOfficeReturn.split(":"))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            Preconditions.checkArgument(parts.size() == 2, "invalid officeReturns value");
            result.add(TTrainOfficeRefundAction.newBuilder()
                    .setDelayInSeconds(parts.get(0))
                    .setAmount(parts.get(1))
                    .build());
        }
        return result;
    }

    public CompletableFuture<BusTestContextRspV1> generateBusToken(BusTestContextReqV1 request) {
        TBusTestContext busTestContext = TBusTestContext.newBuilder()
                .setBookOutcome(request.getBookOutcome())
                .setConfirmOutcome(request.getConfirmOutcome())
                .setRefundInfoOutcome(request.getRefundInfoOutcome())
                .setRefundOutcome(request.getRefundOutcome())
                .setExpireAfterSeconds(request.getExpireAfterSeconds())
                .build();
        try {
            BusTestContextRspV1 rsp = new BusTestContextRspV1();
            rsp.setTestContextToken(tokenEncrypter.toBusTestContextToken(busTestContext));
            return CompletableFuture.completedFuture(rsp);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<SuburbanTestContextRspV1> generateSuburbanToken(SuburbanTestContextReqV1 request) {
        var suburbanTestContext = TSuburbanTestContext.newBuilder()
                .setActualPrice(request.getActualPrice())
                .setTicketNumber(request.getTicketNumber())
                .setTicketBody(request.getTicketBody())
                .setValidForSeconds(request.getValidForSeconds());

        if (request.getBookHandlerErrorCount() > 0) {
            suburbanTestContext.setBookHandler(TSuburbanTestContextHandlerError.newBuilder()
                    .setErrorType(request.getBookHandlerErrorType())
                    .setErrorsCount(request.getBookHandlerErrorCount()).build());
        }

        if (request.getConfirmHandlerErrorCount() > 0) {
            suburbanTestContext.setConfirmHandler(TSuburbanTestContextHandlerError.newBuilder()
                    .setErrorType(request.getConfirmHandlerErrorType())
                    .setErrorsCount(request.getConfirmHandlerErrorCount()).build());
        }

        if (request.getOrderInfoHandlerErrorCount() > 0) {
            suburbanTestContext.setOrderInfoHandler(TSuburbanTestContextHandlerError.newBuilder()
                    .setErrorType(request.getOrderInfoHandlerErrorType())
                    .setErrorsCount(request.getOrderInfoHandlerErrorCount()).build());
        }

        if (request.getTicketBarcodeHandlerErrorCount() > 0) {
            suburbanTestContext.setTicketBarcodeHandler(TSuburbanTestContextHandlerError.newBuilder()
                    .setErrorType(request.getTicketBarcodeHandlerErrorType())
                    .setErrorsCount(request.getTicketBarcodeHandlerErrorCount()).build());
        }

        if (request.getBlankPdfHandlerErrorCount() > 0) {
            suburbanTestContext.setBlankPdfHandler(TSuburbanTestContextHandlerError.newBuilder()
                    .setErrorType(request.getBlankPdfHandlerErrorType())
                    .setErrorsCount(request.getBlankPdfHandlerErrorCount()).build());
        }

        return CompletableFuture.completedFuture(
                SuburbanTestContextRspV1.builder()
                        .testContextToken(tokenEncrypter.toSuburbanTestContextToken(suburbanTestContext.build()))
                        .build());
    }

    private int nullToZero(Integer num) {
        return num == null ? 0 : num;
    }

    public CompletableFuture<PaymentTestContextRspV1> generatePaymentToken(PaymentTestContextReqV1 request) {
        try {
            TPaymentTestContext paymentTestContext = TPaymentTestContext.newBuilder()
                    .setPaymentOutcome(request.getPaymentOutcome() != null ?
                            request.getPaymentOutcome() : EPaymentOutcome.PO_SUCCESS)
                    .setPaymentFailureResponseCode(Strings.nullToEmpty(request.getPaymentFailureResponseCode()))
                    .setPaymentFailureResponseDescription(Strings.nullToEmpty(request.getPaymentFailureResponseDescription()))
                    .setUserActionDelay(generateDelayInterval(
                            request.getMinUserActionDelay(), request.getMaxUserActionDelay()))
                    .setPaymentUrl(Strings.nullToEmpty(request.getPaymentUrl()))
                    .build();
            PaymentTestContextRspV1 rsp = new PaymentTestContextRspV1();
            rsp.setToken(tokenEncrypter.toPaymentTestContextToken(paymentTestContext));
            rsp.setPaymentTestContextToken(rsp.getToken());
            return CompletableFuture.completedFuture(rsp);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private TDelayInterval generateDelayInterval(Duration min, Duration max) {
        if (min == null) {
            min = Duration.ZERO;
        }
        if (max == null) {
            max = min;
        }
        Preconditions.checkArgument(!ComparatorUtils.isLessThan(max, min),
                "The max delay can't be less than the min delay");
        return TDelayInterval.newBuilder()
                .setDelayMin(Math.toIntExact(min.toMillis()))
                .setDelayMax(Math.toIntExact(max.toMillis()))
                .build();
    }
}
