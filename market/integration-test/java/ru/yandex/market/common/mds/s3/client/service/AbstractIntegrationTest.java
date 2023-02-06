package ru.yandex.market.common.mds.s3.client.service;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.PureHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.impl.MdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.api.impl.NamedHistoryMdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.api.impl.PureHistoryMdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.service.data.impl.DefaultKeyGenerator;
import ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory;

import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_FOLDER;
import static ru.yandex.market.common.mds.s3.client.test.TestProperties.PROPS;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public abstract class AbstractIntegrationTest {

    protected NamedHistoryMdsS3Client namedHistoryMdsS3Client;
    protected PureHistoryMdsS3Client pureHistoryMdsS3Client;
    protected ResourceConfigurationProvider resourceConfigurationProvider;
    protected KeyGenerator keyGenerator;
    protected MdsS3Client mdsS3Client;
    static String bucketName = PROPS.bucketName();

    @RegisterExtension
    static final S3MockExtension S3_MOCK =
            S3MockExtension.builder()
                    .silent()
                    .withSecureConnection(false)
                    .withInitialBuckets(bucketName)
                    .build();

    @BeforeEach
    public void onBefore() {
        try {
            final AmazonS3 s3Client = S3_MOCK.createS3Client();
            mdsS3Client = new MdsS3ClientImpl(s3Client);
            resourceConfigurationProvider = ResourceConfigurationProviderFactory.create(bucketName, true);
            keyGenerator = new DefaultKeyGenerator();
            pureHistoryMdsS3Client = new PureHistoryMdsS3ClientImpl(mdsS3Client, keyGenerator);
            namedHistoryMdsS3Client = new NamedHistoryMdsS3ClientImpl(
                pureHistoryMdsS3Client, resourceConfigurationProvider
            );

        } catch (final Throwable ex) {
            throw new RuntimeException("Could not configure setup for integration tests", ex);
        }
    }

    public ResourceLocation location(final String folder, final String key) {
        return ResourceLocation.create(bucketName, folder + DELIMITER_FOLDER + key);
    }

    public ResourceLocation location(final String folder) {
        return ResourceLocation.create(bucketName, folder);
    }

}
