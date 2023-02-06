package ru.yandex.autotests.innerpochta.tests.pq;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.headers.ismixed.SOTypesData;
import ru.yandex.autotests.innerpochta.utils.SSHAuthRule;
import ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.utils.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.*;
import static org.cthul.matchers.object.ContainsPattern.matchesPattern;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static ru.yandex.autotests.innerpochta.tests.headers.ismixed.SOTypesData.SoTypes.*;
import static ru.yandex.autotests.innerpochta.tests.matchers.PqLabelMatcher.hasLabelWithProperties;
import static ru.yandex.autotests.innerpochta.utils.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.utils.HintData.XYandexHintValue.createHintValue;
import static ru.yandex.autotests.innerpochta.utils.HintData.X_YANDEX_HINT;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.*;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailBoxPqTable.mailBoxTableInfoFromPq;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;

/**
 * User: alex89
 * Date: 05.05.15
 * private static final User RECEIVER = new User("mxqa-pg@ya.ru", "qwerty123456");
 * private static final String RECEIVER_UID = "324457098";
 * <p>
 * public static SshLocalPortForwardingRule fwd = viaRemoteHost(URI.create("https://baida2-qa.yandex.ru"))
 * .forwardTo(URI.create("jdbc://xdb-qa01h.cmail.yandex.net:6432"))
 * .onLocalPort(localPortForMocking());
 */

@Stories("FASTSRV")
@Feature("PQ")
@Aqua.Test(title = "Поверка добавления меток к письму в таблицу mail.box и mail.labels постгреса",
        description = "Проверяем, что к письму в таблицу mail.box записались нужные lids меток," +
                " и метки эти верно предствлены (name,type,color) в таблице mail.labels")
@Title("MailBoxTableNewLabelsTest. Поверка добавления меток к письму в таблицу mail.box и mail.labels постгреса")
@Description("Проверяем, что к письму в таблицу mail.box записались нужные lids меток," +
        " и метки эти верно предствлены (name,type,color) в таблице mail.labels")
@RunWith(Parameterized.class)
public class MailBoxTableNewLabelsTest {
    private static final String RECEIVER_UID = "395174367";
    private static final Matcher LABEL_HAS_COLOR_MATCHER = matchesPattern("[0-9]{1}[0-9]+");
    private static final Matcher<String> IMAP_TYPE = equalTo("imap");
    private static final Matcher<String> DOMAIN_TYPE = equalTo("domain");
    private static final Matcher<String> TYPE_TYPE = equalTo("type");
    private static final Matcher<String> SYSTEM_TYPE = equalTo("system");
    private static final Matcher<String> USER_TYPE = equalTo("user");

    private static Logger log = LogManager.getLogger(MailBoxTableNewLabelsTest.class);
    private TestMessage testMsg;
    private String sessionLog;
    private String mid;
    private static User sender;
    private static User receiver;

    @Parameterized.Parameter(0)
    public XYandexHintValue hintValue;
    @Parameterized.Parameter(1)
    public Matcher expectedLidsMatcher;
    @Parameterized.Parameter(2)
    public String caseComment;

    @ClassRule
    public static AccountRule accountRule = new AccountRule();
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            mxTestProps().getNslsAppHost(),
            mxTestProps().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        String randomPrefix = randomAlphanumeric(7);
        String userLabel1 = randomPrefix + "LABEL";
        //теперь все метки, добавляемые с label, которые раньше были user-типа, становятся system.
        String userLabel2 = randomPrefix + "Метка2";
        String userLabel3 = randomNumeric(4);
        String imapLabel = randomPrefix + "LABELIMAP";
        String domainLabel = randomPrefix + "domainlabel";
        String domainLabelReal = "vtnrf0jira";
        //новый параметр userlabel даёт возможность добавлять пользовательские метки
        String userLabel4 = "realUser" + randomNumeric(4);
        //  data.add(new Object[]{createHintValue().addSkipLoopPrevention("1").addLabel("SystMetkaSO:hamon"),
        //          hasLabelWithProperties("28", TYPE_TYPE, isEmptyOrNullString()),
        //         "Проверка простановки системных меток"});

        data.add(new Object[]{createHintValue().addSkipLoopPrevention("1").addLabel("SystMetkaSO:discount"),
                hasLabelWithProperties("62", TYPE_TYPE, isEmptyOrNullString()),
                "Проверка простановки типов за пределами mixed [MPROTO-3423]"});
        //data.add(new Object[]{createHintValue().addSkipLoopPrevention("1").addLabel("SystMetkaSO:mytype"),
        //        hasLabelWithProperties("64", TYPE_TYPE, isEmptyOrNullString()),
        //        "Проверка простановки типов за пределами mixed [MPROTO-3423]"});

