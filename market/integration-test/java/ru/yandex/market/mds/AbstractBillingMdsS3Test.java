package ru.yandex.market.mds;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.impl.MdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.data.impl.DefaultKeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.mds.s3.client.service.factory.S3ClientFactory;
import ru.yandex.market.common.mds.s3.client.util.KeyUtils;

/**
 * Класс для тестов с MDS в mbi-billing.
 */
public abstract class AbstractBillingMdsS3Test extends FunctionalTest {
    // FIXME: Класс нельзя вынести в mbi-core, потому что он наследует FunctionalTest из mbi-billing
    //        Если уноследовать FunctionalTest из mbi-core, то это будет значить, что
    //        тесты из mbi-core и тесты из mbi-billing, унаследованные от этого класса,
    //        будут работать параллельно с одним и тем же спринговым контекстом.
    //        Так нельзя, потому что тесты меняют поведение моков из контекста, в результате
    //        тесты начнут падать из-за состояния гонки по отношению к mock'ам и spy-ям.

    // DEV-ключи
    private static final String ACCESS_KEY = "MgIbnT0Tz9tcuAh8Eq35";
    private static final String SECRET_KEY = "LRc394XiU43u+44l9oQEcyKUwmOmhadI3bqbjufV";

    // Тестовая среда MDS S3
    private static final String ENDPOINT = "https://s3.mdst.yandex.net";

    // DEV bucket
    private static final String BUCKET = "market-mbi-dev";

    // Через сколько дней подчищать тестовые данные, которые могли остаться в bucket'e
    private static final int CLEANUP_PERIOD = 3;

    // Префикс тестовой директории
    private static final String PREFIX_TEST = "test";

    // Флаг, определяющий запускался или нет механизм дочистки в рамках запуска тестов
    private static boolean cleanedUpOldData = false;


    protected MdsS3Client mdsS3Client;
    protected ResourceLocationFactory locationFactory;
    protected DefaultKeyGenerator keyGenerator;


    @Before
    public void onBefore() {
        final AmazonS3 amazonS3 = S3ClientFactory.create(ACCESS_KEY, SECRET_KEY, ENDPOINT);

        keyGenerator = new DefaultKeyGenerator();
        mdsS3Client = new MdsS3ClientImpl(amazonS3);
        locationFactory = ResourceLocationFactory.create(BUCKET, generatePathPrefix());

        cleanUpOldTestFiles();
    }

    @After
    public void onAfter() {
        final ResourceLocation all = locationFactory.createLocation(StringUtils.EMPTY);
        mdsS3Client.deleteUsingPrefix(all);
    }

    private String generatePathPrefix() {
        final long time = System.currentTimeMillis();
        final UUID uuid = UUID.randomUUID();
        return PREFIX_TEST + KeyGenerator.DELIMITER_FOLDER + time + KeyGenerator.DELIMITER_FOLDER + uuid;
    }

    private void cleanUpOldTestFiles() {
        if (!cleanedUpOldData) {
            cleanedUpOldData = true;

            final ResourceLocation location = ResourceLocation.create(BUCKET, PREFIX_TEST);
            final ResourceListing listing = mdsS3Client.list(location, false);
            final List<String> prefixes = listing.getPrefixes();

            for (final String key : prefixes) {
                final long current = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());
                try {
                    final String timestamp = KeyUtils.cleanKey(StringUtils.removeStart(key, PREFIX_TEST));
                    final long time = TimeUnit.MILLISECONDS.toDays(Long.parseLong(timestamp));
                    if (current - time >= CLEANUP_PERIOD) {
                        mdsS3Client.deleteUsingPrefix(ResourceLocation.create(BUCKET, key));
                    }
                } catch (final Exception ignored) {
                }
            }
        }
    }

}
