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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.innerpochta.utils.oper.Get.get;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllParams.getAllParams;
import static ru.yandex.autotests.innerpochta.utils.oper.GetParams.getParams;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateOneParamsSetting;
import static ru.yandex.autotests.innerpochta.utils.matchers.SettingsJsonValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 19.03.13
 * Time: 16:27
 */
@Aqua.Test
@Title("Общие тесты на установку пользовательских параметров")
@Description("Установка по-одному")
@RunWith(Parameterized.class)
@Features("Изменение параметров")
@Stories("Изменение параметров в обе стороны")
public class ParamsChangingSettings {

    public static final String ON = "on";
    public static final String OFF = "off";
    public static final String DONE = "Done";


    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @Parameterized.Parameters(name = "{index}-{0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]
                {
                        new Object[]{"reply_to", "1", "0"},
                        new Object[]{"first_login_my_dummy", ON, OFF},
                        new Object[]{"first_login_my_dummy", ON, ""},
                        new Object[]{"lang_promo", "1", "2"},
                        new Object[]{"last_web_ip_dt", "37.140.169.2|171362399796", "37.141.169.2|171362399796"},
                        new Object[]{"no_bears_bubble", ON, OFF},
                        new Object[]{"no_collectors_bubble", ON, OFF},
                        new Object[]{"no_translator_bubble", ON, OFF},
                        new Object[]{"no_zverushki_bubble", ON, OFF},
                        new Object[]{"phone_confirm", ON, OFF},
                        new Object[]{"phone_confirm_date", "1362399796", "1362399793"},
                        new Object[]{"reply_promo_cnt", "1", "2"},
                        new Object[]{"rph", "mozilla=0&opera=1&webkit=0", "mozilla=1&opera=0&webkit=1"},
                        new Object[]{"someparam", "somevalue1", "somevalue2"},
                        new Object[]{"todo_promo", ON, OFF},
                        new Object[]{"todo_promo_onsent", ON, OFF},
                        new Object[]{"zver_share_faded", "yes", "no"},
                        new Object[]{"localize_imap", ON, OFF},
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
        updateOneParamsSetting(accInfo.uid(), settingName, encode(changeTo, UTF_8.toString()))
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

    private DefaultHttpClient client() {
        return new DefaultHttpClient();
    }
}
