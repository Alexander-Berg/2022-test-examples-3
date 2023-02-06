package ru.yandex.autotests.innerpochta.tests.filters;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNsls;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * User: alex89
 * Date: 15.08.2017
 * У пользователя настроены фильтры
 * 1) Если в письме нет заголовка «Novosibirsk»  — пометить письмо меткой «LABEL2»
 * 2) Если в письме есть заголовок «St-Peterburg»  — пометить письмо меткой «LABEL»
 * 3) Если	заголовок «City» содержит «St-Peterburg» — пометить письмо меткой «LABEL3»
 * 4) Если	заголовок «City» не содержит «Moskawa» — пометить письмо меткой «LABEL4»
 */

@Feature("Фильтры")
@Stories("FASTSRV")
@Aqua.Test(title = "Фильтры на заголовки",
        description = "Проверка работы фильтров на содержание и наличие заголовков")
@Title("FilterHeaderTest. Фильтры на заголовки [MPROTO-3824]")
@Description("Проверка работы фильтров на содержание и наличие заголовков [MPROTO-3824]")
@RunWith(Parameterized.class)
public class FilterHeaderTest {
    private static final User RECEIVER = new User("header-exist-test@ya.ru", "testqa");
    private Logger log = LogManager.getLogger(this.getClass());

    @Parameterized.Parameter(0)
    public TestMessage msg;
    @Parameterized.Parameter(1)
    public List<String> labelNames;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws MessagingException, FileNotFoundException {
        TestMessage msg1 = new TestMessage();
        msg1.setHeader("St-Novosibirsk", "Abrrrr");
        msg1.setHeader("City", "St-Peterburg");
        msg1.saveChanges();
        TestMessage msg2 = new TestMessage();
        msg2.setHeader("Novosibirsk", "Abrrrr");
        msg2.setHeader("St-Peterburg", "St");
        msg2.saveChanges();
        TestMessage msg3 = new TestMessage();
        msg3.setHeader("Novosibirsk", "Abrrrr");
        msg3.setHeader("City", "Peterburg");
        msg3.saveChanges();
        TestMessage msg4 = new TestMessage();
        msg4.setHeader("City", "Moskawa");
        msg4.saveChanges();
        TestMessage msg5 = new TestMessage();
        return asList(new Object[]{msg1, asList("LABEL2", "LABEL3", "LABEL4")},
                new Object[]{msg2, asList("LABEL")},
                new Object[]{msg3, asList("LABEL4")},
                new Object[]{msg4, asList("LABEL2")},
                new Object[]{msg5, asList("LABEL2", "LABEL4")});
    }

    @Before
    public void prepareTestMessage() throws FileNotFoundException, MessagingException {
        inMailbox(RECEIVER).clearDefaultFolder();
        msg.setSubject(format("Expected_LABELS: %s_____%s", labelNames, randomAlphanumeric(20)));
        msg.setFrom("devnull@yandex.ru");
        msg.setText(format("Message with %s labels  %s", labelNames, randomAlphanumeric(20)));
        msg.saveChanges();
    }

    @Test
    public void shouldSeeLabelsOnLetter() throws Exception {
        msg.setRecipient(RECEIVER.getLogin());
        msg.saveChanges();
        log.info("Отправляем письмо, к которому должы будут добавлены метки " + labelNames + ".");
        sendByNsls(msg);
        log.info("Проверяем наличие письма в ящике с заданной темой. Смотрим,что к нему добавилась метка.");
        inMailbox(RECEIVER).shouldSeeLetterWithSubjectAndLabels(msg.getSubject(), labelNames);
    }

}

