package ru.yandex.direct.inventori;

import java.time.Duration;
import java.util.UUID;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.PageBlock;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.VideoCreative;
import ru.yandex.direct.inventori.model.response.OutdoorPredictionResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Ignore("Тест затрагивает реально работающую систему")
public class InventoriClientGetOutdoorPredictionTest {

    private static final String DEFAULT_REQUEST_ID = UUID.randomUUID().toString().toUpperCase();

    private InventoriClient inventoriClient;
    private String url;
    private DefaultAsyncHttpClient asyncHttpClient;

    @Before
    public void setUp() throws Exception {
        url = "http://inventori-test.common.yandex.net/api/";
        asyncHttpClient = new DefaultAsyncHttpClient();
        inventoriClient = createInventoriClient(asyncHttpClient, url, Duration.ofMinutes(1));
    }

    @Test
    public void successFull() {
        Target target = new Target()
                .withGroupType(GroupType.OUTDOOR)
                .withVideoCreatives(asList(
                        new VideoCreative(60000, new BlockSize(970, 250), null),
                        new VideoCreative(120000, new BlockSize(1000, 120), null)
                ))
                .withPageBlocks(asList(
                        new PageBlock(1L, asList(2L, 3L)),
                        new PageBlock(4L, asList(5L, 6L))
                ))
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withExcludedDomains(emptySet())
                .withRegions(emptySet());

        OutdoorPredictionResponse response = getOutdoorPrediction(target);

        assertThat(response, is(notNullValue()));
        assertThat(response.getErrors(), nullValue());
    }

    @Test
    public void successMinimal() {
        Target target = new Target()
                .withGroupType(GroupType.OUTDOOR)
                .withVideoCreatives(emptyList())
                .withPageBlocks(emptyList())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withExcludedDomains(emptySet())
                .withRegions(emptySet());

        OutdoorPredictionResponse response = getOutdoorPrediction(target);

        assertThat(response, is(notNullValue()));
        assertThat(response.getErrors(), nullValue());
    }

    @Test(expected = InventoriException.class)
    public void timeout() {
        inventoriClient = createInventoriClient(asyncHttpClient, url, Duration.ofMillis(1));
        Target target = defaultTarget();

        inventoriClient.getOutdoorPrediction(DEFAULT_REQUEST_ID, target, null, null, "login", null);
    }

    private InventoriClient createInventoriClient(AsyncHttpClient asyncHttpClient, String url,
                                                  Duration requestTimeout) {
        return new InventoriClient(asyncHttpClient,
                new InventoriClientConfig(url, 1, requestTimeout, 1));
    }

    private OutdoorPredictionResponse getOutdoorPrediction(Target target) {
        return inventoriClient.getOutdoorPrediction(DEFAULT_REQUEST_ID, target, 7L, 8L, "login", null);
    }

    private Target defaultTarget() {
        return new Target()
                .withGroupType(GroupType.OUTDOOR)
                .withVideoCreatives(emptyList())
                .withPageBlocks(emptyList())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withExcludedDomains(emptySet())
                .withRegions(emptySet());
    }
}
