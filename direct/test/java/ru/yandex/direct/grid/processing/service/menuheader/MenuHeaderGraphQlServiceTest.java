package ru.yandex.direct.grid.processing.service.menuheader;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.client.GdMenuHeaderInfo;
import ru.yandex.direct.grid.processing.model.client.GdMenuItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MenuHeaderGraphQlServiceTest {

    private static final String QUERY_HANDLE = "menuHeader";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s {\n"
            + "    allowedMenuItems\n"
            + "  }\n"
            + "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    private User operator;
    private Set<GdMenuItem> expectedMenuItems;

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        operator.withChiefUid(operator.getUid());
        TestAuthHelper.setDirectAuthentication(operator);

        expectedMenuItems = ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                GdMenuItem.CREATE,
                GdMenuItem.DOCUMENTS_AND_PAYMENTS,
                GdMenuItem.SHOW_CAMPS,
                GdMenuItem.RECOMMENDATIONS,
                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                GdMenuItem.WORDSTAT);
    }

    @Test
    public void testService() {
        GdMenuHeaderInfo gdMenuHeaderInfo = getAllowedMenuItemsGraphQl();
        Set<GdMenuItem> allowedMenuItems = gdMenuHeaderInfo.getAllowedMenuItems();
        assertThat(allowedMenuItems).containsExactlyInAnyOrder(expectedMenuItems.toArray(new GdMenuItem[0]));
    }

    private GdMenuHeaderInfo getAllowedMenuItemsGraphQl() {
        String query = String.format(QUERY_TEMPLATE, QUERY_HANDLE);
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);

        return GraphQlJsonUtils.convertValue(data.get(QUERY_HANDLE), GdMenuHeaderInfo.class);
    }
}
