package ru.yandex.autotests.innerpochta;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.beans.SignBean;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.List;

import static java.net.URLEncoder.encode;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Every.everyItem;
import static ru.yandex.autotests.innerpochta.utils.beans.SignBean.serialize;
import static ru.yandex.autotests.innerpochta.utils.beans.SignBean.sign;
import static ru.yandex.autotests.innerpochta.utils.matchers.SignMatchers.*;
import static ru.yandex.autotests.innerpochta.utils.oper.GetProfile.returnSignsFor;
import static ru.yandex.autotests.innerpochta.utils.oper.GetProfile.signsInProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateSign;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 19.03.13
 * Time: 16:27
 * Задача в стартреке - AUTOTESTPERS-117
 */
@Aqua.Test
@Title("Тесты на множественные подписи")
@Description("Записываем различные множества подписей - [AUTOTESTPERS-117]")
@Features("Множественные подписи")
@Stories("Установка, удаление, кириллица")
@RunWith(DataProviderRunner.class)
public class MultiSignTests {

    public static final String NOT_JSON = "dummy";
    public static final String ERROR_PARSE_FAILED_LEXICAL_ERR = "yajl_parse failed: lexical error: " +
            "invalid char in json text.\n";
    public static final String ERROR_NO_SUCH_NODE = "No such node";
    public static final String RUS_LANG_CODE = "1";
    public static final String UTF8_ENCODING_CODE = "13";
    public static final String REQUEST_ID = "request id:";


    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @Rule
    public TestRule chain = RuleChain.outerRule(new LogConfigRule());

    @Test
    @DataProvider(value = {
            "some simple sign",
            "Здравствуйте,\n я ваша тетя!"
    }, splitBy = "0")
    public void shouldAddOneSign(String text) throws IOException {
        String email = "abstract-email@ya.ru";
        updateSign(accInfo.uid(),
                serialize(sign(text).isDefault(true).associatedEmails(email)))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);

