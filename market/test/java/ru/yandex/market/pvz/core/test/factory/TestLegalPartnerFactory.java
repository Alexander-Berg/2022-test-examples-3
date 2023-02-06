package ru.yandex.market.pvz.core.test.factory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.client.model.approve.ApproveStatus;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.client.model.partner.CommissionerDocument;
import ru.yandex.market.pvz.client.model.partner.LegalForm;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerType;
import ru.yandex.market.pvz.client.model.partner.TaxationSystem;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRepository;

public class TestLegalPartnerFactory {

    public static final long DEFAULT_DATASOURCE_ID = 22L;
    public static final long DEFAULT_CLIENT_ID = 33L;

    @Autowired
    private TestDeliveryServiceFactory deliveryServiceFactory;

    @Autowired
    private TestPreLegalPartnerFactory preLegalPartnerFactory;

    @Autowired
    private LegalPartnerCommandService legalPartnerCommandService;

    @Autowired
    private LegalPartnerRepository legalPartnerRepository;

    public LegalPartner createLegalPartner() {
        return createLegalPartner(LegalPartnerTestParamsBuilder.builder().build());
    }

    public LegalPartner createLegalPartner(LegalPartnerTestParamsBuilder params) {
        return createLegalPartner(params, null);
    }

    public LegalPartner createLegalPartner(LegalPartnerTestParamsBuilder params, Long balanceClientId) {
        if (params.getDeliveryService() == null) {
            params.setDeliveryService(deliveryServiceFactory.createDeliveryService());
        }
        if (params.getPreLegalPartner() == null) {
            params.setPreLegalPartner(preLegalPartnerFactory.createPreLegalPartner());
        }
        if (params.getPreLegalPartner().getApproveStatus() == PreLegalPartnerApproveStatus.CHECKING) {
            preLegalPartnerFactory.approve(params.getPreLegalPartner().getId());
        }
        var shopInfo = new SimpleShopRegistrationResponse();
        shopInfo.setDatasourceId(DEFAULT_DATASOURCE_ID);
        shopInfo.setCampaignId(params.getPreLegalPartner().getPartnerId());
        shopInfo.setClientId(balanceClientId == null ? params.getParams().getBalanceClientId() : balanceClientId);
        shopInfo.setOwnerId(RandomUtils.nextLong(1, Long.MAX_VALUE));
        var legalPartner = legalPartnerCommandService.create(
                buildLegalPartner(params.getParams()), params.getDeliveryService().getId(),
                shopInfo, ApproveStatus.CHECKING, params.getPreLegalPartner().getId());
        if (params.isApprovePreLegalPartner()) {
            preLegalPartnerFactory.fullActivate(params.getPreLegalPartner().getId());
        }
        return legalPartner;
    }

    public LegalPartner updateLegalPartner(long id, LegalPartnerTestParams params) {
        return legalPartnerCommandService.update(id, buildLegalPartner(params));
    }

    public LegalPartner updateInsensitiveLegalPartner(long id, LegalPartnerTestParams params) {
        return legalPartnerCommandService.updateInsensitive(id, buildLegalPartner(params));
    }

    public LegalPartner sendToCheck(long id) {
        return legalPartnerCommandService.sendToCheck(id);
    }

    public LegalPartner approve(long id, String agreementNo, LocalDate agreementDate) {
        return legalPartnerCommandService.approve(id, agreementNo, agreementDate);
    }

    public LegalPartner reject(long id, String reason) {
        return legalPartnerCommandService.reject(id, reason);
    }

    public LegalPartner forceApprove(long id, LocalDate agreementDate) {
        return forceApprove(id, String.valueOf(RandomUtils.nextInt()), agreementDate);
    }

    @Transactional
    public LegalPartner forceApproveWithOffer(long id, LocalDate offerDate) {
        LegalPartner legalPartner = legalPartnerRepository.findByIdOrThrow(id);
        legalPartner.setApproveStatus(ApproveStatus.APPROVED);
        legalPartner.setOfferSignedSince(offerDate);
        return legalPartnerRepository.save(legalPartner);
    }

    public LegalPartner forceApprove(long id, String agreementNo, LocalDate agreementDate) {
        legalPartnerCommandService.sendToCheck(id);
        return approve(id, agreementNo, agreementDate);
    }

    public LegalPartner forceReject(long id, String reason) {
        legalPartnerCommandService.sendToCheck(id);
        return reject(id, reason);
    }

    @Transactional
    public LegalPartner forceApproveStatus(long id, ApproveStatus status) {
        LegalPartner partner = legalPartnerRepository.findByIdOrThrow(id);
        partner.setApproveStatus(status);
        return legalPartnerRepository.save(partner);
    }

