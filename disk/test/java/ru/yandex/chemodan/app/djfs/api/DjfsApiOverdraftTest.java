package ru.yandex.chemodan.app.djfs.api;

import java.util.UUID;
import java.util.function.Function;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.diskinfo.DiskInfo;
import ru.yandex.chemodan.app.djfs.core.diskinfo.DiskInfoData;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceType;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.misc.test.Assert;


/**
 * @author friendlyevil
 */
public class DjfsApiOverdraftTest extends DjfsApiActionTest {

    private static final DjfsUid UID = DjfsUid.cons(123L);

    @Before
    public void setUp() {
        super.setUp();
        getDjfsUserTestHelper().initializePgUser(UID, 1, Function.identity());
    }

    @Test
    public void firstOverdraftReset() {
        setUpOverdraftCounter();
        resetWithAsserts(1);
    }

    @Test
    public void testOverdraftResets() {
        setUpOverdraftCounter();
        resetWithAsserts(1);
        resetWithAsserts(2);
        resetWithAsserts(3);
        resetWithAsserts(4);
    }

    private void setUpOverdraftCounter() {
        diskInfoDao.insertOrUpdateDiskInfoValue(DiskInfo.builder()
                .id(UUID.randomUUID())
                .uid(UID)
                .path("/overdraft")
                .type(DjfsResourceType.FILE)
                .data(Option.of(DiskInfoData.rawData("{\"overdraft_reset_count\": 0}")))
                .build());
    }

    private void resetWithAsserts(int expectedCounter) {
        HttpResponse httpResponse = getA3TestHelper().post("/api/reset_overdraft_date?uid=" + UID.asLong());
        Assert.assertEquals(204, httpResponse.getStatusLine().getStatusCode());
        Assert.assertEquals(expectedCounter, (int) diskInfoDao.getOverdraftCounter(UID).get());
    }
}
