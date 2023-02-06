package ru.yandex.market.abo.core.storage.json.spark.risks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.spark.api.ISparkClient;
import ru.yandex.market.abo.core.spark.risks.model.SparkRisksReport;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 29.05.2020
 */
class JsonSparkRisksServiceTest extends EmptyTest {

    private static final String OGRN = "1234567890123";

    @Autowired
    private JsonSparkRisksService jsonSparkRisksService;
    @Autowired
    private JsonSparkRisksRepo jsonSparkRisksRepo;
    @Autowired
    private ISparkClient sparkClient;

    @Test
    void testSaveEmptyReport() {
        jsonSparkRisksService.save(OGRN, null);

        assertEquals(0, jsonSparkRisksRepo.count());
    }

    @Test
    void serializationTest() {
        var risksReport = new SparkRisksReport(sparkClient.getCompanySparkRisksReportXML(OGRN).getData().getReport());

        jsonSparkRisksService.save(OGRN, risksReport);
        flushAndClear();

        assertEquals(risksReport, jsonSparkRisksService.getSparkRisks(OGRN).get().getStoredEntity());
    }
}
