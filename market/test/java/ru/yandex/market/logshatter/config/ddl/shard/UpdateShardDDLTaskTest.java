package ru.yandex.market.logshatter.config.ddl.shard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.DDL;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLException;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 08.11.16
 */
public class UpdateShardDDLTaskTest {
    public static final String FIRST_HOST = "first.host";
    public static final String SECOND_HOST = "second.host";
    private List<LogShatterConfig> configs = Collections.singletonList(
        config(
            "some_parser.json",
            "db.some_log",
            "db.some_log_local"
        )
    );
    private UpdateHostDDLTaskFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = mock(UpdateHostDDLTaskFactory.class);
    }

    @Test
    public void oneSuccessfulHost() throws Exception {
        when(factory.create(any(), any())).thenReturn(successfulTask());

        UpdateShardDDLTask sut = new UpdateShardDDLTaskImpl(
            factory,
            Collections.singletonList("host"),
            configs,
            false,
            false
        );

        UpdateShardDDLResult result = sut.run();
        Assert.assertThat(result, instanceOf(UpdateShardDDLResult.Success.class));
    }

    @Test
    public void oneFailingAndOneSuccessfulHost() throws Exception {
        when(factory.create(eq(FIRST_HOST), any())).thenReturn(successfulTask());
        when(factory.create(eq(SECOND_HOST), any())).thenReturn(failingTask(SECOND_HOST));

        UpdateShardDDLTask sut = new UpdateShardDDLTaskImpl(
            factory,
            Arrays.asList(FIRST_HOST, SECOND_HOST),
            configs,
            false,
            false
        );

        UpdateShardDDLResult result = sut.run();
        Assert.assertThat(result, instanceOf(UpdateShardDDLResult.PartialSuccess.class));
    }

    @Test
    public void oneFailingAndOneManualDDLHost() throws Exception {
        when(factory.create(eq(FIRST_HOST), any())).thenReturn(successfulTask());
        when(factory.create(eq(SECOND_HOST), any())).thenReturn(manaualDDLRequiredTask());

        UpdateShardDDLTask sut = new UpdateShardDDLTaskImpl(
            factory,
            Arrays.asList(FIRST_HOST, SECOND_HOST),
            configs,
            false,
            false
        );

        UpdateShardDDLResult result = sut.run();
        Assert.assertThat(result, instanceOf(UpdateShardDDLResult.ManualDDLRequired.class));
    }

    private UpdateHostDDLTask successfulTask() {
        return () -> new UpdateHostDDLResult.Success(new DDL(
            "",
            new ClickHouseTableDefinitionImpl("test", "test", Collections.emptyList(), null)
        ));
    }

    private UpdateHostDDLTask manaualDDLRequiredTask() {
        return () -> new UpdateHostDDLResult.ManualDDLRequired(mock(DDL.class));
    }

    private UpdateHostDDLTask failingTask(String host) {
        return () -> new UpdateHostDDLResult.Error(
            mock(UpdateDDLException.class)
        );
    }

    public static class SomeParser implements LogParser {
        public SomeParser() {
        }

        @Override
        public TableDescription getTableDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void parse(String line, ParserContext context) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    private static LogShatterConfig config(String configFileName, String table, String localTable) {
        return LogShatterConfig.newBuilder()
            .setDataClickHouseTable(new ClickHouseTableDefinitionImpl(localTable, Collections.emptyList(), null))
            .setDistributedClickHouseTable(new ClickHouseTableDefinitionImpl(table, Collections.emptyList(), null))
            .setConfigId(configFileName)
            .setParserProvider(mock(LogParserProvider.class))
            .build();
    }

}
