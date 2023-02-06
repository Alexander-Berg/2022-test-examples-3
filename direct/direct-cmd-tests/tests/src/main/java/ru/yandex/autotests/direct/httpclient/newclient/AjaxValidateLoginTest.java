package ru.yandex.autotests.direct.httpclient.newclient;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.newclient.AjaxValidateLoginParameters;
import ru.yandex.autotests.direct.httpclient.data.newclient.errors.AjaxValidateLoginResponseErrors;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.11.14
 *         https://st.yandex-team.ru/TESTIRT-3207
 */

@Aqua.Test
@Description("Проверка контроллера ajaxValidateLogin")
@Stories(TestFeatures.NewClient.AJAX_VALIDATE_LOGIN)
@Features(TestFeatures.NEW_CLIENT)
@Tag(CmdTag.AJAX_VALIDATE_LOGIN)
@Tag(OldTag.YES)
public class AjaxValidateLoginTest {

    private static final String AGENCY = Logins.AGENCY;
    private static final String SUPER = Logins.SUPER;
    private static final String MANAGER = Logins.MANAGER;
    private static final String SUPPORT = Logins.SUPPORT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private CSRFToken csrfToken;
    private AjaxValidateLoginParameters ajaxValidateLoginParameters;

    @Before
    public void before() {

        init(AGENCY);
    }

    private void init(String login) {
        cmdRule.oldSteps().onPassport().authoriseAs(login, User.get(login).getPassword());
        DirectResponse createAgencyClientResponse =
                cmdRule.oldSteps().onShowRegisterLoginPage().openShowRegisterLoginPage();
        csrfToken = createAgencyClientResponse.getCSRFToken();
        String trackId = cmdRule.oldSteps().commonSteps().
                readResponseJsonProperty(createAgencyClientResponse, "$..track_id[0]");
        ajaxValidateLoginParameters = new AjaxValidateLoginParameters();
        ajaxValidateLoginParameters.setTrackId(trackId);
    }

    @Test
    @Description("Проверяем корректную валидацию логина под ролью менеджер")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10530")
    public void checkLoginValidationWithManagerRole() {
        init(MANAGER);
        ajaxValidateLoginParameters.setLogin("newLogintttrtr");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginCorrectResponse(csrfToken,
                ajaxValidateLoginParameters);
    }

    @Test
    @Description("Проверяем корректную валидацию логина под ролью суперпользователь")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10531")
    public void checkLoginValidationWithSuperRole() {
        init(SUPER);
        ajaxValidateLoginParameters.setLogin("newLogintttrtr");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginCorrectResponse(csrfToken,
                ajaxValidateLoginParameters);
    }

    @Test
    @Description("Проверяем корректную валидацию логина под ролью саппорт")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10532")
    public void checkLoginValidationWithSupportRole() {
        init(SUPPORT);
        ajaxValidateLoginParameters.setLogin("newLogintttrtr");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginCorrectResponse(csrfToken,
                ajaxValidateLoginParameters);
    }

    @Test
    @Description("Проверяем корректную валидацию логина под ролью агентства")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10533")
    public void checkLoginValidationWithAgencyRole() {
        ajaxValidateLoginParameters.setLogin("newLogintttrtr");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginCorrectResponse(csrfToken,
                ajaxValidateLoginParameters);
    }

    @Test
    @Description("Проверяем код ошибки при недопустимом логине")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10529")
    public void checkNotAvailableLoginError() {
        ajaxValidateLoginParameters.setLogin("CLIENT");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.NOT_AVAILABLE.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, начинающимся с цифры")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10534")
    public void checkStartWithDigitLoginError() {
        ajaxValidateLoginParameters.setLogin("1login");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.STARTS_WITH_DIGIT.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при слишком длинном логине")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10535")
    public void checkTooLongLoginError() {
        ajaxValidateLoginParameters.setLogin("logindfgsdfgsdfgsdfgsdfgsdfgsdfgsdfgsdfgsdfgsdfg");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.TOO_LONG.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, начинающимся с точки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10536")
    public void checkStartWithDotLoginError() {
        ajaxValidateLoginParameters.setLogin(".CLIENT");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.STARTS_WITH_DOT.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, начинающимся с дефиса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10537")
    public void checkStartWithHyphenLoginError() {
        ajaxValidateLoginParameters.setLogin("-CLIENT");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.STARTS_WITH_HYPHEN.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, заканчивающегося с дефисом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10538")
    public void checkEndWithHyphenLoginError() {
        ajaxValidateLoginParameters.setLogin("CLIENT-");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.ENDS_WITH_HYPHEN.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, имеющим 2 точки подряд")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10539")
    public void checkDoubledDotsLoginError() {
        ajaxValidateLoginParameters.setLogin("lo..gin");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.DOUBLED_DOT.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, имеющим 2 дефиса подряд")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10540")
    public void checkDoubledHyphenLoginError() {
        ajaxValidateLoginParameters.setLogin("lo--gin");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.DOUBLED_HYPHEN.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, имеющим запрещенные символы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10541")
    public void checkProhibitedSymbolsLoginError() {
        ajaxValidateLoginParameters.setLogin("lo,gin");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.PROHIBITED_SYMBOLS.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, имеющим точку и дефис подряд")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10542")
    public void checkDotHyphenLoginError() {
        ajaxValidateLoginParameters.setLogin("lo.-gin");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.DOT_HYPHEN.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, имеющим дефис и точку подряд")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10543")
    public void checkHyphenDotLoginError() {
        ajaxValidateLoginParameters.setLogin("lo-.gin");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.HYPHEN_DOT.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при логине, оканчивающимся на точку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10544")
    public void checkEndWithDotLoginError() {
        ajaxValidateLoginParameters.setLogin("CLIENT.");
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.ENDS_WITH_DOT.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при пустом логине")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10545")
    public void checkEmptyLoginError() {
        ajaxValidateLoginParameters.setLogin(null);
        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.INVALID_PARAMS.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при пустом track_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10546")
    public void checkEmptyTrackIdError() {
        ajaxValidateLoginParameters.setTrackId(null);
        ajaxValidateLoginParameters.setLogin("logindsfsd");

        cmdRule.oldSteps().onAjaxValidateLogin().checkAjaxValidateLoginResponseErrorCodes(csrfToken,
                ajaxValidateLoginParameters, equalTo(AjaxValidateLoginResponseErrors.INVALID_PARAMS.toString()));
    }
}
