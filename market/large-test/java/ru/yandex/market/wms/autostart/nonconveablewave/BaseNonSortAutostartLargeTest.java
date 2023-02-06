package ru.yandex.market.wms.autostart.nonconveablewave;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.wms.autostart.autostartlogic.ReservationWaveLogic;
import ru.yandex.market.wms.autostart.autostartlogic.ResilienceLogic;
import ru.yandex.market.wms.autostart.autostartlogic.dao.WaveLogDao;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.AosWaveTypeStartSequenceProvider;
import ru.yandex.market.wms.autostart.autostartlogic.service.AosWaveServiceImpl;
import ru.yandex.market.wms.autostart.autostartlogic.service.DeliveryCutOffService;
import ru.yandex.market.wms.autostart.autostartlogic.service.SortingStationService;
import ru.yandex.market.wms.autostart.autostartlogic.service.interfaces.WavingService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.AutostartWavingService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CategorizedOrder;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.WaveInitialization;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.DeliveryOrderDataProcessor;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.InventoryValidatorImpl;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.InventoryValidatorService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.OrderCategorizator;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.WaveSettingsOverrideService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettingsCreator;
import ru.yandex.market.wms.autostart.repository.StationToCarrierRepository;
import ru.yandex.market.wms.autostart.settings.service.AutostartSettingsService;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.service.DimensionsConfigService;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.PackDaoImpl;
import ru.yandex.market.wms.common.spring.dao.implementation.WaveDao;
import ru.yandex.market.wms.common.spring.service.time.WarehouseDateTimeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {BaseNonSortAutostartLargeTest.DefaultTestConfiguration.class})
@SpringBootTest(classes = {BaseTestConfig.class, IntegrationTestConfig.class})
public abstract class BaseNonSortAutostartLargeTest extends TestcontainersConfiguration {

    @Autowired
    @SpyBean
    protected AosWaveServiceImpl autostartLogic;
    @Autowired
    protected ReservationWaveLogic reservationWaveLogic;
    @Mock
    protected OrderDao orderDao;
    @Autowired
    protected WaveDao waveDao;
    @Autowired
    protected OrderDetailDao orderDetailDao;
    @Autowired
    protected PackDaoImpl packDao;
    @Autowired
    protected Clock clock;
    @Autowired
    protected ResilienceLogic resilienceLogic;
    @Autowired
    @SpyBean
    protected AutostartSettingsService settingsService;
    @Autowired
    protected SortingStationService sortingStationService;
    @Autowired
    protected DeliveryCutOffService deliveryCutOffService;
    @Autowired
    protected WarehouseDateTimeService dateTimeService;
    @Autowired
    protected DimensionsConfigService dimensionsConfigService;
    @Autowired
    protected DeliveryOrderDataProcessor deliveryOrderDataProcessor;
    @Autowired
    protected DbConfigService dbConfigService;
    @Autowired
    protected OrderCategorizator orderCategorizator;
    @Autowired
    protected WaveLogDao waveLogDao;
    @Autowired
    protected InventoryValidatorService inventoryValidatorService;
    @Autowired
    protected StationToCarrierRepository stationToCarrierRepository;
    @Autowired
    protected WaveSettingsOverrideService waveSettingsOverrideService;

    private AutoCloseable openedMocks;

    @TestConfiguration
    static class DefaultTestConfiguration {
        @Mock
        InventoryValidatorService inventoryValidatorService;
        @Mock
        InventoryValidatorImpl inventoryValidator;

        @Bean
        @Primary
        public Clock clock() {
            return Clock.fixed(Instant.parse("2020-02-29T15:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        @Primary
        public InventoryValidatorService inventoryValidatorService() {
            MockitoAnnotations.initMocks(this);
            when(inventoryValidatorService.buildEntity(anyList(), nullable(Integer.class)))
                    .thenReturn(inventoryValidator);
            when(inventoryValidator.canAddOrderToTotal(any(Order.class))).thenReturn(Optional.empty());
            return inventoryValidatorService;
        }
    }

    @BeforeEach
    public void beforeEach() {
        openedMocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void afterEach() throws Exception {
        openedMocks.close();
        Mockito.reset(autostartLogic, settingsService);
    }

    protected WavingService getWavingService(int count) {

        Map<SkuId, Long> skuIdBalances = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            skuIdBalances.put(SkuId.of("100", String.format("ROV00000000000000000%02d", i)), 100L);
        }

        when(orderDao.getAllSkuIdInDetailsOnStock(anyList(), isNull(), anyBoolean())).thenReturn(skuIdBalances);

        when(settingsService.getOrdersIntoPutWall()).thenReturn(count);
        WaveSettingsCreator waveSettingsCreator = new WaveSettingsCreator(
                settingsService, dateTimeService, clock);
        WaveInitialization waveInitialization = new WaveInitialization(
                waveSettingsCreator, deliveryOrderDataProcessor, settingsService,
                stationToCarrierRepository, waveSettingsOverrideService
        );
        return new AutostartWavingService(new AosWaveTypeStartSequenceProvider(), deliveryCutOffService,
                orderCategorizator, waveInitialization, waveLogDao);
    }

    protected WavingService buildWavingService() {
        return getWavingService(4);
    }

    protected WavingService buildWavingService(int count) {
        return getWavingService(count);
    }

    protected AosWaveServiceImpl configureAutoStartLogic(Collection<CategorizedOrder> orders) {
        when(autostartLogic.getOrderDetails(anyList()))
                .thenReturn(orderDetailDao.findOrderDetails(orders.stream().map(CategorizedOrder::getOrder).collect(
                        Collectors.toList())));
        return autostartLogic;
    }
}
