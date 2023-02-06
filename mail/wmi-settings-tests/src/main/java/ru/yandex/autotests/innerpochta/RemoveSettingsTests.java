package ru.yandex.autotests.innerpochta;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import wiremock.org.json.JSONException;
import wiremock.org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllParams.getAllParams;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllProfile.getAllProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.Remove.remove;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateOneParamsSetting;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static wiremock.org.skyscreamer.jsonassert.JSONAssert.assertEquals;

@Aqua.Test
@Title("Ручка remove удаления настроек пользователя")
@Description("Ручка remove удаляет настройки по заданному uid пользователя")
@Features("Общее удаление настроек")
@Issue("MAILDEV-536")
public class RemoveSettingsTests {
    private static final String DONE = "Done";
    private static final String REMOVE_NOT_DONE = "Failed to delete settings";
    private static final String PROFILE_SETTINGS_FILE = props().initialSettingsFile();
    private static final String SETTING = "my_god_is";
    private static final String VALUE = "Behemoth";

    private static DefaultHttpClient client =  new DefaultHttpClient();
    private static JSONObject def_params_json;
    private static JSONObject def_profile_json;

    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @BeforeClass
    public static void prepare() throws Exception {
        File file = new File(PROFILE_SETTINGS_FILE);
        String def_settings = FileUtils.readFileToString(file);
        def_params_json = new JSONObject(def_settings).getJSONObject("parameters");
        def_profile_json = new JSONObject(def_settings).getJSONObject("profile");
    }

    @Test
    @Title("Добавляем настройки и проверяем их удаление")
    public void removeForUserWichHaveSettingsHandleShouldReturnOk() throws IOException, JSONException {

        updateOneParamsSetting(accInfo.uid(), SETTING, VALUE)
                .post().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Ответ должен содержать Done", equalTo(DONE));

        getAll(settings(accInfo.uid())).get().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Настройка " + SETTING + " должна иметь значение: " + VALUE,
                        containsString(VALUE));

        remove(settings(accInfo.uid())).post().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Ответ должен содержать Done", equalTo(DONE));


        String parameters = getAllParams(settings(accInfo.uid())).get().via(client).toString();
        JSONObject parameters_json = new JSONObject(parameters).getJSONObject("settings");
        assertEquals(def_params_json, parameters_json, true);

        String profile = getAllProfile(settings(accInfo.uid())).get().via(client).toString();
        JSONObject profile_json = new JSONObject(profile).getJSONObject("settings");
        profile_json.getJSONObject("single_settings").remove("from_name");
        profile_json.getJSONObject("single_settings").remove("default_email");
        assertEquals(def_profile_json, profile_json, true);
    }

    @Test
    @Title("Добавляем настройки и проверяем попытку повторного их удаления")
    public void removeNotExistsSettingsForUserHandleShouldReturn400() throws IOException {
        updateOneParamsSetting(accInfo.uid(), SETTING, VALUE)
                .post().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Ответ должен содержать Done", equalTo(DONE));

        getAll(settings(accInfo.uid())).get().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Настройка " + SETTING + " должна иметь значение: " + VALUE,
                        containsString(VALUE));

        remove(settings(accInfo.uid())).post().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Ответ должен содержать Done", equalTo(DONE));

        remove(settings(accInfo.uid())).post().via(client)
                .statusCodeShouldBe(INTERNAL_SERVER_ERROR_500)
                .assertResponse("Ответ должен содержать Settings were not deleted",
                        containsString(REMOVE_NOT_DONE));
    }
}

