package ru.yandex.autotests.innerpochta;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
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
import ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj;
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

import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.GetQueriesForUserNotInBase.*;
import static ru.yandex.autotests.innerpochta.UpdateQueriesForUserNotInBase.CUSTOM_SKIN_NAME;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllParams.getAllParams;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllProfile.getAllProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.GetParams.getParams;
import static ru.yandex.autotests.innerpochta.utils.oper.GetProfile.getProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateOneParamsSetting;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateParams;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateProfile;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StatusCodeMatcher.hasStatusCode;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StringResponseOperMatcher.hasStringContent;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getLongString;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 19.03.13
 * Time: 16:27
 */
@Aqua.Test
@Title("Негативные кейсы")
@Description("Проверяем реакцию на ошибочные данные")
@RunWith(Parameterized.class)
@Features("Общее")
@Stories("Негативные и странные тесты")
public class NegativeCases {

    public static final String NO_VALUES_IN_SETTINGS_LIST = "no values in 'settings_list' parameter";
    public static final String ERROR_PARAM_UID_NOT_FOUND = "parameter 'uid' not found";
    public static final String ERROR_PARAM_SETLIST_NOT_FOUND = "parameter 'settings_list' not found";
    public static final String ERROR_UID_VALUE_IS_INVALID = "'uid' value is invalid";
    public static final String REQUEST_ID = "request id:";
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String DEFAULT_EMAIL = "default_email";
    private static DefaultHttpClient client = new DefaultHttpClient();
    private final Logger logger = LogManager.getLogger(this.getClass());

    @ClassRule
    public static AccountRule accInfo = new AccountRule().with(NegativeCases.class);

    @Rule
    public TestRule chain = RuleChain.outerRule(new LogConfigRule());
    @Parameterized.Parameter(0)
    public Oper oper;
    @Parameterized.Parameter(1)
    public Matcher<Oper> matcher;
    @Parameterized.Parameter(2)
    public boolean getQuery;
    @Parameterized.Parameter(3)
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

    @Parameterized.Parameters(name = "{index}-{3}")
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();

        //200
        data.add(new Object[]{
                getAll(settings(accInfo.uid())),
                allOf(hasStatusCode(HttpStatus.OK_200),
                        hasStringContent(not(containsString(REQUEST_ID)))),
                true,
                "get_all: Нормальное поведение"
        });

        data.add(new Object[]{
                getAllParams(settings(accInfo.uid())),
                allOf(hasStatusCode(HttpStatus.OK_200),
                        hasStringContent(not(containsString(REQUEST_ID)))),
                true,
                "get_all_params: Нормальное поведение"
        });

        data.add(new Object[]{
                getAllProfile(settings(accInfo.uid())),
                allOf(hasStatusCode(HttpStatus.OK_200),
                        hasStringContent(not(containsString(REQUEST_ID)))),
                true,
                "get_all_profile: Нормальное поведение"
        });

        data.add(new Object[]{
                getParams(settings(accInfo.uid()).settingsList("qwer")),
                allOf(hasStatusCode(HttpStatus.OK_200),
                        hasStringContent(not(containsString("qwer"))),
                        hasStringContent(not(containsString(REQUEST_ID)))),
                true,
                "get_params: Неверное поле - параметры"
        });

        data.add(new Object[]{
                getProfile(settings(accInfo.uid()).settingsList(DEFAULT_EMAIL)),   //without ask_validator
                allOf(hasStatusCode(HttpStatus.OK_200),
                        hasStringContent(not(containsString(DEFAULT_EMAIL))),
                        hasStringContent(not(containsString(REQUEST_ID)))),
                true,
                "get_profile: Не спрашиваем валидатор"
        });

        data.add(new Object[]{
                getProfile(settings(accInfo.uid()).settingsList("qwer")),
                allOf(hasStatusCode(HttpStatus.OK_200),
                        hasStringContent(not(containsString("qwer"))),
                        hasStringContent(not(containsString(REQUEST_ID)))),
                true,
                "get_profile: Неверное поле - профиль"
        });


