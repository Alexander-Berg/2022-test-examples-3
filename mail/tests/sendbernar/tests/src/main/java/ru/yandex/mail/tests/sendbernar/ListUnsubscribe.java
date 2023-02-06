package ru.yandex.mail.tests.sendbernar;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.IsThereMessagesMatcher.hasMsgsIn;
import static ru.yandex.mail.things.matchers.StringDiffer.notDiffersWith;

@Aqua.Test
@Title("Ручка list_unsubscribe")
@Description("Отправляем письмо об отписке от рассылки")
@Stories("mail send")
public class ListUnsubscribe extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.listUnsubscribe;
    }

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(authClient).inbox().outbox();

    @Test
    @Description("Отправляю простое письмо об отписке")
    public void shouldSendMessage() throws Exception {
        listUnsubscribe()
                .withTo(authClient.account().email()).post(shouldBe(ok200()));

        waitWith.inbox().waitDeliver();

        assertThat("Есть письмо в отправленных",
                authClient,
                hasMsgsIn(0, folderList.sentFID()));
    }

    @Test
    @Description("Отправляю простое письмо об отписке с темой и телом")
    public void shouldSendMessageWithSubjectAndBodyAndFromMailbox() throws Exception {
        String body = Random.string();
        String fromMailbox = authClient.account().email("yandex.by");


        listUnsubscribe()
                .withTo(authClient.account().email())
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
                msg.content(),
                notDiffersWith(body)
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));
    }
}
