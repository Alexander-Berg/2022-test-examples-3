package ru.yandex.chemodan.app.docviewer.dao.properties;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class PropertiesDaoTest extends DocviewerSpringTestBase {
    public static final PassportUid UID = new PassportUid(123);
    @Autowired
    private PropertiesDao propertiesDao;

    @Test
    public void saveProperties() {
        cleanProperties();
        Properties props = new Properties(UID, Cf.map("key", "value"));
        propertiesDao.saveProperties(props);
        Properties savedProps = propertiesDao.findProperties(UID).get();
        Assert.equals(props, savedProps);
    }

    @Test
    public void setProperty() {
        cleanProperties();
        propertiesDao.setProperty(UID, "key", "value");
        propertiesDao.setProperty(UID, "key1", "value1");

        Properties props = propertiesDao.findProperties(UID).get();
        Assert.equals("value", props.getProperties().getTs("key"));
        Assert.equals("value1", props.getProperties().getTs("key1"));
    }

    private void cleanProperties() {
        propertiesDao.removeProperties(UID);
        Assert.assertEmpty(propertiesDao.findProperties(UID));
    }

}
