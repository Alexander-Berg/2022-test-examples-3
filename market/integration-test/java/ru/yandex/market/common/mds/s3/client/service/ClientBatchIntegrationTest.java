package ru.yandex.market.common.mds.s3.client.service;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceLifeTime;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.HISTORY_WITH_LAST;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_FOLDER;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_PART;
import static ru.yandex.market.common.mds.s3.client.test.RandUtils.createText;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.todaysRandomFolder;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class ClientBatchIntegrationTest extends AbstractIntegrationTest {

    private static final String FILE_NAME = "test-file";
    private static final String DATE_1 = "2017-05-10";
    private static final String DATE_2 = "2017-05-11";
    private static final String TIME_1 = "12-30-46";
    private static final String TIME_2 = "12-30-45";


    @Test
    void listVersionDates() {
        String folder = todaysRandomFolder();
        putFiles(folder);

        final ResourceConfiguration configuration = resourceConfiguration(folder);
        final ResourceListing listing = mdsS3Client.list(configuration.toLocation(), false);

        assertEquals(listing.getPrefixes().size(), 2);
    }

    @Test
    void listVersionObjects() {
        String folder = todaysRandomFolder();
        putFiles(folder);

        final ResourceConfiguration configuration = resourceConfiguration(folder);
        final ResourceListing listingDates = mdsS3Client.list(configuration.toLocation(), false);

        final String firstDate = listingDates.getPrefixes().iterator().next();
        final ResourceLocation firstVersionDate = ResourceLocation.create(bucketName, firstDate);
        final ResourceListing listingObjects = mdsS3Client.list(firstVersionDate, true);

        assertEquals(listingObjects.getKeys().size(), 2);
    }

    private void putFiles(String folder) {
        mdsS3Client.upload(ResourceLocation.create(bucketName, folder + DELIMITER_FOLDER + FILE_NAME), createText());
        mdsS3Client.upload(location(DATE_1, TIME_2, folder), createText());
        mdsS3Client.upload(location(DATE_1, TIME_1, folder), createText());
        mdsS3Client.upload(location(DATE_2, TIME_2, folder), createText());
        mdsS3Client.upload(location(DATE_2, TIME_1, folder), createText());
    }

    private ResourceLocation location(final String date, final String time, String folder) {
        final String key = folder + DELIMITER_FOLDER +
                FILE_NAME + DELIMITER_FOLDER +
                date + DELIMITER_FOLDER +
                FILE_NAME + DELIMITER_PART +
                date + DELIMITER_PART + time;

        return ResourceLocation.create(bucketName, key);
    }

    private ResourceConfiguration resourceConfiguration(String folder) {
        final ResourceLifeTime lt = ResourceLifeTime.create(1);
        final ResourceFileDescriptor fd = ResourceFileDescriptor.create(folder + DELIMITER_FOLDER + FILE_NAME);

        return ResourceConfiguration.create(bucketName, HISTORY_WITH_LAST, fd, lt);
    }

}
