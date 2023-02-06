package ru.yandex.autotests.direct.cmd.steps.excel;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.excelstatistic.ReportFileFormat;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatTypeEnum;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.allure.annotations.Step;

public class ExcelStatisticSteps extends DirectBackEndSteps {

    public File exportShowStatStatisticReport(ShowStatRequest request) {
        return exportShowStatStatisticReport(request, request.getStatType(), request.getFileFormat());
    }

    public File exportShowStatStatisticReport(ShowStatRequest request, StatTypeEnum reportType,
            ReportFileFormat fileType)
    {
        return exportShowStatStatisticReport(request, reportType, fileType,
                getContext().getProperties().getDirectCmdHost());
    }

    @Step("GET cmd = showStat (экспорт отчета {1} в файл типа {2}) host: {3}")
    private File exportShowStatStatisticReport(ShowStatRequest request, StatTypeEnum reportType,
            ReportFileFormat fileType, String host)
    {
        Objects.requireNonNull(reportType, "report type should be provided");
        Objects.requireNonNull(fileType, "file format should be provided");

        File respFile = get(CMD.SHOW_STAT, request.withFileFormat(fileType).withStatType(reportType), File.class);
        File newFile = new File(respFile.toString() + "." + fileType);
        try {
            newFile.delete();
            FileUtils.moveFile(respFile, newFile);
            attachFileToAllure(newFile, fileType);
            return newFile;
        } catch (IOException e) {
            throw new DirectCmdStepsException("Error while renaming downloaded file", e);
        }
    }

    public File exportToXlsShowCampStatStatisticReport(ShowCampStatRequest request) {
        return exportToXlsShowCampStatStatisticReport(request, getContext().getProperties().getDirectCmdHost());
    }

    @Step("GET cmd = showCampStat (экспорт отчета в xls файл) host:{1}")
    private File exportToXlsShowCampStatStatisticReport(ShowCampStatRequest request, String host) {

        File respFile = get(CMD.SHOW_CAMP_STAT, request, File.class);
        File newFile = new File(respFile.toString() + ".xls");
        try {
            newFile.delete();
            FileUtils.moveFile(respFile, newFile);
            attachFileToAllure(newFile, ReportFileFormat.XLS);
            return newFile;
        } catch (IOException e) {
            throw new DirectCmdStepsException("Error while renaming downloaded file", e);
        }
    }

    private void attachFileToAllure(File file, ReportFileFormat fileType) {
        byte[] bytes;
        try {
            bytes = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            throw new AssumptionException("файл успешно прочтен", e);
        }

        switch (fileType) {
            case CSV:
                AllureUtils.addCsvAttachment("csv файл", bytes);
                break;
            case XLS:
                AllureUtils.addXlsAttachment("xls файл", bytes);
                break;
            case XLSX:
                AllureUtils.addXlsxAttachment("xlsx файл", bytes);
        }
    }
}
