package ru.yandex.market.logistics.nesu.service;

import java.io.InputStream;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.enums.FileExtension;
import ru.yandex.market.logistics.nesu.enums.FileType;
import ru.yandex.market.logistics.nesu.service.feed.FeedService;
import ru.yandex.market.logistics.nesu.service.mds.util.MdsS3FilenameGenerator;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

class FeedServiceTest extends AbstractContextualTest {
    @Autowired
    private FeedService feedService;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private MdsS3FilenameGenerator mdsS3FilenameGenerator;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Test
    @DisplayName("Обработка валидного файла фида")
    @DatabaseSetup("/service/feed/before/good-feed-db.xml")
    @ExpectedDatabase(
        value = "/service/feed/after/good-feed-db-table-offer.xml",
        table = "offer",
        query = "select * from offer",
        assertionMode = NON_STRICT
    )
    @ExpectedDatabase(value = "/service/feed/after/good-feed-db.xml", assertionMode = NON_STRICT)
    void processGoodFeedFile() {
        mockMdsS3Client("service/feed/good-feed.xml");
        feedService.processFeedFile(1L);
        verifyMdsS3Client();
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Обработка невалидного файла фида")
    @DatabaseSetup("/service/feed/before/bad-feed-db.xml")
    @ExpectedDatabase(value = "/service/feed/after/bad-feed-db.xml", assertionMode = NON_STRICT)
    void processBadFeedFile() {
        mockMdsS3Client("service/feed/bad-feed.xml");
        feedService.processFeedFile(1L);
        verifyMdsS3Client();
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Ошибка при скачивании файла из хранилища прокидывается выше")
    @DatabaseSetup("/service/feed/before/unable-to-download-feed-db.xml")
    void unableToDownloadFeed() {
        doThrow(new MdsS3Exception("Could not process operation")).when(mdsS3Client).download(any(), any());
        softly.assertThatThrownBy(() -> feedService.processFeedFile(1L)).isInstanceOf(MdsS3Exception.class);
        verifyMdsS3Client();
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DatabaseSetup("/service/feed/before/feed-not-found-db.xml")
    @DisplayName("Фид с указанным id не найден")
    void feedNotFound() {
        softly.assertThatCode(() -> feedService.processFeedFile(1L))
            .doesNotThrowAnyException();
        verifyZeroInteractions(mdsS3Client);
    }

    private void mockMdsS3Client(String responseFilePath) {
        doAnswer(invocation -> {
            StreamCopyContentConsumer<?> consumer = invocation.getArgument(1);
            InputStream is = Objects.requireNonNull(getSystemResourceAsStream(responseFilePath));
            consumer.consume(is);
            return null;
        }).when(mdsS3Client).download(any(), any());
    }

    private void verifyMdsS3Client() {
        verify(mdsS3Client).download(
            eq(createResourceLocation()),
            any(StreamCopyContentConsumer.class)
        );
    }

    private ResourceLocation createResourceLocation() {
        String fileName = mdsS3FilenameGenerator.generateRequestDocumentFileName(FileType.FEED, 1L, FileExtension.XML);
        return resourceLocationFactory.createLocation(fileName);
    }
}
