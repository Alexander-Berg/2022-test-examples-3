package ru.yandex.market.crm.platform.reader.export.startrek;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.config.SourceConfig;
import ru.yandex.market.crm.platform.config.StartrekSourceConfig;
import ru.yandex.market.crm.platform.config.StorageConfig;
import ru.yandex.market.crm.platform.config.raw.StorageType;
import ru.yandex.market.crm.platform.reader.export.StartTimeImportContext;
import ru.yandex.market.crm.platform.reader.export.dao.FactsImportTasksDAO;
import ru.yandex.market.crm.platform.reader.export.startrek.export.StartrekComplaintsExportServicesProvider;
import ru.yandex.market.crm.platform.reader.export.startrek.export.StartrekComplaintsExporter;
import ru.yandex.market.crm.platform.reader.test.ServicesTestConfig;
import ru.yandex.market.mcrm.db.test.DbTestTool;
import ru.yandex.market.mcrm.startrek.support.StartrekResult;
import ru.yandex.market.mcrm.startrek.support.StartrekService;
import ru.yandex.market.mcrm.startrek.support.impl.service.StartrekSecretSupplier;
import ru.yandex.market.mcrm.startrek.support.impl.service.StartrekServiceImpl;
import ru.yandex.startrek.client.Attachments;
import ru.yandex.startrek.client.Comments;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.startrek.client.model.Event;
import ru.yandex.startrek.client.model.Issue;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ServicesTestConfig.class)
public class StartrekExportRunnerTest {

    private static final java.time.format.DateTimeFormatter MOSCOW_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Europe/Moscow"));

    @Inject
    private DbTestTool dbTestTool;
    @Inject
    private FactsImportTasksDAO dao;
    @Inject
    private StartrekExportRunnerFactory runnerFactory;
    private StartrekComplaintsExportServicesProvider provider;
    private StartrekComplaintsExporter exporter;
    private final Map<String, Issue> issues = new HashMap<>();

    private static FactConfig factConfig(String id) {
        Set<SourceConfig> sources = Collections.singleton(new StartrekSourceConfig(Collections.emptyList(), null, null));
        return new FactConfig(
                id,
                null,
                sources,
                null,
                null,
                null,
                List.of(),
                Map.of("hahn", store())
        );
    }

    private static StorageConfig store() {
        return new StorageConfig(null, StorageType.HDD);
    }

    @Before
    public void before() {
        ListF<String> fields = Cf.arrayList(
            StartrekFieldNames.CREATED_AT,
            StartrekFieldNames.CUSTOMER_ORDER_NUMBER,
            StartrekFieldNames.DESCRIPTION,
            StartrekFieldNames.STATUS,
            StartrekFieldNames.SUMMARY,
            StartrekFieldNames.TYPE,
            StartrekFieldNames.UPDATED_AT,
            StartrekFieldNames.CREATED_BY,
            StartrekFieldNames.ASSIGNEE,
            StartrekFieldNames.QUEUE_NAME
        );
        provider = mock(StartrekComplaintsExportServicesProvider.class);

        StartrekClient client = mock(StartrekClient.class);
        Session session = mock(Session.class);

        when(client.getSession(notNull())).thenReturn(session);

        StartrekSecretSupplier secretSupplier = new StartrekSecretSupplier("<fake Oauth>");
        StartrekService startrekService = new StartrekServiceImpl(client, secretSupplier, 100, 100);
        exporter = new StartrekComplaintsExporter(startrekService, "BLUEMARKETORDER");

        Issue issue1 = mock(Issue.class);
        when(issue1.getId()).thenReturn("1");
        issues.put(issue1.getId(), issue1);

        Issue issue2 = mock(Issue.class);
        when(issue2.getId()).thenReturn("2");
        issues.put(issue2.getId(), issue2);

        Instant now = Instant.now();
        when(issue1.getUpdatedAt()).thenReturn(now);
        when(issue2.getUpdatedAt()).thenReturn(now);

        ListF<Event> events1 = Cf.list(mockEvent("1_event1"));
        when(issue1.getEvents()).thenReturn(events1.iterator());

        ListF<Event> events2 = Cf.list(mockEvent("2_event1"));
        when(issue2.getEvents()).thenReturn(events2.iterator());

        Comments comments = mock(Comments.class);
        when(comments.getAll("1", Cf.arrayList(Comments.Expand.ATTACHMENTS))).thenReturn(Cf.emptyIterator());
        when(comments.getAll("2", Cf.arrayList(Comments.Expand.ATTACHMENTS))).thenReturn(Cf.emptyIterator());

        Attachments attachments = mock(Attachments.class);
        when(attachments.getAll("1")).thenReturn(Cf.emptyIterator());
        when(attachments.getAll("2")).thenReturn(Cf.emptyIterator());

        when(session.comments()).thenReturn(comments);
        when(session.attachments()).thenReturn(attachments);

        ListF<Issue> availableIssues = new ArrayListF<>(Arrays.asList(issue1, issue2));
        Issues issues = mock(Issues.class);

        when(issues.find(eq("queue:BLUEMARKETORDER and Updated:>=\""
                        + MOSCOW_TIME_FORMATTER.format(java.time.Instant.ofEpochMilli(0)) + "\" \"Sort By\":Updated"),
                argThat(x -> CollectionUtils.isEqualCollection(x, fields))))
                .thenReturn(availableIssues.iterator());

        when(issues.find(eq("queue:BLUEMARKETORDER and Updated:>=\""
            + MOSCOW_TIME_FORMATTER.format(java.time.Instant.ofEpochMilli(now.getMillis())) + "\" \"Sort By\":Updated"),
                argThat(x -> CollectionUtils.isEqualCollection(x, fields))))
                .thenReturn(availableIssues.iterator());

        when(session.issues()).thenReturn(issues);
    }

