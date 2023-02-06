package ru.yandex.direct.jobs.abt.check;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableExistsCheckerTest {

    @Test
    void tableExistsTest() {
        var ytProvider = mock(YtProvider.class);
        var ytOperator = mock(YtOperator.class);
        var tableExistsChecker = new TableExistsChecker(ytProvider);
        when(ytProvider.getOperator(any())).thenReturn(ytOperator);
        when(ytOperator.exists(any())).thenReturn(true);
        var exists = tableExistsChecker.check(YtCluster.ZENO, "table");
        assertThat(exists).isTrue();
    }

    @Test
    void tableNotExistsTest() {
        var ytProvider = mock(YtProvider.class);
        var ytOperator = mock(YtOperator.class);
        var tableExistsChecker = new TableExistsChecker(ytProvider);
        when(ytProvider.getOperator(any())).thenReturn(ytOperator);
        when(ytOperator.exists(any())).thenReturn(false);
        var exists = tableExistsChecker.check(YtCluster.ZENO, "table");
        assertThat(exists).isFalse();
    }
}
