package ru.yandex.market.core.upload;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.common.framework.core.RemoteFile;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.core.upload.model.FileUpload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты на логику работы {@link FileUploadService}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "fileUploadService.csv")
class FileUploadServiceTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private FeedFileStorage feedFileStorage;

    @BeforeEach
    void initMock() throws IOException {
        when(feedFileStorage.upload(any(RemoteFile.class), anyLong()))
                .thenReturn(new StoreInfo(100500, "mds.url"));
    }

    /**
     * Тест на {@link FileUploadService#uploadFile(MultipartFile, long) загрузку ассортимента} в файловое
     * хранилище.
     */
    @Test
    @DbUnitDataSet(after = "testFileUpload.after.csv")
    void testFeedUpload() {
        FileUpload feedUpload = fileUploadService.uploadFile(mockMultipartFile(), SUPPLIER_ID);
        ReflectionAssert.assertReflectionEquals(
                getExpectedSupplierFeed(feedUpload.getUploadDateTime()),
                feedUpload
        );
    }

    /**
     * Тест на получение информации о загруженном файле по идентификатору загрузки.
     */
    @Test
    @DbUnitDataSet(before = "testGetFileUpload.csv")
    void testGetFeedUpload() {
        FileUpload actual = fileUploadService.getFile(10L);
        FileUpload expected = new FileUpload.Builder()
                .setSize(9L)
                .setName("supplier_feed.xls")
                .setPartnerId(SUPPLIER_ID)
                .setUploadDate(LocalDateTime.of(LocalDate.of(2018, 1, 20), LocalTime.of(0, 0)))
                .setUrl("https://market-mbi-dev.s3.mdst.yandex.net/upload-feed/843/upload-feed-1216842")
                .setId(10L)
                .setOriginalUploadId(0L)
                .build();
        ReflectionAssert.assertReflectionEquals(
                expected,
                actual
        );
    }

    /**
     * игнорируем поле {@link FileUpload#getUploadDateTime() дата загрузки фида}.
     */
    private FileUpload getExpectedSupplierFeed(LocalDateTime uploadDate) {
        FileUpload feedUpload = new FileUpload.Builder()
                .setPartnerId(SUPPLIER_ID)
                .setName("supplier_feed.xls")
                .setSize(9L)
                .setUploadDate(uploadDate)
                .setId(1L)
                .setUrl("mds.url")
                .build();
        return feedUpload;
    }

    private MultipartFile mockMultipartFile() {
        return new MockMultipartFile(
                "file",
                "supplier_feed.xls",
                "TEXT",
                "file.text".getBytes()
        );
    }
}
