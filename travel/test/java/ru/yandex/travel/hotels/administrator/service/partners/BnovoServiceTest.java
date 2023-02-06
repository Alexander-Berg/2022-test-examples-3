package ru.yandex.travel.hotels.administrator.service.partners;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.misc.ExceptionUtils;
import ru.yandex.travel.hotels.administrator.configuration.HotelConnectionProperties;
import ru.yandex.travel.hotels.administrator.entity.HotelConnection;
import ru.yandex.travel.hotels.administrator.entity.HotelTaxType;
import ru.yandex.travel.hotels.administrator.entity.LegalDetails;
import ru.yandex.travel.hotels.administrator.entity.LegalDetailsUpdate;
import ru.yandex.travel.hotels.administrator.grpc.proto.TAcceptAgreementReq;
import ru.yandex.travel.hotels.administrator.repository.HotelConnectionRepository;
import ru.yandex.travel.hotels.administrator.repository.HotelConnectionUpdateRepository;
import ru.yandex.travel.hotels.administrator.repository.LegalDetailsRepository;
import ru.yandex.travel.hotels.administrator.repository.LegalDetailsUpdateRepository;
import ru.yandex.travel.hotels.administrator.service.AddressUnificationService;
import ru.yandex.travel.hotels.administrator.service.AgreementService;
import ru.yandex.travel.hotels.administrator.service.BillingService;
import ru.yandex.travel.hotels.administrator.service.HotelConnectionService;
import ru.yandex.travel.hotels.administrator.service.StarTrekService;
import ru.yandex.travel.hotels.administrator.service.UpdateResult;
import ru.yandex.travel.hotels.administrator.service.partners.mappers.HotelDetailsMapperImpl;
import ru.yandex.travel.hotels.administrator.workflow.proto.EHotelConnectionState;
import ru.yandex.travel.hotels.administrator.workflow.proto.ELegalDetailsState;
import ru.yandex.travel.hotels.common.partners.bnovo.BNovoClient;
import ru.yandex.travel.hotels.common.partners.bnovo.model.ContactType;
import ru.yandex.travel.hotels.common.partners.bnovo.model.HotelConnectionStatus;
import ru.yandex.travel.hotels.common.partners.bnovo.model.HotelDetails;
import ru.yandex.travel.hotels.common.partners.bnovo.model.HotelDetailsResponse;
import ru.yandex.travel.hotels.common.partners.bnovo.model.TaxType;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.workflow.StateContext;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.testing.TestUtils.genericsFriendlyMock;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
@Import(value = {
        PartnerServiceProvider.class,
        BnovoService.class,
        AgreementService.class,
        HotelDetailsMapperImpl.class,
        HotelConnectionProperties.class
})
@Slf4j
public class BnovoServiceTest {
    private static final String INN = "inn";
    private static final String LEGAL_NAME = "Legal Name";
    private static final String FULL_LEGAL_NAME = "Full Legal Name";
    private static final String KPP = "kpp";
    private static final String BIC = "bic";
    private static final String CURRENT_ACCOUNT = "account";
    private static final String POST_CODE = "111111";
    private static final String ADDRESS = "some address";
    private static final String PHONE = "74959999999";
    private static final String HOTEL_CODE = "hotel_code";
    private static final String EMAIL = "mail@example.com";
    private static final String FACT_ADDRESS = "Address Line";
    private static final String FACT_POST_CODE = "Fact Post Code";
    @Autowired
    private HotelConnectionRepository hotelConnectionRepository;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private LegalDetailsRepository legalDetailsRepository;
    @Autowired
    private LegalDetailsUpdateRepository legalDetailsUpdateRepository;
    @Autowired
    private HotelConnectionUpdateRepository hotelConnectionUpdateRepository;
    @Autowired
    private PartnerServiceProvider partnerServiceProvider;
    @Autowired
    private AgreementService agreementService;
    @Autowired
    private HotelConnectionProperties hotelConnectionProperties;
    @MockBean
    private AddressUnificationService addressUnificationService;
    @MockBean
    private BNovoClient bNovoClient;
    @MockBean
    private StarTrekService starTrekService;
    @MockBean
    private BillingService billingService;

    private LegalDetailsRepository legalDetailsRepositorySpy;
    private HotelConnectionService service;

    @Before
    public void init() {
        // not using spy(obj) to overcome final class issues under "ya make -tt" runs
        legalDetailsRepositorySpy = Mockito.mock(LegalDetailsRepository.class, invocation ->
                forwardMethodCall(invocation, legalDetailsRepository));
        when(billingService.checkRuBankAccount(any(), any())).thenReturn(true);
        service = new HotelConnectionService(
                hotelConnectionRepository, workflowRepository, partnerServiceProvider, legalDetailsRepositorySpy,
                legalDetailsUpdateRepository, hotelConnectionUpdateRepository, addressUnificationService,
                agreementService, starTrekService, billingService, hotelConnectionProperties);
        agreementService.acceptAgreement(TAcceptAgreementReq.newBuilder()
                .setInn(INN)
                .setHotelCode(HOTEL_CODE)
                .setPartnerId(EPartnerId.PI_BNOVO)
                .setContactName("contactName")
                .setContactPhone("contactPhone")
                .build());
    }

