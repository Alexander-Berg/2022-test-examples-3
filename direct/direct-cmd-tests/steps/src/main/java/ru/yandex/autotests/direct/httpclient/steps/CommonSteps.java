package ru.yandex.autotests.direct.httpclient.steps;

import java.util.regex.Pattern;

import ch.lambdaj.Lambda;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.Header;
import org.hamcrest.Matcher;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.utils.beans.IBeanWrapper;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.allure.annotations.Step;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 24.09.14
 */
public class CommonSteps extends DirectBackEndSteps {

    @Step("Проверяем, что ответ удовлетворяет условию {1}")
    public void checkDirectResponse(DirectResponse directResponse, Matcher matcher) {
        assertThat("ответ удовлетворяет условию", directResponse, matcher);
    }


    @Step("Проверяем, что содержимое ответа удовлетворяет условию {1}")
    public void checkDirectResponseContent(DirectResponse directResponse, Matcher<String> matcher) {
        AllureUtils.addHtmlAttachment("Ответ контроллера", directResponse.getResponseContent().asString());
        assertThat("содержимое ответа удовлетворяет условию", directResponse.getResponseContent().asString(), matcher);
    }

    @Step("Проверяем, что свойство {1} в ответе удовлетворяет условию {2}")
    public void checkDirectResponseJsonProperty(DirectResponse directResponse, String propertyPath, Matcher matcher) {
        assertThat("свойство " + propertyPath + " в ответе удовлетворяет условию", directResponse,
                hasJsonProperty(propertyPath, matcher));
    }

    @Step("получаем из ответа свойство по пути {1}")
    public <T> T readResponseJsonProperty(DirectResponse directResponse, String jsonPath) {
        try {
            return JsonPath.read(directResponse.getResponseContent().asString(), jsonPath);
        } catch (IllegalArgumentException e) {
            throw new BackEndClientException("Не удалось прочитать свойство: " + jsonPath + " в ответе контроллера", e);
        }
    }

    @Step("Сравниваем бины")
    public <T> void compareBeans(IBeanWrapper<T> expectedBean, IBeanWrapper<T> actualBean) {
        assertThat("бины одинаковые", actualBean, beanDiffer(expectedBean));
    }

    @Step("Проверяем, что редирект был на страницу {1}")
    public void checkRedirect(DirectResponse directResponse, CMD cmd) {
        checkRedirect(directResponse, containsString("cmd=" + cmd.getName()));
    }

    @Step("Проверяем, что редирект был на страницу {1}")
    public void checkRedirect(DirectResponse directResponse, String expectedSubstring) {
        checkRedirect(directResponse, containsString(expectedSubstring));
    }

    @Step("Проверяем, что редирект удовлетворяет {1}")
    public void checkRedirect(DirectResponse directResponse, Matcher matcher) {
        assertThat("код ответа контроллера соответвует коду переадресации",
                String.valueOf(directResponse.getStatusLine().getStatusCode()), startsWith("30"));
        String location = Lambda.filter(having(on(Header.class).getName(), equalTo("Location")),
                directResponse.getHeaders()).get(0).getValue();
        assertThat("страница, на которую происходит редирект, соответствует условию", location, matcher);
    }

    @Step("Проверяем, что редирект удовлетворяет {1}")
    public void checkJsonRedirect(DirectResponse directResponse, Matcher matcher) {
        String location = directResponse.getResponseContent().asString();
        assumeThat("страница, на которую происходит редирект, соответствует условию", location, matcher);
    }

    @Step("Проверяем, что ответ содержит ошибку {1}")
    public void checkDirectResponseError(DirectResponse directResponse, Matcher matcher) {
        assertThat("ответ содержит ошибку", directResponse, hasJsonProperty(Responses.ERROR.getPath(), matcher));
    }

    @Step("Проверяем, что поле cmd в ответе контроллера соответствует {1}")
    public void checkDirectResponseCmdField(DirectResponse directResponse, Matcher<String> matcher) {
        assertThat("поле cmd в ответе контроллера соответствует условию", directResponse,
                hasJsonProperty(Responses.CMD.getPath(), matcher));
    }

    @Step("Проверяем, что поле cmd в тексте ответа контроллера соответствует {1}")
    public void checkDirectTextResponseCmdField(DirectResponse directResponse, Matcher<String> matcher) {
        String cmdDeclarationInResponse = getResponsePartText(directResponse, "cmd: '.*'");
        assertThat("поле cmd в ответе контроллера соответствует условию", cmdDeclarationInResponse, matcher);
    }

    @Step("Проверяем, что ответ содержит страницу с ошибкой {1}")
    public void checkDirectResponseErrorCMDText(DirectResponse directResponse, String errorText) {
        AllureUtils.addHtmlAttachment("Ответ контроллера", directResponse.getResponseContent().asString());
        String cmdDeclarationInResponse = getResponsePartText(directResponse,
                "cmd.*(" + CMD.ERROR_RBAC.getName() + "|" + CMD.ERROR.getName() + ")");
        assertThat("cmd страницы с ошибкой соответствует ожиданиям", cmdDeclarationInResponse,
                notNullValue());
        assertThat("ответ содержит текст ошибки", directResponse.getResponseContent().asString(),
                containsString(errorText));
    }

    @Step("Проверяем соответствие кода ответа")
    public void checkDirectResponseStatusCodeForRequest(String getRequest, Matcher<Integer> matcher) {
        DirectResponse directResponse = execute(getRequest);
        AllureUtils.addHtmlAttachment("Ответ контроллера", directResponse.getResponseContent().asString());
        assertThat("код ответа равен " + matcher.toString(), directResponse.getStatusLine().getStatusCode(), matcher);
    }

    private String getResponsePartText(DirectResponse directResponse, String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        java.util.regex.Matcher matcher = pattern.matcher(directResponse.getResponseContent().asString());
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
