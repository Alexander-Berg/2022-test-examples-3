package ru.yandex.market.checkout.util.b2b;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.b2b.B2bCustomersApi;
import ru.yandex.market.checkout.checkouter.b2b.B2bCustomersSlowApi.PaymentInvoiceResponse;
import ru.yandex.market.checkout.checkouter.log.CheckouterLogs;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.util.GenericMockHelper;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static ru.yandex.market.checkout.checkouter.log.LoggingSystem.ACTUALIZATION;

@TestComponent
public class B2bCustomersMockConfigurer {

    public static final PaymentInvoiceResponse DEFAULT_PAYMENT_INVOICE =
            new PaymentInvoiceResponse("http://ссылка-на-mds-aka-ответ-от-b2b-customers");
    public static final String POST_GENERATE_PAYMENT_INVOICE = "GeneratePaymentInvoice";
    public static final String POST_GENERATE_MULTI_PAYMENT_INVOICE = "GenerateMultiPaymentInvoice";
    public static final String GET_IS_CLIENT_CAN_ORDER = "IsClientCanOrder";
    public static final String RESERVATION_DATE = "ReservationDate";
    private static final Logger LOG = CheckouterLogs.getMainLog(ACTUALIZATION, B2bCustomersMockConfigurer.class);
    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private WireMockServer b2bCustomersMock;

    public void mockGeneratePaymentInvoice() {
        MappingBuilder builder = post(urlPathMatching("/orders/([0-9]*)/payment-invoice"))
                .withName(POST_GENERATE_PAYMENT_INVOICE)
                .willReturn(okJson(testSerializationService.serializeCheckouterObject(DEFAULT_PAYMENT_INVOICE)));
        b2bCustomersMock.stubFor(builder);
    }

    public void mockGenerateMultiPaymentInvoice() {
        MappingBuilder builder = post(urlPathEqualTo("/orders/generate-payment-invoice"))
                .withName(POST_GENERATE_MULTI_PAYMENT_INVOICE)
                .willReturn(okJson(testSerializationService.serializeCheckouterObject(DEFAULT_PAYMENT_INVOICE)));
        b2bCustomersMock.stubFor(builder);
    }

    /**
     * @param uid               паспортный идентификатор пользователя {@link Buyer#uid}.
     * @param businessBalanceId идентификатор в Балансе {@link Buyer#businessBalanceId}.
     * @param canOrder          может или нет пользователь делать заказ.
     */
    public void mockIsClientCanOrder(long uid, long businessBalanceId, boolean canOrder) {
        try {
            MappingBuilder builder = get(urlPathEqualTo(
                    "/users/" + uid + "/customers/" + businessBalanceId + "/check"))
                    .withName(GET_IS_CLIENT_CAN_ORDER)
                    .willReturn(okJson(new ObjectMapper().writeValueAsString(
                            new B2bCustomersApi.ClientCanOrderResponse(
                                    Long.toString(uid),
                                    canOrder))));
            b2bCustomersMock.stubFor(builder);
        } catch (JsonProcessingException e) {
            LOG.error("Error occurred when serializing to Json.", e);
        }
    }

    public void mockReservationDate(LocalDate reservationDate) {
        MappingBuilder builder = get(urlPathMatching("/orders/reservation-date/\\d{4}-\\d{2}-\\d{2}"))
                .withName(RESERVATION_DATE)
                .willReturn(okJson("{\"payDate\":\"" + reservationDate.toString() + "\"}"));
        b2bCustomersMock.stubFor(builder);
    }

    public List<ServeEvent> servedEvents() {
        return GenericMockHelper.servedEvents(b2bCustomersMock);
    }

    public List<ServeEvent> findEventsByStubName(String stubName) {
        return createEventStreamFilterByStubName(stubName)
                .collect(Collectors.toList());
    }

    public Stream<ServeEvent> createEventStreamFilterByStubName(
            String stubName
    ) {
        return GenericMockHelper.servedEvents(b2bCustomersMock).stream()
                .filter(event -> event.getStubMapping().getName().equals(stubName));
    }

    public void resetAll() {
        b2bCustomersMock.resetAll();
    }
}
