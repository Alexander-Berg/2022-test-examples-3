package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ComposeCheck;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasXPath;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj.msg;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.junitextensions.rules.retry.RetryRule.retry;

@Aqua.Test
@Title("Отправка писем. ПДД юзеры")
@Description("Отправляет простые письма без особых изысков. Тестим пдд")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND, MyStories.PDD})
public class MailSendPDD {

    public static final String DOMAIN_PDD = "kida-lo-vo.name";
    public static final String RF_DOMAIN_PDD = "админкапдд.рф";

    private Logger logger = Logger.getLogger(this.getClass());

    @ClassRule
    public static HttpClientManagerRule pddRFAuth = auth().with("vicdev@" + RF_DOMAIN_PDD, "testqa");

    @ClassRule
    public static HttpClientManagerRule pddAuth = auth().with("lanwen@" + DOMAIN_PDD, "testqa");

    @Rule
    public RuleChain logConfigRule = new LogConfigRule().around(retry().ifException(AssertionError.class).times(1));

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(pddAuth).outbox().inbox();

    @Rule
    public CleanMessagesRule cleanRF = CleanMessagesRule.with(pddRFAuth).outbox().inbox();

    @Test
    @Description("Отправка ПДД юзером письма самому себе.\n" +
            "Ожидаемый результат: письмо дошло.")
    @Issue("DARIA-24291")
    public void testPddSend() throws Exception {
        logger.warn("[DARIA-24291]");
        DefaultHttpClient hc = pddAuth.authHC();
        String subj = sendMailTo(pddAuth.acc().getLogin(), DOMAIN_PDD, hc);
        clean.subject(subj);
        assertThat(hc, withWaitFor(hasMsg(subj)));
    }

    @Test
    @Issue("DARIA-24291")
    @Description("Отправка ПДД юзером письма ППД юзеру с русским доменом.\n" +
            "Ожидаемый результат: письмо дошло.")
    public void testPddRFSend() throws Exception {
        logger.warn("[DARIA-24291]");
        DefaultHttpClient hc = pddRFAuth.authHC();
        String subj = sendMailTo(pddRFAuth.acc().getLogin(), RF_DOMAIN_PDD, hc);
        cleanRF.subject(subj);
        assertThat(hc, withWaitFor(hasMsg(subj)));
    }


    private String sendMailTo(String login, String domain, DefaultHttpClient hc) throws IOException {
        String composeCheck = api(ComposeCheck.class).get().via(hc).getComposeCheckNodeValue();

        MailSendMsgObj msg = msg()
                .setTo(login)
                .setSubj("Тестовое письмо с ПДД " + getRandomString() + " " + domain)
                .setSend("Сцылко: http://ya.ru")
                .setComposeCheck(composeCheck);

        api(MailSend.class).params(msg)
                .post().via(hc)
                .withDebugPrint()
                .assertDocument(hasXPath("//result", containsString("ok")));
        return msg.getSubj();
    }
}