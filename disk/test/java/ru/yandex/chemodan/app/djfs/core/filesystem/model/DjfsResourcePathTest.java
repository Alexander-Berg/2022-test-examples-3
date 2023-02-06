package ru.yandex.chemodan.app.djfs.core.filesystem.model;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.exception.InvalidDjfsResourcePathException;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class DjfsResourcePathTest {
    @Test
    public void consDiskRootFullpath() {
        DjfsResourcePath result = DjfsResourcePath.cons("123456:/disk");
        Assert.equals("123456", result.getUid().asString());
        Assert.equals(DjfsResourceArea.DISK, result.getArea());
        Assert.equals("/disk", result.getPath());
    }

    @Test
    public void consDiskFullpath() {
        DjfsResourcePath result = DjfsResourcePath.cons("123456:/disk/file");
        Assert.equals("123456", result.getUid().asString());
        Assert.equals(DjfsResourceArea.DISK, result.getArea());
        Assert.equals("/disk/file", result.getPath());
    }

    @Test
    public void consTrashFullpath() {
        DjfsResourcePath result = DjfsResourcePath.cons("123456:/trash/file");
        Assert.equals("123456", result.getUid().asString());
        Assert.equals(DjfsResourceArea.TRASH, result.getArea());
        Assert.equals("/trash/file", result.getPath());
    }

    @Test
    public void consNarodFullpath() {
        DjfsResourcePath result = DjfsResourcePath.cons("123456:/lnarod/file");
        Assert.equals("123456", result.getUid().asString());
        Assert.equals(DjfsResourceArea.NAROD, result.getArea());
        Assert.equals("/lnarod/file", result.getPath());
    }

    @Test
    public void consShareProduction() {
        DjfsResourcePath result = DjfsResourcePath.cons("share_production:/share/dist/test");
        Assert.equals("share_production", result.getUid().asString());
        Assert.assertSame(DjfsUid.SHARE_PRODUCTION, result.getUid());
        Assert.equals(DjfsResourceArea.DISK, result.getArea());
        Assert.equals("/share/dist/test", result.getPath());
    }

    @Test
    public void consFullpathWithTrailingSlash() {
        DjfsResourcePath result = DjfsResourcePath.cons("123456:/disk/folder/");
        Assert.equals("123456", result.getUid().asString());
        Assert.equals(DjfsResourceArea.DISK, result.getArea());
        Assert.equals("/disk/folder", result.getPath());
    }

    @Test
    public void getMongoId() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk");
        Assert.equals("2b6ff469e1802a5aa74018e9c3b6ac80", path.getMongoId());
    }

    @Test
    public void isRoot() {
        Assert.isTrue(DjfsResourcePath.root("123456", DjfsResourceArea.DISK).isRoot());
        Assert.isFalse(DjfsResourcePath.areaRoot("123456", DjfsResourceArea.DISK).isRoot());
        Assert.isFalse(DjfsResourcePath.cons("123456", "/disk/folder").isRoot());
    }

    @Test
    public void isAreaRoot() {
        Assert.isFalse(DjfsResourcePath.root("123456", DjfsResourceArea.DISK).isAreaRoot());
        Assert.isTrue(DjfsResourcePath.areaRoot("123456", DjfsResourceArea.DISK).isAreaRoot());
        Assert.isTrue(DjfsResourcePath.cons("123456", "/disk").isAreaRoot());
        Assert.isTrue(DjfsResourcePath.cons("123456", "/disk/").isAreaRoot());
        Assert.isFalse(DjfsResourcePath.cons("123456", "/disk/folder").isAreaRoot());
    }

    @Test
    public void getParent() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/some/folders");
        Assert.equals(DjfsResourcePath.cons("123456:/disk/some"), path.getParent());
    }

    @Test
    public void getAreaRootParent() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk");
        Assert.equals(DjfsResourcePath.root("123456", DjfsResourceArea.DISK), path.getParent());
    }

    @Test
    public void getAllParents() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/attach/some/f o l/d e r s");
        ListF<DjfsResourcePath> expected = Cf.arrayList(
                DjfsResourcePath.root("123456", DjfsResourceArea.ATTACH),
                DjfsResourcePath.areaRoot("123456", DjfsResourceArea.ATTACH),
                DjfsResourcePath.cons("123456:/attach/some"),
                DjfsResourcePath.cons("123456:/attach/some/f o l")
        );
        Assert.assertListsEqual(expected, path.getAllParents());
    }

    @Test
    public void getAllParentsForRoot() {
        DjfsResourcePath path = DjfsResourcePath.root("123456", DjfsResourceArea.DISK);
        Assert.assertListsEqual(Cf.list(), path.getAllParents());
    }

    @Test
    public void getAllParentsForAreaRoot() {
        DjfsResourcePath path = DjfsResourcePath.areaRoot("123456", DjfsResourceArea.DISK);
        Assert.assertListsEqual(Cf.list(DjfsResourcePath.root("123456", DjfsResourceArea.DISK)), path.getAllParents());
    }

    @Test
    public void isParentFor() {
        Assert.isTrue(DjfsResourcePath.cons("1234:/disk/a").isParentFor(DjfsResourcePath.cons("1234:/disk/a/b")));
        Assert.isTrue(DjfsResourcePath.cons("1234:/disk/a").isParentFor(DjfsResourcePath.cons("1234:/disk/a/b/c/d")));
        Assert.isFalse(DjfsResourcePath.cons("1234:/disk/a").isParentFor(DjfsResourcePath.cons("1234:/disk/a")));
        Assert.isFalse(DjfsResourcePath.cons("1234:/disk/a").isParentFor(DjfsResourcePath.cons("2222:/disk/a/b")));
        Assert.isFalse(DjfsResourcePath.cons("1234:/disk/a").isParentFor(DjfsResourcePath.cons("1234:/disk/aa")));
        Assert.isFalse(DjfsResourcePath.cons("1234:/disk/a").isParentFor(DjfsResourcePath.cons("1234:/disk/aa/b")));
        Assert.isFalse(DjfsResourcePath.cons("1234:/disk/a").isParentFor(DjfsResourcePath.cons("1234:/trash/a/b")));

        Assert.isTrue(DjfsResourcePath.areaRoot("1234", DjfsResourceArea.DISK)
                .isParentFor(DjfsResourcePath.cons("1234:/disk/aa/b")));
        Assert.isTrue(DjfsResourcePath.root("1234", DjfsResourceArea.DISK)
                .isParentFor(DjfsResourcePath.cons("1234:/disk/aa/b")));
        Assert.isFalse(DjfsResourcePath.areaRoot("1234", DjfsResourceArea.TRASH)
                .isParentFor(DjfsResourcePath.cons("1234:/disk/aa/b")));
        Assert.isFalse(DjfsResourcePath.root("1234", DjfsResourceArea.TRASH)
                .isParentFor(DjfsResourcePath.cons("1234:/disk/aa/b")));
    }

    @Test
    public void changeArea() {
        DjfsResourcePath path = DjfsResourcePath.cons("11:/disk/cur/rent/folder/file.txt");
        DjfsResourcePath expected = DjfsResourcePath.cons("11:/hidden/cur/rent/folder/file.txt");
        Assert.equals(expected, path.changeArea(DjfsResourceArea.HIDDEN));
    }

    @Test
    public void changeParent() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/cur/rent/folder/file.txt");
        DjfsResourcePath currentPrefix = DjfsResourcePath.cons("123456:/disk/cur/rent");
        DjfsResourcePath newPrefix = DjfsResourcePath.cons("2222:/disk/new");
        DjfsResourcePath expected = DjfsResourcePath.cons("2222:/disk/new/folder/file.txt");
        Assert.equals(expected, path.changeParent(currentPrefix, newPrefix));
    }

    @Test
    public void changeParentWithRootCurrentPrefix() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/cur/rent/folder/file.txt");
        DjfsResourcePath currentPrefix = DjfsResourcePath.root("123456", DjfsResourceArea.DISK);
        DjfsResourcePath newPrefix = DjfsResourcePath.cons("123456:/hidden/fdr");
        DjfsResourcePath expected = DjfsResourcePath.cons("123456:/hidden/fdr/disk/cur/rent/folder/file.txt");
        Assert.equals(expected, path.changeParent(currentPrefix, newPrefix));
    }

    @Test
    public void getChildPath() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/a/b");
        DjfsResourcePath expected = DjfsResourcePath.cons("123456:/disk/a/b/child");
        Assert.equals(expected, path.getChildPath("child"));
    }

    @Test
    public void getChildPathForRoot() {
        DjfsResourcePath path = DjfsResourcePath.root("123456", DjfsResourceArea.PHOTOUNLIM);
        DjfsResourcePath expected = DjfsResourcePath.cons("123456:/photounlim");
        Assert.equals(expected, path.getChildPath("photounlim"));
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void getChildPathForRootThrowsExceptionOnNonAreaChild() {
        DjfsResourcePath path = DjfsResourcePath.root("123456", DjfsResourceArea.PHOTOUNLIM);
        DjfsResourcePath expected = DjfsResourcePath.cons("123456:/photounlim");
        path.getChildPath("disk");
    }

    @Test
    public void getChildPathForAreaRoot() {
        DjfsResourcePath path = DjfsResourcePath.areaRoot("123456", DjfsResourceArea.PHOTOUNLIM);
        DjfsResourcePath expected = DjfsResourcePath.cons("123456:/photounlim/child");
        Assert.equals(expected, path.getChildPath("child"));
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void getChildPathThrowsExceptionOnNameWithSeparator() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/a");
        path.getChildPath("b/c");
    }

    @Test
    public void getMultipleChildPath() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/a/b");
        DjfsResourcePath expected = DjfsResourcePath.cons("123456:/disk/a/b/child");
        Assert.equals(expected, path.getMultipleChildPath("child"));
    }

    @Test
    public void getMultipleChildPathEndingWithDelimeter() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/a/b");
        DjfsResourcePath expected = DjfsResourcePath.cons("123456:/disk/a/b/child");
        Assert.equals(expected, path.getMultipleChildPath("child/"));
    }

    @Test
    public void getMultipleChildPathWithDelimeter() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/a/b");
        DjfsResourcePath expected = DjfsResourcePath.cons("123456:/disk/a/b/c/h/i/l/d");
        Assert.equals(expected, path.getMultipleChildPath("c/h/i/l/d"));
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void getChildMultiplePathThrowsExceptionOnPathStartingWithSeparator() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/a");
        path.getMultipleChildPath("/bc");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void getChildMultiplePathThrowsExceptionOnPathWithDoubleSeparator() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/a");
        path.getMultipleChildPath("b//c");
    }

    @Test
    public void getName() {
        DjfsResourcePath path = DjfsResourcePath.cons("123456:/disk/some/folders");
        Assert.equals("folders", path.getName());
    }

    @Test
    public void getRootName() {
        DjfsResourcePath result = DjfsResourcePath.root(DjfsUid.cons(123456), DjfsResourceArea.DISK);
        Assert.equals("", result.getName());
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnNullFullpath() {
        DjfsResourcePath.cons(null);
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnEmptyFullpath() {
        DjfsResourcePath.cons("");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithoutUid() {
        DjfsResourcePath.cons("/disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithoutSeparator() {
        DjfsResourcePath.cons("/disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithEmptyUid() {
        DjfsResourcePath.cons(":/disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithEmptyPath() {
        DjfsResourcePath.cons("123456:");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithEmptyArea() {
        DjfsResourcePath.cons("123456:/");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithInvalidArea() {
        DjfsResourcePath.cons("123456:/invalid/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithRelativePath() {
        DjfsResourcePath.cons("123456:disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithInvalidUid() {
        DjfsResourcePath.cons("qwe:/disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithDoubleDelimeter() {
        DjfsResourcePath.cons("1:/disk//file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnFullpathWithTrailingDoubleDelimeter() {
        DjfsResourcePath.cons("123456:/disk/file//");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnNullUid() {
        DjfsResourcePath.cons((String) null, "/disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnEmptyUid() {
        DjfsResourcePath.cons("", "/disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnEmptyInvalidUid() {
        DjfsResourcePath.cons("qwe", "/disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnNullPath() {
        DjfsResourcePath.cons("123456", null);
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnEmptyPath() {
        DjfsResourcePath.cons("123456", "");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnEmptyArea() {
        DjfsResourcePath.cons("123456", "/");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnInvalidArea() {
        DjfsResourcePath.cons("123456", "/invalid/path");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnRelativePath() {
        DjfsResourcePath.cons("123456", "disk/path");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnNullMpfsUid() {
        DjfsResourcePath.cons((DjfsUid) null, "/disk/file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnPathWithDoubleDelimeter() {
        DjfsResourcePath.cons("123456", "/disk//file");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnPathWithTrailingDoubleDelimeter() {
        DjfsResourcePath.cons("123456", "/disk/file//");
    }

    @Test(expected = InvalidDjfsResourcePathException.class)
    public void exceptionOnPathEqualToFullpath() {
        DjfsResourcePath.cons("123456", "123456:/disk/file//");
    }

    @Test
    public void getExtensionFromFileWithExtension() {
        Assert.equals("txt", DjfsResourcePath.cons("123", "/disk/file.txt").getExtensionByAfterLastDot());
    }

    @Test
    public void getExtensionFromFileWithoutExtension() {
        Assert.equals("", DjfsResourcePath.cons("123", "/disk/file").getExtensionByAfterLastDot());
    }

    @Test
    public void getExtensionFromFileWithExtensionButDotInParentDirectoryName() {
        Assert.equals("", DjfsResourcePath.cons("123", "/disk/init.d/file").getExtensionByAfterLastDot());
    }

    @Test
    public void getExtensionFromFileWithoutExtensionButDotInParentDirectoryName() {
        Assert.equals("txt", DjfsResourcePath.cons("123", "/disk/init.d/file.txt").getExtensionByAfterLastDot());
    }

    @Test
    public void checkNewPathForValid() {
        DjfsResourcePath.root("123", DjfsResourceArea.DISK).checkNewPath();
        DjfsResourcePath.cons("123", "/disk").checkNewPath();
        DjfsResourcePath.cons("123", "/disk/file.txt").checkNewPath();
        DjfsResourcePath.cons("123", "/disk/folder/too").checkNewPath();
        DjfsResourcePath.cons("123", "/hidden/folder/..").checkNewPath();
        DjfsResourcePath.cons("123", "/hidden/folder/.").checkNewPath();
    }

    @Test
    public void checkNewPathForLongName() {
        DjfsResourcePath path = DjfsResourcePath.cons("123", "/disk/folder/" + StringUtils.repeat("a", 1000));
        Assert.assertThrows(path::checkNewPath, InvalidDjfsResourcePathException.class);
    }

    @Test
    public void checkNewPathForLongPath() {
        String part = "/" + StringUtils.repeat("a", 200);
        DjfsResourcePath path = DjfsResourcePath.cons("123", "/disk/folder" + StringUtils.repeat(part, 200));
        Assert.assertThrows(path::checkNewPath, InvalidDjfsResourcePathException.class);
    }

    @Test
    public void checkNewPathForForbiddenNames() {
        ListF<DjfsResourcePath> paths = Cf.list(
                DjfsResourcePath.cons("123", "/disk/."),
                DjfsResourcePath.cons("123", "/disk/.."),
                DjfsResourcePath.cons("123", "/disk/./folder"),
                DjfsResourcePath.cons("123", "/disk/../folder"));

        for (DjfsResourcePath path : paths) {
            Assert.assertThrows(path::checkNewPath, InvalidDjfsResourcePathException.class);
        }
    }

    @Test
    public void countDepthForRoot() {
        DjfsResourcePath path = DjfsResourcePath.cons("123", "/disk").getParent();
        Assert.equals(0, path.getDepth());
    }

    @Test
    public void countDepthForAreaRoot() {
        DjfsResourcePath path = DjfsResourcePath.cons("123", "/disk");
        Assert.equals(1, path.getDepth());
    }

    @Test
    public void countDepthForResource() {
        DjfsResourcePath path = DjfsResourcePath.cons("123", "/disk/1/2/3");
        Assert.equals(4, path.getDepth());
    }

    @Test
    public void countDepthForResourceWithDelimiterInTheEnd() {
        DjfsResourcePath path = DjfsResourcePath.cons("123", "/disk/1/");
        Assert.equals(2, path.getDepth());
    }

    @Test
    public void containsUid() {
        Assert.isTrue(DjfsResourcePath.containsUid("132:/disk"));
    }

    @Test
    public void containsUidForRawPathWithoutUid() {
        Assert.isFalse(DjfsResourcePath.containsUid("/disk"));
    }

    @Test
    public void containsUidForRawPathWithInvalidUid() {
        Assert.isFalse(DjfsResourcePath.containsUid("abc:/disk"));
    }
}
