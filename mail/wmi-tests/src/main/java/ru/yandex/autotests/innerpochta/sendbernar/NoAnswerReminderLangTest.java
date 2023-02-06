package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendMessageResponse;
import ru.yandex.autotests.innerpochta.data.Langs;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.NoAnswerRemindProperties.langProps;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StringDiffer.notDiffersWith;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.okEmptyBody;


@Aqua.Test
@Title("Письма-напоминания о не ответе на письмо")
@Description("Проверяем выдачу ручки no_answer_remind, а также системные письма для разных языков")
@Credentials(loginGroup = "RemindMe")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.REMINDER)
@Issues({@Issue("DARIA-31161"), @Issue("MPROTO-1977"), @Issue("DARIA-51783"), @Issue("DARIA-52304"), @Issue("MAILPG-586")})
@RunWith(Parameterized.class)
public class NoAnswerReminderLangTest extends BaseSendbernarClass {
    @ClassRule
    public static HttpClientManagerRule reciever = auth().with("RemindReciever").login();

    @Rule
    public CleanMessagesMopsRule cleanReciever = new CleanMessagesMopsRule(reciever).allfolders();

    @Parameterized.Parameter
    public String lang;

    @Parameterized.Parameters(name = "lang-{0}")
    public static Collection<Object[]> data() throws Exception {
        return Langs.languages();
    }

    String verstka_url() throws Exception {
        Scopes scope = props().testingScope();

        if (scope == Scopes.PRODUCTION) {
            return "https://mail.yandex.ru";
        } else if (scope == Scopes.TESTING) {
            return "https://localhost";
        } else {
            throw new Exception("there is no account for scope "+scope.toString());
        }
    }

    @Test
    @Title("Должны получать письмо-напоминание о неотвеченных письмах для всех языков и сравниваем их с шаблоном")
    public void shouldBeEquals() throws Exception {
        String lid = lidByName(noAnswerReminderLabelName);
        String date = "6.06.1999";


        String messageId = sendMessage()
                .withLids(lid)
                .withTo(reciever.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();


        String midLetterInOutbox = waitWith.subj(subj).sent().waitDeliver().getMid();


        assertThat(authClient, hasMsgWithLidInFolder(midLetterInOutbox, folderList.sentFID(), lid));


        noAnswerRemind()
                .withLang(lang)
                .withDate(date)
                .withMessageId(messageId)
                .post(shouldBe(okEmptyBody()));


        String midRemindLetter = waitWith.subj(subj).waitDeliver().getMid();


        assertThat(authClient,
                not(hasMsgWithLidInFolder(midLetterInOutbox, folderList.sentFID(), lid)));


        Message ms = byMid(midRemindLetter);


        assertThat("Нет письма напоминания",
                ms.noReplyNotification(),
                equalTo(midLetterInOutbox));


        assertThat("<From> напоминания не совпадает с ожидаемым",
                ms.fromEmail(),
                equalTo("noreply@yandex.ru"));
        assertThat("Тело письма не совпадает с шаблоном", ms.text(),
                notDiffersWith(langProps(lang)
                        .getContent(subj, date, "To: "+reciever.acc().getSelfEmail().toLowerCase(),
                                midLetterInOutbox, verstka_url()))
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));
    }
}
