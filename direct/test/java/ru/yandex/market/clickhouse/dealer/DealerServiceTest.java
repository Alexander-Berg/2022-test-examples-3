package ru.yandex.market.clickhouse.dealer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.application.monitoring.ComplicatedMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.clickhouse.dealer.config.DealerConfigurationService;
import ru.yandex.market.clickhouse.dealer.config.DealerGlobalConfig;
import ru.yandex.market.clickhouse.dealer.state.DealerDao;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 18.09.18
 */

public class DealerServiceTest {

    DealerService dealerService;
    MonitoringUnit dataMismatchUnit;
    DealerDao dealerDao = Mockito.mock(DealerDao.class);

    @Before
    public void init() {
        DealerConfigurationService configurationService = Mockito.mock(DealerConfigurationService.class);
        Mockito.when(configurationService.getThreadCount()).thenReturn(1);
        Mockito.when(configurationService.getGlobalConfig()).thenReturn(DealerGlobalConfig.newBuilder().build());

        ComplicatedMonitoring cm = new ComplicatedMonitoring();
        dealerService = new DealerService(dealerDao, null, null, configurationService, null, cm);
        dataMismatchUnit = cm.getOrCreateUnit("dataMismatch");
    }

    @Test
    public void testDataMismatchChecker() {
        Mockito.when(dealerDao.getDataMismatchCount(Mockito.any()))
            .thenReturn(0L)
            .thenReturn(2L)
            .thenReturn(100L);

        checkStatus(MonitoringStatus.OK);
        checkStatus(MonitoringStatus.WARNING);
        checkStatus(MonitoringStatus.CRITICAL);
    }

    private void checkStatus(MonitoringStatus expected) {
        dealerService.checkDataMismatches();
        Assert.assertEquals(expected, dataMismatchUnit.getStatus());
    }
}