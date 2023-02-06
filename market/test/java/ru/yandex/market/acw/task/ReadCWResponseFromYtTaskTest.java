package ru.yandex.market.acw.task;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.acw.config.Base;
import ru.yandex.market.acw.internal.CleanWebImageResponseProcessor;
import ru.yandex.market.acw.internal.CleanWebTextResponseProcessor;
import ru.yandex.market.acw.json.CWRawVerdict;
import ru.yandex.market.acw.yt.CWYtManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReadCWResponseFromYtTaskTest extends Base {

    private final static String TABLE_NAME = "table1";

    private ReadCWResponseFromYtTask task;
    private CWYtManager manager;
    private CleanWebTextResponseProcessor textResponseProcessor;
    private CleanWebImageResponseProcessor imageResponseProcessor;

    @BeforeEach
    void setup() {
        manager = mock(CWYtManager.class);
        var res = List.of(new CWRawVerdict("1", "name", "true", null, null, "text"),
                        new CWRawVerdict("1_2_3", "name", "true", null, null, "text"));

        when(manager.getTableNamesFromCWDirectory()).thenReturn(List.of(TABLE_NAME));
        when(manager.getResult(any())).thenReturn(res);

        textResponseProcessor = mock(CleanWebTextResponseProcessor.class);
        imageResponseProcessor = mock(CleanWebImageResponseProcessor.class);
        task = new ReadCWResponseFromYtTask(List.of(manager), extendedCwYtProcessedTablesDao, imageResponseProcessor,
                textResponseProcessor, 300);
    }

    @Test
    @DisplayName("check table names are marked processed")
    void checkTablesMarked() {
        assertThat(extendedCwYtProcessedTablesDao.findAll()).isEmpty();
        task.processYt();
        assertThat(extendedCwYtProcessedTablesDao.fetchByTableName(TABLE_NAME).size()).isOne();
        verify(manager, times(1)).backupTables(Collections.singletonList(TABLE_NAME));
    }

    @Test
    @DisplayName("check incorrect ids are skipped")
    void checkIncorrectIdsSkipped() {
        task.processYt();

        ArgumentCaptor<Map> captorMap = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> captorString = ArgumentCaptor.forClass(String.class);
        verify(textResponseProcessor).processAsyncTextResponse(captorMap.capture(), captorString.capture());
        assertThat(captorMap.getValue().size()).isEqualTo(1);
        assertThat(captorMap.getValue().keySet()).containsExactly(1L);
    }
}
