package ru.yandex.market.yt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.billing.util.yt.YtCluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class YtClusterStub extends YtCluster {
    private final Cypress cypress = Mockito.mock(Cypress.class);
    private final YtTables tables = Mockito.mock(YtTables.class);

    public YtClusterStub(String name) {
        super(name, Mockito.mock(Yt.class));

        when(getYt().cypress()).thenReturn(cypress);
        when(getYt().tables()).thenReturn(tables);
    }

    public void addTable(String tablePath, List<YTreeMapNode> records) {
        doAnswer(answerForTablesRead(records))
                .when(tables).read(
                        argThat(ypath -> ypath.toString().equals(tablePath)),
                        eq(YTableEntryTypes.YSON),
                        any(Consumer.class)
                );

        doReturn(true).when(cypress).exists(
                (YPath) argThat(ypath -> ypath.toString().equals(tablePath))
        );
    }

    public void simulateReadTableError(String tablePath, Throwable error) {
        doThrow(error).when(tables).read(
                argThat(ypath -> ypath.toString().equals(tablePath)),
                eq(YTableEntryTypes.YSON),
                any(Consumer.class)
        );
    }

    @NotNull
    private Answer<Void> answerForTablesRead(List<YTreeMapNode> rawResults) {
        return invocation -> {
            Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
            new ArrayList<>(rawResults).forEach(consumer);
            return null;
        };
    }
}