        assertThat("Список подписей должен содержать подпись с указанным текстом",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(hasItem(signWithText(equalTo(text)))), SECONDS.toMillis(10)));
        assertThat("Список подписей должен содержать 1 подпись", signs(), hasSize(1));
        assertThat("Список подписей должен содержать подпись с указанным мылом", signs(),
                hasItem(signWithEmails(everyItem(equalTo(email)))));
    }


    @Test
    public void cyrillicAssocEmails() throws Exception {
        String text = "some simple sign";
        String email = "русский@емейл.рф";
        updateSign(accInfo.uid(),
                serialize(sign(text).isDefault(true).associatedEmails(email)))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);

        assertThat("Список подписей должен содержать подпись с указанным мылом",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(hasItem(signWithEmails(everyItem(equalTo(email)))))));
        assertThat("Список подписей должен содержать 1 подпись", signs(), hasSize(1));

    }


    @Test
    public void shouldAddHtmlSign() throws Exception {
        String text = "<b>Здравствуйте,</b><br />\n <p>я ваша тетя!</p>";
        String email = "abstract-email@ya.ru";
        updateSign(accInfo.uid(),
                serialize(sign(text).isDefault(true).associatedEmails(email)))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);

        assertThat("Список подписей должен содержать подпись с указанным текстом",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(hasItem(signWithText(equalTo(text)))), SECONDS.toMillis(10)));
    }

    @Test
    public void emptyArrayShouldRemoveAllSigns() throws Exception {
        updateSign(accInfo.uid(),
                serialize())
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);

        assertThat("Список подписей не должен содержать ничего после отсылки пустого массива", signs(), hasSize(0));
    }

    @Test
    public void multiSimple() throws Exception {
        String text = randomAlphabetic(10);
        String text2 = randomAlphabetic(10);
        String email = "multisimple-email@ya.ru";
        String email2 = "multisimple-email+bla@ya.ru";
        updateSign(accInfo.uid(),
                serialize(
                        sign(text).isDefault(true).associatedEmails(email),
                        sign(text2).isDefault(false).associatedEmails(email2)
                )
        )
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);

        assertThat("Список подписей должен содержать подписи с указанным текстом",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(containsInAnyOrder(
                        signWithText(equalTo(text)), signWithText(equalTo(text2)))
                ), SECONDS.toMillis(10))
        );
        assertThat("Список подписей должен содержать 2 подписи", signs(), hasSize(2));
        assertThat("Список подписей должен содержать подписи с указанным мылом", signs(),
                containsInAnyOrder(signWithEmails(hasItem(equalTo(email))), signWithEmails(hasItem(equalTo(email2)))));

        assertThat("Список подписей должен содержать 1 подпись с флагом дефолтной и одну без этого флага", signs(),
                containsInAnyOrder(signIsDefault(true), signIsDefault(false)));

    }


    @Test
    public void emptyAssocEmails() throws Exception {
        String text = "";
        updateSign(accInfo.uid(),
                serialize(
                        sign(text).isDefault(true).associatedEmails(),
                        sign(text).isDefault(false)
                )
        )
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);

        assertThat("Список подписей должен содержать подпись с указанным текстом",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(everyItem(signWithText(equalTo(text)))), SECONDS.toMillis(10)));
        assertThat("Список подписей должен содержать 2 подписи", signs(), hasSize(2));
        assertThat("Список подписей должен содержать подпись без мыл", signs(),
                everyItem(signWithEmails(hasSize(0))));
    }

    @Test
    public void duplicatedAssocEmails() throws Exception {
        String text = "";
        String email = "русский@емейл.рф";
        updateSign(accInfo.uid(),
                serialize(sign(text).isDefault(true).associatedEmails(email, email)))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);

        assertThat("Список подписей должен содержать подпись с указанным мылом",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(hasItem(signWithEmails(everyItem(equalTo(email))))), SECONDS.toMillis(10)));
        assertThat("Список подписей должен содержать подпись без мыл", signs(),
                everyItem(signWithEmails(hasSize(2))));

    }

    @Test
    public void shouldSeeTextTraits() throws Exception {
        String text = "Подпись на русском языке";
        String email = "email@ma.ru";
        updateSign(accInfo.uid(),
                serialize(sign(text).isDefault(true).associatedEmails(email)))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);
        assertThat("Список подписей должен содержать подпись с кодом кодировки " +
                        "UTF8(13) и русским языком (1) в поле text_traits",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(hasItems(
                        signWithTextTraitsCode(equalTo(UTF8_ENCODING_CODE)),
                        signWithTextTraitsLang(equalTo(RUS_LANG_CODE)))), SECONDS.toMillis(10))
        );
    }

    @Test
    public void noDefaultReturns400() throws Exception {
        updateSign(accInfo.uid(),
                serialize(sign("").associatedEmails()))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(allOf(startsWith(ERROR_NO_SUCH_NODE),
                        containsString(REQUEST_ID)));
    }

    @Test
    public void notValidIsDefaultReturns400() throws Exception {
        updateSign(accInfo.uid(),
                encode(sign("").isDefault(true).associatedEmails().toString().replace("true", "bla")))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(allOf(startsWith(ERROR_PARSE_FAILED_LEXICAL_ERR),
                        containsString(REQUEST_ID)));
    }

    @Test
    public void noTextReturns400() throws Exception {
        updateSign(accInfo.uid(),
                serialize(new SignBean().associatedEmails()))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(allOf(startsWith(ERROR_NO_SUCH_NODE),
                        containsString(REQUEST_ID)));
    }

    @Test
    public void notJSONShouldReturns400() throws Exception {
        updateSign(accInfo.uid(), NOT_JSON)
                .post().via(new DefaultHttpClient())
                .statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(allOf(startsWith(ERROR_PARSE_FAILED_LEXICAL_ERR),
                        containsString(REQUEST_ID)));

    }

    @Test
    public void notEncodedSignReturns400() throws Exception {
        updateSign(accInfo.uid(),
                sign("").isDefault(true).associatedEmails().toString())
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(allOf(startsWith(ERROR_NO_SUCH_NODE),
                        containsString(REQUEST_ID)));
    }

    @Test
    public void notArrayAsAssocEmailsReturns400() throws Exception {
        updateSign(accInfo.uid(),
                encode(sign("").isDefault(true).associatedEmails().toString().replace("[]", "\"bla\"")))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(allOf(startsWith(ERROR_NO_SUCH_NODE),
                        containsString(REQUEST_ID)));
    }

    private List<SignBean> signs() throws IOException {
        return returnSignsFor(accInfo.uid());
    }

    @Test
    public void shouldIsNoSanitize() throws IOException {
        updateSign(accInfo.uid(),
                serialize(sign("text").isDefault(true)))
                .post().via(new DefaultHttpClient()).withDebugPrint().statusCodeShouldBe(OK_200);

        assertThat("Список подписей должен содержать подпись с указанным текстом",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(hasItem(signWithText(equalTo("text")))), SECONDS.toMillis(10)));
        assertThat("Список подписей должен содержать не отсанитайженные подписи", signs(),
                everyItem(signIsSanitize(false)));
    }

}
