package ru.yandex.market.rg.asyncreport.daas;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.partner.mvc.controller.delivery.model.ShopPeriodReportFilter;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
@DisplayName("Проверка инфраструктуры для генерации асинхронных отчетов DAAS")
public class DaasGeneratorTest extends FunctionalTest {

    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private List<DaasAsyncReportGenerator> generators;

    @Test
    @DisplayName("Проверка возможности генерации отчетов DAAS")
    void testDaasReport() throws MalformedURLException {
        doReturn(new URL("http://url-to-report")).when(mdsS3Client).getUrl(any());
//        when(mdsS3Client.getUrl(any()))
//            .thenReturn(new URL("http://url-to-report"));
        Assertions.assertEquals(3, generators.size());
        generators.forEach(generator -> Assertions.assertEquals(ReportState.DONE, testReport(generator)));
    }

    @Test
    @DisplayName("Проверка обработки пустых отчетов")
    void testEmptyReport() {
        var filter = new ShopPeriodReportFilter(1L, LocalDate.parse("2010-04-01"), LocalDate.parse("2010-04-30"));
        generators.forEach(generator -> {
            assertThat(generator.generate(UUID.randomUUID().toString(), filter))
                    .extracting(ReportResult::getNewState,
                            reportResult -> reportResult.getReportGenerationInfo().getUrlToDownload(),
                            reportResult -> reportResult.getReportGenerationInfo().getDescription())
                    .containsExactly(ReportState.DONE, null, "Report is empty");
        });
    }

    @Nonnull
    private ReportState testReport(DaasAsyncReportGenerator servicesReportGenerator) {
        return servicesReportGenerator.generate(
            UUID.randomUUID().toString(),
            new ShopPeriodReportFilter(1L, LocalDate.parse("2020-04-01"), LocalDate.parse("2020-04-30")))
                .getNewState();
    }
}
