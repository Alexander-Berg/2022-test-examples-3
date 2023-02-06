package ru.yandex.direct.grid.processing.service.operator;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OperatorGraphQlServiceProcessingTest {
    private static final String QUERY = "{\n"
            + "  operator {\n"
            + "    info {\n"
            + "      userId\n"
            + "      login\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private User user;
    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Before
    public void initTestData() {
        var httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader(eq("X-Real-IP"))).thenReturn("12.12.12.12");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));

        UserInfo userInfo = userSteps.createUser(generateNewUser());
        user = userInfo.getUser();
        context = ContextHelper.buildContext(user);
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
        Map<String, Object> expected = Collections.singletonMap(
                "operator",
                ImmutableMap.of(
                        "info", ImmutableMap.of("userId", user.getUid(), "login", user.getLogin())
                )
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

}
