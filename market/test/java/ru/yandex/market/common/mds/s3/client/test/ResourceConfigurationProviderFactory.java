package ru.yandex.market.common.mds.s3.client.test;

import java.time.temporal.ChronoUnit;

import javax.annotation.Nonnull;

import ru.yandex.market.common.mds.s3.client.content.compress.ContentCompressor;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy;
import ru.yandex.market.common.mds.s3.client.model.ResourceLifeTime;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.service.data.impl.ResourceConfigurationProviderImpl;

import static ru.yandex.market.common.mds.s3.client.content.factory.ContentCompressorFactory.gzip;
import static ru.yandex.market.common.mds.s3.client.content.factory.ContentCompressorFactory.none;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.todaysRandomFolder;

/**
 * Фабрика для создания тестовых {@link ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration}.
 * @author Vladislav Bauer
 */
public final class ResourceConfigurationProviderFactory {

    public static final String NAME_HISTORY_ONLY = "name-history-only";
    public static final String NAME_HISTORY_ONLY_GZIP = "name-history-only-gzip";
    public static final String NAME_LAST_ONLY = "name-last-only";
    public static final String NAME_HISTORY_WITH_LAST_25 = "name-history-with-last-25";
    public static final String NAME_HISTORY_WITH_LAST_25_GZIP = "name-history-with-last-25-gzip";

    private static final int TTL_1 = 1;
    private static final int TTL_25 = 25;


    private ResourceConfigurationProviderFactory() {
        throw new UnsupportedOperationException();
    }


    @Nonnull
    public static ResourceConfigurationProvider create(@Nonnull final String bucketName, boolean withFolder) {
        return new ResourceConfigurationProviderImpl(
            createWithHistoryOnly(bucketName, withFolder),
            createWithLastOnly(bucketName, withFolder),
            createWithHistoryAndLast(bucketName, withFolder),
            createWithHistoryOnly(NAME_HISTORY_ONLY_GZIP, bucketName, withFolder, gzip()),
            createWithHistoryAndLast(NAME_HISTORY_WITH_LAST_25_GZIP, bucketName, withFolder, gzip())
        );
    }

    @Nonnull
    public static ResourceConfiguration createWithHistoryAndLast(@Nonnull final String bucketName, boolean withFolder) {
        return createWithHistoryAndLast(NAME_HISTORY_WITH_LAST_25, bucketName, withFolder, none());
    }

    @Nonnull
    public static ResourceConfiguration createWithHistoryAndLast(@Nonnull final String filename,
                                                                 @Nonnull final String bucketName,
                                                                 boolean withFolder,
                                                                 @Nonnull ContentCompressor contentCompressor) {
        final ResourceFileDescriptor fileDescriptor = ResourceFileDescriptor.create(filename, "txt",
                folder(withFolder));
        final ResourceLifeTime lifeTime = ResourceLifeTime.create(ChronoUnit.HOURS, TTL_25);

        return ResourceConfiguration.create(
                bucketName, ResourceHistoryStrategy.HISTORY_WITH_LAST, fileDescriptor, contentCompressor, lifeTime
        );
    }

    @Nonnull
    public static ResourceConfiguration createWithLastOnly(@Nonnull final String bucketName, boolean withFolder) {
        final ResourceFileDescriptor fileDescriptor = ResourceFileDescriptor.create(NAME_LAST_ONLY, "json",
            folder(withFolder));
        return ResourceConfiguration.create(bucketName, ResourceHistoryStrategy.LAST_ONLY, fileDescriptor, null);
    }

    @Nonnull
    public static ResourceConfiguration createWithHistoryOnly(@Nonnull final String bucketName, boolean withFolder) {
        return createWithHistoryOnly(NAME_HISTORY_ONLY, bucketName, withFolder, none());
    }

    @Nonnull
    public static ResourceConfiguration createWithHistoryOnly(@Nonnull final String filename,
                                                              @Nonnull final String bucketName,
                                                              boolean withFolder,
                                                              @Nonnull ContentCompressor contentCompressor
    ) {
        final ResourceFileDescriptor fileDescriptor = ResourceFileDescriptor.create(filename, null,
                folder(withFolder));
        final ResourceLifeTime lifeTime = ResourceLifeTime.create(TTL_1);

        return ResourceConfiguration.create(bucketName, ResourceHistoryStrategy.HISTORY_ONLY, fileDescriptor,
                contentCompressor, lifeTime);
    }

    public static String folder(boolean withFolder) {
        return withFolder ? todaysRandomFolder() : null;
    }

}
