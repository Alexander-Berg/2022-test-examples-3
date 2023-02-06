package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.InReplyTo;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static java.net.URLEncoder.encode;
import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.NO_VDIRECT_LINKS_WRAP;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MbodyApi.apiMbody;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.InReplyToObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 04.06.14
 * Time: 19:41
 * <p/>
 * [DARIA-37817]
 */

@Aqua.Test
@Title("[HOUND] Ручка /in_reply_to. Возвращаем список черновиков для письма")
@Description("Тест полагается на существующие письма")
@Features(MyFeatures.HOUND)
@Stories(MyStories.MESSAGES_LIST)
@Issues({@Issue("DARIA-37817"), @Issue("MAILPG-380")})
@Credentials(loginGroup = "InReplyToTest")
public class InReplyToTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).before(true).allfolders();

    @Test
    @Title("In_reply_to с письмом без черновика")
    @Description("Дергаем in_reply_to с письмом без черновика.\n" +
            "Список черновиков должен быть пуст")
    public void replyToWithNotAnsweredLetter() throws Exception {
        String mid = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        String messageId = midToMessageId(uid(), mid, authClient.account().userTicket());

        List<Envelope> envelopes = api(InReplyTo.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setMessageId(encode(messageId))).get().via(authClient).resp().getEnvelopes();
        assertThat("Список черновиков оказался не пустым", envelopes, org.hamcrest.Matchers.empty());
    }

    @Test
    @Title("In_reply_to с черновиком")
    @Description("Отправляем письмо, отвечаем на письмо через sendbernar\n" +
            "Ожидаемый результат: есть 1 ответ в черновиках")
    public void replyToWithAnsweredLetter() throws Exception {
        String subject = Util.getRandomString();
        String mid = sendWith(authClient).viaProd().subj(subject).send().waitDeliver().getMid();

        String messageId = midToMessageId(uid(), mid, authClient.account().userTicket());

        //отвечаем на письмо:
        sendWith(authClient).viaProd()
                .subj("Re:" + subject)
                .text(Util.getLongString())
                .inReplyTo(messageId)
                .saveDraft()
                .strict()
                .waitDeliver();

        List<Envelope> envelopes = api(InReplyTo.class).setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setMessageId(encode(messageId))).get().via(authClient).resp().getEnvelopes();

        assertThat("Неверное количество черновиков", envelopes, hasSize(1));
    }

    @Test
    @Title("In_reply_to с черновиками")
    @Description("Сохраняем два ответа в черновики.\n" +
            "Ожидаемый результат: два письма в in_reply_to")
    public void replyToWithTwiceAnsweredLetter() throws Exception {
        String subject = Util.getRandomString();
        String mid = sendWith(authClient).viaProd().subj(subject).send().waitDeliver().getMid();

        String messageId = midToMessageId(uid(), mid, authClient.account().userTicket());
        //отвечаем на письмо:
        sendWith(authClient).viaProd().count(2)
                .subj("Re:" + subject)
                .text(Util.getLongString())
                .inReplyTo(messageId)
                .saveDraft()
                .strict()
                .waitDeliver();

        List<Envelope> envelopes = api(InReplyTo.class).setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setMessageId(encode(messageId))).get().via(authClient).resp().getEnvelopes();

        assertThat("Неверное количество черновиков", envelopes, hasSize(2));
    }

    @Test
    @Issue("DARIA-38630")
    @Title("In_reply_to с message_id в верхнем регистре")
    public void testWithMessageIdBigLetters() {
        String subject = Util.getRandomString();
        String mid = sendWith(authClient).viaProd().subj(subject).send()
                .waitDeliver().getMid();

        String messageId = midToMessageId(uid(), mid, authClient.account().userTicket()).toUpperCase();
        sendWith(authClient).viaProd().count(1)
                .subj("Re:" + subject)
                .text(Util.getLongString())
                .inReplyTo(messageId)
                .saveDraft()
                .strict()
                .waitDeliver();

        List<Envelope> envelopes = api(InReplyTo.class).setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setMessageId(encode(messageId))).get().via(authClient).resp().getEnvelopes();

        assertThat("Неверное количество черновиков", envelopes, hasSize(1));
    }

    private static String midToMessageId(String uid, String mid, String userTicket) {
        return apiMbody(userTicket).message()
                .withFlags(NO_VDIRECT_LINKS_WRAP.toString())
                .withMid(mid)
                .withUid(uid)
                .get(identity()).peek().as(Mbody.class).getInfo()
                .getMessageId();
    }
}
