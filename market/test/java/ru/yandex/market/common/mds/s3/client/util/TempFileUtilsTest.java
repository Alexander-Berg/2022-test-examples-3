package ru.yandex.market.common.mds.s3.client.util;

import java.io.File;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link TempFileUtils}.
 *
 * @author Vladislav Bauer
 */
public class TempFileUtilsTest {

    private static final String TEST_PREFIX = "prefix";
    private static final String TEST_SUFFIX = "suffix";


    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(TempFileUtils.class);
    }

    @Test
    public void testCreateTempFile() {
        checkFile(TempFileUtils.createTempFile());
    }

    @Test
    public void testCreateTempFileWithPrefixPositive() {
        checkFile(TempFileUtils.createTempFile(TEST_PREFIX));
    }

    @Test(expected = MdsS3Exception.class)
    public void testCreateTempFileWithWithPrefixNegative() {
        checkFile(TempFileUtils.createTempFile(EMPTY));
    }

    @Test
    public void testCreateTempFileWithPrefixAndSuffixPositive() {
        checkFile(TempFileUtils.createTempFile(TEST_PREFIX, TEST_SUFFIX));
    }

    @Test(expected = MdsS3Exception.class)
    public void testCreateTempFileWithWithPrefixAndSuffixNegative() {
        checkFile(TempFileUtils.createTempFile(EMPTY, EMPTY));
    }


    private void checkFile(final File file) {
        assertThat(file, notNullValue());
        assertThat(file.isFile(), equalTo(true));
    }

}
