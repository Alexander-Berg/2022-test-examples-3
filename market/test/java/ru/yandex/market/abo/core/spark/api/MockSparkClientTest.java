package ru.yandex.market.abo.core.spark.api;

import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import ru.yandex.market.abo.core.spark.data.EntrepreneurShortReport;
import ru.yandex.market.abo.core.spark.data.ExtendedReport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Тесты для {@link MockSparkClient}.
 */
public class MockSparkClientTest {
    private Jaxb2Marshaller marshaller;
    private MockSparkClient sparkClient;

    protected MockSparkClientTest() {
        this.marshaller = new Jaxb2Marshaller();
        this.marshaller.setPackagesToScan("ru.yandex.market.abo.core.spark.data");
        this.sparkClient = new MockSparkClient();
        this.sparkClient.setJaxbContext(marshaller.getJaxbContext());
    }

    @Test
    void testGetEntrepreneurShortReport() {
        final String inn = "449676180686";
        EntrepreneurShortReport entrepreneurShortReport =
                sparkClient.getEntrepreneurShortReport(new SparkEntity(SparkEntity.Type.INN, inn));
        var report = entrepreneurShortReport.getData().getReport().get(0);
        assertEquals(inn, report.getINN().toString());
        assertNotEquals(inn, report.getOGRNIP().toString());
    }

    @Test
    void testGetCompanyExtendedReport() {
        final String inn = "449676180686";
        ExtendedReport extendedReport =
                sparkClient.getCompanyExtendedReport(new SparkEntity(SparkEntity.Type.INN, inn));
        var report = extendedReport.getData().getReport().get(0);
        assertEquals(inn, report.getINN());
        assertNotEquals(inn, report.getOGRN());
    }

    @Test
    void testGetEntrepreneurShortReportOgrn() {
        String ogrn = "123451234512345";
        EntrepreneurShortReport entrepreneurShortReport =
                sparkClient.getEntrepreneurShortReport(new SparkEntity(SparkEntity.Type.OGRN, ogrn));
        var report = entrepreneurShortReport.getData().getReport().get(0);
        assertEquals(ogrn, report.getOGRNIP().toString());
    }
}
