package ru.yandex.mail.tests.sendbernar;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.junit.Assert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.IsThereMessagesMatcher.hasMsgIn;
import static ru.yandex.mail.things.matchers.WithWaitFor.withWaitFor;

@Aqua.Test
@Title("Отправка писем. ПДД юзеры")
@Description("Отправляет простые письма без особых изысков. Тестим пдд")
@Stories({"mail send", "pdd"})
public class PddTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.pdd;
    }

    private UserCredentials pddAuth = new UserCredentials(Accounts.pddTo);

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(pddAuth).inbox().outbox();

    @Test
    @Description("Отправка ПДД юзером письма самому себе")
    public void testPddSend() throws Exception {
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .withText(Random.string())
                .post(shouldBe(ok200()));


        assertThat("Письмо не найдено во входящих",
                authClient,
                withWaitFor(hasMsgIn(subj, folderList.defaultFID())));
    }

    @Test
    @Issue("DARIA-24291")
    @Description("Отправка ПДД юзером письма ППД юзеру с русским доменом")
    public void testPddRFSend() throws Exception {
        sendMessage()
                .withTo(pddAuth.account().email())
                .withSubj(subj)
                .withText(Random.string())
                .post(shouldBe(ok200()));


        assertThat("Письмо не найдено во входящих",
                pddAuth,
                withWaitFor(hasMsgIn(subj, folderList.defaultFID())));
    }
}

