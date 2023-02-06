package ru.yandex.autotests.innerpochta;

import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.autotests.innerpochta.utils.rules.BackupSettingWithApiRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.innerpochta.utils.oper.Get.get;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllParams.getAllParams;
import static ru.yandex.autotests.innerpochta.utils.oper.GetParams.getParams;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateOneParamsSetting;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProtectedParams.updateOneProtectedParamsSetting;
import static ru.yandex.autotests.innerpochta.utils.matchers.SettingsJsonValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;

@Aqua.Test
@Title("Общие тесты на установку защищенных пользовательских параметров")
@Description("Установка по-одному")
@RunWith(Parameterized.class)
@Features("Изменение защищенных параметров")
@Stories("Изменение защищенных параметров в обе стороны")
public class ProtectedParamsChangingSettings {

    public static final String ON = "on";
    public static final String OFF = "off";
    public static final String DONE = "Done";


    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @Parameterized.Parameters(name = "{index}-{0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]
                {
                        new Object[]{"has_pro_interface", ON, OFF},
                        new Object[]{"has_priority_support", ON, OFF},
                        new Object[]{"is_ad_disabled_via_billing", ON, OFF},
                        new Object[]{"is_user_b2b_mail", ON, OFF},
                        new Object[]{"is_user_b2b_disk", ON, OFF},
                        new Object[]{"priority_mail", ON, OFF},
                };
        return Arrays.asList(data);
    }

    @Parameterized.Parameter(0)
    public String settingName;
    @Parameterized.Parameter(1)
    public String firstChange;
    @Parameterized.Parameter(2)
    public String secondChange;


    public BackupSettingWithApiRule backup = BackupSettingWithApiRule.params(accInfo.uid());

    @Rule
    public TestRule chain = new LogConfigRule().around(new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            backup.backup(settingName);
        }
    }).around(backup);

    @Test
    public void shouldShowChanging() throws IOException {
        shouldSeeChangeAfterUpdateWith(firstChange);
        shouldSeeChangeAfterUpdateWith(secondChange);
    }

    private void shouldSeeChangeAfterUpdateWith(String changeTo) throws IOException {
        updateOneProtectedParamsSetting(accInfo.uid(), settingName, encode(changeTo, UTF_8.toString()))
                .post().via(client())
                .statusCodeShouldBe(HttpStatus.OK_200)
                .assertResponse(equalTo(DONE));


        assertThat(client(), hasSetting(settingName, is(changeTo)).
                with(getParams(settings(accInfo.uid()).settingsList(settingName))));

        assertThat(client(), hasSetting(settingName, is(changeTo)).
                with(get(settings(accInfo.uid()).settingsList(settingName))));

        assertThat(client(), hasSetting(settingName, is(changeTo)).
                with(getAll(settings(accInfo.uid()).askValidator())));

        assertThat(client(), hasSetting(settingName, is(changeTo)).
                with(getAllParams(settings(accInfo.uid()).askValidator())));
    }

    @Test
    public void shouldGetBadRequestWhenUpdatingProtectedSettingByUpdatedParamsPath () throws IOException {
        updateOneParamsSetting(accInfo.uid(), settingName, ON)
                .post().via(client())
                .statusCodeShouldBe(HttpStatus.BAD_REQUEST_400)
                .assertResponse("Ответ должен содержать Invalid parameter и request id",
                    allOf(containsString("request id"), containsString("invalid argument"))
        );
    }

    private DefaultHttpClient client() {
        return new DefaultHttpClient();
    }
}
