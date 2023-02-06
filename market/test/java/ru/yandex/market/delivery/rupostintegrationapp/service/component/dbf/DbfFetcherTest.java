package ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.MimeMessageHelper;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfMail;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfFetcherTest extends BaseTest {

    private static final Long LIMIT = 100L;
    private static List<File> filesInRAR = Arrays.asList(
        new File("test.dbf"),
        new File("test.jpg")
    );
    @Mock
    private DbfMailer mailer;
    private DbfFetcher fetcher;
    private List<Object> emails = Arrays.asList(
        createEmail(),
        createEmail(),
        createEmail()
    );

    private static MimeMessage createEmail() {
        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage email = new MimeMessage(session);
        try {
            InternetAddress tAddress = new InternetAddress("test@local.ru");
            InternetAddress cAddress = new InternetAddress("test@local.ru");
            InternetAddress fAddress = new InternetAddress("test@local.ru");

            email.setFrom(fAddress);
            email.addRecipient(javax.mail.Message.RecipientType.CC, cAddress);
            email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
            email.setSubject("test");
            email.setText(UUID.randomUUID().toString() + "test");
            email.setHeader("Message-ID", UUID.randomUUID().toString());
            MimeMessageHelper helper = new MimeMessageHelper(email, true);
            helper.addAttachment(
                UUID.randomUUID().toString(),
                Files.createTempFile("resource-", ".dbf").toFile()
            );
        } catch (IOException | MessagingException e) {
            e.printStackTrace();
        }
        return email;
    }

    @BeforeEach
    void setUp() throws Exception {
        when(mailer.findNew(anyLong()))
            .thenReturn(emails);
        fetcher = spy(new DbfFetcher(mailer));
    }

    @Test
    void fetch() throws Exception {
        doReturn(filesInRAR).when(fetcher).getDbfFilesFromAttach(any());
        Map<DbfMail, List<File>> mails = fetcher.fetch();
        softly.assertThat(mails).hasSize(emails.size());
        mails.forEach((key, value) -> {
            softly.assertThat(key).isInstanceOf(DbfMail.class);
            softly.assertThat(key.getFiles().size()).isEqualTo(0);
            softly.assertThat(value).isInstanceOf(List.class);
            softly.assertThat(value.size()).isEqualTo(1);
        });
        verify(mailer, times(emails.size())).markSeen(any());
        verify(fetcher, times(emails.size())).getDbfFilesFromAttach(any());
    }

    @Test
    void fetchFailedEmptyMessages() throws Exception {
        when(mailer.findNew(anyLong()))
            .thenReturn(
                Collections.singletonList(
                    mock(MimeMessage.class)
                )
            );
        Map<DbfMail, List<File>> mails = fetcher.fetch();
        softly.assertThat(mails).hasSize(0);
    }

    @Test
    void fetchWithLimit() throws Exception {
        fetcher = new DbfFetcher(mailer, LIMIT);
        when(mailer.findNew(eq(LIMIT))).thenReturn(Collections.emptyList());
        fetcher.fetch();
        verify(mailer).findNew(eq(LIMIT));
    }

    @Test
    void fetchWithZeroLimit() throws Exception {
        fetcher = new DbfFetcher(mailer);
        when(mailer.findNew(eq(0L))).thenReturn(Collections.emptyList());
        fetcher.fetch();
        verify(mailer).findNew(eq(0L));
    }

    @Test
    void fetchFailedMarkSeen() throws Exception {
        doThrow(new MessagingException()).when(mailer).markSeen(any());
        Map<DbfMail, List<File>> mails = fetcher.fetch();
        verify(mailer, times(emails.size())).markSeen(any());
        softly.assertThat(mails).hasSize(emails.size());
    }

    @Test
    void fetchFailedUnrar() throws Exception {
        Map<DbfMail, List<File>> mails = fetcher.fetch();
        verify(mailer, times(emails.size())).markSeen(any());
        softly.assertThat(mails).hasSize(emails.size());
    }
}
