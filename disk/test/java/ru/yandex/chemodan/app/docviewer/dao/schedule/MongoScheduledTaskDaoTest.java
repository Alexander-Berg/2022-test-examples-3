package ru.yandex.chemodan.app.docviewer.dao.schedule;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.docviewer.DocviewerAnnotationContextTestBase;
import ru.yandex.chemodan.app.docviewer.adapters.mongo.MongoDbAdapter;
import ru.yandex.chemodan.app.docviewer.config.MongoDaoContextConfiguration;
import ru.yandex.misc.net.HostnameUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
@ContextConfiguration(classes={MongoDaoContextConfiguration.class})
public class MongoScheduledTaskDaoTest extends DocviewerAnnotationContextTestBase {

    @Autowired
    @Qualifier("mongoDbAdapter")
    private MongoDbAdapter mongoDbAdapter;

    @Test
    public void createFindDeleteTest() {
        String taskId = "asdfasdf-asdf";
        ScheduledTaskDao scheduleDao = new MongoScheduledTaskDao("test", mongoDbAdapter);

        scheduleDao.saveOrUpdateScheduleItem(taskId, HostnameUtils.localHostname());
        Assert.isTrue(scheduleDao.find(taskId).isPresent());

        scheduleDao.delete(taskId);
        Assert.isFalse(scheduleDao.find(taskId).isPresent());
    }
}
