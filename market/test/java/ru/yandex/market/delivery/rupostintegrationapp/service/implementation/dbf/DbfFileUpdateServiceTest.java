package ru.yandex.market.delivery.rupostintegrationapp.service.implementation.dbf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.dbf.DbfFileRepository;
import ru.yandex.market.delivery.rupostintegrationapp.log.JobStatusLogger;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfFile;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.enums.DbfStatus;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfParser;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfStorage;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfFileUpdateServiceTest extends BaseTest {

    @Mock
    private DbfStorage dbfStorage;
    @Mock
    private DbfParser dbfParser;
    @Mock
    private DbfFileRepository dbfFileRepository;

    @Mock
    private JobStatusLogger jobStatusLogger;

    @InjectMocks
    private DbfFileUpdateService dbfFileUpdateService;

    private String origPath = "origPath";

    @Test
    void execute() throws IOException {
        DbfFile dbfFile = new DbfFile();
        dbfFile.setFilepathOrig(origPath);

        softly.assertThat(dbfFile.getRetries()).isEqualTo(DbfFile.RETRIES_COUNT);

        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(
            eq(Collections.singletonList(DbfStatus.READY_TO_GENERATE_DBF)), eq(0)
        )).thenReturn(Optional.of(dbfFile));
        when(dbfStorage.get(anyString())).thenReturn(mock(FileInputStream.class));
        when(dbfParser.updateCodes(any(FileInputStream.class), isNull())).thenReturn(mock(File.class));
        when(dbfStorage.save(any(File.class))).thenReturn("test");

        dbfFileUpdateService.execute();

        verify(dbfStorage).get(any());
        verify(dbfFileRepository, times(2)).save(any(DbfFile.class));

        softly.assertThat(dbfFile.getRetries()).isEqualTo(DbfFile.RETRIES_COUNT - 1);
        softly.assertThat(dbfFile.getStatus()).isEqualTo(DbfStatus.READY_TO_SEND);
    }

    @Test
    void executeFail() throws IOException {
        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(
            eq(Collections.singletonList(DbfStatus.READY_TO_GENERATE_DBF)), eq(0)
        )).thenReturn(Optional.empty());

        dbfFileUpdateService.execute();

        verify(dbfStorage, never()).get(any());
    }

    @Test
    void executeFailWithExeption() throws IOException {
        DbfFile dbfFile = new DbfFile();
        dbfFile.setFilepathOrig(origPath);

        softly.assertThat(dbfFile.getRetries()).isEqualTo(DbfFile.RETRIES_COUNT);

        when(dbfFileRepository.findFirstByStatusInAndRetriesIsGreaterThan(
            eq(Collections.singletonList(DbfStatus.READY_TO_GENERATE_DBF)), eq(0)
        )).thenReturn(Optional.of(dbfFile));
        when(dbfStorage.get(anyString())).thenThrow(new IOException("No storage!"));

        dbfFileUpdateService.execute();

        verify(dbfStorage).get(any());
        verify(dbfFileRepository, times(2)).save(any(DbfFile.class));

        softly.assertThat(dbfFile.getRetries()).isEqualTo(DbfFile.RETRIES_COUNT - 1);
        softly.assertThat(dbfFile.getStatus()).isEqualTo(DbfStatus.ERROR_GENERATE_DBF);
    }
}
