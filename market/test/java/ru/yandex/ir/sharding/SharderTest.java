package ru.yandex.ir.sharding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import ru.yandex.misc.dataSize.DataSize;


public class SharderTest {


    public static final String CATEGORY_PATTERN = "^(?:(all_models|parameters)_)(\\d+)\\.pb(.gz)?$";
    @Rule
    public TemporaryFolder tf = new TemporaryFolder();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testRealLikeCategories() throws IOException {
        File indexDir = tf.newFolder("test_models");
        prepareTempCategoryFiles("index_good_input.csv", indexDir);
        Sharder sharder = new Sharder(indexDir, "^(?:(all_models|parameters)_)(\\d+)\\.pb(.gz)?$", 4,
                DataSize.fromKiloBytes(256).toBytes());
        List<Shard> matcherShards = sharder.makeShards();
        System.out.println("REAL MINIMISED INDEX TEST:");
        Assert.assertEquals(135748, matcherShards.get(0).getSizeBytes());
        Assert.assertEquals(135778, matcherShards.get(1).getSizeBytes());
        Assert.assertEquals(135926, matcherShards.get(2).getSizeBytes());
        Assert.assertEquals(135788, matcherShards.get(3).getSizeBytes());
    }

    @Test
    public void exceptionIndexDirNull() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("IndexDir \"null\" path doesn't exist");
        new Sharder((File) null, CATEGORY_PATTERN, 4, DataSize.fromKiloBytes(256).toBytes()).makeShards();
    }

    @Test
    public void exceptionWhenTooBigCategory() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("category's size is bigger than");
        File indexDir = tf.newFolder("test_models");
        prepareTempCategoryFiles("index_too_big_category.csv", indexDir);
        new Sharder(indexDir, CATEGORY_PATTERN, 4, DataSize.fromKiloBytes(256).toBytes()).makeShards();
    }

    @Test
    public void exceptionWhenShardMaxShardIsNotEnough() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("There are too many big categories");
        File indexDir = tf.newFolder("test_models");
        prepareTempCategoryFiles("index_good_input.csv", indexDir);
        new Sharder(indexDir, CATEGORY_PATTERN, 4, DataSize.fromKiloBytes(130).toBytes()).makeShards();

    }

    private void prepareTempCategoryFiles(String resourceFileName, File parent) throws IOException {
        InputStream inputStream = SharderTest.class.getClassLoader().getResourceAsStream(resourceFileName);
        Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(new InputStreamReader(inputStream));
        for (CSVRecord record : records) {
            File newFile = new File(parent, record.get(0));
            RandomAccessFile raf = new RandomAccessFile(newFile, "rw");
            raf.setLength(Long.parseLong(record.get(1)));
            raf.close();
        }
    }

}
