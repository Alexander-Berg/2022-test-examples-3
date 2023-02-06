package ru.yandex.travel.hotels.common.partners.bnovo;

import java.time.Duration;
import java.util.Map;

import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.bnovo.model.LegalEntitiesResponse;
import ru.yandex.travel.hotels.common.partners.bnovo.model.LegalEntityResponse;
import ru.yandex.travel.hotels.common.partners.bnovo.model.RoomType;

// for manual local tests
@Ignore
@Slf4j
public class DefaultBnovoClientLocalTest {
    private AsyncHttpClient ahc;
    private DefaultBNovoClient client;

    @Before
    public void init() {
        ahc = Dsl.asyncHttpClient();
        AsyncHttpClientWrapper ahcWrapper = new AsyncHttpClientWrapper(
                ahc, log, "bnovo", new MockTracer(), DefaultBNovoClient.getMethods().getNames());
        BNovoClientProperties properties = new BNovoClientProperties();
        // prod & testing have the same settings except for the client name
        properties.setBaseUrl("https://public-api.reservationsteps.ru/v1/api");
        properties.setPrivateApiBaseUrl("https://api.reservationsteps.ru/v1/api");
        properties.setPricesLosApiBaseUrl("https://los-api.reservationsteps.ru/v1/api");
        properties.setHttpRequestTimeout(Duration.ofMillis(15000));
        properties.setHttpReadTimeout(Duration.ofMillis(15000));
        properties.setTokenValidityDuration(Duration.ofMinutes(59));
        // prod user name: travel-hotels-partners@yandex-team.ru
        //properties.setUsername("___USER_NAME___");
        // prod pw: 'bnovo-new-password' from https://yav.yandex-team.ru/secret/sec-01ddqrmtm6vyehw2n4wz3qan16/explore/version/ver-01g8std8rsrsj73bjt97349cm8
        //properties.setPassword("___PASSWORD___");
        client = new DefaultBNovoClient(ahcWrapper, properties, new Retry(new MockTracer()));
    }

    @After
    public void destroy() throws Exception {
        ahc.close();
    }

    @Test
    public void testRoomTypes() {
        Map<Long, RoomType> types = client.getRoomTypesSync(1999, null);
        log.info("Room type: {}", types.get((long) 23519));
    }

    @Test
    public void testLegalEntities() {
        LegalEntitiesResponse resp = client.getLegalEntitiesSync(1946, null);
        log.info("Legal Entities: {}", resp.getLegalEntities());
    }

    @Test
    public void testLegalEntity() {
        LegalEntityResponse resp = client.getLegalEntitySync(1946, 1702, null);
        log.info("Legal Entity: {}", resp.getLegalEntity());
    }

}
