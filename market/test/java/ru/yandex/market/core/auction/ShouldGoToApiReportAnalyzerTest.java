package ru.yandex.market.core.auction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "ShouldGoToApiReportAnalyzerTest.before.csv")
public class ShouldGoToApiReportAnalyzerTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Test
    void testShouldGoToApiReport() {
        assertTrue(new ShouldGoToApiReportAnalyzer("api.percent", environmentService).shouldGoToApiReport());
    }

}
