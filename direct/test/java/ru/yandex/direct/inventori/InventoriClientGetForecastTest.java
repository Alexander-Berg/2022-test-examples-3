package ru.yandex.direct.inventori;

import java.time.Duration;
import java.util.UUID;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.inventori.model.request.CryptaGroup;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.response.ForecastResponse;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Ignore("Тест затрагивает реально работающую систему")
public class InventoriClientGetForecastTest {

    private static final String DEFAULT_REQUEST_ID = UUID.randomUUID().toString().toUpperCase();
    private static final int DEFAULT_REGION_ID = 3;
    private static final String UNKNOWN_CRYPTA_SEGMENT_ID = "-1";
    private InventoriClient inventoriClient;
    private String url;
    private DefaultAsyncHttpClient asyncHttpClient;

    @Before
    public void setUp() throws Exception {
        this.url = "http://inventori-test.common.yandex.net/api/";
        Duration requestTimeout = Duration.ofMinutes(1);
        this.asyncHttpClient = new DefaultAsyncHttpClient();
        this.inventoriClient = new InventoriClient(asyncHttpClient,
                new InventoriClientConfig(url, 1, requestTimeout, 1));
    }

    @Test
    public void success() {
        Target target = new Target()
                .withBlockSizes(emptyList())
                .withExcludedDomains(emptySet())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withRegions(singleton(DEFAULT_REGION_ID));
        final ForecastResponse forecast = this.inventoriClient.getForecast(DEFAULT_REQUEST_ID, target,
                null, null, "login", null);
        assertThat(forecast, is(notNullValue()));
    }

    @Test
    public void successWhenSendVideoCreatives() {
        Target target = new Target()
                .withVideoCreatives(emptyList())
                .withExcludedDomains(emptySet())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withRegions(singleton(DEFAULT_REGION_ID));
        final ForecastResponse forecast = this.inventoriClient.getForecast(DEFAULT_REQUEST_ID, target,
                null, null, "login", null);
        assertThat(forecast, is(notNullValue()));
        assertThat(forecast.getErrors(), nullValue());
    }

    @Test
    public void errorsWhenSendBlockSizesAndVideoCreatives() {
        Target target = new Target()
                .withBlockSizes(emptyList())
                .withVideoCreatives(emptyList())
                .withExcludedDomains(emptySet())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withRegions(singleton(DEFAULT_REGION_ID));
        final ForecastResponse forecast = this.inventoriClient.getForecast(DEFAULT_REQUEST_ID, target,
                null, null, "login", null);
        assertThat(forecast.getErrors(), is(notNullValue()));
    }

    @Test
    public void successOn400() {
        Target target = new Target()
                .withBlockSizes(emptyList())
                .withExcludedDomains(emptySet())
                .withCryptaGroups(singletonList(new CryptaGroup(singleton(UNKNOWN_CRYPTA_SEGMENT_ID))))
                .withAudienceGroups(emptyList())
                .withRegions(emptySet());
        final ForecastResponse forecast = this.inventoriClient.getForecast(DEFAULT_REQUEST_ID, target,
                null, null, "login", null);
        assertThat(forecast.getErrors(), is(notNullValue()));
    }

    @Test(expected = InventoriException.class)
    public void timeout() {
        Duration requestTimeout = Duration.ofMillis(1);
        this.inventoriClient = new InventoriClient(asyncHttpClient,
                new InventoriClientConfig(url, 1, requestTimeout, 1));

        Target target = new Target()
                .withBlockSizes(emptyList())
                .withExcludedDomains(emptySet())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withRegions(singleton(DEFAULT_REGION_ID));
        final ForecastResponse forecast = this.inventoriClient.getForecast(DEFAULT_REQUEST_ID, target,
                null, null, "login", null);
        assertThat(forecast, is(notNullValue()));
    }

}
