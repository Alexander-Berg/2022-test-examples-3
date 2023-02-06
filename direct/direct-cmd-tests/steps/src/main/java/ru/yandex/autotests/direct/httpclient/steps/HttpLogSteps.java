package ru.yandex.autotests.direct.httpclient.steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.httpclient.lite.core.BackEndResponse;
import ru.yandex.autotests.httpclient.lite.core.ResponseContent;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.autotests.httpclient.lite.core.exceptions.ParameterNotFoundException;
import ru.yandex.autotests.httpclient.lite.core.steps.BackEndBaseSteps;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.events.MakeAttachmentEvent;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.autotests.httpclient.lite.utils.HttpUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: mariabye
 * Date: 22.01.14
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */
public class HttpLogSteps extends BackEndBaseSteps {

    public void logBody(StringBuilder sb, HttpEntity entity) {
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) entity.getContentLength());
        try {
            entity.writeTo(out);
            String bodyString = new String(out.toByteArray());
            if (entity instanceof UrlEncodedFormEntity) {
                sb.append("--data ").append("\"")
                        .append(bodyString).append("\"");
            } else {
                sb.insert(0,"cat > curltest.txt\n " +
                        bodyString+"\n");
               sb.append("-H ").append("\"")
               .append(entity.getContentType()).append("\" ")
                       .append("--data-binary @curltest.txt");
            }
        } catch (IOException e) {
            throw new BackEndClientException("Ошибка получения тела запроса при логировании", e);
        }
    }

    private String getLoginUserCmd() {
        StringBuilder loginCmd = new StringBuilder();
        loginCmd.append("curl ");
        loginCmd.append("--silent -c cookies --data ")
                .append("\"login=").append(config.getLogin() == null ? Logins.SUPER : config.getLogin())
                .append("&passwd=").append(config.getPassword() == null ? "at-tester8" : config.getPassword()).append("\" ");
        loginCmd.append("-L ").append("\"https://passport.yandex.ru/passport?mode=auth\"");
        loginCmd.append(" > /dev/null");
        return loginCmd.toString();
    }

    @Step("Http client: {0}")
    public void logContext(HttpUriRequest method, BackEndResponse response)
            throws IOException {
        StringBuilder requestSb = new StringBuilder();
        if (method.getMethod().equals("GET")) {
            requestSb.append("curl ");
            requestSb.append("-k -i -b cookies").append(" \"")
                    .append(getURI(method)).append("?");
        } else {
            requestSb.append("curl ");
            requestSb.append("-b cookies ").append("-k -i -X ").append(method.getMethod()).append(" ");
            requestSb.append("\"").append(getURI(method)).append("\" ");
        }

        printHeaders(requestSb, Arrays.asList(method.getAllHeaders()), true);
        logRequest(requestSb, method);

        StringBuilder responseSb = new StringBuilder();
        responseSb.append("Response:").append("\n");
        responseSb.append(response.getStatusLine().getStatusCode()).append("\n");
        printHeaders(responseSb, response.getHeaders(), false);
        String contentType = MediaType.TEXT_PLAIN;
        if (response.getHeader("Content-Type") != null) {
            contentType = response.getHeader("Content-Type").getValue().split(";")[0];
        }
        logResponse(responseSb, response.getResponseContent(), contentType);


        String attachLog = getLoginUserCmd() + "\n" + String.valueOf(requestSb) + "\n" + responseSb.toString() + "\n";
        AllureUtils.addTextAttachment("log", attachLog);
    }

    private void printHeaders(final StringBuilder b, final List<Header> headers, boolean curl) {
        for (final Header headerEntry : headers) {
            String val = StringUtils.join(headerEntry.getValue(), ",");
            final String header = headerEntry.getName();
            if (curl) {
                b.append("-H \"").append(header).append(": ").append(val).append("\" ");
            } else {
                b.append(header).append(": ").append(val).append("\n");
            }
        }
    }

    private void logRequest(StringBuilder sb, HttpUriRequest method) {
//        String logRequest = method.toString();
//        if (method instanceof HttpPost || method instanceof HttpPut) {
//            logRequest += " cmd=" + getCmd(getMethodParams(method));
//        }
        if(!HttpEntityEnclosingRequestBase.class.isAssignableFrom(method.getClass())) {
            List<NameValuePair> params = getMethodParams(method);
            params.remove(0);
            String requestEntityString = StringUtils.join(params, '&');
            sb.append(requestEntityString).append("\"");
            AllureUtils.addTextAttachment("Параметры запроса", requestEntityString);
        } else {
            logBody(sb, ((HttpEntityEnclosingRequestBase) method).getEntity());
        }

        sb.append(" | ").append("python -m json.tool").append(" | ").append("egrep --color -C 5 cid");
        log.info(sb.toString());
    }

    private void logResponse(StringBuilder sb, ResponseContent responseContent, String contentType)
            throws IOException {
        String responseEntityString = responseContent.asString();
        try {
            JsonParser jsonParser = new JsonParser();
            JsonElement json = jsonParser.parse(responseEntityString);
            AllureUtils.addJsonAttachment("Json Ответ", responseEntityString);
        } catch (JsonSyntaxException e){
            Allure.LIFECYCLE.fire(new MakeAttachmentEvent(responseEntityString.getBytes(), "Ответ", contentType));
        }
        sb.append(responseEntityString);
        log.info(sb.toString());
    }

    private String getCmd(List<NameValuePair> params) {
        try {
            return getUrlParameterValue(params, "cmd");
        } catch (ParameterNotFoundException e) {
            return "";
        }
    }
}
