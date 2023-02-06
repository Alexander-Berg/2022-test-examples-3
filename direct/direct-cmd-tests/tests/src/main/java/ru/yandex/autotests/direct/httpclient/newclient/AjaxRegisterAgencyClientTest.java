package ru.yandex.autotests.direct.httpclient.newclient;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.newclient.AjaxRegisterLoginParameters;
import ru.yandex.autotests.direct.httpclient.data.newclient.errors.AjaxRegisterLoginResponseErrors;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.passport.api.tools.captcha.CaptchaActions;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.11.14
 *         https://st.yandex-team.ru/TESTIRT-3207
 */

@Aqua.Test
@Description("Проверка контроллера ajaxRegisterLogin")
@Stories(TestFeatures.NewClient.AJAX_REGISTER_LOGIN)
@Features(TestFeatures.NEW_CLIENT)
@Tag(TrunkTag.YES)
@Tag(CmdTag.AJAX_REGISTER_LOGIN)
@Tag(OldTag.YES)
@RunWith(Parameterized.class)
public class AjaxRegisterAgencyClientTest {

    private static final boolean GET_VARS = true;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public String login;
    @Parameterized.Parameter(value = 1)
    public String description;

    private CSRFToken csrfToken;
    private AjaxRegisterLoginParameters ajaxRegisterLoginParameters;
    private String captchaId;
    private PropertyLoader<AjaxRegisterLoginParameters> propertyLoader;

    @Parameterized.Parameters(name = "Пользователь: {0}, роль: {1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {Logins.AGENCY, "Агентство"},
                {Logins.SUPER, "Суперпользователь"},
                {Logins.MANAGER, "Менеджер"},
                {Logins.SUPPORT, "Саппорт"}
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {

        cmdRule.oldSteps().onPassport().authoriseAs(login, User.get(login).getPassword());
        DirectResponse createAgencyClientResponse =
                cmdRule.oldSteps().onShowRegisterLoginPage().openShowRegisterLoginPage();
        csrfToken = createAgencyClientResponse.getCSRFToken();
        String trackId = cmdRule.oldSteps().commonSteps().
                readResponseJsonProperty(createAgencyClientResponse, "$..track_id[0]");
        propertyLoader = new PropertyLoader<>(AjaxRegisterLoginParameters.class);
        ajaxRegisterLoginParameters = propertyLoader.getHttpBean("ajaxRegisterLoginParameters");
        ajaxRegisterLoginParameters.setLogin("at-direct-rnd-" + RandomStringUtils.randomNumeric(10));
        ajaxRegisterLoginParameters.setTrackId(trackId);
        captchaId = cmdRule.oldSteps().commonSteps().
                readResponseJsonProperty(createAgencyClientResponse, "$..captcha_id[0]");
        ajaxRegisterLoginParameters.setxCaptchaId(captchaId);
    }

    @Test
    @Description("Проверяем корректную регистрацию нового клиента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10523")
    public void checkRegisterAgencyClient() {
        String captchaCode = CaptchaActions.getCaptchaAnswerByKey(captchaId);
        ajaxRegisterLoginParameters.setxCaptchaCode(captchaCode);

        DirectResponse response = cmdRule.oldSteps().onAjaxRegisterLogin().registerAgencyClient(csrfToken,
                ajaxRegisterLoginParameters);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, hasJsonProperty("$.status",
                equalTo("ok")));
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, hasJsonProperty("$.uid",
                is(not(emptyCollectionOf(Object.class)))));
    }

    @Test
    @Description("Проверяем код ошибки при регистрации нового клиента с неправильной капчей")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10524")
    public void checkIncorrectCaptchaErrorCode() {
        ajaxRegisterLoginParameters.setxCaptchaCode("invalidCaptchaCode");
        cmdRule.oldSteps().onAjaxRegisterLogin().checkAjaxValidateLoginResponseValidationErrorCodes(csrfToken,
                ajaxRegisterLoginParameters,
                contains(AjaxRegisterLoginResponseErrors.INCORRECT_CAPTCHA.toString()));
    }

    @Test
    @Description("Проверяем ошибку при регистрации нового клиента c недопустимым логином")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10525")
    public void checkRegisterAgencyClientWithInvalidLogin() {
        ajaxRegisterLoginParameters.setLogin("CLIENT");
        String captchaCode = CaptchaActions.getCaptchaAnswerByKey(captchaId);
        ajaxRegisterLoginParameters.setxCaptchaCode(captchaCode);
        cmdRule.oldSteps().onAjaxRegisterLogin().checkAjaxValidateLoginResponseError(csrfToken,
                ajaxRegisterLoginParameters,
                contains(AjaxRegisterLoginResponseErrors.LOGIN_NOT_AVAILABLE.toString()));
    }

    @Test
    @Description("Проверяем ошибку при регистрации нового клиента c коротким паролем")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10526")
    public void checkRegisterAgencyClientWithInvalidPassword() {
        ajaxRegisterLoginParameters.setPassword("12345");
        String captchaCode = CaptchaActions.getCaptchaAnswerByKey(captchaId);
        ajaxRegisterLoginParameters.setxCaptchaCode(captchaCode);
        cmdRule.oldSteps().onAjaxRegisterLogin().checkAjaxValidateLoginResponseError(csrfToken,
                ajaxRegisterLoginParameters,
                contains(AjaxRegisterLoginResponseErrors.PASSWORD_SHORT.toString()));
    }

}
