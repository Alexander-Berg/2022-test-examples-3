package ru.yandex.chemodan.app.djfs.core.share;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.legacy.exception.LegacyUserIsReadOnlyException;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class MongoGroupLinkDaoTest extends DjfsTestBase {
    @Test
    public void longPathDeserialization() {
        String path = "/disk/" + StringUtils.repeat("a", 1000);

        MongoCollection<Document> collection = commonMongoClient.getDatabase("group_links")
                .getCollection("group_links");
        collection.insertOne(new Document(Tuple2List.<String, Object>fromPairs(
                "_id", "test",
                "uid", "11",
                "gid", "1",
                "path", path,
                "v", 1,
                "rights", 660
        ).toMap()));

        ListF<GroupLink> groupLinks = mongoGroupLinkDao.findAll(DjfsUid.cons("11"));
        Assert.equals(1, groupLinks.length());
        Assert.equals(path, groupLinks.get(0).getPath().getPath());
    }

    @Test
    public void insertReadonly() {
        String id = UuidUtils.randomToHexString();
        String path = "/disk/" + StringUtils.repeat("a", 1000);

        GroupLink groupLink = GroupLink.builder()
                .id(id)
                .uid(DjfsUid.cons(11))
                .groupId(UuidUtils.randomToHexString())
                .path(DjfsResourcePath.cons(11, path))
                .version(1L)
                .permissions(SharePermissions.READ_WRITE)
                .creationTime(Instant.now())
                .build();

        mongoGroupLinkDao = new MongoGroupLinkDao(commonMongoClient, () -> true);
        Assert.assertThrows(() -> mongoGroupLinkDao.insert(groupLink), LegacyUserIsReadOnlyException.class);
    }
}
