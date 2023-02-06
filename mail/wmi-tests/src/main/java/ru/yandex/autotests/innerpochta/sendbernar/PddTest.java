package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgIn;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;


@Aqua.Test
@Title("Отправка писем. ПДД юзеры")
@Description("Отправляет простые письма без особых изысков. Тестим пдд")
@Features(MyFeatures.SENDBERNAR)
@Stories({MyStories.MAIL_SEND, MyStories.PDD})
@Credentials(loginGroup = "PDDUsual")
public class PddTest extends BaseSendbernarClass {
    @ClassRule
    public static HttpClientManagerRule pddAuth = auth().with("Adminkapdd");

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(pddAuth).inbox().outbox();

    @Test
    @Description("Отправка ПДД юзером письма самому себе")
    public void testPddSend() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withText(getRandomString())
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
                .withTo(pddAuth.acc().getSelfEmail())
                .withSubj(subj)
                .withText(getRandomString())
                .post(shouldBe(ok200()));


        assertThat("Письмо не найдено во входящих",
                pddAuth,
                withWaitFor(hasMsgIn(subj, folderList.defaultFID())));
    }
}
