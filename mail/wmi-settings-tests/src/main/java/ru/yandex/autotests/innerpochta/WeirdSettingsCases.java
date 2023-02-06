package ru.yandex.autotests.innerpochta;

import com.google.common.net.HttpHeaders;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.oper.Get;
import ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.autotests.innerpochta.utils.rules.BackupSettingWithApiRule;
import ru.yandex.autotests.innerpochta.utils.rules.BackupSettingWithApiRule.DoNotBackupAny;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsApiObj;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.net.HttpURLConnection;

import wiremock.org.json.JSONObject;

import ru.yandex.autotests.innerpochta.utils.matchers.SettingsJsonValueMatcher;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.eclipse.jetty.http.HttpMethods.POST;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.NegativeCases.ERROR_UID_VALUE_IS_INVALID;
import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.utils.beans.SignBean.serialize;
import static ru.yandex.autotests.innerpochta.utils.beans.SignBean.sign;
import static ru.yandex.autotests.innerpochta.utils.matchers.SignMatchers.hasSigns;
import static ru.yandex.autotests.innerpochta.utils.matchers.SignMatchers.signWithText;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllProfile.getAllProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.GetParams.getParams;
import static ru.yandex.autotests.innerpochta.utils.oper.GetProfile.getProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.GetProfile.signsInProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.*;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateOneProfileSetting;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateProfile;
import static ru.yandex.autotests.innerpochta.utils.rules.BackupSettingWithApiRule.profile;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static ru.yandex.autotests.innerpochta.utils.SettingsUtils.profileCount;
import static ru.yandex.autotests.innerpochta.utils.SettingsUtils.paramsCount;

import static wiremock.org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 19.03.13
 * Time: 16:27
 */
@Aqua.Test
@Title("Особенные тесты на установку")
@Description("Установка по-одному вне границ, установка сразу нескольких значений")
@Features("Общее")
@Stories("Негативные и странные тесты")
public class WeirdSettingsCases {

    public static final String SIGNATURE_SETTING = "signature";
    public static final String SHOW_CHAT_SETTING = "show_chat";
    public static final String ON = "on";
    public static final String OFF = "off";
    public static final String DONE = "Done";
    public static final String NO_PARAMETERS_TO_SET = "No parameters to set";

    public static final String PARAMS_SETTING = "messages_avatars";
    public static final String PROFILE_SETTING = "show_advertisement";

    public static final String JSON_PARSE_ERROR_IN_CALLBACK = "yajl_parse failed: parse error: client cancelled" +
            " parse via callback return value";
    public static final String NO_SUCH_NODE_ERROR = "No such node";
    public static final String REQUEST_ID = "request id:";


    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    public BackupSettingWithApiRule backupSign = profile(accInfo.uid()).backup(SIGNATURE_SETTING);
    public BackupSettingWithApiRule backupFlag = profile(accInfo.uid()).backup(SHOW_CHAT_SETTING);

    @Rule
    public TestRule chain = RuleChain.outerRule(new LogConfigRule())
            .around(backupSign)
            .around(backupFlag);

    private static DefaultHttpClient client() {
        return new DefaultHttpClient();
    }

    @Test
    @Description("[MAILPROTO-1983] Пытаемся установить несуществующую настройку юзера, " +
            "и проверяем что она не выставилась," +
            "\n[DARIA-40444] - меняются параметры в PG когда меняем профиль")
    @Issue("DARIA-40444")
    @DoNotBackupAny
    public void cantSetUnknownProfileSetting() throws IOException {
        String setting = "my_dummy_";
        String wrongValue = "201";
        updateOneProfileSetting(accInfo.uid(), setting, wrongValue)
                .post().via(client()).assertResponse(equalTo(DONE));

        Integer countProfile = profileCount(getProfile(settings(accInfo.uid()).settingsList("setting"))
                .get().via(client()).statusCodeShouldBe(HttpStatus.OK_200).toString());

        assertThat("Неверное количество узлов с парам-настройками", countProfile, equalTo(0));

        Integer countParams = paramsCount(getParams(settings(accInfo.uid()).settingsList(setting))
                .get().via(new DefaultHttpClient()).statusCodeShouldBe(HttpStatus.OK_200).toString());

        assertThat("Неверное количество узлов с парам-настройками", countParams, equalTo(0));
    }

