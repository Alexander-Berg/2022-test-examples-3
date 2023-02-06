package ru.yandex.common.framework.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcOperations;

import ru.yandex.common.util.collections.Pair;

/**
 * User: mixey
 * Date: 05.09.2008
 * Time: 17:40:37
 */
public class MockMessageService implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MockMessageService.class);

    private DbMessageService dbMessageService = new DbMessageService();

    @Required
    public void setJdbcTemplate(final JdbcOperations jdbcTemplate) {
        this.dbMessageService.setJdbcTemplate(jdbcTemplate);
    }

    private final List<Pair<Integer, Object>> sentMessages = new ArrayList<Pair<Integer, Object>>();

    public boolean sendMessage(int messageId, Object[] values) {
        sentMessages.add(new Pair<Integer, Object>(messageId, values));
        output(dbMessageService.createMessageTemplate(messageId, values));
        return true;
    }

    public boolean sendMessage(int messageId, Map<String, Object> params) {
        sentMessages.add(new Pair<Integer, Object>(messageId, params));
        output(dbMessageService.createMessageTemplate(messageId, params));
        return true;
    }

    private void output(MessageTemplate template) {
        log.debug("To: " + template.getTo());
        log.debug("Sublect : " + template.getSubject());
        log.debug("Text:\n" + template.getText());
    }

    public void reset() {
        sentMessages.clear();
    }

    public MessageTemplate createMessageTemplate(int messageId, Object[] values) {
        return dbMessageService.createMessageTemplate(messageId, values);
    }

    public MessageTemplate createMessageTemplate(int messageId, Map<String, Object> params) {
        return dbMessageService.createMessageTemplate(messageId, params);
    }

    public int getMessagesCount() {
        return sentMessages.size();
    }

    public Pair<Integer, Object> getMessage(int idx) {
        return sentMessages.get(idx);
    }

    public boolean registerMessage(final int messageId, final Map<String, Object> params) {
        return false;
    }

    public int sendNewMessages() {
        return 0;
    }

    public boolean sendMessage(int messageId, MessageBody body) {
        sentMessages.add(new Pair<Integer, Object>(messageId, body));
        return true;
    }

    public MessageTemplate createMessageTemplate(int messageId, MessageBody body) {
        return null;
    }

    public boolean sendMessageTemplate(MessageTemplate messageTemplate) {
        return false;
    }
}