    @After
    public void tearDown() {
        dbTestTool.clearDatabase();
    }

    @Test
    public void firstConsumerReadsAllData() throws Exception {
        StartrekComplaintsOrdersConsumerStub ordersConsumer =
            new StartrekComplaintsOrdersConsumerStub(null, null, null);

        FactConfig fact = factConfig("fact");
        addFactExport(fact, exporter, ordersConsumer);
        startExport(fact);

        StartrekResult lastConsumed1 = ordersConsumer.getAccepted().poll(50, TimeUnit.SECONDS);

        Assert.assertEquals(2, lastConsumed1.getIssues().size());
        Assert.assertEquals("1", lastConsumed1.getIssues().get(0).getId());
        Assert.assertEquals("2", lastConsumed1.getIssues().get(1).getId());
        Assert.assertTrue(lastConsumed1.getComments(issues.get("1")).isEmpty());
        Assert.assertTrue(lastConsumed1.getAttachments(issues.get("1")).isEmpty());
        Assert.assertTrue(lastConsumed1.getComments(issues.get("2")).isEmpty());
        Assert.assertTrue(lastConsumed1.getAttachments(issues.get("2")).isEmpty());

        long lastSaved = lastConsumed1.getEndTime().getMillis();
        assertSavedChangeEventIds(fact, "BLUEMARKETORDER", lastSaved);

        startExport(fact);
        lastConsumed1 = ordersConsumer.getAccepted().poll(50, TimeUnit.SECONDS);

        Assert.assertEquals(0, lastConsumed1.getIssues().size());
        assertSavedChangeEventIds(fact, "BLUEMARKETORDER", lastSaved);
    }

    private void addFactExport(
        FactConfig config,
        StartrekComplaintsExporter exporter,
        StartrekComplaintsOrdersConsumer ordersConsumer
    ) {
        when(provider.getExporter("BLUEMARKETORDER")).thenReturn(exporter);
        when(provider.getStartrekComplaintsOrdersConsumer(eq(config), any(StartrekSourceConfig.class))).thenReturn(ordersConsumer);
    }

    private void assertSavedChangeEventIds(FactConfig fact, String queue, long startTime) throws InterruptedException {
        long lastSaved = -1;
        for (int i = 0; i < 3; ++i) {
            TimeUnit.MILLISECONDS.sleep(500);
            long saved = dao.getImportContext(fact.getId() + "#" + queue, StartTimeImportContext.class)
                    .map(StartTimeImportContext::getStartTime)
                    .orElse(-1L);

            lastSaved = saved;
            if (saved == startTime) {
                return;
            }
        }
        throw new AssertionError("Invalid saved changelog " + lastSaved);
    }

    private Event mockEvent(String id) {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(id);
        return event;
    }

    private void startExport(FactConfig... configs) {
        StartrekExportRunner exportRunner = runnerFactory.create(asList(configs), provider);
        exportRunner.startExport();
    }
}
