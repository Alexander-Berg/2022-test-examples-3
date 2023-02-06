package ru.yandex.market.pvz.internal.domain.legal_partner;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.pvz.client.crm.dto.CrmPayloadDto;
import ru.yandex.market.pvz.client.crm.dto.CrmPayloadType;
import ru.yandex.market.pvz.client.crm.dto.PreLegalPartnerCrmDto;
import ru.yandex.market.pvz.client.model.approve.ApproveStatus;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.core.domain.approve.crm.CrmApproveFlowBranches;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRequestData;
import ru.yandex.market.pvz.core.domain.legal_partner.offer.model.LegalPartnerOfferCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.offer.model.LegalPartnerOfferQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.AccountantDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.AccountantFinishDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.AddressDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.BankDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.CommissionerDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.CommissionerFinishDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.DelegateDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.InsensitiveLegalPartnerDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.LegalPartnerDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.LegalPartnerFinishRegisterDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.OrganizationDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.PersonNameDto;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.mapper.LegalPartnerDtoMapper;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderReportDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.Recipient;
import ru.yandex.market.pvz.internal.controller.pi.report.dto.OrderReportParamsDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.logbroker.domain.outgoing.QueuedLogbrokerEventManager;
import ru.yandex.market.tpl.common.logbroker.domain.outgoing.QueuedLogbrokerEventParams;
import ru.yandex.market.tpl.common.util.TplObjectMappers;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.SEPARATE_PARTNER_AND_PVZ_FLOWS;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.ACTIVE;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.APPROVED_BY_CRM;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.AWAITING_CRM_APPROVE;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.CONTRACT_TERMINATED;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.DEBT;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.PICKUP_POINT_CREATION_REQUIRED;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.REJECTED;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.REJECTED_BY_CRM;
import static ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerStatus.REJECTED_WITHOUT_RETRY;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AccountantTestParams.DEFAULT_ACCOUNTANT_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AccountantTestParams.DEFAULT_ACCOUNTANT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_BUILDING;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_CITY;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_HOUSE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_HOUSING;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_OFFICE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_STREET;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.AddressTestParams.DEFAULT_ZIPCODE;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_BANK_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_CHECKING_ACCOUNT_NUMBER;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.BankTestParams.DEFAULT_RCBIC;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.CommissionerTestParams.DEFAULT_COMMISSIONER_DOCUMENT;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.CommissionerTestParams.DEFAULT_COMMISSIONER_POSITION;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.DEFAULT_TAXATION_SYSTEM;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.generateRandomINN;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.PersonNameTestParams.DEFAULT_FIRST_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.PersonNameTestParams.DEFAULT_LAST_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.PersonNameTestParams.DEFAULT_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.PersonNameTestParams.DEFAULT_PATRONYMIC;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_LEGAL_FORM;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_LEGAL_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_OGRN;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_ORGANISATION_NAME;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerServiceTest {

    private static final long BALANCE_CLIENT_ID = 12345L;
    private static final String TRACER_UID = "uid";
    private static final String FILE_NAME = "file.pdf";
    private static final String CONTENT_TYPE = MediaType.APPLICATION_PDF_VALUE;
    private static final byte[] FILE_DATA = {4, 8, 15, 16, 23, 42};
    private static final String OFFER_URL = "https://pvz-int.vs.market.yandex.net/v1/pi/partners/{}/offer";
    private static final long BUYER_YANDEX_UID = 88005553535L;
    private static final String RECIPIENT_PHONE = "+7987654321";

    private static final ObjectMapper OBJECT_MAPPER = TplObjectMappers.baseObjectMapper();

    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestLegalPartnerTerminationFactory terminationFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory testOrderFactory;

    private final LegalPartnerService legalPartnerService;
    private final LegalPartnerDtoMapper mapper;

    private final LegalPartnerOfferCommandService legalPartnerOfferCommandService;
    private final LegalPartnerOfferQueryService legalPartnerOfferQueryService;
    private final LegalPartnerQueryService legalPartnerQueryService;

    private final QueuedLogbrokerEventManager queuedLogbrokerEventManager;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    private final Clock clock;
    private LegalPartnerRequestData requestData;
    private Order cardPaidOrder;
    private Order onDemandOrder;
    private Order fashionOrder;

    @SpyBean
    private CrmApproveFlowBranches crmApproveFlowBranches;

    @MockBean
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void configure() {
        configurationGlobalCommandService.setValue(SEPARATE_PARTNER_AND_PVZ_FLOWS, true);

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        requestData = new LegalPartnerRequestData(legalPartner.getId(),
                legalPartner.getPartnerId(), legalPartner.getOrganization().getFullName(), 1L);
        cardPaidOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("1")
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        Order cashPaidOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CASH)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .recipientPhone(RECIPIENT_PHONE)
                        .buyerYandexUid(BUYER_YANDEX_UID)
                        .externalId("2")
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        Order arrivedOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("3")
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        testOrderFactory.receiveOrder(arrivedOrder.getId());

        Order deliveredOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("4")
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        testOrderFactory.receiveOrder(deliveredOrder.getId());
        testOrderFactory.deliverOrder(
                deliveredOrder.getId(),
                OrderDeliveryType.UNKNOWN,
                OrderPaymentType.PREPAID
        );

        onDemandOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        fashionOrder = testOrderFactory.createSimpleFashionOrder(false, pickupPoint);
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    void createPartnerFromPrePartner() {
        var preLegalPartner = preLegalPartnerFactory.createApprovedPreLegalPartner();
        LegalPartnerFinishRegisterDto requestDto = buildLegalPartnerFinishRegisterDto().build();

        LegalPartnerDto actualResponse = legalPartnerService.create(preLegalPartner.getPartnerId(), requestDto);

        LegalPartnerParams actualCreated = legalPartnerQueryService.getByPartnerId(preLegalPartner.getPartnerId());

        LegalPartnerParams expectedCreated = LegalPartnerParams.builder()
                .id(actualCreated.getId())
                .partnerId(preLegalPartner.getPartnerId())
                .organization(LegalPartnerParams.OrganizationParams.builder()
                        .legalType(DEFAULT_LEGAL_TYPE)
                        .legalForm(DEFAULT_LEGAL_FORM)
                        .name(DEFAULT_ORGANISATION_NAME)
                        .taxpayerNumber(actualResponse.getOrganization().getTaxpayerNumber())
                        .ogrn(DEFAULT_OGRN)
                        .build())
                .delegate(LegalPartnerParams.DelegateParams.builder()
                        .delegateName(LegalPartnerParams.PersonNameParams.builder()
                                .firstName(DEFAULT_DELEGATE_NAME.split(" ")[1])
                                .lastName(DEFAULT_DELEGATE_NAME.split(" ")[0])
                                .patronymic(DEFAULT_DELEGATE_NAME.split(" ")[2])
                                .build())
                        .delegateFio(DEFAULT_DELEGATE_NAME)
                        .delegateEmail(DEFAULT_DELEGATE_EMAIL)
                        .delegatePhone(DEFAULT_DELEGATE_PHONE)
                        .build())
                .accountant(LegalPartnerParams.AccountantParams.builder()
                        .accountantName(LegalPartnerParams.PersonNameParams.builder()
                                .firstName(DEFAULT_FIRST_NAME)
                                .lastName(DEFAULT_LAST_NAME)
                                .patronymic(DEFAULT_PATRONYMIC)
                                .build())
                        .accountantFio(DEFAULT_LAST_NAME + " " + DEFAULT_FIRST_NAME + " " + DEFAULT_PATRONYMIC)
                        .accountantEmail(DEFAULT_ACCOUNTANT_EMAIL)
                        .accountantPhone(DEFAULT_ACCOUNTANT_PHONE)
                        .build())
                .businessAddress(LegalPartnerParams.AddressParams.builder()
                        .city(DEFAULT_CITY)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .office(DEFAULT_OFFICE)
                        .zipcode(DEFAULT_ZIPCODE)
                        .build())
                .postAddress(LegalPartnerParams.AddressParams.builder()
                        .city(DEFAULT_CITY)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .office(DEFAULT_OFFICE)
                        .zipcode(DEFAULT_ZIPCODE)
                        .build())
                .taxation(DEFAULT_TAXATION_SYSTEM)
                .commissioner(LegalPartnerParams.CommissionerParams.builder()
                        .commissionerName(LegalPartnerParams.PersonNameParams.builder()
                                .firstName(DEFAULT_FIRST_NAME)
                                .lastName(DEFAULT_LAST_NAME)
                                .patronymic(DEFAULT_PATRONYMIC)
                                .build())
                        .commissionerFio(DEFAULT_LAST_NAME + " " + DEFAULT_FIRST_NAME + " " + DEFAULT_PATRONYMIC)
                        .commissionerDocument(DEFAULT_COMMISSIONER_DOCUMENT)
                        .commissionerPosition(DEFAULT_COMMISSIONER_POSITION)
                        .build())
                .bank(LegalPartnerParams.BankParams.builder()
                        .rcbic(DEFAULT_RCBIC)
                        .checkingAccountNumber(DEFAULT_CHECKING_ACCOUNT_NUMBER)
                        .correspondentAccountNumber(DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER)
                        .bank(DEFAULT_BANK_NAME)
                        .build())
                .approveStatus(ApproveStatus.CHECKING)
                .marketShopId(preLegalPartner.getMarketShopId())
                .balanceClientId(preLegalPartner.getBalanceClientId())
                .ownerUid(preLegalPartner.getOwnerUid())
                .build();

        assertThat(actualCreated).isEqualTo(expectedCreated);

        LegalPartnerDto expectedResponse = LegalPartnerDto.builder()
                .id(actualCreated.getId())
                .partnerId(preLegalPartner.getPartnerId())
                .organization(OrganizationDto.builder()
                        .legalType(DEFAULT_LEGAL_TYPE)
                        .legalForm(DEFAULT_LEGAL_FORM)
                        .name(DEFAULT_ORGANISATION_NAME)
                        .taxpayerNumber(actualResponse.getOrganization().getTaxpayerNumber())
                        .ogrn(DEFAULT_OGRN)
                        .build())
                .delegate(DelegateDto.builder()
                        .delegateName(PersonNameDto.builder()
                                .firstName(DEFAULT_DELEGATE_NAME.split(" ")[1])
                                .lastName(DEFAULT_DELEGATE_NAME.split(" ")[0])
                                .patronymic(DEFAULT_DELEGATE_NAME.split(" ")[2])
                                .build())
                        .delegateFullName(DEFAULT_DELEGATE_NAME)
                        .delegateEmail(DEFAULT_DELEGATE_EMAIL)
                        .delegatePhone(DEFAULT_DELEGATE_PHONE)
                        .build())
                .accountant(AccountantDto.builder()
                        .accountantName(PersonNameDto.builder()
                                .firstName(DEFAULT_FIRST_NAME)
                                .lastName(DEFAULT_LAST_NAME)
                                .patronymic(DEFAULT_PATRONYMIC)
                                .build())
                        .accountantFullName(DEFAULT_NAME)
                        .accountantEmail(DEFAULT_ACCOUNTANT_EMAIL)
                        .accountantPhone(DEFAULT_ACCOUNTANT_PHONE)
                        .build())
                .businessAddress(AddressDto.builder()
                        .city(DEFAULT_CITY)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .office(DEFAULT_OFFICE)
                        .zipcode(DEFAULT_ZIPCODE)
                        .build())
                .postAddress(AddressDto.builder()
                        .city(DEFAULT_CITY)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .office(DEFAULT_OFFICE)
                        .zipcode(DEFAULT_ZIPCODE)
                        .build())
                .taxation(DEFAULT_TAXATION_SYSTEM)
                .commissioner(CommissionerDto.builder()
                        .commissionerName(PersonNameDto.builder()
                                .firstName(DEFAULT_FIRST_NAME)
                                .lastName(DEFAULT_LAST_NAME)
                                .patronymic(DEFAULT_PATRONYMIC)
                                .build())
                        .commissionerFullName(DEFAULT_NAME)
                        .commissionerDocument(DEFAULT_COMMISSIONER_DOCUMENT)
                        .commissionerPosition(DEFAULT_COMMISSIONER_POSITION)
                        .build())
                .bank(BankDto.builder()
                        .rcbic(DEFAULT_RCBIC)
                        .checkingAccountNumber(DEFAULT_CHECKING_ACCOUNT_NUMBER)
                        .correspondentAccountNumber(DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER)
                        .bank(DEFAULT_BANK_NAME)
                        .build())
                .approveStatus(ApproveStatus.CHECKING)
                .actions(List.of())
                .build();

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(crmApproveFlowBranches, never()).afterSecurityApproveOrPickupPointApprove(any());
    }

    private LegalPartnerFinishRegisterDto.LegalPartnerFinishRegisterDtoBuilder buildLegalPartnerFinishRegisterDto() {
        return LegalPartnerFinishRegisterDto.builder()
                .accountant(AccountantFinishDto.builder()
                        .accountantName(DEFAULT_NAME)
                        .accountantEmail(DEFAULT_ACCOUNTANT_EMAIL)
                        .accountantPhone(DEFAULT_ACCOUNTANT_PHONE)
                        .build())
                .businessAddress(AddressDto.builder()
                        .city(DEFAULT_CITY)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .office(DEFAULT_OFFICE)
                        .zipcode(DEFAULT_ZIPCODE)
                        .build())
                .postAddress(AddressDto.builder()
                        .city(DEFAULT_CITY)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .office(DEFAULT_OFFICE)
                        .zipcode(DEFAULT_ZIPCODE)
                        .build())
                .taxation(DEFAULT_TAXATION_SYSTEM)
                .commissioner(CommissionerFinishDto.builder()
                        .commissionerPosition(DEFAULT_COMMISSIONER_POSITION)
                        .commissionerName(DEFAULT_NAME)
                        .commissionerDocument(DEFAULT_COMMISSIONER_DOCUMENT)
                        .build())
                .bank(BankDto.builder()
                        .rcbic(DEFAULT_RCBIC)
                        .checkingAccountNumber(DEFAULT_CHECKING_ACCOUNT_NUMBER)
                        .correspondentAccountNumber(DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER)
                        .bank(DEFAULT_BANK_NAME)
                        .build());
    }

    private InsensitiveLegalPartnerDto.InsensitiveLegalPartnerDtoBuilder buildInsensitiveLegalPartnerDto() {
        return InsensitiveLegalPartnerDto.builder()
                .businessAddress(AddressDto.builder()
                        .city(DEFAULT_CITY)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .office(DEFAULT_OFFICE)
                        .zipcode(DEFAULT_ZIPCODE)
                        .build())
                .postAddress(AddressDto.builder()
                        .city(DEFAULT_CITY)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .office(DEFAULT_OFFICE)
                        .zipcode(DEFAULT_ZIPCODE)
                        .build())
                .bank(BankDto.builder()
                        .rcbic(DEFAULT_RCBIC)
                        .checkingAccountNumber(DEFAULT_CHECKING_ACCOUNT_NUMBER)
                        .correspondentAccountNumber(DEFAULT_CORRESPONDENT_ACCOUNT_NUMBER)
                        .bank(DEFAULT_BANK_NAME)
                        .build())
                .taxation(DEFAULT_TAXATION_SYSTEM);
    }

    @Test
    void createPartnerFromSecurityApprovedPrePartner() {
        var preLegalPartner = preLegalPartnerFactory.createApprovedPreLegalPartner();
        preLegalPartner = preLegalPartnerFactory.forceChangeStatus(
                preLegalPartner.getId(), PreLegalPartnerApproveStatus.APPROVED_BY_SECURITY);
        LegalPartnerFinishRegisterDto requestDto = buildLegalPartnerFinishRegisterDto().build();

        LegalPartnerDto actualResponse = legalPartnerService.create(preLegalPartner.getPartnerId(), requestDto);
        assertThat(actualResponse.getPartnerId()).isEqualTo(preLegalPartner.getPartnerId());

        verify(crmApproveFlowBranches, times(1)).afterSecurityApproveOrPickupPointApprove(any());
    }

    @Test
    void tryToCreatePartnerFromCheckingPrePartner() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        LegalPartnerFinishRegisterDto requestDto = buildLegalPartnerFinishRegisterDto().build();

        assertThatThrownBy(() -> legalPartnerService.create(preLegalPartner.getPartnerId(), requestDto))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void tryToCreatePartnerFromRejectedPrePartner() {
        var preLegalPartner = preLegalPartnerFactory.createRejectedPreLegalPartner();
        LegalPartnerFinishRegisterDto requestDto = buildLegalPartnerFinishRegisterDto().build();

        assertThatThrownBy(() -> legalPartnerService.create(preLegalPartner.getPartnerId(), requestDto))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void tryToCreatePartnerFromNotExistentPrePartner() {
        var preLegalPartner = preLegalPartnerFactory.createApprovedPreLegalPartner();
        LegalPartnerFinishRegisterDto requestDto = buildLegalPartnerFinishRegisterDto().build();

        assertThatThrownBy(() -> legalPartnerService.create(preLegalPartner.getPartnerId() + 1, requestDto))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void testNewPartner() {
        LegalPartner partner = createInStatus(ApproveStatus.NEW);
        assertThat(legalPartnerService.getStatus(partner.getPartnerId())).isEqualTo(PICKUP_POINT_CREATION_REQUIRED);
    }

    @Test
    void testActive() {
        LegalPartner partner = createInStatus(ApproveStatus.APPROVED);
        assertThat(legalPartnerService.getStatus(partner.getPartnerId())).isEqualTo(ACTIVE);
    }

    @Test
    void testDebt() {
        LegalPartner partner = createInStatus(ApproveStatus.APPROVED);
        terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.DEBT)
                                        .fromTime(OffsetDateTime.now())
                                        .legalPartnerId(partner.getId())
                                        .build()
                        )
                        .build()
        );
        assertThat(legalPartnerService.getStatus(partner.getPartnerId())).isEqualTo(DEBT);
    }

    @Test
    void testContractTerminated() {
        LegalPartner partner = createInStatus(ApproveStatus.APPROVED);
        terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                                        .fromTime(OffsetDateTime.now())
                                        .legalPartnerId(partner.getId())
                                        .build()
                        )
                        .build()
        );
        assertThat(legalPartnerService.getStatus(partner.getPartnerId())).isEqualTo(CONTRACT_TERMINATED);
    }

    @Test
    void testRejected() {
        LegalPartner partner = createInStatus(ApproveStatus.REJECTED);
        assertThat(legalPartnerService.getStatus(partner.getPartnerId())).isEqualTo(REJECTED);
    }

    private LegalPartner createInStatus(ApproveStatus status) {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        return legalPartnerFactory.forceApproveStatus(partner.getId(), status);
    }

    @Test
    void testRejectedWithoutRetry() {
        LegalPartner partner = createInStatus(ApproveStatus.REJECTED_WITHOUT_RETRY);
        assertThat(legalPartnerService.getStatus(partner.getPartnerId())).isEqualTo(REJECTED_WITHOUT_RETRY);
    }

    @Test
    void testCheckingByCrm() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        assertThat(legalPartnerService.getStatus(preLegalPartner.getPartnerId())).isEqualTo(AWAITING_CRM_APPROVE);
    }

    @Test
    void testApprovedByCrm() {
        var preLegalPartner = preLegalPartnerFactory.createApprovedPreLegalPartner();
        assertThat(legalPartnerService.getStatus(preLegalPartner.getPartnerId())).isEqualTo(APPROVED_BY_CRM);
    }

    @Test
    void testRejectedByCrm() {
        var preLegalPartner = preLegalPartnerFactory.createRejectedPreLegalPartner();
        assertThat(legalPartnerService.getStatus(preLegalPartner.getPartnerId())).isEqualTo(REJECTED_BY_CRM);
    }

    @Test
    void testToUpdateParamsWhenPartnerInStatusRejectedWithoutRetry() {
        LegalPartner partner = createInStatus(ApproveStatus.REJECTED_WITHOUT_RETRY);
        assertThatThrownBy(() -> legalPartnerService.update(partner.getPartnerId(), mapper.map(partner)))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void tryToUpdatePartnerWithNotUniqueTaxpayer() {
        String inn = generateRandomINN();
        legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(TestLegalPartnerFactory.LegalPartnerTestParams.builder()
                                .organization(TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams
                                        .builder()
                                        .taxpayerNumber(inn)
                                        .build())
                                .build())
                        .build()
        );
        LegalPartner legalPartner2 = legalPartnerFactory.createLegalPartner();

        LegalPartnerDto dto = mapper.map(legalPartner2);
        OrganizationDto organizationDto = dto.getOrganization();
        organizationDto.setTaxpayerNumber(inn);
        assertThatThrownBy(() -> legalPartnerService.update(legalPartner2.getPartnerId(), dto))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void updatePartnerWithTheSameData() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .approvePreLegalPartner(false)
                        .build()
        );
        legalPartner = legalPartnerFactory.reject(legalPartner.getId(), "Не благонадежный");
        LegalPartnerDto created = mapper.map(legalPartner);

        LegalPartnerDto updated = legalPartnerService.update(legalPartner.getPartnerId(), created);

        assertThat(updated).isEqualTo(created);
    }

    @Test
    void updateInsensitiveTest() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .approvePreLegalPartner(false)
                        .build()
        );

        LegalPartnerDto created = mapper.map(legalPartner);

        LegalPartnerDto updated = legalPartnerService.updateInsensitive(legalPartner.getPartnerId(),
                buildInsensitiveLegalPartnerDto().build());

        assertThat(mapper.map(updated)).isEqualTo(mapper.map(created));
    }

    @Test
    @SneakyThrows
    void testSendOfferDataToCrm() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build());
        preLegalPartnerFactory.bindSecurityTicket(preLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(preLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(preLegalPartner.getId());

        legalPartnerOfferCommandService.saveOffer(legalPartner.getPartnerId(), FILE_NAME, FILE_DATA);
        assertThat(legalPartnerOfferQueryService.tryGetOffer(legalPartner.getId())).isPresent();


        legalPartnerService.checkOffer(legalPartner.getPartnerId());

        List<CrmPayloadDto<?>> events = queuedLogbrokerEventManager.getSome().stream()
                .map(this::parseCrmPayloadDto)
                .collect(Collectors.toList());

        PreLegalPartnerCrmDto lastEvent = events.stream()
                .filter(e -> e.getType() == CrmPayloadType.PRE_LEGAL_PARTNER)
                .reduce((first, last) -> last)
                .map(CrmPayloadDto::getValue)
                .map(v -> OBJECT_MAPPER.convertValue(v, PreLegalPartnerCrmDto.class))
                .orElse(null);

        assertThat(lastEvent.getOfferFile()).isNotNull();
        assertThat(lastEvent.getOfferFile()).isEqualTo(PreLegalPartnerCrmDto.FileData.builder()
                .url(sf(OFFER_URL, legalPartner.getPartnerId()))
                .fileName(FILE_NAME)
                .fileSize((long) FILE_DATA.length)
                .contentType(CONTENT_TYPE)
                .build());
    }

    @SneakyThrows
    private CrmPayloadDto<?> parseCrmPayloadDto(QueuedLogbrokerEventParams e) {
        return OBJECT_MAPPER.readValue(e.getPayload(), CrmPayloadDto.class);
    }

    @Test
    void testFilterOrdersByPaymentType() {
        assertThat(getOrders(OrderPaymentType.CARD).getContent()).hasSize(2);
        assertThat(getOrders(OrderPaymentType.CASH).getContent()).hasSize(1);
    }

    private Page<OrderReportDto> getOrders(OrderPaymentType paymentType) {
        return legalPartnerService.getOrders(
                requestData,
                OrderReportParamsDto.builder().resultPaymentType(paymentType.name()).build(),
                Pageable.unpaged());
    }

    @Test
    void testFilterOrdersByArrived() {
        LocalDate date = LocalDate.now(clock);
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .arrivedDateFrom(date)
                .arrivedDateTo(date)
                .resultPaymentType("CARD")
                .build();
        Page<OrderReportDto> orders = legalPartnerService.getOrders(requestData, paramsDto, Pageable.unpaged());
        assertThat(orders.getContent()).hasSize(0);

        testOrderFactory.receiveOrder(cardPaidOrder.getId());
        testOrderFactory.receiveOrder(fashionOrder.getId());

        orders = legalPartnerService.getOrders(requestData, paramsDto, Pageable.unpaged());
        assertThat(orders.getContent()).hasSize(2);

        orders = legalPartnerService.getOrders(requestData, OrderReportParamsDto.builder()
                .arrivedDateFrom(date.plusDays(1))
                .arrivedDateTo(date.plusDays(1))
                .resultPaymentType("CARD")
                .build(), Pageable.unpaged());
        assertThat(orders.getContent()).hasSize(0);
    }

    @Test
    void filterByDelivered() {
        LocalDate date = LocalDate.now(clock);
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .arrivedDateFrom(date)
                .arrivedDateTo(date)
                .deliveredDateFrom(date)
                .deliveredDateTo(date)
                .build();
        Page<OrderReportDto> orders = legalPartnerService.getOrders(requestData, paramsDto, Pageable.unpaged());
        int size = orders.getContent().size();

        testOrderFactory.receiveOrder(cardPaidOrder.getId());
        testOrderFactory.deliverOrder(cardPaidOrder.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        orders = legalPartnerService.getOrders(requestData, paramsDto, Pageable.unpaged());
        assertThat(orders.getContent()).hasSize(size + 1);
    }

    @Test
    void filterByOnDemand() {
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .types("ON_DEMAND")
                .build();
        Page<OrderReportDto> orders = legalPartnerService.getOrders(requestData, paramsDto, Pageable.unpaged());

        assertThat(orders.getContent()).hasSize(1);
        var order = orders.getContent().get(0);
        assertThat(order.getType()).isEqualTo(OrderType.ON_DEMAND);
        assertThat(order.getExternalId()).isEqualTo(onDemandOrder.getExternalId());
    }

    @Test
    void testFilterByRecipientPhoneAndBuyerUid() {
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .commonQuery(RECIPIENT_PHONE)
                .buyerYandexUid(String.valueOf(BUYER_YANDEX_UID))
                .build();
        Page<OrderReportDto> orders = legalPartnerService.getOrders(requestData, paramsDto, Pageable.unpaged());

        assertThat(orders.getContent()).hasSize(1);
        Recipient recipient = orders.getContent().get(0).getRecipient();
        assertThat(recipient.getPhone()).isEqualTo("");
        assertThat(recipient.getBuyerYandexUid()).isEqualTo(String.valueOf(BUYER_YANDEX_UID));
    }

    @Test
    void getOrdersWithNoPickupPoints() {
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder().build();
        var legalPartner = legalPartnerFactory.createLegalPartner();
        var requestData = new LegalPartnerRequestData(
                legalPartner.getId(), legalPartner.getPartnerId(), legalPartner.getOrganization().getFullName(), 1L);
        Page<OrderReportDto> orders = legalPartnerService.getOrders(requestData, paramsDto, Pageable.unpaged());

        assertThat(orders.getContent()).hasSize(0);
    }
}
