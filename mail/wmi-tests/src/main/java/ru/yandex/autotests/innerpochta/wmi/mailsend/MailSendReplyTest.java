package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.filter.VDirectCut;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageBody;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FAKE_SEEN_LBL;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.06.14
 * Time: 12:18
 */
@Aqua.Test
@Title("Отвечаем на письма")
@Description("Отвечаем на письмо. Сохранияем ответ в черновики")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "MailSendReply")
public class MailSendReplyTest extends BaseTest {

    public static final String REPLIED = "replied";

    private String mid;
    private String subj;
    private String messageId;

    @Rule
    public CleanMessagesRule clean = with(authClient).all().inbox().outbox();

    @Before
    public void prepareLetter() throws Exception {
        SendUtils sendUtils = sendWith.waitDeliver().viaProd().send();
        mid = sendUtils.getMid();
        subj = sendUtils.getSubj();
        messageId = jsx(MessageBody.class).params(MessageObj.getMsg(mid)).filters(new VDirectCut())
                .get().via(hc).getMessageId();
    }

    @Test
    @Description("Отсылаем себе письмо. Затем отсылаем себе ответ\n" +
            "Ожидаемый резльтат: ответ есть во входящих")
    public void replyToMailTest() throws Exception {
        MailSendMsgObj obj = MailSendMsgObj.empty()
                .setSubj("Re:" + subj)
                .setComposeCheck(composeCheck.getComposeCheckNodeValue())
                .setFromMailbox(authClient.acc().getSelfEmail())
                .setMarkIds(mid)
                .setMarkAs(REPLIED)
                .setOverwrite(mid)
                .setIgnOverwrite("yes")
                        //два параметра для ответа
                .setReferences(messageId)
                .setInreplyto(messageId)
                .setTo(authClient.acc().getSelfEmail());
        jsx(MailSend.class).params(obj).post().via(hc).shouldBe().statusOk();
        //Re: парсится как prefix и письма скливаются в один тред
        List<String> mids = waitWith.subj(subj).count(2).waitDeliver().getMids();
        mids.remove(mid);
        String midReplied = mids.get(0);

        assertThat(hc, hasMsgWithLid(mid, labels.answered()));
        assertThat(hc, not(hasMsgWithLid(midReplied, labels.answered())));
    }

    @Test
    @Stories(MyStories.DRAFT)
    @Issue("DARIA-54416")
    @Description("Отсылаем себе письмо. Затем ответ сораняем в черновики.\n" +
            "Ожидаемый резльтат: ответ есть в черновиках")
    public void saveReplyInDraftTest() throws Exception {
        MailSendMsgObj obj = MailSendMsgObj.empty()
                .setSubj("Re:" + subj)
                .setComposeCheck(composeCheck.getComposeCheckNodeValue())
                .setFromMailbox(authClient.acc().getSelfEmail())
                .setMarkAs(REPLIED)
                .setMarkIds(mid)
                .setSend(Util.getLongString())
                .setOverwrite(mid)
                .setIgnOverwrite("yes")
                .setNosend("yes")
                .setAutosave("yes")
                        //два параметра для ответа
                .setReferences(messageId)
                .setInreplyto(messageId)
                .setTo(authClient.acc().getSelfEmail());

        jsx(MailSend.class).params(obj).post().via(hc).shouldBe().statusOk();
        String midReplied = waitWith.subj(subj).count(1).inFid(folderList.draftFID()).waitDeliver().getMid();
        messageInDraftShouldBeSeen(midReplied, folderList.draftFID(), hc);

        assertThat(hc, not(hasMsgWithLid(mid, labels.answered())));
        assertThat(hc, not(hasMsgWithLidInFolder(midReplied, folderList.draftFID(), labels.answered())));
    }

    @Step("Письмо с мид {0} в папке \"Черновики\" <{1}> должно быть прочитано")
    public static void messageInDraftShouldBeSeen(String mid, String draftFid, DefaultHttpClient hc) {
        assertThat("Черновик должен быть прочитанным [DARIA-54416]", hc,
                hasMsgWithLidInFolder(mid, draftFid, FAKE_SEEN_LBL));
    }
}