    @Test
    @Title("Подпись не должна меняться при обновлении параметров или профиля")
    @Description("[st/STORAGE-428] Теряли подписи при обновлении профиля или параметров")
    @Issue("STORAGE-428")
    public void shouldNotChangeSignsOnParamsUpdate() throws IOException {
        String text = "some simple sign";
        String email = "abstract-email@ya.ru";
        UpdateProfile.updateSign(accInfo.uid(),
                serialize(sign(text).isDefault(true).associatedEmails(email)))
                .post().via(client()).withDebugPrint().statusCodeShouldBe(OK_200);

        String setting = "my_dummy_setting" + randomAlphanumeric(3);
        String value = random(3);
        updateOneParamsSetting(accInfo.uid(), setting, value)
                .post().via(client())
                .statusCodeShouldBe(HttpStatus.OK_200)
                .assertResponse(equalTo(DONE));

        updateOneProfileSetting(accInfo.uid(), "save_sent", value)
                .post().via(client()).assertResponse(equalTo(DONE));

        assertThat("Список подписей должен содержать подпись с указанным текстом",
                signsInProfile(accInfo.uid()),
                withWaitFor(hasSigns(both((Matcher) hasSize(1))
                        .and(hasItem(signWithText(equalTo(text))))), SECONDS.toMillis(10)));
    }

    @Test
    public void setMultiplySettings() throws Exception {
        shouldUpdateSignAndChat(randomAlphanumeric(10), ON);
        shouldUpdateSignAndChat(randomAlphanumeric(100), OFF);
    }

    @Test
    @DoNotBackupAny
    public void updateNothing() throws Exception {
        String before = getAll(settings(accInfo.uid())).get()
                .via(client()).toString();

        updateProfile(settings(accInfo.uid()))
                .post().via(client())
                .statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(containsString(NO_PARAMETERS_TO_SET));

        updateParams(settings(accInfo.uid()))
                .post().via(client())
                .statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(containsString(NO_PARAMETERS_TO_SET));

        String after = getAll(settings(accInfo.uid())).get()
                .via(client()).toString();

        assertEquals(new JSONObject(after), new JSONObject(before), true);
    }

    @Test
    @Description("[DARIA-38562] - ручка GET с предфильтрацией на сервере обеих таблиц в одном запросе")
    @DoNotBackupAny
    public void shouldReturnBothProfileAndParamsSettingWithGetHandle() throws IOException {
        String settings = Get.get(settings(accInfo.uid())
                .settingsList(PARAMS_SETTING, PROFILE_SETTING))
                .get().via(client())
                .statusCodeShouldBe(HttpStatus.OK_200).toString();

        assertThat("Неверное количество узлов с парам-настройками",
                profileCount(settings), equalTo(1));
        assertThat("Неверное количество узлов с парам-настройками",
                paramsCount(settings), equalTo(1));
    }

    @Test
    @Title("Должны ругнуться на пустые данные")
    @DoNotBackupAny
    public void shouldWarnAboutEmptySign() throws IOException {
        UpdateProfile.updateSign(accInfo.uid(), "")
                .post().via(client())
                .withDebugPrint()
                .statusCodeShouldBe(BAD_REQUEST_400)
                .assertResponse("Должна быть ошибка", containsString("yajl_complete_parse failed"));
    }

    @Test
    @Title("Должны ругнуться на неправильный формат данных")
    @DoNotBackupAny
    public void shouldWarnAboutBadFormatOfSign() throws IOException {
        UpdateProfile.updateSign(accInfo.uid(), "null")
                .post().via(client())
                .withDebugPrint()
                .statusCodeShouldBe(BAD_REQUEST_400)
                .assertResponse("Должна быть ошибка парсинга",
                        containsString(JSON_PARSE_ERROR_IN_CALLBACK));
    }

    @Test
    @Title("Должны ругнуться на неправильный json в данных (пустой ключ)")
    @DoNotBackupAny
    public void shouldWarnAboutBadJsonFormatOfSign() throws IOException {
        UpdateProfile.updateSign(accInfo.uid(), encode("{\"\":\"fs\"}", UTF_8.toString()))
                .post().via(client())
                .withDebugPrint()
                .statusCodeShouldBe(BAD_REQUEST_400)
                .assertResponse("Должна быть ошибка об отсутствии ключа",
                        containsString(NO_SUCH_NODE_ERROR));
    }

    @Test
    @Title("Должны ругнуться на неправильный json в данных (странный ключ)")
    @DoNotBackupAny
    public void shouldWarnAboutNullKeyInJsonSign() throws IOException {
        UpdateProfile.updateSign(accInfo.uid(), encode("{\"˚-˚\":\"fs\"}", UTF_8.toString()))
                .post().via(client())
                .withDebugPrint()
                .statusCodeShouldBe(BAD_REQUEST_400)
                .assertResponse("Должна быть ошибка отсутствия ключа",
                        containsString(NO_SUCH_NODE_ERROR));
    }

    @Test
    @Title("Должны ругнуться на неправильный json в данных json_data")
    @DoNotBackupAny
    public void shouldWarnAboutNullKeyInJsonData() throws IOException {
        UpdateProfile.updateJsonData(accInfo.uid(), encode("{\"\":\"fs\"}", UTF_8.toString()))
                .post().via(client())
                .withDebugPrint()
                .statusCodeShouldBe(BAD_REQUEST_400)
                .assertResponse("Должна быть ошибка об отсутствии параметров",
                        containsString(NO_SUCH_NODE_ERROR));
    }

