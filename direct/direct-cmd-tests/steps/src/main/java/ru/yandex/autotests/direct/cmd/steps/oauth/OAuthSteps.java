package ru.yandex.autotests.direct.cmd.steps.oauth;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import ru.yandex.autotests.direct.cmd.data.oauth.AuthoriseRequest;
import ru.yandex.autotests.direct.cmd.data.oauth.VerificationCodeRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.auth.PassportSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.httpclientlite.core.RequestBuilder;
import ru.yandex.qatools.allure.annotations.Step;

public class OAuthSteps extends DirectBackEndSteps {

    @Step
    public void auth(String login, String password) {
        getInstance(PassportSteps.class, getContext()).authoriseAs(login, password);
    }

    @Step("Переходим по ссылке https://oauth.yandex.ru/verification_code?dev=true")
    public void setDevTrue() {
        getContext().getConnectionContext().path("/verification_code");
        executeRaw(RequestBuilder.Method.GET, new VerificationCodeRequest(), Document.class);
    }

    @Step("Переходим по ссылке https://oauth.yandex.ru/authorize?response_type=token&client_id=<>")
    public Document getResponseTypeToken(AuthoriseRequest params) {
        getContext().getConnectionContext().path("/authorize");
        return executeRaw(RequestBuilder.Method.GET, params, Document.class);
    }

    @Step("Подтверждаем приложению доступ к учетной записи")
    public Document acceptAccess(AuthoriseRequest params) {
        getContext().getConnectionContext().path("/authorize/allow");
        return executeRaw(RequestBuilder.Method.POST, params, Document.class);
    }

    @Step("Запрашиваем токен")
    public String getTokenForApp(String clientID) {
        return getTokenForApp(clientID, "direct:api");
    }

    @Step("Запрашиваем токен")
    public String getTokenForApp(String clientID, String grantedScopes) {
        setDevTrue();
        AuthoriseRequest openTokenParams = new AuthoriseRequest()
                .withResponseType("token")
                .withClientId(clientID)
                .withForceConfirm("1");

        Document response = getResponseTypeToken(openTokenParams);

        AuthoriseRequest getTokenParams = new AuthoriseRequest()
                .withCsrf(response.select("[name=csrf]").attr("value"))
                .withRequestId(response.select("[name=request_id]").attr("value"))
                .withResponseType("token")
                .withGrantedScopes(grantedScopes)
                .withRedirectUri("https://oauth.yandex.ru/verification_code?dev=True");

        Document redirectResponse = acceptAccess(getTokenParams);

        String content = redirectResponse.getElementsByTag("html").get(0)
                .getElementsByTag("head").get(0)
                .getElementsByTag("meta").get(0)
                .attr("content");

        String result = StringUtils.substringBetween(content, "access_token=", "&");
        if (result == null) {
            result = StringUtils.substringAfter(content, "access_token=");
        }
        return result;
    }


}
