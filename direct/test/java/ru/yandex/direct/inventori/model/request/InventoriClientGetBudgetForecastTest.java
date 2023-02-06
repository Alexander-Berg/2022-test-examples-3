package ru.yandex.direct.inventori.model.request;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.InventoriClientConfig;
import ru.yandex.direct.inventori.model.response.CampaignPredictionAvailableResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionLowReachResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Тест затрагивает реально работающую систему")
public class InventoriClientGetBudgetForecastTest {
    private static final String LOGIN = "test";
    private static final String REQUEST_ID = UUID.randomUUID().toString().toUpperCase();
    private InventoriClient client;
    private CampaignParameters parameters;

    @Before
    public void setUp() throws Exception {
        InventoriClientConfig config = new InventoriClientConfig(
                "https://inventori-test.common.yandex.net/api/",
                3,
                Duration.ofSeconds(10),
                1);
        AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
        client = new InventoriClient(httpClient, config);

        CampaignParametersRf rf = new CampaignParametersRf(5, 0);
        CampaignParametersSchedule schedule = CampaignParametersSchedule.builder()
                .withStrategyType(StrategyType.MIN_CPM)
                .withBudget(10000000000L)
                .withStartDate("2018-08-01")
                .withEndDate("2018-08-31")
                .build();
        parameters = new CampaignParameters(schedule, rf);
    }

    @Test
    public void correctResponseTest() throws Exception {
        List<Target> targets = singletonList(
                new Target()
                        .withBlockSizes(singletonList(new BlockSize(240, 400)))
                        .withExcludedDomains(emptySet())
                        .withCryptaGroups(asList(
                                new CryptaGroup(ImmutableSet.of("617:2")),
                                new CryptaGroup(ImmutableSet.of("618:4"))))
                        .withAudienceGroups(emptyList())
                        .withRegions(ImmutableSet.of(1, 10174)));
        CampaignPredictionRequest request = new CampaignPredictionRequest(null, null,
                InventoriCampaignType.MEDIA_RSYA, targets, parameters, null,
                null, null);
        CampaignPredictionResponse response = client.getCampaignPrediction(REQUEST_ID, LOGIN, LOGIN, request);
        assertThat(response)
                .isInstanceOf(CampaignPredictionAvailableResponse.class);
    }

    @Test
    public void lessThanResponseTest() throws Exception {
        List<Target> targets = singletonList(
                new Target()
                        .withGroupType(GroupType.VIDEO)
                        .withBlockSizes(singletonList(new BlockSize(160, 600)))
                        .withExcludedDomains(ImmutableSet.of(""))
                        .withCryptaGroups(asList(
                                new CryptaGroup(ImmutableSet.of("616:1")),
                                new CryptaGroup(ImmutableSet.of("617:0")),
                                new CryptaGroup(ImmutableSet.of("618:4")),
                                new CryptaGroup(ImmutableSet.of("547:1023")),
                                new CryptaGroup(ImmutableSet.of("547:1025")),
                                new CryptaGroup(ImmutableSet.of("547:1037", "547:1033", "547:1032", "547:1040")),
                                new CryptaGroup(
                                        ImmutableSet.of("602:1", "601:1", "601:55", "602:55", "602:141", "601:173",
                                                "601:141", "602:183", "602:173", "601:183"))))
                        .withAudienceGroups(emptyList())
                        .withRegions(ImmutableSet.of(255)));
        CampaignPredictionRequest request = new CampaignPredictionRequest(null, null,
                InventoriCampaignType.MEDIA_RSYA, targets, parameters, null,
                null, null);
        CampaignPredictionResponse response = client.getCampaignPrediction(REQUEST_ID, LOGIN, LOGIN, request);
        assertThat(response)
                .isInstanceOf(CampaignPredictionLowReachResponse.class);
    }
}
