package ru.yandex.market.jmf.module.mail.test.impl;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.mail.MailConnection;
import ru.yandex.market.jmf.module.mail.impl.MimeMessageWrapper;

@Component
public class MailTestUtils {

    @Inject
    BcpService bcpService;

    public MimeMessageWrapper openMessage(String path) throws MessagingException {
        byte[] resource = ResourceHelpers.getResource(path);
        MimeMessage msg = new MimeMessage(null, new ByteArrayInputStream(resource));
        return new MimeMessageWrapper(msg);
    }

    public MailConnection createMailConnection(String code) {
        return createMailConnection(code, Map.of());
    }

    public MailConnection createMailConnection(String code, Map<String, Object> additionalProperties) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(MailConnection.ENABLED, true);
        properties.put(MailConnection.CODE, code);
        properties.put(MailConnection.TITLE, "Подключение для использования в тестах");
        properties.put(MailConnection.HOST, "imap.yandex.ru");
        properties.put(MailConnection.PORT, "993");
        properties.put(MailConnection.SSL, true);
        properties.put(MailConnection.FOLDER, "INBOX");
        properties.put(MailConnection.DELETE, true);
        properties.put(MailConnection.USERNAME, "NNTY3iK");
        properties.put(MailConnection.ENCRYPTED_PASSWORD, "bgUwv4xBCJI0IDEpn9yc3");

        properties.putAll(additionalProperties);
        return bcpService.create(MailConnection.FQN, properties);
    }
}
