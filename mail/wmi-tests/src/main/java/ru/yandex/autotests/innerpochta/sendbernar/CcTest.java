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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;



@Aqua.Test
@Title("Ручка send_message")
@Description("Смотрим, что у адресата корректно отображается CC")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "Mailsendcc")
@Issue("DARIA-51811")
public class CcTest extends BaseSendbernarClass {
    @ClassRule
    public static HttpClientManagerRule pddAuth = auth().with("Adminkapdd");

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(pddAuth).inbox().outbox();

    @Test
    @Issue("DARIA-29751")
    @Title("Должны отправлять письмо с email с русским доменом в cc")
    @Description("Первый раз возникало в DARIA-29751")
    public void shouldSendWithRussianCc() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withCc(pddAuth.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        assertThat("Неверный СС",
                byMid(mid).ccEmail(),
                equalTo(pddAuth.acc().getSelfEmail()));
    }

    @Test
    @Title("СС должен быть пустым когда совпадает с адресатом")
    public void shouldSendWithSelfEmailAsCC() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withCc(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        assertThat("СС должен быть пустым когда совпадает с адресатом",
                byMid(mid).ccEmail(),
                isEmptyString());
    }
}
