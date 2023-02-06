package ru.yandex.market.checkout.checkouter.trust.service;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.trust.DarkSpiritTvmTicketProvider;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.TrustEndpointsUidToggles;
import ru.yandex.market.checkout.checkouter.trust.service.impl.HttpTrustPaymentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.common.web.CheckoutHttpParameters.SERVICE_TICKET_HEADER;

public class TestDarkSpiritTvm extends AbstractWebTestBase {

    private static final String TVM_TOKEN = "123";
    private static final String OLD_DARK_SPIRIT_HOST = "https://darkspirit.paysys.yandex.net:999/v1/some_method/555";
    private static final String OLD_TESTING_SPIRIT_HOST = "https://greed-ts.paysys.yandex.net:99/v1/some_method/5559";
    private static final String DARK_SPIRIT_HOST = "https://darkspirit.yandex.net/v1/some_method/555";
    private static final String TESTING_SPIRIT_HOST = "https://darkspirit.testing.yandex.net/v1/some_method/5559";

    @Mock
    private HttpClient mockClient;

    @Captor
    private ArgumentCaptor<HttpUriRequest> uriCaptor;

    @Autowired
    private TrustEndpointsUidToggles trustEndpointsUidToggles;

    private TrustPaymentService trustPaymentService;

    @BeforeEach
    void setUp() {
        var mockTvmProvider = Mockito.mock(DarkSpiritTvmTicketProvider.class);
        Mockito.doReturn(Optional.of(TVM_TOKEN)).when(mockTvmProvider).getServiceTicket();
        MockitoAnnotations.initMocks(this);
        trustPaymentService = new HttpTrustPaymentService(
                mockClient, "not_used", mockTvmProvider, trustEndpointsUidToggles);
    }

    @ParameterizedTest
    @ValueSource(strings = {DARK_SPIRIT_HOST, TESTING_SPIRIT_HOST, OLD_DARK_SPIRIT_HOST, OLD_TESTING_SPIRIT_HOST})
    void testTvmAppending(String host) throws IOException {
        getReceipt(host);
        Mockito.verify(mockClient).execute(uriCaptor.capture());
        var request = uriCaptor.getValue();
        assertTrue(request.containsHeader(SERVICE_TICKET_HEADER));
        assertEquals(TVM_TOKEN, request.getFirstHeader(SERVICE_TICKET_HEADER).getValue());
    }

    private void getReceipt(String url) {
        try {
            trustPaymentService.getPaymentReceiptRaw((Long) null, url);
        } catch (NullPointerException expected) {
            // that's ok and expected
        }
    }

}
