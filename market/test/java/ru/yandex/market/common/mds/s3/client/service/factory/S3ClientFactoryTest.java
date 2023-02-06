package ru.yandex.market.common.mds.s3.client.service.factory;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link S3ClientFactory}.
 *
 * @author Vladislav Bauer
 */
public class S3ClientFactoryTest {

    private static final String ACCESS_KEY = "fake access key";
    private static final String SECRET_KEY = "fake secret key";
    private static final String ENDPOINT = "http://fake.end.point";


    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(S3ClientFactory.class);
    }

    @Test
    public void testCreate() {
        final AmazonS3 client = S3ClientFactory.create(ACCESS_KEY, SECRET_KEY, ENDPOINT);
        assertThat(client, notNullValue());
    }

}
