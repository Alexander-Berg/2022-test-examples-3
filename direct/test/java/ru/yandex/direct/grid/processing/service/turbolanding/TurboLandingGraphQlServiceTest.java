package ru.yandex.direct.grid.processing.service.turbolanding;

import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.cliententity.GdTurbolanding;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.turbolandings.client.TurboLandingsClient;
import ru.yandex.direct.turbolandings.client.model.GetIdByUrlResponseItem;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;

/**
 * Проверка GraphQl-ных ручек для работы с турболендингами
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TurboLandingGraphQlServiceTest {

    private static final String FIND_TURBOLANDINGS_TEMPLATE = ""
            + "{ findTurboLandings(input: { urlList: [\"%s\"] }) {\n"
            + "     foundTurboLandings\n"
            + "  }\n"
            + "}\n";
    private static final String REGULAR_SITE_URL = "https://www.somesite.net/index2.html?ext=zpa";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    UserRepository userRepository;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private TurboLandingsClient turboLandingsClient;

    private GridGraphQLContext context;
    private TurboLanding turboLanding;


    @Before
    public void setup() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        User user = userRepository.fetchByUids(clientInfo.getShard(), singletonList(clientInfo.getUid())).get(0);
        turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(user.getClientId());
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
    }

    @After
    public void cleanup() {
        Mockito.reset(turboLandingsClient);
    }

    @Test
    public void findTurboLandings_success() {
        String url = turboLanding.getUrl();

        doReturn(singletonList(new GetIdByUrlResponseItem()
                .withClientId(turboLanding.getClientId())
                .withUrl(turboLanding.getUrl())
                .withLandingId(turboLanding.getId())))
                .when(turboLandingsClient).getTurbolandingIdsByUrl(eq(Set.of((url))));

        String query = String.format(FIND_TURBOLANDINGS_TEMPLATE, url);
        ExecutionResult result = processor.processQuery(null, query, null, context);

        Map<String, GdTurbolanding> turboLandingsByUrl = extractData(result);

        assertThat(turboLandingsByUrl).isEqualTo(singletonMap(url, getExpectedTurboLanding(turboLanding)));
    }

    @Test
    public void findTurboLandings_unsetAlienClientId() {
        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        TurboLanding alienTurboLanding = steps.turboLandingSteps()
                .createDefaultTurboLanding(anotherClientInfo.getClientId());
        String url = alienTurboLanding.getUrl();

        doReturn(singletonList(new GetIdByUrlResponseItem()
                .withClientId(alienTurboLanding.getClientId())
                .withUrl(alienTurboLanding.getUrl())
                .withLandingId(alienTurboLanding.getId()))
        ).when(turboLandingsClient).getTurbolandingIdsByUrl(eq(Set.of(url)));

        String query = String.format(FIND_TURBOLANDINGS_TEMPLATE, url);
        ExecutionResult result = processor.processQuery(null, query, null, context);

        Map<String, GdTurbolanding> turboLandingsByUrl = extractData(result);

        assertThat(turboLandingsByUrl).isEqualTo(singletonMap(url, getExpectedTurboLanding(alienTurboLanding).withClientId(0L)));
    }

    @Test
    public void findTurboLandings_turboLandingsClientMethodNotCalledForRegularSiteUrl() {
        String url = REGULAR_SITE_URL;

        doThrow(new RuntimeException())
                .when(turboLandingsClient).getTurbolandingIdsByUrl(anyCollection());

        String query = String.format(FIND_TURBOLANDINGS_TEMPLATE, url);
        ExecutionResult result = processor.processQuery(null, query, null, context);

        Map<String, GdTurbolanding> turboLandingsByUrl = extractData(result);

        assertThat(turboLandingsByUrl).isEmpty();
    }

    @Test
    public void findTurboLandings_notFound() {
        String url = turboLanding.getUrl();

        doReturn(emptyList())
                .when(turboLandingsClient).getTurbolandingIdsByUrl(eq(Set.of(url)));

        String query = String.format(FIND_TURBOLANDINGS_TEMPLATE, url);
        ExecutionResult result = processor.processQuery(null, query, null, context);

        Map<String, GdTurbolanding> turboLandingsByUrl = extractData(result);

        assertThat(turboLandingsByUrl).isEmpty();
    }


    private Map<String, GdTurbolanding> extractData(ExecutionResult result) {
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        return getDataValue(data, "findTurboLandings/foundTurboLandings");
    }

    private GdTurbolanding getExpectedTurboLanding(TurboLanding turboLanding) {
        return new GdTurbolanding()
                .withClientId(turboLanding.getClientId())
                .withHref(turboLanding.getUrl())
                .withTurboSiteHref(turboLanding.getTurboSiteHref())
                .withId(turboLanding.getId())
                .withName(turboLanding.getName())
                .withPreviewHref(turboLanding.getPreviewHref());
    }
}
