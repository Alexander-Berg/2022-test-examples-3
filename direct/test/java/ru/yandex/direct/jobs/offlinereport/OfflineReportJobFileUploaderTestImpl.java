package ru.yandex.direct.jobs.offlinereport;

import java.io.File;
import java.io.FileOutputStream;

import ru.yandex.direct.jobs.offlinereport.model.OfflineReportHeader;

public class OfflineReportJobFileUploaderTestImpl implements OfflineReportJobFileUploader {
    @Override
    public String uploadReportMds(byte[] report, OfflineReportHeader header) {
        try {
            File file = new File("/tmp/out.xlsx");
            try (FileOutputStream os = new FileOutputStream(file)) {
                os.write(report);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "http://localhost/file";
    }
}
