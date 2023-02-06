package ru.yandex.autotests.innerpochta;

import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.oper.GetProfile;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cthul.matchers.CthulMatchers.both;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateOneProfileSetting;
import static ru.yandex.autotests.innerpochta.utils.matchers.SettingsJsonValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static ru.yandex.autotests.innerpochta.utils.SettingsUtils.getSettingValue;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 19.03.13
 * Time: 16:27
 */
@Aqua.Test
@Title("Общие негативные тесты на установку настроек профиля")
@Description("Установка по-одному")
@RunWith(Parameterized.class)
@Features("Изменение профиля")
@Stories("Неизменение профиля")
public class ProfileNotChangingSettings {

    public static final String ON = "on";
    public static final String OFF = "off";
    public static final Matcher<String> DONE = equalTo("Done");

    public static final String GIVEN_DEF_EMAIL_NOT_VALIDATED_ERROR = "Given default email was not validated";
    public static final String REQUEST_ID = "request id:";


    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @Parameterized.Parameters(name = "{index}-{0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]
                {
                        //readonly
                        new Object[]{"no_mailbox_selection", ON, OFF, DONE},
                        new Object[]{"no_news", ON, OFF, DONE},
                        new Object[]{"no_advertisement", ON, OFF, DONE},
                        new Object[]{"no_firstline", ON, OFF, DONE},
                        new Object[]{"disable_social_notification", ON, OFF, DONE},
                        //flag
                        new Object[]{"show_unread", "aaa", "bbb", DONE},
                        //int
                        new Object[]{"subs_messages_per_page", "0", "-1", DONE},
                        new Object[]{"messages_per_page", "0", "-1", DONE},
                        new Object[]{"messages_per_page", "220", "201", DONE},
                        //char
                        new Object[]{"quotation_char", "Ф", "к", DONE},
                        //select
                        new Object[]{"skin_name", "grr", "bazinga!", DONE},
                        new Object[]{"page_after_send", "done11", "current_list11", DONE},
                        //other
                        new Object[]{"default_email", "done11", "lala@lala.ru",
                                allOf(startsWith(GIVEN_DEF_EMAIL_NOT_VALIDATED_ERROR), containsString(REQUEST_ID))},
                        new Object[]{"default_email", "null@yandex.ru", "my.ru",
                                allOf(startsWith(GIVEN_DEF_EMAIL_NOT_VALIDATED_ERROR), containsString(REQUEST_ID))},
                        new Object[]{"mail_limit", randomAlphanumeric(50), randomAlphanumeric(50),
                                both(not(equalTo(""))).and(notNullValue(String.class))},
                };
        return Arrays.asList(data);
    }

    private String settingName;
    private String firstChange;
    private String secondChange;
    private Matcher<String> result;


    public ProfileNotChangingSettings(String settingName, String firstChange,
                                      String secondChange, Matcher<String> result) {
        this.settingName = settingName;
        this.firstChange = firstChange;
        this.secondChange = secondChange;
        this.result = result;
    }


    @Test
    public void profileShouldNotChange() throws IOException {
        shouldSeeChangeAfterUpdate(firstChange);
        shouldSeeChangeAfterUpdate(secondChange);
    }

    private void shouldSeeChangeAfterUpdate(String changeTo) throws IOException {
        GetProfile oper = GetProfile.getProfile(settings(accInfo.uid())
                .askValidator().settingsList(settingName));

        String current = getSettingValue(oper.get().via(new DefaultHttpClient())
                .statusCodeShouldBe(HttpStatus.OK_200).toString(), settingName);

        updateOneProfileSetting(accInfo.uid(), settingName, changeTo)
                .post().via(new DefaultHttpClient()).assertResponse(result);

        assertThat(new DefaultHttpClient(), hasSetting(settingName, current).
                with(oper));
    }
}
