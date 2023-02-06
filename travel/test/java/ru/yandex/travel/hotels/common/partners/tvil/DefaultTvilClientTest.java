package ru.yandex.travel.hotels.common.partners.tvil;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.tvil.model.TvilOffer;
import ru.yandex.travel.hotels.common.partners.tvil.model.TvilSearchRequest;
import ru.yandex.travel.hotels.common.partners.tvil.model.TvilSearchRequest.TvilGuests;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class DefaultTvilClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderClasspath("fixtures/tvil"));

    private DefaultTvilClient client;

    @Before
    public void init() {
        AsyncHttpClientWrapper clientWrapper = new AsyncHttpClientWrapper(new DefaultAsyncHttpClient(),
                log, "tvil", new MockTracer(), null);
        TvilClientProperties clientProperties = new TvilClientProperties();
        clientProperties.setHttpRequestTimeout(Duration.ofMillis(2000));
        clientProperties.setHttpReadTimeout(Duration.ofMillis(2000));
        clientProperties.setBaseUrl(String.format("http://localhost:%s/yandexTravel", wireMockRule.port()));
        client = new DefaultTvilClient(clientProperties, clientWrapper, new Retry(new MockTracer()));
    }

    @Test
    public void searchOffersSimple() {
        var results = client.searchOffersSync(TvilSearchRequest.builder()
                .hotelIds(Set.of("424275", "424280"))
                .checkIn(LocalDate.parse("2020-07-27"))
                .checkOut(LocalDate.parse("2020-08-04"))
                .guests(List.of(TvilGuests.builder().adults(2).childrenAges(List.of(5, 2)).build()))
                .language("ru")
                .currency("RUB")
                .userCountry("RU")
                .build());
        assertThat(results.keySet()).isEqualTo(Set.of("424275", "424280"));

        var hotels1 = results.get("424275");
        assertThat(hotels1).hasSize(2);
        assertThat(hotels1.stream().map(TvilOffer::getRoomId).collect(Collectors.toList()))
                .isEqualTo(List.of(433908, 433909));
        assertThat(hotels1.get(0)).satisfies(offer -> {
            assertThat(offer.getUrl()).startsWith("https://kakugodno.tvil.ru/city/divnomorskoe/hotels/424275/?");
            assertThat(offer.getName()).isEqualTo("Трехместный стандарт");
            assertThat(offer.getBreakfast()).isFalse();
            assertThat(offer.getSmoking()).isFalse();
            assertThat(offer.getRefundable()).isTrue();
            assertThat(offer.getAvailable()).isEqualTo(1);
            assertThat(offer.getPrice()).isEqualTo(new BigDecimal(30600));
            assertThat(offer.getRoomId()).isEqualTo(433908);
            assertThat(offer.getRoomTypeId()).isEqualTo(32);
        });
        assertThat(results.get("424280").get(0)).satisfies(offer -> {
            assertThat(offer.getName()).isEqualTo("Двухкомнатный номер без балкона");
            assertThat(offer.getBreakfast()).isTrue();
            assertThat(offer.getPrice()).isEqualTo(new BigDecimal(49900));
        });

        var hotels2 = results.get("424280");
        assertThat(hotels2).hasSize(1);
        assertThat(hotels2.get(0).getRoomId()).isEqualTo(433934);
    }

    @Test
    public void searchOffersEmpty() {
        var results = client.searchOffersSync(TvilSearchRequest.builder()
                .hotelIds(Set.of("424276"))
                .checkIn(LocalDate.parse("2020-07-27"))
                .checkOut(LocalDate.parse("2020-08-04"))
                .guests(List.of(TvilGuests.builder().adults(1).build()))
                .language("ru")
                .currency("RUB")
                .userCountry("RU")
                .build());
        assertThat(results).isEmpty();
    }
}