    @Test
    @Title("Должны вернуть BAD_REQUEST_400 при отсутствии Content-Length заголовка в пост запросе")
    @Issue("DARIA-45268")
    @DoNotBackupAny
    public void shouldReturn400WithoutContentLength() throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) fromUri(props().settingsUri())
                .path("/update_profile").port(80).build().toURL().openConnection();
        urlConnection.setRequestMethod(POST);

        assertThat("Ждем BAD_REQUEST_400 на отсутствие заголовка Content-Length",
                urlConnection.getResponseCode(), is(BAD_REQUEST_400));
    }

    @Test
    @Title("Не должны выставлять параметр когда он начинается с цифры")
    @Issue("DARIA-45268")
    @Description("Параметры должны попадать под регулярку ^[A-Za-z][^=]*$")
    @DoNotBackupAny
    public void shouldReturn400OnParamNonAlphabetic() throws IOException {
        String name = "1";
        updateOneParamsSetting(accInfo.uid(), name, "value").post().via(client())
                .statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(containsString(format("Wrong setting name: \"%s\"", name)));
    }

    @Test
    @Title("Не должны выставлять параметр когда есть «=» в названии")
    @Issue("DARIA-45268")
    @Description("Параметры должны попадать под регулярку ^[A-Za-z][^=]*$")
    @DoNotBackupAny
    public void shouldReturn400OnParamContainsEqSymb() throws IOException {
        updateParamsWithJsonData(accInfo.uid(),
                "{\"single_settings\": {\"p=p\": \"asd\"}}")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post().via(client())
                .statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(containsString("Wrong setting name: \"p=p\""));
    }

    @Test
    @Title("Не должны выставлять параметр без имени")
    @Issue("DARIA-45268")
    @Description("Параметры должны попадать под регулярку ^[A-Za-z][^=]*$")
    @DoNotBackupAny
    public void shouldReturn400OnEmptyNameParam() throws IOException {
        updateParamsWithJsonData(accInfo.uid(),
                "{\"single_settings\": {\"\": \"asd\"}}")
                .post().via(client())
                .statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(containsString("Wrong setting name: \"\""));
    }

    @Test
    @Title("Должны возвращать json по дефолту")
    @Issue("MAILPG-358")
    @DoNotBackupAny
    public void shouldReturnJsonByDefault() throws IOException {
        getAllProfile(new SettingsApiObj().uid(accInfo.uid()))
                .expectHeader(HttpHeaders.CONTENT_TYPE, is("application/json"))
                .get().via(client());
    }


    //TODO кейс на отключение бб и получение списка провалидированных email

    @Test
    @Title("Не должны возвращать 500-ку при пустом uid")
    @Issue("DARIA-53807")
    @Description("update_params: При пустою uid и заданном параметре должны возвращать 400-ку")
    @DoNotBackupAny
    public void shouldReturn400OnEmptyUidAndParam() throws IOException {
        updateParamsWithJsonData("",
                "{\"single_settings\": {\"reply_to\": \"1\"}}")
                .post().via(client())
                .statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse(allOf(containsString(ERROR_UID_VALUE_IS_INVALID),
                        containsString(REQUEST_ID)));

    }

    @Test
    @Title("Не должны возвращать 500-ку при пустом uid")
    @Issue("DARIA-53807")
    @Description("update_profile: При пустою uid и заданном параметре должны возвращать 400-ку")
    @DoNotBackupAny
    public void shouldReturn400OnEmptyUidAndParam1() throws IOException {
        updateOneProfileSetting("", "show_avatars", "on")
                .post().via(client())
                .statusCodeShouldBe(BAD_REQUEST_400)
                .assertResponse(allOf(containsString(ERROR_UID_VALUE_IS_INVALID),
                        containsString(REQUEST_ID)));
    }

    private void shouldUpdateSignAndChat(String sign, String chat) throws IOException {
        updateProfile(settings(accInfo.uid())
                .set(true, SIGNATURE_SETTING, sign)
                .set(true, SHOW_CHAT_SETTING, chat))
                .post().via(client());

        assertThat("Одновременное изменение нескольких настроек должно сработать",
                client(),
                allOf(hasSetting(SIGNATURE_SETTING, sign),
                        hasSetting(SHOW_CHAT_SETTING, chat))
        );
    }

    private SettingsJsonValueMatcher hasSetting(String name, String value) {
        return SettingsJsonValueMatcher.hasSetting(name, value)
                .with(getAll(settings(accInfo.uid())));
    }
}
