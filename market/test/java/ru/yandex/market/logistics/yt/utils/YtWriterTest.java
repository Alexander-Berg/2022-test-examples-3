package ru.yandex.market.logistics.yt.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import lombok.Data;
import lombok.experimental.Accessors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.YtException;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.OptionSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.YTreeSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializerFactory;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class YtWriterTest {
    public static final YTreeSerializer<TestRecord> SERIALIZER =
        YTreeObjectSerializerFactory.forClass(TestRecord.class);
    private static final YPath Y_PATH = YPath.simple("//aaa");
    private static final Instant EXPIRES_AT = Instant.parse("2021-11-11T11:11:11.00Z");
    private static final YTreeNode TEST_SCHEMA = new YtSchemaBuilder()
        .field("abc", YtWriter.STRING, true)
        .build();
    private static final MapF<String, YTreeNode> ATTRIBUTES = Cf.map(
        "schema", TEST_SCHEMA,
        "expiration_time", YTree.stringNode(LocalDateTime.ofInstant(
            EXPIRES_AT,
            ZoneOffset.UTC
        ).toString())
    );
    public static final ArgumentMatcher<CreateNode> CREATE_NODE_ARGUMENT_MATCHER =
        argument -> argument.getType().equals(ObjectType.Table) &&
            argument.getPath().equals(Y_PATH) &&
            argument.getAttributes().equals(ATTRIBUTES) &&
            argument.isRecursive() &&
            argument.isIgnoreExisting();
    public static final String EXCEPTION_MESSAGE = "Network fail";

    private YtWriter ytWriter;
    private Yt mainYt;
    private Yt backupYt;
    private Cypress mainCypress;
    private Cypress backupCypress;
    private YtTables mainTables;
    private YtTables backupTables;

    @BeforeEach
    void setUp() {
        mainYt = mock(Yt.class);
        mainCypress = mock(Cypress.class);
        mainTables = mock(YtTables.class);

        backupYt = mock(Yt.class);
        backupCypress = mock(Cypress.class);
        backupTables = mock(YtTables.class);

        when(mainYt.cypress()).thenReturn(mainCypress);
        when(backupYt.cypress()).thenReturn(backupCypress);
        when(mainYt.tables()).thenReturn(mainTables);
        when(backupYt.tables()).thenReturn(backupTables);

        ytWriter = new YtWriter(
            mainYt,
            backupYt
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mainYt, mainCypress, mainTables, backupYt, backupCypress, backupTables);
    }

    @Test
    void writeTable() {
        TestRecord record1 = new TestRecord().setAbc("a");
        TestRecord record2 = new TestRecord().setAbc("b");

        ytWriter.writeTable(
            Y_PATH,
            EXPIRES_AT,
            Arrays.asList(record1, record2),
            TestRecord.class,
            TEST_SCHEMA,
            1,
            true
        );

        YTreeMapNode node1 = convert(record1);
        YTreeMapNode node2 = convert(record2);

        verify(mainTables).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(backupTables).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(mainYt).cypress();
        verify(mainYt).tables();
        verify(backupYt).cypress();
        verify(backupYt).tables();

        verify(mainCypress).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
        verify(backupCypress).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
    }

    @Test
    void writeMainTableWithoutBackup() {
        TestRecord record1 = new TestRecord().setAbc("a");
        TestRecord record2 = new TestRecord().setAbc("b");

        ytWriter.writeTable(
            Y_PATH,
            EXPIRES_AT,
            Arrays.asList(record1, record2),
            TestRecord.class,
            TEST_SCHEMA,
            1,
            false
        );

        YTreeMapNode node1 = convert(record1);
        YTreeMapNode node2 = convert(record2);

        verify(mainTables).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(mainYt).cypress();
        verify(mainYt).tables();
        verify(mainCypress).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
    }

    @Test
    void writeTableMainFailed() {
        TestRecord record1 = new TestRecord().setAbc("a");
        TestRecord record2 = new TestRecord().setAbc("b");

        doThrow(new YtException(EXCEPTION_MESSAGE))
            .when(mainTables)
            .write(eq(Y_PATH), eq(YTableEntryTypes.YSON), any(Iterable.class));

        Assertions.assertThatThrownBy(() -> ytWriter.writeTable(
                    Y_PATH,
                    EXPIRES_AT,
                    Arrays.asList(record1, record2),
                    TestRecord.class,
                    TEST_SCHEMA,
                    1,
                    true
                )
            )
            .isInstanceOf(RuntimeException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(YtException.class)
            .extracting(x -> ((Exception) x).getMessage())
            .isEqualTo(EXCEPTION_MESSAGE);

        YTreeMapNode node1 = convert(record1);
        YTreeMapNode node2 = convert(record2);

        verify(mainTables, times(2)).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(backupTables).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(mainYt, times(2)).cypress();
        verify(mainYt, times(2)).tables();
        verify(backupYt).cypress();
        verify(backupYt).tables();

        verify(mainCypress, times(2)).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
        verify(backupCypress).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
    }

    @Test
    void writeTableBackupFailed() {
        TestRecord record1 = new TestRecord().setAbc("a");
        TestRecord record2 = new TestRecord().setAbc("b");

        doThrow(new YtException(EXCEPTION_MESSAGE))
            .when(backupTables)
            .write(eq(Y_PATH), eq(YTableEntryTypes.YSON), any(Iterable.class));

        Assertions.assertThatThrownBy(() -> ytWriter.writeTable(
                    Y_PATH,
                    EXPIRES_AT,
                    Arrays.asList(record1, record2),
                    TestRecord.class,
                    TEST_SCHEMA,
                    1,
                    true
                )
            )
            .isInstanceOf(RuntimeException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(YtException.class)
            .extracting(x -> ((Exception) x).getMessage())
            .isEqualTo(EXCEPTION_MESSAGE);

        YTreeMapNode node1 = convert(record1);
        YTreeMapNode node2 = convert(record2);

        verify(mainTables).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(backupTables, times(2)).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(mainYt).cypress();
        verify(mainYt).tables();
        verify(backupYt, times(2)).cypress();
        verify(backupYt, times(2)).tables();

        verify(mainCypress).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
        verify(backupCypress, times(2)).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
    }

    @Test
    void writeTableMainRetries() {
        TestRecord record1 = new TestRecord().setAbc("a");
        TestRecord record2 = new TestRecord().setAbc("b");

        doThrow(new YtException(EXCEPTION_MESSAGE)) // 1st call failed
            .doAnswer(invocation -> null) // 2nd call success
            .when(mainTables)
            .write(eq(Y_PATH), eq(YTableEntryTypes.YSON), any(Iterable.class));

        ytWriter.writeTable(
            Y_PATH,
            EXPIRES_AT,
            Arrays.asList(record1, record2),
            TestRecord.class,
            TEST_SCHEMA,
            1,
            true
        );

        YTreeMapNode node1 = convert(record1);
        YTreeMapNode node2 = convert(record2);

        verify(mainTables, times(2)).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(backupTables).write(
            eq(Y_PATH),
            eq(YTableEntryTypes.YSON),
            eq(Cf.list(
                node1,
                node2
            ))
        );

        verify(mainYt, times(2)).cypress();
        verify(mainYt, times(2)).tables();
        verify(backupYt).cypress();
        verify(backupYt).tables();

        verify(mainCypress, times(2)).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
        verify(backupCypress).create(argThat(CREATE_NODE_ARGUMENT_MATCHER));
    }

    private YTreeMapNode convert(TestRecord record1) {
        YTreeBuilder builder1 = new YTreeBuilder();
        SERIALIZER.serialize(record1, builder1);
        YTreeMapNode node1 = (YTreeMapNode) builder1.build();
        return node1;
    }

    @YTreeObject(
        nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY,
        optionSerializationStrategy = OptionSerializationStrategy.EMPTY_OPTION
    )
    @Data
    @Accessors(chain = true)
    public static class TestRecord {
        @YTreeField
        String abc;
    }
}
