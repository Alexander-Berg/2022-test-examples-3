package ru.yandex.market.crm.platform.reader.logbroker;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.junit.Test;
import ru.yandex.inside.yt.TabSeparatedKeyValue;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.config.LBInstallationMode;
import ru.yandex.market.crm.platform.config.LockModeConfig;
import ru.yandex.market.crm.platform.config.LogBrokerSource;
import ru.yandex.market.crm.platform.config.Model;
import ru.yandex.market.crm.platform.config.SourceConfig;
import ru.yandex.market.crm.platform.config.StorageConfig;
import ru.yandex.market.crm.platform.config.TestConfigs;
import ru.yandex.market.crm.platform.config.raw.StorageType;
import ru.yandex.market.crm.platform.models.TestCartEvent;
import ru.yandex.market.crm.platform.models.TestCartEventStringEventTimeFormat;
import ru.yandex.market.crm.platform.reader.test.AbstractServiceTest;
import ru.yandex.market.crm.platform.services.facts.impl.FactsServiceImpl;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.platform.yt.YtTables;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.request.trace.TskvRecordBuilder;
import ru.yandex.yt.ytclient.proxy.YtClient;

import javax.inject.Inject;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.platform.reader.test.FactUtils.assertRow;

/**
 * @author apershukov
 */
public class FactMessageConsumerTest extends AbstractServiceTest {

    private static final String PROTO_CART_EVENT_FACT = "TestFact1";
    private static final String TSKV_CART_EVENT_FACT = "TestFact2";
    private static final String PROTO_CART_EVENT_ISO_EVENT_FACT = "TestFact3";

    private static final SourceConfig TSKV_SOURCE = new LogBrokerSource(
        Collections.emptyList(),
        "test-ident",
        "test-type",
        LBInstallationMode.LOGBROKER,
        FactMessageConsumerTest::toFacts,
        LockModeConfig.PARTITION
    );

    private static List<Message> toFacts(byte[] is) {
        return new BufferedReader(new StringReader(CrmStrings.valueOf(is))).lines()
            .map(FactMessageConsumerTest::toFact)
            .collect(Collectors.toList());
    }

