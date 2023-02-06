package ru.yandex.autotests.innerpochta.sendbernar;


import java.util.Properties;
import java.util.function.Predicate;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;


@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляем письма с различными цифровыми логинами")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "DigitLogin")
@RunWith(DataProviderRunner.class)
public class DigitLoginTest extends BaseSendbernarClass {

    @ClassRule
    public static HttpClientManagerRule authClient2 = auth().with("DigitLoginTo");

    @ClassRule
    public static CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient2).allfolders();

    @DataProvider
    public static Object[][] phones() throws Exception {
        Scopes scope = props().testingScope();

        Predicate<Properties> isBoradorTesting = (properties) -> properties
                .getProperty("mailboxes.yaml.file", "").equals("accounts-web-test-monitoring.yaml");

        if (scope == Scopes.PRODUCTION) {
            return new Object[][] {
                    { "79999867295@yandex.ru"  },
                    { "+79999867295@yandex.ru" },
                    { "89999867295@yandex.ru"  },
                    { "+89999867295@yandex.ru" },
                    { "++79999867295@yandex.ru"}
            };
        } else if (isBoradorTesting.test(System.getProperties())) {
            return new Object[][] {
                    { "79653944057@yandex.ru"  },
                    { "+79653944057@yandex.ru" },
                    { "89653944057@yandex.ru"  },
                    { "+79653944057@yandex.ru" },
                    { "++79653944057@yandex.ru"}
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
        String text = getRandomString();
        sendMessage()
                .withTo(to)
                .withSubj(subj)
                .withText(text)
                .post(shouldBe(ok200()));


        String mid = waitWith.usingHttpClient(authClient2).subj(subj).waitDeliver().getMid();
        Message message = byMid(mid, authClient2);


        assertThat("<To> в пришедшем письме отображается неправильно ",
                message.toEmail(),
                is(to));
        assertThat("<From> в пришедшем письме отображается неправильно ",
                message.fromEmail(),
                is(authClient.acc().getSelfEmail()));
        assertThat("Отправленный текст изменился ",
                message.firstline(),
                is(text));
    }

}
