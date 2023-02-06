package ru.yandex.market.pers.pay.client;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Supplier;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.dto.PaymentOfferDto;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.pay.client.PersPayConstants.PAY_INTERNAL_KEY;
import static ru.yandex.market.pers.pay.model.PersPayEntityType.MODEL_GRADE;
import static ru.yandex.market.pers.pay.model.PersPayUserType.UID;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.mockResponseWithFile;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withHeader;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 05.03.2021
 */
public class PersPayClientTest {
    private final Supplier<String> tvmSupplier = mock(Supplier.class);
    private final HttpClient httpClient = mock(HttpClient.class);
    private final PersPayClient client = new PersPayClient("http://localhost:1234",
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)),
        tvmSupplier);

    @Test
    public void testFindPayments() {
        List<String> payKeys = List.of(
            PersPayConstants.buildPayKey(new PersPayUser(UID, 123), new PersPayEntity(MODEL_GRADE, 998)),
            PersPayConstants.buildPayKey(new PersPayUser(UID, 222), new PersPayEntity(MODEL_GRADE, 425))
        );

        mockResponseWithFile(
            httpClient,
            200,
            "/data/found_payments.json",
            and(
                withMethod(HttpMethod.GET),
                withPath("/pay/grade/check"),
                withQueryParam(PAY_INTERNAL_KEY, "0-123-1-998"),
                withQueryParam(PAY_INTERNAL_KEY, "0-222-1-425"),
                withHeader(PersPayClient.SERVICE_TICKET_HEADER, null)
            ));

        List<PaymentOfferDto> payments = client.findPayments(payKeys);
        assertEquals(2, payments.size());

        PaymentOfferDto dto = payments.get(0);
        assertEquals(UID, dto.getUserType());
        assertEquals("123", dto.getUserId());
        assertEquals(MODEL_GRADE, dto.getEntityType());
        assertEquals("998", dto.getEntityId());
        assertEquals(PersPayerType.MARKET, dto.getPayerType());
        assertEquals("EXP", dto.getPayerId());
        assertEquals(30, dto.getAmount().intValue());
    }

    @Test
    public void testTvm() {
        List<String> payKeys = List.of("2", "3");

        when(tvmSupplier.get()).thenReturn("token");

        mockResponseWithFile(
            httpClient,
            200,
            "/data/found_payments.json",
            and(
                withPath("/pay/grade/check"),
                withQueryParam(PAY_INTERNAL_KEY, "2"),
                withQueryParam(PAY_INTERNAL_KEY, "3"),
                withHeader(PersPayClient.SERVICE_TICKET_HEADER, "token")
            ));

        List<PaymentOfferDto> payments = client.findPayments(payKeys);
        assertEquals(2, payments.size());
    }
}
