package ru.yandex.avia.booking.partners.gateways.aeroflot;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import ru.yandex.avia.booking.enums.PassengerCategory;
import ru.yandex.avia.booking.enums.Sex;
import ru.yandex.avia.booking.partners.gateways.BookingRetryableException;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderCreateResult;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderRef;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderStatus;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.model.availability.PriceChangedException;
import ru.yandex.avia.booking.partners.gateways.model.availability.VariantNotAvailableException;
import ru.yandex.avia.booking.partners.gateways.model.booking.BookingFailureException;
import ru.yandex.avia.booking.partners.gateways.model.booking.TravellerInfo;
import ru.yandex.avia.booking.partners.gateways.model.payment.PaymentFailureReason;
import ru.yandex.avia.booking.remote.RpcContext;
import ru.yandex.avia.booking.tests.wiremock.Wiremock;
import ru.yandex.avia.booking.tests.wiremock.WiremockServerResolver;
import ru.yandex.avia.booking.tests.wiremock.WiremockUri;
import ru.yandex.avia.booking.tests.wiremock.matching.WiremockStartsWithPattern;
import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.defaultAhcClient;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.loadSampleTdRequestNdcV3SingleTariff;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.stubRequest;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.testClientInfo;
import static ru.yandex.travel.testing.misc.TestResources.readResource;

// todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
@ExtendWith(WiremockServerResolver.class)
class AeroflotGatewayInitPaymentTest {
    private AeroflotGateway gateway = AeroflotApiStubsHelper.defaultGateway("url_not_needed");
    private AsyncHttpClientWrapper ahcClientWrapper;

