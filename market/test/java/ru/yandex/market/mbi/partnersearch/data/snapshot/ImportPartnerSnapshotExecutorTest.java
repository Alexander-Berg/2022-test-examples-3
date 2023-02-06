package ru.yandex.market.mbi.partnersearch.data.snapshot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.quartz.task.ImportDataSnapshotExecutor;
import ru.yandex.market.yt.util.reader.YtTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Проверяем запуск импорта партнеров {@link ImportDataSnapshotExecutor}
 */
class ImportPartnerSnapshotExecutorTest extends AbstractFunctionalTest {

    @Autowired
    private ImportDataSnapshotExecutor importPartnerSnapshotExecutor;
    @Autowired
    private YtTemplate dataImportYtTemplate;

    @Test
    @DbUnitDataSet(before = "ImportPartnerSnapshotExecutorTest.before.csv",
            after = "ImportPartnerSnapshotExecutorTest.after.csv")
    void checkExecutor() {
        when(dataImportYtTemplate.getFromYt(any())).thenReturn("last/table/path");

        importPartnerSnapshotExecutor.doJob(null);
        verify(dataImportYtTemplate, times(1)).getFromYt(any());
        verify(dataImportYtTemplate, times(1)).runInYt(any());

        importPartnerSnapshotExecutor.doJob(null);
        verify(dataImportYtTemplate, times(2)).getFromYt(any());
        verifyNoMoreInteractions(dataImportYtTemplate);

    }

    @Test
    @DbUnitDataSet(before = "ImportPartnerSnapshotExecutorTest.before.csv",
            after = "ImportPartnerSnapshotExecutorTest.before.csv")
    void checkExecutorFail() {
        doThrow(new RuntimeException("check fail")).when(dataImportYtTemplate).runInYt(any());

        assertThatThrownBy(() -> importPartnerSnapshotExecutor.doJob(null))
                .isInstanceOf(RuntimeException.class);
        verify(dataImportYtTemplate, times(1)).getFromYt(any());
    }
}
