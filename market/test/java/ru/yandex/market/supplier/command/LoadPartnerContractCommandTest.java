package ru.yandex.market.supplier.command;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.grpc.stub.StreamObserver;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.balance.xmlrpc.model.ClientContractsStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.nesu.client.NesuClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link LoadPartnerContractCommand}.
 *
 * @author Vadim Lyalin
 */
@ExtendWith(MockitoExtension.class)
public class LoadPartnerContractCommandTest extends FunctionalTest {
    @Autowired
    private LoadPartnerContractCommand loadPartnerContractCommand;
    @Autowired
    private NesuClient nesuClient;
    @Mock
    private Terminal terminal;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    /**
     * Проверяет обновление договоров из Баланса и отправку в nesu.
     */
    @Test
    @DbUnitDataSet(before = "LoadPartnerContractCommand.before.csv",
            after = "LoadPartnerContractCommand.after.csv")
    void testLoadContracts() {
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(100, true, 10)));
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.SPENDABLE)))
                .thenReturn(List.of(buildContract(101, true, 11)));
        when(lmsClient.getPartner(eq(101L))).thenReturn(Optional.of(PartnerResponse.newBuilder().build()));
        mockMarketId();

        loadPartnerContractCommand.executeCommand(
                new CommandInvocation("load-partner-contract", new String[]{"1"}, Collections.emptyMap()), terminal);

        verify(nesuClient).configureShop(eq(1L), any());
        verifyZeroInteractions(nesuClient);
    }

    /**
     * Проверяет обновление договоров из Баланса с изменением seller-client-id и отправку в nesu.
     */
    @Test
    @DbUnitDataSet(before = "LoadPartnerContractCommand.before.csv",
            after = "LoadPartnerContractCommand.changeSellerClient.after.csv")
    void testChangeClientAndLoadContracts() {
        when(balanceService.getClientContracts(eq(999L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(100, true, 10)));
        when(balanceService.getClientContracts(eq(999L), eq(ContractType.SPENDABLE)))
                .thenReturn(List.of(buildContract(101, true, 11)));
        when(lmsClient.getPartner(eq(101L))).thenReturn(Optional.of(PartnerResponse.newBuilder().build()));
        mockMarketId();

        loadPartnerContractCommand.executeCommand(
                new CommandInvocation("load-partner-contract", new String[]{"1"}, Map.of("seller-client-id", "999")),
                terminal);

        verify(nesuClient).configureShop(eq(1L), any());
        verifyZeroInteractions(nesuClient);
    }

    /**
     * Проверяет обновление договоров из Баланса и отправку в nesu для доставочного партнера.
     */
    @Test
    @DbUnitDataSet(before = "LoadPartnerContractCommand.delivery.before.csv",
            after = "LoadPartnerContractCommand.delivery.after.csv")
    void testLoadDeliveryContracts() {
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(100, true, 22)));
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(20).build());
            marketAccountStreamObserver.onCompleted();
            return 20;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(20).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());

        loadPartnerContractCommand.executeCommand(
                new CommandInvocation("load-partner-contract", new String[]{"1"}, Collections.emptyMap()),
                terminal);

        verify(balanceService).getClientContracts(eq(10000L), eq(ContractType.GENERAL));
        verify(nesuClient).registerShop(any());
        verifyZeroInteractions(nesuClient, balanceService);
    }

    private void mockMarketId() {
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            var marketAccount = MarketAccount.newBuilder().setMarketId(20).build();
            var response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();
            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(), any());
    }

    private ClientContractInfo buildContract(int id, boolean isActive, int personId) {
        ClientContractsStructure contract = new ClientContractsStructure();
        contract.setId(id);
        contract.setExternalId("1/1");
        contract.setIsActive(BooleanUtils.toInteger(isActive));
        contract.setCurrency("RUR");
        contract.setDt(DateUtil.asDate(LocalDate.of(2018, Month.JANUARY, 1)));
        contract.setPersonId(personId);
        return ClientContractInfo.fromBalanceStructure(contract);
    }
}
