package ru.yandex.autotests.direct.httpclient.core;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.httpclient.lite.core.BackEndRequestBuilder;
import ru.yandex.autotests.httpclient.lite.core.config.HttpClientConnectionConfig;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class DirectRequestBuilder extends BackEndRequestBuilder {

    private boolean returnVariableDump = false;

    public DirectRequestBuilder(HttpClientConnectionConfig config, boolean returnVariableDump) {
        this(config);
        this.returnVariableDump = returnVariableDump;
    }

    public DirectRequestBuilder(HttpClientConnectionConfig config) {
        super(config);
    }

    public HttpPost post(CMD cmd, IFormParameters form) {
        return post(cmd, CSRFToken.EMPTY, form);
    }

    private HttpEntity buildFormEntity(List<NameValuePair> requestParams) {
        return new UrlEncodedFormEntity(requestParams, Charset.forName("UTF8"));
    }

    private HttpEntity buildMultipartEntity(List<NameValuePair> requestParams) {
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (int index = 0; index < requestParams.size(); index++) {
            if (requestParams.get(index).getName().equalsIgnoreCase("file_picture") ||
                    requestParams.get(index).getName().equalsIgnoreCase("feed_file")) {
                entity.addPart(requestParams.get(index).getName(), new FileBody(new File(requestParams.get(index).getValue())));
            } else {
                try {
                    entity.addPart(requestParams.get(index).getName(), new StringBody(requestParams.get(index).getValue(),
                            Consts.UTF_8));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return entity;
    }

    private HttpEntity buildEntity(List<NameValuePair> requestParams) {
        if (requestParams.stream().anyMatch(t -> t.getName().equalsIgnoreCase("file_picture") ||
                t.getName().equalsIgnoreCase("feed_file"))) {
            return buildMultipartEntity(requestParams);
        } else {
            return buildFormEntity(requestParams);
        }
    }

    public HttpPost post(CMD cmd, CSRFToken token, IFormParameters form) {
        List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
        requestParams.addAll(form.parameters());
        requestParams.add(cmd.asPair());
        if (returnVariableDump) {
            setGetVarsInNameValuePairs(requestParams);
        }
        if (!token.getValue().equals("")) {
            requestParams.add(token.asPair());
        }
        return post(buildEntity(requestParams), new ArrayList<NameValuePair>());
    }

    public HttpGet get(CMD cmd) {
        return get(cmd, CSRFToken.EMPTY, new IFormParameters() {
            @Override
            public List<NameValuePair> parameters() {
                return EMPTY_CONTEXT;
            }
        });
    }

    public HttpGet get(CMD cmd, IFormParameters form) {
        return get(cmd, CSRFToken.EMPTY, form);
    }

    public HttpGet get(CMD cmd, CSRFToken token) {
        return get(cmd, token, new IFormParameters() {
            @Override
            public List<NameValuePair> parameters() {
                return EMPTY_CONTEXT;
            }
        });
    }

    public HttpGet get(CMD cmd, CSRFToken token, IFormParameters form) {
        List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
        requestParams.addAll(form.parameters());
        requestParams.add(cmd.asPair());
        if (!token.getValue().equals("")) {
            requestParams.add(token.asPair());
        }
        if (returnVariableDump) {
            setGetVarsInNameValuePairs(requestParams);
        }
        return get(requestParams);
    }

    public static void setGetVarsInNameValuePairs(List<NameValuePair> nameValuePairs) {
        NameValuePair pair = null;
        for (NameValuePair nameValuePair : nameValuePairs) {
            if (nameValuePair.getName().equals("get_vars")) {
                pair = nameValuePair;
                break;
            }
        }
        if (pair != null) {
            nameValuePairs.remove(pair);
        }
        nameValuePairs.add(new BasicNameValuePair("get_vars", "1"));
    }

    public boolean isReturnVariableDump() {
        return returnVariableDump;
    }
}
