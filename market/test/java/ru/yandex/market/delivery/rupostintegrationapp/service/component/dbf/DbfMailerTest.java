package ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DbfMailerTest extends BaseTest {

    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";
    private static final String HOST = "test";
    private static final String FROMEMAIL = "test@test.ru";
    private static final List<String> EMAILS = Arrays.asList("test@test.ru", "test2@test.ru", "");
    private DbfMailer mailer;
    @Mock
    private Folder folder;
    @Mock
    private Store store;
    @Mock
    private Transport transport;

    @BeforeEach
    void setUp() throws Exception {
        Session session = mock(Session.class);

        when(store.getFolder(isNull(String.class))).thenReturn(folder);

        when(transport.isConnected()).thenReturn(false);
        doNothing().when(transport).connect(eq(USERNAME), eq(PASSWORD));
        doNothing().when(transport).close();

        when(session.getStore()).thenReturn(store);
        when(session.getTransport()).thenReturn(transport);

        mailer = spy(new DbfMailer(session, USERNAME, PASSWORD, HOST, FROMEMAIL));

        Class<?> mailerClass = mailer.getClass();
        Field defaultRecipients = mailerClass.getDeclaredField("defaultRecipients");
        defaultRecipients.setAccessible(true);
        defaultRecipients.set(mailer, EMAILS);
    }

    @Test
    void findNew() throws MessagingException {
        when(folder.search(any())).thenReturn(getMessages());
        List<Object> messages = mailer.findNew(1L);

        softly.assertThat(messages).hasSize(1);

        doNothing().when(folder).open(eq(Folder.READ_WRITE));

        verify(store).isConnected();
        verify(store).connect(eq(HOST), eq(USERNAME), eq(PASSWORD));
    }

    @Test
    void findById() throws MessagingException {
        when(folder.search(any())).thenReturn(getMessages());
        Optional<Message> messages = mailer.findById("test");

        softly.assertThat(messages.isPresent()).isTrue();

        doNothing().when(folder).open(eq(Folder.READ_WRITE));

        verify(store).isConnected();
        verify(store).connect(eq(HOST), eq(USERNAME), eq(PASSWORD));
    }

    @Test
    void send() throws MessagingException {
        MimeMessage message = getMessages()[0];
        Address[] addresses = getDefaultAddresses();
        when(message.getAllRecipients()).thenReturn(addresses);

        doNothing().when(transport).sendMessage(any(MimeMessage.class), eq(addresses));
        softly.assertThat(mailer.send(message)).isTrue();

        verify(transport).isConnected();
        verify(transport).connect(eq(USERNAME), eq(PASSWORD));
    }

    @Test
    void sendFail() throws MessagingException {
        MimeMessage message = getMessages()[0];
        Address[] addresses = getDefaultAddresses();
        when(message.getAllRecipients()).thenReturn(addresses);

        doThrow(new MessagingException()).when(transport).sendMessage(any(), any());
        softly.assertThat(mailer.send(message)).isFalse();

        verify(transport).isConnected();
        verify(transport).connect(eq(USERNAME), eq(PASSWORD));
    }

    @Test
    void fillMessage() throws IOException {
        MimeMessage message = getMessages()[0];

        ByteArrayResource attach = new ByteArrayResource(
            IOUtils.toByteArray(
                Files.newInputStream(Files.createTempFile("test", ".dbf"))
            )
        );

        List<DbfMailer.DbfAttach> attaches = Collections.singletonList(new DbfMailer.DbfAttach("test", attach));

        softly.assertThat(
            mailer.fillMessage(
                message,
                "test@test.ru",
                "body text",
                getAttaches(attach)
            )
        )
            .isTrue();
    }

    @Test
    void fillMessageFail() {
        MimeMessage message = getMessages()[0];

        ByteArrayResource attach = mock(ByteArrayResource.class);
        when(attach.isOpen()).thenReturn(true);

        softly.assertThat(
            mailer.fillMessage(
                message,
                "test@test.ru",
                "body text",
                getAttaches(attach)
            )
        )
            .isFalse();
    }

    @Test
    void fillMessageMimeMessageHelperFail() throws Exception {
        MimeMessageHelper helper = mock(MimeMessageHelper.class);

        doThrow(new MessagingException()).when(helper).setTo(anyString());

        MimeMessage message = getMessages()[0];

        doReturn(helper).when(mailer).createHelper(message);

        ByteArrayResource attach = new ByteArrayResource(
            IOUtils.toByteArray(
                Files.newInputStream(Files.createTempFile("test", ".dbf"))
            )
        );

        softly.assertThat(
            mailer.fillMessage(
                message,
                "test@test.ru",
                "body text",
                getAttaches(attach)
            )
        )
            .isFalse();

        verify(helper).setTo(anyString());
        verify(mailer).createHelper(message);
    }

    private List<DbfMailer.DbfAttach> getAttaches(ByteArrayResource attach) {
        return Collections.singletonList(new DbfMailer.DbfAttach("test", attach));
    }


    @Test
    void markSeen() throws MessagingException {
        mailer.markSeen(getMessages()[0]);

        doNothing().when(folder).open(eq(Folder.READ_WRITE));
        doNothing().when(folder).setFlags(
            any(Message[].class),
            eq(new Flags(Flags.Flag.SEEN)),
            eq(true)
        );

        verify(store).isConnected();
        verify(store).connect(eq(HOST), eq(USERNAME), eq(PASSWORD));
    }

    private Address[] getDefaultAddresses() {
        return EMAILS.stream().map(r -> {
            try {
                return new InternetAddress(r);
            } catch (AddressException e) {
                e.printStackTrace();
            }
            return new InternetAddress();
        })
            .filter(a -> Optional.ofNullable(a.getAddress()).isPresent())
            .toArray(InternetAddress[]::new);
    }


    private MimeMessage[] getMessages() {
        List<Message> messages = new ArrayList<>();
        messages.add(mock(MimeMessage.class));
        messages.add(mock(MimeMessage.class));
        messages.add(mock(MimeMessage.class));

        return messages.toArray(new MimeMessage[messages.size()]);
    }
}
