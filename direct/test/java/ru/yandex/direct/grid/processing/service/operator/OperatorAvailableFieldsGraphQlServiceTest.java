package ru.yandex.direct.grid.processing.service.operator;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdCampaignsAvailableFields;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@GridProcessingTest
@RunWith(Parameterized.class)
public class OperatorAvailableFieldsGraphQlServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    private static final String QUERY = "{\n"
            + "  operator {\n"
            + "    access {\n"
            + "      availableFields {\n"
            + "        campaignFields\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private ClientSteps clientSteps;

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {RbacRole.SUPER, List.of(GdCampaignsAvailableFields.CONTENT_LANGUAGE.name())},
                {RbacRole.SUPERREADER, emptyList()}
        });
    }

    @Parameterized.Parameter
    public RbacRole role;

    @Parameterized.Parameter(1)
    public List<String> expectedCampaignFields;


    @Before
    public void initTestData() {
        var httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader(eq("X-Real-IP"))).thenReturn("12.12.12.12");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
        User operator = clientSteps.createDefaultClientWithRole(role).getChiefUserInfo().getUser();
        User user = clientSteps.createDefaultClient().getChiefUserInfo().getUser();
        context = ContextHelper.buildContext(operator, user);
        TestAuthHelper.setDirectAuthentication(operator, user);
    }

    @After
    public void afterTest() {
        RequestContextHolder.resetRequestAttributes();
    }


    @Test
    public void testService() {
        ExecutionResult result = processor.processQuery(null, QUERY, null, context);

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        assertThat(data).containsKey("operator");
        DocumentContext context = JsonPath.parse(toJson(data.get("operator")));
        List<String> campaignFields = context.read("$.access.availableFields.campaignFields");
        assertThat(campaignFields).containsExactlyInAnyOrder(expectedCampaignFields.toArray(new String[0]));
    }
}
