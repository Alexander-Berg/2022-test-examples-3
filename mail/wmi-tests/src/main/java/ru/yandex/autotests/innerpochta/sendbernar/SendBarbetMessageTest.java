package ru.yandex.autotests.innerpochta.sendbernar;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FoldersObj;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders.folders;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;

@Aqua.Test
@Title("Ручка send_barbet_message")
@Description("Проверяем, что письмо отправилось")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@RunWith(DataProviderRunner.class)
@Credentials(loginGroup = "SendBarbetMessage")
public class SendBarbetMessageTest extends BaseSendbernarClass {
    @DataProvider
    public static Object[][] templates() {
        return new String[][] {
                {"create_failed"},
                {"restore_failed"}
        };
    }

    @Test
    @Description("Проверяем, что шаблон отправился")
    @UseDataProvider("templates")
    public void shouldSendLetter(String template) throws IOException {
        int numberOfMessages = folders(
                FoldersObj.empty().setUid(authClient.account().uid())
        )
                .get()
                .via(authClient)
                .count(folderList.defaultFID());

        assertThat("Недоочищенный inbox", numberOfMessages, equalTo(0));

        sendBarbetMessage()
                .withType(template)
                .withUniqId("ololo")
                .post(shouldBe(ok200()));

        assertThat("Письмо не дошло", authClient, withWaitFor(hasMsgsIn(1, folderList.defaultFID())));
    }
}
