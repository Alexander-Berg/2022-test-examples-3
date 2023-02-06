package ru.yandex.market.wms.packing;

import org.springframework.stereotype.Component;

import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao;
import ru.yandex.market.wms.common.spring.service.ClickHouseTaskActionLogConsumer;
import ru.yandex.market.wms.common.spring.service.EmptyToteService;
import ru.yandex.market.wms.common.spring.service.balance.BalanceService;
import ru.yandex.market.wms.common.spring.service.time.WarehouseDateTimeService;
import ru.yandex.market.wms.packing.logging.PackingAlgoLogger;
import ru.yandex.market.wms.packing.service.MetricService;
import ru.yandex.market.wms.packing.service.PackingTableService;
import ru.yandex.market.wms.packing.service.PackingTaskService;
import ru.yandex.market.wms.packing.service.PickByLightService;
import ru.yandex.market.wms.packing.service.PromoTaskService;
import ru.yandex.market.wms.packing.service.SettingsService;
import ru.yandex.market.wms.packing.worker.Manager;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

@Component
public class TestManagerWrapper extends Manager {

    @SuppressWarnings("checkstyle:ParameterNumber")
    public TestManagerWrapper(PackingTaskService taskService, SettingsService settingsService,
                              PackingTableService tableService, WarehouseDateTimeService dateTimeService,
                              ClickHouseTaskActionLogConsumer taskActionLogConsumer, MetricService metricService,
                              BalanceService balanceService, EmptyToteService emptyToteService,
                              PromoTaskService promoTaskService, LotLocIdDao lotLocIdDao,
                              PickByLightService pickByLightService, SecurityDataProvider securityDataProvider,
                              PackingAlgoLogger packingAlgoLogger) {
        super(taskService, tableService, settingsService, dateTimeService, taskActionLogConsumer, metricService,
                balanceService, emptyToteService, promoTaskService, lotLocIdDao, pickByLightService,
                securityDataProvider, packingAlgoLogger);
    }

    public void reload() {
        consumerData.clear();
        loadTickets();
    }
}
