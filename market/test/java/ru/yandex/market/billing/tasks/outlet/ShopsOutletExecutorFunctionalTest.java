package ru.yandex.market.billing.tasks.outlet;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.config.BillingMdsS3Config;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.outlet.moderation.OutletStatus;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopsOutletExecutorFunctionalTest extends FunctionalTest {

    @Autowired
    private ShopsOutletExecutor shopsOutletExecutor;

    @Autowired
    private NamedHistoryMdsS3Client namedHistoryMdsS3Client;

    @Autowired
    private DatePeriodProvider shopsOutletExecutorDatePeriodProvider;

    @Autowired
    private EnvironmentService environmentService;

    private static void mockAndAssertContent(NamedHistoryMdsS3Client mdsS3Client, String expectedFileName) {
        when(mdsS3Client.upload(eq(BillingMdsS3Config.SHOPS_OUTLETS_V2_RESOURCE), any()))
                .then(invocation -> {
                    ContentProvider contentProvider = invocation.getArgument(1);
                    try (GZIPInputStream is = new GZIPInputStream(contentProvider.getInputStream())) {
                        String actualContent = IOUtils.toString(is, StandardCharsets.UTF_8);
                        String expectedContent = getExpectedContent(expectedFileName);

                        MbiAsserts.assertXmlEquals(expectedContent, actualContent);
                    }

                    return null;
                });
    }

    private static String getExpectedContent(String expectedFileName) {
        try (InputStream is = ShopsOutletExecutorFunctionalTest.class.getResourceAsStream(expectedFileName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("Could not read " + expectedFileName, ex);
        }
    }

    /**
     * Тест проверяет, что для магазина успешно выгружается точка самовывоза в статусе
     * {@link OutletStatus#MODERATED} и {@code hidden == 0}
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "/ru/yandex/market/core/delivery/calendar/impl/calendar_common.csv",
                    "ShopsOutletExecutorFunctionalTest.testExportShopOutlet.csv"
            }
    )
    void testExportShopOutlet() {
        when(shopsOutletExecutorDatePeriodProvider.provide())
                .thenReturn(DatePeriod.of(LocalDate.of(2017, Month.DECEMBER, 20), 17));
        mockAndAssertContent(namedHistoryMdsS3Client, "data/testExportShopOutlet.xml");

        shopsOutletExecutor.doJob(null);

        verifyUploadHappened();
    }

    /**
     * Тест проверяет, что неактиваный ранее подключённый магазин не выгружатеся с включённым флагом
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "/ru/yandex/market/core/delivery/calendar/impl/calendar_common.csv",
                    "ShopsOutletExecutorFunctionalTest.testExportShopOutlet.csv",
                    "ShopsOutletExecutorFunctionalTest.notAliveEverActivated.csv"
            }
    )
    void testSkipNotAliveEverActivatedForExport() {
        environmentService.setValue("OutletExportSettings.skipExportNotAliveEverActivatedShops",
                Boolean.TRUE.toString());
        when(shopsOutletExecutorDatePeriodProvider.provide())
                .thenReturn(DatePeriod.of(LocalDate.of(2017, Month.DECEMBER, 20), 17));
        mockAndAssertContent(namedHistoryMdsS3Client, "data/testExportShopOutlet.xml");

        shopsOutletExecutor.doJob(null);

        verifyUploadHappened();
    }

    /**
     * Тест проверяет, что неактиваный ранее подключённый магазин выгружатеся без включённого флага
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "/ru/yandex/market/core/delivery/calendar/impl/calendar_common.csv",
                    "ShopsOutletExecutorFunctionalTest.testExportShopOutlet.csv",
                    "ShopsOutletExecutorFunctionalTest.notAliveEverActivated.csv"
            }
    )
    void testNotSkipNotAliveEverActivatedForExport() {
        when(shopsOutletExecutorDatePeriodProvider.provide())
                .thenReturn(DatePeriod.of(LocalDate.of(2017, Month.DECEMBER, 20), 17));
        mockAndAssertContent(namedHistoryMdsS3Client, "data/testExportShopOutletWithoutSkip.xml");

        shopsOutletExecutor.doJob(null);

        verifyUploadHappened();
    }

    /**
     * Тест проверяет, что неактиваный и неподключённый магазин выгружатеся с ограниченным количеством аутлетов
     * при включённом параметре
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "/ru/yandex/market/core/delivery/calendar/impl/calendar_common.csv",
                    "ShopsOutletExecutorFunctionalTest.testExportShopOutlet.csv",
                    "ShopsOutletExecutorFunctionalTest.notAliveNeverActivated.csv"
            }
    )
    void testLimitedPartnerOutletsForExport() {
        environmentService.setValue("OutletExportSettings.limitOutletsForNotAliveNeverActivatedShops",
                "true");
        environmentService.setValue("OutletExportSettings.limitOutletsSize", "2");
        when(shopsOutletExecutorDatePeriodProvider.provide())
                .thenReturn(DatePeriod.of(LocalDate.of(2017, Month.DECEMBER, 20), 17));
        mockAndAssertContent(namedHistoryMdsS3Client, "data/testExportShopOutletLimitedPartner.xml");

        shopsOutletExecutor.doJob(null);

        verifyUploadHappened();
    }

    /**
     * Тест проверяет, что неактиваный и неподключённый магазин выгружатеся с ограниченным количеством аутлетов
     * при включённом параметре
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "/ru/yandex/market/core/delivery/calendar/impl/calendar_common.csv",
                    "ShopsOutletExecutorFunctionalTest.testExportShopOutlet.csv",
                    "ShopsOutletExecutorFunctionalTest.notAliveNeverActivated.csv"
            }
    )
    void testNotLimitedPartnerOutletsForExport() {
        environmentService.setValue("OutletExportSettings.limitOutletsSize", "2");
        when(shopsOutletExecutorDatePeriodProvider.provide())
                .thenReturn(DatePeriod.of(LocalDate.of(2017, Month.DECEMBER, 20), 17));
        mockAndAssertContent(namedHistoryMdsS3Client, "data/testExportShopOutletLimitedPartnerWithoutLimit.xml");

        shopsOutletExecutor.doJob(null);

        verifyUploadHappened();
    }

    private void verifyUploadHappened() {
        verify(namedHistoryMdsS3Client).upload(eq(BillingMdsS3Config.SHOPS_OUTLETS_V2_RESOURCE), any());
    }
}
