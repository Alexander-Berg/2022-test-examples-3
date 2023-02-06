package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.RetryableMbiLmsClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SyncNesuStockStrategyCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;

    @Autowired
    private SyncNesuStockStrategyCommand commandUnderTest;

    @Autowired
    private RetryableMbiLmsClient retryableMbiLmsClient;

    @Autowired
    private NesuClient nesuClient;

    @BeforeEach
    private void setUp() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("badArgs")
    void testBadArgs(String description, CommandInvocation invocation) {
        Assertions.assertThatIllegalArgumentException().isThrownBy(
                () -> SyncNesuStockStrategyCommand.Args.fromCommand(invocation)
        );
    }

    @Test
    @DbUnitDataSet(before = "SyncNesuStockStrategyCommand.before.csv")
    void testExecuteCommand() {
        var invocation = new CommandInvocation("sync-nesu-stock-settings", new String[]{"100,301"}, Map.of());

        when(retryableMbiLmsClient.getLmsPartnersByMbiPartners(anyCollection())).thenReturn(Map.of(
                100L, List.of(PartnerResponse.newBuilder()
                                .id(1005)
                                .partnerType(PartnerType.SUPPLIER)
                                .build(),
                        PartnerResponse.newBuilder()
                                .id(1007)
                                .partnerType(PartnerType.FULFILLMENT)
                                .build()),
                301L, List.of(PartnerResponse.newBuilder().id(1103).partnerType(PartnerType.DROPSHIP).build())
        ));

        commandUnderTest.executeCommand(invocation, terminal);

        verify(nesuClient).setStockSyncStrategy(eq(1103L), eq(301L), eq(false));
        verify(nesuClient).setStockSyncStrategy(eq(1005L), eq(100L), eq(true));
        verifyNoMoreInteractions(nesuClient);
    }

    private static Stream<Arguments> badArgs() {
        return Stream.of(
                Arguments.of(
                        "Too few arguments",
                        new CommandInvocation("sync-nesu-stock-settings", new String[0], Map.of())
                ),
                Arguments.of(
                        "Too many arguments",
                        new CommandInvocation("sync-nesu-stock-settings", new String[]{"123,124", "125"}, Map.of())
                )
        );
    }
}
