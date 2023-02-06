package ru.yandex.avia.booking.partners.gateways.aeroflot;

import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.avia.booking.partners.gateways.BookingTooManyRequestsException;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderRef;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotServicePayload;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.model.booking.ClientInfo;
import ru.yandex.avia.booking.remote.RpcContext;
import ru.yandex.avia.booking.tests.wiremock.Wiremock;
import ru.yandex.avia.booking.tests.wiremock.WiremockServerResolver;
import ru.yandex.avia.booking.tests.wiremock.WiremockUri;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.loadSampleTdRequestNdcV3SingleTariff;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.stubRequest;

// todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
@Disabled
@ExtendWith(WiremockServerResolver.class)
class AeroflotGatewayGetOrderStatusTest {
    private AeroflotGateway gateway = AeroflotApiStubsHelper.defaultGateway("url_not_needed");

    @BeforeEach
    void before(@WiremockUri String bookingUri) {
        gateway = AeroflotApiStubsHelper.defaultGateway(bookingUri);
    }

    @Test
    void initPaymentUnexpectedError(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_status_response_error_some.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        AeroflotServicePayload statusReq = statusRequest(variant);
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> gateway.getOrderStatus(statusReq, RpcContext.empty()))
                .withMessageContaining("Unexpected error");
    }

    @Test
    void initPaymentTooManyRequests(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/generic_response_error_429.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        AeroflotServicePayload statusReq = statusRequest(variant);
        assertThatExceptionOfType(BookingTooManyRequestsException.class)
                .isThrownBy(() -> gateway.getOrderStatus(statusReq, RpcContext.empty()))
                .withMessageContaining("Too Many Requests");
    }

    private AeroflotServicePayload statusRequest(AeroflotVariant variant) {
        return AeroflotServicePayload.builder()
                .variant(variant)
                .partnerId("YaAviaTest")
                .travellers(Collections.emptyList())
                .preliminaryCost(Money.of(0, "RUB"))
                .clientInfo(ClientInfo.builder()
                        .email("mail@example.com")
                        .phone("7123456789")
                        .userAgent("UserAgent")
                        .userIp("127.0.0.1")
                        .build())
                .bookingRef(AeroflotOrderRef.builder()
                        .pnr("pnr")
                        .pnrDate("PNR_DATE")
                        .orderId("order_id")
                        .mdOrderId("md_order_id")
                        .build())
                .build();
    }
}
