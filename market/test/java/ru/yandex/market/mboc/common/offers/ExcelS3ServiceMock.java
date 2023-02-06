package ru.yandex.market.mboc.common.offers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mboc.app.offers.ExcelFileType;
import ru.yandex.market.mboc.app.offers.ExcelS3Context;
import ru.yandex.market.mboc.app.offers.ExcelS3Service;
import ru.yandex.market.mboc.app.offers.S3UploadException;
import ru.yandex.market.mboc.app.offers.UploadResult;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

public class ExcelS3ServiceMock implements ExcelS3Service {
    private static final String URL = "http://mock-url.com";
    private final Map<String, byte[]> filesByUrl = new HashMap<>();

    public ExcelFile getFileAsExcel(String url) {
        var inputSrteam = new ByteArrayInputStream(filesByUrl.get(url));
        return ExcelFileConverter.convert(inputSrteam, ExcelHeaders.MBOC_EXCEL_IGNORES_CONFIG);
    }

    @Nonnull
    @Override
    public UploadResult uploadAsExportFile(ExcelS3Context context) throws S3UploadException {
        String s3key = String.format("/export-excel/%s/%s %s%s", LocalDate.now(), context.getFilePrefix(),
            DateTimeUtils.dateTimeNow(), context.getFileExtension());
        String url = String.format("%s%s", URL, s3key);
        if (context.getInputStream() != null) {
            try (var inputStream = context.getInputStream();
                 var outputStream = new ByteArrayOutputStream()) {
                IOUtils.copy(inputStream, outputStream);
                filesByUrl.put(url, outputStream.toByteArray());
            } catch (IOException e) {
                throw new S3UploadException("Error", e);
            }
        } else if (context.getExcelS3Writable() != null) {
            try (var outputStream = new ByteArrayOutputStream()) {
                context.getExcelS3Writable().write(outputStream);
                filesByUrl.put(url, outputStream.toByteArray());
            } catch (IOException e) {
                throw new S3UploadException("Error", e);
            }
        } else {
            throw new IllegalArgumentException("Expected InputStream or ExcelS3Writable");
        }
        return new UploadResult(url, s3key);
    }

    @Override
    @Nullable
    public String uploadAsImportFile(@Nullable Integer supplierId, String login,
                                     String fileName, byte[] fileBytes,
                                     @Nullable Object metaInfo, ExcelFileType fileType) {


        String url = String.format("%s/import-excel/%s/%s-%s-%s", URL, LocalDate.now(), supplierId,
            login, fileName);
        filesByUrl.put(url, fileBytes);
        return url;
    }

    @Override
    public InputStream getFile(String filename) {
        String url = String.format("%s%s", URL, filename);
        return new ByteArrayInputStream(filesByUrl.get(url));
    }
}
