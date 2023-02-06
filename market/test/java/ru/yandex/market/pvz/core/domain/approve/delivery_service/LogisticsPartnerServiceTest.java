package ru.yandex.market.pvz.core.domain.approve.delivery_service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.CreatePartnerDto;
import ru.yandex.market.logistics.management.entity.request.partner.LegalInfoDto;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.TaxationSystem;
import ru.yandex.market.pvz.client.model.partner.LegalForm;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.client.model.partner.LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP;
import static ru.yandex.market.pvz.client.model.partner.LegalPartnerType.LEGAL_PERSON;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.LogisticsPartnerService.DEFAULT_OUTLET_NAME;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.LogisticsPartnerService.NAME_PREFIX;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.LogisticsPartnerService.ORGANIZATION_URL;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.LogisticsPartnerService.PVZ_PARTNER_SUBTYPE_ID;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.LogisticsPartnerService.PVZ_PARTNER_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_BUILDING;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_CITY;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_HOUSE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_HOUSING;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_OFFICE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_STREET;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_ZIPCODE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_CHECKING_ACCOUNT_NUMBER;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_RCBIC;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.DelegateTestParams.DEFAULT_DELEGATE_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.DelegateTestParams.DEFAULT_DELEGATE_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_KPP;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_ORGANIZATION_NAME;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LogisticsPartnerServiceTest {
    private static final LocalDate DEFAULT_OFFER_SIGNED_SINCE = LocalDate.of(2021, 3, 22);
    private static final long MOCK_DELIVERY_SERVICE_ID = 1006155;

    private final TestableClock clock;

    private final TestLegalPartnerFactory legalPartnerFactory;

    private final LegalPartnerQueryService legalPartnerQueryService;

    private final LogisticsPartnerService logisticsPartnerService;

    @MockBean
    private LMSClient lmsClient;

    @BeforeEach
    void initLmsClient() {
        when(lmsClient.createPartner(any())).thenReturn(PartnerResponse.newBuilder()
                .id(MOCK_DELIVERY_SERVICE_ID)
                .build());
    }

    @Test
    void createIndividualEntrepreneurship() {
        clock.setFixed(OffsetDateTime.of(
                LocalDateTime.of(
                        DEFAULT_OFFER_SIGNED_SINCE, LocalTime.of(15, 30)), ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC);
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(TestLegalPartnerFactory.LegalPartnerTestParams.builder()
                                .organization(TestLegalPartnerFactory.LegalPartnerTestParams
                                        .OrganizationTestParams.builder()
                                        .legalType(INDIVIDUAL_ENTREPRENEURSHIP)
                                        .legalForm(null)
                                        .build())
                                .build())
                        .build());

        LegalPartnerParams legalPartnerParams = legalPartnerQueryService.get(legalPartner.getId());

        var expectedRequest = CreatePartnerDto.newBuilder()
                .partnerType(PVZ_PARTNER_TYPE)
                .subtypeId(PVZ_PARTNER_SUBTYPE_ID)
                .name(NAME_PREFIX + INDIVIDUAL_ENTREPRENEURSHIP.getDescription() + " " + DEFAULT_ORGANIZATION_NAME)
                .readableName(NAME_PREFIX + INDIVIDUAL_ENTREPRENEURSHIP.getDescription() +
                        " " + DEFAULT_ORGANIZATION_NAME)
                .billingClientId(legalPartnerParams.getBalanceClientId())
                .defaultOutletName(DEFAULT_OUTLET_NAME)
                .contractSignedSince(DEFAULT_OFFER_SIGNED_SINCE)
                .legalInfo(LegalInfoDto.newBuilder()
                        .incorporation(legalPartner.getOrganization().getFullName())
                        .ogrn(Long.valueOf(legalPartner.getOrganization().getOgrn()))
                        .url(ORGANIZATION_URL)
                        .legalForm(INDIVIDUAL_ENTREPRENEURSHIP.getDescription())
                        .inn(legalPartnerParams.getOrganization().getTaxpayerNumber())
                        .phone(DEFAULT_DELEGATE_PHONE)
                        .email(DEFAULT_DELEGATE_EMAIL)
                        .bik(DEFAULT_RCBIC)
                        .account(DEFAULT_CHECKING_ACCOUNT_NUMBER)
                        .kpp(DEFAULT_KPP)
                        .postAddress(defaultAddress())
                        .legalAddress(defaultAddress())
                        .build())
                .passportUid(legalPartner.getOwnerUid())
                .taxationSystem(TaxationSystem.COMMON)
                .build();

        long deliveryServiceId = logisticsPartnerService.create(legalPartnerParams);

        verify(lmsClient, times(1)).createPartner(eq(expectedRequest));

        assertThat(deliveryServiceId).isEqualTo(MOCK_DELIVERY_SERVICE_ID);
    }

    @Test
    void createLegalPerson() {
        clock.setFixed(OffsetDateTime.of(
                LocalDateTime.of(
                        DEFAULT_OFFER_SIGNED_SINCE, LocalTime.of(15, 30)), ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC);
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(TestLegalPartnerFactory.LegalPartnerTestParams.builder()
                                .organization(TestLegalPartnerFactory.LegalPartnerTestParams
                                        .OrganizationTestParams.builder()
                                        .legalType(LEGAL_PERSON)
                                        .legalForm(LegalForm.OAO)
                                        .build())
                                .build())
                        .build());

        LegalPartnerParams legalPartnerParams = legalPartnerQueryService.get(legalPartner.getId());

        var expectedRequest = CreatePartnerDto.newBuilder()
                .partnerType(PVZ_PARTNER_TYPE)
                .subtypeId(PVZ_PARTNER_SUBTYPE_ID)
                .name(NAME_PREFIX + LegalForm.OAO.getDescription() + " " + DEFAULT_ORGANIZATION_NAME)
                .readableName(NAME_PREFIX + LegalForm.OAO.getDescription() + " " + DEFAULT_ORGANIZATION_NAME)
                .defaultOutletName(DEFAULT_OUTLET_NAME)
                .billingClientId(legalPartnerParams.getBalanceClientId())
                .contractSignedSince(DEFAULT_OFFER_SIGNED_SINCE)
                .legalInfo(LegalInfoDto.newBuilder()
                        .incorporation(legalPartner.getOrganization().getFullName())
                        .ogrn(Long.valueOf(legalPartner.getOrganization().getOgrn()))
                        .url(ORGANIZATION_URL)
                        .legalForm(LegalForm.OAO.getDescription())
                        .inn(legalPartnerParams.getOrganization().getTaxpayerNumber())
                        .phone(DEFAULT_DELEGATE_PHONE)
                        .email(DEFAULT_DELEGATE_EMAIL)
                        .bik(DEFAULT_RCBIC)
                        .account(DEFAULT_CHECKING_ACCOUNT_NUMBER)
                        .kpp(DEFAULT_KPP)
                        .postAddress(defaultAddress())
                        .legalAddress(defaultAddress())
                        .build())
                .passportUid(legalPartner.getOwnerUid())
                .taxationSystem(TaxationSystem.COMMON)
                .build();

        long deliveryServiceId = logisticsPartnerService.create(legalPartnerParams);

        verify(lmsClient, times(1)).createPartner(eq(expectedRequest));

        assertThat(deliveryServiceId).isEqualTo(MOCK_DELIVERY_SERVICE_ID);
    }

    private Address defaultAddress() {
        return Address.newBuilder()
                .settlement(DEFAULT_CITY)
                .street(DEFAULT_STREET)
                .house(DEFAULT_HOUSE)
                .housing(DEFAULT_HOUSING)
                .building(DEFAULT_BUILDING)
                .apartment(DEFAULT_OFFICE)
                .postCode(DEFAULT_ZIPCODE)
                .build();
    }
}
