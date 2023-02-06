package ru.yandex.chemodan.app.docviewer.dao.uris;

import java.util.Date;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.adapters.mongo.MongoDbAdapter;
import ru.yandex.chemodan.app.docviewer.adapters.mongo.MongoDbUtils;
import ru.yandex.chemodan.app.docviewer.cleanup.UriCleanup;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.copy.StoredUriManager;
import ru.yandex.chemodan.app.docviewer.dao.rights.UriRightsDao;
import ru.yandex.chemodan.app.docviewer.dao.sessions.SessionKey;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author ssytnik
 */
public class MongoStoredUriDaoTest extends DocviewerSpringTestBase {
    private static final String FILE_ID = "stored-uri-file-id";

    @Autowired
    private MongoStoredUriDao storedUriDao;
    @Autowired
    private UriRightsDao uriRightsDao;
    @Autowired
    private StoredUriManager storedUriManager;
    @Autowired
    private UriCleanup uriCleanup;
    @Autowired
    @Qualifier("mongoDbAdapter")
    private MongoDbAdapter mongoDbAdapter;

    @Test
    public void latestAvailableForThisUid() throws Exception {
        PassportUidOrZero UID_123 = PassportUidOrZero.fromUid(123L);
        PassportUidOrZero UID_456 = PassportUidOrZero.fromUid(456L);

        prepareRecords(Cf.repeat(UID_123, 5).plus(Cf.repeat(UID_456, 5)));
        assertIs(4, findByUid(UID_123).get().getUri());
        assertIs(9, findByUid(UID_456).get().getUri());
    }

    @Test
    public void zeroUid() throws Exception {
        PassportUidOrZero UID_0 = PassportUidOrZero.fromUid(0L);
        PassportUidOrZero UID_555 = PassportUidOrZero.fromUid(555L);
        PassportUidOrZero UID_777 = PassportUidOrZero.fromUid(777L);

        prepareRecords(Cf.list(UID_555, UID_0));
        assertIs(0, findByUid(UID_555).get().getUri());
        assertIs(1, findByUid(UID_0).get().getUri());
        assertIs(1, findByUid(UID_777).get().getUri()); // fallback to UID_0
    }

    @Test
    public void readLongValue() throws Exception {
        prepareRecords(Cf.list(TestUser.YA_TEAM_AKIRAKOZOV.uid));
        assertIs(0, findByUid(TestUser.YA_TEAM_AKIRAKOZOV.uid).get().getUri());
    }

    @Test
    public void removeExpiredItem() {
        // DOCVIEWER-1174
        String id = "123";
        MongoDbUtils.removeRecordById(getCollection(), id);

        // create object
        DBObject query = new BasicDBObject();
        query.put("_id", id);
        query.put("timestamp", new Date(100));
        getCollection().insert(query);

        Assert.notNull(MongoDbUtils.findRecordById(getCollection(), id));

        storedUriDao.deleteByTimestampLessBatch(new Instant());
        // remove
        storedUriDao.deleteByTimestampLessBatch(new Instant(200), Function1V.nop());
        Assert.isNull(MongoDbUtils.findRecordById(getCollection(), id));
    }

    @Test
    public void removeNonvalidItem() {
        String id = "123";
        MongoDbUtils.removeRecordById(getCollection(), id);

        // create object
        DBObject query = new BasicDBObject();
        query.put("_id", id);
        query.put("timestamp", new Date(100));
        query.put("error", "error");
        getCollection().insert(query);

        Assert.notNull(MongoDbUtils.findRecordById(getCollection(), id));

        storedUriDao.deleteErrorsByTimestampLessBatch(new Instant());
        Assert.isNull(MongoDbUtils.findRecordById(getCollection(), id));
    }

