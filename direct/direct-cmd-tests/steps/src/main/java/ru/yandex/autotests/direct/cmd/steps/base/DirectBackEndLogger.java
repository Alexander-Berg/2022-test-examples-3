package ru.yandex.autotests.direct.cmd.steps.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.autotests.direct.utils.model.MongoUser;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.autotests.httpclientlite.core.Response;
import ru.yandex.autotests.httpclientlite.utils.ConsoleLogger;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.events.MakeAttachmentEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.autotests.httpclientlite.utils.HttpUtils.getMethodParams;

public class DirectBackEndLogger extends ConsoleLogger {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static JsonParser jsonParser = new JsonParser();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private DirectStepsContext context;

    public DirectBackEndLogger(DirectStepsContext context) {
        this.context = context;
    }

    @Override
    @Step("http-запрос")
    public void logRequest(HttpUriRequest request) {
        super.logRequest(request);
        String authCurl = createUserAuthenticationCurl();
        AllureUtils.addTextAttachment("CURL для аутентификации", authCurl);
        AllureUtils.addTextAttachment("Сырой запрос", super.requestToString(request));
        AllureUtils.addTextAttachment("CURL запроса", requestToCurl(request));
        AllureUtils.addTextAttachment("Параметры запроса", extractRequestParams(request));
        //todo
//        if (props.isLogCurlToConsole()) {
            log.info("\nAuth:\n"+authCurl + SPACE);
            log.info("\nCommand:\n"+requestToCurl(request) + SPACE);
//        }
    }

    @Override
    @Step("http-ответ")
    public void logResponse(Response response) {
        super.logResponse(response);
        AllureUtils.addTextAttachment("Сырой ответ", super.responseToString(response));
        tryToMakeJsonAttachment(response);
    }


    protected String createUserAuthenticationCurl() {

        String login;
        String passwd;
        if(context.getAuthConfig().getLogin() == null) {
            MongoUser usr = MongoUser.get("at-direct-super");
            login = usr.getLogin();
            passwd = usr.getPassword();
        } else {
            login = context.getAuthConfig().getLogin();
            passwd = context.getAuthConfig().getPassword();
        }

        return String.format("curl --silent -c cookies --data \"login=%s&passwd=%s\" -L \"%s\" > /dev/null",
                login, passwd, context.getProperties().getDirectCmdAuthPassportHost());
    }

    /**
     * В случае, если запрос имеет тело типа application/x-www-form-urlencoded,
     * метод достает параметры из тела.
     *
     * В противном случае метод достает параметры из query урла.
     * @param request запрос
     * @return параметры запроса из тела или урла
     */
    protected String extractRequestParams(HttpUriRequest request) {
        List<NameValuePair> params = getMethodParams(request);
        params.remove(0);                                           // todo wtf?
        return StringUtils.join(params, "\n");
    }

    protected String requestToCurl(HttpUriRequest request) {
        StringBuilder requestSb = new StringBuilder();
        if (request.getMethod().equals("GET")) {
            requestSb
                    .append("curl -k -i -b cookies")
                    .append(" \"")
                    .append(request.getURI())
                    .append("\"");
        } else {
            requestSb
                    .append("curl -b cookies -k -i -X ")
                    .append(request.getMethod())
                    .append(" \"")
                    .append(request.getURI())
                    .append("\" ");
        }
        appendCurlHeaders(requestSb, Arrays.asList(request.getAllHeaders()));

        if (request instanceof HttpEntityEnclosingRequestBase) {
            appendCurlBody(requestSb, ((HttpEntityEnclosingRequestBase) request).getEntity());
        }

        requestSb.append(" | python -m json.tool | egrep --color -C 5 cid");

        return requestSb.toString();
    }

    protected void tryToMakeJsonAttachment(Response response) {
        String responseEntityString = response.getResponseContent().asString();
        String mimeType = response.getResponseContent().getType().getMimeType();
        JsonElement json;
        try {
            json = jsonParser.parse(responseEntityString);
            AllureUtils.addJsonAttachment("JSON-ответ (" + mimeType + ")", gson.toJson(json));
        } catch (JsonSyntaxException e){
            Allure.LIFECYCLE.fire(
                    new MakeAttachmentEvent(
                            responseEntityString.getBytes(), "Ответ " + mimeType, mimeType));
        }
    }


    private void appendCurlHeaders(final StringBuilder sb, final List<Header> headers) {
        for (Header headerEntry : headers) {
            sb.append("-H \"")
                    .append(headerEntry.getName())
                    .append(": ")
                    .append(headerEntry.getValue())
                    .append("\" ");
        }
    }

    private void appendCurlBody(final StringBuilder sb, HttpEntity entity) {
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) entity.getContentLength());
        try {
            entity.writeTo(out);
            String bodyString = new String(out.toByteArray());
            if (entity instanceof UrlEncodedFormEntity) {
                sb
                        .append("--data ")
                        .append("\"")
                        .append(bodyString)
                        .append("\"");
            } else {
                sb.insert(0, "cat > curltest.txt\n " + bodyString + "\n");
                sb
                        .append("-H ")
                        .append("\"")
                        .append(entity.getContentType())
                        .append("\" ")
                        .append("--data-binary @curltest.txt");
            }
        } catch (IOException e) {
            throw new HttpClientLiteException("Ошибка получения тела запроса при логировании", e);
        }
    }
}
