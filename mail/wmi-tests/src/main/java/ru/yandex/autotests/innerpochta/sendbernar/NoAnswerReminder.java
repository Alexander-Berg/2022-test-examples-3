package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;


@Aqua.Test
@Title("Письма-напоминание о неответе")
@Description("Проверяем выдачу ручки no_answer_remind")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.REMINDER)
@Credentials(loginGroup = "NoAnswerReminder")
public class NoAnswerReminder extends BaseSendbernarClass {
    private Envelope letterInSent;
    private String lid;

    @ClassRule
    public static HttpClientManagerRule receiverTo = auth().with("NoAnswerReminderReciever");

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(receiverTo).inbox().outbox();

    @Before
    public void sendMessageWithReminderPeriod() throws Exception {
        lid = lidByName(noAnswerReminderLabelName);

        sendMessage()
                .withTo(receiverTo.acc().getSelfEmail())
                .withSubj(subj)
                .withLids(lid)
                .withNoanswerRemindPeriod("10")
                .post(shouldBe(ok200()));


        waitWith.subj(subj).usingHttpClient(receiverTo).waitDeliver();
        letterInSent = waitWith.subj(subj).sent().waitDeliver().getEnvelope().get();
    }

    @Test
    @Title("Проверяем, что метка есть на письме до ответа и снимается после ответа")
    public void shouldUnsetTheLabelInCaseOfTheLetterToBeAnswered() throws Exception {
        assertThat("Системная метка должны была поставиться в отправленном письме ",
                authClient, hasMsgWithLidInFolder(letterInSent.getMid(), folderList.sentFID(), lid));


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withInreplyto(letterInSent.getRfcId())
                .withReferences(letterInSent.getRfcId())
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();
        Message ms = byMid(mid);


        assertThat("<From> письма не совпадает с ожидаемым",
                ms.fromEmail(),
                equalTo(authClient.acc().getSelfEmail().toLowerCase()));
        assertThat("Метка не снялась",
                authClient,
                not(hasMsgWithLidInFolder(letterInSent.getMid(), folderList.sentFID(), lid)));
    }

    @Test
    @Title("Проверяем, что метка есть на письме до ответа и снимается после доставки ремайндера")
    public void shouldUnsetTheLabelInCaseOfTheRemindMessageToBeReceived() throws Exception {
        assertThat("Системная метка должны была поставиться в отправленном письме ",
                authClient, hasMsgWithLidInFolder(letterInSent.getMid(), folderList.sentFID(), lid));


        String mid = waitWith.subj(subj).waitDeliver().getMid();
        Message ms = byMid(mid);


        assertThat("<From> напоминания не совпадает с ожидаемым",
                ms.fromEmail(),
                equalTo("noreply@yandex.ru"));
        assertThat("Метка не снялась",
                authClient,
                not(hasMsgWithLidInFolder(letterInSent.getMid(), folderList.sentFID(), lid)));
    }

    @Test
    @Title("Проверяем, что метка есть на письме-напоминании")
    public void shouldSeeSomeSystemLabels() throws Exception {
        String mid = waitWith.subj(subj).waitDeliver().getMid();


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
