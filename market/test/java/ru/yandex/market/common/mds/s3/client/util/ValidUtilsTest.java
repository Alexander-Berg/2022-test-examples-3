package ru.yandex.market.common.mds.s3.client.util;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link ValidUtils}.
 *
 * @author Vladislav Bauer
 */
public class ValidUtilsTest {

    private static final String ERROR_MESSAGE = "In order to succeed you must fail";


    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(ValidUtils.class);
    }

    @Test(expected = MdsS3Exception.class)
    public void testCheckNegative() {
        ValidUtils.check(false, ERROR_MESSAGE);
    }

    @Test
    public void testCheckNotBlankNegative() {
        final String[] variants = { null, StringUtils.EMPTY, " " };
        for (final String variant : variants) {
            try {
                fail(ValidUtils.checkNotBlank(variant, ERROR_MESSAGE));
            } catch (final MdsS3Exception ignored) {
            }
        }
    }

    @Test
    public void testCheckNotBlankPositive() {
        final String text = "I am not BLAnK";
        final String result = ValidUtils.checkNotBlank(text, text);

        assertThat(result, equalTo(text));
    }

    @Test(expected = MdsS3Exception.class)
    public void testCheckBucketName() {
        fail(ValidUtils.checkBucket(StringUtils.EMPTY));
    }

}
