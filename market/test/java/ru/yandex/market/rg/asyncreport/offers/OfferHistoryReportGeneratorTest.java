package ru.yandex.market.rg.asyncreport.offers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.asyncreport.offers.history.OfferHistoryParams;
import ru.yandex.market.rg.asyncreport.offers.history.OfferHistoryReportGenerator;
import ru.yandex.market.rg.asyncreport.offers.history.OfferHistoryReportItem;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(before = "OfferHistory.before.csv")
public class OfferHistoryReportGeneratorTest extends FunctionalTest {

    @Autowired
    private OfferHistoryReportGenerator offerHistoryReportGenerator;

    @Autowired
    @Qualifier("autoClusterChytJdbcTemplate")
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private EnvironmentService environmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void init() throws MalformedURLException {
        doReturn(new URL("http://path/to")).
                when(mdsS3Client).getUrl(any());
    }


    @DisplayName("Без данных")
    @Test
    public void testOffersWithEmptyResult() throws IOException {
        String offerHistoryRequest = "{\n" +
                "\t\"entityId\": 1,\n" +
                "\t\"offerId\": \"some-offer\"\n" +
                "}";
        ReportResult result = offerHistoryReportGenerator.generate("report",
                objectMapper.readValue(offerHistoryRequest, OfferHistoryParams.class));

        Assertions.assertEquals(ReportState.DONE, result.getNewState());
        Assertions.assertNull(result.getReportGenerationInfo().getDescription());
        Assertions.assertEquals("http://path/to", result.getReportGenerationInfo().getUrlToDownload());
    }

    @DisplayName("С данными")
    @Test
    public void testOffersWithResult() throws IOException {
        doReturn(
                List.of(
                        OfferHistoryReportItem.builder()
                                .setOfferId("123")
                                .setPrice("1000")
                                .setPriceSource(4)
                                .setDate("2022-01-14 16:44:00")
                                .build(),
                        OfferHistoryReportItem.builder()
                                .setOfferId("123")
                                .setPrice("1200")
                                .setPriceSource(4)
                                .setDate("2022-01-14 16:43:00")
                                .build())
        ).when(namedParameterJdbcTemplate).query(any(), any(SqlParameterSource.class), any(RowMapper.class));

        String offerHistoryRequest = "{\n" +
                "\t\"entityId\": 1,\n" +
                "\t\"offerId\": \"some-offer\"\n" +
                "}";
        ReportResult result = offerHistoryReportGenerator.generate("report",
                objectMapper.readValue(offerHistoryRequest, OfferHistoryParams.class));

        Assertions.assertEquals(ReportState.DONE, result.getNewState());
        Assertions.assertNull(result.getReportGenerationInfo().getDescription());
        Assertions.assertEquals("http://path/to", result.getReportGenerationInfo().getUrlToDownload());
    }

    @Test
    @Disabled
    public void manual() throws IOException {
        environmentService.setValue("OfferHistoryReport.useChyt", "false");
        mockFileStorageToLocal();
        String offerHistoryRequest = "{\n" +
                "\t\"entityId\": 10336543,\n" +
                "\t\"offerId\": \"push-monitor-check-http-price\"\n" +
                "}";
        ReportResult reportResult = offerHistoryReportGenerator.generate("report",
                objectMapper.readValue(offerHistoryRequest, OfferHistoryParams.class));
    }

    private void mockFileStorageToLocal() {
        Mockito.doAnswer(invocation -> {
            try {
                FileContentProvider fileProvider = invocation.getArgument(1);
                File dir = new File(SystemUtils.getUserHome(), "reports");
                FileUtils.forceMkdir(dir);
                File target = new File(dir, "offer-history.xlsx");
                FileUtils.copyInputStreamToFile(fileProvider.getInputStream(), target);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new StoreInfo(900, "https://mds3.ya.net");
        }).when(mdsS3Client).upload(any(), any());
    }
}

