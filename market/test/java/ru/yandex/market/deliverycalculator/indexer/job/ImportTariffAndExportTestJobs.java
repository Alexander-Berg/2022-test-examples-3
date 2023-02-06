package ru.yandex.market.deliverycalculator.indexer.job;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.googlecode.protobuf.format.JsonFormat;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.PbSnUtils;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexer.constant.PbSnMagicConstant;
import ru.yandex.market.deliverycalculator.indexer.service.TariffImportService;
import ru.yandex.market.deliverycalculator.indexer.util.HttpClientTestUtils;
import ru.yandex.market.deliverycalculator.indexer.util.PrintResultExportJobTestUtils;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.tariff.CargoTypeRestrictionsDto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONCompare.compareJSON;

class ImportTariffAndExportTestJobs extends FunctionalTest {

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private TariffImportService daasTariffImportService;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    private ExportYadoTariffGenerationJob exportWhiteTariffGenerationJob;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(after = "white-courier/after.csv")
    void testWhiteCourierTariffLoadAndExport() throws IOException {
        testTariff(14221L, exportWhiteTariffGenerationJob, "white-courier/tariff_14221.xml",
                "white-courier/metaTariff.json",
                "white-courier/gen.json",
                "MARDO_WHITE_COURIER_GENERATIONS", "GENERATIONS", "YA_DELIVERY_TARIFF_PROGRAMS");
    }

    private void testTariff(Long tariffId, ExportYadoTariffGenerationJob job,
                            String tariff, String meta, String generation, String... tables)
            throws IOException {
        String originalXml = readResource(tariff);
        when(mdsS3Client.getUrl(any())).thenReturn(new URL("http://test.yandex.ru/"));
        when(lmsClient.getCargoTypesByTariffId(eq(tariffId)))
                .thenReturn(new CargoTypeRestrictionsDto(1L, Collections.emptyList(), Collections.emptyList()));
        when(httpClient.execute(any())).thenAnswer(invocation -> {
            HttpGet get = invocation.getArgument(0);

            String path = get.getURI().getPath();
            if (path.contains("/revisions/last")) {
                return HttpClientTestUtils.mockResponse(getClass().getResourceAsStream(meta));
            } else {
                return HttpClientTestUtils.mockResponse(IOUtils.toInputStream(originalXml, StandardCharsets.UTF_8));
            }
        });

        StringBuilder uploadedXml = new StringBuilder();
        doAnswer(invocation -> {
            ContentProvider contentProvider = invocation.getArgument(1);
            uploadedXml.append(IOUtils.toString(contentProvider.getInputStream(), StandardCharsets.UTF_8));
            return null;
        }).when(mdsS3Client).upload(any(), any());

        daasTariffImportService.importYaDeliveryTariff();

        Assertions.assertEquals(originalXml, uploadedXml.toString());

        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> tariff, getClass());
        List<DeliveryCalcProtos.FeedDeliveryOptionsResp> resp = new ArrayList<>();
        doAnswer(invocation -> {
            ContentProvider contentProvider = invocation.getArgument(1);
            resp.add(readFeedDeliveryOptionsResp(contentProvider.getInputStream()));
            return null;
        }).when(mdsS3Client).upload(any(), any());

        job.doJob(Mockito.mock(JobExecutionContext.class));

        PrintResultExportJobTestUtils.print(transactionTemplate, resp.get(0), tables);

        JSONCompareResult jsonCompareResult = compareJSON(JsonFormat.printToString(resp.get(0)),
                readResource(generation),
                new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                        new Customization("update_time_ts", (o1, o2) -> true)));
        assertFalse(jsonCompareResult.failed(), jsonCompareResult.getMessage());
    }

    private String readResource(final String fileName) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(fileName)) {
            Validate.notNull(stream, "Could not find resource " + fileName);
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    private static DeliveryCalcProtos.FeedDeliveryOptionsResp readFeedDeliveryOptionsResp(final InputStream stream)
            throws Exception {
        return PbSnUtils.readPbSnMessage(
                PbSnMagicConstant.FEED_DELIVERY_OPTIONS_RESP,
                DeliveryCalcProtos.FeedDeliveryOptionsResp.parser(),
                stream
        );
    }
}