    private LegalPartnerParams buildLegalPartner(LegalPartnerTestParams params) {
        return LegalPartnerParams.builder()
                .partnerId(params.getPartnerId())
                .agreementNo(params.getAgreementNo())
                .agreementDate(params.getAgreementDate())
                .offerSignedSince(params.getOfferSignedSince())
                .virtualAccountNumber(params.getVirtualAccountNumber())
                .organization(LegalPartnerParams.OrganizationParams.builder()
                        .legalType(params.getOrganization().getLegalType())
                        .legalForm(params.getOrganization().getLegalForm())
                        .name(params.getOrganization().getName())
                        .taxpayerNumber(params.getOrganization().getTaxpayerNumber())
                        .kpp(params.getOrganization().getKpp())
                        .ogrn(params.getOrganization().getOgrn())
                        .issueDate(params.getOrganization().getIssueDate())
                        .build())
                .delegate(LegalPartnerParams.DelegateParams.builder()
                        .delegateName(LegalPartnerParams.PersonNameParams.builder()
                                .firstName(params.getDelegate().getDelegateName().getFirstName())
                                .lastName(params.getDelegate().getDelegateName().getLastName())
                                .patronymic(params.getDelegate().getDelegateName().getPatronymic())
                                .build())
                        .delegateFio(params.getDelegate().getDelegateFullName())
                        .delegateEmail(params.getDelegate().getDelegateEmail())
                        .delegatePhone(params.getDelegate().getDelegatePhone())
                        .build())
                .accountant(LegalPartnerParams.AccountantParams.builder()
                        .accountantName(LegalPartnerParams.PersonNameParams.builder()
                                .firstName(params.getAccountant().getAccountantName().getFirstName())
                                .lastName(params.getAccountant().getAccountantName().getLastName())
                                .patronymic(params.getAccountant().getAccountantName().getPatronymic())
                                .build())
                        .accountantFio(params.getAccountant().getAccountantFullName())
                        .accountantEmail(params.getAccountant().getAccountantEmail())
                        .accountantPhone(params.getAccountant().getAccountantPhone())
                        .build())
                .businessAddress(LegalPartnerParams.AddressParams.builder()
                        .city(params.getBusinessAddress().getCity())
                        .street(params.getBusinessAddress().getStreet())
                        .house(params.getBusinessAddress().getHouse())
                        .housing(params.getBusinessAddress().getHousing())
                        .building(params.getBusinessAddress().getBuilding())
                        .office(params.getBusinessAddress().getOffice())
                        .zipcode(params.getBusinessAddress().getZipcode())
                        .build())
                .postAddress(LegalPartnerParams.AddressParams.builder()
                        .city(params.getPostAddress().getCity())
                        .street(params.getPostAddress().getStreet())
                        .house(params.getPostAddress().getHouse())
                        .housing(params.getPostAddress().getHousing())
                        .building(params.getPostAddress().getBuilding())
                        .office(params.getPostAddress().getOffice())
                        .zipcode(params.getPostAddress().getZipcode())
                        .build())
                .taxation(params.getTaxation())
                .commissioner(LegalPartnerParams.CommissionerParams.builder()
                        .commissionerPosition(params.getCommissioner().getCommissionerPosition())
                        .commissionerName(LegalPartnerParams.PersonNameParams.builder()
                                .firstName(params.getCommissioner().getCommissionerName().getFirstName())
                                .lastName(params.getCommissioner().getCommissionerName().getLastName())
                                .patronymic(params.getCommissioner().getCommissionerName().getPatronymic())
                                .build())
                        .commissionerFio(params.getCommissioner().getCommissionerFullName())
                        .commissionerDocument(params.getCommissioner().getCommissionerDocument())
                        .build())
                .bank(LegalPartnerParams.BankParams.builder()
                        .bankKpp(params.getBank().getKpp())
                        .rcbic(params.getBank().getRcbic())
                        .checkingAccountNumber(params.getBank().getCheckingAccountNumber())
                        .correspondentAccountNumber(params.getBank().getCorrespondentAccountNumber())
                        .bank(params.getBank().getBank())
                        .bankCity(params.getBank().getBankCity())
                        .build())
                .balanceClientId(params.getBalanceClientId())
                .partnerId(params.getPartnerId())
                .build();
    }

    @Data
    @Builder
    public static class LegalPartnerTestParamsBuilder {

        @Builder.Default
        private LegalPartnerTestParams params = LegalPartnerTestParams.builder().build();

        // TODO: must not be set in creation (set via Startrek during approving)
        private DeliveryService deliveryService;

        private PreLegalPartnerParams preLegalPartner;

        @Builder.Default
        private boolean approvePreLegalPartner = true;

    }

    @Data
    @Builder
    public static class LegalPartnerTestParams {

