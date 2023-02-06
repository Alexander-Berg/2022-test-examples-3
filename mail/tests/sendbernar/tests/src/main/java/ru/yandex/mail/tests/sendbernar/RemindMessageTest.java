package ru.yandex.mail.tests.sendbernar;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.okEmptyBody;
import static ru.yandex.mail.tests.sendbernar.models.RemindMsgProperties.langProps;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLid;

@Aqua.Test
@Title("Письма-напоминание о письме")
@Description("Проверяем две ручки: set_msg_reminder и remind_message. Различные кейсы для различных языков")
@Stories("reminder")
@Issues({@Issue("DARIA-49553"), @Issue("DARIA-51146"), @Issue("MPROTO-2049"), @Issue("MAILPG-1264")})
public class RemindMessageTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.remind;
    }

    private static String LANG_RU = "ru";

    @Test
    @Description("Дергаем ручку remind_message на письме без метки")
    public void shouldNotSetReminderAtUnlabeledLetter() throws IOException {
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        remindMessage()
                .withLang(LANG_RU)
                .withMid(mid)
                .withDate(String.valueOf(System.currentTimeMillis()))
                .withAccount(authClient.account().email())
                .post(shouldBe(okEmptyBody()));


        waitWith.subj(langProps(LANG_RU).getSubj(subj)).count(0).waitDeliver();
    }

    @Test
    @Description("Проверка, что системная метка поставилась на письмо")
    public void shouldSetLabelOnTheLetterAndDeleteItAfterReply() throws IOException {
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();

        long extraYears = TimeUnit.SECONDS.convert(365 * 1000, TimeUnit.DAYS);

        setMsgReminder()
                .withMid(mid)
                .withDate(String.valueOf(System.currentTimeMillis() / 1000 + extraYears))
                .get(shouldBe(okEmptyBody()));


        String lid = lidByName(remindMessageLabelName);


        assertThat(String.format("Нет системной метки %s на письме", remindMessageLabelName),
                authClient,
                hasMsgWithLid(mid, lid));


        String replySubj = langProps(LANG_RU).getSubj(subj);
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(replySubj)
                .post(shouldBe(ok200()));


        String repliedMid = waitWith.subj(replySubj).waitDeliver().getMid();


        assertThat("Должны снимать метку, после ответа",
                authClient,
                not(hasMsgWithLid(repliedMid, lid)));
    }

    @Test
    @Issue("MPROTO-2049")
    @Title("Должны увидеть письмо-напоминание")
    public void shouldSendRemindMessage() throws IOException {
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String replySubj = langProps(LANG_RU).getSubj(subj);
        String timestamp = String.valueOf((System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20)) / 1000);


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        setMsgReminder()
                .withMid(mid)
                .withDate(timestamp)
                .get(shouldBe(okEmptyBody()));


        waitWith.subj(replySubj).waitDeliver().getMid();
    }

    @Test
    @Title("Проверяем, что метка есть на письме-напоминании")
    public void shouldSeeSomeSystemLabels() throws Exception {
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();
        String timestamp = String.valueOf((System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10)) / 1000);


        setMsgReminder()
                .withMid(mid)
                .withDate(timestamp)
                .get(shouldBe(okEmptyBody()));


        String midReminder = waitWith.subj(langProps(LANG_RU).getSubj(subj)).waitDeliver().getMid();


        String people = lidByNameAndType("4", "so");
        String reminder = lidByNameAndType("59", "so");
        String greeting = lidByNameAndType("12", "so");


        assertThat("Системная метка должна была поставиться в письме-напоминании ",
                authClient,
                hasMsgWithLid(midReminder, people));
        assertThat("Системная метка должна была поставиться в письме-напоминании ",
                authClient,
                hasMsgWithLid(midReminder, reminder));
        assertThat("Системная метка должна была поставиться в письме-напоминании ",
                authClient,
                hasMsgWithLid(midReminder, greeting));
    }
}
