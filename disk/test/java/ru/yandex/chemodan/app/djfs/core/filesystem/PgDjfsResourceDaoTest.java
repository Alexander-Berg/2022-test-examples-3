package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.misc.geo.Coordinates;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

/**
 * @author yappo
 */
public class PgDjfsResourceDaoTest extends DjfsTestBase {
    private static final DjfsUid UID = DjfsUid.cons("12345");
    private static final DjfsResourcePath PATH = DjfsResourcePath.cons(UID, "/disk/test");

    @Before
    public void setUp() {
        super.setUp();
        initializeUser(UID, 0);
    }

    @Test
    public void sizeGreaterThanInt() {
        JdbcTemplate3 shard = pgShardResolver.resolve(UID);

        Long size = 2527261358L;
        filesystem.createFile(DjfsPrincipal.SYSTEM, PATH);
        String q = "UPDATE disk.storage_files as s"
                + " SET size=" + size.toString()
                + " FROM disk.files as f"
                + " WHERE f.storage_id = s.storage_id";
        shard.execute(q);

        FileDjfsResource file = (FileDjfsResource) djfsResourceDao.find(PATH).get();
        Assert.equals(size, file.getSize());
    }

    @Test
    public void deserializeMd5WithoutDashes() {
        filesystem.createFile(DjfsPrincipal.SYSTEM, PATH);
        FileDjfsResource file = (FileDjfsResource) djfsResourceDao.find(PATH).get();
        Assert.isFalse(file.getMd5().contains("-"));
    }

    @Test
    public void deserializSha256() {
        JdbcTemplate3 shard = pgShardResolver.resolve(UID);

        filesystem.createFile(DjfsPrincipal.SYSTEM, PATH);
        String q = "UPDATE disk.storage_files as s"
                + " SET sha256_sum='\\xcb2dd1735ec8871980d748ac4f7170ebaade2b98a40a92d11aefa3f973037ee0'"
                + " FROM disk.files as f"
                + " WHERE f.storage_id = s.storage_id";
        shard.execute(q);

        FileDjfsResource file = (FileDjfsResource) djfsResourceDao.find(PATH).get();
        Assert.equals("cb2dd1735ec8871980d748ac4f7170ebaade2b98a40a92d11aefa3f973037ee0", file.getSha256());
    }

    @Test
    public void folderCustomSetpropFieldsIsNull() {
        JdbcTemplate3 template = pgShardResolver.resolve(UID);

        FolderDjfsResource folder = filesystem.createFolder(DjfsPrincipal.SYSTEM, PATH);
        String q = "UPDATE disk.folders"
                + " SET custom_setprop_fields = NULL"
                + " WHERE fid = :fid";
        int update = template.update(q, Cf.map("fid", folder.getId()));
        Assert.equals(1, update);

        FolderDjfsResource actual = (FolderDjfsResource) djfsResourceDao.find(PATH).get();
        Assert.isEmpty(actual.getExternalProperties());
    }

    @Test
    public void folderCustomSetpropFieldsHasTotalResulsCount() {
        JdbcTemplate3 template = pgShardResolver.resolve(UID);

        FolderDjfsResource folder = filesystem.createFolder(DjfsPrincipal.SYSTEM, PATH);
        String q = "UPDATE disk.folders"
                + " SET custom_setprop_fields = '{\"total_results_count\": 37}'::json"
                + " WHERE fid = :fid";
        int update = template.update(q, Cf.map("fid", folder.getId()));
        Assert.equals(1, update);

        FolderDjfsResource actual = (FolderDjfsResource) djfsResourceDao.find(PATH).get();
        Assert.isEmpty(actual.getExternalProperties());
    }

    @Test
    public void fileCustomSetpropFieldsIsNull() {
        JdbcTemplate3 template = pgShardResolver.resolve(UID);

        FileDjfsResource file = filesystem.createFile(DjfsPrincipal.SYSTEM, PATH);
        String q = "UPDATE disk.files"
                + " SET custom_setprop_fields = NULL"
                + " WHERE fid = :fid";
        int update = template.update(q, Cf.map("fid", file.getId()));
        Assert.equals(1, update);

        FileDjfsResource actual = (FileDjfsResource) djfsResourceDao.find(PATH).get();
        Assert.isEmpty(actual.getExternalProperties());
    }

