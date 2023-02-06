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
import ru.yandex.mail.common.properties.Scopes;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;

@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляем письма с различными цифровыми логинами")
@Stories("mail send")
@RunWith(DataProviderRunner.class)
public class DigitLoginTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.digitLogin;
    }

    private UserCredentials digitLoginTo = new UserCredentials(Accounts.digitLoginTo);

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(digitLoginTo).inbox().outbox();

    @DataProvider
    public static Object[][] phones() throws Exception {
        Scopes scope = props().scope();

        if (scope == Scopes.PRODUCTION) {
            return new Object[][] {
                    { "79213157505@yandex.ru"  },
                    { "+79213157505@yandex.ru" },
                    { "89213157505@yandex.ru"  },
                    { "+89213157505@yandex.ru" },
                    { "++79213157505@yandex.ru"}
            };
        } else if (scope == Scopes.TESTING) {
            return new Object[][] {
                    { "79999867295@yandex.ru"  },
                    { "+79999867295@yandex.ru" },
                    { "89999867295@yandex.ru"  },
                    { "+89999867295@yandex.ru" },
                    { "++79999867295@yandex.ru"}
            };
        } else {
            throw new Exception("there is no account for scope "+scope.toString());
        }
    }

    @Test
    @Description("Отсылаем сообщения к пользователю с различными цифровыми логинами\n" +
            "Проверяем, что пришедшее письмо соответствует отправленному.")
    @UseDataProvider("phones")
    public void shouldSendMailToDigitLogin(String to) {
        String text = Random.string();
        sendMessage()
                .withTo(to)
                .withSubj(subj)
                .withText(text)
                .post(shouldBe(ok200()));


        String mid = waitWith.usingHttpClient(digitLoginTo).subj(subj).waitDeliver().getMid();
        Message message = byMid(mid, digitLoginTo);


        assertThat("<To> в пришедшем письме отображается неправильно ",
                message.toEmail(),
                is(to));
        assertThat("<From> в пришедшем письме отображается неправильно ",
                message.fromEmail(),
                is(authClient.account().email()));
        assertThat("Отправленный текст изменился ",
                message.firstline(),
                is(text));
    }
}
