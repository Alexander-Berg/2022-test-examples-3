package ru.yandex.travel.orders.integration.aeroflot;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.SneakyThrows;
import org.assertj.core.util.Preconditions;
import org.assertj.core.util.Strings;
import org.mockito.Mockito;

import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderCreateResult;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderRef;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderStatus;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderSubStatus;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotServicePayload;
import ru.yandex.avia.booking.partners.gateways.model.PayloadMapper;
import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.TJson;
import ru.yandex.travel.orders.commons.proto.EOrderType;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.commons.proto.TAviaPaymentTestContext;
import ru.yandex.travel.orders.commons.proto.TAviaTestContext;
import ru.yandex.travel.orders.proto.TCreateOrderReq;
import ru.yandex.travel.orders.proto.TCreateServiceReq;
import ru.yandex.travel.orders.proto.TOrderInfo;
import ru.yandex.travel.orders.proto.TUserInfo;
import ru.yandex.travel.orders.services.payments.TrustClient;
import ru.yandex.travel.orders.services.payments.model.PaymentStatusEnum;
import ru.yandex.travel.orders.services.payments.model.TrustBasketStatusResponse;
import ru.yandex.travel.orders.services.payments.model.TrustBindingToken;
import ru.yandex.travel.orders.services.payments.model.TrustCreateBasketResponse;
import ru.yandex.travel.orders.services.payments.model.TrustStartPaymentResponse;
import ru.yandex.travel.testing.misc.TestResources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TestHelpers {
    static final String YANDEX_UID = "1234567890";
    static final String SESSION_KEY = "qwerty";

    static void mockTrustCalls(TrustClient aeroflotTrustClient) {
        Mockito.clearInvocations(aeroflotTrustClient);
        TrustCreateBasketResponse basketRsp = new TrustCreateBasketResponse();
        basketRsp.setPurchaseToken("<purchase_token>");
        when(aeroflotTrustClient.createBasket(any(), any(), any())).thenReturn(basketRsp);

        TrustStartPaymentResponse startPaymentRsp = new TrustStartPaymentResponse();
        startPaymentRsp.setPaymentUrl("https://some-paymenet-url.yandex.net/api/...");
        when(aeroflotTrustClient.startPayment(eq(basketRsp.getPurchaseToken()), any())).thenReturn(startPaymentRsp);

        TrustBasketStatusResponse statusStarted = new TrustBasketStatusResponse();
        statusStarted.setPaymentStatus(PaymentStatusEnum.STARTED);
        TrustBasketStatusResponse statusProcessed = new TrustBasketStatusResponse();
        statusProcessed.setPaymentStatus(PaymentStatusEnum.CLEARED);
        TrustBindingToken token = new TrustBindingToken();
        token.setExpiration(LocalDateTime.now().plusMinutes(2));
        token.setValue(UUID.randomUUID().toString());
        statusProcessed.setBindingToken(token);
        when(aeroflotTrustClient.getBasketStatus(eq(basketRsp.getPurchaseToken()), any()))
                .thenReturn(statusStarted, statusStarted, statusStarted, statusProcessed);
    }

    static AeroflotOrderCreateResult aeroflotOrderCreateResultSuccessWith3ds(String pnr) {
        return aeroflotOrderCreateResultSuccess(AeroflotOrderStatus.PAYMENT_PARTIALLY_FAILED,
                AeroflotOrderSubStatus.REQUIRES_3DS, pnr, "some_url");
    }

    static AeroflotOrderCreateResult aeroflotOrderCreateResultSuccess(String pnr) {
        return aeroflotOrderCreateResultSuccess(AeroflotOrderStatus.PAID_TICKETED, null, pnr, null);
    }

    static AeroflotOrderCreateResult aeroflotOrderCreateResultSuccess(AeroflotOrderStatus status,
                                                                      AeroflotOrderSubStatus subStatus, String pnr,
                                                                      String confirmationUrl) {
        return AeroflotOrderCreateResult.builder()
                .statusCode(status)
                .subStatusCode(subStatus)
                .orderRef(AeroflotOrderRef.builder()
                        .pnr(pnr)
                        .pnrDate("PNR_date_2019-05-22")
                        .orderId("some_order_id")
                        .mdOrderId("some_md_order_id")
                        .build())
                .confirmationUrl(confirmationUrl)
                .build();
    }

    static TCreateOrderReq createOrderRequest() {
        return createOrderRequest(null, null);
    }

    static TCreateOrderReq createOrderRequest(String variantId) {
        return createOrderRequest(variantId, null, null);
    }

    static TCreateOrderReq createOrderRequest(TAviaTestContext testContext,
                                              TAviaPaymentTestContext paymentTestContext) {
        return createOrderRequest(null, testContext, paymentTestContext);
    }

    static TCreateOrderReq createOrderRequest(String variantId,
                                              TAviaTestContext testContext,
                                              TAviaPaymentTestContext paymentTestContext) {
        String payloadJson = TestResources.readResource("integration/aeroflot/aeroflot_order_item_payload.json");
        if (!Strings.isNullOrEmpty(variantId)) {
            String variantIdMacros = "_COULD_BE_OVERRIDEN_VARIANT_ID_";
            Preconditions.checkState(payloadJson.contains(variantIdMacros), "no variant id macros to replace");
            payloadJson = payloadJson.replace(variantIdMacros, variantId);
        }
        TCreateServiceReq.Builder servicesBuilder = TCreateServiceReq.newBuilder()
                .setServiceType(EServiceType.PT_FLIGHT)
                .setSourcePayload(TJson.newBuilder().setValue(payloadJson));
        if (testContext != null) {
            servicesBuilder.setAviaTestContext(testContext);
        }
        TCreateOrderReq.Builder requestBuilder = TCreateOrderReq.newBuilder()
                .setOrderType(EOrderType.OT_AVIA_AEROFLOT)
                .setDeduplicationKey(UUID.randomUUID().toString())
                .addCreateServices(servicesBuilder)
                .setCurrency(ECurrency.C_RUB)
                .setOwner(TUserInfo.newBuilder()
                        .setEmail("test@test.com")
                        .setPhone("+79111111111")
                        .setYandexUid(YANDEX_UID)
                )
                .setMockPayment(paymentTestContext != null);
        if (paymentTestContext != null) {
            requestBuilder.setAviaPaymentTestContext(paymentTestContext);
        }
        return requestBuilder.build();
    }

    @SneakyThrows
    static AeroflotServicePayload parsePayload(TOrderInfo order) {
        String json = order.getService(0).getServiceInfo().getPayload().getValue();
        return PayloadMapper.mapper.readValue(json, AeroflotServicePayload.class);
    }

    public static AeroflotOrderCreateResult incompletePaymentResult() {
        return paymentResult(AeroflotOrderStatus.PAYMENT_PARTIALLY_FAILED);
    }

    public static AeroflotOrderCreateResult paymentResult(AeroflotOrderStatus status) {
        AeroflotOrderRef fakeRef = AeroflotOrderRef.builder().orderId("<fake_order_id>").build();
        return new AeroflotOrderCreateResult(status, null, null, fakeRef, null);
    }

    public static AeroflotOrderCreateResult successfulPaymentResult(String pnr) {
        return new AeroflotOrderCreateResult(AeroflotOrderStatus.PAID_TICKETED, null, null,
                AeroflotOrderRef.builder().orderId("<fake_order_id>").pnr(pnr).build(), null);
    }
}
