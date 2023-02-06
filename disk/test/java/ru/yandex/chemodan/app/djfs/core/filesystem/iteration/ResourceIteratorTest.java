package ru.yandex.chemodan.app.djfs.core.filesystem.iteration;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class ResourceIteratorTest extends DjfsSingleUserTestBase {
    @Autowired
    private ResourceIterator.Factory resourceIteratorFactory;

    private static DjfsResourcePath iterationRootPath = DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK);

    private static ListF<String> topDownExpectedIterationOrder = Cf.list(
            "/disk/d0",
            "/disk/d0/d0",
            "/disk/d0/d0/d0",
            "/disk/d0/d0/d1",
            "/disk/d0/d0/f0",
            "/disk/d0/d0/f1",
            "/disk/d0/d1",
            "/disk/d0/d1/d0",
            "/disk/d0/d1/d1",
            "/disk/d0/d1/f0",
            "/disk/d0/d1/f1",
            "/disk/d0/f0",
            "/disk/d0/f1",
            "/disk/d1",
            "/disk/d1/d0",
            "/disk/d1/d0/d0",
            "/disk/d1/d0/d1",
            "/disk/d1/d0/f0",
            "/disk/d1/d0/f1",
            "/disk/d1/d1",
            "/disk/d1/d1/d0",
            "/disk/d1/d1/d1",
            "/disk/d1/d1/f0",
            "/disk/d1/d1/f1",
            "/disk/d1/f0",
            "/disk/d1/f1",
            "/disk/f0",
            "/disk/f1"
    );

    private static ListF<String> bottomUpExpectedIterationOrder = Cf.list(
            "/disk/d0/d0/d0",
            "/disk/d0/d0/d1",
            "/disk/d0/d0/f0",
            "/disk/d0/d0/f1",
            "/disk/d0/d0",
            "/disk/d0/d1/d0",
            "/disk/d0/d1/d1",
            "/disk/d0/d1/f0",
            "/disk/d0/d1/f1",
            "/disk/d0/d1",
            "/disk/d0/f0",
            "/disk/d0/f1",
            "/disk/d0",
            "/disk/d1/d0/d0",
            "/disk/d1/d0/d1",
            "/disk/d1/d0/f0",
            "/disk/d1/d0/f1",
            "/disk/d1/d0",
            "/disk/d1/d1/d0",
            "/disk/d1/d1/d1",
            "/disk/d1/d1/f0",
            "/disk/d1/d1/f1",
            "/disk/d1/d1",
            "/disk/d1/f0",
            "/disk/d1/f1",
            "/disk/d1",
            "/disk/f0",
            "/disk/f1"
    );

    private static String hexFromPath(DjfsResource.Builder resource) {
        return resource.getPath().getPath().replace("/disk", "").replace("/", "");
    }

    private static String hexFromParentPath(DjfsResource.Builder resource) {
        return resource.getPath().getParent().getPath().replace("/disk", "").replace("/", "");
    }

    @Test
    public void topDownIteration() {
        util.fs.initialize(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK), 2, 2, 3,
                x -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0"))),
                x -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0"))));

        ResourceIterator iterator = resourceIteratorFactory.create(iterationRootPath,
                ResourceIterator.TraversalType.TOP_DOWN);

        ResourceIterator.ResourceIteratorState state = iterator.initialize();

        ListF<DjfsResource> actual = Cf.list();
        while (iterator.hasNext(state)) {
            Tuple2<ResourceIterator.ResourceIteratorState, ListF<DjfsResource>> result = iterator.next(state, 1);
            actual = actual.plus(result._2);
            state = result._1;
        }

        Assert.arraysEquals(topDownExpectedIterationOrder.toArray(), actual.map(x -> x.getPath().getPath()).toArray());
    }

    @Test
    public void bottomUpIteration() {
        util.fs.initialize(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK), 2, 2, 3,
                x -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0"))),
                x -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0"))));

        ResourceIterator iterator = resourceIteratorFactory.create(iterationRootPath,
                ResourceIterator.TraversalType.BOTTOM_UP);

        ResourceIterator.ResourceIteratorState state = iterator.initialize();

        ListF<DjfsResource> actual = Cf.list();
        while (iterator.hasNext(state)) {
            Tuple2<ResourceIterator.ResourceIteratorState, ListF<DjfsResource>> result = iterator.next(state, 1);
            actual = actual.plus(result._2);
            state = result._1;
        }

        Assert.arraysEquals(bottomUpExpectedIterationOrder.toArray(), actual.map(x -> x.getPath().getPath()).toArray());
    }

    @Test
    public void topDownIterationWithEqualFileIds() {
        DjfsFileId fileId = DjfsFileId.cons(StringUtils.repeat("a", 64));
        util.fs.initialize(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK), 2, 2, 3,
                x -> x.id(UuidUtils.fromHex(StringUtils.leftPad(hexFromPath(x), 32, "0")))
                        .parentId(x.getPath().getParent().isAreaRoot() ? x.getParentId() : Option
                                .of(UuidUtils.fromHex(StringUtils.leftPad(hexFromParentPath(x), 32, "0"))))
                        .fileId(fileId),
                x -> x.id(UuidUtils.fromHex(StringUtils.leftPad(hexFromPath(x), 32, "0")))
                        .parentId(x.getPath().getParent().isAreaRoot() ? x.getParentId() : Option
                                .of(UuidUtils.fromHex(StringUtils.leftPad(hexFromParentPath(x), 32, "0"))))
                        .fileId(fileId));

        ResourceIterator iterator = resourceIteratorFactory.create(iterationRootPath,
                ResourceIterator.TraversalType.TOP_DOWN);

        ResourceIterator.ResourceIteratorState state = iterator.initialize();

        ListF<DjfsResource> actual = Cf.list();
        while (iterator.hasNext(state)) {
            Tuple2<ResourceIterator.ResourceIteratorState, ListF<DjfsResource>> result = iterator.next(state, 1);
            actual = actual.plus(result._2);
            state = result._1;
        }

        Assert.arraysEquals(topDownExpectedIterationOrder.toArray(), actual.map(x -> x.getPath().getPath()).toArray());
    }

    @Test
    public void bottomUpIterationWithEqualFileIds() {
        DjfsFileId fileId = DjfsFileId.cons(StringUtils.repeat("a", 64));
        util.fs.initialize(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK), 2, 2, 3,
                x -> x.id(UuidUtils.fromHex(StringUtils.leftPad(hexFromPath(x), 32, "0")))
                        .parentId(x.getPath().getParent().isAreaRoot() ? x.getParentId() : Option
                                .of(UuidUtils.fromHex(StringUtils.leftPad(hexFromParentPath(x), 32, "0"))))
                        .fileId(fileId),
                x -> x.id(UuidUtils.fromHex(StringUtils.leftPad(hexFromPath(x), 32, "0")))
                        .parentId(x.getPath().getParent().isAreaRoot() ? x.getParentId() : Option
                                .of(UuidUtils.fromHex(StringUtils.leftPad(hexFromParentPath(x), 32, "0"))))
                        .fileId(fileId));

        ResourceIterator iterator = resourceIteratorFactory.create(iterationRootPath,
                ResourceIterator.TraversalType.BOTTOM_UP);

        ResourceIterator.ResourceIteratorState state = iterator.initialize();

        ListF<DjfsResource> actual = Cf.list();
        while (iterator.hasNext(state)) {
            Tuple2<ResourceIterator.ResourceIteratorState, ListF<DjfsResource>> result = iterator.next(state, 1);
            actual = actual.plus(result._2);
            state = result._1;
        }

        Assert.arraysEquals(bottomUpExpectedIterationOrder.toArray(), actual.map(x -> x.getPath().getPath()).toArray());
    }

    @Test
    public void nextDoesNotReturnPartOfFilesWithSameFileId() {
        DjfsFileId fileId1 = DjfsFileId.cons(StringUtils.repeat("a", 64));
        DjfsFileId fileId2 = DjfsFileId.cons(StringUtils.repeat("b", 64));
        DjfsFileId fileId3 = DjfsFileId.cons(StringUtils.repeat("c", 64));

        for (Tuple2<String, DjfsFileId> tuple : Tuple2List.<String, DjfsFileId>fromPairs(
                "f3", fileId1, "f1", fileId1, "f2", fileId1,
                "f6", fileId2, "f4", fileId2, "f5", fileId2,
                "f9", fileId3, "f7", fileId3, "f8", fileId3)) {

            FileDjfsResource file = FileDjfsResource.random(iterationRootPath.getChildPath(tuple._1),
                    x -> x.id(UuidUtils.fromHex(StringUtils.leftPad(tuple._1, 32, "0"))).fileId(tuple._2));
            djfsResourceDao.insert(file);
        }

        ResourceIterator iterator = resourceIteratorFactory.create(iterationRootPath,
                ResourceIterator.TraversalType.TOP_DOWN);

        Tuple2<ResourceIterator.ResourceIteratorState, ListF<DjfsResource>> result;
        ResourceIterator.ResourceIteratorState state = iterator.initialize().startIteratingFiles();
        result = iterator.next(state, 2);
        Assert.arraysEquals(new String[]{"f1", "f2"}, result._2.map(x -> x.getPath().getName()).toArray());
        result = iterator.next(result._1, 5);
        Assert.arraysEquals(new String[]{"f3", "f4", "f5", "f6", "f7"}, result._2.map(x -> x.getPath().getName()).toArray());
        result = iterator.next(result._1, 2);
        Assert.arraysEquals(new String[]{"f8", "f9"}, result._2.map(x -> x.getPath().getName()).toArray());
    }

    @Test
    public void resumeIterationAfterResourcesWereDeleted() {
        util.fs.initialize(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK), 2, 2, 3,
                x -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0"))),
                x -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0"))));

        ResourceIterator iterator = resourceIteratorFactory.create(iterationRootPath,
                ResourceIterator.TraversalType.TOP_DOWN);

        Tuple2<ResourceIterator.ResourceIteratorState, ListF<DjfsResource>> result;
        ResourceIterator.ResourceIteratorState state = iterator.initialize();

        result = iterator.next(state, 1);
        Assert.arraysEquals(new String[]{"/disk/d0"}, result._2.map(x -> x.getPath().getPath()).toArray());
        result = iterator.next(result._1, 1);
        Assert.arraysEquals(new String[]{"/disk/d0/d0"}, result._2.map(x -> x.getPath().getPath()).toArray());
        result = iterator.next(result._1, 1);
        Assert.arraysEquals(new String[]{"/disk/d0/d0/d0"}, result._2.map(x -> x.getPath().getPath()).toArray());

        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d0/d0"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d0/d1"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d0/f0"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d0/f1"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d1/d0"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d1/d1"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d1/f0"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d1/f1"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d0"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/d1"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/f0"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0/f1"));
        djfsResourceDao.delete(DjfsResourcePath.cons(UID, "/disk/d0"));

        do {
            result = iterator.next(result._1, 1);
        } while (iterator.hasNext(result._1) && result._2.length() == 0);
        Assert.arraysEquals(new String[]{"/disk/d1"}, result._2.map(x -> x.getPath().getPath()).toArray());
    }
}
