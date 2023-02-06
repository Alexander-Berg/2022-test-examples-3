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
import ru.yandex.autotests.direct.httpclient.data.newclient.AjaxValidatePasswordParameters;
import ru.yandex.autotests.direct.httpclient.data.newclient.errors.AjaxValidatePasswordResponseErrors;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.11.14
 *         https://st.yandex-team.ru/TESTIRT-3207
 */

@Aqua.Test
@Description("Проверка контроллера ajaxValidatePassword")
@Stories(TestFeatures.NewClient.AJAX_VALIDATE_PASSWORD)
@Features(TestFeatures.NEW_CLIENT)
@Tag(CmdTag.AJAX_VALIDATE_PASSWORD)
@Tag(OldTag.YES)
public class AjaxValidatePasswordTest {

    private static final String AGENCY = Logins.AGENCY;
    private static final String SUPER = Logins.SUPER;
    private static final String MANAGER = Logins.MANAGER;
    private static final String SUPPORT = Logins.SUPPORT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private CSRFToken csrfToken;
    private AjaxValidatePasswordParameters ajaxValidatePasswordParameters;

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
        ajaxValidatePasswordParameters = new AjaxValidatePasswordParameters();
        ajaxValidatePasswordParameters.setTrackId(trackId);
    }

    @Test
    @Description("Проверяем корректную валидацию пароля под ролью менеджера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10555")
    public void checkPasswordValidationManagerRole() {
        init(MANAGER);
        ajaxValidatePasswordParameters.setPassword("at-tester1");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordCorrectResponse(csrfToken,
                ajaxValidatePasswordParameters);
    }

    @Test
    @Description("Проверяем корректную валидацию пароля под ролью суперпользователя")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10547")
    public void checkPasswordValidationSuperRole() {
        init(SUPER);
        ajaxValidatePasswordParameters.setPassword("at-tester1");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordCorrectResponse(csrfToken,
                ajaxValidatePasswordParameters);
    }

    @Test
    @Description("Проверяем корректную валидацию пароля под ролью саппорта")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10548")
    public void checkPasswordValidationSupportRole() {
        init(SUPPORT);
        ajaxValidatePasswordParameters.setPassword("at-tester1");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordCorrectResponse(csrfToken,
                ajaxValidatePasswordParameters);
    }

    @Test
    @Description("Проверяем корректную валидацию пароля под ролью агенства")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10549")
    public void checkPasswordValidationAgencyRole() {
        ajaxValidatePasswordParameters.setPassword("at-tester1");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordCorrectResponse(csrfToken,
                ajaxValidatePasswordParameters);
    }

    @Test
    @Description("Проверяем код ошибки при слишком коротком пароле")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10550")
    public void checkTooShortPasswordError() {
        ajaxValidatePasswordParameters.setPassword("login");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordResponseValidationErrorCodes(csrfToken,
                ajaxValidatePasswordParameters, equalTo(AjaxValidatePasswordResponseErrors.TOO_SHORT.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при слишком длинном пароле (длине > 255 символов)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10551")
    public void checkTooLongPasswordError() {
        ajaxValidatePasswordParameters.setPassword("qwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwert" +
                "yqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwer" +
                "tyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwertyqwerty");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordResponseValidationErrorCodes(csrfToken,
                ajaxValidatePasswordParameters, equalTo(AjaxValidatePasswordResponseErrors.TOO_LONG.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при пароле, имеющим запрещенные символы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10552")
    public void checkProhibitedSymbolsAtPasswordError() {
        ajaxValidatePasswordParameters.setPassword("log~in123");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordResponseValidationErrorCodes(csrfToken,
                ajaxValidatePasswordParameters,
                equalTo(AjaxValidatePasswordResponseErrors.PROHIBITED_SYMBOLS.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при слабом пароле")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10576")
    public void checkWeakPasswordError() {
        // раньше на этот пароль приходило предупреждение
        ajaxValidatePasswordParameters.setPassword("login1");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordResponseValidationErrorCodes(csrfToken,
                ajaxValidatePasswordParameters,
                equalTo(AjaxValidatePasswordResponseErrors.WEAK.toString()));
    }

    @Test
    @Description("Проверяем код предупреждения при слабом пароле")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10553")
    public void checkWeakPasswordWarning() {
        // проверка может ужесточиться, для предыдущего значения стала приходить ошибка валидации вместо предупреждения
        ajaxValidatePasswordParameters.setPassword("aaabbbcdef");
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordResponseWarningCodes(csrfToken,
                ajaxValidatePasswordParameters,
                equalTo(AjaxValidatePasswordResponseErrors.WEAK.toString()));
    }

    @Test
    @Description("Проверяем код ошибки при отсутствующем пароле")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10554")
    public void checkEmptyPasswordError() {
        ajaxValidatePasswordParameters.setPassword(null);
        cmdRule.oldSteps().onAjaxValidatePassword().checkAjaxValidatePasswordResponseErrorsCodes(csrfToken,
                ajaxValidatePasswordParameters,
                equalTo(AjaxValidatePasswordResponseErrors.INVALID_PARAMS.toString()));
    }
}
