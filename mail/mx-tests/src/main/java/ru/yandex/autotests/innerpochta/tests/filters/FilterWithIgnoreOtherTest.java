package ru.yandex.autotests.innerpochta.tests.filters;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import javax.mail.MessagingException;
import java.io.FileNotFoundException;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNsls;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 *  * todo доделать
 */
//@Feature("Фильтры")
//@Stories("FASTSRV")
//@Aqua.Test(title = "Фильтры с метками", description = "Проверка работы фильтров на проставление метки")
//@Title("FilterLabelsTest. Фильтры с метками")
//@Description("Проверка работы фильтров на проставление метки")
//@RunWith(Parameterized.class)
public class FilterWithIgnoreOtherTest {
    private Logger log = LogManager.getLogger(this.getClass());
    private static final User RECEIVER = new User("ignore-another-filters-test@ya.ru", "testqa12345678");
    private TestMessage msg;

    //  @Parameterized.Parameter(0)
    public String labelName = "label2";

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    //@Parameterized.Parameters
    // public static Collection<Object[]> data() {
    //      return asList(new Object[]{"red"}, new Object[]{"black"});
    // }

    @Before
    public void prepareTestMessage() throws FileNotFoundException, MessagingException {
        this.msg = new TestMessage();
        msg.setSubject(format("FILTER_1 %s", randomAlphanumeric(20)));
        msg.setFrom("devnull@yandex.ru");
        msg.setText(format("FILTER_2 %s", randomAlphanumeric(20)));
        msg.saveChanges();
    }

    @Test
    public void shouldSeeLabelsOnLettersForOftenUser() throws Exception {
        msg.setRecipient(RECEIVER.getLogin());
        msg.saveChanges();
        log.info("Отправляем письмо, к которому должна будет добавиться метка " + labelName + ".");
        sendByNsls(msg);
        log.info("Проверяем наличие письма в ящике с заданной темой. Смотрим,что к нему добавилась метка.");
        inMailbox(RECEIVER).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), labelName);
        inMailbox(RECEIVER).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), labelName);
    }
}

