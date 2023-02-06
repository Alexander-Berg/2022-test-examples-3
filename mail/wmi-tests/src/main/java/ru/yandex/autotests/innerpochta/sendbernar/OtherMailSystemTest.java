package ru.yandex.autotests.innerpochta.sendbernar;


import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Scope;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.*;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes.PRODUCTION;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;


@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляем письма в другие почтовые системы")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "SendToEnemyMailTest")
@RunWith(DataProviderRunner.class)
@Scope(PRODUCTION)
public class OtherMailSystemTest extends BaseSendbernarClass {
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
