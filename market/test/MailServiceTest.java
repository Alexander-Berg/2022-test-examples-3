package ru.yandex.market.jmf.module.mail.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.mail.MailConnection;
import ru.yandex.market.jmf.module.mail.MailMessage;
import ru.yandex.market.jmf.module.mail.ReadMailService;

@Transactional
@SpringJUnitConfig(InternalModuleMailTestConfiguration.class)
public class MailServiceTest {

    @Inject
    ReadMailService readMailService;
    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;

    @BeforeEach
    public void init() {
        dbService.createQuery("delete from mailMessage").executeUpdate();
        dbService.createQuery("delete from mailConnection").executeUpdate();
    }

    @Test
    @Disabled("Не проходит в sandbox т.к. нет доступа до SMTP и IMAP сервера")
    public void doImport() throws Exception {
        // настройка системы
        String subject = Randoms.string();
        sendTestEmail(subject);

        // Ждем доставки письма адресату
        Thread.sleep(1_000);

        createConnection();

        // вызов системы
        readMailService.read();

        // проверка утверждений
        List<Entity> result = dbService.list(Query.of(InMailMessage.FQN));
        Assertions.assertEquals(1, result.size());
        Entity msg = result.get(0);
        String title = msg.getAttribute(MailMessage.TITLE);
        Assertions.assertEquals(subject, title);
    }

    /**
     * Проверяем правильность интерфейса
     */
    @Test
    public void connection() {
        MailConnection connection = createConnection();

        Assertions.assertEquals("testConnection", connection.getCode());
        Assertions.assertEquals("Подключение для использования в тестах", connection.getTitle());
        Assertions.assertEquals("imap.yandex.ru", connection.getHost());
        Assertions.assertEquals(Long.valueOf(993), connection.getPort());
        Assertions.assertEquals(Boolean.TRUE, connection.getSsl());
        Assertions.assertEquals("INBOX", connection.getFolder());
        Assertions.assertEquals(Boolean.TRUE, connection.getDelete());
        Assertions.assertEquals("NNTY3iK", connection.getUsername());
        Assertions.assertEquals("bgUwv4xBCJI0IDEpn9yc3", connection.getPassword());
    }

    private MailConnection createConnection() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(MailConnection.ENABLED, true);
        properties.put(MailConnection.CODE, "testConnection");
        properties.put(MailConnection.TITLE, "Подключение для использования в тестах");
        properties.put(MailConnection.HOST, "imap.yandex.ru");
        properties.put(MailConnection.PORT, "993");
        properties.put(MailConnection.SSL, true);
        properties.put(MailConnection.FOLDER, "INBOX");
        properties.put(MailConnection.DELETE, true);
        properties.put(MailConnection.USERNAME, "NNTY3iK");
        properties.put(MailConnection.ENCRYPTED_PASSWORD, "bgUwv4xBCJI0IDEpn9yc3");

        return bcpService.create(MailConnection.FQN, properties);
    }

    private void sendTestEmail(String subject) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", "mx.yandex.ru");

        Session session = Session.getDefaultInstance(properties);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("NNTY3iK@yandex.ru"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("NNTY3iK@yandex.ru"));
        message.setSubject(subject);
        message.setText("This is test message");

        Transport.send(message);
    }
}
