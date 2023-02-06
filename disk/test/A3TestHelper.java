package ru.yandex.chemodan.test;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapLikeType;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.eclipse.jetty.io.RuntimeIOException;
import org.springframework.context.ApplicationContext;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.spring.ApplicationContextUtils;
import ru.yandex.misc.web.servletContainer.SingleWarJetty;

public class A3TestHelper {

    private final int port;

    public A3TestHelper(int port) {
        this.port = port;
    }

    public void startServers(ApplicationContext applicationContext) {
        ListF<SingleWarJetty> servers =
                ApplicationContextUtils.beansOfType(applicationContext, SingleWarJetty.class);
        for (SingleWarJetty server : servers) {
            if (!server.isStarted()) {
                server.start();
            }
        }
    }

    public HttpResponse get(String relativeUrl) {
        return execute(buildGetRequest(relativeUrl), null);
    }

    public HttpResponse delete(String relativeUrl) {
        HttpRequestBase request = new HttpDelete(buildUrl(relativeUrl));
        return execute(request, null);
    }

    public Map<String, Long> postForMapStringLong(String relativeUrl, MapF<String, String> params) {
        HttpResponse response = post(relativeUrl, params);
        ObjectMapper objectMapper = new ObjectMapper();
        MapLikeType type = objectMapper.getTypeFactory().constructMapLikeType(Map.class, String.class, Long.class);
        try {
            return objectMapper.readValue(response.getEntity().getContent(), type);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public HttpResponse post(String relativeUrl) {
        return post(relativeUrl, Cf.map());
    }

    public HttpResponse post(String relativeUrl, MapF<String, String> params) {
        HttpPost request = new HttpPost(buildUrl(relativeUrl));
        ListF<NameValuePair> httpParams = Cf.arrayList();
        for (Map.Entry<String, String> param : params.entrySet()) {
            httpParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        return execute(request, new UrlEncodedFormEntity(httpParams, HTTP.DEF_CONTENT_CHARSET));
    }

    public HttpResponse post(String relativeUrl, String data) {
        HttpEntityEnclosingRequestBase request = new HttpPost(buildUrl(relativeUrl));
        return execute(request, convert(data));
    }

    public HttpResponse patch(String relativeUrl, String data) {
        HttpEntityEnclosingRequestBase request = new HttpPatch(buildUrl(relativeUrl));
        return execute(request, convert(data));
    }

    public HttpEntity convert(String data) {
        if (data == null) {
            return null;
        }
        return new StringEntity(data, ContentType.APPLICATION_JSON);
    }

    public HttpResponse put(String relativeUrl, String data) {
        HttpEntityEnclosingRequestBase request = new HttpPut(buildUrl(relativeUrl));
        return execute(request, convert(data));
    }

    public HttpResponse execute(HttpRequestBase request, HttpEntity entity) {
        try {
            if (entity != null) {
                ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
            }
            return getHttpClient().execute(request);
        } catch (Exception ex) {
            throw new RuntimeIOException(ex);
        }
    }

    public String getResult(HttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent());
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
    }

    public String buildUrl(String relativeUrl) {
        return "http://localhost:" + port + relativeUrl;
    }

    private HttpGet buildGetRequest(String relativeUrl) {
        return new HttpGet(buildUrl(relativeUrl));
    }

    private HttpClient getHttpClient() {
        return ApacheHttpClientUtils.singleConnectionClient(Timeout.seconds(50));
    }
}
