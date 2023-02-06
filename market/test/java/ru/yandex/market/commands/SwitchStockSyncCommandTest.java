package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.logistics.management.client.LmsHttpClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class SwitchStockSyncCommandTest extends FunctionalTest {

    @Autowired
    private SwitchStockSyncCommand switchStockSyncCommand;

    @Autowired
    private ParamService paramService;

    @Autowired
    private LmsHttpClient lmsClient;

    @BeforeEach
    public void before() {
        Mockito.reset(lmsClient);
    }

    @Test
    @DbUnitDataSet(
            before = "SwitchStockSyncCommandTest.before.csv"
    )
    @DisplayName("Обновление настроек логистического партнёра при изменении параметра IGNORE_STOCKS")
    void testExecutionWithParamChangeLeadsToPartnerSettingsUpdate() {
        long partnerId = 12L;
        long logisticPartnerId = 1000L;
        when(lmsClient.getPartner(eq(logisticPartnerId))).thenReturn(getMockedLmsResponse(true));

        boolean ignoreStocksBefore = getIgnoreStocks(partnerId);
        executeCommand("false");
        boolean ignoreStocksAfter = getIgnoreStocks(partnerId);
        assertFalse(ignoreStocksBefore);
        assertTrue(ignoreStocksAfter);

        Mockito.verify(lmsClient, times(1))
                .updatePartnerSettings(Mockito.eq(logisticPartnerId), Mockito.any());
    }


    @Test
    @DbUnitDataSet(
            before = "SwitchStockSyncCommandTest.before.csv"
    )
    @DisplayName(
            "Не обновляются настройки логистического партнёра, если значение параметра IGNORE_STOCKS не изменилось"
    )
    void testExecutionWithoutParamChangeDoesntLeadToPartnerSettingsUpdate() {
        long partnerId = 12L;
        long logisticPartnerId = 1000L;
        when(lmsClient.getPartner(eq(logisticPartnerId))).thenReturn(getMockedLmsResponse(true));

        boolean ignoreStocksBefore = getIgnoreStocks(partnerId);
        executeCommand("true");
        boolean ignoreStocksAfter = getIgnoreStocks(partnerId);
        assertFalse(ignoreStocksBefore);
        assertFalse(ignoreStocksAfter);

        Mockito.verify(lmsClient, times(0))
                .updatePartnerSettings(Mockito.eq(logisticPartnerId), Mockito.any());
    }

    private Boolean getIgnoreStocks(long partnerId) {
        return Optional.ofNullable(paramService.getParam(ParamType.IGNORE_STOCKS, partnerId))
                .map(ParamValue::getValueAsBoolean)
                .orElse(false);
    }


    private void executeCommand(String... commandArguments) {
        final CommandInvocation commandInvocation = commandInvocation(commandArguments);
        final Terminal terminal = createTerminal();

        switchStockSyncCommand.executeCommand(commandInvocation, terminal);
    }


    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }

    private static CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("switch-stock-sync", args, Collections.emptyMap());
    }

    private static Optional<PartnerResponse> getMockedLmsResponse(boolean allSyncs) {
        return Optional.of(PartnerResponse.newBuilder()
                .id(1000L)
                .autoSwitchStockSyncEnabled(allSyncs)
                .korobyteSyncEnabled(allSyncs)
                .stockSyncEnabled(allSyncs)
                .stockSyncSwitchReason(StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL)
                .status(PartnerStatus.FROZEN)
                .locationId(1)
                .trackingType("TRACK")
                .build());
    }
}
