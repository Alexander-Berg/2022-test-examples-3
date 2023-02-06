package ru.yandex.market.common.mds.s3.client.service.history;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceLifeTime;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.AbstractIntegrationTest;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.HISTORY_ONLY;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.LAST_ONLY;
import static ru.yandex.market.common.mds.s3.client.service.api.impl.PureHistoryMdsS3ClientImpl.DEFAULT_ZONE_ID;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_FOLDER;
import static ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory.NAME_HISTORY_WITH_LAST_25;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class MdsS3HistoryCleanerIntegrationTest extends AbstractIntegrationTest {

    private static final int LIFE_TIME_5_DAYS = 5;
    private static final int UPLOAD_FILES_COUNT = 5;

    @Test
    void cleanBefore() {
        final ResourceConfiguration conf = resourceConfigurationProvider.getByName(NAME_HISTORY_WITH_LAST_25);
        storeFiles(conf);

        final ZonedDateTime now = ZonedDateTime.now(DEFAULT_ZONE_ID);
        final ResourceFileDescriptor fileDescriptor = conf.getFileDescriptor();
        final String barrier = keyGenerator.generateForDate(fileDescriptor, conf.getHistoryFileCompressor(), now);

        /* удалить все, что раньше 25 часов */
        final String confName = fileDescriptor.getFolder().get() + DELIMITER_FOLDER + fileDescriptor.getName();
        final ResourceListing deleted = namedHistoryMdsS3Client.deleteOld(confName);
        final ResourceListing restObjects = mdsS3Client.list(location(confName), true);

        /* проверяем результат удаления */
        assertEquals(restObjects.getKeys().size(), 2 + 1); /* two history + current */
        for (String deletedPrefix : deleted.getPrefixes()) {
            assertTrue(deletedPrefix.compareTo(barrier) < 0);
        }

        for (final String key : deleted.getKeys()) {
            assertTrue(key.compareTo(barrier) < 0);
        }

        /* проверяем, что файлов больше нет */
        for (final String prefix : deleted.getPrefixes()) {
            final ResourceListing list = mdsS3Client.list(location(prefix), true);
            assertEquals(list.getKeys().size(), 0);
        }
        for (final String key : deleted.getKeys()) {
            assertFalse(mdsS3Client.contains(ResourceLocation.create(conf.getBucketName(), key)));
        }
    }

    @Test
    void cleanBeforeChangeHistoryToLast() {
        final ResourceConfiguration originConf = resourceConfigurationProvider.getByName(NAME_HISTORY_WITH_LAST_25);
        storeFiles(originConf);

        final ResourceFileDescriptor fileDescriptor = originConf.getFileDescriptor();
        final ResourceConfiguration newConfiguration = ResourceConfiguration.create(
                originConf.getBucketName(), LAST_ONLY, fileDescriptor, null
        );

        final ResourceListing deleted = pureHistoryMdsS3Client.deleteOld(newConfiguration);
        final ResourceListing restObjects =
                mdsS3Client.list(location(fileDescriptor.getFolder().get(), fileDescriptor.getName()), true);

        assertEquals(deleted.getKeys().size(), 1); /* удалил сегодняшний файл */
        assertEquals(deleted.getPrefixes().size(), 2); /* удалил вчера и позавчера */

        assertEquals(restObjects.getKeys().size(), 1); /* остался только current */
    }

    @Test
    void cleanBeforeChangeLastToHistory() {
        final ResourceConfiguration originConf = resourceConfigurationProvider.getByName(NAME_HISTORY_WITH_LAST_25);
        final ResourceFileDescriptor fileDescriptor = originConf.getFileDescriptor();

        storeFiles(originConf);

        final ResourceLifeTime lifeTime = ResourceLifeTime.create(LIFE_TIME_5_DAYS);
        final ResourceConfiguration newConfiguration = ResourceConfiguration.create(
                originConf.getBucketName(), HISTORY_ONLY, fileDescriptor, lifeTime
        );

        final ResourceListing deleted = pureHistoryMdsS3Client.deleteOld(newConfiguration);

        final ResourceListing restObjects =
                mdsS3Client.list(location(fileDescriptor.getFolder().get(), fileDescriptor.getName()), true);

        assertEquals(deleted.getKeys().size(), 1); /* удалил только current */
        assertEquals(deleted.getPrefixes().size(), 0); /* не удалял историю */

        assertEquals(restObjects.getKeys().size(), 1 + 2 + 1); /* остался вся история */
    }


    private void storeFiles(final ResourceConfiguration conf) {
        final TextContentProvider text = RandUtils.createText();
        final ResourceFileDescriptor fileDescriptor = conf.getFileDescriptor();

        /* upload last */
        mdsS3Client.upload(location(keyGenerator.generateLast(fileDescriptor)), text);
        /* upload some history */
        final int hoursInADay = (int) TimeUnit.HOURS.convert(1, TimeUnit.DAYS);
        /* сегодня */
        mdsS3Client.upload(location(keyBeforeHours(conf, 0)), text);
        /* позавчера */
        mdsS3Client.upload(location(keyBeforeHours(conf, hoursInADay * 2)), text);
        /* вчера - 2 час */
        mdsS3Client.upload(location(keyBeforeHours(conf, hoursInADay + 2)), text);
        /* вчера */
        mdsS3Client.upload(location(keyBeforeHours(conf, hoursInADay)), text);

        String path = fileDescriptor.getFolder().get() + DELIMITER_FOLDER + fileDescriptor.getName();

        final ResourceListing originObjects = mdsS3Client.list(location(path), true);

        assertTrue(originObjects.getPrefixes().isEmpty());
        assertEquals(UPLOAD_FILES_COUNT, originObjects.getKeys().size());
    }

    private String keyBeforeHours(ResourceConfiguration conf, int hours) {
        final ResourceFileDescriptor fileDescriptor = conf.getFileDescriptor();
        final ZonedDateTime time = beforeHours(hours);

        return keyGenerator.generateForDate(fileDescriptor, conf.getHistoryFileCompressor(), time);
    }

    private ZonedDateTime beforeHours(final int hours) {
        return ZonedDateTime.now(DEFAULT_ZONE_ID).minus(hours, ChronoUnit.HOURS);
    }

}
