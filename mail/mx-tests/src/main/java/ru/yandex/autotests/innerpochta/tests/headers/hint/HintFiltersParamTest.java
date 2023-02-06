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
 */
@Stories("FASTSRV")
@Feature("Тестирование почтовых заголовков. X-Yandex-Hint")
@Aqua.Test(title = "Тестирование filters",
        description = "Проверяем влияние filters на фильтрацию письма")
@Title("HintFiltersParamTest.Тестирование filters")
@Description("Проверяем влияние filters на фильтрацию письма")
@RunWith(Parameterized.class)
public class HintFiltersParamTest {
    private static final String LABEL_BY_FILTER = "отфильтровано";
    private static final String TEXT_FOR_FILTER = "Раздражитель фильтров.";
    private static final List<String> FILTER_PARAMS = asList("-1", "1", "3");
    private static final List<String> NO_FILTER_PARAMS = asList("0", "aaaaa");
    private Logger log = LogManager.getLogger(this.getClass());
    private XYandexHintValue hintValue = createHintValue();
    private TestMessage msg;
    private String messageId;
    private static User sender;
    private static User receiver;
    private static User receiverByForwardFilter;

    @Parameterized.Parameter(0)
    public String filtersParamValue;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        List<String> allTestCases = new ArrayList<String>();
        allTestCases.addAll(FILTER_PARAMS);
        allTestCases.addAll(NO_FILTER_PARAMS);
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
        hintValue = createHintValue().addFilters(filtersParamValue);
        msg = new TestMessage();
        msg.setFrom(sender.getEmail());
        msg.setRecipient(receiver.getEmail());
        msg.setSubject(hintValue + ">>>" + randomAlphanumeric(20));
        msg.setText(TEXT_FOR_FILTER);
        msg.setHeader(X_YANDEX_HINT, hintValue.encode());
        msg.saveChanges();
    }

    @Test
    public void shouldSeeFiltersActionsIfXYandexHintFiltersParamIsSwitchedOn() throws IOException, MessagingException {
        assumeTrue("Проверяем для тех значений параметра filters, которые должны приводить к фильтрации",
                FILTER_PARAMS.contains(filtersParamValue));
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL_BY_FILTER);
        inMailbox(receiverByForwardFilter).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL_BY_FILTER);
    }

    @Test
    public void shouldNotSeeFiltersActionsIfXYandexHintFiltersParamIsSwitchedOff()
            throws IOException, MessagingException {
        assumeTrue("Проверяем для тех значений параметра filters, которые НЕ должны приводить к фильтрации",
                NO_FILTER_PARAMS.contains(filtersParamValue));
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue);
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubjectAndWithoutLabel(msg.getSubject(), LABEL_BY_FILTER);
        inMailbox(receiverByForwardFilter).shouldNotSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void shouldNotSeeForwardsActionsIfXYandexHintFiltersParamIsSwitchedOnAndForwardParamIsSwitchedOff()
            throws IOException, MessagingException {
        assumeTrue("Проверяем для тех значений параметра, которые должны приводить к фильтрации",
                FILTER_PARAMS.contains(filtersParamValue));
        msg.setHeader(X_YANDEX_HINT, hintValue.addForward("0").encode());
        msg.saveChanges();
        messageId = getMessageIdByServerResponse(sendByNsls(msg));
        log.info("Отправили письмо с X-Yandex-Hint=" + hintValue + ", ожидается частичная фильтрация.");
        log.info(getInfoFromNsls(sshAuthRule.conn(), messageId));

        inMailbox(receiver).shouldSeeLetterWithSubjectAndLabel(msg.getSubject(), LABEL_BY_FILTER);
        inMailbox(receiverByForwardFilter).shouldNotSeeLetterWithSubject(msg.getSubject());
    }
}
