package ru.yandex.market.logistics.nesu.converter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.enums.FeedStatus;
import ru.yandex.market.logistics.nesu.enums.FileExtension;
import ru.yandex.market.logistics.nesu.enums.FileType;
import ru.yandex.market.logistics.nesu.model.ModelFactory;
import ru.yandex.market.logistics.nesu.model.dto.FeedDto;
import ru.yandex.market.logistics.nesu.model.dto.MdsFileDto;

import static ru.yandex.market.logistics.nesu.model.ModelFactory.FEED_ID;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.FILE_NAME;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.FILE_URL;

class FeedConverterTest extends AbstractContextualTest {
    @Autowired
    private FeedConverter feedConverter;

    @Test
    void convertFeedWithSomeNullFields() {
        FeedDto actual = feedConverter.toApi(ModelFactory.feed()
            .setMdsFile(ModelFactory.mdsFile())
        );
        FeedDto expected = new FeedDto(
            FEED_ID,
            FeedStatus.ACTIVE,
            null,
            MdsFileDto.builder()
                .url(FILE_URL)
                .fileName(FILE_NAME)
                .fileType(FileType.FEED)
                .fileExtension(FileExtension.XML)
                .build()
        );
        assertThatModelEquals(expected, actual);
    }

    @Test
    void convertFeedWithAllFieldsSet() {
        FeedDto actual = feedConverter.toApi(ModelFactory.feed()
            .setMdsFile(ModelFactory.mdsFile())
            .setStatus(FeedStatus.ERROR)
            .setError("test-error")
        );
        FeedDto expected = new FeedDto(
            FEED_ID,
            FeedStatus.ERROR,
            "test-error",
            MdsFileDto.builder()
                .url(FILE_URL)
                .fileName(FILE_NAME)
                .fileType(FileType.FEED)
                .fileExtension(FileExtension.XML)
                .build()
        );
        assertThatModelEquals(expected, actual);
    }
}
