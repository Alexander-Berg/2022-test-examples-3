package ru.yandex.market.common.mds.s3.client.service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Iterables;

import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;
import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProvider;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3NotFoundException;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_FOLDER;
import static ru.yandex.market.common.mds.s3.client.test.RandUtils.createText;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.todaysRandomFolder;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class ClientCRUDIntegrationTest extends AbstractIntegrationTest {

    private static final int DELETE_ATTEMPTS = 3;

    @Test
    void testUnknownFile() {
        final String key = RandUtils.randomText() + "IAmNotExisted";
        final ResourceLocation location = location(todaysRandomFolder(), key);

        assertFalse(mdsS3Client.contains(location));
        assertThrows(MdsS3NotFoundException.class, () -> mdsS3Client.download(location, ContentConsumerFactory.text()));
    }

    @Test
    void testCRUD() {

        String file = UUID.randomUUID().toString();

        final TextContentProvider text = createText();
        String folder = todaysRandomFolder();
        final ResourceLocation location = location(folder, file);

        mdsS3Client.upload(location, text);
        final String receive = mdsS3Client.download(location, ContentConsumerFactory.text());
        assertEquals(receive, text.getText());

        checkListing(folder, file, 1, false);
        checkListing(folder, file, 1, true);

        // XXX(vbauer): Удаление несуществующего объекта из S3 не должно приводить к исключительной ситуации
        IntStream.range(0, DELETE_ATTEMPTS).forEach(attempt -> {
            mdsS3Client.delete(location);
            assertFalse(mdsS3Client.contains(location));
        });
    }

    @Test
    void testUpdateFile() {

        String file = UUID.randomUUID().toString();

        final TextContentProvider text1 = createText();
        final TextContentProvider text2 = createText();
        final ResourceLocation location = location(todaysRandomFolder(), file);

        mdsS3Client.upload(location, text1);
        mdsS3Client.upload(location, text2);

        final String receive = mdsS3Client.download(location, ContentConsumerFactory.text());
        assertEquals(receive, text2.getText());
    }


    private void checkListing(String folder, String file, int size, boolean recursive) {
        final ResourceListing listing = mdsS3Client.list(location(folder), recursive);

        final List<String> keys = listing.getKeys();
        assertEquals(keys.size(), size);

        final String key = Iterables.firstOf(keys);
        assertEquals(key, folder + DELIMITER_FOLDER + file);
    }

    /**
     * Тест удаляет папки созданные не сегодня,
     * чтобы собрать мусор, оставленный тестами, в случае падения ci.
     */
    @Test
    void cleanUp() {
        ResourceLocation root = location(StringUtils.EMPTY);
        ResourceListing listing = mdsS3Client.list(root, false);
        String today = ZonedDateTime.now().toLocalDate().toString();
        for (String prefix : listing.getPrefixes()) {
            if (!prefix.equals(today + DELIMITER_FOLDER)) {
                ResourceLocation location = location(prefix);
                mdsS3Client.deleteUsingPrefix(location);
            }
        }
    }

}
