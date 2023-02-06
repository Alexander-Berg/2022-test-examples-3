package ru.yandex.market.delivery.mdbapp.integration.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.mdbapp.components.queue.order.to.ship.dto.OrderToShipDto;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.CapacityCountersUpdater;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.CapacityExecutorFactory;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.CapacityService;
import ru.yandex.market.delivery.mdbapp.components.service.notification.TelegramNotificationService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShip;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.CapacityCounterRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.DeletedCapacityCounterRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.DeletedPartnerCapacityRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderToShipRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PartnerCapacityRepository;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.enums.PlatformClient;
import ru.yandex.market.delivery.mdbapp.integration.converter.OrderToShipConverter;
import ru.yandex.market.delivery.mdbclient.model.dto.CapacityServiceType;
import ru.yandex.market.delivery.mdbclient.model.dto.DeliveryType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus.CANCELLED;
import static ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus.CREATED;
import static ru.yandex.market.delivery.mdbapp.enums.PlatformClient.BERU;
import static ru.yandex.market.delivery.mdbapp.enums.PlatformClient.YANDEX_DELIVERY;

@Slf4j
@DisplayName("Отключение подсчета капасити для платформы Беру")
public class DisabledBeruCapacityCounterUpdaterTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private PartnerCapacityRepository partnerCapacityRepository;

    @Mock
    private DeletedPartnerCapacityRepository deletedPartnerCapacityRepository;

    @Mock
    private CapacityCounterRepository capacityCounterRepository;

    @Mock
    private DeletedCapacityCounterRepository deletedCapacityCounterRepository;

    @Mock
    private OrderToShipRepository orderToShipRepository;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @Mock
    private LMSClient lmsClient;

    @Mock
    private PvzLogisticsClient pvzLogisticsClient;

    @Mock
    private CapacityService capacityService;

    private final FeatureProperties properties = new FeatureProperties();

    private CapacityCountersUpdater capacityCountersUpdater;

    private final OrderToShip beruCreatedOrder = createOrderToShip(BERU, CREATED);
    private final OrderToShip ydCreatedOrder = createOrderToShip(YANDEX_DELIVERY, CREATED);
    private final TestableClock clock = new TestableClock();

    @Before
    public void init() {
        clock.setFixed(LocalDateTime.now().toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        properties.setDisableBeruCapacityCount(true);
        capacityCountersUpdater = new CapacityCountersUpdater(
            capacityService,
            partnerCapacityRepository,
            deletedPartnerCapacityRepository,
            capacityCounterRepository,
            deletedCapacityCounterRepository,
            orderToShipRepository,
            telegramNotificationService,
            lmsClient,
            clock,
            new CapacityExecutorFactory() {
                @Override
                public Executor createExecutor() {
                    return Runnable::run;
                }
            },
            1,
            properties
        );
    }

    @After
    public void tearDown() {
        clock.clearFixed();
        verifyNoMoreInteractions(capacityService, lmsClient, pvzLogisticsClient);
    }

    @Test
    @DisplayName("Счетчик не обновляется для созданного заказа Беру")
    public void updateForCreatedDoesntUpdateCapacity() {
        capacityCountersUpdater.update(OrderToShipDto.of(beruCreatedOrder));
    }

    @Test
    @DisplayName("Счетчик не обновляется для отмененого заказа Беру")
    public void updateForCancelledDoesntUpdateCapacity() {
        capacityCountersUpdater.update(OrderToShipDto.of(createOrderToShip(BERU, CANCELLED)));
    }

    @Test
    @DisplayName("Счетчик обновляется для созданного заказа ЯДо")
    public void updateForYDWorksWell() {
        OrderToShipDto dto = OrderToShipDto.of(ydCreatedOrder);
        capacityCountersUpdater.update(dto);
        verify(capacityService).getCapacityCountersForIncrement(eq(dto), eq(List.of()));
        verify(capacityService).incrementCounters(eq(List.of()), eq(dto));
    }

    @Test
    @DisplayName("Обновление обработанных заказов после изменения настроек капасити не обновляет заказы Беру")
    public void updateProcessedCountersDoesntUpdateCounters() {
        OrderToShipDto ydDto = OrderToShipDto.of(ydCreatedOrder);
        List<OrderToShip> orders = List.of(ydCreatedOrder, beruCreatedOrder);
        when(orderToShipRepository.findAllProcessedWithOnlyOneStatusByPartnerId(eq(LocalDate.now(clock)), eq(123L)))
            .thenReturn(orders);
        capacityCountersUpdater.updateProcessedCounters(123L, List.of());

        verify(capacityService).getCapacityCountersForIncrement(eq(ydDto), eq(List.of()));
        verify(capacityService).incrementCounters(eq(List.of()), eq(ydDto));
    }

    @Test
    @DisplayName("Декремент счетчика не работает для Беру")
    public void decrementDoesntUpdateCounters() {
        capacityCountersUpdater.decrementCountersAndDeleteDayOffs(OrderToShipDto.of(beruCreatedOrder));
    }

    @Test
    @DisplayName("Инкремент счетчика не работает для Беру")
    public void incrementDoesntUpdateCounters() {
        capacityCountersUpdater.incrementCountersAndCreateDayOffs(OrderToShipDto.of(beruCreatedOrder));
    }

    @Nonnull
    private OrderToShip createOrderToShip(PlatformClient platformClient, OrderToShipStatus status) {
        return new OrderToShip()
            .setId(OrderToShipConverter.toOrderToShipId(
                "123", platformClient.getId(), 123L, CapacityServiceType.DELIVERY, status))
            .setLocationFromId(1L)
            .setLocationToId(213L)
            .setDeliveryType(DeliveryType.DELIVERY)
            .setShipmentDay(LocalDate.now());
    }
}