    private static Message toFact(String v) {
        TabSeparatedKeyValue tskv = TabSeparatedKeyValue.valueOf(v);
        return TestCartEvent.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setYandexuid(tskv.getO("yandexuid").orElse(""))
                    .setPuid(
                        tskv.getO("puid")
                            .map(Long::valueOf)
                            .orElse(0L)
                    )
                    .setEmail(tskv.getValueByKey().getOrDefault("email", ""))
            )
            .setTimestamp(
                tskv.getO("timestamp")
                    .map(Long::valueOf)
                    .orElse(0L)
            )
            .setModelId(
                tskv.getO("model")
                    .map(Long::valueOf)
                    .orElse(0L)
            )
            .build();
    }

    private static final SourceConfig PROTOBUF_SOURCE = new LogBrokerSource(
        Collections.emptyList(),
        "test-ident",
        "test-source",
        LBInstallationMode.LOGBROKER,
        null,
        LockModeConfig.PARTITION
    );

    private static final Model CART_EVENT_MODEL = TestConfigs.model(TestCartEvent.class);

    private static final Model CART_EVENT_ISO_MODEL = TestConfigs.model(TestCartEventStringEventTimeFormat.class);

    private static final FactConfig PROTOBUF_CART_EVENT_CONFIG = new FactConfig(
        PROTO_CART_EVENT_FACT,
        PROTO_CART_EVENT_FACT,
        Collections.singletonList(PROTOBUF_SOURCE),
        CART_EVENT_MODEL,
        null,
        null,
            List.of(),
            Map.of("hahn", store())
    );

    private static final FactConfig TSKV_CART_EVENT_CONFIG = new FactConfig(
            TSKV_CART_EVENT_FACT,
            TSKV_CART_EVENT_FACT,
            Collections.singletonList(TSKV_SOURCE),
            CART_EVENT_MODEL,
            null,
            null,
            List.of(),
            Map.of("hahn", store())
    );

    private static final FactConfig PROTO_CART_EVENT_ISO_CONFIG = new FactConfig(
            PROTO_CART_EVENT_ISO_EVENT_FACT,
            PROTO_CART_EVENT_ISO_EVENT_FACT,
            Collections.singleton(PROTOBUF_SOURCE),
            CART_EVENT_ISO_MODEL,
            null,
            null,
            List.of(),
            Map.of("hahn", store())
    );

    private static StorageConfig store() {
        return new StorageConfig(null, StorageType.HDD);
    }


    @Inject
    private FactsServiceImpl factsService;

    @Inject
    private YtSchemaTestUtils schemaTestUtils;

    @Inject
    private YtTables ytTables;

    @Inject
    private YtClient ytClient;

    @Test
    public void testProcessSingleFact() throws InvalidProtocolBufferException {
        FactMessageConsumer consumer = prepareConsumer(PROTOBUF_CART_EVENT_CONFIG);

        TestCartEvent message = TestCartEvent.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setYandexuid("111")
            )
            .setTimestamp(System.currentTimeMillis())
            .setModelId(1234)
            .build();

        byte[] bytes = message.toByteArray();
        run(consumer, bytes);

        List<YTreeMapNode> rows = readAllRows(PROTOBUF_CART_EVENT_CONFIG);

        assertEquals(1, rows.size());
        assertRow(
            message.getUserIds().getYandexuid(),
            UidType.YANDEXUID,
            message.getTimestamp(),
            String.valueOf(message.getModelId()),
            message,
            TestCartEvent.parser(),
            rows.get(0)
        );
    }

    private void run(FactMessageConsumer consumer, byte[] bytes) {
        consumer.accept(consumer.transform(bytes));
    }

    @Test
    public void testProcessFactWithIsoEventTime() throws InvalidProtocolBufferException {
        FactMessageConsumer consumer = prepareConsumer(PROTO_CART_EVENT_ISO_CONFIG);

        LocalDateTime eventTime = LocalDateTime.now();
        TestCartEventStringEventTimeFormat message = TestCartEventStringEventTimeFormat.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setYandexuid("111")
            )
            .setTimestamp(eventTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .setModelId(1234)
            .build();

        run(consumer, message.toByteArray());

        List<YTreeMapNode> rows = readAllRows(PROTO_CART_EVENT_ISO_CONFIG);
        assertEquals(1, rows.size());
        assertRow(
            message.getUserIds().getYandexuid(),
            UidType.YANDEXUID,
            eventTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
            String.valueOf(message.getModelId()),
            message,
            TestCartEventStringEventTimeFormat.parser(),
            rows.get(0)
        );
    }

    @Test
    public void testIgnoreFactWithWrongEventTimeFormat() {
        FactMessageConsumer consumer = prepareConsumer(PROTO_CART_EVENT_ISO_CONFIG);

        LocalDateTime eventTime = LocalDateTime.now();
        TestCartEventStringEventTimeFormat message = TestCartEventStringEventTimeFormat.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setYandexuid("111")
            )
            .setTimestamp(eventTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .setModelId(1234)
            .build();

        run(consumer, message.toByteArray());

//        List<YTreeMapNode> rows = kvStorageClient.getRows(PROTO_CART_EVENT_ISO_EVENT_TABLE_PATH);
//        assertTrue(rows.isEmpty());
    }

    @Test
    public void testIgnoreFactWithoutUserId() {
        FactMessageConsumer consumer = prepareConsumer(PROTOBUF_CART_EVENT_CONFIG);

        TestCartEvent message = TestCartEvent.newBuilder()
            .setUserIds(UserIds.newBuilder())
            .setTimestamp(System.currentTimeMillis())
            .setModelId(1234)
            .build();

        run(consumer, message.toByteArray());

        List<YTreeMapNode> rows = readAllRows(PROTOBUF_CART_EVENT_CONFIG);
        assertTrue(rows.isEmpty());
    }

    @Test
    public void testUseMapperToParseFactFromTskv() throws InvalidProtocolBufferException {
        FactMessageConsumer consumer = prepareConsumer(TSKV_CART_EVENT_CONFIG);

        TestCartEvent fact = TestCartEvent.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setYandexuid("123")
            )
            .setTimestamp(Instant.now().toEpochMilli())
            .setModelId(222)
            .build();

        run(consumer, toTskv(fact).getBytes(StandardCharsets.UTF_8));

        List<YTreeMapNode> rows = readAllRows(TSKV_CART_EVENT_CONFIG);

        assertEquals(1, rows.size());
        assertRow(
            fact.getUserIds().getYandexuid(),
            UidType.YANDEXUID,
            fact.getTimestamp(),
            String.valueOf(fact.getModelId()),
            fact,
            TestCartEvent.parser(),
            rows.get(0)
        );
    }

    @Test
    public void testUseMapperToParseFactsFromMulipleLines() throws InvalidProtocolBufferException {
        FactMessageConsumer consumer = prepareConsumer(TSKV_CART_EVENT_CONFIG);

        long time = System.currentTimeMillis();

        TestCartEvent fact1 = TestCartEvent.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setYandexuid("123")
            )
            .setTimestamp(time)
            .setModelId(222)
            .build();

        TestCartEvent fact2 = TestCartEvent.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setPuid(321)
            )
            .setTimestamp(time + 1)
            .setModelId(333)
            .build();

        TestCartEvent fact3 = TestCartEvent.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setPuid(321)
            )
            .setTimestamp(time + 2)
            .setModelId(444)
            .build();

        String lines = toTskv(fact1) + "\n" + toTskv(fact2) + "\n" + toTskv(fact3);

        run(consumer, CrmStrings.getBytes(lines));

        List<YTreeMapNode> rows = readAllRows(TSKV_CART_EVENT_CONFIG);

        assertEquals(3, rows.size());

        assertRow(
            fact1.getUserIds().getYandexuid(),
            UidType.YANDEXUID,
            fact1.getTimestamp(),
            String.valueOf(fact1.getModelId()),
            fact1,
            TestCartEvent.parser(),
            rows.get(0)
        );

        assertRow(
            String.valueOf(fact2.getUserIds().getPuid()),
            UidType.PUID,
            fact2.getTimestamp(),
            String.valueOf(fact2.getModelId()),
            fact2,
            TestCartEvent.parser(),
            rows.get(1)
        );

        assertRow(
            String.valueOf(fact3.getUserIds().getPuid()),
            UidType.PUID,
            fact3.getTimestamp(),
            String.valueOf(fact3.getModelId()),
            fact3,
            TestCartEvent.parser(),
            rows.get(2)
        );
    }

    @Test
    public void testAcceptEmailBoundFact() throws InvalidProtocolBufferException {
        FactMessageConsumer consumer = prepareConsumer(TSKV_CART_EVENT_CONFIG);

        TestCartEvent fact = TestCartEvent.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setEmail("apershukov@yandex-team.ru")
            )
            .setTimestamp(System.currentTimeMillis())
            .setModelId(222)
            .build();

        run(consumer, CrmStrings.getBytes(toTskv(fact)));

        List<YTreeMapNode> rows = readAllRows(TSKV_CART_EVENT_CONFIG);

        assertEquals(1, rows.size());

        assertRow(
            fact.getUserIds().getEmail(),
            UidType.EMAIL,
            fact.getTimestamp(),
            String.valueOf(fact.getModelId()),
            fact,
            TestCartEvent.parser(),
            rows.get(0)
        );
    }

    @Test
    public void testIgnoreFactWithTimestampInSeconds() {
        FactMessageConsumer consumer = prepareConsumer(PROTOBUF_CART_EVENT_CONFIG);

        TestCartEvent message = TestCartEvent.newBuilder()
            .setUserIds(
                UserIds.newBuilder()
                    .setYandexuid("111")
            )
            .setTimestamp(Instant.now().getEpochSecond())
            .setModelId(1234)
            .build();

        run(consumer, message.toByteArray());

        List<YTreeMapNode> rows = readAllRows(PROTOBUF_CART_EVENT_CONFIG);
        assertEquals(0, rows.size());
    }

    private String toTskv(TestCartEvent fact) {
        TskvRecordBuilder builder = new TskvRecordBuilder();

        String yuid = fact.getUserIds().getYandexuid();
        if (!Strings.isNullOrEmpty(yuid)) {
            builder.add("yandexuid", yuid);
        }

        String email = fact.getUserIds().getEmail();
        if (!Strings.isNullOrEmpty(email)) {
            builder.add("email", email);
        }

        builder.add("puid", fact.getUserIds().getPuid());
        builder.add("timestamp", fact.getTimestamp());
        builder.add("model", fact.getModelId());
        return builder.build();
    }

    private FactMessageConsumer prepareConsumer(FactConfig config) {
        FactMessageConsumer consumer = new FactMessageConsumer(
            config,
            (LogBrokerSource) Iterables.get(config.getSources(), 0),
            factsService
        );

        schemaTestUtils.prepareFactTable(config);

        return consumer;
    }

    private List<YTreeMapNode> readAllRows(FactConfig config) {
        YPath path = ytTables.getFactTable(config.getId());
        return ytClient.selectRows("* FROM [" + path + "]").join()
                .getYTreeRows();
    }
}
