package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveDraftResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendShareResponse;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendshare.ApiSendShare;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.*;


@Aqua.Test
@Title("Ручка send_share")
@Description("Проверяем работу ручки send_share")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "SendShare")
public class SendShareTest extends BaseSendbernarClass {
    @ClassRule
    public static HttpClientManagerRule adminClient = auth().with("SendShareAdmin");

    public static FolderList adminFolderList = new FolderList(adminClient);

    @Rule
    public CleanMessagesMopsRule adminClean = new CleanMessagesMopsRule(adminClient).allfolders();

    ApiSendShare sendShare() {
        return super
                .sendShare()
                .withAdminUid(adminClient.account().uid());
    }

    @Test
    @Description("Проверяем, что From берётся из admin_uid")
    public void shouldTakeFromEmailFromAdminUid() throws Exception {
        sendShare()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String fromEmail = byMid(waitWith.subj(subj).waitDeliver().getMid()).fromEmail();


        assertThat("Новый from_mailbox не совпадает с from",
                adminClient.acc().getSelfEmail(),
                is(fromEmail));
    }

    @Test
    @Title("Отправка письма с source_mid из черновиков")
    @Description("Не должны удалять source_mid")
    public void shouldNotRemoveSourceMid() throws Exception {
        String draft = saveDraft()
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        sendShare()
                .withTo(authClient.acc().getSelfEmail())
                .withSourceMid(draft)
                .withSubj(subj)
                .post(shouldBe(ok200()));
        waitWith.subj(subj).waitDeliver().getMid();


        byMid(draft).exists();
    }

    @Test
    @Title("Проверяем, сохраняем ли параметр message_id")
    public void shouldSaveMessageId() {
        String messageId = "<1121537786231@wmi5-qa.yandex.ru>";

        SendShareResponse resp = sendShare()
                .withTo(authClient.acc().getSelfEmail())
                .withMessageId(messageId)
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendShareResponse.class);

        assertThat("message_id в ответе сендбернара не совпадает с переданным",
                resp.getMessageId(),
                equalTo(messageId));

        String mid = waitWith.subj(subj).waitDeliver().getMid();

        String messageIdHeader = byMid(mid).getHeader("message-id");

        assertThat("message_id в ответе mbody не совпадает с переданным в sendbernar",
                messageIdHeader,
                equalTo(messageId));
    }

    @Test
    @Title("Проверяем что письмо остаётся в отправленных у админа")
    public void shouldSaveToSent() {
        sendShare()
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(ok200()));

        waitWith.subj(WmiConsts.NO_SUBJECT_TITLE).waitDeliver();

        assertThat("Отправленное письмо не должно быть сохранено в аккаунте отправляемого",
                authClient,
                hasMsgsIn(0, folderList.sentFID()));

        assertThat("Отправленное письмо должно быть сохранено в аккаунте админа",
                adminClient,
                hasMsgsIn(1, adminFolderList.sentFID()));
    }
}
