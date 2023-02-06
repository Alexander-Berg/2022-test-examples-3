package ru.yandex.chemodan.app.notes.dao.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.notes.dao.model.RequestRecord;
import ru.yandex.misc.test.Assert;

/**
 * @author vpronto
 */
public class NotesRequestsDaoTest extends NotesJdbcDaoTestSupport {

    @Before
    public void insert() {
        requestsDao.create(requestR);
    }

    @After
    public void delete() {
        requestsDao.delete(requestR);
    }

    @Test
    public void find() {
        Option<RequestRecord> requestRecords = requestsDao.find(requestR);
        Assert.isTrue(requestRecords.isPresent());
        Assert.equals(requestRecords.get().requestId, requestR.requestId);
        Assert.equals(requestRecords.get().uid, requestR.uid);
        Assert.equals(requestRecords.get().entityId, requestR.entityId);
        Assert.equals(requestRecords.get().revision, requestR.revision);
    }

    @Test
    public void update() {
        requestsDao.update(requestR.toBuilder().revision(2L).entityId("updated").build());
        Option<RequestRecord> requestRecords = requestsDao.find(requestR);
        Assert.isTrue(requestRecords.isPresent());
        Assert.equals(requestRecords.get().requestId, requestR.requestId);
        Assert.equals(requestRecords.get().uid, requestR.uid);
        Assert.equals(requestRecords.get().entityId, "updated");
        Assert.equals(requestRecords.get().revision, 2L);
    }
}
