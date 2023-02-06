package ru.yandex.market.core.partner.contract;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.SignatoryDocType;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.balance.xmlrpc.model.ClientContractsStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;
import ru.yandex.market.core.balance.model.FullClientInfo;
import ru.yandex.market.core.supplier.model.PartnerExternalContract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PartnerContractServiceImplTest extends FunctionalTest {

    private static final long CLIENT_ID = 1L;
    private static final long PERSON_ID = 2L;
    private static final long SUBSIDIES_PERSON_ID = 3L;
    private static final long CONTRACT_ID = 4L;
    private static final long SUBSIDIES_CONTRACT_ID = 5L;

    private static final int ACTUAL_INCOME_CONTRACT_ID = 53;
    private static final int ACTUAL_OUTCOME_CONTRACT_ID = 54;
    private static final int UNKNOWN_CONTRACT_ID = 58;

    private static final boolean ACTIVE = true;
    private static final boolean INACTIVE = false;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private PartnerContractService partnerContractService;

    @Captor
    private ArgumentCaptor<PersonStructure> personStructureCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(balanceService.createClient(any(), anyLong(), anyLong())).thenReturn(CLIENT_ID);
        when(balanceService.createOrUpdatePerson(any(), anyLong())).thenReturn(PERSON_ID, SUBSIDIES_PERSON_ID);
        when(balanceService.createOffer(any(), anyLong())).thenReturn(CONTRACT_ID, SUBSIDIES_CONTRACT_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "shouldSuccessfullyCreateContractsForSupplierTest.before.csv",
            after = "shouldSuccessfullyCreateContractsForSupplierTest.after.csv")
    void shouldSuccessfullyCreateContractsForSupplier() {
        mockBalanceForContractCreation();
        partnerContractService.createContracts(1L, 1L, request(), false);
    }

    @Test
    @DbUnitDataSet(
            before = "shouldSuccessfullyCreateContractsForSupplierTest.before.csv",
            after = "shouldSuccessfullyCreateContractsForShopTest.after.csv")
    void shouldSuccessfullyCreateContractsForShop() {
        mockBalanceForContractCreation();
        partnerContractService.createContracts(1L, 1L, request(), false);
    }

    @Test
    @DbUnitDataSet(before = "shouldGetExternalContractIdsTest.before.csv")
    void shouldGetExternalContractIds() {
        //noinspection unchecked
        when(balanceService.getClientContracts(anyLong(), any(ContractType.class))).thenReturn(
                buildIncomeContracts(),
                buildOutcomeContracts());

        PartnerExternalContract contractIds = partnerContractService.getExternalContractIds(1L);
        assertThat(contractIds.getGeneralContractId()).isEqualTo("451545/18");
        assertThat(contractIds.getSubsidiesContractId()).isEqualTo("164516/18");
    }

    @Test
    void testNullSellerClientPersonId() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> partnerContractService.updateContracts(1L, 2L, request()));
    }

    @Test
    void testNullClientId() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    PrepayRequest prepayRequest = request();
                    prepayRequest.setPersonId(44L);
                    partnerContractService.updateContracts(1L, 2L, prepayRequest);
                });
    }

    @Test
    @DbUnitDataSet(before = "shouldUpdateExternalContractIdsTest.before.csv")
    void testUpdate() {
        PrepayRequest prepayRequest = request();
        prepayRequest.setSellerClientId(44L);
        prepayRequest.setPersonId(55L);
        partnerContractService.updateContracts(1L, 2L, prepayRequest);

        verify(balanceService).createClient(eq(new FullClientInfo(44L, null)), anyLong(), anyLong());
        verify(balanceService, times(2)).createOrUpdatePerson(personStructureCaptor.capture(), anyLong());

        List<PersonStructure> allPersonValues = personStructureCaptor.getAllValues();
        assertThat(allPersonValues.stream().map(PersonStructure::getPersonId))
                .containsExactlyInAnyOrder(55L, 111L);
    }

    @Test
    @DbUnitDataSet(before = "shouldUpdateExternalContractIdsTest.before.csv")
    void shouldSendAuthorityDocTypeToBalanceAtUpdate() {
        PrepayRequest prepayRequest = request();
        prepayRequest.setSellerClientId(44L);
        prepayRequest.setPersonId(55L);
        prepayRequest.setSignatoryDocType(SignatoryDocType.AOA_OR_ENTREPRENEUR);
        partnerContractService.updateContracts(1L, 2L, prepayRequest);

        verify(balanceService, times(2)).createOrUpdatePerson(personStructureCaptor.capture(), anyLong());
        Set<String> authorityDocTypes = personStructureCaptor.getAllValues()
                .stream()
                .map(PersonStructure::getAuthorityDocType)
                .collect(Collectors.toSet());

        assertThat(authorityDocTypes).containsExactlyInAnyOrder("Устав");
    }

    @Test
    @DbUnitDataSet(
            before = "testCreateForDropship.before.csv",
            after = "autoCreateForDropship.after.csv"
    )
    void testAutoCreateForDropship() {
        when(balanceService.getClientContracts(eq(1L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(4, true, 2)));
        when(balanceService.getClientContracts(eq(1L), eq(ContractType.SPENDABLE)))
                .thenReturn(List.of(buildContract(5, true, 3)));

        PrepayRequest prepayRequest = request();
        partnerContractService.createContracts(1, 1, prepayRequest, false);
        verify(balanceService, times(2)).createOffer(any(), anyLong());
        verify(balanceService, times(1)).getClientContracts(CLIENT_ID, ContractType.GENERAL);
        verify(balanceService, times(1)).getClientContracts(CLIENT_ID, ContractType.SPENDABLE);
    }

    /**
     * Проверяет создание договоров для партнеров Яндекса без плательщика и договора в балансе.
     */
    @Test
    @DbUnitDataSet(
            before = {"testCreateForDropship.before.csv", "testCreateYandexContracts.before.csv"},
            after = "testCreateYandexContracts.after.csv"
    )
    void testCreateYandexContracts() {
        PrepayRequest prepayRequest = request();
        partnerContractService.createContracts(1, 1, prepayRequest, false);

        verify(balanceService).createClient(eq(new FullClientInfo(0, null)), anyLong(), anyLong());
        verify(balanceService).getClientContracts(1L, ContractType.GENERAL);
        verify(balanceService).getClientContracts(1L, ContractType.SPENDABLE);
        verifyNoMoreInteractions(balanceService);
    }

    @DisplayName("Получение id договора из Баланса. Позитивный кейс")
    @Test
    @DbUnitDataSet(
            before = "SupplierContractServiceImplTest.testLoadFromBalance.before.csv",
            after = "SupplierContractServiceImplTest.testLoadFromBalance.after.csv"
    )
    void testLoadFromBalance() {
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(100, true, 10)));
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.SPENDABLE)))
                .thenReturn(List.of(buildContract(101, true, 11)));
        partnerContractService.loadPartnerContracts(1L);
    }

    @DisplayName("Получение id договора из Баланса. Позитивный кейс без субсидий")
    @Test
    @DbUnitDataSet(
            before = "SupplierContractServiceImplTest.testLoadFromBalance.before.csv",
            after = "SupplierContractServiceImplTest.testLoadFromBalance.after2.csv"
    )
    void testLoadFromBalance2() {
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(100, true, 10)));
        partnerContractService.loadPartnerContracts(1L);
    }

    @DisplayName("Получение id договора из Баланса. В Балансе ещё ничего нет")
    @Test
    @DbUnitDataSet(
            before = "SupplierContractServiceImplTest.testLoadFromBalance.before.csv",
            after = "SupplierContractServiceImplTest.testLoadFromBalance.after3.csv"
    )
    void testLoadFromBalanceNoContracts() {
        partnerContractService.loadPartnerContracts(1L);
    }

    @DisplayName("В Балансе отключили один договор")
    @Test
    @DbUnitDataSet(
            before = "SupplierContractServiceImplTest.testLoadFromBalanceDisabledContract.before.csv",
            after = "SupplierContractServiceImplTest.testLoadFromBalanceDisabledContract.after.csv"
    )
    void testLoadFromBalanceDisabledContract() {
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(100, true, 10)));
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.SPENDABLE)))
                .thenReturn(List.of(buildContract(101, false, 11)));
        partnerContractService.loadPartnerContracts(1L);
    }

    @DisplayName("В Балансе отключили оба договора")
    @Test
    @DbUnitDataSet(
            before = "SupplierContractServiceImplTest.testLoadFromBalanceDisabledContract.before.csv",
            after = "SupplierContractServiceImplTest.testLoadFromBalanceDisabledAllContracts.after.csv"
    )
    void testLoadFromBalanceAllDisabledContracts() {
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(100, false, 10)));
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.SPENDABLE)))
                .thenReturn(List.of(buildContract(101, false, 11)));
        partnerContractService.loadPartnerContracts(1L);
    }

    @DisplayName("В Балансе удалили один договор")
    @Test
    @DbUnitDataSet(
            before = "SupplierContractServiceImplTest.testLoadFromBalanceDisabledContract.before.csv",
            after = "SupplierContractServiceImplTest.testLoadFromBalanceDisabledContract.afterDeleted.csv"
    )
    void testLoadFromBalanceDeletedContract() {
        when(balanceService.getClientContracts(eq(10000L), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract(100, true, 10)));
        partnerContractService.loadPartnerContracts(1L);
    }

    private static PrepayRequest request() {
        return new PrepayRequest(1, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED, 1);
    }

    private static List<ClientContractInfo> buildOutcomeContracts() {
        return Arrays.asList(
                buildContract(ACTUAL_OUTCOME_CONTRACT_ID, ACTIVE, 45),
                buildContract(UNKNOWN_CONTRACT_ID, ACTIVE, 45),
                buildContract(UNKNOWN_CONTRACT_ID, INACTIVE, 45)
        );
    }

    private static List<ClientContractInfo> buildIncomeContracts() {
        return Arrays.asList(
                buildContract(ACTUAL_INCOME_CONTRACT_ID, ACTIVE, 45),
                buildContract(UNKNOWN_CONTRACT_ID, ACTIVE, 45),
                buildContract(UNKNOWN_CONTRACT_ID, INACTIVE, 45)
        );
    }

    private static ClientContractInfo buildContract(int id, boolean isActive, int personId) {
        ClientContractsStructure contract = new ClientContractsStructure();
        contract.setId(id);
        contract.setExternalId("1/1");
        contract.setIsActive(BooleanUtils.toInteger(isActive));
        contract.setCurrency("RUR");
        contract.setDt(DateUtil.asDate(LocalDate.of(2018, Month.JANUARY, 1)));
        contract.setPersonId(personId);
        return ClientContractInfo.fromBalanceStructure(contract);
    }

    private void mockBalanceForContractCreation() {
        when(balanceService.getClientContracts(eq(CLIENT_ID), eq(ContractType.GENERAL)))
                .thenReturn(List.of(buildContract((int) CONTRACT_ID, true, (int) PERSON_ID)));
        when(balanceService.getClientContracts(eq(CLIENT_ID), eq(ContractType.SPENDABLE)))
                .thenReturn(List.of(buildContract((int) SUBSIDIES_CONTRACT_ID, true, (int) SUBSIDIES_PERSON_ID)));
    }
}
