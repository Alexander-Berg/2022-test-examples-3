package ru.yandex.market.common.mds.s3.client.exception;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;

/**
 * Unit-тесты для {@link MdsS3Exception}.
 *
 * @author Vladislav Bauer
 */
public class MdsS3ExceptionTest {

    @Test(expected = MdsS3Exception.class)
    public void testDoOperationRunnableNegative() {
        MdsS3Exception.doOperation((Runnable) () -> {
            throw new RuntimeException();
        });
    }

    @Test(expected = MdsS3Exception.class)
    public void testDoOperationCallableNegative() {
        MdsS3Exception.doOperation(() -> {
            throw new RuntimeException();
        });
    }

    @Test(expected = MdsS3NotFoundException.class)
    public void testDoOperationRunnableNotFound() {
        MdsS3Exception.doOperation((Runnable) () -> {
            final AmazonS3Exception exception = new AmazonS3Exception(StringUtils.EMPTY);
            exception.setStatusCode(HttpStatus.SC_NOT_FOUND);
            throw exception;
        });
    }

    @Test(expected = MdsS3NotFoundException.class)
    public void testDoOperationCallableNotFound() {
        MdsS3Exception.doOperation(() -> {
            final AmazonS3Exception exception = new AmazonS3Exception(StringUtils.EMPTY);
            exception.setStatusCode(HttpStatus.SC_NOT_FOUND);
            throw exception;
        });
    }

}
