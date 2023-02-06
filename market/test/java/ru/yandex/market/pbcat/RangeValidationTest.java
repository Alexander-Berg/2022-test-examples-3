package ru.yandex.market.pbcat;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.pbcat.exceptions.WrongOptionsException;

/**
 * Тест проверяет корректность работу при задании границ.
 * @author s-ermakov
 */
public class RangeValidationTest {

    private final static String TEST_SNAP_PB_FILE = "fpcat.pbuf.sn";
    private final static int TEST_SNAP_PB_TOTAL_MESSAGE_SIZE = 84;

    private File snapPbFile;
    private File snapJsonFile;

    @Before
    public void setUp() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        snapPbFile = new File(classLoader.getResource(TEST_SNAP_PB_FILE).getFile());

        snapJsonFile = new File(snapPbFile.getParentFile(), TEST_SNAP_PB_FILE + ".json");
        snapJsonFile.delete();
    }

    @Test
    public void testZeroOffset() throws Exception {
        final int offset = 0;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--offset", String.valueOf(offset)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE, messageResult.messages.size());
        Assert.assertEquals(offset, messageResult.offset.intValue());
        Assert.assertNull(messageResult.size);
        Assert.assertNull(messageResult.totalSize);
    }

    @Test(expected = WrongOptionsException.class)
    public void testNegativeOffset() throws Exception {
        final int offset = -1;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--offset", String.valueOf(offset)
        };

        // act
        ArgsParser argsParser = new ArgsParser();
        ArgsParser.Options options = argsParser.parseCommandLineArguments(args);

        JavaPbcat javaPbcat = new JavaPbcat(options);
        javaPbcat.process();
    }

    @Test
    public void testPositiveOffset() throws Exception {
        final int offset = 5;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--offset", String.valueOf(offset)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE - offset, messageResult.messages.size());
        Assert.assertEquals(offset, messageResult.offset.intValue());
        Assert.assertNull(messageResult.size);
        Assert.assertNull(messageResult.totalSize);
    }

    @Test
    public void testTotalSizeOffset() throws Exception {
        final int offset = TEST_SNAP_PB_TOTAL_MESSAGE_SIZE;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--offset", String.valueOf(offset)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertNull(messageResult.messages);
        Assert.assertEquals(offset, messageResult.offset.intValue());
        Assert.assertNull(messageResult.size);
        Assert.assertNull(messageResult.totalSize);
    }

    @Test
    public void testBigOffset() throws Exception {
        final int offset = TEST_SNAP_PB_TOTAL_MESSAGE_SIZE + 1;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--offset", String.valueOf(offset)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertNull(messageResult.messages);
        Assert.assertEquals(offset, messageResult.offset.intValue());
        Assert.assertNull(messageResult.size);
        Assert.assertNull(messageResult.totalSize);
    }

    @Test
    public void testZeroSize() throws Exception {
        final int size = 0;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--size", String.valueOf(size)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertNull(messageResult.messages);
        Assert.assertNull(messageResult.offset);
        Assert.assertEquals(size, messageResult.size.intValue());
        Assert.assertNull(messageResult.totalSize);
    }

    @Test(expected = WrongOptionsException.class)
    public void testNegativeSize() throws Exception {
        final int size = -1;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--size", String.valueOf(size)
        };

        // act
        ArgsParser argsParser = new ArgsParser();
        ArgsParser.Options options = argsParser.parseCommandLineArguments(args);

        JavaPbcat javaPbcat = new JavaPbcat(options);
        javaPbcat.process();
    }

    @Test
    public void testPositiveSize() throws Exception {
        final int size = 5;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--size", String.valueOf(size)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertEquals(size, messageResult.messages.size());
        Assert.assertNull(messageResult.offset);
        Assert.assertEquals(size, messageResult.size.intValue());
        Assert.assertNull(messageResult.totalSize);
    }

    @Test
    public void testSizeEqualsTotalSize() throws Exception {
        final int size = TEST_SNAP_PB_TOTAL_MESSAGE_SIZE;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--size", String.valueOf(size)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE, messageResult.messages.size());
        Assert.assertNull(messageResult.offset);
        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE, messageResult.size.intValue());
        Assert.assertNull(messageResult.totalSize);
    }

    @Test
    public void testBigSize() throws Exception {
        final int size = TEST_SNAP_PB_TOTAL_MESSAGE_SIZE + 10;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--size", String.valueOf(size)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE, messageResult.messages.size());
        Assert.assertNull(messageResult.offset);
        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE, messageResult.size.intValue());
        Assert.assertNull(messageResult.totalSize);
    }

    @Test
    public void testInRangeOffsetAndSize() throws Exception {
        final int offset = 5;
        final int size = 15;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--offset", String.valueOf(offset),
                "--size", String.valueOf(size)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertEquals(size, messageResult.messages.size());
        Assert.assertEquals(offset, messageResult.offset.intValue());
        Assert.assertEquals(size, messageResult.size.intValue());
        Assert.assertNull(messageResult.totalSize);
    }

    @Test
    public void testOutOfRangeOffsetAndSize() throws Exception {
        final int offset = 5;
        final int size = TEST_SNAP_PB_TOTAL_MESSAGE_SIZE + 15;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--offset", String.valueOf(offset),
                "--size", String.valueOf(size)
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE - offset, messageResult.messages.size());
        Assert.assertEquals(offset, messageResult.offset.intValue());
        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE - offset, messageResult.size.intValue());
        Assert.assertNull(messageResult.totalSize);
    }

    @Test
    public void testTotalSize() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--total-size"
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertNull(messageResult.messages);
        Assert.assertNull(messageResult.offset);
        Assert.assertNull(messageResult.size);
        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE, messageResult.totalSize.intValue());
    }

    @Test
    public void testTotalSizeWithRange() throws Exception {
        final int offset = 5;
        final int size = 15;

        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le",
                "--offset", String.valueOf(offset),
                "--size", String.valueOf(size),
                "--total-size"
        };

        // act
        JavaPbcat.main(args);

        // assert
        MessageResult messageResult;
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            messageResult = gson.fromJson(reader, MessageResult.class);
        }

        Assert.assertNull(messageResult.messages);
        Assert.assertNull(messageResult.offset);
        Assert.assertNull(messageResult.size);
        Assert.assertEquals(TEST_SNAP_PB_TOTAL_MESSAGE_SIZE, messageResult.totalSize.intValue());
    }

    private class MessageResult<T> {
        private List<T> messages;
        private Integer offset;
        private Integer size;
        private Integer totalSize;
    }
}
