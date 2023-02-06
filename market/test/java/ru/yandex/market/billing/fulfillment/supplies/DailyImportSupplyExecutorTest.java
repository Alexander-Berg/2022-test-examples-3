package ru.yandex.market.billing.fulfillment.supplies;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тесты для задачи импорта поставок из YT. {@link DailyImportSupplyExecutor}.
 *
 * @author samodurov-d
 */
@ExtendWith(MockitoExtension.class)
class DailyImportSupplyExecutorTest extends FunctionalTest {
    @Autowired
    private DailyImportSupplyExecutor dailyImportSupplyExecutor;

    @Autowired
    private Yt hahnYt;

    @Mock
    private Cypress cypress;

    @BeforeEach
    void init() {
        when(hahnYt.cypress()).thenReturn(cypress);
        when(cypress.exists(any(YPath.class))).thenReturn(true);
    }

    @Test
    @DisplayName("Импорт поставок для биллинга хранения")
    @DbUnitDataSet(
            before = "DailyImportSupplyExecutorTest.before.csv",
            after = "DailyImportSupplyExecutorTest.after.csv"
    )
    void test_doJobImportSupply() {
        dailyImportSupplyExecutor.doJob(null);
    }
}
