package ru.yandex.market.jmf.module.mail.test.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.mail.EmailBodyLimiter;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.mail.MailMessage;
import ru.yandex.market.jmf.module.mail.impl.MimeMessageWrapper;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.html.Htmls;
import ru.yandex.market.jmf.utils.html.RedirectContext;

@Component
public class MailMessageBuilderService {
    private final EmailBodyLimiter emailBodyLimiter;
    private final BcpService bcpService;
    private final RedirectContext redirectContext;
    private final MailTestUtils mailTestUtils;
    private final Htmls htmls;

    public MailMessageBuilderService(EmailBodyLimiter emailBodyLimiter, RedirectContext redirectContext,
                                     MailTestUtils mailTestUtils,
                                     BcpService bcpService, Htmls htmls) {
        this.emailBodyLimiter = emailBodyLimiter;
        this.redirectContext = redirectContext;
        this.mailTestUtils = mailTestUtils;
        this.bcpService = bcpService;
        this.htmls = htmls;
    }

    public MailMessageBuilder getMailMessageBuilder(String connection) {
        return new MailMessageBuilder(connection);
    }

    public MailMessageBuilder getMailMessageBuilder(String connection, String defaultMessagePath) throws MessagingException {
        return new MailMessageBuilder(connection, defaultMessagePath, mailTestUtils);
    }

    public final class MailMessageBuilder {

        protected final Map<String, Object> properties;

        public MailMessageBuilder(String connection) {
            properties = Maps.of(
                    MailMessage.CONNECTION, connection,
                    MailMessage.MESSAGE_ID, Randoms.string(),
                    MailMessage.IN_REPLY_TO, Randoms.email(),
                    MailMessage.REFERENCES, Randoms.string(),
                    MailMessage.TITLE, Randoms.string(),
                    MailMessage.SENDER, Randoms.email(),
                    MailMessage.SENDER_NAME, Randoms.string(),
                    MailMessage.TO, Randoms.email(),
                    MailMessage.TO_NAME, Randoms.string(),
                    MailMessage.BODY, Randoms.string(),
                    MailMessage.HEADERS, Maps.of(),
                    MailMessage.DEDUPLICATION_KEY, Randoms.string()
            );
        }

        public MailMessageBuilder(String connection, String defaultMessagePath,
                                  MailTestUtils mailTestUtils) throws MessagingException {
            MimeMessageWrapper msg = mailTestUtils.openMessage(defaultMessagePath);
            properties = Maps.of(
                    InMailMessage.CONNECTION, connection,
                    InMailMessage.MESSAGE_ID, msg.getMessageID(),
                    InMailMessage.IN_REPLY_TO, msg.getInReplyTo(),
                    InMailMessage.REFERENCES, msg.getReferences(),
                    InMailMessage.TITLE, msg.getSubject(),
                    InMailMessage.SENDER, msg.getFrom(),
                    InMailMessage.SENDER_NAME, msg.getFromPersonal(),
                    InMailMessage.TO, msg.getTo(),
                    InMailMessage.TO_NAME, msg.getToPersonal(),
                    InMailMessage.BODY, msg.getHtmlBody(Collections.emptyList(), htmls, emailBodyLimiter),
                    InMailMessage.ORIGINAL_BODY, msg.getOriginalBody(),
                    InMailMessage.DEDUPLICATION_KEY, msg.getMessageID()
            );
        }

        public MailMessageBuilder setSubject(String subject) {
            properties.put(MailMessage.TITLE, subject);
            return this;
        }

        public MailMessageBuilder setBody(String body) {
            properties.put(MailMessage.BODY, body);
            return this;
        }

        public MailMessageBuilder setMessageId(String messageId) {
            properties.put(MailMessage.MESSAGE_ID, messageId);
            return this;
        }

        public MailMessageBuilder newDeduplicationKey() {
            return setDeduplicationKey(Randoms.string());
        }

        public MailMessageBuilder setFrom(String sender) {
            properties.put(MailMessage.SENDER, sender);
            return this;
        }

        public MailMessageBuilder setTo(String recipient) {
            properties.put(MailMessage.TO, recipient);
            return this;
        }

        public MailMessageBuilder setToList(String... recipients) {
            properties.put(MailMessage.TO_LIST, List.of(recipients));
            return this;
        }

        public MailMessageBuilder setCcList(String... recipients) {
            properties.put(MailMessage.CC_LIST, List.of(recipients));
            return this;
        }

        public MailMessageBuilder setReplyToList(String... replyToList) {
            properties.put(MailMessage.REPLY_TO_LIST, List.of(replyToList));
            return this;
        }

        public MailMessageBuilder setReferences(String references) {
            properties.put(MailMessage.REFERENCES, references);
            return this;
        }

        public MailMessageBuilder setHeader(Map<String, List<String>> headers) {
            properties.put(MailMessage.HEADERS, headers);
            return this;
        }

        public MailMessageBuilder setDeduplicationKey(String deduplicationKey) {
            properties.put(MailMessage.DEDUPLICATION_KEY, deduplicationKey);
            return this;
        }

        public InMailMessage build() {
            return bcpService.create(InMailMessage.FQN, properties);
        }
    }
}
