package ru.yandex.market.abo.tms.mm;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.db.MultiIdGenerator;
import ru.yandex.market.abo.mm.db.DbMailService;
import ru.yandex.market.abo.mm.model.Account;
import ru.yandex.market.abo.mm.model.Message;
import ru.yandex.market.abo.mm.model.MessageType;
import ru.yandex.market.abo.util.monitoring.SharedMonitoringUnit;

import static java.util.Collections.enumeration;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.tms.mm.MailLoader.ERRORS_FOLDER;
import static ru.yandex.market.abo.tms.mm.MailLoader.MAIL_FETCH_SIZE;
import static ru.yandex.market.abo.tms.mm.MailLoader.MAX_PROCESSED_PER_SESSION;
import static ru.yandex.market.abo.tms.mm.MailLoader.SPAM_FOLDERS;

/**
 * @author artemmz
 * @date 27/11/18.
 */
class MailLoaderTest {
    private static final String FOLDER_NAME = "FOLD";

    private static final String TO_EMAIL = "foo@bar.com";
    private static final String TO_NAME = "bugs bunny";
    private static final String FROM_EMAIL = "bazz@fuzz.org";
    private static final String FROM_NAME = "mickey mouse";

    private static final String SUBJ = "subj";
    private static final Date RECEIVED_DATE = new Date(0);
    private static final String HEADER_NAME = "HEAD NAME";
    private static final String HEADER_VAL = "HEAD VAL";
    private static final String MSG_TEXT = "run rabbit";
    private static final long DB_ID = 1L;
    private static final Long ACCOUNT_ID = 100L;

    @InjectMocks
    MailLoader mailLoader;

    @Mock
    DbMailService dbMailService;
    @Mock
    MultiIdGenerator idGenerator;

    @Mock
    Store store;
    @Mock
    Folder folder;
    @Mock
    Folder subFolder;
    @Mock
    Folder spamFolder;
    @Mock
    Folder archive;
    @Mock
    javax.mail.Message message;
    @Mock
    SharedMonitoringUnit emailCollectingMonitoring;
    @Mock
    Account account;
    @Captor
    private ArgumentCaptor<List<Message>> savedCaptor;

    private javax.mail.Message[] messages;

    @BeforeEach
    void setUp() throws MessagingException, IOException {
        MockitoAnnotations.openMocks(this);
        messages = new javax.mail.Message[]{message};

        when(store.getFolder(FOLDER_NAME)).thenReturn(folder);
        when(store.getFolder(AdditionalMatchers.not(eq(FOLDER_NAME)))).thenReturn(archive);
        mockFolder(folder);

        when(message.getAllRecipients()).thenReturn(new InternetAddress[]{new InternetAddress(TO_EMAIL, TO_NAME)});
        when(message.getSubject()).thenReturn(SUBJ);
        when(message.getFolder()).thenReturn(folder);
        when(message.getReceivedDate()).thenReturn(RECEIVED_DATE);
        when(message.getFrom()).thenReturn(new InternetAddress[]{new InternetAddress(FROM_EMAIL, FROM_NAME)});
        when(message.getAllHeaders()).then(inv -> enumeration(singleton(new Header(HEADER_NAME, HEADER_VAL))));

        when(message.isMimeType("text/*")).thenReturn(true);
        when(message.isMimeType("multipart/*")).thenReturn(false);
        when(message.getContent()).thenReturn(MSG_TEXT);
        when(message.isExpunged()).thenReturn(false);

        when(idGenerator.getIds(1)).thenReturn(singletonList(DB_ID));
        when(account.getId()).thenReturn(ACCOUNT_ID);
    }

    private void mockFolder(Folder folderToMock) throws MessagingException {
        when(folderToMock.exists()).thenReturn(true);
        when(folderToMock.getMessageCount()).thenReturn(messages.length, 0);
        when(folderToMock.getMessages(anyInt(), anyInt())).thenReturn(messages, new javax.mail.Message[0]);
        when(folderToMock.getFullName()).thenReturn(FOLDER_NAME);
        when(folderToMock.list()).thenReturn(new Folder[0]);
    }

    @Test
    void testLoadNewMessages() throws MessagingException {
        testLoadNewMessages(folder);
    }

