package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.beans.yplatform.Error;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByMessageId;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeNoException;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withCode;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withMessage;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withReason;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MbodyApi.apiMbody;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByMessageIdObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] ручка messages_by_message_id")
@Description("Общие тесты на ручку messages_by_message_id")
@Features(MyFeatures.HOUND)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = "MessagesByMessageIdTest")
@Issue("MAILPG-1393")
public class MessagesByMessageIdTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("messages_by_message_id без fid'а с пустым ответом")
    public void testMessagesByMessageIdWithoutFidForEmptyResponse() throws Exception {
        MessagesByMessageId response = getMessagesByMessageId("1");

        assertNull("Получили ошибку, которую не ждали", response.parsed());

        List<String> mids = response.resp();
        assertThat("Нашлось письмо, которого быть не должно", mids.size(), equalTo(0));
    }

    @Test
    @Title("messages_by_message_id без fid'а с ответом из нескольких mid'ов")
    public void testMessagesByMessageIdWithoutFidForResponseWithManyMids() throws Exception {
        List<String> messageIds = sendAndReturnMsgIds();

        assertThat("Количество msgId не совпадает с ожидаемым", messageIds.size(), equalTo(1));

        List<String> mids = getMessagesByMessageId(messageIds.get(0)).resp();

        assertThat("Количество писем не совпадает с ожидаемым", mids.size(), equalTo(2));
    }

    @Test
    @Title("messages_by_message_id с fid'ом с пустым ответом")
    public void testMessagesByMessageIdWithFidForEmptyResponse() throws Exception {
        List<String> messageIds = sendAndReturnMsgIds();

        assertThat("Количество msgId не совпадает с ожидаемым", messageIds.size(), equalTo(1));

        String NONEXISTENT_FID = "65535";
        Error err = getMessagesByMessageId(messageIds.get(0), NONEXISTENT_FID).parsed();

        assertThat("Должна быть ошибка", err, allOf(
                withCode(is(31L)),
                withMessage(is("internal error")),
                withReason(is("expected exactly one mid, but query returned 0"))
        ));
    }

    @Test
    @Title("messages_by_message_id с fid'ом с ответом из одного mid'а")
    public void testMessagesByMessageIdWithFidForResponseWithOneMid() throws Exception {
        List<String> messageIds = sendAndReturnMsgIds();

        assertThat("Количество msgId не совпадает с ожидаемым", messageIds.size(), equalTo(1));

        List<String> mids = getMessagesByMessageId(messageIds.get(0), folderList.defaultFID()).resp();

        assertThat("Количество писем не совпадает с ожидаемым", mids.size(), equalTo(1));
    }

    @Test
    @Title("messages_by_message_id с fid'ом с ответом из нескольких mid'ов")
    public void testMessagesByMessageIdWithFidForResponseWithManyMids() throws Exception {
        String SUBJECT = "testMessagesByMessageIdWithFidForResponseWithManyMids";

        List<String> mids = send(SUBJECT);
        assertThat("Количество писем не совпадает с ожидаемым", mids.size(), equalTo(1));

        Mops.complexMove(authClient, folderList.sentFID(), new MidsSource(mids.get(0)))
                .post(shouldBe(okSync()));

        String messageId = convertMidToMessageId(mids.get(0));
        Error err = getMessagesByMessageId(messageId, folderList.sentFID()).parsed();

        assertThat("Должна быть ошибка", err, allOf(
                withCode(is(31L)),
                withMessage(is("internal error")),
                withReason(is("expected exactly one mid, but query returned 2"))
        ));
    }

    private List<String> send(String subj) throws Exception {
        return sendWith(authClient).subj(subj).send().waitDeliver().getMids();
    }

    private List<String> send() throws Exception {
        return sendWith(authClient).send().waitDeliver().getMids();
    }

    private static String encode(String content) {
        try {
            return URLEncoder.encode(content, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            assumeNoException(e);
        }
        return content;
    }

    private String convertMidToMessageId(String mid) {
        String messageId = apiMbody(authClient.account().userTicket()).message()
                .withUid(uid())
                .withMid(mid)
                .get(Function.identity())
                .peek().as(Mbody.class).getInfo()
                .getMessageId();

        return encode(messageId);
    }

    private List<String> convertMidsToMessageIds(List<String> mids) {
        return mids.stream()
                .map(this::convertMidToMessageId)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> sendAndReturnMsgIds() throws Exception {
        return convertMidsToMessageIds(send());
    }

    private MessagesByMessageId getMessagesByMessageId(String msgId) {
        return api(MessagesByMessageId.class)
                .setHost(props().houndUri())
                .params(empty()
                        .setUid(uid())
                        .setMsgId(msgId)
                )
                .get()
                .via(authClient);
    }

    private MessagesByMessageId getMessagesByMessageId(String msgId, String fid) {
        return api(MessagesByMessageId.class)
                .setHost(props().houndUri())
                .params(empty()
                        .setUid(uid())
                        .setMsgId(msgId)
                        .setFid(fid)
                )
                .get()
                .via(authClient);
    }
}
