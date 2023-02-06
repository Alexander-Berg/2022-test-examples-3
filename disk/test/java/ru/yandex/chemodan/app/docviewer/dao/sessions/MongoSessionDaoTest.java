package ru.yandex.chemodan.app.docviewer.dao.sessions;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.docviewer.DocviewerAnnotationContextTestBase;
import ru.yandex.chemodan.app.docviewer.config.MongoDaoContextConfiguration;
import ru.yandex.chemodan.app.docviewer.dao.sessions.SessionKey.SessionCopyPassword;
import ru.yandex.chemodan.app.docviewer.dao.sessions.SessionKey.SessionPlain;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
@ContextConfiguration(classes={MongoDaoContextConfiguration.class})
public class MongoSessionDaoTest extends DocviewerAnnotationContextTestBase {

    @Autowired
    private SessionDao sessionDao;

    @Test
    public void deleteByTimestampAndCount() {
        SessionKey k = new SessionPlain("k");
        sessionDao.createOrUpdate("cleanup-id", k, "v");

        sessionDao.deleteByTimestampLessBatch(new Instant().minus(10000L));
        Assert.lt(0L, sessionDao.count());
        Assert.equals(1L, sessionDao.count("cleanup-id", k));

        sessionDao.deleteByTimestampLessBatch(new Instant());
        Assert.equals(0L, sessionDao.count());
        Assert.equals(0L, sessionDao.count("cleanup-id", k));
    }

    @Test
    public void writeAndRead() {
        SessionKey key = new SessionCopyPassword("http://url123.ru");
        String value = "password456";

        String newSessionId = sessionDao.createOrUpdate("", key, value);
        String readValue = sessionDao.findValidValue(newSessionId, key).get();

        Assert.notEmpty(newSessionId);
        Assert.isTrue(StringUtils.isAlphanumeric(newSessionId));

        Assert.equals(value, readValue);
        Assert.equals(1L, sessionDao.count(newSessionId, key));
    }

    @Test
    public void update() {
        SessionKey key = new SessionPlain("update");

        sessionDao.createOrUpdate("update-id", key, "value");
        sessionDao.createOrUpdate("update-id", key, "value");

        Assert.equals(1L, sessionDao.count("update-id", key));
    }
}
