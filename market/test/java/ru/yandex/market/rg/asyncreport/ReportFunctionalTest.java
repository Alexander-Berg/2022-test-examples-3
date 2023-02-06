package ru.yandex.market.rg.asyncreport;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.excel.wrapper.PoiWorkbook;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.model.ReportGenerationInfo;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.util.ParamsUtils;
import ru.yandex.market.core.asyncreport.worker.ReportGenerator;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
public abstract class ReportFunctionalTest extends FunctionalTest {

    @Autowired
    protected ReportsService<ReportsType> reportsService;
    @Autowired
    protected MdsS3Client mdsS3Client;

    public <T extends Enum<T>, U> ReportGenerationInfo checkRawReport(String reportId,
                                                                      long partnerId,
                                                                      ReportGenerator<T, U> reportGenerator,
                                                                      @Nullable Consumer<byte[]> checker) {
        U params = ParamsUtils.extractParamsForReport(reportsService, partnerId, reportId,
                reportGenerator.getParamsType());
        assertThat(params)
                .isNotNull();

        ByteArrayOutputStream reportData = new ByteArrayOutputStream();
        doAnswer((Answer<Void>) invocation -> {
            ContentProvider contentProvider = invocation.getArgument(1);
            InputStream inputStream = contentProvider.getInputStream();
            IOUtils.copy(inputStream, reportData);
            return null;
        }).when(mdsS3Client)
                .upload(Mockito.any(ResourceLocation.class), Mockito.any(ContentProvider.class));

        mockReportUrl();

        ReportResult generate = reportGenerator.generate(reportId, params);
        if (checker != null) {
            checker.accept(reportData.toByteArray());
        }
        return generate.getReportGenerationInfo();
    }

    public <T extends Enum<T>, U> void checkReport(String reportId,
                                                   long partnerId,
                                                   ReportGenerator<T, U> reportGenerator,
                                                   Consumer<InputStream> checker) {
        ReportGenerationInfo info = checkRawReport(reportId, partnerId, reportGenerator,
                data -> checker.accept(new ByteArrayInputStream(data)));
        assertThat(info.getUrlToDownload())
                .isNotNull();
    }

    public <T extends Enum<T>, U> void checkExcelReport(String reportId,
                                                        long partnerId,
                                                        ReportGenerator<T, U> reportGenerator,
                                                        Consumer<Sheet> checker) {
        checkReport(reportId, partnerId, reportGenerator, inputStream -> {
            PoiWorkbook poiWorkbook = PoiWorkbook.load(inputStream);
            // XXX(vbauer): Раскомментируйте данную строчку чтобы посмотреть полученный файл
            // writeToTempFile(poiWorkbook);

            Workbook workbook = poiWorkbook.getWorkbook();
            checker.accept(workbook.getSheetAt(0));
        });
    }

    public <T extends Enum<T>, U> void checkEmptyReport(String reportId,
                                                        long partnerId,
                                                        ReportGenerator<T, U> reportGenerator) {
        ReportGenerationInfo info = checkRawReport(reportId, partnerId, reportGenerator, null);
        assertThat(info.getUrlToDownload())
                .isNull();
    }

    protected List<String> getCsvFileAsStrings(InputStream inputStream) {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));

        return r.lines()
                .map(l -> l.replaceAll("\ufeff", ""))
                .collect(Collectors.toList());
    }

    protected void csvCheck(List<String> expected,
                            InputStream inputStream) {
        List<String> list = getCsvFileAsStrings(inputStream);

        assertThat(list)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    protected void xlsCheck(List<String> expected,
                            InputStream inputStream,
                            String delimiter) {
        PoiWorkbook poiWorkbook = PoiWorkbook.load(inputStream);
        // XXX(vbauer): Раскомментируйте данную строчку чтобы посмотреть полученный файл
        // writeToTempFile(poiWorkbook);

        Workbook workbook = poiWorkbook.getWorkbook();
        Sheet sheet = workbook.getSheetAt(0);

        Assertions.assertEquals(expected.size() - 1, sheet.getLastRowNum());

        for (int i = 0; i < expected.size(); i++) {
            Row row = sheet.getRow(i);
            String[] expectedCells = expected.get(i)
                    .split(delimiter);

            for (int j = 0; j < expectedCells.length; j++) {
                Cell cell = row.getCell(j);
                CellType cellType = cell.getCellType();
                switch (cellType) {
                    case STRING:
                        assertThat(cell.getStringCellValue()).isEqualTo(expectedCells[j]);
                        return;
                    case NUMERIC:
                        assertThat(String.valueOf(cell.getNumericCellValue())).isEqualTo(expectedCells[j]);
                        return;
                    default:
                        throw new UnsupportedOperationException("Could not read cell type " + cellType);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void writeToTempFile(PoiWorkbook poiWorkbook) {
        try {
            poiWorkbook.write(new FileOutputStream("test-report-output.xls"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void mockReportUrl() {
        try {
            doReturn(new URL("http://nowhere.su")).when(mdsS3Client).getUrl(any());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