        public static final DelegateTestParams DEFAULT_DELEGATE = DelegateTestParams.builder().build();
        public static final AccountantTestParams DEFAULT_ACCOUNTANT = AccountantTestParams.builder().build();
        public static final AddressTestParams DEFAULT_BUSINESS_ADDRESS = AddressTestParams.builder().build();
        public static final AddressTestParams DEFAULT_POST_ADDRESS = AddressTestParams.builder().build();
        public static final TaxationSystem DEFAULT_TAXATION_SYSTEM = TaxationSystem.COMMON;
        public static final CommissionerTestParams DEFAULT_COMMISSIONER = CommissionerTestParams.builder().build();
        public static final BankTestParams DEFAULT_BANK = BankTestParams.builder().build();
        public static final LocalDate DEFAULT_AGREEMENT_DATE = LocalDate.of(2020, 9, 22);

        @Builder.Default
        private Long partnerId = RandomUtils.nextLong();

        @Builder.Default
        private String agreementNo = String.valueOf(RandomUtils.nextInt());

        @Builder.Default
        private LocalDate agreementDate = DEFAULT_AGREEMENT_DATE;

        private LocalDate offerSignedSince;

        @Builder.Default
        private String virtualAccountNumber = "Virtual Account Number";

        @Builder.Default
        private OrganizationTestParams organization = OrganizationTestParams.builder().build();

        @Builder.Default
        private DelegateTestParams delegate = DEFAULT_DELEGATE;

        @Builder.Default
        private AccountantTestParams accountant = DEFAULT_ACCOUNTANT;

        @Builder.Default
        private AddressTestParams businessAddress = DEFAULT_BUSINESS_ADDRESS;

        @Builder.Default
        private AddressTestParams postAddress = DEFAULT_POST_ADDRESS;

        @Builder.Default
        private TaxationSystem taxation = DEFAULT_TAXATION_SYSTEM;

        @Builder.Default
        private CommissionerTestParams commissioner = DEFAULT_COMMISSIONER;

        @Builder.Default
        private BankTestParams bank = DEFAULT_BANK;

        @Builder.Default
        private Long balanceClientId = RandomUtils.nextLong(0, 1000);

        @Data
        @Builder
        public static class OrganizationTestParams {

            private static final int[] INN_10_CHECKSUM_FACTORS = {2, 4, 10, 3, 5, 9, 4, 6, 8};

            public static final LegalPartnerType DEFAULT_LEGAL_TYPE = LegalPartnerType.LEGAL_PERSON;
            public static final LegalForm DEFAULT_LEGAL_FORM = LegalForm.OOO;
            public static final String DEFAULT_ORGANIZATION_NAME = "ВАСИЛИЙ ДЕЛИВЕРИ СОЛЮШЕНС АНЛИМИТЕД";
            public static final String DEFAULT_FULL_ORGANIZATION_NAME = "ООО ВАСИЛИЙ ДЕЛИВЕРИ СОЛЮШЕНС АНЛИМИТЕД";
            public static final String DEFAULT_KPP = "399401234";
            public static final String DEFAULT_OGRN = "315774500419700";
            public static final LocalDate DEFAULT_ISSUE_DATE = LocalDate.of(2020, 10, 8);

            @Builder.Default
            private LegalPartnerType legalType = DEFAULT_LEGAL_TYPE;

            @Builder.Default
            private LegalForm legalForm = DEFAULT_LEGAL_FORM;

            @Builder.Default
            private String name = DEFAULT_ORGANIZATION_NAME;

            @Builder.Default
            private String taxpayerNumber = generateRandomINN();

            @Builder.Default
            private String kpp = DEFAULT_KPP;

            @Builder.Default
            private String ogrn = DEFAULT_OGRN;

            @Builder.Default
            private LocalDate issueDate = DEFAULT_ISSUE_DATE;

            public static String generateRandomINN() {
                List<Integer> nums = new ArrayList<>();
                var checkSum = 0;
                for (int i = 0; i < INN_10_CHECKSUM_FACTORS.length; i++) {
                    nums.add(RandomUtils.nextInt(i == 0 ? 1 : 0, 9));
                    checkSum += INN_10_CHECKSUM_FACTORS[i] * nums.get(i);
                }
                nums.add(checkSum % 11 % 10);
                return StreamEx.of(nums).joining();
            }
        }

        @Data
        @Builder
        public static class DelegateTestParams {

            public static final PersonNameTestParams DEFAULT_DELEGATE_NAME = PersonNameTestParams.builder().build();
            public static final String DEFAULT_DELEGATE_FULL_NAME = PersonNameTestParams.DEFAULT_NAME;
            public static final String DEFAULT_DELEGATE_EMAIL = "sberbank@sberbank.ru";
            public static final String DEFAULT_DELEGATE_PHONE = "+7(495)747-37-31";

            @Builder.Default
            private PersonNameTestParams delegateName = DEFAULT_DELEGATE_NAME;

            @Builder.Default
            private String delegateFullName = DEFAULT_DELEGATE_FULL_NAME;