    @Test
    public void passwords() {
        ActualUri uri = new ActualUri(getUriString(99)).withArchivePath("//inner.doc");

        storedUriDao.saveOrUpdateUri(uri, Option.empty(), TargetType.HTML_WITH_IMAGES, 1.0f);
        try {
            Assert.isEmpty(storedUriDao.getPasswordsSorted(uri));

            storedUriDao.updatePasswordRaw(uri, "", "outer-password");
            Tuple2<String, String> entry = storedUriDao.getPasswordsSorted(uri).single();
            Assert.equals("", entry.get1());
            Assert.equals(SessionKey.toHashValue("outer-password"), entry.get2());

            for (int i = 1; i <= 3; i++) {
                // 1 = add entry, 2 = update entry, 3 = do not update entry
                String innerPassword = "inner-password-" + (i == 1 ? 1 : 2);

                storedUriDao.updatePasswordRaw(uri, "//inner.doc", innerPassword);
                entry = storedUriDao.getPasswordsSorted(uri).drop(1).single();
                Assert.equals("//inner.doc", entry.get1());
                Assert.equals(SessionKey.toHashValue(innerPassword), entry.get2());
            }

        } finally {
            storedUriDao.delete(uri);
        }
    }

    @Test
    public void storeAndGetExtInfo() {
        ActualUri uri = new ActualUri(getUriString(1));
        try {
            Instant now = new Instant();
            storedUriDao.saveOrUpdateUri(uri, Option.empty(), TargetType.PDF, 1f);
            storedUriDao.updateUri(
                    uri, Option.empty(), FILE_ID, DataSize.ZERO, Option.of(now));

            StoredUri stored = storedUriDao.find(uri).get();
            Assert.equals(now, stored.getExtendedInfo(StoredUri.SERP_LAST_ACCESS).get());
        } finally {
            storedUriDao.delete(uri);
        }
    }

    @Test
    public void storeAndGetContentSize() {
        ActualUri uri = new ActualUri(getUriString(1));
        try {
            storedUriDao.saveOrUpdateUri(uri, Option.empty(), TargetType.PDF, 1f);
            storedUriDao.updateUri(
                    uri, Option.empty(), FILE_ID, DataSize.MEGABYTE, Option.empty());

            StoredUri stored = storedUriDao.find(uri).get();
            Assert.equals(DataSize.MEGABYTE, stored.getContentSize().get());
        } finally {
            storedUriDao.delete(uri);
        }
    }

    public void prepareRecords(ListF<PassportUidOrZero> uids) {
        cleanupData();
        fillData(uids);
    }

    private DBCollection getCollection() {
        return mongoDbAdapter.getDatabase().getCollection(MongoStoredUriDao.COLLECTION);
    }

    private void cleanupData() {
        for (StoredUri storedUri : storedUriDao.findAllByFileId(FILE_ID)) {
            uriCleanup.cleanupByActualUri(storedUri.getUri());
        }
    }

    private void fillData(ListF<PassportUidOrZero> uids) {
        int n = 0;
        for (PassportUidOrZero uid : uids) {
            saveEntry(uid, n++);
        }
    }

    private void saveEntry(PassportUidOrZero uid, int n) {
        ActualUri uri = new ActualUri(getUriString(n));
        storedUriDao.saveOrUpdateUri(uri, Option.empty(), TargetType.PDF, 1f);
        storedUriDao.updateUri(uri, Option.empty(), FILE_ID, DataSize.ZERO, Option.empty());

        uriRightsDao.saveOrUpdateUriRight(uri, uid);
        uriRightsDao.updateUriRights(uri, FILE_ID);

        ThreadUtils.sleep(1); // ensure that timestamps are different
    }

    private String getUriString(int n) {
        return "http://fakefakehost.ru/n" + n;
    }

    private Option<StoredUri> findByUid(PassportUidOrZero uid) {
        return storedUriManager.findByFileIdAndUidO(FILE_ID, uid);
    }

    private void assertIs(int expectedN, ActualUri uri) {
        Assert.equals(getUriString(expectedN), uri.getUriString());
    }
}
