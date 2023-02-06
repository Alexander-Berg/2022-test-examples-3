package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageSourceObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 22.04.14
 * Time: 18:49
 * <p/>
 * [DARIA-35109]
 */
@Aqua.Test
@Title("Smtp инъекция")
@Description("Отправляем себе письмо с smtp инъекцией в subject-е")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Issue("DARIA-35109")
@Credentials(loginGroup = "SmtpInjection")
public class MailSendWithSmptpInjection extends BaseTest {
    private String mid;
    private String subj;

    public static final String INJECTION = "key=value";

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox().all();

    @Test
    public void sendMailWithSmtpIjection() throws Exception {
        subj = Util.getRandomString() + "\n" + INJECTION;
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg().setSubj(subj);
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        String subjectInInbox = subj.replace("\n", " ");
        mid = waitWith.subj(subjectInInbox).waitDeliver().getMid();

        MessageSource resp = api(MessageSource.class).params(MessageSourceObj.getSourceByMid(mid)).post().via(hc);

        assertThat("Обнаружили заголовок-инъекцию в сорцах письма [DARIA-35109]" + INJECTION, resp.toString(),
                containsString("\tkey=value"));
    }
}
