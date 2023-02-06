package ru.yandex.market.common.mds.s3.client.service.data;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceLifeTime;
import ru.yandex.market.common.mds.s3.client.service.data.impl.DefaultKeyGenerator;

import static com.jcabi.matchers.RegexMatchers.matchesPattern;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.HISTORY_WITH_LAST;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_EXTENSION;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_FOLDER;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_PART;

/**
 * TODO(vbauer): Добавить тест на отсутствие EXTENSION'a.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class DefaultKeyGeneratorTest {

    private static final String UUID = "([a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12})";
    private static final String DATE = "\\d{4}-\\d{2}-\\d{2}";
    private static final String TIME = "\\d{2}-\\d{2}-\\d{2}";

    private static final DefaultKeyGenerator GENERATOR = new DefaultKeyGenerator();

    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String EXT = "txt";


    @Test
    public void generateCurrent() {
        final ResourceConfiguration conf = createResourceConfiguration();
        final ResourceFileDescriptor fileDescriptor = conf.getFileDescriptor();
        final String generate = GENERATOR.generateLast(fileDescriptor);

        final String name = fileDescriptor.getName();
        final String extension = fileDescriptor.getExtension().orElseThrow(RuntimeException::new);
        final String pattern = name + DELIMITER_FOLDER +
            DefaultKeyGenerator.CURRENT + DELIMITER_PART +
            name + DELIMITER_EXTENSION + extension;

        assertThat(generate, matchesPattern(pattern));
    }

    @Test
    public void generateHistory() {
        final ResourceConfiguration conf = createResourceConfiguration();
        final ResourceFileDescriptor fileDescriptor = conf.getFileDescriptor();

        final String generate = GENERATOR.generateForDate(fileDescriptor, conf.getHistoryFileCompressor(),
                ZonedDateTime.of(2017, 5, 24, 15, 6, 0, 0, ZoneId.systemDefault()));

        final String name = fileDescriptor.getName();
        final String extension = fileDescriptor.getExtension().orElseThrow(RuntimeException::new);

        final String pattern = name + DELIMITER_FOLDER +
            DATE + DELIMITER_FOLDER +
            name + DELIMITER_PART +
            DATE + DELIMITER_PART +
            TIME + DELIMITER_PART +
            UUID + DELIMITER_EXTENSION + extension;

        assertThat(generate, matchesPattern(pattern));
    }


    private ResourceConfiguration createResourceConfiguration() {
        final ResourceFileDescriptor fileDescriptor = ResourceFileDescriptor.create(KEY, EXT);
        final ResourceLifeTime lifeTime = ResourceLifeTime.create(1);

        return ResourceConfiguration.create(BUCKET, HISTORY_WITH_LAST, fileDescriptor, lifeTime);
    }

}
