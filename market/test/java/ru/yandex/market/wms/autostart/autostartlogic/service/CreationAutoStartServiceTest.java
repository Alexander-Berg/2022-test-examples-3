package ru.yandex.market.wms.autostart.autostartlogic.service;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.autostart.model.AosWave;
import ru.yandex.market.wms.autostart.settings.service.AutostartSettingsService;
import ru.yandex.market.wms.common.exception.OptimisticLockException;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderStatusHistoryDao;
import ru.yandex.market.wms.trace.Module;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreationAutoStartServiceTest {

    @InjectMocks
    private CreationAutoStartService creationAutoStartService;

    @Mock
    private OrderDao orderDao;

    @Mock
    private OrderStatusHistoryDao orderStatusHistoryDao;

    @Mock
    private AutostartSettingsService settingsService;

    @Test
    void setStatusForNotSuitableOrdersWhenNotChangeOrderStatusTest() {
        List<Order> notSuitableOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        AosWave aosWave = AosWave.builder().notSuitableOrders(notSuitableOrders).build();
        String editor = Module.AUTOSTART.name();
        String comment = "Не запущено автостартом";

        when(settingsService.isChangeOrderStatusIfNotEnoughStock()).thenReturn(false);

        creationAutoStartService.setStatusForNotSuitableOrders(aosWave);

        verify(orderDao, times(0))
                .setStatusForNotSuitableOrdersForAos(notSuitableOrders, editor);
        verify(orderDao, times(1))
                .setMarkNotSuitableOrdersForAos(notSuitableOrders, comment, editor);
        verify(orderStatusHistoryDao, times(0)).createOrderStatusHistories(notSuitableOrders);
    }

    @Test
    void setStatusForNotSuitableOrdersWhenChangeOrderStatusAndChangesSuccessfulTest() {
        List<Order> notSuitableOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        AosWave aosWave = AosWave.builder().notSuitableOrders(notSuitableOrders).build();
        String editor = Module.AUTOSTART.name();
        String comment = "Не запущено автостартом";
        int updatesCount = 2;

        when(orderDao.setStatusForNotSuitableOrdersForAos(notSuitableOrders, editor)).thenReturn(updatesCount);
        when(settingsService.isChangeOrderStatusIfNotEnoughStock()).thenReturn(true);

        creationAutoStartService.setStatusForNotSuitableOrders(aosWave);

        verify(orderDao, times(1))
                .setStatusForNotSuitableOrdersForAos(notSuitableOrders, editor);
        verify(orderDao, times(0))
                .setMarkNotSuitableOrdersForAos(notSuitableOrders, comment, editor);
        verify(orderStatusHistoryDao, times(1)).createOrderStatusHistories(notSuitableOrders);
    }

    @Test
    void setStatusForNotSuitableOrdersWhenChangeOrderStatusAndOptimisticLockExceptionTest() {
        List<Order> notSuitableOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        AosWave aosWave = AosWave.builder().notSuitableOrders(notSuitableOrders).build();
        String editor = Module.AUTOSTART.name();
        String comment = "Не запущено автостартом";
        int updatesCount = 1;

        when(orderDao.setStatusForNotSuitableOrdersForAos(notSuitableOrders, editor)).thenReturn(updatesCount);
        when(settingsService.isChangeOrderStatusIfNotEnoughStock()).thenReturn(true);

        Assertions.assertThrows(OptimisticLockException.class, () -> creationAutoStartService
                .setStatusForNotSuitableOrders(aosWave));

        verify(orderDao, times(1))
                .setStatusForNotSuitableOrdersForAos(notSuitableOrders, editor);
        verify(orderDao, times(0))
                .setMarkNotSuitableOrdersForAos(notSuitableOrders, comment, editor);
        verify(orderStatusHistoryDao, times(0)).createOrderStatusHistories(notSuitableOrders);
    }

    @Test
    void setMarkNotSuitableOrdersForAosWithNotSuitableOrdersTest() {
        String editor = Module.AUTOSTART.name();
        int updatesCount = 2;
        List<Order> notSuitableOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        AosWave aosWave = AosWave.builder().notSuitableOrders(notSuitableOrders).build();

        when(orderDao.setStatusForNotSuitableOrdersForAos(notSuitableOrders, editor)).thenReturn(updatesCount);
        when(settingsService.isChangeOrderStatusIfNotEnoughStock()).thenReturn(true);

        creationAutoStartService.setMarkNotSuitableOrdersForAos(aosWave);

        verify(orderDao, times(1))
                .setStatusForNotSuitableOrdersForAos(notSuitableOrders, editor);
        verify(orderDao, times(0)).setMarkNotSuitableOrdersForAos(any(), any(), any());
        verify(orderStatusHistoryDao, times(1)).createOrderStatusHistories(notSuitableOrders);
    }

    @Test
    void setMarkNotSuitableOrdersForAosWithBigOrdersTest() {
        List<Order> bigOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        AosWave aosWave = AosWave.builder().bigOrders(bigOrders).build();
        String comment = "Большой заказ";
        String editor = Module.AUTOSTART.name();

        creationAutoStartService.setMarkNotSuitableOrdersForAos(aosWave);

        verify(orderDao, times(0)).setStatusForNotSuitableOrdersForAos(any(), any());
        verify(orderDao, times(1)).setMarkNotSuitableOrdersForAos(bigOrders, comment, editor);
        verify(orderStatusHistoryDao, times(0)).createOrderStatusHistories(any());
    }

    @Test
    void setMarkNotSuitableOrdersForAosWithOversizeOrdersTest() {
        List<Order> oversizeOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        AosWave aosWave = AosWave.builder().oversizeOrders(oversizeOrders).build();
        String comment = "Заказ с нонстор товарами";
        String editor = Module.AUTOSTART.name();

        creationAutoStartService.setMarkNotSuitableOrdersForAos(aosWave);

        verify(orderDao, times(0)).setStatusForNotSuitableOrdersForAos(any(), any());
        verify(orderDao, times(1))
                .setMarkNotSuitableOrdersForAos(oversizeOrders, comment, editor);
        verify(orderStatusHistoryDao, times(0)).createOrderStatusHistories(any());
    }

    private static Stream<Arguments> setMarkNotSuitableOrdersForAosTestArgs() {
        List<Order> notSuitableOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        List<Order> bigOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        List<Order> oversizeOrders = asList(
                Order.builder().build(),
                Order.builder().build()
        );
        AosWave aosWave1 = AosWave.builder().notSuitableOrders(notSuitableOrders).build();
        AosWave aosWave2 = AosWave.builder().bigOrders(bigOrders).build();
        AosWave aosWave3 = AosWave.builder().oversizeOrders(oversizeOrders).build();

        return Stream.of(
                Arguments.of(
                        true,
                        aosWave1,
                        Module.AUTOSTART.name(),
                        "Не запущено автостартом"
                ),
                Arguments.of(
                        false,
                        aosWave2,
                        Module.AUTOSTART.name(),
                        "Большой заказ"
                ),
                Arguments.of(
                        false,
                        aosWave3,
                        Module.AUTOSTART.name(),
                        "Заказ с нонстор товарами"
                )
        );
    }
}
