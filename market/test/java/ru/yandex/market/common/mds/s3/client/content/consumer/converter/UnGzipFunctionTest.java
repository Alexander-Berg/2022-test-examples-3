package ru.yandex.market.common.mds.s3.client.content.consumer.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link UnGzipFunction}
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class UnGzipFunctionTest {

    @Test
    public void applyPositive() throws Exception {
        final String expected = "positive";

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStream gzipOut = new GZIPOutputStream(out);
        IOUtils.write(expected, gzipOut, StandardCharsets.UTF_8);
        gzipOut.close();

        final InputStream in = new ByteArrayInputStream(out.toByteArray());
        final InputStream unGzipIn = new UnGzipFunction().apply(in);
        final String actual = IOUtils.toString(unGzipIn, StandardCharsets.UTF_8);

        assertThat(actual, equalTo(expected));
    }

    @Test(expected = MdsS3Exception.class)
    public void applyNegative() {
        fail(String.valueOf(new UnGzipFunction().apply(null)));
    }

}
