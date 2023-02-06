package ru.yandex.market.marketid;

import java.util.Collections;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.http.BadRequestException;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.terminal.TestTerminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


class MarketIdCommandTest extends FunctionalTest {

    private static final TestTerminal TEST_TERMINAL = new TestTerminal();

    @Autowired
    private MarketIdCommand command;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    private static CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("market-id", args, Collections.emptyMap());
    }

    @BeforeEach
    void init() {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(1001).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());

        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(1001).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());
    }

    @DbUnitDataSet(before = "MarketIdCommandTest.before.csv")
    @DisplayName("Тест команды на создание market-id по id партнера.")
    @Test
    void executeGetOrCreateCommand() {
        CommandInvocation commandInvocation = commandInvocation("get-or-create", "101");
        command.executeCommand(commandInvocation, TEST_TERMINAL);
        verify(marketIdServiceImplBase, times(1)).getOrCreateMarketId(any(), any());
    }

    @DbUnitDataSet(before = "MarketIdCommandTest.before.csv", after = "MarketIdCommandTestLinkOrCreate.after.csv")
    @DisplayName("Тест команды на создание market-id и ее связь с партнером по id партнера и id заявки.")
    @Test
    void executeLinkOrCreateCommandWithoutMarketId() {
        CommandInvocation commandInvocation = commandInvocation("link-or-create-market-id", "101", "120446");
        command.executeCommand(commandInvocation, TEST_TERMINAL);
        verify(marketIdServiceImplBase, times(1)).getOrCreateMarketId(any(), any());
        verify(marketIdServiceImplBase, never()).linkMarketIdRequest(any(), any());
    }

    @DbUnitDataSet(before = "MarketIdCommandTest.before.csv")
    @DisplayName("Тест команды на связь указанного market-id с партнером по id партнера и id заявки.")
    @Test
    void executeLinkOrCreateCommandWithMarketId() {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(1002).build());
            marketAccountStreamObserver.onCompleted();
            return true;
        }).when(marketIdServiceImplBase).linkMarketIdRequest(any(), any());

        CommandInvocation commandInvocation = commandInvocation("link-or-create-market-id", "101", "120446", "1002");
        command.executeCommand(commandInvocation, TEST_TERMINAL);
        verify(marketIdServiceImplBase, never()).getOrCreateMarketId(any(), any());
        verify(marketIdServiceImplBase, only()).linkMarketIdRequest(any(), any());
    }

    @DbUnitDataSet(before = "MarketIdCommandTest.before.csv")
    @DisplayName("Тест команды на добавление партнера в nesu.")
    @Test
    void executeRegisterInNesu() {
        CommandInvocation commandInvocation = commandInvocation("register-in-nesu", "102", "120447");
        command.executeCommand(commandInvocation, TEST_TERMINAL);
        ArgumentCaptor<RegisterShopDto> nesuClientShopCaptor = ArgumentCaptor.forClass(RegisterShopDto.class);
        verify(nesuClient, times(1)).registerShop(nesuClientShopCaptor.capture());
        assertEquals(
                RegisterShopDto.builder()
                        .id(102L)
                        .marketId(1003L)
                        .regionId(213)
                        .role(ShopRole.DROPSHIP)
                        .balanceClientId(111002L)
                        .balanceContractId(351873L)
                        .balancePersonId(43L)
                        .name("ООО Ромашишка 2")
                        .businessId(1002L)
                        .build(),
                nesuClientShopCaptor.getValue());
    }

    @DbUnitDataSet(before = "MarketIdCommandTest.before.csv")
    @DisplayName("Тест команды на добавление партнера в nesu для DBS.")
    @Test
    void executeRegisterDbsPartnerInNesu() {
        long partnerId = 103;
        CommandInvocation commandInvocation = commandInvocation("register-in-nesu", String.valueOf(partnerId), "120448");
        command.executeCommand(commandInvocation, TEST_TERMINAL);
        ArgumentCaptor<RegisterShopDto> nesuClientRegisterCaptor = ArgumentCaptor.forClass(RegisterShopDto.class);
        ArgumentCaptor<ConfigureShopDto> nesuClientConfigureCaptor = ArgumentCaptor.forClass(ConfigureShopDto.class);
        verify(nesuClient, times(1)).registerShop(nesuClientRegisterCaptor.capture());
        verify(nesuClient, times(1)).configureShop(eq(partnerId), nesuClientConfigureCaptor.capture());

        assertEquals(
                RegisterShopDto.builder()
                        .id(partnerId)
                        .regionId(213)
                        .role(ShopRole.DROPSHIP_BY_SELLER)
                        .name("ООО Ромашишка 3")
                        .businessId(1003L)
                        .build(),
                nesuClientRegisterCaptor.getValue());

        assertEquals(
                ConfigureShopDto.builder()
                        .marketId(1004L)
                        .balanceClientId(111003L)
                        .balanceContractId(351874L)
                        .balancePersonId(44L)
                        .build(),
                nesuClientConfigureCaptor.getValue());
    }

    @DbUnitDataSet(before = "MarketIdCommandTest.before.csv")
    @DisplayName("Тест команды на добавление партнера в nesu для DBS (без заявки).")
    @Test
    void executeRegisterDbsPartnerInNesuWithoutRequest() {
        long partnerId = 103;
        CommandInvocation commandInvocation = commandInvocation("register-in-nesu", String.valueOf(partnerId));
        command.executeCommand(commandInvocation, TEST_TERMINAL);
        ArgumentCaptor<RegisterShopDto> nesuClientRegisterCaptor = ArgumentCaptor.forClass(RegisterShopDto.class);
        verify(nesuClient, times(1)).registerShop(nesuClientRegisterCaptor.capture());
        verifyNoMoreInteractions(nesuClient);

        assertEquals(
                RegisterShopDto.builder()
                        .id(partnerId)
                        .regionId(213)
                        .role(ShopRole.DROPSHIP_BY_SELLER)
                        .name("нет в жизни щастья")
                        .businessId(1003L)
                        .build(),
                nesuClientRegisterCaptor.getValue());

    }

    @DbUnitDataSet(before = "MarketIdCommandTest.before.csv")
    @DisplayName("Тест команды на добавление партнера в nesu для DBS (без marketId).")
    @Test
    void executeRegisterDbsPartnerInNesuWithoutMarketId() {
        long partnerId = 104;
        CommandInvocation commandInvocation = commandInvocation("register-in-nesu", String.valueOf(partnerId), "120449");
        try {
            command.executeCommand(commandInvocation, TEST_TERMINAL);
            fail("BadRequestException should be thrown");
        } catch(BadRequestException ex) {
            // as expected
        }

        ArgumentCaptor<RegisterShopDto> nesuClientRegisterCaptor = ArgumentCaptor.forClass(RegisterShopDto.class);
        verify(nesuClient, times(1)).registerShop(nesuClientRegisterCaptor.capture());
        verifyNoMoreInteractions(nesuClient);

        assertEquals(
                RegisterShopDto.builder()
                        .id(partnerId)
                        .regionId(213)
                        .role(ShopRole.DROPSHIP_BY_SELLER)
                        .name("ООО Ромашишка 3")
                        .businessId(1004L)
                        .build(),
                nesuClientRegisterCaptor.getValue());

    }

    @DbUnitDataSet(before = "MarketIdCommandTest.before.csv")
    @DisplayName("Тест команды на добавление marketId в nesu.")
    @Test
    void executeSetDBSMarketIdInNesuRequest() {
        long partnerId = 103;
        CommandInvocation commandInvocation = commandInvocation("set-nesu-market-id", String.valueOf(partnerId));
        command.executeCommand(commandInvocation, TEST_TERMINAL);
        ArgumentCaptor<ConfigureShopDto> nesuClientConfigureCaptor = ArgumentCaptor.forClass(ConfigureShopDto.class);
        verify(nesuClient).configureShop(eq(partnerId), nesuClientConfigureCaptor.capture());
        verifyNoMoreInteractions(nesuClient);

        assertEquals(
                ConfigureShopDto.builder()
                        .marketId(1004L)
                        .balanceClientId(111003L)
                        .balanceContractId(351874L)
                        .balancePersonId(44L)
                        .build(),
                nesuClientConfigureCaptor.getValue());
    }
}
