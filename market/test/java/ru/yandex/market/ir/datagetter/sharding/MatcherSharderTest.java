package ru.yandex.market.ir.datagetter.sharding;

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


public class MatcherSharderTest {

    @Rule
    public TemporaryFolder tf = new TemporaryFolder();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testRealLikeCategories() throws IOException {
        File indexDir = tf.newFolder("test_models");
        prepareTempCategoryFiles("index_good_input.csv", indexDir);
        List<MatcherShard> matcherShards
                = MatcherSharder.makeShards(indexDir, 4, DataSize.fromKiloBytes(256).toBytes());
        System.out.println("REAL MINIMISED INDEX TEST:");
        matcherShards.forEach(o -> System.out.println(o.toString()));
        Assert.assertEquals(135748, matcherShards.get(0).getSizeBytes());
        Assert.assertEquals(135778, matcherShards.get(1).getSizeBytes());
        Assert.assertEquals(135926, matcherShards.get(2).getSizeBytes());
        Assert.assertEquals(135788, matcherShards.get(3).getSizeBytes());
    }

    @Test
    public void exceptionIndexDirNull() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Matcher indexDir path doesn't exist");
        List<MatcherShard> matcherShards
                = MatcherSharder.makeShards(null, 4, DataSize.fromKiloBytes(256).toBytes());
    }

    @Test
    public void exceptionWhenTooBigCategory() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("category's size is bigger than");
        File indexDir = tf.newFolder("test_models");
        prepareTempCategoryFiles("index_too_big_category.csv", indexDir);
        List<MatcherShard> matcherShards
                = MatcherSharder.makeShards(indexDir, 4, DataSize.fromKiloBytes(256).toBytes());
    }

    @Test
    public void exceptionWhenShardMaxShardIsNotEnough() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("There are too many big categories");
        File indexDir = tf.newFolder("test_models");
        prepareTempCategoryFiles("index_good_input.csv", indexDir);
        List<MatcherShard> matcherShards
                = MatcherSharder.makeShards(indexDir, 4, DataSize.fromKiloBytes(130).toBytes());

    }

    private void prepareTempCategoryFiles(String resourceFileName, File parent) throws IOException {
        InputStream inputStream = MatcherSharderTest.class.getClassLoader().getResourceAsStream(resourceFileName);
        Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(new InputStreamReader(inputStream));
        for (CSVRecord record : records) {
            File newFile = new File(parent, record.get(0));
            RandomAccessFile raf = new RandomAccessFile(newFile, "rw");
            raf.setLength(Long.parseLong(record.get(1)));
            raf.close();
        }
    }

}
