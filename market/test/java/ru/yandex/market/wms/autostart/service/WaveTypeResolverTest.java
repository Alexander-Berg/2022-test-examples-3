package ru.yandex.market.wms.autostart.service;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.wms.autostart.settings.service.ManualStarterSettingsService;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class WaveTypeResolverTest extends BaseTest {

    private static ProcessTypeProvider processTypeProvider;

    @BeforeAll
    static void setUp() {
        processTypeProvider = new ProcessTypeProvider();
    }

    @ParameterizedTest
    @EnumSource(value = OrderType.class, names = { "OUTBOUND_FIT", "OUTBOUND_WH_2_WH",
            "OUTBOUND_WH_2_WH_DMG", "OUTBOUND_WH_2_WH_EXP",
            "OUTBOUND_DEFECT", "OUTBOUND_EXPIRED", "OUTBOUND_SURPLUS",
            "MANUAL_UTILIZATION_OUTBOUND", "PLAN_UTILIZATION_OUTBOUND",
            "OUTBOUND_DEFECT_1P_SALE", "OUTBOUND_AUCTION", "OUTBOUND_AUTO",
            "OUTBOUND_FIX_LOST_INVENTARIZATION", "OUTBOUND_OPER_LOST_INVENTARIZATION" })
    public void resolveBigWithdrawalWhenEnabled(OrderType type) {
        var manualStarterSettingsService = mock(ManualStarterSettingsService.class);
        doReturn(true).when(manualStarterSettingsService).getBigWithdrawalEnabled();

        var resolver = new WaveTypeResolver(null, null, processTypeProvider, manualStarterSettingsService, null);

        var order = Order.builder()
                .type(type.getCode())
                .build();

        var resolvedType = resolver.resolve(List.of(order));

        assertions.assertThat(resolvedType).isEqualTo(WaveType.BIG_WITHDRAWAL);
    }

    @ParameterizedTest
    @EnumSource(value = OrderType.class, names = { "OUTBOUND_FIT", "OUTBOUND_WH_2_WH",
            "OUTBOUND_WH_2_WH_DMG", "OUTBOUND_WH_2_WH_EXP",
            "OUTBOUND_DEFECT", "OUTBOUND_EXPIRED", "OUTBOUND_SURPLUS",
            "MANUAL_UTILIZATION_OUTBOUND", "PLAN_UTILIZATION_OUTBOUND",
            "OUTBOUND_DEFECT_1P_SALE", "OUTBOUND_AUCTION", "OUTBOUND_AUTO",
            "OUTBOUND_FIX_LOST_INVENTARIZATION", "OUTBOUND_OPER_LOST_INVENTARIZATION" })
    public void resolveBigWithdrawalWhenDisabled(OrderType type) {
        var manualStarterSettingsService = mock(ManualStarterSettingsService.class);
        doReturn(false).when(manualStarterSettingsService).getBigWithdrawalEnabled();

        var resolver = new WaveTypeResolver(null, null, processTypeProvider, manualStarterSettingsService, null);

        var order = Order.builder()
                .type(type.getCode())
                .build();

        var resolvedType = resolver.resolve(List.of(order));

        assertions.assertThat(resolvedType).isEqualTo(WaveType.WITHDRAWAL);
    }

    @ParameterizedTest
    @EnumSource(value = OrderType.class, names = { "OUTBOUND_FIT", "OUTBOUND_WH_2_WH",
            "OUTBOUND_WH_2_WH_DMG", "OUTBOUND_WH_2_WH_EXP",
            "OUTBOUND_DEFECT", "OUTBOUND_EXPIRED", "OUTBOUND_SURPLUS",
            "MANUAL_UTILIZATION_OUTBOUND", "PLAN_UTILIZATION_OUTBOUND",
            "OUTBOUND_DEFECT_1P_SALE", "OUTBOUND_AUCTION", "OUTBOUND_AUTO",
            "OUTBOUND_FIX_LOST_INVENTARIZATION", "OUTBOUND_OPER_LOST_INVENTARIZATION" })
    public void resolveWithdrawalWhenEnabled(OrderType type) {
        var manualStarterSettingsService = mock(ManualStarterSettingsService.class);
        doReturn(true).when(manualStarterSettingsService).getBigWithdrawalEnabled();

        var resolver = new WaveTypeResolver(null, null, processTypeProvider, manualStarterSettingsService, null);

        var order1 = Order.builder()
                .type(type.getCode())
                .build();
        var order2 = Order.builder()
                .type(type.getCode())
                .build();

        var resolvedType = resolver.resolve(List.of(order1, order2));

        assertions.assertThat(resolvedType).isEqualTo(WaveType.WITHDRAWAL);
    }

    @ParameterizedTest
    @EnumSource(value = OrderType.class, names = { "OUTBOUND_FIT", "OUTBOUND_WH_2_WH",
            "OUTBOUND_WH_2_WH_DMG", "OUTBOUND_WH_2_WH_EXP",
            "OUTBOUND_DEFECT", "OUTBOUND_EXPIRED", "OUTBOUND_SURPLUS",
            "MANUAL_UTILIZATION_OUTBOUND", "PLAN_UTILIZATION_OUTBOUND",
            "OUTBOUND_DEFECT_1P_SALE", "OUTBOUND_AUCTION", "OUTBOUND_AUTO",
            "OUTBOUND_FIX_LOST_INVENTARIZATION", "OUTBOUND_OPER_LOST_INVENTARIZATION" })
    public void resolveWithdrawalWhenDisabled(OrderType type) {
        var manualStarterSettingsService = mock(ManualStarterSettingsService.class);
        doReturn(false).when(manualStarterSettingsService).getBigWithdrawalEnabled();

        var resolver = new WaveTypeResolver(null, null, processTypeProvider, manualStarterSettingsService, null);

        var order1 = Order.builder()
                .type(type.getCode())
                .build();
        var order2 = Order.builder()
                .type(type.getCode())
                .build();

        var resolvedType = resolver.resolve(List.of(order1, order2));

        assertions.assertThat(resolvedType).isEqualTo(WaveType.WITHDRAWAL);
    }
}
