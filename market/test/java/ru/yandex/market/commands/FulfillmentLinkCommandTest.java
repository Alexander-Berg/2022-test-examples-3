package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.AsyncTarifficatorService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FulfillmentLinkCommandTest extends FunctionalTest {

    @Autowired
    private FulfillmentLinkCommand command;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private AsyncTarifficatorService asyncTarifficatorService;

    @Test
    @DisplayName("DBS. Создание новой связи")
    @DbUnitDataSet(
            before = "FulfillmentLinkCommand/dbs.create.before.csv",
            after = "FulfillmentLinkCommand/dbs.create.after.csv"
    )
    void testCreateNewFfLink() {
        long partnerId = 123L;
        long serviceId = 45L;

        CommandInvocation commandInvocation = new CommandInvocation("ff-link",
                new String[]{"create", String.valueOf(partnerId), String.valueOf(serviceId)},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        when(lmsClient.getPartner(eq(serviceId))).thenReturn(getMockedLmsResponse(false));
        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("DBS. Обновление фидов в существующей связи")
    @DbUnitDataSet(
            before = "FulfillmentLinkCommand/dbs.refresh-feed.before.csv",
            after = "FulfillmentLinkCommand/dbs.refresh-feed.after.csv"
    )
    void testRefreshFeedsInFfLink() {
        long partnerId = 123L;
        long serviceId = 45L;

        CommandInvocation commandInvocation = new CommandInvocation("ff-link",
                new String[]{"create", String.valueOf(partnerId), String.valueOf(serviceId)},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        when(lmsClient.getPartner(eq(serviceId))).thenReturn(getMockedLmsResponse(false));
        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("DBS. Создание новой связи при наличии другой связи")
    @DbUnitDataSet(
            before = "FulfillmentLinkCommand/dbs.create-secondary.before.csv",
            after = "FulfillmentLinkCommand/dbs.create-secondary.after.csv"
    )
    void testCreateSecondaryFfLink() {
        long partnerId = 123L;
        long serviceId = 45L;

        CommandInvocation commandInvocation = new CommandInvocation("ff-link",
                new String[]{"create", String.valueOf(partnerId), String.valueOf(serviceId)},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        when(lmsClient.getPartner(eq(serviceId))).thenReturn(getMockedLmsResponse(false));
        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("DBS. Удаление одной из связей")
    @DbUnitDataSet(
            before = "FulfillmentLinkCommand/dbs.delete.before.csv",
            after = "FulfillmentLinkCommand/dbs.delete.after.csv"
    )
    void testDeleteFfLink() {
        long partnerId = 123L;
        long serviceId = 45L;

        CommandInvocation commandInvocation = new CommandInvocation("ff-link",
                new String[]{"delete", String.valueOf(partnerId), String.valueOf(serviceId)},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        when(lmsClient.getPartner(eq(serviceId))).thenReturn(getMockedLmsResponse(false));
        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("DBS. Обновление идентификаторов фида одной из связей")
    @DbUnitDataSet(
            before = "FulfillmentLinkCommand/dbs.update.before.csv",
            after = "FulfillmentLinkCommand/dbs.update.after.csv"
    )
    void testUpdateFfLink() {
        long partnerId = 123L;
        long serviceId = 45L;

        CommandInvocation commandInvocation = new CommandInvocation("ff-link",
                new String[]{"update", String.valueOf(partnerId), String.valueOf(serviceId), "0"},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        when(lmsClient.getPartner(eq(serviceId))).thenReturn(getMockedLmsResponse(false));
        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("Синхронизация данных о ФФ-линке со сторонними системами")
    @DbUnitDataSet(
            before = "FulfillmentLinkCommand/dbs.update.before.csv"
    )
    void testSyncFFLink() {
        long partnerId = 123L;

        CommandInvocation commandInvocation = new CommandInvocation("ff-link",
                new String[]{"sync", String.valueOf(partnerId)},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        command.executeCommand(commandInvocation, terminal);

        verify(asyncTarifficatorService, times(2)).syncShopMetaData(eq(123L), any(ActionType.class));
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }

    private static Optional<PartnerResponse> getMockedLmsResponse(boolean allSyncs) {
        return Optional.of(PartnerResponse.newBuilder()
                .id(12L)
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
