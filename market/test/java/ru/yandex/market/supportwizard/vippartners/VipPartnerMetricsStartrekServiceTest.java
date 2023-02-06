package ru.yandex.market.supportwizard.vippartners;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.SimpleUserRef;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.storage.EnvironmentRepository;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueRef;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DbUnitDataSet
public class VipPartnerMetricsStartrekServiceTest extends BaseFunctionalTest {

    @Autowired
    EnvironmentRepository environmentRepository;
    @Autowired
    private VipPartnerMetricsStartrekService vipPartnerMetricsStartrekService;
    @Autowired
    private Session startrekSession;

    @NotNull
    private Issue getIssues(String id) {
        Map<String, Object> values = Map.of(
                "createdBy", new SimpleUserRef("pupkin"),
                "numberOfErrors", Option.of(8L)
        );
        return new Issue(id, null, null, null, 0, DefaultMapF.wrap(values), startrekSession);
    }


    @Test
    @DbUnitDataSet(before = "testWriteMetricsToStartrek.before.csv")
    @DisplayName("Проверить корректность записи метрик в стратрек")
    void testWriteMetricsToStartrek() {
        Issues issues = mock(Issues.class);
        ArgumentCaptor<IssueRef> issueUpdateRefCaptor = ArgumentCaptor.forClass(IssueRef.class);
        ArgumentCaptor<IssueUpdate> issueUpdateCaptor = ArgumentCaptor.forClass(IssueUpdate.class);
        when(startrekSession.issues()).thenReturn(issues);
        when(issues.update(issueUpdateRefCaptor.capture(), issueUpdateCaptor.capture())).thenReturn(null);

        ArgumentCaptor<IssueCreate> issueCreateCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        when(issues.create(issueCreateCaptor.capture())).thenReturn(null);

        when(issues.find(contains("page-1")))
                .thenReturn(DefaultIteratorF.wrap(List.of(getIssues("MBIMON-1")).iterator()));
        when(issues.find(contains("method-1")))
                .thenReturn(DefaultIteratorF.wrap(List.of(getIssues("MBIMON-2")).iterator()));
        when(issues.find(contains("page-2"))).thenReturn(Cf.emptyIterator());
        when(issues.find(contains("method-2"))).thenReturn(Cf.emptyIterator());


        List<VipPartnerMetric> vipPartnerMetrics =
                Arrays.asList(new VipPartnerMetric("page-1", MetricType.PAGE, new TreeSet<>(Set.of(1L)), Arrays.asList("a/b", "c/d"), 2L),
                        new VipPartnerMetric("page-2", MetricType.PAGE, new TreeSet<>(Set.of(1L, 2L)), Arrays.asList("e/f", "g/h"), 4L),
                        new VipPartnerMetric("method-1", MetricType.METHOD, new TreeSet<>(Set.of(4L, 8L)), Arrays.asList("k/l", "m/n"), 8L),
                        new VipPartnerMetric("method-2", MetricType.METHOD, new TreeSet<>(Set.of(8L, 16L)), Arrays.asList("o/o", "q/r"), 16L));

        vipPartnerMetricsStartrekService.writeMetricToStartrek("foo", vipPartnerMetrics);

        Assert.assertEquals(issueUpdateRefCaptor.getAllValues().size(), 2L);
        assertUpdate(issueUpdateCaptor.getAllValues().get(0),
                "ОПИСАНИЕ ГЕНЕРИРУЕТСЯ АВТОМАТИЧЕСКИ, НЕ ПИШИТЕ НИЧЕГО СЮДА\n"
                        + "-------------------------------------------------------------\n"
                        + "**Таблица с данными о кампаниях**\n"
                        + "#|\n"
                        + "||ид. кампании|ид. партнера|ид. бизнеса|название партнера|тип||\n"
                        + "|| 1|101|100|partner1|SHOP||\n"
                        + "|#\n"
                        + "\n"
                        + "**Трассировки**\n"
                        + "<{последние трассировки\n"
                        + "  https://tsum.yandex-team.ru/trace/a/b\n"
                        + "  https://tsum.yandex-team.ru/trace/c/d\n"
                        + "}>\n"
                        + "\n"
                        + "**Примеры запросов в yql**\n"
                        + "partner1 (1)\n"
                        + "\n"
                        + "((https://yql.yandex-team.ru/?type=CLICKHOUSE&query=foo+where+page+%3D%27page-1%27 Посмотреть в yql))\n"
                        + "\n"
                        + "-------------------------------------------------------------\n"
                        + "ОПИСАНИЕ ГЕНЕРИРУЕТСЯ АВТОМАТИЧЕСКИ, НЕ ПИШИТЕ НИЧЕГО СЮДА",
                "1", 10L);
        assertUpdate(issueUpdateCaptor.getAllValues().get(1),
                "ОПИСАНИЕ ГЕНЕРИРУЕТСЯ АВТОМАТИЧЕСКИ, НЕ ПИШИТЕ НИЧЕГО СЮДА\n"
                        + "-------------------------------------------------------------\n"
                        + "**Таблица с данными о кампаниях**\n"
                        + "#|\n"
                        + "||ид. кампании|ид. партнера|ид. бизнеса|название партнера|тип||\n"
                        + "|| 4|104|100|partner4|SUPPLIER||\n"
                        + "|| 8|108|102|partner8|SUPPLIER||\n"
                        + "|#\n"
                        + "\n"
                        + "**Трассировки**\n"
                        + "<{последние трассировки\n"
                        + "  https://tsum.yandex-team.ru/trace/k/l\n"
                        + "  https://tsum.yandex-team.ru/trace/m/n\n"
                        + "}>\n"
                        + "\n"
                        + "**Примеры запросов в yql**\n"
                        + "partner4 (4),partner8 (8)\n"
                        + "\n"
                        + "((https://yql.yandex-team.ru/?type=CLICKHOUSE&query=foo+where+dove+%3D+%27method-1%27 Посмотреть в yql))\n"
                        + "\n"
                        + "-------------------------------------------------------------\n"
                        + "ОПИСАНИЕ ГЕНЕРИРУЕТСЯ АВТОМАТИЧЕСКИ, НЕ ПИШИТЕ НИЧЕГО СЮДА",
                "4 8", 16L);

        Assert.assertEquals(issueCreateCaptor.getAllValues().size(), 2L);
        assertCreate(issueCreateCaptor.getAllValues().get(0),
                "page-2",
                "MBIMON",
                "ОПИСАНИЕ ГЕНЕРИРУЕТСЯ АВТОМАТИЧЕСКИ, НЕ ПИШИТЕ НИЧЕГО СЮДА\n"
                        + "-------------------------------------------------------------\n"
                        + "**Таблица с данными о кампаниях**\n"
                        + "#|\n"
                        + "||ид. кампании|ид. партнера|ид. бизнеса|название партнера|тип||\n"
                        + "|| 1|101|100|partner1|SHOP||\n"
                        + "|| 2|102|100|partner2|SUPPLIER||\n"
                        + "|#\n"
                        + "\n"
                        + "**Трассировки**\n"
                        + "<{последние трассировки\n"
                        + "  https://tsum.yandex-team.ru/trace/e/f\n"
                        + "  https://tsum.yandex-team.ru/trace/g/h\n"
                        + "}>\n"
                        + "\n"
                        + "**Примеры запросов в yql**\n"
                        + "partner1 (1),partner2 (2)\n"
                        + "\n"
                        + "((https://yql.yandex-team.ru/?type=CLICKHOUSE&query=foo+where+page+%3D%27page-2%27 Посмотреть в yql))\n"
                        + "\n"
                        + "-------------------------------------------------------------\n"
                        + "ОПИСАНИЕ ГЕНЕРИРУЕТСЯ АВТОМАТИЧЕСКИ, НЕ ПИШИТЕ НИЧЕГО СЮДА",
                "1 2", 4L);

        assertCreate(issueCreateCaptor.getAllValues().get(1),
                "method-2",
                "MBIMON",
                "ОПИСАНИЕ ГЕНЕРИРУЕТСЯ АВТОМАТИЧЕСКИ, НЕ ПИШИТЕ НИЧЕГО СЮДА\n"
                        + "-------------------------------------------------------------\n"
                        + "**Таблица с данными о кампаниях**\n"
                        + "#|\n"
                        + "||ид. кампании|ид. партнера|ид. бизнеса|название партнера|тип||\n"
                        + "|| 8|108|102|partner8|SUPPLIER||\n"
                        + "|| 16|116|102|partner16|SHOP||\n"
                        + "|#\n"
                        + "\n"
                        + "**Трассировки**\n"
                        + "<{последние трассировки\n"
                        + "  https://tsum.yandex-team.ru/trace/o/o\n"
                        + "  https://tsum.yandex-team.ru/trace/q/r\n"
                        + "}>\n"
                        + "\n"
                        + "**Примеры запросов в yql**\n"
                        + "partner8 (8),partner16 (16)\n"
                        + "\n"
                        + "((https://yql.yandex-team.ru/?type=CLICKHOUSE&query=foo+where+dove+%3D+%27method-2%27 Посмотреть в yql))\n"
                        + "\n"
                        + "-------------------------------------------------------------\n"
                        + "ОПИСАНИЕ ГЕНЕРИРУЕТСЯ АВТОМАТИЧЕСКИ, НЕ ПИШИТЕ НИЧЕГО СЮДА",
                "8 16", 16L);

        verify(startrekSession, times(8)).issues();
        verify(issues, times(4)).find(anyString());
        verify(issues, times(2)).update(any(IssueRef.class), any(IssueUpdate.class));
        verify(issues, times(2)).create(any(IssueCreate.class));
        verifyNoMoreInteractions(startrekSession, issues);
    }

