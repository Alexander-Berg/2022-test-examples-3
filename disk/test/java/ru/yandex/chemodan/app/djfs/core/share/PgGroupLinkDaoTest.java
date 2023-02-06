package ru.yandex.chemodan.app.djfs.core.share;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author m-messiah
 */
public class PgGroupLinkDaoTest extends DjfsTestBase {
    @Test
    public void longPathDeserialization() {
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

        pgGroupLinkDao.insert(groupLink);
        ListF<GroupLink> groupLinks = pgGroupLinkDao.findAll(DjfsUid.cons("11"));
        Assert.equals(1, groupLinks.length());
        Assert.equals(path, groupLinks.get(0).getPath().getPath());
    }
}
