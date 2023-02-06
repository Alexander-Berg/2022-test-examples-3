package ru.yandex.chemodan.app.djfs.core.share;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author m-messiah
 */
public class PgGroupDaoTest extends DjfsTestBase {
    @Test
    public void longPathDeserialization() {
        String id = UuidUtils.randomToHexString();
        String path = "/disk/" + StringUtils.repeat("a", 1000);

        Group group = Group.builder()
                .id(id)
                .owner(DjfsUid.cons("1"))
                .path(DjfsResourcePath.cons(1, path))
                .size(1)
                .version(Option.of(0L))
                .build();
        pgGroupDao.insert(group);
        Option<Group> groupResult = pgGroupDao.find(id);
        Assert.equals(group, groupResult.get());
    }

    @Test
    public void increaseSize() {
        String id = UuidUtils.randomToHexString();
        Group group = Group.builder()
                .id(id)
                .owner(DjfsUid.cons("1"))
                .path(DjfsResourcePath.cons(1, "/disk/shared"))
                .size(10)
                .build();
        pgGroupDao.insert(group);
        pgGroupDao.increaseSize(id, 100L);

        Option<Group> groupResult = pgGroupDao.find(id);
        Assert.some(110L, groupResult.map(Group::getSize));
    }
}
