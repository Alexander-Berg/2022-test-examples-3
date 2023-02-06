package ru.yandex.market.partner;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.balance.BalanceConstants;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.ExternalBalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.FullClientInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static ru.yandex.market.common.balance.BalanceConstants.SUBSIDIES_BALANCE_SERVICE;

/**
 * Тесты для {@link FixAllPartnersCommand}
 */
class FixAllPartnersCommandTest extends FunctionalTest {

    @Autowired
    private FixAllPartnersCommand fixAllPartnersCommand;

    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;

    @Autowired
    @Qualifier("patientBalanceService")
    private ExternalBalanceService patientBalanceService;

    @Autowired
    private AgencyService agencyService;

    private Terminal terminal = Mockito.mock(Terminal.class);

    @BeforeEach
    void setUp() {
        Mockito.reset(terminal);
        Mockito.when(terminal.getWriter()).thenReturn(new PrintWriter(ByteStreams.nullOutputStream()));
    }

    @Test
    @DbUnitDataSet(
            before = "FixAllPartnersCommandTest.noClientInBalance.before.csv",
            after = "FixAllPartnersCommandTest.noClientInBalance.after.csv"
    )
    void testNoClientInBalance() {
        Mockito.when(
                balanceService.createClient(eq(FullClientInfo.createDefault("supplier1", 0)), anyLong(), anyLong()))
                .thenReturn(101L);
        Mockito.when(balanceService.getClient(1L)).thenReturn(new ClientInfo(1L, ClientType.OOO, false, 0));

        CommandInvocation ci = new CommandInvocation("fix-all-partners", new String[]{"100"}, Collections.emptyMap());
        fixAllPartnersCommand.executeCommand(ci, terminal);

        Mockito.verify(balanceService)
                .createClient(eq(FullClientInfo.createDefault("supplier1", 0)), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "FixAllPartnersCommandTest.severalPartnerTypes.before.csv",
            after = "FixAllPartnersCommandTest.severalPartnerTypes.after.csv"
    )
    void testFixAll() {
        Mockito.when(
                balanceService.createClient(eq(FullClientInfo.createDefault("supplier2", 0)), anyLong(), anyLong()))
                .thenReturn(2003L);
        Mockito.when(balanceService.getClient(2003L)).thenReturn(new ClientInfo(2003L, ClientType.OOO, false, 0));
        Mockito.when(balanceService.getClient(23L)).thenReturn(new ClientInfo(23L, ClientType.OOO, false, 0));

        CommandInvocation ci = new CommandInvocation("fix-all-partners", new String[0], Collections.emptyMap());
        fixAllPartnersCommand.executeCommand(ci, terminal);

        Mockito.verify(balanceService)
                .createClient(eq(FullClientInfo.createDefault("supplier2", 0)), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "FixAllPartnersCommandTest.noClientInBalance.before.csv",
            after = "FixAllPartnersCommandTest.noClientInBalance.before.csv"
    )
    void testExceptionRollback() {
        Mockito.when(
                balanceService.createClient(eq(FullClientInfo.createDefault("supplier1", 0)), anyLong(), anyLong()))
                .thenReturn(101L);
        Mockito.when(balanceService.getClient(1L)).thenReturn(new ClientInfo(1L, ClientType.OOO, false, 0));

        Mockito.doThrow(new IllegalStateException()).when(patientBalanceService)
                .linkUid(anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "FixAllPartnersCommandTest.severalPartnerTypes.before.csv",
            after = "FixAllPartnersCommandTest.severalPartnerTypes.after.csv"
    )
    void testSeveralPartnerTypesNoClientInBalance() {
        Mockito.when(
                balanceService.createClient(eq(FullClientInfo.createDefault("supplier2", 0)), anyLong(), anyLong()))
                .thenReturn(2003L);
        Mockito.when(balanceService.getClient(2003L)).thenReturn(new ClientInfo(2003L, ClientType.OOO, false, 0));
        Mockito.when(balanceService.getClient(23L)).thenReturn(new ClientInfo(23L, ClientType.OOO, false, 0));

        fixAllPartnersCommand.fixPartners(Collections.singleton(203L), terminal);

        Mockito.verify(balanceService)
                .createClient(eq(FullClientInfo.createDefault("supplier2", 0)), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "FixAllPartnersCommandTest.noSuperAdmin.before.csv",
            after = "FixAllPartnersCommandTest.noSuperAdmin.after.csv"
    )
    void testNoSuperAdmin() {
        Mockito.when(balanceService.createClient(eq(FullClientInfo.createDefault("shop4", 0)), anyLong(), anyLong()))
                .thenReturn(401L);
        Mockito.when(balanceService.getClient(401L)).thenReturn(new ClientInfo(401L, ClientType.OOO, false, 0));

        fixAllPartnersCommand.fixPartners(Collections.singleton(400L), terminal);

        Mockito.verify(balanceService).createClient(eq(FullClientInfo.createDefault("shop4", 0)), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "FixAllPartnersCommandTest.createContracts.before.csv",
            after = "FixAllPartnersCommandTest.createContracts.after.csv"
    )
    void testCreateContracts() {
        Mockito.when(
                balanceService.createClient(eq(FullClientInfo.createDefault("supplier5", 0)), anyLong(), anyLong()))
                .thenReturn(501L);
        Mockito.when(patientBalanceService.createClient(any(), anyLong(), anyLong())).thenReturn(505L);
        Mockito.doReturn(5L).when(patientBalanceService).createOffer(Mockito.argThat(
                offer -> offer.get("services").equals(
                        ImmutableList.of(
                                BalanceConstants.DELIVERY_BALANCE_SERVICE,
                                BalanceConstants.COMMISSION_BALANCE_SERVICE
                        )
                )),
                anyLong()
        );
        Mockito.doReturn(6L).when(patientBalanceService).createOffer(Mockito.argThat(
                offer -> offer.get("services").equals(Collections.singletonList(SUBSIDIES_BALANCE_SERVICE))),
                anyLong()
        );
        mockGetClientContracts(5L, 6L);

        fixAllPartnersCommand.fixPartners(Collections.singleton(500L), terminal);

        Mockito.verify(balanceService)
                .createClient(eq(FullClientInfo.createDefault("supplier5", 0)), anyLong(), anyLong());
    }

    @Disabled("Так как внутри balanceService создается агентство в нашей бд, если удастся размокать balanceService, "
            + "то можно вернуть тест")
    @Test
    @DbUnitDataSet(
            before = "FixAllPartnersCommandTest.agencyWithOkClients.before.csv",
            after = "FixAllPartnersCommandTest.agencyWithOkClients.after.csv"
    )
    void testAgencyWithOkClients() {
        FullClientInfo agencyClientInfo = FullClientInfo.createDefault("agency", 0);
        agencyClientInfo.setAgency(true);
        Mockito.when(balanceService.createClient(eq(agencyClientInfo), anyLong(), anyLong())).thenReturn(767L);
        Mockito.when(balanceService.getClient(767L)).thenReturn(new ClientInfo(767, ClientType.OOO, true, 767));
        Mockito.when(balanceService.getClient(6)).thenReturn(new ClientInfo(6, ClientType.ZAO));
        Mockito.when(balanceService.getClient(600)).thenReturn(new ClientInfo(600, ClientType.OOO));
        Mockito.when(balanceService.getClient(700)).thenReturn(new ClientInfo(700, ClientType.OOO));

        fixAllPartnersCommand.fixAgencies(Collections.singleton(agencyService.getAgency(676)), terminal);

        Mockito.verify(balanceService).createClient(eq(agencyClientInfo), anyLong(), anyLong());
        Mockito.verify(patientBalanceService).linkUid(eq(676000L), eq(767L), anyLong(), anyLong());
    }

    @Disabled("Так как внутри balanceService создается агентство в нашей бд, если удастся размокать balanceService, "
            + "то можно вернуть тест")
    @Test
    @DbUnitDataSet(
            before = "FixAllPartnersCommandTest.agencyWithBrokenClients.before.csv",
            after = "FixAllPartnersCommandTest.agencyWithBrokenClients.after.csv"
    )
    void testAgencyWithBrokenClients() {
        FullClientInfo agencyClientInfo = FullClientInfo.createDefault("agency2", 0);
        agencyClientInfo.setAgency(true);
        Mockito.when(balanceService.createClient(eq(agencyClientInfo), anyLong(), anyLong())).thenReturn(989L);
        Mockito.when(balanceService.getClient(989L)).thenReturn(new ClientInfo(989, ClientType.OOO, true, 989));
        Mockito.when(balanceService.getClient(900)).thenReturn(new ClientInfo(900, ClientType.OOO));
        Mockito.when(
                balanceService.createClient(eq(FullClientInfo.createDefault("supplier8", 0)), anyLong(), anyLong()))
                .thenReturn(888L);
        Mockito.when(balanceService.getClient(888L)).thenReturn(new ClientInfo(888, ClientType.OOO, false, 0));

        Mockito.when(patientBalanceService.createClient(any(), anyLong(), anyLong())).thenReturn(808L);
        Mockito.doReturn(8L).when(patientBalanceService).createOffer(Mockito.argThat(
                offer -> offer.get("services").equals(
                        ImmutableList.of(
                                BalanceConstants.DELIVERY_BALANCE_SERVICE,
                                BalanceConstants.COMMISSION_BALANCE_SERVICE
                        )
                )),
                anyLong()
        );
        Mockito.doReturn(9L).when(patientBalanceService).createOffer(Mockito.argThat(
                offer -> offer.get("services").equals(Collections.singletonList(SUBSIDIES_BALANCE_SERVICE))),
                anyLong()
        );
        mockGetClientContracts(8L, 9L);

        fixAllPartnersCommand.fixAgencies(Collections.singleton(agencyService.getAgency(898)), terminal);

        Mockito.verify(balanceService).createClient(eq(agencyClientInfo), anyLong(), anyLong());
        Mockito.verify(patientBalanceService).linkUid(eq(898000L), eq(989L), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(before = "FixAllPartnersCommandTest.testNoLinkedUidInBalance.before.csv")
    void testNoLinkedUidInBalance() {
        Mockito.when(balanceService.getClient(eq(100L))).thenReturn(new ClientInfo(100, ClientType.OOO));
        fixAllPartnersCommand.fixPartners(Collections.singleton(100L), terminal);
    }

    @Test
    @DbUnitDataSet(before = "FixAllPartnersCommandTest.testNoLinkedUidInBalance.before.csv")
    void testWrongLinkedUidInBalance() {
        Mockito.when(balanceService.getClient(eq(100L))).thenReturn(new ClientInfo(100, ClientType.OOO));
        fixAllPartnersCommand.fixPartners(Collections.singleton(100L), terminal);
    }

    private void mockGetClientContracts(Long contractId, Long subsidiesContractId) {
        if (contractId != null) {
            Mockito.when(patientBalanceService.getClientContracts(anyLong(), eq(ContractType.GENERAL)))
                    .thenAnswer(invocation -> List.of(new ClientContractInfo.ClientContractInfoBuilder()
                            .withId(contractId)
                            .isActive(true)
                            .build()));
        }
        if (subsidiesContractId != null) {
            Mockito.when(patientBalanceService.getClientContracts(anyLong(), eq(ContractType.SPENDABLE)))
                    .thenAnswer(invocation -> List.of(new ClientContractInfo.ClientContractInfoBuilder()
                            .withId(subsidiesContractId)
                            .isActive(true)
                            .build()));
        }
    }

    @Test
    @DbUnitDataSet(before = "FixPartnerDSBSCommandTest.before.csv")
    void testFixDsbs() {
        Mockito.when(balanceService.getClient(203L)).thenReturn(new ClientInfo(203L, ClientType.OOO, false, 0));
        CommandInvocation ci = new CommandInvocation("fix-all-partners", new String[]{"203"}, Collections.emptyMap());
        fixAllPartnersCommand.executeCommand(ci, terminal);

        InOrder inOrder = Mockito.inOrder(patientBalanceService);
        // затем плательщика для одного договора
        inOrder.verify(patientBalanceService, times(1)).createOrUpdatePerson(any(), anyLong());
        // затем контракт
        inOrder.verify(patientBalanceService, times(1)).createOffer(any(), anyLong());
    }

    @Test
    @DbUnitDataSet(before = "FixClientSinceCommandTest.before.csv")
    void testPartnersSince() {
        Mockito.when(balanceService.getClient(204L)).thenReturn(new ClientInfo(204L, ClientType.OOO, false, 0));
        CommandInvocation ci = new CommandInvocation("fix-all-partners", new String[]{"since", "203"},
                Collections.emptyMap());
        fixAllPartnersCommand.executeCommand(ci, terminal);
        Mockito.verify(patientBalanceService, times(0)).linkUid(eq(20003L), eq(203L), anyLong(), anyLong());
    }
}
