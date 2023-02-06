package ru.yandex.direct.grid.processing.service.offlinereport;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.offlinereport.model.OfflineReport;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportState;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportType;
import ru.yandex.direct.core.entity.offlinereport.repository.OfflineReportRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OfflineReportGraphQlServiceTest {
    private static final String QUERY = ""
            + "{\n"
            + "  offlineReportList{\n"
            + "    list {\n"
            + "      reportId\n"
            + "      uid\n"
            + "      args\n"
            + "      scheduledAt\n"
            + "      reportState\n"
            + "      reportType\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    private GridGraphQLContext context;

    @Autowired
    private OfflineReportRepository repository;
    @Autowired
    private ShardHelper shardHelper;

    private User user;
    private Long reportId1;
    private Long reportId2;

    @Before
    public void before() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        user = userInfo.getUser();

        context = ContextHelper.buildContext(user).withFetchedFieldsReslover(null);
        var reportIds = shardHelper.generateOfflineReportIds(2);
        reportId1 = reportIds.get(0);
        reportId2 = reportIds.get(1);

        repository.addOfflineReport(userInfo.getShard(),
                new OfflineReport()
                        .withReportId(reportId1)
                        .withReportState(OfflineReportState.NEW)
                        .withArgs("{\\\"monthFrom\\\":\\\"201904\\\",\\\"monthTo\\\":\\\"201903\\\"}")
                        .withReportType(OfflineReportType.DOMAINS)
                        .withScheduledAt(LocalDateTime.of(2017, Month.JULY, 9, 11, 6, 22))
                        .withUid(user.getUid()));
        repository.addOfflineReport(userInfo.getShard(),
                new OfflineReport()
                        .withReportId(reportId2)
                        .withReportState(OfflineReportState.NEW)
                        .withArgs("{\\\"monthFrom\\\":\\\"201904\\\",\\\"monthTo\\\":\\\"201903\\\"}")
                        .withReportType(OfflineReportType.DOMAINS)
                        .withScheduledAt(LocalDateTime.of(2017, Month.JULY, 19, 9, 9, 9))
                        .withUid(user.getUid()));
    }

    @Test
    public void testService() {
        ExecutionResult result = processor.processQuery(null, QUERY, null, context);
        Map<Object, Object> data = result.getData();

        assertThat(result.getErrors()).isEmpty();
        assertThat(data).containsKey("offlineReportList");
        Map<String, Object> expected = ImmutableMap.of(
                "offlineReportList",
                ImmutableMap.of(
                        "list", ImmutableList.of(
                                ImmutableMap.<String, Object>builder()
                                        .put("reportState", "NEW")
                                        .put("reportType", "DOMAINS")
                                        .put("reportId", reportId2)
                                        .put("scheduledAt", "2017-07-19T09:09:09")
                                        .put("uid", user.getUid())
                                        .put("args", "{\\\"monthFrom\\\":\\\"201904\\\",\\\"monthTo\\\":\\\"201903\\\"}")
                                        .build(),
                                ImmutableMap.<String, Object>builder()
                                        .put("reportState", "NEW")
                                        .put("reportType", "DOMAINS")
                                        .put("reportId", reportId1)
                                        .put("scheduledAt", "2017-07-09T11:06:22")
                                        .put("uid", user.getUid())
                                        .put("args", "{\\\"monthFrom\\\":\\\"201904\\\",\\\"monthTo\\\":\\\"201903\\\"}")
                                        .build()
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }
}
