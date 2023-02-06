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
import java.util.ArrayList;
import java.util.List;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllParams.getAllParams;
import static ru.yandex.autotests.innerpochta.utils.oper.DeleteParams.deleteParamsWithSettingsList;
import static ru.yandex.autotests.innerpochta.utils.oper.Remove.remove;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateOneParamsSetting;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static wiremock.org.skyscreamer.jsonassert.JSONAssert.assertEquals;

@Aqua.Test
@Title("Ручка delete_params удаления параметров пользователя")
@Description("Ручка delete_params удаляет параметры по заданному uid пользователя и списку имен параметров")
@Features("Удаление параметров")
@Issue("MAILPG-2766")
public class DeleteParamsTests {
    private static final String DONE = "Done";
    private static final String PROFILE_SETTINGS_FILE = props().initialSettingsFile();
    private static final String SETTING = "my_god_is";
    private static final String NOT_EXISTS_SETTING = "tutu";
    private static final String VALUE = "Behemoth";

    public static DefaultHttpClient client =  new DefaultHttpClient();
    private static JSONObject def_settings_json;

    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @BeforeClass
    public static void prepare() throws Exception {
        File file = new File(PROFILE_SETTINGS_FILE);
        String def_settings = FileUtils.readFileToString(file);
        def_settings_json = new JSONObject(def_settings);
    }

    private static List<String> toList(String... settings) {
        List<String> ret = new ArrayList<>();
        for (String setting : settings) {
            ret.add(setting);
        }
        return ret;
    }

    private static String getProfileSettingsName() throws JSONException {
        return (String) def_settings_json
                .getJSONObject("profile")
                .getJSONObject("single_settings")
                .keys().next();
    }

    @Test
    @Title("При удачном удалении параметров должен вернуться код 200")
    public void deleteParamsHandleShouldReturnOk() throws IOException, JSONException {

        remove(settings(accInfo.uid())).post().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Ответ должен содержать Done", equalTo(DONE));

        updateOneParamsSetting(accInfo.uid(), SETTING, VALUE)
                .post().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Ответ должен содержать Done", equalTo(DONE));

        getAll(settings(accInfo.uid())).get().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Настройка " + SETTING + " должна иметь значение: " + VALUE,
                        containsString(VALUE));

        deleteParamsWithSettingsList(accInfo.uid(), toList(SETTING, NOT_EXISTS_SETTING))
                .post().via(client)
                .statusCodeShouldBe(OK_200)
                .assertResponse("Ответ должен содержать Done", equalTo(DONE));

        String settings = getAllParams(settings(accInfo.uid())).get().via(client).toString();

        assertEquals(def_settings_json.getJSONObject("parameters"),
                new JSONObject(settings).getJSONObject("settings"), true);
    }

    @Test
    @Title("При удалении профиля должен вернуться код 400")
    public void deleteProfileHandleShouldReturnOk() throws IOException, JSONException {

        deleteParamsWithSettingsList(accInfo.uid(), toList(getProfileSettingsName(), NOT_EXISTS_SETTING))
                .post().via(client)
                .statusCodeShouldBe(BAD_REQUEST_400)
                .assertResponse("Ответ должен содержать Invalid parameter и request id",
                        allOf(containsString("request id"), containsString("invalid argument"))
                );

        String settings = getAllParams(settings(accInfo.uid())).get().via(client).toString();

        assertEquals(def_settings_json.getJSONObject("parameters"),
                new JSONObject(settings).getJSONObject("settings"), true);
    }
}
