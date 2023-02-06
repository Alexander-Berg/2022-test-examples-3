package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StringDiffer.notDiffersWith;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.ListUnsubscribeResponse.ok200;

@Aqua.Test
@Title("Ручка list_unsubscribe")
@Description("Отправляем письмо об отписке от рассылки")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "ListUnsubscribeTest")
public class ListUnsubscribe extends BaseSendbernarClass {
    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(authClient).inbox().outbox();

    @Test
    @Description("Отправляю простое письмо об отписке")
    public void shouldSendMessage() throws Exception {
        listUnsubscribe()
                .withTo(authClient.acc().getSelfEmail()).post(shouldBe(ok200()));

        waitWith.inbox().waitDeliver();

        assertThat("Есть письмо в отправленных",
                authClient,
                hasMsgsIn(0, folderList.sentFID()));
    }

    @Test
    @Description("Отправляю простое письмо об отписке с темой и телом")
    public void shouldSendMessageWithSubjectAndBodyAndFromMailbox() throws Exception {
        String body = Util.getRandomString();
        String fromMailbox = authClient.acc().getLogin()+"@yandex.by";


        listUnsubscribe()
                .withTo(authClient.acc().getSelfEmail())
                .withBody(body)
                .withSubject(subj)
                .withFromMailbox(fromMailbox)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();
        Message msg = byMid(mid, authClient);


        assertThat("Заголовок From на письме не совпадает",
                msg.fromEmail(),
                equalTo(fromMailbox));
        assertThat("Тема письма не совпадает с ожидаемым",
                msg.subject(),
                equalTo(subj));
        assertThat("Текст письма не совпадает с ожидаемым",
                msg.sourceContent(),
                notDiffersWith(body)
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));
    }
}
