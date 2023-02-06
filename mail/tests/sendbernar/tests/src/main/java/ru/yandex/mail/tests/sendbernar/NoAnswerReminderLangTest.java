package ru.yandex.mail.tests.sendbernar;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.properties.Scopes;
import ru.yandex.mail.tests.sendbernar.generated.SendMessageResponse;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.okEmptyBody;
import static ru.yandex.mail.tests.sendbernar.models.NoAnswerRemindProperties.langProps;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLidInFolder;
import static ru.yandex.mail.things.matchers.StringDiffer.notDiffersWith;

@Aqua.Test
@Title("Письма-напоминания о не ответе на письмо")
@Description("Проверяем выдачу ручки no_answer_remind, а также системные письма для разных языков")
@Stories("reminders")
@Issues({@Issue("DARIA-31161"), @Issue("MPROTO-1977"), @Issue("DARIA-51783"), @Issue("DARIA-52304"), @Issue("MAILPG-586")})
@RunWith(Parameterized.class)
public class NoAnswerReminderLangTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.noAnswerLang;
    }

    private UserCredentials reciever = new UserCredentials(Accounts.noAnswerLangTo);

    @Rule
    public CleanMessagesMopsRule cleanReciever = new CleanMessagesMopsRule(reciever).allfolders();

    @Parameterized.Parameter
    public String lang;

    @Parameterized.Parameters(name = "lang-{0}")
    public static Collection<Object[]> data() throws Exception {
        List<Object[]> dataLangs = new ArrayList<Object[]>();
        dataLangs.add(new Object[]{"az"});
        dataLangs.add(new Object[]{"ka"});
        dataLangs.add(new Object[]{"ru"});
        dataLangs.add(new Object[]{"en"});
        dataLangs.add(new Object[]{"be"});
        dataLangs.add(new Object[]{"kk"});
        dataLangs.add(new Object[]{"tt"});
        dataLangs.add(new Object[]{"tr"});
        dataLangs.add(new Object[]{"hy"});
        dataLangs.add(new Object[]{"uk"});
        dataLangs.add(new Object[]{"ro"});
        return dataLangs;
    }

    private String verstka_url() throws Exception {
        Scopes scope = props().scope();

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
                .withTo(reciever.account().email())
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
                        .getContent(subj, date, "To: "+reciever.account().email().toLowerCase(),
                                midLetterInOutbox, verstka_url()))
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));
    }
}

