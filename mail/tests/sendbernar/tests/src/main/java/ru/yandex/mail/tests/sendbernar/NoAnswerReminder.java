package ru.yandex.mail.tests.sendbernar;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLid;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLidInFolder;

@Aqua.Test
@Title("Письма-напоминание о неответе")
@Description("Проверяем выдачу ручки no_answer_remind")
@Stories("reminder")
public class NoAnswerReminder extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.noAnswer;
    }

    private String midLetterInSent;
    private String lid;

    @ClassRule
    public static UserCredentials receiverTo = new UserCredentials(Accounts.noAnswerTo);

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(receiverTo).inbox().outbox();

    @Before
    public void sendMessageWithReminderPeriod() throws Exception {
        lid = lidByName(noAnswerReminderLabelName);

        sendMessage()
                .withTo(receiverTo.account().email())
                .withSubj(subj)
                .withLids(lid)
                .withNoanswerRemindPeriod("10")
                .post(shouldBe(ok200()));


        waitWith.subj(subj).usingHttpClient(receiverTo).waitDeliver();
        midLetterInSent = waitWith.subj(subj).sent().waitDeliver().getMid();
    }

    @Step("Ждём получения напоминания о неответе")
    private String waitForNoAnserLetter() {
        return waitWith.subj(subj).waitDeliver().getMid();
    }

    @Test
    @Title("Проверяем, что метка есть на письме до ответа и снимается после ответа")
    public void shouldUnsetTheLabelInCaseOfTheLetterToBeAnswered() throws Exception {
        assertThat("Системная метка должны была поставиться в отправленном письме ",
                authClient, hasMsgWithLidInFolder(midLetterInSent, folderList.sentFID(), lid));

        waitForNoAnserLetter();

        Message ms = byMid(midLetterInSent);

        assertThat("<From> письма не совпадает с ожидаемым",
                ms.fromEmail(),
                equalTo(authClient.account().email().toLowerCase()));
        assertThat("Метка не снялась",
                authClient,
                not(hasMsgWithLidInFolder(midLetterInSent, folderList.sentFID(), lid)));
    }

    @Test
    @Title("Проверяем, что метка есть на письме до ответа и снимается после доставки ремайндера")
    public void shouldUnsetTheLabelInCaseOfTheRemindMessageToBeReceived() throws Exception {
        assertThat("Системная метка должны была поставиться в отправленном письме ",
                authClient, hasMsgWithLidInFolder(midLetterInSent, folderList.sentFID(), lid));


        String mid = waitForNoAnserLetter();
        Message ms = byMid(mid);


        assertThat("<From> напоминания не совпадает с ожидаемым",
                ms.fromEmail(),
                equalTo("noreply@yandex.ru"));
        assertThat("Метка не снялась",
                authClient,
                not(hasMsgWithLidInFolder(midLetterInSent, folderList.sentFID(), lid)));
    }

    @Test
    @Title("Проверяем, что метка есть на письме-напоминании")
    public void shouldSeeSomeSystemLabels() throws Exception {
        String mid = waitForNoAnserLetter();

        String people = lidByNameAndType("4", "so");
        String reminder = lidByNameAndType("59", "so");
        String greeting = lidByNameAndType("12", "so");


        assertThat("Системная метка должна была поставиться в письме-напоминании ",
                authClient, hasMsgWithLid(mid, people));
        assertThat("Системная метка должна была поставиться в письме-напоминании ",
                authClient, hasMsgWithLid(mid, reminder));
        assertThat("Системная метка должна была поставиться в письме-напоминании ",
                authClient, hasMsgWithLid(mid, greeting));
    }
}
