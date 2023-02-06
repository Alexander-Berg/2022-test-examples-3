package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.direct.binlogbroker.logbroker_utils.models.SourceType;
import ru.yandex.direct.binlogbroker.logbrokerwriter.models.ImmutableSourceState;
import ru.yandex.direct.mysql.schema.ColumnSchema;
import ru.yandex.direct.mysql.schema.DatabaseSchema;
import ru.yandex.direct.mysql.schema.ServerSchema;
import ru.yandex.direct.mysql.schema.TableSchema;
import ru.yandex.direct.mysql.schema.TableType;
import ru.yandex.inside.yt.kosher.impl.common.YtErrorMapping;

import static ru.yandex.direct.env.EnvironmentType.DEVTEST;

@ParametersAreNonnullByDefault
public class YtSourceStateRepositoryTest {
    private static final SourceType SOURCE_TYPE = SourceType.fromType(DEVTEST, "ppc:1");

    @Rule
    public JunitLocalYt localYt = new JunitLocalYt();
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    @Rule
    public Timeout timeout = new Timeout(2, TimeUnit.MINUTES);

    @Test
    public void emptyStateAllowed() {
        try (YtSourceStateRepository ytSourceStateRepository = new YtSourceStateRepository(
                localYt.getYt(), localYt.getTestPath(), true)) {
            ImmutableSourceState state = ytSourceStateRepository.loadState(SOURCE_TYPE);
            softly.assertThat(state).isEqualTo(new ImmutableSourceState());
        }
    }

    @Test
    public void emptyStateDisallowed() {
        try (YtSourceStateRepository ytSourceStateRepository = new YtSourceStateRepository(
                localYt.getYt(), localYt.getTestPath(), false)) {
            softly.assertThatCode(() -> ytSourceStateRepository.loadState(SOURCE_TYPE))
                    .hasRootCauseInstanceOf(YtErrorMapping.ResolveError.class);
        }
    }

    @Test
    public void sequentialSavesAndLoads() {
        ImmutableSourceState state3;
        ImmutableSourceState loadedState3;
        try (YtSourceStateRepository ytSourceStateRepository = new YtSourceStateRepository(
                localYt.getYt(), localYt.getTestPath(), false)) {
            ServerSchema serverSchema1 = new ServerSchema(ImmutableList.of(new DatabaseSchema(
                    "dbname",
                    "create database dbname",
                    ImmutableList.of(new TableSchema(
                            "tablename",
                            TableType.TABLE,
                            "create table tablename hurr durr",
                            ImmutableList.of(new ColumnSchema(
                                    "colname",
                                    "int",
                                    "int",
                                    "0",
                                    false)),
                            ImmutableList.of(),
                            ImmutableList.of())),
                    ImmutableList.of())));
            byte[] serializedServerSchema1 = serverSchema1.toJsonBytes();
            ImmutableSourceState state1 = new ImmutableSourceState(123, 0, "foobar:1-123", 0, serializedServerSchema1);

            ytSourceStateRepository.saveState(SOURCE_TYPE, state1);
            ImmutableSourceState loadedState1 = ytSourceStateRepository.loadState(SOURCE_TYPE);
            softly.assertThat(loadedState1)
                    .describedAs("Save and load state initially")
                    .isEqualTo(state1);

            ImmutableSourceState state2 = new ImmutableSourceState(456, 0, "foobar:1-456", 0, serializedServerSchema1);

            ytSourceStateRepository.saveState(SOURCE_TYPE, state2);
            ImmutableSourceState loadedState2 = ytSourceStateRepository.loadState(SOURCE_TYPE);
            softly.assertThat(loadedState2)
                    .describedAs("Changed only seq_no and gtid_set")
                    .isEqualTo(state2);

            ServerSchema serverSchema2 = new ServerSchema(ImmutableList.of(new DatabaseSchema(
                    "dbname_changed",
                    "create database dbname_changed",
                    ImmutableList.of(new TableSchema(
                            "tablename_changed",
                            TableType.TABLE,
                            "create table tablename_changed hurr durr",
                            ImmutableList.of(new ColumnSchema(
                                    "colname_changed",
                                    "int",
                                    "int",
                                    "0",
                                    false)),
                            ImmutableList.of(),
                            ImmutableList.of())),
                    ImmutableList.of())));

            byte[] serializedServerSchema2 = serverSchema2.toJsonBytes();
            state3 = new ImmutableSourceState(789, 0, "foobar:1-789", 0, serializedServerSchema2);

            ytSourceStateRepository.saveState(SOURCE_TYPE, state3);
            loadedState3 = ytSourceStateRepository.loadState(SOURCE_TYPE);
        }
        softly.assertThat(loadedState3)
                .describedAs("Changed seq_no, gtid_set and schema")
                .isEqualTo(state3);
    }

    @Test
    public void severalStates() {
        List<ImmutableSourceState> expectedStates;
        List<ImmutableSourceState> loadedStates;
        try (YtSourceStateRepository ytSourceStateRepository = new YtSourceStateRepository(
                localYt.getYt(), localYt.getTestPath(), false)) {
            List<SourceType> sources = ImmutableList.of(
                    SourceType.fromType(DEVTEST, "ppc:1"),
                    SourceType.fromType(DEVTEST, "ppc:2"),
                    SourceType.fromType(DEVTEST, "ppc:3"));
            expectedStates = new ArrayList<>();

            for (int i = 0; i < sources.size(); ++i) {
                ServerSchema serverSchema = new ServerSchema(ImmutableList.of(new DatabaseSchema(
                        "dbname" + i,
                        "create database dbname" + i,
                        ImmutableList.of(new TableSchema(
                                "tablename" + i,
                                TableType.TABLE,
                                "create table tablename hurr durr",
                                ImmutableList.of(new ColumnSchema(
                                        "colname" + i,
                                        "int",
                                        "int",
                                        "0",
                                        false)),
                                ImmutableList.of(),
                                ImmutableList.of())),
                        ImmutableList.of())));
                byte[] serializedSchema = serverSchema.toJsonBytes();
                expectedStates.add(new ImmutableSourceState(1000 + i * 10, 0, "foobar:1-" + (1000 + i * 10), 0,
                        serializedSchema));

                ytSourceStateRepository.saveState(sources.get(i), expectedStates.get(i));
            }

            loadedStates = new ArrayList<>();
            for (SourceType source : sources) {
                loadedStates.add(ytSourceStateRepository.loadState(source));
            }
        }

        softly.assertThat(loadedStates).isEqualTo(expectedStates);
    }
}
