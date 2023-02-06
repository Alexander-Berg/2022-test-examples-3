package ru.yandex.chemodan.app.telemost.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.config.TelemostConfiguration;
import ru.yandex.chemodan.app.telemost.repository.model.ApiVersion;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.chemodan.util.web.A3JettyContextConfiguration;
import ru.yandex.commune.alive2.location.TestLocationResolverConfiguration;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        A3JettyContextConfiguration.class,
        TestLocationResolverConfiguration.class,
        ChemodanInitContextConfiguration.class,
        TelemostConfiguration.class
})
public abstract class TelemostWebActionBaseTest extends TelemostBaseContextTest {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Value("${a3.port}")
    private int port;

    private A3TestHelper a3TestHelper;

    @PostConstruct
    public void startServers() {
        this.a3TestHelper = new A3TestHelper(port);
        getA3TestHelper().startServers(getApplicationContext());
    }

    protected A3TestHelper getA3TestHelper() {
        return a3TestHelper;
    }

    protected Map<String, Object> createConferenceV1(MapF<String, Object> parameters) throws IOException {
        return createConference(ApiVersion.V1, parameters);
    }

    protected Map<String, Object> createConferenceV2(MapF<String, Object> parameters) throws IOException {
        return createConference(ApiVersion.V2, parameters);
    }

    protected Map<String, Object> createConference(ApiVersion apiVersion, MapF<String, Object> parameters) throws IOException {
        A3TestHelper helper = getA3TestHelper();
        StringBuilder linkBuilder = new StringBuilder("/" + apiVersion.name().toLowerCase()).append("/conferences");
        if (!parameters.isEmpty()) {
            linkBuilder.append("?");
        }
        parameters.forEach((key, value) -> appendUrlParameter(key, value, linkBuilder));
        HttpResponse response = helper.post(linkBuilder.toString());
        Assert.equals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        return mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>(){});
    }

    private void appendUrlParameter(String key, Object value, StringBuilder url) {
        if (url.charAt(url.length() - 1) != '?') {
            url.append("&");
        }
        try {
            url.append(key).append("=").append(URLEncoder.encode(value.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
