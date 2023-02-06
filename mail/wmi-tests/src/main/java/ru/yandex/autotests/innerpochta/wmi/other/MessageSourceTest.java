package ru.yandex.autotests.innerpochta.wmi.other;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageSourceObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.*;

import java.util.Arrays;

import static com.google.common.base.Joiner.on;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

/**
 * Unmodify
 */
@Aqua.Test
@Title("Проверка сорцов письма")
@Description("Смотрим на jsx выдачу [DARIA-7475]")
@Features(MyFeatures.WMI)
@Stories(MyStories.MESSAGE_SOURCE)
@Issue("DARIA-7475")
@Credentials(loginGroup = "SourceTest")
public class MessageSourceTest extends BaseTest {

    public static final String UTF_STRING = "Русские и китайские символы 时间是最好的稀释剂，新舆论热点的出现，" +
            "不断转移公众的视线，掩盖了旧闻的解决。但是，一" +
            "个成熟的社会不会因为新热点的出现而习惯性地遗忘“旧闻”";

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).inbox().outbox();

    @Test
    @Issue("DARIA-7475")
    @Title("Сорцы письма как jsx")
    public void getMessageSourceTest() throws Exception {
        logger.warn("Сорцы письма как jsx [DARIA-7475]");

        MessageSource oper = api(MessageSource.class)
                .params(new MessageSourceObj()
                        .setMid(api(MailBoxList.class).post().via(hc).getMidOfFirstMessage()));

        MessageSource resp = oper.post().via(hc);

        assertThat("С исходником что-то не так", resp.toString(), allOf(
                containsString("Received:"),
                containsString("From:"),
                containsString("Subject:")));
    }

    @Test
    @Issue("DARIA-23733")
    @Title("Должны содержаться русские символы")
    public void shouldContainRusSymbols() throws Exception {
        logger.warn("[DARIA-23733]");
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg().setSend(UTF_STRING);
        clean.subject(msg.getSubj());
        api(MailSend.class).params(msg).post().via(hc);

        String mid = waitWith.subj(msg.getSubj()).waitDeliver().getMid();

        api(MessageSource.class).params(MessageSourceObj.getSourceByMid(mid)).post().via(hc)
                .assertResponse("Исходник должен содержать русские символы [DARIA-23733]", containsString(UTF_STRING));
    }

    @Test
    @Issue("DARIA-24445")
    @Title("Заголовок References не должен содержать переносов, ломающих ref строки")
    public void sendLongReferences() throws Exception {
        logger.warn("[DARIA-24445]");
        String[] ref = {
                "<issue-97dfsdsdfsdsdf9739@jira-bugs>",
                "<1898727330.18655.1335522965914.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<520931115.16663.1335521415678.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<408010231.16468.1335521406158.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<1451333304.16068.1335520873980.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<953565332.15807.1335520748443.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<1257070065.15676.1335520515528.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<2101202272.15140.1335520155654.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<1404504538.14732.1335519909768.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<1454510090.14352.1335519552441.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<2030884372.13684.1335518946885.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<2132896298.12671.1335517926790.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<676140582.12053.1335517331480.JavaMail.jira@jirabugs01e.tools.yandex.net>",
                "<0987654321@localhost.localdomain>"
        };
        logger.info("Длина ref: " + on(" ").join(ref).length());
        MailSendMsgObj msg = msgFactory
                .getSimpleEmptySelfMsg().setSend("Reply!")
                .setReferences(on(" ").join(ref));
        clean.subject(msg.getSubj());

        api(MailSend.class).params(msg).post().via(hc);

        String mid = waitWith.subj(msg.getSubj()).waitDeliver().getMid();

        MessageSource source = api(MessageSource.class).params(MessageSourceObj.getSourceByMid(mid)).post().via(hc).withDebugPrint();

        String referencesHeader = StringUtils.substringBetween(source.messageSource(), "References: ", "Subject: ");
        assertThat("Заголовок References не должен содержать переносов, ломающих ref строки [DARIA-24445]",
                Arrays.asList(referencesHeader.split(" |\r\n|\t")), hasItems(ref));
    }
}
