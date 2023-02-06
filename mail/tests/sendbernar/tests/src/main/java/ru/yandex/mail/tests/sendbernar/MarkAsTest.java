package ru.yandex.mail.tests.sendbernar;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.sendbernar.generated.SendMessageResponse;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLidInFolder;

@Aqua.Test
@Title("Взаимодействие send_message с mops")
@Description("Запросы в mops при отправке писем")
@Stories("mail send")
@RunWith(DataProviderRunner.class)
public class MarkAsTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.markAs;
    }
    
    private UserCredentials another = new UserCredentials(Accounts.markAsTo);

    @Rule
    public CleanMessagesMopsRule cleanAnother = new CleanMessagesMopsRule(another).inbox().outbox().draft().deleted();


    @DataProvider
    public static Object[][] cases() {
        return new Object[][]{
                {"replied", "answered"},
                {"forwarded", "forwarded"},
        };
    }

    @Test
    @Description("Отвечаем на письмо и проверяем метку")
    @UseDataProvider("cases")
    public void replyAndCheckUserJournalClientType(String markAs, String labelName) {
        String secondMessageId = sendMessage()
                    .withUid(another.account().uid())
                    .withCaller(caller)
                    .withTo(authClient.account().email())
                    .withSubj(subj)
                    .post(shouldBe(ok200()))
                    .as(SendMessageResponse.class)
                    .getMessageId();

        String midOfLetterToBeMarked = waitWith.subj(subj).inbox().waitDeliver().getMid();


        String anotherSubj = Random.string();
        sendMessage()
                .withTo(another.account().email())
                .withSubj(anotherSubj)
                .withInreplyto(secondMessageId)
                .withMarkAs(markAs)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class);

        waitWith.subj(anotherSubj).sent().waitDeliver();


        String lid = lidByName(labelName);


        assertThat("Письмо не помечено как " + markAs,
                authClient,
                hasMsgWithLidInFolder(midOfLetterToBeMarked, folderList.defaultFID(), lid));
    }
}
