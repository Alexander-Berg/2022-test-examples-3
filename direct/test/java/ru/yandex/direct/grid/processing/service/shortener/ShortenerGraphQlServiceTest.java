package ru.yandex.direct.grid.processing.service.shortener;


import java.util.Map;

import graphql.ExecutionResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.frontdb.repository.FilterShortcutRepository;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter;
import ru.yandex.direct.grid.processing.model.shortener.GdSaveFilter;
import ru.yandex.direct.grid.processing.model.shortener.GdShortFilter;
import ru.yandex.direct.grid.processing.model.shortener.GdShortFilterUnion;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ShortenerGraphQlServiceTest {

    private static final String SAVE_FILTER_MUTATION = "saveFilter";
    private static final String SAVE_FILTER_WITH_KEY_MUTATION = "saveFilterWithKey";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    filterKey\n"
            + "  }\n"
            + "}";

    private User operator;
    private AdGroupInfo adGroupInfo;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private GridContextProvider contextProvider;

    @Autowired
    private FilterShortcutRepository filterShortcutRepository;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createDefaultUser();
        operator = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(operator);
        contextProvider.setGridContext(buildContext(operator));

        adGroupInfo = new AdGroupInfo()
                .withClientInfo(userInfo.getClientInfo());
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void saveFilter_Success() {
        String json = "{\"archived\":false}";

        GdShortFilterUnion input = new GdShortFilterUnion()
                .withCampaignFilter(new GdCampaignFilter()
                        .withArchived(false));

        String query = String.format(MUTATION_TEMPLATE, SAVE_FILTER_MUTATION,
                graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        GdShortFilter payload = GraphQlJsonUtils
                .convertValue(data.get(SAVE_FILTER_MUTATION), GdShortFilter.class);

        String expectedHash = filterShortcutsSteps.getHashForJsonFilter(json);
        GdShortFilter expected = new GdShortFilter()
                .withFilterKey(expectedHash);

        assertThat(payload)
                .is(matchedBy(beanDiffer(expected)));

        String filter = filterShortcutRepository.getFilter(operator.getClientId(), expectedHash);
        assertThat(filter).isEqualTo(json);
    }

    @Test
    public void saveFilterWithKey_Success() {
        String hash = RandomStringUtils.randomAlphabetic(10);
        String json = "{\"archived\":false}";

        GdShortFilterUnion input = new GdShortFilterUnion()
                .withCampaignFilter(new GdCampaignFilter()
                        .withArchived(false));
        GdSaveFilter gdSaveFilter = new GdSaveFilter()
                .withFilterKey(hash)
                .withFilterUnion(input);

        String query = String.format(MUTATION_TEMPLATE, SAVE_FILTER_WITH_KEY_MUTATION,
                graphQlSerialize(gdSaveFilter));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        GdShortFilter payload = GraphQlJsonUtils
                .convertValue(data.get(SAVE_FILTER_WITH_KEY_MUTATION), GdShortFilter.class);

        GdShortFilter expected = new GdShortFilter()
                .withFilterKey(hash);

        assertThat(payload)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void saveFilter_NoInputValue_Error() {
        GdShortFilterUnion input = new GdShortFilterUnion();

        String query = String.format(MUTATION_TEMPLATE, SAVE_FILTER_MUTATION,
                graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());

        assertThat(result.getErrors()).hasSize(1);
    }
}

