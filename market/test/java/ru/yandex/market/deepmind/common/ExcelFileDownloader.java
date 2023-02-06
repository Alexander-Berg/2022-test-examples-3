package ru.yandex.market.deepmind.common;

import java.io.InputStream;

import lombok.RequiredArgsConstructor;

import ru.yandex.market.deepmind.common.mocks.ExcelS3ServiceMock;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.misc.io.http.UrlUtils;

@RequiredArgsConstructor
public class ExcelFileDownloader {

    private final BackgroundServiceMock backgroundService;
    private final ExcelS3ServiceMock excelS3Service;

    public ExcelFile downloadExport(int actionId) {
        BackgroundActionStatus result;
        do {
            result = backgroundService.getAction(actionId);

            if (result.getStatus() == BackgroundActionStatus.ActionStatus.FINISHED) {
                String url = result.getUrl();
                String s3key = getS3KeyFromProxyUrl(url);
                InputStream inputStream = excelS3Service.getFile(s3key);
                if (inputStream == null) {
                    throw new IllegalStateException("Failed to find file at url: " + url);
                }
                return ExcelFileConverter.convert(inputStream, ExcelHeaders.MBOC_EXCEL_IGNORES_CONFIG);
            }
            if (result.getStatus() == BackgroundActionStatus.ActionStatus.FAILED) {
                throw new RuntimeException(result.getMessage());
            }
        } while (result.getStatus() != BackgroundActionStatus.ActionStatus.FINISHED);
        throw new IllegalStateException("Got result: " + result);
    }

    private String getS3KeyFromProxyUrl(String url) {
        String filenameParamKey = "?filename=";
        int paramIndex = url.indexOf(filenameParamKey);
        if (paramIndex < 0) {
            throw new RuntimeException("Wrong S3 proxy URL! " + url);
        }
        String filename = url.substring(paramIndex + filenameParamKey.length());
        return UrlUtils.urlDecode(filename);
    }
}