        // 400 ---------
        data.add(new Object[]{
                getAll(EmptyObj.empty()),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(REQUEST_ID),
                                containsString(ERROR_PARAM_UID_NOT_FOUND)))),
                true,
                "get_all: Не указываем данных для аутенфикации - all"
        });

        data.add(new Object[]{
                getAllParams(EmptyObj.empty()),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(REQUEST_ID),
                                containsString(ERROR_PARAM_UID_NOT_FOUND)))),
                true,
                "get_all_params: Не указываем данных для аутенфикации - all params"
        });

        data.add(new Object[]{
                getAllProfile(
                        EmptyObj.empty()),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(REQUEST_ID),
                                containsString(ERROR_PARAM_UID_NOT_FOUND)))),
                true,
                "get_all_profile: Не указываем данных для аутенфикации - all profile"
        });

        data.add(new Object[]{
                getParams(EmptyObj.empty()),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(REQUEST_ID),
                                containsString(ERROR_PARAM_UID_NOT_FOUND)))),
                true,
                "get_params: Не указываем данных для аутенфикации - params"
        });

        data.add(new Object[]{
                getProfile(EmptyObj.empty()),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(REQUEST_ID),
                                containsString(ERROR_PARAM_UID_NOT_FOUND)))),
                true,
                "get_profile: Не указываем данных для аутенфикации - profile"
        });

        data.add(new Object[]{
                updateParams(EmptyObj.empty()),
                allOf(hasStatusCode(HttpStatus.BAD_REQUEST_400),
                        hasStringContent(allOf(containsString(ERROR_PARAM_UID_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                false,
                "update_params: Не указываем параметров"
        });

        data.add(new Object[]{
                updateProfile(EmptyObj.empty()),
                allOf(hasStatusCode(HttpStatus.BAD_REQUEST_400),
                        hasStringContent(allOf(containsString(ERROR_PARAM_UID_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                false,
                "updateProfile: Не указываем параметров"
        });

        data.add(new Object[]{
                updateOneParamsSetting(UID_NOT_EXISTING, messagesAvatarsParam, "off"),
                allOf(hasStatusCode(HttpStatus.NOT_FOUND_404),
                        hasStringContent(allOf(containsString(ERROR_USER_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                false,
                "updateParams: Указываем несуществующий юид (нет в ЧЯ)"
        });

        data.add(new Object[]{
                updateProfile(settings(UID_NOT_EXISTING).set(true, skinNameParam, CUSTOM_SKIN_NAME)),
                allOf(hasStatusCode(HttpStatus.NOT_FOUND_404),
                        hasStringContent(allOf(containsString(ERROR_USER_NOT_FOUND),
                                containsString(REQUEST_ID)))),
                false,
                "updateProfile: Указываем несуществующий юид (нет в ЧЯ)"
        });

        data.add(new Object[]{
                updateProfile(settings("").set(true, skinNameParam, CUSTOM_SKIN_NAME)),
                allOf(hasStatusCode(HttpStatus.BAD_REQUEST_400),
                        hasStringContent(allOf(containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                false,
                "updateProfile: Указываем пустой юид (нет в ЧЯ)"
        });

        data.add(new Object[]{
                getProfile(settings(accInfo.uid()).settingsList("")),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(NO_VALUES_IN_SETTINGS_LIST),
                                containsString(REQUEST_ID)))),
                true,
                "get_profile: Неверный список настроек при несуществующем суид"
        });


        data.add(new Object[]{
                getAll(settings("-1")),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_all: Невалидный uid"
        });


        data.add(new Object[]{
                getAll(settings("1000004352345234533333")),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_all: Несуществующий uid"
        });


        data.add(new Object[]{
                getAllParams(settings(getLongString())),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_all_params: Long String uid"
        });

        data.add(new Object[]{
                getAllProfile(settings(accInfo.uid()).format(getLongString())),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(containsString("unsupported 'format' value")),
                        hasStringContent(containsString(REQUEST_ID))),
                true,
                "get_all_profile: Неверный параметр формата"
        });

        data.add(new Object[]{
                getParams(settings(accInfo.uid())
                        .settingsList(getLongString(), getLongString())),
                allOf(hasStatusCode(HttpStatus.OK_200),
                        hasStringContent(not(containsString(REQUEST_ID)))),
                true,
                "get_params: Длинное название параметра"
        });

        data.add(new Object[]{
                getAll(settings("").settingsList("bla")),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_all: Пустой uid при указанном settings_list (DARIA-53807)"
        });

        data.add(new Object[]{
                getAll(settings("").settingsList("bla").askValidator()),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_all ask_validator: Пустой uid при указанном settings_list (DARIA-53807)"
        });

        data.add(new Object[]{
                getAllProfile(settings("")),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(
                                containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_all_profile: Задан пустой uid"
        });

        data.add(new Object[]{
                getAllParams(settings("")),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_all_params: Задан пустой uid"
        });

        data.add(new Object[]{
                getParams(settings("").settingsList(DEFAULT_EMAIL)),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_params: Задан пустой uid"
        });


        data.add(new Object[]{
                getProfile(settings("").settingsList(DEFAULT_EMAIL)),
                allOf(hasStatusCode(BAD_REQUEST_400),
                        hasStringContent(allOf(containsString(ERROR_UID_VALUE_IS_INVALID),
                                containsString(REQUEST_ID)))),
                true,
                "get_profile: Задан пустой uid"
        });

        return data;
    }

    @Test
    public void test() throws IOException {
        Oper afterQuery;
        if (getQuery) {
            afterQuery = oper.get().via(client);
        } else afterQuery = oper.post().via(client);

        assertThat(afterQuery, matcher);
    }
}
