package ru.yandex.direct.jobs.xlshistory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.mdsfile.service.MdsFileService;
import ru.yandex.direct.core.entity.xlshistory.repository.XlsHistoryRepository;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты на джобу {@link ClearXlsJob}
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class ClearXlsJobTest {

    private final static int SHARD = 2;

    @Mock
    private XlsHistoryRepository xlsHistoryRepository;
    @Mock
    private MdsFileService mdsFileService;

    private ClearXlsJob job;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        job = new ClearXlsJob(SHARD, xlsHistoryRepository, mdsFileService);
    }


    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    /**
     * Тест: если данных из Xls_History таблицы для удаления нет -> функция удаления
     * {@link XlsHistoryRepository#deleteById} XlsHistory не будет вызвана
     */
    @Test
    void whenThereIsNoXlsHistory() {
        when(xlsHistoryRepository.getIdsByLogdateLessThan(anyInt(), any(LocalDateTime.class), anyInt()))
                .thenReturn(Collections.emptyList());
        executeJob();
        verify(xlsHistoryRepository, never()).deleteById(anyInt(), anyCollection());
    }

    /**
     * Тест: если получаемые данные из Xls_History таблицы будут разбиты на несколько чанков -> в функцию удаления
     * {@link XlsHistoryRepository#deleteById} XlsHistory будут переданы все id, и эта функция будет вызвана
     * 'количество чанков' раз
     */
    @Test
    void whenThereIsManyChunksOfXlsHistory() {
        when(xlsHistoryRepository.getIdsByLogdateLessThan(anyInt(), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(1L))
                .thenReturn(List.of(2L))
                .thenReturn(Collections.emptyList());
        executeJob();
        verify(xlsHistoryRepository).deleteById(anyInt(), ArgumentMatchers.eq(List.of(1L)));
        verify(xlsHistoryRepository).deleteById(anyInt(), ArgumentMatchers.eq(List.of(2L)));
        // проверка на количество вызовов метода (количество итераций)
        verify(xlsHistoryRepository, times(2)).deleteById(anyInt(), anyCollection());
    }
}
