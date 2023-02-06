package ru.yandex.market.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.core.delivery.DbDeliveryInfoService;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;
import ru.yandex.market.core.delivery.region_blacklist.dao.DeliveryRegionBlacklistYtDao;
import ru.yandex.market.core.delivery.region_blacklist.model.DeliveryRegionBlacklist;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DeliveryRegionBlacklistChangeCommandTest {
    private static final Instant NOW = Instant.parse("2021-11-23T05:45:15Z");

    DeliveryRegionBlacklistChangeCommand command;
    DeliveryRegionBlacklistYtDao daoMock;
    DbDeliveryInfoService deliveryInfoService;
    TestableClock clock;

    private ByteArrayInputStream is;
    private ByteArrayOutputStream os;
    private Terminal terminal;

    @BeforeEach
    void setUp() {
        daoMock = mock(DeliveryRegionBlacklistYtDao.class);
        deliveryInfoService = mock(DbDeliveryInfoService.class);
        clock = new TestableClock();
        clock.setFixed(NOW, ZoneId.systemDefault());

        command = new DeliveryRegionBlacklistChangeCommand(daoMock, deliveryInfoService, clock);

        is = new ByteArrayInputStream(new byte[0]);
        os = new ByteArrayOutputStream();
        terminal = new Terminal(is, os) {
            @Override
            protected void onStart() {

            }

            @Override
            protected void onClose() {

            }
        };
    }

    @Test
    void get() {
        when(daoMock.getPartnerDeliveryRegionBlacklists(12345L))
                .thenReturn(getTestBlacklists());

        executeCommand("get", "12345");

        verify(daoMock).getPartnerDeliveryRegionBlacklists(12345L);
        verifyNoMoreInteractions(daoMock);
        verifyNoInteractions(deliveryInfoService);

        assertThat(getOutput(), equalTo("" +
                "partnerId: 12345, warehouseId: 23456, regionsBlacklist: [54321, 65432]," +
                " updatedAt: 2021-11-23T06:32:15Z\n" +
                "partnerId: 12345, warehouseId: 3456, regionsBlacklist: [65432]," +
                " updatedAt: 2021-11-23T06:33:15Z\n"
        ));
    }

    @Test
    void setByPartnerWarehouse() {
        executeCommand("set", "12340", "23400", "234,34,5");

        verify(daoMock).setPartnerDeliveryRegionBlacklist(
                DeliveryRegionBlacklist.builder()
                        .setPartnerId(12340L)
                        .setWarehouseId(23400L)
                        .setRegions(List.of(234L, 34L, 5L))
                        .setUpdatedAt(NOW)
                        .build()
        );
        verifyNoMoreInteractions(daoMock);
        verifyNoInteractions(deliveryInfoService);

        assertThat(getOutput(), equalTo("" +
                "The following blacklists will be set:\n" +
                "partnerId: 12340, warehouseId: 23400, regionsBlacklist: [234, 34, 5]," +
                " updatedAt: 2021-11-23T05:45:15Z\n" +
                "done\n"
        ));
    }

    @Test
    void setByPartner() {
        when(deliveryInfoService.getAvailableDeliveryServices(eq(12340L), any(Set.class)))
                .thenReturn(getTestWarehouses(List.of(234L, 3456L)));

        executeCommand("set", "12340", "234,34,5");

        verify(daoMock).setPartnerDeliveryRegionBlacklist(
                DeliveryRegionBlacklist.builder()
                        .setPartnerId(12340L)
                        .setWarehouseId(234L)
                        .setRegions(List.of(234L, 34L, 5L))
                        .setUpdatedAt(NOW)
                        .build()
        );
        verify(daoMock).setPartnerDeliveryRegionBlacklist(
                DeliveryRegionBlacklist.builder()
                        .setPartnerId(12340L)
                        .setWarehouseId(3456L)
                        .setRegions(List.of(234L, 34L, 5L))
                        .setUpdatedAt(NOW)
                        .build()
        );
        verifyNoMoreInteractions(daoMock);

        assertThat(getOutput(), equalTo("" +
                "The following blacklists will be set:\n" +
                "partnerId: 12340, warehouseId: 3456, regionsBlacklist: [234, 34, 5]," +
                " updatedAt: 2021-11-23T05:45:15Z\n" +
                "partnerId: 12340, warehouseId: 234, regionsBlacklist: [234, 34, 5]," +
                " updatedAt: 2021-11-23T05:45:15Z\n" +
                "done\n"
        ));
    }

    private List<DeliveryServiceInfo> getTestWarehouses(List<Long> warehouseIds) {
        return warehouseIds.stream()
                .map(id -> new DeliveryServiceInfo(id, "warehouse" + id))
                .collect(Collectors.toList());
    }

    @Test
    void deleteByPartnerWarehouse() {
        executeCommand("delete", "12340", "23400");

        verify(daoMock).deleteDeliveryRegionBlacklist(12340L, 23400L);
        verifyNoMoreInteractions(daoMock);
        verifyNoInteractions(deliveryInfoService);

        assertThat(getOutput(), equalTo("" +
                "Region blacklist will be deleted for partner: 12340 warehouses: [23400]\n" +
                "done\n"
        ));
    }

    @Test
    void deleteByPartner() {
        when(daoMock.getPartnerDeliveryRegionBlacklists(12345L))
                .thenReturn(getTestBlacklists());

        executeCommand("delete", "12345");

        verify(daoMock).getPartnerDeliveryRegionBlacklists(12345L);
        verify(daoMock).deleteDeliveryRegionBlacklist(12345L, 23456L);
        verify(daoMock).deleteDeliveryRegionBlacklist(12345L, 3456L);
        verifyNoMoreInteractions(daoMock);
        verifyNoInteractions(deliveryInfoService);

        assertThat(getOutput(), equalTo("" +
                "Region blacklist will be deleted for partner: 12345 warehouses: [23456, 3456]\n" +
                "done\n"
        ));
    }

    private void executeCommand(String... arguments) {
        CommandInvocation commandInvocation = new CommandInvocation("delivery-region-blacklist",
                Arrays.array(arguments),
                Map.of());
        os.reset();

        command.executeCommand(commandInvocation, terminal);
        terminal.getWriter().flush();
    }

    private String getOutput() {
        return os.toString();
    }

    private List<DeliveryRegionBlacklist> getTestBlacklists() {
        return List.of(
                DeliveryRegionBlacklist.builder()
                        .setPartnerId(12345L)
                        .setWarehouseId(23456L)
                        .setRegions(List.of(54321L, 65432L))
                        .setUpdatedAt(Instant.parse("2021-11-23T06:32:15Z"))
                        .build(),
                DeliveryRegionBlacklist.builder()
                        .setPartnerId(12345L)
                        .setWarehouseId(3456L)
                        .setRegions(List.of(65432L))
                        .setUpdatedAt(Instant.parse("2021-11-23T06:33:15Z"))
                        .build()
        );
    }
}
