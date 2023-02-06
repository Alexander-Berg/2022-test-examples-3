package ru.yandex.travel.hotels.common.partners.tvil;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.tvil.model.TvilOffer;
import ru.yandex.travel.hotels.common.partners.tvil.model.TvilSearchRequest;

/**
 * cURL Example:
 * {@code curl -k 'https://chm-kakugodno.tvil.ru/yandexTravel/availability?hotels=424275,
 * 424280&check_in=2020-07-01&check_out=2020-07-07&guests=1&language=ru&currency=RUB&user_country=RU'}
 */
@Ignore
@Slf4j
public class DefaultTvilClientRealTests {
    private AsyncHttpClient ahcClient = Dsl.asyncHttpClient(Dsl.config()
            .setThreadPoolName("tvilRealTestsAhcPool")
            .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
            .build());
    private AsyncHttpClientWrapper ahcWrapper = new AsyncHttpClientWrapper(ahcClient,
            log, "real_tvil_api", new MockTracer());
    private TvilClientProperties properties = TvilClientProperties.builder()
            .baseUrl("https://chm-kakugodno.tvil.ru/yandexTravel")
            //.baseUrl("http://localhost:4242/tvil/yandexTravel")
            .httpRequestTimeout(Duration.ofSeconds(30))
            .httpReadTimeout(Duration.ofSeconds(30))
            .build();
    private DefaultTvilClient client = new DefaultTvilClient(properties, ahcWrapper, new Retry(new MockTracer()));

    @Test
    public void tests() {
        LocalDate from = LocalDate.now().plusDays(100);
        var offers = client.searchOffersSync(TvilSearchRequest.builder()
                .hotelIds(Set.of("42427", "424280"))
                .checkIn(from)
                .checkOut(from.plusDays(7))
                .guests(List.of(TvilSearchRequest.TvilGuests.builder()
                        .adults(1)
                        //.childrenAges(List.of(1, 2))
                        .build()))
                .language("ru")
                .currency("RUB")
                .userCountry("RU")
                .build());
        for (Map.Entry<String, List<TvilOffer>> entry : offers.entrySet()) {
            log.info("HOTEL: {}", entry.getKey());
            for (TvilOffer value : entry.getValue()) {
                log.info("\t{}", value);
            }
        }
    }
}
