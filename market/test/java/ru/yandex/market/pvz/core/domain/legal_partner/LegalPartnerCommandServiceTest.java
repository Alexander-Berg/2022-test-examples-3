package ru.yandex.market.pvz.core.domain.legal_partner;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.model.approve.ApproveStatus;
import ru.yandex.market.pvz.client.model.partner.CommissionerDocument;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.db.jpa.BaseJpaEntity;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.client.model.partner.TaxationSystem.SIMPLIFIED_INCOME_MINUS_EXPENSE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.DEFAULT_DATASOURCE_ID;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AccountantTestParams.DEFAULT_ACCOUNTANT_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AccountantTestParams.DEFAULT_ACCOUNTANT_FULL_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AccountantTestParams.DEFAULT_ACCOUNTANT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_BUILDING;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_CITY;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_HOUSE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_HOUSING;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_OFFICE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_STREET;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_ZIPCODE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_BANK_CITY;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_BANK_KPP;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_BANK_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_CHECKING_ACCOUNT_NUMBER;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_RCBIC;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.CommissionerTestParams.DEFAULT_COMMISSIONER_DOCUMENT;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.CommissionerTestParams.DEFAULT_COMMISSIONER_FULL_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.CommissionerTestParams.DEFAULT_COMMISSIONER_POSITION;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.DEFAULT_TAXATION_SYSTEM;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.DelegateTestParams.DEFAULT_DELEGATE_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.DelegateTestParams.DEFAULT_DELEGATE_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.DelegateTestParams.DEFAULT_DELEGATE_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_ISSUE_DATE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_KPP;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_LEGAL_FORM;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_LEGAL_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_OGRN;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_ORGANIZATION_NAME;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerCommandServiceTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPreLegalPartnerFactory preLegalPartnerFactory;

    private final LegalPartnerQueryService legalPartnerQueryService;
    private final PreLegalPartnerQueryService preLegalPartnerQueryService;
    private final LegalPartnerCommandService legalPartnerCommandService;
    private final LegalPartnerRepository legalPartnerRepository;
    private final TestLegalPartnerTerminationFactory terminationFactory;

    @Test
    void createLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .approvePreLegalPartner(false)
                        .build()
        );

        LegalPartnerParams created = legalPartnerQueryService.get(legalPartner.getId());

        assertOrganization(created.getOrganization());
        assertDelegate(created.getDelegate());
        assertAccountant(created.getAccountant());
        assertAddress(created.getBusinessAddress());
        assertAddress(created.getPostAddress());
        assertThat(created.getTaxation()).isEqualTo(DEFAULT_TAXATION_SYSTEM);
        assertCommissioner(created.getCommissioner());
        assertBank(created.getBank());
        assertThat(created.getApproveStatus()).isEqualTo(ApproveStatus.CHECKING);
        assertThat(created.getAgreementNo()).isNull();
        assertThat(created.getAgreementDate()).isNull();
        assertThat(created.getPartnerId()).isNotNull();
        assertThat(created.getPartnerId()).isGreaterThan(0);
        assertThat(created.getBalanceClientId()).isEqualTo(legalPartner.getBalanceClientId());
        assertThat(created.getMarketShopId()).isEqualTo(DEFAULT_DATASOURCE_ID);
    }

    private void assertOrganization(LegalPartnerParams.OrganizationParams actual) {
        assertThat(actual.getName()).isEqualTo(DEFAULT_ORGANIZATION_NAME);
        assertThat(actual.getLegalForm()).isEqualTo(DEFAULT_LEGAL_FORM);
        assertThat(actual.getLegalType()).isEqualTo(DEFAULT_LEGAL_TYPE);
        assertThat(actual.getKpp()).isEqualTo(DEFAULT_KPP);
        assertThat(actual.getOgrn()).isEqualTo(DEFAULT_OGRN);
        assertThat(actual.getIssueDate()).isEqualTo(DEFAULT_ISSUE_DATE);
    }

    private void assertDelegate(LegalPartnerParams.DelegateParams actual) {
        assertPersonName(actual.getDelegateName());
        assertThat(actual.getDelegateEmail()).isEqualTo(DEFAULT_DELEGATE_EMAIL);
        assertThat(actual.getDelegatePhone()).isEqualTo(DEFAULT_DELEGATE_PHONE);
    }

    private void assertAccountant(LegalPartnerParams.AccountantParams actual) {
        assertPersonName(actual.getAccountantName());
        assertThat(actual.getAccountantEmail()).isEqualTo(DEFAULT_ACCOUNTANT_EMAIL);
        assertThat(actual.getAccountantPhone()).isEqualTo(DEFAULT_ACCOUNTANT_PHONE);
    }

    private void assertCommissioner(LegalPartnerParams.CommissionerParams actual) {
        assertThat(actual.getCommissionerPosition()).isEqualTo(DEFAULT_COMMISSIONER_POSITION);
        assertPersonName(actual.getCommissionerName());
        assertThat(actual.getCommissionerDocument()).isEqualTo(DEFAULT_COMMISSIONER_DOCUMENT);
    }

    private void assertBank(LegalPartnerParams.BankParams actual) {
        assertThat(actual.getBankKpp()).isEqualTo(DEFAULT_BANK_KPP);
        assertThat(actual.getRcbic()).isEqualTo(DEFAULT_RCBIC);
        assertThat(actual.getCheckingAccountNumber()).isEqualTo(DEFAULT_CHECKING_ACCOUNT_NUMBER);
        assertThat(actual.getCorrespondentAccountNumber()).isEqualTo(DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER);
        assertThat(actual.getBank()).isEqualTo(DEFAULT_BANK_NAME);
        assertThat(actual.getBankCity()).isEqualTo(DEFAULT_BANK_CITY);
    }

    private void assertPersonName(LegalPartnerParams.PersonNameParams actual) {
        assertThat(actual.getFirstName()).isEqualTo(DEFAULT_DELEGATE_NAME.getFirstName());
        assertThat(actual.getLastName()).isEqualTo(DEFAULT_DELEGATE_NAME.getLastName());
        assertThat(actual.getPatronymic()).isEqualTo(DEFAULT_DELEGATE_NAME.getPatronymic());
    }

    private void assertAddress(LegalPartnerParams.AddressParams actual) {
        assertThat(actual.getCity()).isEqualTo(DEFAULT_CITY);
        assertThat(actual.getStreet()).isEqualTo(DEFAULT_STREET);
        assertThat(actual.getHouse()).isEqualTo(DEFAULT_HOUSE);
        assertThat(actual.getHousing()).isEqualTo(DEFAULT_HOUSING);
        assertThat(actual.getBuilding()).isEqualTo(DEFAULT_BUILDING);
        assertThat(actual.getOffice()).isEqualTo(DEFAULT_OFFICE);
        assertThat(actual.getZipcode()).isEqualTo(DEFAULT_ZIPCODE);
    }

    @Test
    void sendToCheckTest() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        partner = legalPartnerFactory.sendToCheck(partner.getId());
        assertThat(partner.getApproveStatus()).isEqualTo(ApproveStatus.CHECKING);
    }

    @Test
    void approveTest() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();

        legalPartnerFactory.sendToCheck(partner.getId());

        String agreementNo = "228-1994";
        LocalDate agreementDate = LocalDate.of(2020, 9, 26);
        legalPartnerFactory.approve(partner.getId(), agreementNo, agreementDate);

        LegalPartnerParams approved = legalPartnerQueryService.get(partner.getId());
        assertThat(approved.getAgreementNo()).isEqualTo(agreementNo);
        assertThat(approved.getAgreementDate()).isEqualTo(agreementDate);
        assertThat(approved.getApproveStatus()).isEqualTo(ApproveStatus.APPROVED);
        assertThat(approved.getVirtualAccountNumber()).isBlank();
    }

    @Test
    void unableToApproveTest() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .approvePreLegalPartner(false)
                        .build()
        );
        legalPartnerFactory.reject(partner.getId(), "Не подходит по идеологическим причинам");

        String agreementNo = "228-1994";
        LocalDate agreementDate = LocalDate.of(2020, 9, 26);
        legalPartnerFactory.approve(partner.getId(), agreementNo, agreementDate);

        LegalPartnerParams approved = legalPartnerQueryService.get(partner.getId());
        assertThat(approved.getAgreementNo()).isNull();
        assertThat(approved.getAgreementDate()).isNull();
        assertThat(approved.getApproveStatus()).isEqualTo(ApproveStatus.REJECTED);
    }

    @Test
    void rejectTest() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();

        legalPartnerFactory.sendToCheck(partner.getId());

        String reason = "Неверный формат ИНН";
        legalPartnerFactory.reject(partner.getId(), reason);

        LegalPartnerParams rejected = legalPartnerQueryService.get(partner.getId());
        assertThat(rejected.getAgreementNo()).isNull();
        assertThat(rejected.getAgreementDate()).isNull();
        assertThat(rejected.getApproveStatus()).isEqualTo(ApproveStatus.REJECTED);
    }

    @Test
    void unableToRejectTest() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .approvePreLegalPartner(false)
                        .build()
        );
        legalPartnerFactory.approve(partner.getId(), "123", LocalDate.of(2022, 2, 24));

        String reason = "Неверный формат ИНН";
        legalPartnerFactory.reject(partner.getId(), reason);

        LegalPartnerParams rejected = legalPartnerQueryService.get(partner.getId());
        assertThat(rejected.getAgreementNo()).isEqualTo("123");
        assertThat(rejected.getAgreementDate()).isEqualTo(LocalDate.of(2022, 2, 24));
        assertThat(rejected.getApproveStatus()).isEqualTo(ApproveStatus.APPROVED);
    }

    @Test
    void updateLegalPartner() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .approvePreLegalPartner(false)
                        .build()
        );
        legalPartnerFactory.reject(partner.getId(), "Не выгодный");

        TestLegalPartnerFactory.LegalPartnerTestParams updateParams = TestLegalPartnerFactory.LegalPartnerTestParams
                .builder()
                .organization(TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.builder()
                        .name("Тестовая организация")
                        .build())
                .delegate(TestLegalPartnerFactory.LegalPartnerTestParams.DelegateTestParams.builder()
                        .delegateName(TestLegalPartnerFactory.LegalPartnerTestParams.PersonNameTestParams.builder()
                                .build())
                        .delegateFullName("Александров Александр Иванович")
                        .delegateEmail("test@yandex.ru")
                        .build())
                .accountant(TestLegalPartnerFactory.LegalPartnerTestParams.AccountantTestParams.builder()
                        .accountantName(TestLegalPartnerFactory.LegalPartnerTestParams.PersonNameTestParams.builder()
                                .build())
                        .accountantPhone("7(495)747-31-37")
                        .accountantFullName("Петров Петр Васильевич")
                        .build())
                .businessAddress(TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.builder()
                        .city("Санкт-Петербург")
                        .build())
                .postAddress(TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.builder()
                        .zipcode("987654")
                        .build())
                .taxation(SIMPLIFIED_INCOME_MINUS_EXPENSE)
                .commissioner(TestLegalPartnerFactory.LegalPartnerTestParams.CommissionerTestParams.builder()
                        .commissionerPosition("Главарь")
                        .commissionerName(TestLegalPartnerFactory.LegalPartnerTestParams.PersonNameTestParams.builder()
                                .build())
                        .commissionerFullName("Сидоров Посейдон Евгеньевич")
                        .commissionerDocument(CommissionerDocument.POWER_OF_ATTORNEY)
                        .build())
                .bank(TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.builder()
                        .bank("Тинькофф Банк")
                        .build())
                .build();

        legalPartnerFactory.updateLegalPartner(partner.getId(), updateParams);

        LegalPartnerParams updated = legalPartnerQueryService.get(partner.getId());

        assertThat(updated.getOrganization().getName()).isEqualTo("Тестовая организация");
        assertThat(updated.getDelegate().getDelegateFio()).isEqualTo("Александров Александр Иванович");
        assertThat(updated.getDelegate().getDelegateEmail()).isEqualTo("test@yandex.ru");
        assertThat(updated.getAccountant().getAccountantFio()).isEqualTo("Петров Петр Васильевич");
        assertThat(updated.getAccountant().getAccountantPhone()).isEqualTo("7(495)747-31-37");
        assertThat(updated.getBusinessAddress().getCity()).isEqualTo("Санкт-Петербург");
        assertThat(updated.getPostAddress().getZipcode()).isEqualTo("987654");
        assertThat(updated.getTaxation()).isEqualTo(SIMPLIFIED_INCOME_MINUS_EXPENSE);
        assertThat(updated.getCommissioner().getCommissionerPosition()).isEqualTo("Главарь");
        assertThat(updated.getCommissioner().getCommissionerFio()).isEqualTo("Сидоров Посейдон Евгеньевич");
        assertThat(updated.getCommissioner().getCommissionerDocument())
                .isEqualTo(CommissionerDocument.POWER_OF_ATTORNEY);
        assertThat(updated.getBank().getBank()).isEqualTo("Тинькофф Банк");

        assertThat(updated.getPartnerId()).isEqualTo(partner.getPartnerId());
        assertThat(updated.getOrganization().getOgrn()).isEqualTo(DEFAULT_OGRN);
        assertThat(updated.getDelegate().getDelegatePhone()).isEqualTo(DEFAULT_DELEGATE_PHONE);
        assertThat(updated.getAccountant().getAccountantEmail()).isEqualTo(DEFAULT_ACCOUNTANT_EMAIL);
        assertThat(updated.getBusinessAddress().getOffice()).isEqualTo(DEFAULT_OFFICE);
        assertThat(updated.getPostAddress().getStreet()).isEqualTo(DEFAULT_STREET);
        assertThat(updated.getBank().getCheckingAccountNumber()).isEqualTo(DEFAULT_CHECKING_ACCOUNT_NUMBER);
    }

    @Test
    void updateInsensitiveLegalPartner() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();

        TestLegalPartnerFactory.LegalPartnerTestParams updateParams = TestLegalPartnerFactory.LegalPartnerTestParams
                .builder()
                .bank(TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.builder()
                        .bank("Тинькофф Банк")
                        .build())
                .businessAddress(TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.builder()
                        .city("Санкт-Петербург")
                        .build())
                .postAddress(TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.builder()
                        .zipcode("987654")
                        .build())
                .taxation(SIMPLIFIED_INCOME_MINUS_EXPENSE)
                .build();

        legalPartnerFactory.updateInsensitiveLegalPartner(partner.getPartnerId(), updateParams);

        LegalPartnerParams updated = legalPartnerQueryService.get(partner.getId());

        assertThat(updated.getBusinessAddress().getCity()).isEqualTo("Санкт-Петербург");
        assertThat(updated.getPostAddress().getZipcode()).isEqualTo("987654");
        assertThat(updated.getTaxation()).isEqualTo(SIMPLIFIED_INCOME_MINUS_EXPENSE);
        assertThat(updated.getBank().getBank()).isEqualTo("Тинькофф Банк");
    }

    @Test
    void unableToUpdateApprovedPartner() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        var approvedPartner = legalPartnerFactory.forceApprove(partner.getId(), LocalDate.now());

        var updateParams = TestLegalPartnerFactory.LegalPartnerTestParams
                .builder()
                .taxation(SIMPLIFIED_INCOME_MINUS_EXPENSE)
                .build();

        assertThatThrownBy(() -> legalPartnerFactory.updateLegalPartner(approvedPartner.getId(), updateParams))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void unableToUpdateCheckingPartner() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        LegalPartner checkingPartner = legalPartnerFactory.sendToCheck(partner.getId());

        var updateParams = TestLegalPartnerFactory.LegalPartnerTestParams
                .builder()
                .taxation(SIMPLIFIED_INCOME_MINUS_EXPENSE)
                .build();

        assertThatThrownBy(() -> legalPartnerFactory.updateLegalPartner(checkingPartner.getId(), updateParams))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void updateRejectedPartner() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        partner = legalPartnerFactory.forceReject(partner.getId(), "Все ерунда, давай по новой");

        TestLegalPartnerFactory.LegalPartnerTestParams updateParams = TestLegalPartnerFactory.LegalPartnerTestParams
                .builder()
                .taxation(SIMPLIFIED_INCOME_MINUS_EXPENSE)
                .build();

        legalPartnerFactory.updateLegalPartner(partner.getId(), updateParams);
        LegalPartnerParams updated = legalPartnerQueryService.get(partner.getId());

        assertThat(updated.getTaxation()).isEqualTo(SIMPLIFIED_INCOME_MINUS_EXPENSE);
    }

    @Test
    void getByExternalId() {
        LegalPartner created = legalPartnerFactory.createLegalPartner();

        LegalPartnerParams found = legalPartnerQueryService.getByPartnerId(created.getPartnerId());
        assertThat(found).isNotNull();
    }

    @Test
    void testPopulateAgreementData() {
        String agreementNo = "123567890";
        String virtualAccountNumber = "VIRTUAL_ACCOUNT_NUMBER";

        LegalPartner created = legalPartnerFactory.createLegalPartner();
        LegalPartner updated = legalPartnerCommandService
                .populateAgreementData(created.getId(), agreementNo, virtualAccountNumber, null);

        assertThat(updated.getAgreementNo()).isEqualTo(agreementNo);
        assertThat(updated.getVirtualAccountNumber()).isEqualTo(virtualAccountNumber);
    }

    @Test
    void testReleaseInnForRejectedPartner() {
        long legalPartnerId = createLegalPartnerWithPreLegal();
        legalPartnerCommandService.sendToCheck(legalPartnerId);
        legalPartnerCommandService.reject(legalPartnerId, "");
        LegalPartner legalPartner = legalPartnerCommandService.releaseInn(legalPartnerId);

        assertThat(legalPartner.getOrganization().getTaxpayerNumber()).endsWith(LegalPartner.RELEASED_INN_SUFFIX);
        var preLegal = preLegalPartnerQueryService.getByLegalPartnerId(legalPartner.getId());
        assertThat(preLegal.getTaxpayerNumber()).endsWith(LegalPartner.RELEASED_INN_SUFFIX);
    }

    @Test
    void testReleaseInnForPartnerWithContractTerminated() {
        long legalPartnerId = createLegalPartnerWithPreLegal();
        LegalPartner legalPartner = legalPartnerRepository.findByIdOrThrow(legalPartnerId);
        pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build()
        );
        terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                                        .legalPartnerId(legalPartnerId)
                                        .build()
                        )
                        .build()
        );
        legalPartner = legalPartnerCommandService.releaseInn(legalPartnerId);

        assertThat(legalPartner.getOrganization().getTaxpayerNumber()).endsWith(LegalPartner.RELEASED_INN_SUFFIX);
        var preLegal = preLegalPartnerQueryService.getByLegalPartnerId(legalPartner.getId());
        assertThat(preLegal.getTaxpayerNumber()).endsWith(LegalPartner.RELEASED_INN_SUFFIX);
    }

    @Test
    void testReleaseInnForRejectedPartnerWithPickupPoints() {
        long legalPartnerId = createLegalPartnerWithPreLegal();

        legalPartnerCommandService.sendToCheck(legalPartnerId);
        LegalPartner legalPartner = legalPartnerCommandService.reject(legalPartnerId, "");
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .active(false)
                                .build()).build()
        );

        assertThat(pickupPoint.getActive()).isFalse();
        legalPartner = legalPartnerCommandService.releaseInn(legalPartner.getId());
        assertThat(legalPartner.getOrganization().getTaxpayerNumber()).endsWith(LegalPartner.RELEASED_INN_SUFFIX);
        var preLegal = preLegalPartnerQueryService.getByLegalPartnerId(legalPartner.getId());
        assertThat(preLegal.getTaxpayerNumber()).endsWith(LegalPartner.RELEASED_INN_SUFFIX);
    }

    @Test
    void testNotReleaseInnForRejectedPartnerWithActivePickupPoints() {
        long legalPartnerId = createLegalPartnerWithPreLegal();
        legalPartnerCommandService.sendToCheck(legalPartnerId);
        LegalPartner legalPartner = legalPartnerCommandService.reject(legalPartnerId, "");

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder().legalPartner(legalPartner).build());
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder().active(true).build());

        assertThat(pickupPoint.getActive()).isTrue();
        assertThatThrownBy(() -> legalPartnerCommandService.releaseInn(legalPartnerId))
                .isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessage("Partner has active pickup points!");
    }

    private long createLegalPartnerWithPreLegal() {
        PreLegalPartnerParams approvedPreLegalPartner = preLegalPartnerFactory.createApprovedPreLegalPartner();
        preLegalPartnerFactory.createLegalPartnerForLead(approvedPreLegalPartner.getId());
        return legalPartnerRepository.findByPartnerId(approvedPreLegalPartner.getPartnerId())
                .map(BaseJpaEntity.LongGenAud::getId)
                .orElseThrow();
    }

    @Test
    void testNotReleaseInnForNotRejectedPartner() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        assertThatThrownBy(() -> legalPartnerCommandService.releaseInn(legalPartner.getId()));
    }

    @Test
    void updateSensitiveAllFields() {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        String newOrganizationName = "Comment Out";

        String newDelegateName = "Милохин Даниил Вячеславович";
        String newDelegatePhone = "87776665544";
        String newDelegateEmail = "d.milohin@yandex.ru";

        String newAccountantName = "Гудков Александр Владимирович";
        String newAccountantPhone = "81112223344";
        String newAccountantEmail = "gudok@ya.ru";

        String newCommissionerName = "Миногарова Мария Альбертовна";

        var params = LegalPartnerParams.builder()
                .organization(LegalPartnerParams.OrganizationParams.builder()
                        .name(newOrganizationName)
                        .build())
                .delegate(LegalPartnerParams.DelegateParams.builder()
                        .delegateFio(newDelegateName)
                        .delegatePhone(newDelegatePhone)
                        .delegateEmail(newDelegateEmail)
                        .build())
                .accountant(LegalPartnerParams.AccountantParams.builder()
                        .accountantFio(newAccountantName)
                        .accountantPhone(newAccountantPhone)
                        .accountantEmail(newAccountantEmail)
                        .build())
                .commissioner(LegalPartnerParams.CommissionerParams.builder()
                        .commissionerFio(newCommissionerName)
                        .build())
                .build();

        var actual = legalPartnerCommandService.updateSensitive(legalPartner.getId(), params);

        assertThat(actual.getOrganization().getName()).isEqualTo(newOrganizationName);
        assertThat(actual.getDelegate().getDelegateName()).isEqualTo(newDelegateName);
        assertThat(actual.getDelegate().getDelegatePhone()).isEqualTo(newDelegatePhone);
        assertThat(actual.getDelegate().getDelegateEmail()).isEqualTo(newDelegateEmail);
        assertThat(actual.getAccountant().getAccountantName()).isEqualTo(newAccountantName);
        assertThat(actual.getAccountant().getAccountantPhone()).isEqualTo(newAccountantPhone);
        assertThat(actual.getAccountant().getAccountantEmail()).isEqualTo(newAccountantEmail);
        assertThat(actual.getCommissioner().getCommissionerName()).isEqualTo(newCommissionerName);
    }

    @Test
    void updateSensitiveOneField() {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        String newDelegateName = "Милохин Даниил Вячеславович";

        var params = LegalPartnerParams.builder()
                .delegate(LegalPartnerParams.DelegateParams.builder()
                        .delegateFio(newDelegateName)
                        .build())
                .build();

        var actual = legalPartnerCommandService.updateSensitive(legalPartner.getId(), params);

        assertThat(actual.getDelegate().getDelegateName()).isEqualTo(newDelegateName);
        assertThat(actual.getDelegate().getDelegatePhone()).isEqualTo(DEFAULT_DELEGATE_PHONE);
        assertThat(actual.getDelegate().getDelegateEmail()).isEqualTo(DEFAULT_DELEGATE_EMAIL);
        assertThat(actual.getAccountant().getAccountantName()).isEqualTo(DEFAULT_ACCOUNTANT_FULL_NAME);
        assertThat(actual.getAccountant().getAccountantPhone()).isEqualTo(DEFAULT_ACCOUNTANT_PHONE);
        assertThat(actual.getAccountant().getAccountantEmail()).isEqualTo(DEFAULT_ACCOUNTANT_EMAIL);
        assertThat(actual.getCommissioner().getCommissionerName()).isEqualTo(DEFAULT_COMMISSIONER_FULL_NAME);
    }

    @Test
    void tryToUpdateSensitiveForNotExistentPartner() {
        String newDelegateName = "Милохин Даниил Вячеславович";

        var params = LegalPartnerParams.builder()
                .delegate(LegalPartnerParams.DelegateParams.builder()
                        .delegateFio(newDelegateName)
                        .build())
                .build();

        assertThatThrownBy(() -> legalPartnerCommandService.updateSensitive(99L, params))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void updateBalanceClientId() {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        long newBalanceClientId = 7777777777L;

        var reply = legalPartnerCommandService.updateBalanceClient(legalPartner.getId(), newBalanceClientId);

        assertThat(reply.getBalanceClientId()).isEqualTo(newBalanceClientId);

    }

}