    private void testLoadNewMessages(Folder... folders) throws MessagingException {
        mailLoader.saveRecursive(FOLDER_NAME, store, account);

        for (Folder checkFolder : folders) {
            verify(checkFolder).getMessages(1, messages.length);
            verify(checkFolder).copyMessages(messages, archive);
            verify(checkFolder, never()).create(anyInt());
        }

        verify(message, times(folders.length)).setFlag(Flags.Flag.DELETED, true);
        verify(dbMailService, times(folders.length)).store(savedCaptor.capture());

        List<Message> savedMessages = savedCaptor.getValue();
        assertEquals(1, savedMessages.size());

        Message saved = savedMessages.iterator().next();
        assertEquals(DB_ID, saved.getId());

        assertEquals(TO_EMAIL, saved.getToEmail());
        assertEquals(TO_NAME, saved.getToName());

        assertEquals(FROM_EMAIL, saved.getFromEmail());
        assertEquals(FROM_NAME, saved.getFromName());

        assertEquals(SUBJ, saved.getSubject());
        assertEquals(MSG_TEXT, saved.getBody());
        assertFalse(saved.isSpam());
        assertEquals(RECEIVED_DATE, saved.getTime());
        assertEquals(ACCOUNT_ID, saved.getAccountId());
        assertTrue(saved.getAttachments().isEmpty());
        assertEquals(HEADER_NAME + ": " + HEADER_VAL, saved.getHeader());
        assertEquals(MessageType.AUTO, saved.getType());
    }

    @Test
    void noMessages() throws MessagingException {
        when(folder.getMessageCount()).thenReturn(0);
        mailLoader.saveRecursive(FOLDER_NAME, store, account);

        verifyNoMoreInteractions(idGenerator, dbMailService, message);
        verify(folder, never()).getMessages(anyInt(), anyInt());
        verify(folder, never()).copyMessages(any(), any());
    }

    @Test
    void infiniteInbox() throws MessagingException {
        int msgCount = 100000;
        when(folder.getMessageCount()).thenReturn(msgCount);
        when(folder.getMessages(anyInt(), anyInt())).then(
                inv -> Stream.generate(() -> message).limit((int) inv.getArguments()[1]).toArray(javax.mail.Message[]::new));

        mailLoader.saveRecursive(FOLDER_NAME, store, account);

        int processed = MAX_PROCESSED_PER_SESSION / MAIL_FETCH_SIZE;
        verify(folder, times(processed)).getMessages(anyInt(), anyInt());
        verify(dbMailService, times(processed)).store(any());
        verify(folder, times(processed)).copyMessages(any(), eq(archive));
    }

    @Test
    void dbTrouble() throws MessagingException {
        var errorFolder = mock(Folder.class);
        when(store.getFolder(ERRORS_FOLDER)).thenReturn(errorFolder);
        doThrow(new RuntimeException("cannot store in db")).when(dbMailService).store(any());
        mailLoader.saveRecursive(FOLDER_NAME, store, account);
        verify(emailCollectingMonitoring).critical(any(), any());
        verify(folder).copyMessages(any(), eq(errorFolder));
    }

    @Test
    void dbTroubleInErrorsFolder() throws MessagingException {
        var errorFolder = mock(Folder.class);
        mockFolder(errorFolder);
        when(store.getFolder(ERRORS_FOLDER)).thenReturn(errorFolder);
        doThrow(new RuntimeException("cannot store in db")).when(dbMailService).store(any());
        mailLoader.processErrors(store, account);
        verify(emailCollectingMonitoring).critical(any(), any());
        verify(folder, never()).copyMessages(any(), eq(errorFolder));
    }

    @Test
    void subFolders() throws MessagingException {
        mockFolder(subFolder);
        when(folder.list()).thenReturn(new Folder[]{subFolder});
        testLoadNewMessages(folder, subFolder);
    }

    @Test
    void noSpamFolder() throws MessagingException {
        when(spamFolder.exists()).thenReturn(false);
        for (String spam : SPAM_FOLDERS) {
            when(store.getFolder(spam)).thenReturn(spamFolder);
        }
        assertThrows(IllegalStateException.class, () -> mailLoader.processSpam(store, account));
    }

    @Test
    void firstSpamFolderGuessIsWrong() throws MessagingException {
        when(spamFolder.exists()).thenReturn(false).thenReturn(true);
        for (String spam : SPAM_FOLDERS) {
            when(store.getFolder(spam)).thenReturn(spamFolder);
        }
        mailLoader.processSpam(store, account);

        verify(store).getFolder(SPAM_FOLDERS.stream().skip(1).findFirst().orElse(null));
        verify(spamFolder).getMessageCount();
    }
}
