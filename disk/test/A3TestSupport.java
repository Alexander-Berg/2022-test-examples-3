package ru.yandex.chemodan.test;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.boot.ChemodanPropertiesLoadStrategy;
import ru.yandex.misc.property.load.PropertiesLoader;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author nshmakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public abstract class A3TestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${a3.port}")
    private int port;

    private A3TestHelper a3TestHelper;

    @BeforeClass
    public static void init() {
        TestHelper.initialize();
        PropertiesLoader.initialize(
                new ChemodanPropertiesLoadStrategy(new SimpleAppName("disk", "counters-api"), true));
    }

    @PostConstruct
    public void startServers() {
        a3TestHelper = new A3TestHelper(port);
        a3TestHelper.startServers(applicationContext);
    }

    protected HttpResponse get(String relativeUrl) {
        return a3TestHelper.get(relativeUrl);
    }

    protected HttpResponse delete(String relativeUrl) {
        return a3TestHelper.delete(relativeUrl);
    }


    protected Map<String, Long> postForMapStringLong(String relativeUrl, MapF<String, String> params) {
        return a3TestHelper.postForMapStringLong(relativeUrl, params);
    }

    protected HttpResponse post(String relativeUrl) {
        return a3TestHelper.post(relativeUrl);
    }

    protected HttpResponse post(String relativeUrl, MapF<String, String> params) {
        return a3TestHelper.post(relativeUrl, params);
    }

    protected HttpResponse post(String relativeUrl, String data) {
        return a3TestHelper.post(relativeUrl, data);
    }

    protected HttpResponse patch(String relativeUrl, String data) {
        return a3TestHelper.patch(relativeUrl, data);
    }

    protected HttpEntity convert(String data) {
        return a3TestHelper.convert(data);
    }

    protected HttpResponse put(String relativeUrl, String data) {
        return a3TestHelper.put(relativeUrl, data);
    }

    protected HttpResponse execute(HttpRequestBase request, HttpEntity entity) {
        return a3TestHelper.execute(request, entity);
    }

    protected String getResult(HttpResponse response) {
        return a3TestHelper.getResult(response);
    }

    protected String buildUrl(String relativeUrl) {
        return a3TestHelper.buildUrl(relativeUrl);
    }
}
