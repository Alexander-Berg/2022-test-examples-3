package ru.yandex.market.abo.core.spark.risks;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.clch.model.OrganizationInfo;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.spark.api.ISparkClient;
import ru.yandex.market.abo.core.spark.api.SparkApiDataLoader;
import ru.yandex.market.abo.core.spark.yt.SparkYtDataLoader;
import ru.yandex.market.abo.core.storage.json.spark.risks.JsonSparkRisks;
import ru.yandex.market.abo.core.storage.json.spark.risks.JsonSparkRisksService;
import ru.yandex.market.abo.cpa.MbiApiService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.spark.risks.SparkRisksManager.SPARK_RISKS_REPORT_EXPIRATION_TIME_DAYS;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 28.05.2020
 */
class SparkRisksManagerTest{

    private static final long SHOP_ID = 123L;
    private static final String OGRN = "1234567890123";

    @InjectMocks
    private SparkRisksManager sparkRisksManager;

    @Mock
    private ISparkClient sparkClient;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private JsonSparkRisksService jsonSparkRisksService;
    @Mock
    private SparkApiDataLoader sparkApiDataLoader;
    @Mock
    private SparkYtDataLoader sparkYtDataLoader;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        mockOrgInfo(OGRN);
        when(sparkApiDataLoader.isDisabled()).thenReturn(false);
        when(sparkYtDataLoader.isDisabled()).thenReturn(true);
    }

    @Test
    void testUpdateSparkRisksReport__doNotUpdateFreshReport() {
        var savedReport = mock(JsonSparkRisks.class);
        when(savedReport.getCreationTime()).thenReturn(
                DateUtil.asDate(LocalDateTime.now().minusDays(SPARK_RISKS_REPORT_EXPIRATION_TIME_DAYS - 1))
        );
        when(jsonSparkRisksService.getSparkRisks(OGRN)).thenReturn(Optional.of(savedReport));

        sparkRisksManager.updateSparkRisksReportIfNecessary(SHOP_ID);

        verifyRisksReportNotUpdated();
    }

    @Test
    void testUpdateSparkRisksReport__doNotLoadRisksForNonRussianShop() {
        when(jsonSparkRisksService.getSparkRisks(OGRN)).thenReturn(Optional.empty());
        when(shopInfoService.getShopCountry(SHOP_ID)).thenReturn((long) Regions.BELARUS);

        sparkRisksManager.updateSparkRisksReportIfNecessary(SHOP_ID);

        verifyRisksReportNotUpdated();
    }

    @ParameterizedTest
    @CsvSource({"1234567890123, 1, 0", "123456789012345, 0, 1"})
    void testUpdateSparkRisksReport__useDifferentMethodsForCompanyAndIp(String ogrn, int companyMethodCallCount, int ipMethodCallCount) {
        when(jsonSparkRisksService.getSparkRisks(ogrn)).thenReturn(Optional.empty());
        when(shopInfoService.getShopCountry(SHOP_ID)).thenReturn((long) Regions.RUSSIA);

        mockOrgInfo(ogrn);

        sparkRisksManager.updateSparkRisksReportIfNecessary(SHOP_ID);

        verify(sparkClient, times(companyMethodCallCount)).getCompanySparkRisksReportXML(ogrn);
        verify(sparkClient, times(ipMethodCallCount)).getPersonSparkRisksReportXML(ogrn);
        verify(jsonSparkRisksService).save(eq(ogrn), any());
    }

    private void mockOrgInfo(String ogrn) {
        var orgInfo = mock(OrganizationInfo.class);
        when(orgInfo.getOgrn()).thenReturn(ogrn);
        when(mbiApiService.getOrgInfo(SHOP_ID)).thenReturn(orgInfo);
    }

    private void verifyRisksReportNotUpdated() {
        verify(sparkClient, never()).getCompanySparkRisksReportXML(any());
        verify(sparkClient, never()).getPersonSparkRisksReportXML(any());
        verify(jsonSparkRisksService, never()).save(any(JsonSparkRisks.class));
    }
}