    private void assertUpdate(IssueUpdate update, String expectedDescription,
            String expectedCampaignIDs, long expectedNumberOfErrors)
    {
        Object description = ((ScalarUpdate) update.getValues().getOrThrow("description")).getSet().get();
        Object campaignIDs = ((ScalarUpdate) update.getValues().getOrThrow("campaignIDs")).getSet().get();
        Object numberOfErrors = ((ScalarUpdate) update.getValues().getOrThrow("numberOfErrors")).getSet().get();

        Assert.assertEquals(expectedDescription, description);
        Assert.assertEquals(expectedCampaignIDs, campaignIDs);
        Assert.assertEquals(expectedNumberOfErrors, numberOfErrors);
    }

    private void assertCreate(IssueCreate create, String expectedSummary, String expectedQueue,
            String expectedDescription, String expectedCampaignIDs, long expectedNumberOfErrors)
    {
        Object summary = create.getValues().getOrThrow("summary");
        Object queue = create.getValues().getOrThrow("queue");
        Object description = create.getValues().getOrThrow("description");
        Object campaignIDs = create.getValues().getOrThrow("campaignIDs");
        Object numberOfErrors = create.getValues().getOrThrow("numberOfErrors");

        Assert.assertEquals(expectedSummary, summary);
        Assert.assertEquals(expectedQueue, queue);
        Assert.assertEquals(expectedDescription, description);
        Assert.assertEquals(expectedCampaignIDs, campaignIDs);
        Assert.assertEquals(expectedNumberOfErrors, numberOfErrors);
    }
}
