package ru.yandex.autotests.innerpochta;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.events.TestCaseEvent;
import ru.yandex.qatools.allure.model.TestCaseResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.NegativeCases.*;
import static ru.yandex.autotests.innerpochta.utils.oper.Get.get;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllParams.getAllParams;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllProfile.getAllProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.GetParams.getParams;
import static ru.yandex.autotests.innerpochta.utils.oper.GetProfile.getProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.Remove.remove;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StatusCodeMatcher.hasStatusCode;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StringResponseOperMatcher.hasStringContent;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;

/**
 * Created with IntelliJ IDEA.
 * User: angrybird
 * Date: 9.12.15
 * Time: 16:27
 */
@Aqua.Test
@Title("Ручки get для юзера без данных в базе")
@Description("Ручки get должны возвращать дефолтные настройки, если пользователя нет в базе")
@RunWith(Parameterized.class)
@Features("Общее")
@Stories("Чтение настроек")
public class GetQueriesForUserNotInBase {

    private final Logger logger = LogManager.getLogger(this.getClass());
    public static final String DEF_SKIN_NAME = "neo2";
    public static final String DEF_MESSAGES_AVATARS = "on";
    public static final String UID_NOT_EXISTING = "2";
    public static final String skinNameParam = "skin_name";
    public static final String messagesAvatarsParam = "messages_avatars";
    @Rule
    public TestRule chain = RuleChain.outerRule(new LogConfigRule());
    @Parameterized.Parameter(0)
    public Oper oper;
    @Parameterized.Parameter(1)
    public Matcher<Oper> matcher;
    @Parameterized.Parameter(2)
    public String comment;
    @Rule
    public TestWatcher allureTitleSetRule = new TestWatcher() {
        @Override
        protected void starting(org.junit.runner.Description description) {
            Allure.LIFECYCLE.fire(new TestCaseEvent() {
                @Override
                public void process(TestCaseResult testCaseResult) {
                    testCaseResult.withTitle(comment);
                    logger.warn(comment);
                }
            });
        }
    };

    @ClassRule
    public static AccountRule accInfo = new AccountRule().with(GetQueriesForUserNotInBase.class);

    @Parameterized.Parameters(name = "{index}-{2}")
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();

        remove(settings(accInfo.uid())).post().via(new DefaultHttpClient());

        data.add(new Object[]{
                get(settings(accInfo.uid()).settingsList(skinNameParam)),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEF_SKIN_NAME),
                                not(containsString(REQUEST_ID))))),
                "get: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                get(settings(UID_NOT_EXISTING).settingsList(skinNameParam)),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(ERROR_USER_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                "get: отдает 404 user not found"
        });

        data.add(new Object[]{
                get(settings(accInfo.uid()).settingsList(DEFAULT_EMAIL).askValidator()),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEFAULT_EMAIL),
                                not(containsString(REQUEST_ID))))),
                "get ask_val: дефолтное значение из профиля"
        });

        data.add(new Object[]{
                get(settings(UID_NOT_EXISTING).settingsList(DEFAULT_EMAIL).askValidator()),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(REQUEST_ID),
                                containsString(ERROR_USER_NOT_FOUND)))),
                "get ask_val: отдает 404 user not found"
        });

        data.add(new Object[]{
                getAll(settings(accInfo.uid())),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEF_SKIN_NAME),
                                not(containsString(REQUEST_ID))))),
                "get_all: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                getAll(settings(UID_NOT_EXISTING)),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(ERROR_USER_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                "get_all: отдает 404 user not found"
        });

        data.add(new Object[]{
                getAll(settings(accInfo.uid()).askValidator()),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEF_SKIN_NAME),
                                not(containsString(REQUEST_ID))))),
                "get_all ask_val: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                getAll(settings(UID_NOT_EXISTING).askValidator()),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(REQUEST_ID),
                                containsString(ERROR_USER_NOT_FOUND)))),
                "get_all ask_val: отдает 404 user not found"
        });

        data.add(new Object[]{
                getAllProfile(settings(accInfo.uid())),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEF_SKIN_NAME),
                                not(containsString(REQUEST_ID))))),
                "get_all_profile: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                getAllProfile(settings(UID_NOT_EXISTING)),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(ERROR_USER_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                "get_all_profile: отдает 404 user not found"
        });

        data.add(new Object[]{
                getAllProfile(settings(accInfo.uid()).askValidator()),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(not(containsString(REQUEST_ID)),
                                containsString(DEF_SKIN_NAME)))),
                "get_all_profile ask_val: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                getAllProfile(settings(UID_NOT_EXISTING).askValidator()),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(REQUEST_ID),
                                containsString(ERROR_USER_NOT_FOUND)))),
                "get_all_profile ask_val: отдает 404 user not found"
        });

        data.add(new Object[]{
                getProfile(settings(accInfo.uid()).settingsList(skinNameParam)),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEF_SKIN_NAME),
                                not(containsString(REQUEST_ID))))),
                "get_profile: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                getProfile(settings(UID_NOT_EXISTING).settingsList(skinNameParam)),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(ERROR_USER_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                "get_profile: отдает 404 user not found"
        });

        data.add(new Object[]{
                getProfile(settings(accInfo.uid()).settingsList(skinNameParam).askValidator()),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEF_SKIN_NAME),
                                not(containsString(REQUEST_ID))))),
                "get_profile ask_val: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                getProfile(settings(UID_NOT_EXISTING).settingsList(DEFAULT_EMAIL).askValidator()),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(REQUEST_ID),
                                containsString(ERROR_USER_NOT_FOUND)))),
                "get_profile ask_val: отдает 404 user not found"
        });

        data.add(new Object[]{
                getAllParams(settings(accInfo.uid())),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEF_MESSAGES_AVATARS),
                                not(containsString(REQUEST_ID))))),
                "get_all_params: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                getAllParams(settings(UID_NOT_EXISTING)),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(ERROR_USER_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                "get_all_params: отдает 404 user not found"
        });

        data.add(new Object[]{
                getParams(settings(accInfo.uid()).settingsList(messagesAvatarsParam)),
                allOf(hasStatusCode(OK_200),
                        hasStringContent(allOf(containsString(DEF_MESSAGES_AVATARS),
                                not(containsString(REQUEST_ID))))),
                "get_params: отдает дефолтное значение из профиля"
        });

        data.add(new Object[]{
                getParams(settings(UID_NOT_EXISTING).settingsList(messagesAvatarsParam)),
                allOf(hasStatusCode(NOT_FOUND_404),
                        hasStringContent(allOf(containsString(ERROR_USER_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                "get_params: отдает 404 user not found"
        });

        return data;
    }

    @Test
    public void test() throws IOException {
        Oper afterReq = oper
                .get().via(new DefaultHttpClient());

        assertThat(afterReq, matcher);
    }
}
