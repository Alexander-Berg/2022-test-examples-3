package ru.yandex.autotests.direct.cmd.steps.base;

import java.io.File;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.KeyValue;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.jsoup.nodes.Document;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.CSRFToken;
import ru.yandex.autotests.direct.cmd.data.XmlResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.utils.BaseSteps;
import ru.yandex.autotests.httpclientlite.HttpClientLite;
import ru.yandex.autotests.httpclientlite.context.ContextRequestExecutor;
import ru.yandex.autotests.httpclientlite.core.RequestBuilder;
import ru.yandex.autotests.httpclientlite.core.Response;
import ru.yandex.autotests.httpclientlite.core.ResponseParser;
import ru.yandex.autotests.httpclientlite.core.request.form.FormRequestBuilder;
import ru.yandex.autotests.httpclientlite.core.request.utils.ConvertUtils;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.KeyValueExtractor;
import ru.yandex.autotests.httpclientlite.core.response.file.FileResponseParser;
import ru.yandex.autotests.httpclientlite.core.response.html.JsoupHtmlParser;
import ru.yandex.autotests.httpclientlite.core.response.json.JsonResponseParser;
import ru.yandex.autotests.httpclientlite.core.response.multi.MultiParserAdapter;
import ru.yandex.autotests.httpclientlite.core.response.text.TextParser;
import ru.yandex.autotests.httpclientlite.core.response.xml.XmlResponseParser;

import static ru.yandex.autotests.direct.cmd.data.Headers.ACCEPT_JSON_HEADER;
import static ru.yandex.autotests.direct.cmd.data.Headers.X_REQUESTED_WITH_HEADER;

public abstract class DirectBackEndSteps extends BaseSteps<DirectStepsContext> {

    private ContextRequestExecutor requestExecutor;

    protected ContextRequestExecutor getRequestExecutor() {
        return requestExecutor;
    }

    /* New school */

    public <ResponseType> ResponseType get(CMD cmd,
            Object bean,
            Class<ResponseType> responseTypeClass)
    {
        return execute(RequestBuilder.Method.GET, cmd, bean, responseTypeClass);
    }

    public <ResponseType> ResponseType post(CMD cmd,
            Object bean,
            Class<ResponseType> responseTypeClass)
    {
        return execute(RequestBuilder.Method.POST, cmd, bean, responseTypeClass);
    }

    public <ResponseType> ResponseType executeRaw(RequestBuilder.Method method,
            Object bean,
            Class<ResponseType> responseTypeClass)
    {
        return requestExecutor.execute(method, bean, responseTypeClass);
    }

    protected <ResponseType> ResponseType execute(RequestBuilder.Method method,
            CMD cmd,
            Object bean,
            Class<ResponseType> responseTypeClass)
    {
        DirectBeanWrapper beanWrapper = new DirectBeanWrapper(cmd, getCsrfToken(), bean);
        return requestExecutor.execute(method, beanWrapper, responseTypeClass);
    }



    /* Old school */

    @Deprecated
    public Response get(CMD cmd) {
        return this.get(cmd, null);
    }

    @Deprecated
    public Response get(CMD cmd, Object bean) {
        DirectBeanWrapper beanWrapper = new DirectBeanWrapper(cmd, getCsrfToken(), bean);
        List<KeyValue<String, ?>> keyValues = KeyValueExtractor.beanToKeyValues(beanWrapper);
        List<NameValuePair> nameValuePairs = ConvertUtils.extractNameValuePairs(keyValues);
        return requestExecutor.get(nameValuePairs);
    }

    @Deprecated
    public Response post(CMD cmd, Object bean) {
        DirectBeanWrapper beanWrapper = new DirectBeanWrapper(cmd, getCsrfToken(), bean);
        return requestExecutor.post(beanWrapper);
    }

    protected void init(DirectStepsContext context) {
        super.init(context);

        HttpClientLite clientLite = new HttpClientLite.Builder().
                withClient(context.getHttpClient()).
                withRequestBuilder(buildRequestBuilder()).
                withResponseParser(buildResponseParser()).
                withLogger(new DirectBackEndLogger(context)).
                build();

        this.requestExecutor = new ContextRequestExecutor(clientLite, context.getConnectionContext());
    }

    protected RequestBuilder buildRequestBuilder() {
        FormRequestBuilder requestBuilder = new FormRequestBuilder();
        requestBuilder.setHeaders(ACCEPT_JSON_HEADER, X_REQUESTED_WITH_HEADER);
        return requestBuilder;
    }

    protected ResponseParser buildResponseParser() {
        return new MultiParserAdapter()
                .registerDefault(new JsonResponseParser()
                                .setFailOnResponseCodeMismatch(true)
                        /*setFailOnContentTypeMismatch(true)*/) //отключено, т.к. editAdGroupsPerformance возвращает Content-Type: text/html
                .register(Document.class, new JsoupHtmlParser()
                        .setFailOnResponseCodeMismatch(true) // не отключать, от этого зависят тесты!
                        .setFailOnContentTypeMismatch(true))
                .register(String.class, new TextParser())
                .register(RedirectResponse.class, new RedirectParser())
                .register(XmlResponse.class, new XmlResponseParser())
                .register(File.class, new FileResponseParser() {
                    @Override
                    protected List<ContentType> getAcceptedContentTypes() {
                        return ImmutableList.<ContentType>builder()
                                .addAll(super.getAcceptedContentTypes())
                                .add(ContentType.create("text/csv"))
                                .build();
                    }
                }.setFailOnResponseCodeMismatch(true)
                        .setFailOnContentTypeMismatch(true));
    }

    protected CSRFToken getCsrfToken() {
        return getContext().getAuthConfig().getCsrfToken();
    }
}
