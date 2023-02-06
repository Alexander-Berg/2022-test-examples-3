package ru.yandex.autotests.reporting.api.steps;

import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.stat.util.FileReaderUtils;
import ru.yandex.autotests.reporting.api.beans.ReportFileInfo;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Created by kateleb on 21.11.16.
 */
public class ReportingApiFileSteps {

    public static final Pattern PATTERN_ANY_DATA = Pattern.compile(".*");

    @Step
    public void checkFiles(ReportingApiParams testType, List<ReportFileInfo> files) {
        Attacher.attach("Files", files);
        assertThat("No files were generated!", files.size(), greaterThan(0));
        String shopBaseName = getShopDomainName(testType);
        if (testType.buildCpc()) checkCpcFile(files, shopBaseName);
        if (testType.buildCpa()) checkCpaFile(files, shopBaseName);
        if (testType.buildAssortment()) checkAssortment(files, shopBaseName);
        if (testType.buildPrice()) checkPrices(files, shopBaseName);
        if (testType.buildForecaster()) checkForecaster(files, shopBaseName);
        checkZipFile(files, shopBaseName);
    }

    @Step
    private void checkCpcFile(List<ReportFileInfo> files, String shopBaseName) {
        checkPptxFile(files, shopBaseName, "CPC");
    }

    @Step
    private void checkCpaFile(List<ReportFileInfo> files, String shopBaseName) {
        checkPptxFile(files, shopBaseName, "CPA");
    }

    @Step
    private void checkAssortment(List<ReportFileInfo> files, String shopBaseName) {
        checkXlsFile(files, shopBaseName, "Ассортимент");
    }

    @Step
    private void checkPrices(List<ReportFileInfo> files, String shopBaseName) {
        checkXlsFile(files, shopBaseName, "Отклонение");
    }

    @Step
    private void checkForecaster(List<ReportFileInfo> files, String shopBaseName) {
        checkXlsFile(files, shopBaseName, "Прогноз");
    }

    private void checkXlsFile(List<ReportFileInfo> files, String shopBaseName, String stringToFind) {
        checkFile(files, shopBaseName, stringToFind, "xlsx");
    }

    private void checkPptxFile(List<ReportFileInfo> files, String shop, String stringToFind) {
        checkFile(files, shop, stringToFind, "pptx");
    }

    private void checkFile(List<ReportFileInfo> files, String shopBaseName, String stringToFind, String fileType) {
        ReportFileInfo file = files.stream().filter(f -> f.getName().contains(stringToFind) && f.getName().endsWith("." + fileType)).findAny().orElseThrow(() ->
                new AssertionError("No " + fileType + " file found!"));
        assertThat(fileType + " file name does not contain shop name!", file.getName(), containsString(shopBaseName));
        List<String> data = FileReaderUtils.getMatchesFromFile(file.getUrl(), PATTERN_ANY_DATA, 100);
        assertThat(fileType + " file does not contain anything!", data.toString().length(), greaterThan(100));
    }

    @Step
    private void checkZipFile(List<ReportFileInfo> files, String shop) {
        Map<String, Long> filesData = files.stream().filter(f -> !f.getName().endsWith(".zip")).
                collect(Collectors.toMap(ReportFileInfo::getName, item -> FileReaderUtils.getFileSize(item.getUrl())));
        ReportFileInfo zipFile = files.stream().filter(f -> f.getName().endsWith(".zip")).findAny().orElse(null);
        Map<String, Long> zipData = FileReaderUtils.getFileSizesFromZipFileStream(zipFile.getUrl());

        Attacher.attach("Not .zip files", filesData.keySet());
        assertThat("No zip file found!", zipFile, notNullValue());
        assertThat("Zip file name does not contain shop name!", zipFile.getName(), containsString(shop));

        Attacher.attach("Files in gzip", zipData.keySet());
        assertThat("Some files not found in zip", zipData.keySet(), containsInAnyOrder(filesData.keySet().toArray(new String[filesData.size()])));

        List<String> fileNamesWithDifferentLines =
                filesData.keySet().stream().filter(el -> !filesData.get(el).equals(zipData.get(el))).collect(Collectors.toList());
        fileNamesWithDifferentLines.forEach(el -> Attacher.attach("File size diff in zip and in files for file " + el,
                "zip: " + zipData.get(el) + ", files: " + filesData.get(el)));
        assertThat("File size diff is not empty", fileNamesWithDifferentLines, empty());
    }

    public static String getShopDomainName(ReportingApiParams testType) {
        String shop = testType.getShop();
        assertNotNull("Null shop name!", shop);
        String[] parts = shop.split("\\.");
        return parts.length > 1 ? parts[parts.length - 2] : shop;
    }
}
