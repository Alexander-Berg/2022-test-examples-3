package ru.yandex.market.delivery.rupostintegrationapp.service.implementation.dbf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.linuxense.javadbf.DBFException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.dbf.DbfCodeRepository;
import ru.yandex.market.delivery.rupostintegrationapp.dao.dbf.DbfFileRepository;
import ru.yandex.market.delivery.rupostintegrationapp.log.JobStatusLogger;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfCode;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfFile;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.enums.DbfStatus;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfParser;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfStorage;

import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfParserServiceTest extends BaseTest {

    @Mock
    private DbfStorage dbfStorage;
    @Mock
    private DbfParser dbfParser;
    @Mock
    private DbfFileRepository dbfFileRepository;
    @Mock
    private DbfCodeRepository dbfCodeRepository;

    @InjectMocks
    private DbfParserService dbfParserService;

    @Mock
    private JobStatusLogger jobStatusLogger;

    @Test
    void executeNoFiles() {
        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(any(), any()))
            .thenReturn(Optional.empty());
        dbfParserService.execute();
        verify(dbfFileRepository, never()).save(any(DbfFile.class));
    }

    @Test
    void executeFailedToReadFile() {
        ArgumentCaptor<DbfStatus> captorStatus = ArgumentCaptor.forClass(DbfStatus.class);
        doThrow(new DBFException("test")).when(dbfParser).readCodes(any());

        DbfFile dbfFile = mock(DbfFile.class);

        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(any(), any()))
            .thenReturn(Optional.of(dbfFile));

        dbfParserService.execute();

        verify(dbfFile, times(2)).setStatus(captorStatus.capture());
        verify(dbfFileRepository, times(2)).save(any(DbfFile.class));
        verify(dbfFileRepository).findFirstByStatusInAndRetriesIsGreaterThan(any(), any());
        verify(dbfParser).readCodes(any());
        softly.assertThat(captorStatus.getAllValues().get(0)).isEqualTo(DbfStatus.IN_PROGRESS);
        softly.assertThat(captorStatus.getAllValues().get(1)).isEqualTo(DbfStatus.ERROR_BAD_FILE);
    }

    @Test
    void executeFailedStorageGet() throws IOException {
        ArgumentCaptor<DbfStatus> captorStatus = ArgumentCaptor.forClass(DbfStatus.class);
        when(dbfStorage.get(any())).thenThrow(new IOException("test"));

        DbfFile dbfFile = mock(DbfFile.class);

        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(any(), any()))
            .thenReturn(Optional.of(dbfFile));

        dbfParserService.execute();

        verify(dbfFile, times(2)).setStatus(captorStatus.capture());
        verify(dbfFileRepository, times(2)).save(any(DbfFile.class));
        verify(dbfFileRepository).findFirstByStatusInAndRetriesIsGreaterThan(any(), any());
        verify(dbfParser, never()).readCodes(any());
        softly.assertThat(captorStatus.getAllValues().get(0)).isEqualTo(DbfStatus.IN_PROGRESS);
        softly.assertThat(captorStatus.getAllValues().get(1)).isEqualTo(DbfStatus.ERROR_BAD_FILE);
    }

    @Test
    void executeEmptyCodes() {
        ArgumentCaptor<DbfStatus> captorStatus = ArgumentCaptor.forClass(DbfStatus.class);
        when(dbfParser.readCodes(any())).thenReturn(Collections.emptyList());

        DbfFile dbfFile = mock(DbfFile.class);

        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(any(), any()))
            .thenReturn(Optional.of(dbfFile));

        dbfParserService.execute();

        verify(dbfFile, times(2)).setStatus(captorStatus.capture());
        verify(dbfFileRepository, times(2)).save(any(DbfFile.class));
        verify(dbfFileRepository).findFirstByStatusInAndRetriesIsGreaterThan(any(), any());
        verify(dbfParser).readCodes(any());
        softly.assertThat(captorStatus.getAllValues().get(0)).isEqualTo(DbfStatus.IN_PROGRESS);
        softly.assertThat(captorStatus.getAllValues().get(1)).isEqualTo(DbfStatus.ERROR_EMPTY_FILE);
    }

    @Test
    void executeEmptySearch() {
        ArgumentCaptor<DbfStatus> captorStatus = ArgumentCaptor.forClass(DbfStatus.class);
        ArgumentCaptor<List> captorListCodes = ArgumentCaptor.forClass(List.class);
        when(dbfParser.readCodes(any())).thenReturn(Collections.singletonList(new DbfCode()));

        DbfFile dbfFile = mock(DbfFile.class);

        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(any(), any()))
            .thenReturn(Optional.of(dbfFile));

        dbfParserService.execute();

        verify(dbfFile, times(2)).setStatus(captorStatus.capture());
        verify(dbfFileRepository, times(2)).save(any(DbfFile.class));
        verify(dbfFileRepository).findFirstByStatusInAndRetriesIsGreaterThan(any(), any());
        verify(dbfCodeRepository).findByCodeIsIn(anyList());
        verify(dbfParser).readCodes(any());
        verify(dbfFile, times(2)).addCodes(captorListCodes.capture());

        softly.assertThat(captorListCodes.getAllValues()).hasSize(2);
        softly.assertThat(captorListCodes.getAllValues().get(0)).hasSize(0); // сохраняем старых кодов 0
        softly.assertThat(captorListCodes.getAllValues().get(1)).hasSize(1); // сохраняем новых кодов 1

        softly.assertThat(captorStatus.getAllValues().get(0)).isEqualTo(DbfStatus.IN_PROGRESS);
        softly.assertThat(captorStatus.getAllValues().get(1)).isEqualTo(DbfStatus.READY_TO_SEARCH);
    }


    @Test
    void executeCodeInListAndInBaseAreTheSameResultEmptyFile() {
        DbfCode code = new DbfCode();
        code.setCode("test");

        ArgumentCaptor<DbfStatus> captorStatus = ArgumentCaptor.forClass(DbfStatus.class);

        when(dbfParser.readCodes(any())).thenReturn(Collections.singletonList(code));

        DbfFile dbfFile = mock(DbfFile.class);
        when(dbfFile.getCodes()).thenReturn(Collections.singletonList(code));

        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(any(), any()))
            .thenReturn(Optional.of(dbfFile));

        dbfParserService.execute();

        verify(dbfFile, times(2)).setStatus(captorStatus.capture());
        verify(dbfFileRepository, times(2)).save(any(DbfFile.class));
        verify(dbfFileRepository).findFirstByStatusInAndRetriesIsGreaterThan(any(), any());
        verify(dbfCodeRepository, never()).findByCodeIsIn(anyList());
        verify(dbfParser).readCodes(any());
        softly.assertThat(captorStatus.getAllValues().get(0)).isEqualTo(DbfStatus.IN_PROGRESS);
        softly.assertThat(captorStatus.getAllValues().get(1)).isEqualTo(DbfStatus.ERROR_EMPTY_FILE);
    }

    @Test
    void executeCodeInListAndFindInBaseResultEmptyFile() {
        DbfCode code = new DbfCode();
        code.setCode("test");
        code.setId(1L);

        DbfCode code2 = new DbfCode();
        code2.setCode("test");
        code2.setId(2L);

        ArgumentCaptor<List> captorListCodes = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<DbfStatus> captorStatus = ArgumentCaptor.forClass(DbfStatus.class);
        when(dbfParser.readCodes(any())).thenReturn(Collections.singletonList(code));
        when(dbfCodeRepository.findByCodeIsIn(anyList())).thenReturn(Arrays.asList(code, code2));

        DbfFile dbfFile = mock(DbfFile.class);

        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(any(), any()))
            .thenReturn(Optional.of(dbfFile));

        dbfParserService.execute();

        verify(dbfFile, times(2)).setStatus(captorStatus.capture());
        verify(dbfFileRepository, times(2)).save(any(DbfFile.class));
        verify(dbfFileRepository).findFirstByStatusInAndRetriesIsGreaterThan(any(), any());
        verify(dbfCodeRepository).findByCodeIsIn(anyList());
        verify(dbfParser).readCodes(any());

        verify(dbfFile, times(2)).addCodes(captorListCodes.capture());

        softly.assertThat(captorListCodes.getAllValues()).hasSize(2);
        softly.assertThat(captorListCodes.getAllValues().get(0)).hasSize(2); // сохраняем старых кодов 2
        softly.assertThat(captorListCodes.getAllValues().get(1)).hasSize(0); // сохраняем новых кодов 0

        softly.assertThat(captorStatus.getAllValues().get(0)).isEqualTo(DbfStatus.IN_PROGRESS);
        softly.assertThat(captorStatus.getAllValues().get(1)).isEqualTo(DbfStatus.READY_TO_SEARCH);
    }
}
