package ru.yandex.market.delivery.rupostintegrationapp.service.implementation.dbf;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.dbf.DbfMailRepository;
import ru.yandex.market.delivery.rupostintegrationapp.log.JobStatusLogger;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfFile;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfMail;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.enums.DbfStatus;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfMailer;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfStorage;
import ru.yandex.market.delivery.rupostintegrationapp.util.DbfFilePacker;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DbfSendResponseServiceTest extends BaseTest {

    @Mock
    private DbfMailer dbfMailer;
    @Mock
    private DbfMailRepository dbfMailRepository;
    @Mock
    private DbfStorage dbfStorage;
    @Mock
    private MimeMessage mimeMessage;
    @Mock
    private DbfFilePacker dbfFilePacker;
    @Mock
    private FileInputStream fileInputStream;

    @Mock
    private JobStatusLogger jobStatusLogger;

    private DbfSendResponseService dbfSendResponseService;

    @BeforeEach
    void setUp() throws Exception {
        dbfSendResponseService = spy(new DbfSendResponseService(
            jobStatusLogger,
            dbfMailer,
            dbfMailRepository,
            dbfStorage
        ));

        doReturn(dbfFilePacker).when(dbfSendResponseService).makeFilePacker(any(), any());

        when(dbfFilePacker.getOutputFilename()).thenReturn(getRandomString());
        when(dbfFilePacker.getOutputFileSource()).thenReturn(getRandomRes());

        when(mimeMessage.reply(anyBoolean())).thenReturn(mimeMessage);
    }

    private String getRandomString() {
        return UUID.randomUUID().toString();
    }

    private ByteArrayResource getRandomRes() {
        return mock(ByteArrayResource.class);
    }

    @Test
    void executeEmpty() {
        dbfSendResponseService.execute();
        verify(dbfMailRepository, never()).save(any(DbfMail.class));
    }

    @Test
    void execute() throws Exception {
        List<DbfMail> mails = getMailsOK();
        when(dbfMailRepository.findAllByRepliedIsNullAndDeletedIsNull()).thenReturn(mails);
        when(dbfMailer.findById(anyString())).thenReturn(Optional.of(mimeMessage));
        when(dbfMailer.fillMessage(any(), any(), any(), any())).thenReturn(true);
        when(dbfMailer.send(any())).thenReturn(true);
        when(dbfFilePacker.pack()).thenReturn(true);
        when(dbfStorage.get(anyString())).thenReturn(fileInputStream);

        dbfSendResponseService.execute();

        verify(dbfMailRepository).findAllByRepliedIsNullAndDeletedIsNull();
        verify(dbfMailer, times(getMailsOK().size())).findById(anyString());
        verify(dbfMailer, times(getMailsOK().size())).findByIdAndMark(
            any(DbfMail.class), any(Flags.Flag.class), anyBoolean()
        );
        verify(dbfMailer, times(getMailsOK().size())).send(any());
        verify(dbfMailer, times(getMailsOK().size())).fillMessage(any(), any(), any(), any());
        verify(dbfFilePacker, times(
            getMailsOK()
                .stream()
                .mapToInt(m -> m.getFiles().size())
                .sum()
        )).pack();

        checkSend(mails);
    }

    @Test
    void executeBadEmails() throws Exception {
        List<DbfMail> mails = getMailsFail();
        when(dbfMailRepository.findAllByRepliedIsNullAndDeletedIsNull()).thenReturn(mails);
        when(dbfMailer.findById(anyString())).thenReturn(Optional.of(mimeMessage));
        when(dbfMailer.fillMessage(any(), any(), any(), any())).thenReturn(true);
        when(dbfMailer.send(any())).thenReturn(true);
        when(dbfFilePacker.pack()).thenReturn(true);
        when(dbfStorage.get(anyString())).thenReturn(fileInputStream);

        dbfSendResponseService.execute();

        verify(dbfMailRepository).findAllByRepliedIsNullAndDeletedIsNull();
        verify(dbfMailer, never()).findById(anyString());

        checkNotSend(mails);
    }

    @Test
    void executeNotAllBadEmails() throws Exception {
        List<DbfMail> mails = getMailsNotAllOk();
        when(dbfMailRepository.findAllByRepliedIsNullAndDeletedIsNull()).thenReturn(mails);
        when(dbfMailer.findById(anyString())).thenReturn(Optional.of(mimeMessage));
        when(dbfMailer.fillMessage(any(), any(), any(), any())).thenReturn(true);
        when(dbfMailer.send(any())).thenReturn(true);
        when(dbfFilePacker.pack()).thenReturn(true);
        when(dbfStorage.get(anyString())).thenReturn(fileInputStream);

        dbfSendResponseService.execute();

        verify(dbfMailRepository).findAllByRepliedIsNullAndDeletedIsNull();
        verify(dbfMailer, times(getMailsOK().size())).findById(anyString());
        verify(dbfMailer, times(getMailsOK().size())).findByIdAndMark(
            any(DbfMail.class), any(Flags.Flag.class), anyBoolean()
        );
        verify(dbfMailer, times(getMailsOK().size())).send(any());
        verify(dbfMailer, times(getMailsOK().size())).fillMessage(any(), any(), any(), any());
        verify(dbfFilePacker, times(
            getMailsOK()
                .stream()
                .mapToInt(m -> m.getFiles().size())
                .sum()
        )).pack();

        checkNotAllSend(mails);
    }

    @Test
    void executeFailToPrepareMessage() throws MessagingException {
        List<DbfMail> mails = getMailsOK();
        when(dbfMailRepository.findAllByRepliedIsNullAndDeletedIsNull()).thenReturn(mails);
        when(mimeMessage.reply(anyBoolean())).thenThrow(new MessagingException());
        when(dbfMailer.findById(anyString())).thenReturn(Optional.of(mimeMessage));

        dbfSendResponseService.execute();

        verify(dbfMailRepository).findAllByRepliedIsNullAndDeletedIsNull();
        verify(dbfMailer, times(getMailsOK().size())).findById(anyString());
        verify(dbfMailer, never()).fillMessage(any(), any(), any(), any());
        checkNotSend(mails);
    }

    @Test
    void executeFailToFillMessage() throws MessagingException, IOException {
        List<DbfMail> mails = getMailsOK();
        when(dbfMailRepository.findAllByRepliedIsNullAndDeletedIsNull()).thenReturn(mails);
        when(dbfMailer.findById(anyString())).thenReturn(Optional.of(mimeMessage));
        when(dbfMailer.fillMessage(any(), any(), any(), any())).thenReturn(false);
        when(dbfFilePacker.pack()).thenReturn(true);
        when(dbfStorage.get(anyString())).thenReturn(fileInputStream);

        dbfSendResponseService.execute();

        verify(dbfMailRepository).findAllByRepliedIsNullAndDeletedIsNull();
        verify(dbfMailer, times(getMailsOK().size())).findById(anyString());
        verify(dbfMailer, times(getMailsOK().size())).fillMessage(any(), any(), any(), any());
        verify(dbfMailer, never()).send(any());
        checkNotSend(mails);
    }

    @Test
    void executeFailToPackAttaches() throws MessagingException, IOException {
        List<DbfMail> mails = getMailsOK();
        when(dbfMailRepository.findAllByRepliedIsNullAndDeletedIsNull()).thenReturn(mails);
        when(dbfMailer.findById(anyString())).thenReturn(Optional.of(mimeMessage));
        when(dbfMailer.fillMessage(any(), any(), any(), any())).thenReturn(false);
        when(dbfFilePacker.pack()).thenReturn(false);
        when(dbfStorage.get(anyString())).thenReturn(fileInputStream);

        dbfSendResponseService.execute();

        verify(dbfMailRepository).findAllByRepliedIsNullAndDeletedIsNull();
        verify(dbfMailer, times(getMailsOK().size())).findById(anyString());
        verify(dbfMailer, never()).fillMessage(any(), any(), any(), any());
        verify(dbfMailer, never()).send(any());
        checkNotSend(mails);
    }

    @Test
    void executeFailToGetFromStorage() throws MessagingException, IOException {
        List<DbfMail> mails = getMailsOK();
        when(dbfMailRepository.findAllByRepliedIsNullAndDeletedIsNull()).thenReturn(mails);
        when(dbfMailer.findById(anyString())).thenReturn(Optional.of(mimeMessage));
        when(dbfMailer.fillMessage(any(), any(), any(), any())).thenReturn(true);
        when(dbfFilePacker.pack()).thenReturn(true);
        when(dbfStorage.get(anyString())).thenThrow(new IOException());

        dbfSendResponseService.execute();

        verify(dbfMailRepository).findAllByRepliedIsNullAndDeletedIsNull();
        verify(dbfMailer, times(getMailsOK().size())).findById(anyString());
        verify(dbfMailer, never()).fillMessage(any(), any(), any(), any());
        verify(dbfMailer, never()).send(any());
        checkNotSend(mails);
    }

    @Test
    void executeFailToSendMessage() throws MessagingException, IOException {
        List<DbfMail> mails = getMailsOK();
        when(dbfMailRepository.findAllByRepliedIsNullAndDeletedIsNull()).thenReturn(mails);
        when(dbfMailer.findById(anyString())).thenReturn(Optional.of(mimeMessage));
        when(dbfMailer.fillMessage(any(), any(), any(), any())).thenReturn(true);
        when(dbfMailer.send(any())).thenThrow(new MessagingException());
        when(dbfFilePacker.pack()).thenReturn(true);
        when(dbfStorage.get(anyString())).thenReturn(fileInputStream);

        dbfSendResponseService.execute();

        verify(dbfMailRepository).findAllByRepliedIsNullAndDeletedIsNull();
        verify(dbfMailer, times(getMailsOK().size())).findById(anyString());
        verify(dbfMailer, times(getMailsOK().size())).fillMessage(any(), any(), any(), any());
        verify(dbfMailer, times(getMailsOK().size())).send(any());
        checkNotSend(mails);
    }

    private void checkSend(List<DbfMail> mails) {
        mails.forEach(
            m -> {
                softly.assertThat(m.getReplied()).isNotNull();
                m.getFiles().forEach(
                    f -> softly.assertThat(
                        f.getStatus()).isEqualTo(DbfStatus.SENDED
                    )
                );
            }
        );
    }

    private void checkNotSend(List<DbfMail> mails) {
        mails.forEach(
            m -> {
                softly.assertThat(m.getReplied()).isNull();
                m.getFiles().forEach(
                    f -> softly.assertThat(
                        f.getStatus()).isNotEqualTo(DbfStatus.SENDED
                    )
                );
            }
        );
    }

    private void checkNotAllSend(List<DbfMail> mails) {
        softly.assertThat(
            mails.stream().map(DbfMail::getReplied).filter(Objects::nonNull)
        )
            .describedAs(
                "Ожидаем, что кол-во писем с пометкой \"отправлное-в\" равно кол-ву \"хороших\" писем"
            )
            .hasSize(getMailsOK().size());

        softly.assertThat(
            mails
                .stream()
                .map(DbfMail::getFiles)
                .flatMap(List::stream)
                .map(DbfFile::getStatus)
                .filter(Objects::nonNull)
                .filter(s -> s.equals(DbfStatus.SENDED))
        )
            .describedAs(
                "Ожидаем, что колво статусов \"DbfStatus.SENDED\" равно кол-ву файлов в \"хороших\" письмах"
            )
            .hasSize(
                getMailsOK()
                    .stream()
                    .map(DbfMail::getFiles)
                    .mapToInt(List::size)
                    .sum()
            );
    }

    private List<DbfMail> getMailsOK() {
        List<DbfMail> mails = new ArrayList<>();
        DbfMail mail = new DbfMail();
        mail.setLetterId("letterId1");
        mail.setSender("test@test.local");
        mail.setFiles(getFilesOk());
        mails.add(mail);

        mail = new DbfMail();
        mail.setLetterId("letterId2");
        mail.setSender("test@test.local");
        mail.setFiles(getFilesOk());
        mails.add(mail);
        return mails;
    }

    private List<DbfMail> getMailsFail() {
        List<DbfMail> mails = new ArrayList<>();
        DbfMail mail = new DbfMail();
        mail.setSender("test@test.local");
        mail.setFiles(getFilesOk());
        mails.add(mail);

        mail = new DbfMail();
        mail.setLetterId("letterId2Bad");
        mail.setFiles(getFilesOk());
        mails.add(mail);

        mail = new DbfMail();
        mail.setLetterId("letterId3Bad");
        mail.setSender("test2@test.local");
        mails.add(mail);

        mail = new DbfMail();
        mail.setLetterId("letterId4Bad");
        mail.setSender("test3@test.local");
        mail.setFiles(getFilesNotAllOk());
        mails.add(mail);
        return mails;
    }

    private List<DbfMail> getMailsNotAllOk() {
        List<DbfMail> mails = new ArrayList<>();
        mails.addAll(getMailsOK());
        mails.addAll(getMailsFail());
        return mails;
    }

    private List<DbfFile> getFilesOk() {
        DbfFile file1 = new DbfFile();
        file1.setStatus(DbfStatus.READY_TO_SEND);
        file1.setFilepathNew("file1");
        file1.setFilename("testFile1");

        DbfFile file2 = new DbfFile();
        file2.setStatus(DbfStatus.READY_TO_SEND);
        file2.setFilepathNew("file2");
        file2.setFilename("testFile2");

        return Arrays.asList(file1, file2);
    }

    private List<DbfFile> getFilesFail() {
        DbfFile file1 = new DbfFile();
        file1.setStatus(DbfStatus.ERROR_EMPTY_FILE);
        file1.setFilepathNew("file1");
        file1.setFilename("testFile");

        DbfFile file2 = new DbfFile();
        file2.setStatus(DbfStatus.READY_TO_SEND);

        return Arrays.asList(file1, file2);
    }

    private List<DbfFile> getFilesNotAllOk() {
        List<DbfFile> list = new java.util.ArrayList<>();
        list.addAll(getFilesOk());
        list.addAll(getFilesFail());
        return list;
    }
}
