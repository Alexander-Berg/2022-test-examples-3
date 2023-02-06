package ru.yandex.autotests.innerpochta.tests.headers.hint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 06.08.13
 * <p>
 * 1)hint-forward-test@ya.ru, robbitter-8783286632@ya.ru
 * Если	«Тело письма» содержит «Раздражитель фильтров.»
 * — пометить письмо меткой «отфильтровано»
 * Если	«Тело письма» содержит «Раздражитель фильтров.»
 * — переслать письмо по адресу «hint-forward-test2@ya.ru»
 * <p>
 * 2)hint-forward-test2@ya.ru, robbitter-5887671687@ya.ru
 * Если	«Тело письма» содержит «Раздражитель фильтров»
 * — пометить письмо меткой «отфильтровано»
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование forward",
        description = "Проверяем влияние forward на включение/отключение форвардных фильтров")
@Title("HintForwardParamTest.Тестирование forward")
@Description("Проверяем влияние forward на включение/отключение форвардных фильтров")
@RunWith(Parameterized.class)
public class HintForwardParamTest {
    private static final String LABEL_BY_FILTER = "отфильтровано";
    private static final String TEXT_FOR_FILTER = "Раздражитель фильтров.";
    private static final List<String> FORWARD_PARAMS = asList(/*"-1", */"1"/*, "3"*/);
    private static final List<String> NO_FORWARD_PARAMS = asList("0" /*, "aaaaa"*/);
    private Logger log = LogManager.getLogger(this.getClass());
    private XYandexHintValue hintValue;
    private TestMessage msg;
    private String messageId;
    private static User sender;
    private static User receiver;
    private static User receiverByForwardFilter;

    @Parameterized.Parameter(0)
    public String forwardParamValue;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        List<String> allTestCases = new ArrayList<String>();
        allTestCases.addAll(FORWARD_PARAMS);
        allTestCases.addAll(NO_FORWARD_PARAMS);
        for (String value : allTestCases)
            data.add(new Object[]{value});
        return data;
    }

    @BeforeClass
    public static void setReceiversAndSenderAndClearMailboxes() throws Exception {
        assumeFalse("Автотест не приспособлен к корпоративной почте (в корп. почте не создать форвардные фильтры!?)",
                mxTestProps().isCorpServer());
        List<User> receivers = accountRule.getReceiverUsers();
        sender = accountRule.getSenderUser();
        receiver = receivers.get(0);
        receiverByForwardFilter = receivers.get(1);
        inMailbox(receiver).clearAll();
        inMailbox(receiverByForwardFilter).clearAll();
    }

    @Before
    public void prepareTestMessage() throws IOException, MessagingException {
        hintValue = createHintValue().addForward(forwardParamValue);
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(hintValue + ">>>" + randomAlphanumeric(20));
        msg.setText(TEXT_FOR_FILTER);
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.saveChanges();

    }

    @Test
    public void shouldSeeAllFiltrationActionsIfXYandexHintForwardParamIsSwitchedOn()
            throws IOException, MessagingException {
        assumeTrue("Проверяем для тех значений параметра forward, которые должны приводить к фильтрации",
                FORWARD_PARAMS.contains(forwardParamValue));
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL_BY_FILTER);
        inMailbox(receiverByForwardFilter).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL_BY_FILTER);
    }

    @Test
    public void shouldNotSeeForwardButSeeOtherFiltrationActionsIfXYandexHintForwardParamIsSwitchedOff()
            throws IOException, MessagingException {
        assumeTrue("Проверяем для тех значений параметра forward, которые НЕ должны приводить к фильтрации",
                NO_FORWARD_PARAMS.contains(forwardParamValue));
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL_BY_FILTER);
        inMailbox(receiverByForwardFilter).shouldNotSeeLetterWithSubject(msg.getSubject(), "[MAILPROTO-1479]");
    }

    @Test
    public void shouldSeeThatXYandexHintForwardParamDoesNotWorkWhenFiltersParamIsSwitchedOff()
            throws IOException, MessagingException {
        msg.setHeader(X_YANDEX_HINT, hintValue.addFilters("0").encode());
        msg.setSubject(hintValue + ">>>" + randomAlphanumeric(20));
        msg.saveChanges();
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubjectAndWithoutLabel(msg.getSubject(), LABEL_BY_FILTER);
        inMailbox(receiverByForwardFilter).shouldNotSeeLetterWithSubject(msg.getSubject());
    }


    @Test
    public void shouldNotSeeForwardButSeeOtherFiltrationIfHintFiltersParamIsSwitchedOnAndForwardParamIsSwitchedOff()
            throws IOException, MessagingException {
        assumeTrue("Проверяем для тех значений параметра forward, которые НЕ должны приводить к фильтрации",
                NO_FORWARD_PARAMS.contains(forwardParamValue));
        msg.setHeader(X_YANDEX_HINT, hintValue.addFilters("1").encode());
        msg.setSubject(hintValue + ">>>" + randomAlphanumeric(20));
        msg.saveChanges();
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue + ". Ожидается, что фильтрация отключится.");
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL_BY_FILTER);
        inMailbox(receiverByForwardFilter).shouldNotSeeLetterWithSubject(msg.getSubject(), LABEL_BY_FILTER);
    }
}
