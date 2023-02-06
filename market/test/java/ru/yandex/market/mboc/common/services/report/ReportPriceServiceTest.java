package ru.yandex.market.mboc.common.services.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicStatusLine;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author galaev@yandex-team.ru
 * @since 26/07/2018.
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ReportPriceServiceTest {

    private final long testSkuId = 100210224492L;
    private ReportPriceService reportPriceService;
    private HttpClient httpClient;

    @Before
    public void setup() {
        httpClient = Mockito.mock(HttpClient.class);
        reportPriceService = new ReportPriceService(httpClient);
    }

    @Test
    public void testServiceWorks() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class)))
            .thenAnswer(invocation -> getHttpResponse("report/good_response.json"));

        Offer offer = createTestOffer();
        List<String> logMessages = new ArrayList<>();
        reportPriceService.fetchPrices(Collections.singletonList(offer), logMessages::add);
        Assertions.assertThat(offer.getReferencePrice()).isEqualTo(5290);
        Assertions.assertThat(offer.getBeruPrice()).isEqualTo(5990);
        Assertions.assertThat(logMessages).contains("Получение цен из репорта для 1 офферов");
    }

    @Test
    public void testNullMappings() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class)))
            .thenAnswer(invocation -> getHttpResponse("report/good_response.json"));

        Offer offer = new Offer();
        reportPriceService.fetchPrices(Collections.singletonList(offer), log -> {
        });
        Assertions.assertThat(offer.getReferencePrice()).isNull();
        Assertions.assertThat(offer.getBeruPrice()).isNull();
    }

    @Test
    public void testBadResponse() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class)))
            .thenAnswer(invocation -> getHttpResponse("report/bad_response.xml"));

        Offer offer = createTestOffer();
        List<String> logMessages = new ArrayList<>();
        reportPriceService.fetchPrices(Collections.singletonList(offer), logMessages::add);
        Assertions.assertThat(offer.getReferencePrice()).isNull();
        Assertions.assertThat(offer.getBeruPrice()).isNull();
        Assertions.assertThat(logMessages).contains("В репорте отсутствуют офферы для sku 100210224492");
    }

    @Test
    public void testException() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class)))
            .thenAnswer(invocation -> {
                throw new IOException();
            });

        Offer offer = createTestOffer();
        List<String> logMessages = new ArrayList<>();
        reportPriceService.fetchPrices(Collections.singletonList(offer), logMessages::add);
        Assertions.assertThat(offer.getReferencePrice()).isNull();
        Assertions.assertThat(offer.getBeruPrice()).isNull();
        Assertions.assertThat(logMessages).contains("Ошибка получения цены для sku 100210224492");
    }

    private Offer createTestOffer() {
        return new Offer()
            .setId(1)
            .setTitle("Offer")
            .setSuggestSkuMapping(new Offer.Mapping(testSkuId, DateTimeUtils.dateTimeNow()));
    }

    private HttpResponse getHttpResponse(String fileName) {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getStatusLine())
            .thenAnswer(i -> new BasicStatusLine(new ProtocolVersion("stub", 1, 1), SC_OK, "Its okay"));
        Mockito.when(response.getEntity()).thenAnswer(i -> {
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(getClass().getClassLoader().getResourceAsStream(fileName));
            return entity;
        });
        return response;
    }
}
