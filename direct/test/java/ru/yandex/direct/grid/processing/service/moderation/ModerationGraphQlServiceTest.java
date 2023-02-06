package ru.yandex.direct.grid.processing.service.moderation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.core.testing.steps.ModerationDiagSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.model.moderation.GdModerationReasonType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.moderation.GdModerationDiagsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationGraphQlServiceTest {
    private static final String QUERY = ""
            + "query {\n"
            + "  moderationDiags(input: %s) {\n"
            + "      id\n"
            + "      type\n"
            + "      token\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private GridContextProvider contextProvider;

    @Autowired
    private ModerationDiagSteps moderationDiagSteps;
    @Autowired
    private ModerationDiagService moderationDiagService;

    @Before
    public void before() {
        var userInfo = userSteps.createDefaultUser();
        contextProvider.setGridContext(buildContext(userInfo.getUser()));
        moderationDiagService.invalidateAll();
        moderationDiagSteps.insertStandartDiags();
    }

    @After
    public void after() {
        moderationDiagSteps.cleanup();
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getModerationDiags() {
        var diag1 = TestModerationDiag.createModerationDiag1();
        var diag2 = TestModerationDiag.createModerationDiag2();
        var diagPerformance = TestModerationDiag.createModerationDiagPerformance();

        GdModerationDiagsContainer input = new GdModerationDiagsContainer()
                .withReasonIdsByType(Map.of(GdModerationReasonType.COMMON, Set.of(diag1.getId(), diag2.getId()),
                        GdModerationReasonType.PERFORMANCE, Set.of(diagPerformance.getId())));

        String query = String.format(QUERY, graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors()).isEmpty();

        Map<Object, Object> data = result.getData();
        assertThat(data).containsKey("moderationDiags").hasSize(1);

        Object moderationDiags = data.get("moderationDiags");
        assertThat(moderationDiags).isInstanceOf(Collection.class);

        Set<Object> expected = Set.of(
                Map.of("id", diag1.getId(),
                        "type", "COMMON",
                        "token", diag1.getToken()),
                Map.of("id", diag2.getId(),
                        "type", "COMMON",
                        "token", diag2.getToken()),
                Map.of("id", diagPerformance.getId(),
                        "type", "PERFORMANCE",
                        "token", diagPerformance.getToken())
        );

        assertThat(Set.copyOf((Collection<?>) moderationDiags)).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getNonexistentModerationDiags() {
        GdModerationDiagsContainer input = new GdModerationDiagsContainer()
                .withReasonIdsByType(Map.of(GdModerationReasonType.COMMON, Set.of(99999L, 666666L)));

        String query = String.format(QUERY, graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors()).isEmpty();

        Map<Object, Object> data = result.getData();
        assertThat(data).containsKey("moderationDiags").hasSize(1);

        Object moderationDiags = data.get("moderationDiags");
        assertThat(moderationDiags).isInstanceOf(Collection.class);
        assertThat((Collection<?>) moderationDiags).isEmpty();
    }

    @Test
    public void getNonexistentAndExistentModerationDiags() {
        var diag = TestModerationDiag.createModerationDiag1();

        GdModerationDiagsContainer input = new GdModerationDiagsContainer()
                .withReasonIdsByType(Map.of(GdModerationReasonType.COMMON, Set.of(666666L, diag.getId(), 99999L),
                        GdModerationReasonType.PERFORMANCE, Set.of(666666L)));

        String query = String.format(QUERY, graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors()).isEmpty();

        Map<Object, Object> data = result.getData();
        assertThat(data).containsKey("moderationDiags").hasSize(1);

        Object moderationDiags = data.get("moderationDiags");
        assertThat(moderationDiags).isInstanceOf(Collection.class);

        Set<Object> expected = Set.of(
                Map.of("id", diag.getId(),
                        "type", "COMMON",
                        "token", diag.getToken())
        );

        assertThat(Set.copyOf((Collection<?>) moderationDiags)).is(matchedBy(beanDiffer(expected)));
    }
}
