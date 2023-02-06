package ru.yandex.mail.tests.sendbernar;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;

@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляем письма в другие почтовые системы")
@Stories("mail send")
@RunWith(DataProviderRunner.class)
public class OtherMailSystemTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.otherMailSystem;
    }

    @Test
    @Issues({@Issue("AUTOTESTPERS-158"), @Issue("MPROTO-1550")})
    @Description("Отсылаем письмо в другую почтовую систему и проверяем его наличие в отправленных")
    @DataProvider({
            "antivagcom@gmail.com",
            "alena-test1@mail.ru",
            "vicdev@yahoo.com",
            "alena-test1@rambler.ru"
    })
    public void shouldSendMessageToAnotherMailSystem(String to) throws Exception {
        sendMessage()
                .withSubj(subj)
                .withTo(to)
                .withText("Тестовое письмо от Yandex. Не отвечайте на него")
                .post(shouldBe(ok200()));


        waitWith.subj(subj).sent()
                .errorMsg("Не появилось письмо в папке \"Отправленные\" для " + to)
                .waitDeliver();
    }
}