    @Test
    public void fileCustomSetpropFieldsHasTotalResulsCount() {
        JdbcTemplate3 template = pgShardResolver.resolve(UID);

        FileDjfsResource file = filesystem.createFile(DjfsPrincipal.SYSTEM, PATH);
        String q = "UPDATE disk.files"
                + " SET custom_setprop_fields = '{\"total_results_count\": 37}'::json"
                + " WHERE fid = :fid";
        int update = template.update(q, Cf.map("fid", file.getId()));
        Assert.equals(1, update);

        FileDjfsResource actual = (FileDjfsResource) djfsResourceDao.find(PATH).get();
        Assert.isEmpty(actual.getExternalProperties());
    }

    @Test
    public void createFileWithCoordinates() {
        double latitude = 123.321;
        double longitude = 53.123;

        filesystem.createFile(DjfsPrincipal.SYSTEM, PATH, x -> x.coordinates(new Coordinates(latitude, longitude)));

        FileDjfsResource file = (FileDjfsResource) djfsResourceDao.find(PATH).get();
        Assert.some(file.getCoordinates());

        Coordinates coordinates = file.getCoordinates().get();
        Assert.equals(latitude, coordinates.getLatitude());
        Assert.equals(longitude, coordinates.getLongitude());
    }

    @Test
    public void testSaveCoordinates() {
        FileDjfsResource fileWithoutCoordinates1 = filesystem.createFile(
                DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/disk/file-1.txt"),
                x -> x.coordinates(Option.empty()));
        FileDjfsResource fileWithoutCoordinates2 = filesystem.createFile(
                DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/disk/file-2.txt"),
                x -> x.coordinates(Option.empty()));
        FileDjfsResource fileWithCoordinates = filesystem.createFile(
                DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/disk/file-3.txt"),
                x -> x.coordinates(new Coordinates(10, 20)));

        DjfsUid anotherUid = DjfsUid.cons("100500");
        initializeUser(anotherUid, sharpeiShardResolver.shardByUid(UID).get().getShardId());
        FileDjfsResource anotherUserFile = filesystem.createFile(
                DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(anotherUid, "/disk/file-4.txt"),
                x -> x.coordinates(new Coordinates(1, 42)));

        Coordinates coordinates1 = new Coordinates(21.23451, 45.1234);
        Coordinates coordinates2 = new Coordinates(24.45212, 51.4312);
        Coordinates coordinates3 = new Coordinates(26.12431, 11.4522);
        Tuple2List<DjfsFileId, Coordinates> fileId2Coordinates = Tuple2List.fromPairs(
                fileWithoutCoordinates1.getFileId().get(), coordinates1,
                fileWithoutCoordinates2.getFileId().get(), coordinates2,
                fileWithCoordinates.getFileId().get(), coordinates3
        );

        djfsResourceDao.setCoordinates(UID, fileId2Coordinates, true);

        FileDjfsResource file1 = (FileDjfsResource) djfsResourceDao.find(fileWithoutCoordinates1.getPath()).get();
        Assert.some(coordinates1, file1.getCoordinates());
        FileDjfsResource file2 = (FileDjfsResource) djfsResourceDao.find(fileWithoutCoordinates2.getPath()).get();
        Assert.some(coordinates2, file2.getCoordinates());
        FileDjfsResource file3 = (FileDjfsResource) djfsResourceDao.find(fileWithCoordinates.getPath()).get();
        Assert.some(fileWithCoordinates.getCoordinates().get(), file3.getCoordinates());
        FileDjfsResource file4 = (FileDjfsResource) djfsResourceDao.find(anotherUserFile.getPath()).get();
        Assert.some(anotherUserFile.getCoordinates().get(), file4.getCoordinates());

        djfsResourceDao.setCoordinates(UID, fileId2Coordinates, false);

        file1 = (FileDjfsResource) djfsResourceDao.find(fileWithoutCoordinates1.getPath()).get();
        Assert.some(coordinates1, file1.getCoordinates());
        file2 = (FileDjfsResource) djfsResourceDao.find(fileWithoutCoordinates2.getPath()).get();
        Assert.some(coordinates2, file2.getCoordinates());
        file3 = (FileDjfsResource) djfsResourceDao.find(fileWithCoordinates.getPath()).get();
        Assert.some(coordinates3, file3.getCoordinates());
        file4 = (FileDjfsResource) djfsResourceDao.find(anotherUserFile.getPath()).get();
        Assert.some(anotherUserFile.getCoordinates().get(), file4.getCoordinates());
    }
}