            @Builder.Default
            private String delegateEmail = DEFAULT_DELEGATE_EMAIL;

            @Builder.Default
            private String delegatePhone = DEFAULT_DELEGATE_PHONE;
        }

        @Data
        @Builder
        public static class AccountantTestParams {

            public static final PersonNameTestParams DEFAULT_ACCOUNTANT_NAME = PersonNameTestParams.builder().build();
            public static final String DEFAULT_ACCOUNTANT_FULL_NAME = PersonNameTestParams.DEFAULT_NAME;
            public static final String DEFAULT_ACCOUNTANT_EMAIL = "sberbank@sberbank.ru";
            public static final String DEFAULT_ACCOUNTANT_PHONE = "+7(495)747-37-31";

            @Builder.Default
            private PersonNameTestParams accountantName = DEFAULT_ACCOUNTANT_NAME;

            @Builder.Default
            private String accountantFullName = DEFAULT_ACCOUNTANT_FULL_NAME;

            @Builder.Default
            private String accountantEmail = DEFAULT_ACCOUNTANT_EMAIL;

            @Builder.Default
            private String accountantPhone = DEFAULT_ACCOUNTANT_PHONE;
        }

        @Data
        @Builder
        public static class CommissionerTestParams {

            public static final String DEFAULT_COMMISSIONER_POSITION = "генеральный директор";
            public static final PersonNameTestParams DEFAULT_COMMISSIONER_NAME = PersonNameTestParams.builder().build();
            public static final String DEFAULT_COMMISSIONER_FULL_NAME = PersonNameTestParams.DEFAULT_NAME;
            public static final CommissionerDocument DEFAULT_COMMISSIONER_DOCUMENT = CommissionerDocument.CHARTER;

            @Builder.Default
            private String commissionerPosition = DEFAULT_COMMISSIONER_POSITION;

            @Builder.Default
            private PersonNameTestParams commissionerName = DEFAULT_COMMISSIONER_NAME;

            @Builder.Default
            private String commissionerFullName = DEFAULT_COMMISSIONER_FULL_NAME;

            @Builder.Default
            private CommissionerDocument commissionerDocument = DEFAULT_COMMISSIONER_DOCUMENT;

        }

        @Data
        @Builder
        public static class BankTestParams {

            public static final String DEFAULT_BANK_KPP = "339401284";
            public static final String DEFAULT_RCBIC = "044525225";
            public static final String DEFAULT_CHECKING_ACCOUNT_NUMBER = "41202910500000000001";
            public static final String DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER = "30101810400000000225";
            public static final String DEFAULT_BANK_NAME = "Сбербанк";
            public static final String DEFAULT_BANK_CITY = "Москва";

            @Builder.Default
            private String kpp = DEFAULT_BANK_KPP;

            @Builder.Default
            private String rcbic = DEFAULT_RCBIC;

            @Builder.Default
            private String checkingAccountNumber = DEFAULT_CHECKING_ACCOUNT_NUMBER;

            @Builder.Default
            private String correspondentAccountNumber = DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER;

            @Builder.Default
            private String bank = DEFAULT_BANK_NAME;

            @Builder.Default
            private String bankCity = DEFAULT_BANK_CITY;
        }

        @Data
        @Builder
        public static class PersonNameTestParams {

            public static final String DEFAULT_FIRST_NAME = "Герман";
            public static final String DEFAULT_LAST_NAME = "Греф";
            public static final String DEFAULT_PATRONYMIC = "Оскарович";
            public static final String DEFAULT_NAME = "Греф Герман Оскарович";

            @Builder.Default
            private String firstName = DEFAULT_FIRST_NAME;

            @Builder.Default
            private String lastName = DEFAULT_LAST_NAME;

            @Builder.Default
            private String patronymic = DEFAULT_PATRONYMIC;
        }

        @Data
        @Builder
        public static class AddressTestParams {

            public static final String DEFAULT_CITY = "Москва";
            public static final String DEFAULT_STREET = "Ленинский проспект";
            public static final String DEFAULT_HOUSE = "20";
            public static final String DEFAULT_HOUSING = "2";
            public static final String DEFAULT_BUILDING = "1";
            public static final String DEFAULT_OFFICE = "223";
            public static final String DEFAULT_ZIPCODE = "123456";

            @Builder.Default
            private String city = DEFAULT_CITY;

            @Builder.Default
            private String street = DEFAULT_STREET;

            @Builder.Default
            private String house = DEFAULT_HOUSE;

            @Builder.Default
            private String housing = DEFAULT_HOUSING;

            @Builder.Default
            private String building = DEFAULT_BUILDING;

            @Builder.Default
            private String office = DEFAULT_OFFICE;

            @Builder.Default
            private String zipcode = DEFAULT_ZIPCODE;

        }
    }
}
