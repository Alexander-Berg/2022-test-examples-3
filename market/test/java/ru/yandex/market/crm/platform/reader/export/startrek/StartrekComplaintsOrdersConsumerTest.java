package ru.yandex.market.crm.platform.reader.export.startrek;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.Instant;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.config.Model;
import ru.yandex.market.crm.platform.config.SourceConfig;
import ru.yandex.market.crm.platform.config.StartrekSourceConfig;
import ru.yandex.market.crm.platform.config.StorageConfig;
import ru.yandex.market.crm.platform.config.TestConfigs;
import ru.yandex.market.crm.platform.config.raw.StorageType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.reader.checkouter.OrdersService;
import ru.yandex.market.crm.platform.reader.export.startrek.parsers.StartrekComplaintOrderParser;
import ru.yandex.market.crm.platform.reader.test.ServicesTestConfig;
import ru.yandex.market.crm.platform.services.facts.FactsServiceConfiguration;
import ru.yandex.market.crm.platform.services.facts.impl.FactsServiceImpl;
import ru.yandex.market.crm.platform.services.json.SerializationConfiguration;
import ru.yandex.market.crm.platform.test.yt.CoreTestYtConfig;
import ru.yandex.market.crm.platform.test.yt.TestKvStorageClient;
import ru.yandex.market.mcrm.startrek.support.StartrekResult;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueTypeRef;
import ru.yandex.startrek.client.model.StatusRef;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ServicesTestConfig.class, SerializationConfiguration.class, FactsServiceConfiguration.class})
public class StartrekComplaintsOrdersConsumerTest {

    private static final String ORDER_FACT = "fact1";
    private static final String ORDER_TABLE_PATH = CoreTestYtConfig.YT_HOME + "/facts/" + ORDER_FACT;

    private static final SourceConfig STARTREK_SOURCE = new StartrekSourceConfig(
            Collections.emptyList(),
            "/path",
            "Parser"
    );

    private static final Model ORDER_MODEL = TestConfigs.model(Order.class);

    private static final FactConfig ORDER_CONFIG = new FactConfig(
            ORDER_FACT,
            ORDER_FACT,
            Collections.singleton(STARTREK_SOURCE),
            ORDER_MODEL,
            null,
            null,
            List.of(),
            Map.of("hahn", store())
    );

    @Inject
    private TestKvStorageClient kvStorageClient;
    @Inject
    private FactsServiceImpl factsService;

    private static StorageConfig store() {
        return new StorageConfig(null, StorageType.HDD);
    }

    @After
    public void tearDown() {
        kvStorageClient.clear();
    }

    @Test
    @Ignore
    public void testSaved() {
        StartrekComplaintsOrdersConsumer consumer = new StartrekComplaintsOrdersConsumer(
            ORDER_CONFIG,
            new StartrekComplaintOrderParser(mock(OrdersService.class)),
            factsService);

        IssueTypeRef typeRef = mock(IssueTypeRef.class);
        when(typeRef.getDisplay()).thenReturn("Задача");

        StatusRef statusRef = mock(StatusRef.class);
        when(statusRef.getDisplay()).thenReturn("В работе");

        Issue issue1 = mock(Issue.class);
        when(issue1.getId()).thenReturn("1");
        when(issue1.getO("customerOrderNumber")).thenReturn(Option.of("123455"));
        when(issue1.getSummary()).thenReturn("Some problem happened 1");
        when(issue1.getDescription()).thenReturn(Option.empty());
        when(issue1.getType()).thenReturn(typeRef);
        when(issue1.getCreatedAt()).thenReturn(Instant.parse("2018-12-21T10:15:30.00Z"));
        when(issue1.getUpdatedAt()).thenReturn(Instant.parse("2018-12-21T10:30:30.00Z"));
        when(issue1.getStatus()).thenReturn(statusRef);


        Issue issue2 = mock(Issue.class);
        when(issue2.getId()).thenReturn("2");
        when(issue2.getO("customerOrderNumber")).thenReturn(Option.of("123455"));
        when(issue2.getSummary()).thenReturn("Some problem happened 2");
        when(issue2.getDescription()).thenReturn(Option.empty());
        when(issue2.getType()).thenReturn(typeRef);
        when(issue2.getCreatedAt()).thenReturn(Instant.parse("2018-12-21T10:15:30.00Z"));
        when(issue2.getUpdatedAt()).thenReturn(Instant.parse("2018-12-21T10:30:30.00Z"));
        when(issue2.getStatus()).thenReturn(statusRef);

        Issue issue3 = mock(Issue.class);
        when(issue3.getId()).thenReturn("3");
        when(issue3.getO("customerOrderNumber")).thenReturn(Option.of("     123456, 5656"));
        when(issue3.getSummary()).thenReturn("Some problem happened");
        when(issue3.getDescription()).thenReturn(Option.empty());
        when(issue3.getType()).thenReturn(typeRef);
        when(issue3.getCreatedAt()).thenReturn(Instant.parse("2018-12-21T10:45:30.00Z"));
        when(issue3.getUpdatedAt()).thenReturn(Instant.parse("2018-12-21T10:50:30.00Z"));
        when(issue3.getStatus()).thenReturn(statusRef);


        StartrekResult export = new StartrekResult(
                Arrays.asList(issue1, issue2, issue3),
                new HashMap<>(),
                new HashMap<>()
        );
        consumer.accept(export);

        List<YTreeMapNode> rows = kvStorageClient.getRows(ORDER_TABLE_PATH);
        assertEquals(3, rows.size());
    }
}
