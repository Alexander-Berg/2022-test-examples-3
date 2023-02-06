package ru.yandex.market.common.mds.s3.client.service;

import java.util.Objects;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.service.factory.S3ClientFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.common.mds.s3.client.test.TestProperties.PROPS;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@Disabled
public class S3ClientFactoryIntegrationTest {

    @Test
    void createPositive() {
        final AmazonS3 client = S3ClientFactory.create(
            PROPS.accessKey(),
            PROPS.secretKey(),
            PROPS.endpoint()
        );
        assertTrue(isConnected(client));
    }

    @Test
    void createNegative() {
        final AmazonS3 client = S3ClientFactory.create(
            PROPS.accessKey(),
            PROPS.secretKey().substring(1),
            PROPS.endpoint()
        );
        assertFalse(isConnected(client));
    }

    private boolean isConnected(final AmazonS3 client) {
        try {
            return Objects.nonNull(client.listBuckets());
        } catch (final Exception ignored) {
            return false;
        }
    }

}
