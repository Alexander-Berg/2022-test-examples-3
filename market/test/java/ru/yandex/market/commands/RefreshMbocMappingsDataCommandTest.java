package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

public class RefreshMbocMappingsDataCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private RefreshMbocMappingsDataCommand tested;
    @Autowired
    private MboMappingsService patientMboMappingsService;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    @DbUnitDataSet(before = "RefreshMbocMappingsDataCommandTest.before.csv",
            after = "RefreshMbocMappingsDataCommandTest.after.csv")
    void testExecuteCommand() {
        Mockito.when(patientMboMappingsService.searchOfferProcessingStatusesByShopId(Mockito.any())).
                thenReturn(MboMappings.SearchOfferProcessingStatusesResponse.newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
                                        .setOfferCount(3)
                                        .build()
                        )
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.REVIEW)
                                        .setOfferCount(3)
                                        .build()
                        ).build());

        CommandInvocation commandInvocation = new CommandInvocation("refresh-mboc-mappings-data",
                new String[]{"115"},
                Collections.emptyMap());

        tested.executeCommand(commandInvocation, terminal);
    }
}
