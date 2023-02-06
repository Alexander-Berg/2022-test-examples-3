package ru.yandex.autotests.innerpochta;

import org.apache.http.impl.client.DefaultHttpClient;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.SettingsProperties;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Headers;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.autotests.testpers.ssh.SSHAuthRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.StringUtils.leftPad;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.beans.tskv.SettingsTskv.*;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateOneParamsSetting;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateOneProfileSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;

/**
 * User: lanwen
 * Date: 28.11.14
 * Time: 15:40
 */
@Aqua.Test
@Title("TSKV лог - обязательные поля и значения")
@Description("[DARIA-45268] - добавлен tskv лог лежащий /app/log/user_journal.tskv")
@Features("Служебное")
@Stories("Логирование")
@Issues({@Issue("DARIA-45268")})
public class TskvTest {
    public static final String TSKV_LOG_PATH = "/app/log/user_journal.tskv";
    public static final String SETTINGS_URI_PREFIX = "app.";

    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(
            SETTINGS_URI_PREFIX.concat(SettingsProperties.props().settingsUri().getHost()),
            WmiCoreProperties.props().getRobotGerritWebmailTeamSshKey()
    ).withLogin("root");

    @Rule
    public LogConfigRule logs = new LogConfigRule();

    private String setName;
    private String newValue = randomAlphanumeric(15);
    String requestId = randomAlphanumeric(15);
    private String tableName = "users_history";

    @Test
    @Issue("MAILDEV-2019")
    @Title("При изменении значения настройки на недопустимое должно вернутся дефолтное значение")
    public void shouldReturnAllDefaultValueForBadUpdateProfileSetting() throws IOException, InterruptedException {
        updateOneProfileSetting(accInfo.uid(), "messages_per_page", "500")
                .header(Headers.REQUEST_ID, requestId)
                .post().via(client());
        shouldSeeSettingsChangesInLog("messages_per_page", "30");
    }

    @Test
    @Title("Должны вернуть все обязательные поля в логе при изменении параметра")
    public void shouldReturnAllRequiredFieldsAfterParamChanging() throws IOException, InterruptedException {
        setName = "someparam";
        updateOneParamsSetting(accInfo.uid(), setName, newValue)
                .header(Headers.REQUEST_ID, requestId)
                .post().via(client());
        shouldSeeSettingsChangesInLog(setName, newValue);
    }

    @Test
    @Title("Должны вернуть специальные поля в логе при изменении параметра с дополнительными хедерами")
    public void shouldReturnCustomFieldsAfterParamChangingWithExtraHeaders() throws IOException,
            InterruptedException {
        setName = "someparam";

        String expBoxes = randomAlphanumeric(15);
        String enableExpBoxes = randomAlphanumeric(15);
        String clientVersion = randomAlphanumeric(15);

        updateOneParamsSetting(accInfo.uid(), setName, newValue)
                .header(Headers.ENABLED_EXP_BOXES, enableExpBoxes)
                .header(Headers.EXP_BOXES, expBoxes)
                .header(Headers.CLIENT_VERSION, clientVersion)
                .header(Headers.REQUEST_ID, requestId)
                .post().via(client());
        shouldSeeExtraFields(expBoxes, enableExpBoxes, clientVersion, requestId);
    }


    @Test
    @Title("Должны вернуть все обязательные поля в логе при изменении профиля")
    public void shouldReturnAllRequiredFieldsAfterProfileChanging() throws IOException, InterruptedException {
        setName = "from_name_eng";
        updateOneProfileSetting(accInfo.uid(), setName, newValue)
                .header(Headers.REQUEST_ID, requestId)
                .post().via(client());
        shouldSeeSettingsChangesInLog(setName, newValue);
    }

    @Test
    @Title("Должны вернуть специальные поля в логе при изменении профиля с дополнительными хедерами")
    public void shouldReturnCustomFieldsAfterProfileChangingWithExtraHeaders() throws IOException, InterruptedException {
        setName = "from_name_eng";

        String expBoxes = randomAlphanumeric(15);
        String enableExpBoxes = randomAlphanumeric(15);
        String clientVersion = randomAlphanumeric(15);

        updateOneProfileSetting(accInfo.uid(), setName, newValue)
                .header(Headers.ENABLED_EXP_BOXES, enableExpBoxes)
                .header(Headers.EXP_BOXES, expBoxes)
                .header(Headers.CLIENT_VERSION, clientVersion)
                .header(Headers.REQUEST_ID, requestId)
                .post().via(client());
        shouldSeeExtraFields(expBoxes, enableExpBoxes, clientVersion, requestId);
    }

    @Step("Должны быть дополнительные поля в tskv логе")
    private void shouldSeeExtraFields(String expBoxesValue, String enableExpBoxesValue, String clientVersion,
                                      String requestId) {
        assertThat(sshAuthRule.ssh().conn(), should(logEntryShouldMatch(asList(
                        hasEntry(TEST_BUCKETS.toString(),expBoxesValue),
                        hasEntry(ENABLED_TEST_BUCKETS.toString(), enableExpBoxesValue),
                        hasEntry(CLIENT_VERSION.toString(), clientVersion),
                        hasEntry(REQUEST_ID.toString(), requestId)), TSKV_LOG_PATH, newValue))
                .whileWaitingUntil(timeoutHasExpired(10000).withPollingInterval(500)));
    }

    @Step("Должны быть изменения в логе для {0}")
    private void shouldSeeSettingsChangesInLog(String name, String value) throws IOException, InterruptedException {
        List<Matcher<Map<? extends String, ? extends String>>> logMatchers = Arrays.asList(
                hasEntry(TABLE_NAME.toString(), tableName),
                hasEntry(TARGET.toString(), "settings"),
                hasEntry(OPERATION.toString(), "settings_update"),
                hasEntry(HIDDEN.toString(), "1"),
                hasEntry(MODULE.toString(), "settings"),
                hasEntry(AFFECTED.toString(), "1"),
                hasEntry(UID.toString(), leftPad(accInfo.uid(), 20, "0")),
                hasEntry(STATE.toString(), name + "=" + value),
                hasEntry(TSKV_FORMAT.toString(), "mail-user-journal-tskv-log"),
                hasEntry(containsString(UNIXTIME.toString()), not(isEmptyOrNullString()))
                );
        assertThat(sshAuthRule.ssh().conn(), should(logEntryShouldMatch(logMatchers, TSKV_LOG_PATH, requestId))
                .whileWaitingUntil(timeoutHasExpired(10000).withPollingInterval(500)));
    }

    private DefaultHttpClient client() {
        return new DefaultHttpClient();
    }
}
