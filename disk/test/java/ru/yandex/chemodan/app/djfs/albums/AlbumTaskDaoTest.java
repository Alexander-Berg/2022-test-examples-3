package ru.yandex.chemodan.app.djfs.albums;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.album.DjfsAlbumsTestBase;
import ru.yandex.chemodan.app.djfs.core.album.worker.DjfsAlbumsSubtask;
import ru.yandex.chemodan.app.djfs.core.album.worker.DjfsAlbumsTaskDao;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        DjfsAlbumsTestContextConfiguration.class,
})
public class AlbumTaskDaoTest extends DjfsAlbumsTestBase {
    @Autowired
    protected DjfsAlbumsTaskDao djfsAlbumsTaskDao;

    @Test
    public void testAddSubtask() {

        DjfsAlbumsSubtask task = new DjfsAlbumsSubtask(UID, "subtask-id", "resource-id");

        djfsAlbumsTaskDao.addSubtask(task);

        Assert.sizeIs(1, djfsAlbumsTaskDao.getSubtasks(UID, 10));

    }

    @Test
    public void testDeleteSubtask() {

        DjfsAlbumsSubtask task = new DjfsAlbumsSubtask(UID, "subtask-id", "resource-id");

        djfsAlbumsTaskDao.addSubtask(task);

        ListF<DjfsAlbumsSubtask> list = djfsAlbumsTaskDao.getSubtasks(UID, 10);

        djfsAlbumsTaskDao.deleteSubtask(list.get(0));

        Assert.sizeIs(0, djfsAlbumsTaskDao.getSubtasks(UID, 10));

    }

    @Test
    public void testSetNextRetry() {

        DjfsAlbumsSubtask task = new DjfsAlbumsSubtask(UID, "subtask-id", "resource-id");

        djfsAlbumsTaskDao.addSubtask(task);

        ListF<DjfsAlbumsSubtask> list = djfsAlbumsTaskDao.getSubtasks(UID, 10);

        djfsAlbumsTaskDao.setNextRetry(list.get(0), task.getRetryCount() + 1, task.getNextSchedule());

        list = djfsAlbumsTaskDao.getSubtasks(UID, 10);

        Assert.equals(task.getRetryCount() + 1, list.get(0).getRetryCount());

    }
}