    @Test
    public void actualizeHotelConnection_newHotel() {
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(bNovoClient.getHotelDetailsSync(any())).thenReturn(hotelDetailsResponse());
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection(), stateContext);
        Assert.assertEquals(UpdateResult.ResultType.SUCCESS, updateResult.getType());
        Assert.assertEquals(0, legalDetailsUpdateRepository.findAll().size());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        HotelConnection hotelConnection = hotelConnectionList.get(0);
        Assert.assertEquals(EPartnerId.PI_BNOVO, hotelConnection.getPartnerId());
        Assert.assertEquals(HotelTaxType.COMMON_10_VAT, hotelConnection.getTaxType());
        Assert.assertEquals(POST_CODE + ", " + FACT_ADDRESS , hotelConnection.getAddress());
        Assert.assertEquals(EMAIL, hotelConnection.getAccountantEmail());
        Assert.assertEquals("ReservationPhone", hotelConnection.getReservationPhone());
        Assert.assertEquals("Name", hotelConnection.getContractPersonName());
        Assert.assertEquals("Position", hotelConnection.getContractPersonPosition());
        Assert.assertEquals("Email", hotelConnection.getContractPersonEmail());
        Assert.assertEquals("Phone", hotelConnection.getContractPersonPhone());
        Assert.assertEquals("bnovoId", hotelConnection.getExternalHotelId());
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(INN, legalDetails.getInn());
        Assert.assertEquals(KPP, legalDetails.getKpp());
        Assert.assertEquals(BIC, legalDetails.getBic());
        Assert.assertEquals(CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(POST_CODE + ", " + FACT_ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(PHONE, legalDetails.getPhone());
    }

    @Test
    public void actualizeHotelConnection_newHotelWithExistingTravellineDetailsWithMinorChange() {
        LegalDetails existingLegalDetails = existingTravellineLegalDetails();
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = hotelConnection();
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(bNovoClient.getHotelDetailsSync(any())).thenReturn(hotelDetailsResponse());
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.SUCCESS, updateResult.getType());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(INN, legalDetails.getInn());
        Assert.assertEquals(KPP, legalDetails.getKpp());
        Assert.assertEquals(BIC, legalDetails.getBic());
        Assert.assertEquals(CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(POST_CODE + ", " + ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(PHONE, legalDetails.getPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertEquals(1, legalDetailsUpdateList.size());
        LegalDetailsUpdate legalDetailsUpdate = legalDetailsUpdateList.get(0);
        Assert.assertEquals(legalDetails.getId(), legalDetailsUpdate.getLegalDetails().getId());
        Assert.assertEquals(PHONE, legalDetailsUpdate.getPhone());
    }

    private Object forwardMethodCall(InvocationOnMock invocation, Object object) {
        try {
            return invocation.getMethod().invoke(object, invocation.getArguments());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtils.throwException(e.getCause());
        }
    }

    private HotelConnection hotelConnection() {
        HotelConnection connection = new HotelConnection();
        connection.setId(UUID.randomUUID());
        connection.setHotelCode(HOTEL_CODE);
        connection.setPartnerId(EPartnerId.PI_BNOVO);
        connection.setState(EHotelConnectionState.CS_NEW);
        connection.setAccountantEmail(EMAIL);
        return connection;
    }

    private LegalDetails existingTravellineLegalDetails() {
        LegalDetails legalDetails = LegalDetails.builder()
                .id(UUID.randomUUID())
                .inn(INN)
                .kpp(KPP)
                .bic(BIC)
                .paymentAccount(CURRENT_ACCOUNT)
                .legalName(LEGAL_NAME)
                .fullLegalName(FULL_LEGAL_NAME)
                .legalPostCode(POST_CODE)
                .legalAddress(POST_CODE + ", " + ADDRESS)
                .postCode(POST_CODE)
                .postAddress(POST_CODE + ", " + ADDRESS)
                .phone(PHONE)
                .state(ELegalDetailsState.DS_NEW)
                .partnerId(EPartnerId.PI_TRAVELLINE)
                .managedByAdministrator(true)
                .build();
        Workflow workflow = Workflow.createWorkflowForEntity(legalDetails);
        workflowRepository.save(workflow);
        return legalDetails;
    }

    private HotelDetailsResponse hotelDetailsResponse() {
        return HotelDetailsResponse.builder()
                .hotelDetails(HotelDetails.builder()
                        .connectionStatus(HotelConnectionStatus.CONNECTED)
                        .address(HotelDetails.Address.builder()
                                .postalCode(FACT_POST_CODE)
                                .addressLine(POST_CODE + ", " + FACT_ADDRESS)
                                .build())
                        .bankAccountDetails(HotelDetails.BankAccountDetails.builder()
                                .inn(INN)
                                .kpp(KPP)
                                .bic(BIC)
                                .currentAccount(CURRENT_ACCOUNT)
                                .personLegalName(LEGAL_NAME)
                                .branchName(FULL_LEGAL_NAME)
                                .address(HotelDetails.Address.builder()
                                        .postalCode(POST_CODE)
                                        .addressLine(POST_CODE + ", " + ADDRESS)
                                        .build())
                                .phone(PHONE)
                                .tax(TaxType.COMMON_10_VAT)
                                .build())
                        .contactInfo(List.of(
                                HotelDetails.HotelContactInfo.builder()
                                        .contactType(ContactType.ACCOUNTANT)
                                        .email(EMAIL)
                                        .build(),
                                HotelDetails.HotelContactInfo.builder()
                                        .contactType(ContactType.CONTRACT)
                                        .name("Name")
                                        .position("Position")
                                        .phone("Phone")
                                        .email("Email")
                                        .build(),
                                HotelDetails.HotelContactInfo.builder()
                                        .contactType(ContactType.RESERVATION)
                                        .phone("ReservationPhone")
                                        .build()
                        ))
                        .hotelCode("some_code")
                        .bnovoId("bnovoId")
                        .build())
                .build();
    }
}
