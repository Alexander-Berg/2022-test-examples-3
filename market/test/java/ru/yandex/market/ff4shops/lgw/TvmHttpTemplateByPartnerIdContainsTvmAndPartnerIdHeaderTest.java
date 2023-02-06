package ru.yandex.market.ff4shops.lgw;

import java.util.HashMap;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TvmHttpTemplateByPartnerIdContainsTvmAndPartnerIdHeaderTest extends FunctionalTest {

    private static final String PARTNER_ID_HEADER = "X-Partner-Id";
    private static final long PARTNER_ID = 912459L;

    private static final String SERVICE_TICKET_HEADER = "X-Ya-Service-Ticket";
    private static final String TVM_TICKET = "tvm-header-value";
    private static final String BASE_URL = "http://some.link.net";
    private static final String RELATIVE_PATH = "gateway";
    private static final String FULL_URL = BASE_URL + "/" + RELATIVE_PATH;
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";

    private RestTemplate restTemplate;
    private TvmTicketProvider tvmTicketProvider;

    private TvmHttpTemplateByPartnerId tvmHttpTemplateByPartnerId;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        tvmTicketProvider = mock(TvmTicketProvider.class);
        tvmHttpTemplateByPartnerId = new TvmHttpTemplateByPartnerId(
            BASE_URL,
            restTemplate,
            MediaType.TEXT_XML,
            tvmTicketProvider,
            PARTNER_ID
        );
        doReturn(ResponseEntity.ok(RESPONSE))
            .when(restTemplate)
            .exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        doReturn(TVM_TICKET)
            .when(tvmTicketProvider)
            .provideServiceTicket();
    }

    @Test
    void executePost() {
        tvmHttpTemplateByPartnerId.executePost(REQUEST, String.class, RELATIVE_PATH);

        verifyRestTemplate(HttpMethod.POST);
    }

    @Test
    void executePostNoResponse() {
        tvmHttpTemplateByPartnerId.executePost(REQUEST, RELATIVE_PATH);

        verifyRestTemplate(HttpMethod.POST);
    }

    @Test
    void executeGet() {
        tvmHttpTemplateByPartnerId.executeGet(String.class, new HashMap<>(), RELATIVE_PATH);

        verifyRestTemplate(HttpMethod.GET);
    }

    private void verifyRestTemplate(HttpMethod httpMethod) {
        verify(restTemplate).exchange(
            eq(FULL_URL),
            eq(httpMethod),
            argThat((HttpEntity<String> entity) ->
                Objects.equals(entity.getHeaders().getFirst(SERVICE_TICKET_HEADER), TVM_TICKET) &&
                    Objects.equals(entity.getHeaders().getFirst(PARTNER_ID_HEADER), String.valueOf(PARTNER_ID))
            ),
            any(Class.class)
        );
    }

}