    @BeforeEach
    void before(@WiremockUri String bookingUri) {
        ahcClientWrapper = Mockito.spy(defaultAhcClient());
        gateway = AeroflotApiStubsHelper.defaultGateway(bookingUri, ahcClientWrapper);
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentSuccess3ds(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/order_create_response.xml");

        // testing only partner response parsing here, no real input is required
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        AeroflotOrderCreateResult result = gateway.initPayment(
                variant, testTravellers(), "card1", testClientInfo(), "redirect.url", RpcContext.empty());

        assertThat(result.is3dsRequired()).isTrue();
        assertThat(result.getConfirmationUrl()).isEqualTo("https://pay.test.aeroflot.ru/test-rc/aeropayment/" +
                "3dsRedirect?mdOrder=be204afc-7ab7-4bad-9a9d-0d9c00a08985&language=ru");

        AeroflotOrderRef orderRef = result.getOrderRef();
        assertThat(orderRef.getPnr()).isEqualTo("RCGNJP");
        assertThat(orderRef.getPnrDate()).isEqualTo("PNR_date_2019-01-09");
        assertThat(orderRef.getMdOrderId()).isEqualTo("be204afc-7ab7-4bad-9a9d-0d9c00a08985");
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentSuccessNo3ds(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/order_create_response_no_3ds.xml");

        // testing only partner response parsing here, no real input is required
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());

        AeroflotOrderCreateResult result = gateway.initPayment(
                variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty());
        assertThat(result.getStatusCode()).isEqualTo(AeroflotOrderStatus.PAID_TICKETED);
        assertThat(result.getConfirmationUrl()).isEmpty();

        AeroflotOrderRef orderRef = result.getOrderRef();
        assertThat(orderRef.getPnr()).isEqualTo("IFLFFH");
        assertThat(orderRef.getPnrDate()).isEqualTo("PNR_date_2019-05-13");
        assertThat(orderRef.getMdOrderId()).isEqualTo("36d9c6f9-56cf-4247-a37a-5fb6875eeffa");
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentInvalidEmail(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_create_response_error_317.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(BookingFailureException.class)
                .isThrownBy(() -> gateway.initPayment(
                        variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .withMessageContaining("Client contact is invalid");
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentNotAvailable(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_create_response_error_421.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(VariantNotAvailableException.class)
                .isThrownBy(() -> gateway.initPayment(
                        variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .withMessageContaining("The offer isn't available anymore");
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentNotAvailableDepartureIsTooClose(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_create_response_error_350.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(VariantNotAvailableException.class)
                .isThrownBy(() -> gateway.initPayment(
                        variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .withMessageContaining("The offer isn't available anymore");
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentPriceChanged(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_create_response_error_727.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(PriceChangedException.class)
                .isThrownBy(() -> gateway.initPayment(
                        variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .withMessageContaining("The offer price has changed");
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentRejectedError(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_create_response_error_911_code_2.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatThrownBy(() -> gateway.initPayment(
                variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .isExactlyInstanceOf(AeroflotPaymentException.class)
                .hasMessageContaining("Отказ в проведении оплаты")
                .satisfies(ex -> {
                    AeroflotOrderRef orderRef = ((AeroflotPaymentException) ex).getOrderRef();
                    assertThat(orderRef).isNotNull();
                    assertThat(orderRef.getPnr()).isEqualTo("ROKEYJ");
                    assertThat(orderRef.getPnrDate()).isEqualTo("PNR_date_2019-03-21");
                    assertThat(orderRef.getMdOrderId()).isNull();
                });
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    void initCardRejectedError(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_create_response_error_911_code_2.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatThrownBy(() -> gateway.initPayment(
                variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .isExactlyInstanceOf(AeroflotPaymentException.class)
                .hasMessageContaining("Отказ в проведении оплаты")
                .satisfies(ex -> {
                    AeroflotOrderRef orderRef = ((AeroflotPaymentException) ex).getOrderRef();
                    assertThat(orderRef).isNotNull();
                    assertThat(orderRef.getPnr()).isEqualTo("ROKEYJ");
                    assertThat(orderRef.getPnrDate()).isEqualTo("PNR_date_2019-03-21");
                    assertThat(orderRef.getMdOrderId()).isNull();
                });
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentTokenError(@Wiremock WireMockServer wmServer) {
        String rsp911c26 = readResource("__files/server_responses/aeroflot/errors/order_create_response_error_911_template.xml")
                .replace("%PAYMENT_STATUS%", readResource("__files/server_responses/aeroflot/errors/payment_status_911_code_m26.xml"));
        stubRequest(wmServer, rsp911c26, new WiremockStartsWithPattern("<OrderCreateRQ "));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatThrownBy(() -> gateway.initPayment(
                variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .isExactlyInstanceOf(AeroflotPaymentException.class)
                .hasMessageContaining("Некорректный или устаревший токен")
                .satisfies(ex -> assertThat(((AeroflotPaymentException) ex).getOrderRef()).isNull());
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentUnknownErrorWithPnr(@Wiremock WireMockServer wmServer) {
        String rsp911c26 = readResource("__files/server_responses/aeroflot/errors/order_create_response_error_911_template.xml")
                .replace("%PAYMENT_STATUS%", readResource("__files/server_responses/aeroflot/errors/payment_status_911_code_mUnknown.xml"))
                .replace("%BOOKING_REFERENCE%", readResource("__files/server_responses/aeroflot/errors/booking_reference_semi_failed.xml"));
        stubRequest(wmServer, rsp911c26, new WiremockStartsWithPattern("<OrderCreateRQ "));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatThrownBy(() -> gateway.initPayment(
                variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .isExactlyInstanceOf(AeroflotPaymentException.class)
                .hasMessageContaining("Other registration/payment error")
                .hasMessageContaining("Internal Server Error")
                .satisfies(ex -> {
                    // when we get an error with a PNR we will treat it a payment error to keep the order externally-recoverable
                    AeroflotOrderRef orderRef = ((AeroflotPaymentException) ex).getOrderRef();
                    assertThat(orderRef).isNotNull();
                    assertThat(orderRef.getPnr()).isEqualTo("ROKEYJ");
                    assertThat(orderRef.getPnrDate()).isEqualTo("PNR_date_2019-03-21");
                    assertThat(orderRef.getMdOrderId()).isNull();
                });
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentUnexpectedError(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_create_response_error_911_code_5.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> gateway.initPayment(
                        variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .withMessageContaining("Сервер бронирования недоступен");
    }

    @Test
    void initPaymentCardInvalidDataError(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<?xml version='1.0' encoding='UTF-8'?><IATA_OrderCreateRQ ",
                "server_responses/aeroflot/errors/order_create_response_card_rejected.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatThrownBy(() -> gateway.initPayment(
                variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .isExactlyInstanceOf(AeroflotPaymentException.class)
                .hasMessageContaining("card has been rejected")
                .satisfies(ex -> assertThat(((AeroflotPaymentException) ex).getReason())
                        .isEqualTo(PaymentFailureReason.PAYMENT_REJECTED));
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentTooManyRequests(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<OrderCreateRQ ",
                "server_responses/aeroflot/errors/generic_response_error_429.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        // we don't retry these errors at the moment
        //assertThatExceptionOfType(BookingTooManyRequestsException.class)
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> gateway.initPayment(
                        variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .withMessageContaining("Too Many Requests");
    }

    // todo(tlg-13): TRAVELBACK-1149: the class is being replaced by the v3 tests
    @Disabled
    @Test
    void initPaymentNetworkUnavailable() {
        AeroflotGateway gateway = AeroflotApiStubsHelper.defaultGateway("http://connect-error-test.yandex-team.ru");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(BookingRetryableException.class)
                .isThrownBy(() -> gateway.initPayment(
                        variant, testTravellers(), "card1", testClientInfo(), "rUrl", RpcContext.empty()))
                .withMessageContaining("OrderCreateRQ call hasn't happened");
    }

    private static List<TravellerInfo> testTravellers() {
        return asList(
                mockTraveller("1", PassengerCategory.ADULT),
                mockTraveller("2", PassengerCategory.ADULT),
                mockTraveller("3", PassengerCategory.CHILD)
        );
    }

    private static TravellerInfo mockTraveller(String id, PassengerCategory category) {
        return TravellerInfo
                .builder()
                .travellerInfoId(id)
                .category(category)
                .nationalityCode("RU")
                .sex(Sex.FEMALE)
                .build();
    }
}
