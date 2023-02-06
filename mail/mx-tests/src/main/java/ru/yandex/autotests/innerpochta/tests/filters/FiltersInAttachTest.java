package ru.yandex.autotests.innerpochta.tests.filters;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * User: alex89
 * Date: 26.11.13
 * https://jira.yandex-team.ru/browse/MAILPROTO-1766
 */

@Stories("FASTSRV")
@Feature("Фильтры")
@Aqua.Test(title = "Фильтрация  письма с вложениями",
        description = "Проверяем, что фильтр не анализирует вложение.")
@Title("FiltersInAttachTest. Фильтрация  письма с вложениями")
@Description("Проверяем, что фильтр не анализирует вложение.")
public class FiltersInAttachTest {
    private static final User RECEIVER = new User("filters-in-attach@ya.ru", "12345678");
    private static final String LETTER_WITH_STRANGE_ATTACH = "filter-attach.eml";
    private static final String FILTERED_LABEL = "FILTERED";
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    //@Rule
    //public SshConnectionRule sshConnectionRule = new SshConnectionRule(mxTestProps().getNslsHost());

    @Before
    public void prepareTestMessage() throws Exception {
        inMailbox(RECEIVER).clearAll();
        msg = new TestMessage(new File(this.getClass().getClassLoader()
                .getResource(LETTER_WITH_STRANGE_ATTACH).getFile()));
        msg.setSubject("FiltersInAttachTest " + randomAlphanumeric(20));
        msg.setRecipient(RECEIVER.getLogin());
        msg.saveChanges();
    }

    @Test
    public void shouldSeeLabelFilterActionUnderAttachOfLetter() throws Exception {
        log.info("Отправляем письмо с аттачем типа text/plain к пользователю, у которого настроен фильтр:\n" +
                "\"Если в письме не содержится текст \"ААА\", то пометить его меткой FILTERED\".\n" +
                "Если фастсрв не проигнорирует фильтрацию аттача, то текст содержащий ААА в аттаче будет обнаружен, " +
                "и фильтр не выставит метку. ==> Ожидаем установки метки. [MAILPROTO-1766]");
        String messageId = getMessageIdByServerResponse(sendByNsls(msg));
        //log.info(getInfoFromNsls(sshConnectionRule.getConn(), messageId));
        inMailbox(RECEIVER).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), FILTERED_LABEL);
    }
}
