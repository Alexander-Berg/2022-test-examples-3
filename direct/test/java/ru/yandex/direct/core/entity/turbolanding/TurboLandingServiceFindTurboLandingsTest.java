package ru.yandex.direct.core.entity.turbolanding;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository;
import ru.yandex.direct.core.entity.turbolanding.service.TurboLandingService;
import ru.yandex.direct.core.entity.turbolanding.service.UpdateCounterGrantsService;
import ru.yandex.direct.core.entity.turbolanding.service.validation.TurboLandingValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.turbolandings.client.TurboLandingsClient;
import ru.yandex.direct.turbolandings.client.model.GetIdByUrlResponseItem;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TurboLandingServiceFindTurboLandingsTest {
    private final DefaultCompareStrategy compareStrategy =
            DefaultCompareStrategies.allFieldsExcept(newPath(".+", "metrikaCounters"));

    @Autowired
    private Steps steps;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private TurboLandingRepository turboLandingRepository;
    @Autowired
    private TurboLandingValidationService turboLandingValidationService;
    @Autowired
    private UpdateCounterGrantsService updateCounterGrantsService;


    private TurboLandingsClient turboLandingsClient;
    private TurboLandingService turboLandingService;

    private ClientId clientId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        turboLandingsClient = mock(TurboLandingsClient.class);
        turboLandingService = new TurboLandingService(shardHelper, turboLandingRepository, updateCounterGrantsService,
                turboLandingValidationService, turboLandingsClient);
    }

    @Test
    public void findTurboLandings_successful() {
        TurboLanding turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientId);

        String turboLandingUrl = turboLanding.getUrl();
        Set<String> urls = Set.of(turboLandingUrl, "https://yandex.ru/turbo?non_existing_turbolanding_url");
        when(turboLandingsClient.getTurbolandingIdsByUrl(eq(urls)))
                .thenReturn(turboLandingsToGetIdByUrlResponseItems(singletonList(turboLanding)));

        Map<String, TurboLanding> foundTurboLandings = turboLandingService.findTurboLandingsByUrl(urls);

        assertThat(foundTurboLandings, beanDiffer(singletonMap(turboLandingUrl, turboLanding))
                .useCompareStrategy(compareStrategy));
    }

    @Test
    public void findTurboLandings_not_found() {
        Set<String> urls = Set.of("https://yandex.ru/turbo?non_existing_turbolanding_url",
                "https://ya.ru/another_non_existing_turbolanding_url");
        when(turboLandingsClient.getTurbolandingIdsByUrl(eq(urls)))
                .thenReturn(emptyList());

        Map<String, TurboLanding> foundTurboLandings = turboLandingService.findTurboLandingsByUrl(urls);

        assertThat(foundTurboLandings.values(), hasSize(0));
    }

    private List<GetIdByUrlResponseItem> turboLandingsToGetIdByUrlResponseItems(
            Collection<TurboLanding> turboLandings)
    {
        return StreamEx.of(turboLandings)
                .map(tl -> new GetIdByUrlResponseItem()
                        .withLandingId(tl.getId())
                        .withClientId(tl.getClientId())
                        .withUrl(tl.getUrl()))
                .toList();
    }

    @Test
    public void findTurboLandings_not_cannonical_url() {
        TurboLanding turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientId);

        String turboLandingUrl = "https://кириллица_с_подчеркиваниями_12-вот-так.turbo.site/page17";
        Set<String> urls = Set.of(turboLandingUrl);
        when(turboLandingsClient.getTurbolandingIdsByUrl(eq(urls)))
                .thenReturn(singletonList(
                        new GetIdByUrlResponseItem()
                                .withUrl(turboLandingUrl)
                                .withLandingId(turboLanding.getId())
                                .withClientId(turboLanding.getClientId())));

        Map<String, TurboLanding> foundTurboLandings = turboLandingService.findTurboLandingsByUrl(urls);

        assertThat(foundTurboLandings, beanDiffer(singletonMap(turboLandingUrl, turboLanding))
                .useCompareStrategy(compareStrategy));
    }


}
