package ru.yandex.market.common.mds.s3.client.service;

import java.util.UUID;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.service.factory.S3ClientFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.common.mds.s3.client.test.TestProperties.PROPS;

/**
 * Unit-тесты для проверки, что {@link com.amazonaws.services.s3.AmazonS3} работает так, как мы ожидаем.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class AmazonS3CapabilityTest extends AbstractIntegrationTest {

    @Test
    void wrongAccessKeysPositive() {
        final AmazonS3 wrongAmazon = S3ClientFactory.create(
                PROPS.accessKey(),
                UUID.randomUUID().toString(),
                PROPS.endpoint()
        );
        assertThrows(SdkClientException.class, wrongAmazon::listBuckets);
    }

    @Test
    void trueAccessKeysPositive() {
        final AmazonS3 goodAmazon = S3ClientFactory.create(
            PROPS.accessKey(),
            PROPS.secretKey(),
            PROPS.endpoint()
        );
        assertTrue(goodAmazon.listBuckets().size() > 0);
    }
}
