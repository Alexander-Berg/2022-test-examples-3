package ru.yandex.market.rg.asyncreport.content.loyalty;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "ContentLoyalty.before.csv")
public class ContentLoyaltyReportGeneratorTest extends FunctionalTest {

    @Autowired
    private ContentLoyaltyReportGenerator contentLoyaltyReportGenerator;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    @Qualifier("autoClusterChytJdbcTemplate")
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private ObjectMapper objectMapper;

    @BeforeEach
    void init() throws MalformedURLException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        doReturn(new URL("http://path/to")).
                when(mdsS3Client).getUrl(any());
    }

    @DisplayName("Без данных")
    @Test
    public void testWithEmptyResult() throws IOException {
        String contentLoyaltyRequest = "{\n" +
                "\t\"entityId\": 1207586,\n" +
                "\t\"date\": \"2022-07-04\"\n" +
                "}";
        ReportResult result = contentLoyaltyReportGenerator.generate("report",
                objectMapper.readValue(contentLoyaltyRequest, ContentLoyaltyParams.class));

        Assertions.assertEquals(ReportState.DONE, result.getNewState());
        Assertions.assertNull(result.getReportGenerationInfo().getDescription());
        Assertions.assertEquals("http://path/to", result.getReportGenerationInfo().getUrlToDownload());
    }

    @Test
    @Disabled
    public void manual() throws IOException {
        mockFileStorageToLocal();
        String request = "{\n" +
                "\t\"entityId\": 711690," +
                "\t\"date\": \"2022-07-11\"\n" +
                "}";
        ReportResult reportResult = contentLoyaltyReportGenerator.generate("report",
                objectMapper.readValue(request, ContentLoyaltyParams.class));
    }

    private void mockFileStorageToLocal() {
        Mockito.doAnswer(invocation -> {
            try {
                FileContentProvider fileProvider = invocation.getArgument(1);
                File dir = new File(SystemUtils.getUserHome(), "reports");
                FileUtils.forceMkdir(dir);
                File target = new File(dir, "content-loyalty.xlsx");
                FileUtils.copyInputStreamToFile(fileProvider.getInputStream(), target);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new StoreInfo(900, "https://mds3.ya.net");
        }).when(mdsS3Client).upload(any(), any());
    }
}
