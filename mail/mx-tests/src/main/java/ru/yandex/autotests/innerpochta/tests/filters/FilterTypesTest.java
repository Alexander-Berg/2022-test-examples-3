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
import ru.yandex.autotests.innerpochta.utils.HintData;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.tests.headers.ismixed.SOTypesData.SO_TYPE_PREFIX;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * User: alex89
 * Date: 15.08.2017
 * У пользователя настроены фильтры
 * <p>
 * MULTI
 * Если	заголовок «type» совпадает c «s_news»
 * и	заголовок «type» совпадает c «s_aviaticket»
 * — пометить письмо меткой «label-multi»
 * <p>
 * rrCrinHWeFFqUGcKMCGh
 * Если	заголовок «type» совпадает c «s_aviaticket»
 * — пометить письмо меткой «label-avia»
 * <p>
 * BiQoUixrUbYmdIJfLHAc
 * Если	заголовок «type» совпадает c «s_news»
 * — пометить письмо меткой «label-snews»
 * <p>
 * XPqoZMPZmdTfGR1lnGr7
 * Если	заголовок «type» не совпадает c «news»
 * — пометить письмо меткой «label-no-news»
 */

@Feature("Фильтры")
@Stories("FASTSRV")
@Aqua.Test(title = "Фильтры на СО-типы",
        description = "Проверка работы фильтров на СО-типы")
@Title("FilterTypesTest. Фильтры на СО-типы")
@Description("Проверка работы фильтров на СО-типы")
@RunWith(Parameterized.class)
public class FilterTypesTest {
    private static final User RECEIVER = new User("types-filter-user2@ya.ru", "testqa");
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public HintData.XYandexHintValue hintValue;
    @Parameterized.Parameter(1)
    public List<String> labelNames;

    //@Rule
    //public SshConnectionRule sshConnectionRule = new SshConnectionRule(mxTestProps().getNslsHost());
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws MessagingException, FileNotFoundException {
        return asList(
                new Object[]{createHintValue().addLabel(SO_TYPE_PREFIX + "news"),
                        asList()},
                new Object[]{createHintValue().addLabel(SO_TYPE_PREFIX + "s_news").addLabel(SO_TYPE_PREFIX + "news"),
                        asList("label-snews")},
                new Object[]{createHintValue().addLabel(SO_TYPE_PREFIX + "s_news"),
                        asList("label-snews", "label-no-news")},
                new Object[]{createHintValue().addLabel(SO_TYPE_PREFIX + "s_aviaeticket"),
                        asList("label-avia", "label-no-news")},
                new Object[]{createHintValue().addLabel(SO_TYPE_PREFIX + "s_news")
                        .addLabel(SO_TYPE_PREFIX + "s_aviaeticket"),
                        asList("label-multi", "label-no-news", "label-snews", "label-avia")},
                new Object[]{createHintValue().addLabel(SO_TYPE_PREFIX + "s_news")
                        .addLabel(SO_TYPE_PREFIX + "news")
                        .addLabel(SO_TYPE_PREFIX + "s_aviaeticket"),
                        asList("label-multi", "label-avia", "label-snews")});
    }

    @Before
    public void prepareTestMessage() throws IOException, MessagingException {
        inMailbox(RECEIVER).clearDefaultFolder();
        msg = new TestMessage();
        msg.addHeader(X_YANDEX_HINT, hintValue.encode());
        msg.setSubject(format("Expected_LABELS: %s_____%s", labelNames, randomAlphanumeric(20)));
        msg.setFrom("devnull@yandex.ru");
        msg.setRecipient(RECEIVER.getLogin());
        msg.setText(format("Message for %s labels  %s", labelNames, randomAlphanumeric(20)));
        msg.saveChanges();
        log.info("Отправляем письмо, к которому должы будут добавлены метки " + labelNames + ".");
        String serverResponse = sendByNsls(msg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        //log.info(getInfoFromMaillog(sshConnectionRule.getConn(), messageId));
        //log.info(getInfoFromNsls(sshConnectionRule.getConn(), messageId));
    }

    @Test
    public void shouldSeeLabelsOnLetter() throws Exception {
        log.info("Проверяем наличие письма в ящике с заданной темой. Смотрим,что к нему добавилась метка.");
        inMailbox(RECEIVER).shouldSeeLetterWithSubjectAndLabels(msg.getSubject(), labelNames);
    }
}

