package ru.yandex.market.admin.outlet.dao;

import java.io.IOException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.fileupload.FileProcessingStatus;
import ru.yandex.market.admin.fileupload.FileUploadInfo;
import ru.yandex.market.admin.outlet.OutletUploadDao;
import ru.yandex.market.admin.outlet.errorserializer.ErrorFormatVersion;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

class OutletUploadDaoTest extends FunctionalTest {

    @Autowired
    private OutletUploadDao outletUploadDao;

    @Test
    @DisplayName("Создание новой записи в таблице загрузки аутлетов")
    @DbUnitDataSet(before = "csv/outletUpload.creation.before.csv", after = "csv/outletUpload.creation.after.csv")
    void testInsert() {
        long id = outletUploadDao.create(1001L, -2L, true, "filename.xls", "tmp/outlets01.xls");
        assertThat(id).isEqualTo(1L);
    }

    @Test
    @DisplayName("Обновление статуса в таблице загрузки аутлетов")
    @DbUnitDataSet(before = "csv/outletUpload.updating.before.csv", after = "csv/outletUpload.updating.after.csv")
    void testUpdateStatus() {
        outletUploadDao.updateStatus(4L, FileProcessingStatus.SUCCESS);
    }

    @Test
    @DisplayName("Добавление ошибок обработки файла в таблицу загрузки аутлетов")
    @DbUnitDataSet(before = "csv/outletUpload.updating.before.csv", after = "csv/outletUpload.updatingError.after.csv")
    void testUpdateStatusError() throws IOException {
        String xml = IOUtils.readInputStream(getClass().getResourceAsStream("../errors/xml/errors.xml"));
        outletUploadDao.setProcessingErrors(4L, ErrorFormatVersion.XML.getId(), xml);
    }

    @Test
    @DisplayName("Получение ошибок обработки файла из таблицы загрузки аутлетов")
    @DbUnitDataSet(before = "csv/outletUpload.getErrors.before.csv")
    void testGetErrorsAndVersion() throws IOException {
        String expected = IOUtils.readInputStream(getClass().getResourceAsStream("../errors/xml/error.xml"));
        String actual = outletUploadDao.getErrors(4L);
        Assertions.assertEquals(expected, actual);

        ErrorFormatVersion version = outletUploadDao.getErrorFormatVersion(4L);
        Assertions.assertEquals(ErrorFormatVersion.XML, version);
    }

    @Test
    @DisplayName("Получение ошибок обработки файла из таблицы загрузки аутлетов")
    @DbUnitDataSet(before = "csv/outletUpload.getLastRows.before.csv")
    void testGetLastRows() {
        List<FileUploadInfo> expected = Arrays.asList(
                FileUploadInfo.builder()
                        .id(String.valueOf(8L))
                        .userId(-2L)
                        .datasourceId(1001L)
                        .originalFileName("filename03.xls")
                        .removeOthers(false)
                        .filePath("tmp/outlets08.xls")
                        .status(FileProcessingStatus.UPLOADED)
                        .uploadDate(new GregorianCalendar(2018, 9, 10).getTime())
                        .build(),
                FileUploadInfo.builder()
                        .id(String.valueOf(9L))
                        .userId(-2L)
                        .datasourceId(1001L)
                        .originalFileName("filename05.xls")
                        .removeOthers(true)
                        .filePath("tmp/outlets09.xls")
                        .status(FileProcessingStatus.SUCCESS)
                        .uploadDate(new GregorianCalendar(2018, 9, 9).getTime())
                        .build()
        );
        List<FileUploadInfo> actual = outletUploadDao.getLastRows(1001L, 2);
        Assertions.assertEquals(expected, actual);
    }
}
