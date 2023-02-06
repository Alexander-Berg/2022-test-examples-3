package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.docviewer.dao.properties.PropertiesDao;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author akirakozov
 */
public class SavePropertyActionTest extends DocviewerWebSpringTestBase {
    private static final String SET_URL = "http://localhost:32405/save-property";
    private static final String GET_URL = "http://localhost:32405/get-properties";

    @Autowired
    private PropertiesDao propertiesDao;

    @Test
    public void setGetProperties() {
        PassportUid uid = new PassportUid(123);
        propertiesDao.removeProperties(uid);

        String url = UrlUtils.addParameters(
                SET_URL,
                Cf.map("uid", uid.getUid(), "key", "property", "value", "some value"));
        getResponse(url);

        url = UrlUtils.addParameter(GET_URL, "uid", uid.getUid());
        String response = getResponse(url);

        Document doc = Dom4jUtils.read(response.getBytes());
        Element property = doc.getRootElement().element("property");
        Assert.equals("property", property.attribute("key").getValue());
        Assert.equals("some value", property.attribute("value").getValue());
    }

    private String getResponse(String url) {
        return ApacheHttpClientUtils.executeReadString(new HttpGet(url), Timeout.seconds(3));
    }
}
