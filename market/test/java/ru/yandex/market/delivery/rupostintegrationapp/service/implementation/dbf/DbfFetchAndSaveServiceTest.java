package ru.yandex.market.delivery.rupostintegrationapp.service.implementation.dbf;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.dbf.DbfMailRepository;
import ru.yandex.market.delivery.rupostintegrationapp.log.JobStatusLogger;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfFile;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfMail;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.enums.DbfStatus;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfFetcher;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfStorage;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfFetchAndSaveServiceTest extends BaseTest {

    @InjectMocks
    private DbfFetchAndSaveService dbfFetchAndSaveService;

    @Mock
    private DbfMailRepository dbfMailRepository;
    @Mock
    private DbfFetcher dbfFetcher;
    @Mock
    private DbfStorage dbfStorage;
    @Mock
    private JobStatusLogger jobStatusLogger;

    private String letterId = "testLetterId";

    @Test
    void executeLetterExist() throws Exception {
        DbfMail mail = new DbfMail();
        mail.setLetterId(letterId);

        when(dbfMailRepository.existsByLetterId(eq(letterId))).thenReturn(true);

        when(dbfFetcher.fetch()).thenReturn(new HashMap<>() {{
            put(mail, Collections.singletonList(mock(File.class)));
        }});

        dbfFetchAndSaveService.execute();

        verify(dbfMailRepository, never()).save(any(DbfMail.class));
    }

    @Test
    void executeSaveEmptyList() throws Exception {
        DbfMail mail = new DbfMail();
        mail.setLetterId(letterId);

        File file = mock(File.class);

        when(dbfMailRepository.existsByLetterId(eq(letterId))).thenReturn(false);

        when(dbfFetcher.fetch()).thenReturn(new HashMap<DbfMail, List<File>>() {{
            put(mail, Collections.singletonList(file));
        }});

        dbfFetchAndSaveService.execute();

        verify(dbfStorage).save(eq(file));
        softly.assertThat(mail.getFiles()).isEmpty();
    }

    @Test
    void execute() throws Exception {

        String filename = "testFileName";
        String filepath = "testPath";

        DbfMail mail = new DbfMail();
        mail.setLetterId(letterId);

        File file = mock(File.class);

        when(file.getName()).thenReturn(filename);
        when(dbfMailRepository.existsByLetterId(eq(letterId))).thenReturn(false);
        when(dbfStorage.save(eq(file))).thenReturn(filepath);
        when(dbfFetcher.fetch()).thenReturn(new HashMap<DbfMail, List<File>>() {{
            put(mail, Collections.singletonList(file));
        }});

        softly.assertThat(mail.getFiles()).isEmpty();

        dbfFetchAndSaveService.execute();

        DbfFile dbfFile = mail.getFiles().get(0);

        verify(dbfStorage).save(eq(file));
        verify(dbfMailRepository).save(any(DbfMail.class));

        softly.assertThat(mail.getFiles()).isNotEmpty();
        softly.assertThat(mail.getFiles()).hasSize(1);
        softly.assertThat(dbfFile.getFilepathOrig()).isEqualTo(filepath);
        softly.assertThat(dbfFile.getFilename()).isEqualTo(filename);
        softly.assertThat(dbfFile.getStatus()).isEqualTo(DbfStatus.READY_TO_PARSE);
        softly.assertThat(dbfFile.getLetterId()).isEqualTo(letterId);
    }
}
