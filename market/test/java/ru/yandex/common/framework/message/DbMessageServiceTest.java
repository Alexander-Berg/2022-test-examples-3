package ru.yandex.common.framework.message;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 * @date 11.03.2008
 */
public class DbMessageServiceTest extends EmptyTest {

    @Autowired
    private MessageService messageService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    private static final int TEST_MESSAGE_ID_2 = 1;

    @BeforeEach
    public void init() {
        pgJdbcTemplate.update("INSERT INTO message_template(\n" +
                "\tid, name, description, message, subject, from_alias, reply_alias, importance)\n" +
                "\tVALUES (1, 'monitor','Мониторинги АБО','Здравствуйте!\n" +
                "\n" +
                "У нас произошел сбой.\n" +
                "\n" +
                "Сломался кусок \"{0}\", а произошло следующее: \n" +
                "\n" +
                "{1}\n" +
                "\n" +
                "--\n" +
                "Всегда ваша Служба Контроля Качества\n" +
                "Яндекс.Маркета', 'АБО: критическая ошибка [{0}]','AboDevelopers','AboDevelopers',1)");
    }

    @Test
    @Disabled
    public void testRegisterMessage() {
        final Map<String, Object> params = new HashMap<>();
        params.put("number", 20);
        assertTrue(messageService.registerMessage(1, params));
        assertTrue(0 < messageService.sendNewMessages());
    }

    @Test
    public void testCreateMessage() {
//        messageService.sendMessage(TEST_MESSAGE_ID_2, new Object[]{"клоновый чекер", "проверка отправки письма"});
        MessageTemplate message = messageService.createMessageTemplate(TEST_MESSAGE_ID_2,
                new Object[]{"клоновый чекер", "проверка отправки письма", "КЧ"});

        assertEquals(
                "Здравствуйте!\n" +
                        "\n" +
                        "У нас произошел сбой.\n" +
                        "\n" +
                        "Сломался кусок \"клоновый чекер\", а произошло следующее: \n" +
                        "\n" +
                        "проверка отправки письма\n" +
                        "\n" +
                        "--\n" +
                        "Всегда ваша Служба Контроля Качества\n" +
                        "Яндекс.Маркета",
                message.getText());
        assertEquals("АБО: критическая ошибка [клоновый чекер]", message.getSubject());
    }

}
