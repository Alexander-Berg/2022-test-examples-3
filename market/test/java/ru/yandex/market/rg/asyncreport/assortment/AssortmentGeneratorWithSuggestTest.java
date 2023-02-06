package ru.yandex.market.rg.asyncreport.assortment;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.util.ParamsUtils;
import ru.yandex.market.rg.asyncreport.assortment.model.AssortmentParams;
import ru.yandex.market.rg.config.FunctionalTest;

@ParametersAreNonnullByDefault
// AbstractMappingControllerFunctionalTest экстендит общий для ПИ FunctionText
public class AssortmentGeneratorWithSuggestTest extends FunctionalTest {

    @Autowired
    MdsS3Client mdsS3Client;
    @Autowired
    private ReportsDao<ReportsType> reportsDao;

    @Test
    @DisplayName("Вытащить параметры из базы и убедиться, что json извлекли корректно")
    @DbUnitDataSet(before = "csv/testAssortmentReportParams.csv")
    public void paramsTest() {
        ReportInfo<ReportsType> reportInfo = reportsDao.getPendingReportWithLock(
                Collections.singleton(ReportsType.ASSORTMENT));
        Assertions.assertNotNull(reportInfo);
        Assertions.assertEquals("1", reportInfo.getId());
        Assertions.assertEquals(774L, reportInfo.getReportRequest().getEntityId());
        Assertions.assertEquals(ReportsType.ASSORTMENT, reportInfo.getReportRequest().getReportType());
        Assertions.assertIterableEquals(Collections.singletonList("ACTIVE"),
                (Iterable<?>) reportInfo.getReportRequest().getParams().get("availabilityStatuses"));
        Assertions.assertIterableEquals(Collections.singletonList("IN_WORK"),
                (Iterable<?>) reportInfo.getReportRequest().getParams().get("offerStatuses"));

        Assertions.assertEquals(true, reportInfo.getReportRequest().getParams().get("useSuggesting"));
        Assertions.assertEquals(41595, reportInfo.getReportRequest().getParams().get("suggestId"));
    }

    @Test
    @DisplayName("Вытащить параметры из базы и убедиться, что json извлекли корректно с сериализацией новых")
    @DbUnitDataSet(before = "csv/testAssortmentReportParams.csv")
    public void paramsTestWithExtension() {
        ReportInfo<ReportsType> reportInfo = reportsDao.getReportInfo("2");
        Assertions.assertNotNull(reportInfo);
        Assertions.assertEquals("2", reportInfo.getId());
        AssortmentParams assortmentParams = ParamsUtils.convertToParams(reportInfo.getReportRequest().getParams(),
                AssortmentParams.class);
        Assertions.assertEquals(100000L, assortmentParams.getCreationTsFrom());
        Assertions.assertEquals(200000L, assortmentParams.getCreationTsTo());
    }
}
