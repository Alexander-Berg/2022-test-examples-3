package ru.yandex.direct.inventori;

import java.time.Duration;
import java.util.UUID;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.PageBlock;
import ru.yandex.direct.inventori.model.request.ProfileCorrection;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.VideoCreative;
import ru.yandex.direct.inventori.model.response.IndoorPredictionResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Ignore("Тест затрагивает реально работающую систему")
public class InventoriClientGetIndoorPredictionTest {

    private static final String DEFAULT_REQUEST_ID = UUID.randomUUID().toString().toUpperCase();

    private InventoriClient inventoriClient;
    private String url;
    private DefaultAsyncHttpClient asyncHttpClient;

    @Before
    public void setUp() throws Exception {
        url = "http://inventori-test.yandex-team.ru/api/";
        asyncHttpClient = new DefaultAsyncHttpClient();
        inventoriClient = createInventoriClient(asyncHttpClient, url, Duration.ofMinutes(1));
    }

    @Test
    public void successFull() {
        Target target = new Target()
                .withGroupType(GroupType.INDOOR)
                .withVideoCreatives(asList(
                        new VideoCreative(60000, null, Sets.newSet(new BlockSize(16, 9), new BlockSize(4, 3))),
                        new VideoCreative(120000, null, Sets.newSet(new BlockSize(16, 9), new BlockSize(4, 3)))
                ))
                .withPageBlocks(asList(
                        new PageBlock(1L, asList(2L, 3L)),
                        new PageBlock(4L, asList(5L, 6L))
                ))
                .withProfileCorrections(asList(
                        ProfileCorrection.builder()
                                .withGender(ProfileCorrection.Gender.MALE)
                                .withAge(ProfileCorrection.Age._0_17)
                                .withCorrection(110)
                                .build(),
                        ProfileCorrection.builder()
                                .withAge(ProfileCorrection.Age._18_24)
                                .withCorrection(120)
                                .build(),
                        ProfileCorrection.builder()
                                .withGender(ProfileCorrection.Gender.FEMALE)
                                .withCorrection(130)
                                .build()
                ))
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withExcludedDomains(emptySet())
                .withRegions(emptySet());

        IndoorPredictionResponse response = getIndoorPrediction(target);

        assertThat(response, is(notNullValue()));
        assertThat(response.getErrors(), nullValue());
    }

    @Test
    public void successMinimal() {
        Target target = new Target()
                .withGroupType(GroupType.INDOOR)
                .withVideoCreatives(emptyList())
                .withPageBlocks(emptyList())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withExcludedDomains(emptySet())
                .withRegions(emptySet());

        IndoorPredictionResponse response = getIndoorPrediction(target);

        assertThat(response, is(notNullValue()));
        assertThat(response.getErrors(), nullValue());
    }

    @Test(expected = InventoriException.class)
    public void timeout() {
        inventoriClient = createInventoriClient(asyncHttpClient, url, Duration.ofMillis(1));
        Target target = defaultTarget();
        getIndoorPrediction(target);
    }

    private InventoriClient createInventoriClient(AsyncHttpClient asyncHttpClient, String url,
                                                  Duration requestTimeout) {
        return new InventoriClient(asyncHttpClient,
                new InventoriClientConfig(url, 1, requestTimeout, 1));
    }

    private IndoorPredictionResponse getIndoorPrediction(Target target) {
        return inventoriClient.getIndoorPrediction(DEFAULT_REQUEST_ID, target, 7L, 8L, "login", null);
    }

    private Target defaultTarget() {
        return new Target()
                .withGroupType(GroupType.INDOOR)
                .withVideoCreatives(emptyList())
                .withPageBlocks(emptyList())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList())
                .withExcludedDomains(emptySet())
                .withRegions(emptySet());
    }
}