        data.add(new Object[]{createHintValue().addUserLabel(userLabel4),
                hasLabelWithProperties(userLabel4, USER_TYPE, LABEL_HAS_COLOR_MATCHER),
                "Проверка простановки пользовательских меток [MPROTO-1778]"});
        data.add(new Object[]{createHintValue().addLabel(userLabel1).addLabel(userLabel2),
                allOf(hasLabelWithProperties(userLabel1, SYSTEM_TYPE, isEmptyOrNullString()),
                        hasLabelWithProperties(userLabel2, SYSTEM_TYPE, isEmptyOrNullString())),
                "Проверка простановки пользовательских меток [MPROTO-1778]"});
        data.add(new Object[]{createHintValue().addLabel(userLabel3),
                hasLabelWithProperties(userLabel3, SYSTEM_TYPE, isEmptyOrNullString()),
                "Проверка простановки пользовательских меток [MPROTO-1778]"});
        data.add(new Object[]{createHintValue().addImapLabel(imapLabel),
                hasLabelWithProperties(imapLabel, IMAP_TYPE, isEmptyOrNullString()),
                "Проверка простановки imap меток"});
        data.add(new Object[]{createHintValue().addSkipLoopPrevention("1").addLabel("SystMetkaSO:cancel"),
                hasLabelWithProperties("28", TYPE_TYPE, isEmptyOrNullString()),
                "Проверка простановки типов [MPROTO-1778]"});
        for (SOTypesData.SoTypes label : SOTypesData.SoTypes.getShuffledTypesList().subList(0, 5)) {
            data.add(new Object[]{createHintValue().addSkipLoopPrevention("1").addLabel("SystMetkaSO:" + label),
                    hasLabelWithProperties(format("%d", label.getCode()), TYPE_TYPE, isEmptyOrNullString()),
                    "Проверка простановки типов [MPROTO-1778]"});
        }
        data.add(new Object[]{createHintValue().addSkipLoopPrevention("1").addLabel("domain_" + domainLabel),
                hasLabelWithProperties(domainLabel, DOMAIN_TYPE, isEmptyOrNullString()),
                "Проверка простановки доменных меток [MPROTO-1778]"});
        data.add(new Object[]{createHintValue().addSkipLoopPrevention("1").addLabel("domain_" + domainLabelReal),
                hasLabelWithProperties(domainLabelReal, DOMAIN_TYPE, isEmptyOrNullString()),
                "Проверка простановки доменных меток (несинтетическая)[MPROTO-1778]"});
        data.add(new Object[]{createHintValue().addSkipLoopPrevention("1").addLabel("answered"),
                hasLabelWithProperties("answered", SYSTEM_TYPE, isEmptyOrNullString()),
                "Проверка простановки системных меток"});
        data.add(new Object[]{createHintValue().addSkipLoopPrevention("1")
                .addLabel("SystMetkaSO:" + REGISTRATION).addLabel("SystMetkaSO:" + PEOPLE)
                .addLabel("SystMetkaSO:" + ETICKET).addLabel("SystMetkaSO:" + ESHOP)
                .addLabel("SystMetkaSO:" + BOUNCE).addLabel("SystMetkaSO:" + GREETING),
                allOf(hasLabelWithProperties(REGISTRATION.getCode(), TYPE_TYPE, isEmptyOrNullString()),
                        hasLabelWithProperties(PEOPLE.getCode(), TYPE_TYPE, isEmptyOrNullString()),
                        hasLabelWithProperties(ETICKET.getCode(), TYPE_TYPE, isEmptyOrNullString()),
                        hasLabelWithProperties(ESHOP.getCode(), TYPE_TYPE, isEmptyOrNullString()),
                        hasLabelWithProperties(BOUNCE.getCode(), TYPE_TYPE, isEmptyOrNullString()),
                        hasLabelWithProperties(GREETING.getCode(), TYPE_TYPE, isEmptyOrNullString())),
                "Проверка простановки более 5 типов [MPROTO-3763]"});
        return data;
    }

    @BeforeClass
    public static void setReceiverAndSender() throws Exception {
        sender = accountRule.getSenderUser();
        receiver = accountRule.getReceiverUser();
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        testMsg = new TestMessage();
        testMsg.setFrom(sender.getEmail());
        testMsg.setSubject("hi!!!!" + randomAlphabetic(10));
        testMsg.setText("GOOD_TEXT_LETTER");
        testMsg.setRecipient(receiver.getEmail());
        testMsg.setHeader(X_YANDEX_HINT, hintValue.encode());
        testMsg.saveChanges();
        log.info(format("Отправили письмо с темой %s и хинтовыми парметрами %s", testMsg.getSubject(), hintValue));
        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        sessionLog = getInfoFromNsls(sshAuthRule.conn(), messageId);
        log.info(sessionLog);
//        assertThat("Hе прошла покладка в ПГ!", sessionLog, anyOf(containsString("message stored in pq backed"),
//                containsString("message stored in db=pg backed")));
        mid = extractedParamFromLogByPattern(MID_LOG_STRING_PATTERN, sessionLog);
    }

    @Test
    public void shouldSeeLabelsOnLettersInMailBoxWithCorrectTypeAndCollor() {
        assertThat(format("Записались неверные данные о lids в таблицу mail.box для случая: '%s'", caseComment),
                mailBoxTableInfoFromPq(sshAuthRule.conn(), RECEIVER_UID, mid),
                allOf(expectedLidsMatcher));
    }
}
