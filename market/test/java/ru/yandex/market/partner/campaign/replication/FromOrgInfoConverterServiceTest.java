package ru.yandex.market.partner.campaign.replication;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.abo.api.entity.spark.CompanyExtendedReport;
import ru.yandex.market.abo.api.entity.spark.data.Address;
import ru.yandex.market.abo.api.entity.spark.data.Report;
import ru.yandex.market.abo.api.entity.spark.data.ReportInfo;
import ru.yandex.market.abo.api.entity.spark.data.ResponseSparkStatus;
import ru.yandex.market.api.cpa.checkout.AsyncCheckouterService;
import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestForm;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestValidatorService;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.abo._public.AboPublicService;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.BankInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.contact.db.BusinessOwnerService;
import ru.yandex.market.core.contact.db.DbContactService;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.orginfo.dao.OrganizationInfoDAO;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.partner.PartnerLinkService;
import ru.yandex.market.core.supplier.contact.model.ReturnContact;
import ru.yandex.market.core.supplier.contact.model.ReturnContactType;
import ru.yandex.market.core.supplier.contact.service.ReturnContactService;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.partner.campaign.replication.model.ReplicationOrgInfoModel;
import ru.yandex.market.partner.mvc.controller.business.model.ContactInfo;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Проверяем конвертирование в {@link FromOrgInfoConverterService}.
 */
@DbUnitDataSet(before = "FromOrgInfoConverterServiceTest.before.csv")
class FromOrgInfoConverterServiceTest extends FunctionalTest {
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    private MarketIdGrpcService marketIdGrpcService;
    @Autowired
    private BusinessOwnerService businessOwnerService;
    @Autowired
    private AgencyService agencyService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private OrganizationInfoDAO organizationInfoDAO;
    @Autowired
    private PrepayRequestService prepayRequestService;
    @Autowired
    private PrepayRequestValidatorService prepayRequestValidatorService;
    @Autowired
    private ReturnContactService returnContactService;
    @Autowired
    private AboPublicService aboPublicService;
    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private DbContactService contactService;

    @Autowired
    private FromOrgInfoConverterService fromOrgInfoConverterService;

    @Autowired
    private OrgInfoReplicationService orgInfoReplicationService;

    @Autowired
    private AsyncCheckouterService asyncCheckouterService;

    @Autowired
    private PartnerLinkService partnerLinkService;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    private static PersonStructure createPersonStructure(long clientId, long personId, String postCode, String type) {
        PersonStructure personStructure = new PersonStructure();
        personStructure.setClientId(clientId);
        personStructure.setPersonId(personId);
        personStructure.setBik("044525225");
        personStructure.setAccount("40702810340000003354");
        personStructure.setEmail("email@ya.ru");
        personStructure.setKpp("111111111");
        personStructure.setInn("7710757670");
        personStructure.setPostAddress("где-то на белом свете");
        personStructure.setPostCode(postCode);
        personStructure.setPhone("123-123-123");
        personStructure.setType(type);
        personStructure.setSignerPersonName("Petr First");
        return personStructure;
    }

    private static Stream<Arguments> checkConverterData() {
        ContactInfo contactInfo1 = new ContactInfo();
        contactInfo1.setFirstName("Ilya");
        contactInfo1.setLastName("Muromets");

        ContactInfo contactInfo2 = new ContactInfo();
        contactInfo2.setFirstName("Dobrynya");
        contactInfo2.setLastName("Nikitich");

        ContactInfo contactInfo3 = new ContactInfo();
        contactInfo3.setFirstName("Alesha");
        contactInfo3.setLastName("Popovich");

        ContactInfo contactInfo4 = new ContactInfo();
        contactInfo4.setFirstName("Ivan");
        contactInfo4.setLastName("Ivanov");
        return Stream.of(
                Arguments.of(1983L, 1001983L, 71L, 5001L, "319774600350303", OrganizationType.IP,
                        "JURIDICAL ADDRESS", "ur", "90-90-90", contactInfo1, false, "437730231589", "437730231589",
                        PartnerApplicationStatus.COMPLETED),
                Arguments.of(1983L, 1001983L, 71L, 5001L, "319774600350303", OrganizationType.IP,
                        "JURIDICAL ADDRESS", "ur", "90-90-90", contactInfo1, false, "7710757670", null,
                        PartnerApplicationStatus.NEW),
                Arguments.of(1983L, 1001983L, 71L, 5001L, "319774600350303", OrganizationType.IP,
                        "JURIDICAL ADDRESS", "ph", "90-90-90", contactInfo1, false, "771075767012", null,
                        PartnerApplicationStatus.NEW),
                Arguments.of(5451L, 1005451L, 72L, 5002L, "1147746422086", OrganizationType.OOO,
                        "г. Москва, Вязовский 1-й пр-д, д. 4, корп. 1", "ur", "90-90-90", contactInfo2, false,
                        "7710757670", "7710757670", PartnerApplicationStatus.COMPLETED),
                Arguments.of(132986L, 100132986L, 73L, 5003L, "1035005521320",
                        OrganizationType.OOO,
                        "141006, Московская область, г. Мытищи, Волковское шоссе, владение 15, строение 1, офис 603",
                        "ur", "+7 0000000000", contactInfo3, false, "7710757670", "7710757670",
                        PartnerApplicationStatus.COMPLETED),
                //сабклиент, заявку не переводим в COMPLETED
                Arguments.of(132987L, 100132987L, 74L, 5004L, "1999999783",
                        OrganizationType.OOO, "Юридический адрес", "ur", "+7 0000000000", contactInfo4, true,
                        "7710757670", "7710757670",
                        PartnerApplicationStatus.NEW)
        );
    }

    private static Stream<Arguments> choosePersonStructureArgs() {
        //физик
        PersonStructure psPh = new PersonStructure()
                .setPersonId(100L)
                .setType("ph")
                .setDT("2021-03-09 12:21:45")
                .setPostAddress("far far away")
                .setAccount("1234567890");
        //юрики
        PersonStructure psUrWithoutAddress = new PersonStructure()
                .setPersonId(99L)
                .setType("ur")
                .setDT("2021-03-09 12:21:45")
                .setAccount("1234567890");

        PersonStructure psUrWithoutAddressAndAccount = new PersonStructure()
                .setPersonId(98L)
                .setType("ur")
                .setDT("2021-03-09 12:21:45");

        PersonStructure psUrWithoutAccount = new PersonStructure()
                .setPersonId(97L)
                .setType("ur")
                .setDT("2021-03-09 12:21:45")
                .setPostAddress("far far away");

        PersonStructure psUrNorm1 = new PersonStructure()
                .setPersonId(96L)
                .setType("ur")
                .setDT("2021-03-09 08:21:45")
                .setPostAddress("far far away")
                .setAccount("1234567890");

        PersonStructure psUrNorm2 = new PersonStructure()
                .setPersonId(95L)
                .setType("ur")
                .setDT("2021-03-09 10:21:45")
                .setPostAddress("far far away")
                .setAccount("1234567890");

        PersonStructure psUrNorm3 = new PersonStructure()
                .setPersonId(97L)
                .setType("ur")
                .setDT("2021-03-09 10:21:45")
                .setPostAddress("far far away")
                .setAccount("1234567890");

        PersonStructure psUrNorm4 = new PersonStructure()
                .setPersonId(94L)
                .setType("ur")
                .setDT("2021-03-11 20:21:45")
                .setPostAddress("far far away")
                .setAccount("1234567890");


        return Stream.of(
                Arguments.of("Поиск самого последнего по времени",
                        List.of(psPh, psUrNorm1, psUrNorm2, psUrWithoutAccount, psUrWithoutAddressAndAccount,
                                psUrWithoutAddress), psUrNorm2),
                Arguments.of("Поиск плательщика единственного полностью заполненного",
                        List.of(psPh, psUrNorm1, psUrWithoutAccount, psUrWithoutAddressAndAccount,
                                psUrWithoutAddress), psUrNorm1),
                Arguments.of("Один плательщик с адресом",
                        List.of(psPh, psUrWithoutAccount, psUrWithoutAddressAndAccount,
                                psUrWithoutAddress), psUrWithoutAccount),
                Arguments.of("Нет подходящего плательщик, все незаполнены",
                        List.of(psPh, psUrWithoutAddressAndAccount,
                                psUrWithoutAddress), null),
                Arguments.of("По ид среди заполненных по дате и ид",
                        List.of(psUrNorm1, psUrNorm2, psUrNorm3), psUrNorm3),
                Arguments.of("По времени среди заполненных по дате и ид",
                        List.of(psUrNorm1, psUrNorm2, psUrNorm3, psUrNorm4), psUrNorm4)

        );
    }

    @BeforeEach
    void setup() {
        asyncCheckouterService = mock(AsyncCheckouterService.class);
        fromOrgInfoConverterService = new FromOrgInfoConverterService(marketIdGrpcService,
                contactService, businessOwnerService, balanceService, organizationInfoDAO,
                returnContactService, aboPublicService, agencyService);

        orgInfoReplicationService = new OrgInfoReplicationService(prepayRequestService,
                fromOrgInfoConverterService, asyncCheckouterService);

        when(balanceService.getBankByBik(eq("044525225")))
                .thenReturn(new BankInfo("Банк", "044525225", "Moscow", "30101810400000000225"));

        willReturn(Optional.of(MarketAccount.newBuilder()
                .setMarketId(12345L)
                .build()
        )).given(marketIdGrpcService).findByPartner(anyLong(), eq(CampaignType.SHOP));

        willReturn(12345L).given(marketIdGrpcService).linkOrCreateMarketId(anyLong(), anyLong(), anyLong(), anyLong());

        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(12345L).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).linkMarketIdRequest(any(), any());

    }

    private void preparePersons(long clientId, String personType) {
        when(balanceService.getClientPersons(eq(clientId), eq(PersonStructure.TYPE_GENERAL)))
                .thenReturn(List.of(
                        createPersonStructure(clientId, 1001, "100001", personType),
                        createPersonStructure(clientId, 1002, "100002", personType),
                        createPersonStructure(clientId, 1003, "100003", personType)
                ));
    }

    @SuppressWarnings("ParameterNumber")
    @ParameterizedTest(name = "[{index}] {1}, {2}, {3}")
    @MethodSource("checkConverterData")
    void checkConverter(
            long donorPartnerId,
            long replicatedPartnerId,
            long clientId,
            long uid,
            String ogrn,
            OrganizationType type,
            String jurAddress,
            String personType,
            String shopPhone,
            ContactInfo contactInfo,
            boolean isSubclient,
            String inn,
            String expectedInn,
            PartnerApplicationStatus expectedAppStatus
    ) {
        preparePersons(clientId, personType);
        boolean isUrPersonType = "ur".equals(personType);

        String expectedKpp = type != OrganizationType.IP ? "111111111" : null;
        mockSparkResponse(ogrn, jurAddress, inn, expectedKpp);

        ReplicationOrgInfoModel replicationOrgInfoModel =
                fromOrgInfoConverterService.convert(donorPartnerId, replicatedPartnerId, clientId, shopPhone, 11L);


        assertThat(replicationOrgInfoModel.getApplicationTargetStatus()).isEqualTo(expectedAppStatus);
        assertThat(replicationOrgInfoModel.getMarketId()).isEqualTo(12345L);
        assertThat(replicationOrgInfoModel.getSuperAdminUid()).isEqualTo(uid);

        PrepayRequestForm prepayRequestForm = replicationOrgInfoModel.getPrepayRequestForm();
        assertThat(prepayRequestForm.getDatasourceIds()).isEqualTo(List.of(replicatedPartnerId));

        OrganizationInfoDTO organizationInfo = prepayRequestForm.getOrganizationInfo();
        assertThat(organizationInfo.getType()).isEqualTo(type);
        assertThat(organizationInfo.getOgrn()).isEqualTo(ogrn);

        assertThat(organizationInfo.getInn()).isEqualTo(expectedInn);

        assertThat(organizationInfo.getKpp()).isEqualTo(expectedKpp);
        String expectedJurAddress = isUrPersonType ? jurAddress : "";
        assertThat(organizationInfo.getJuridicalAddress()).isEqualTo(expectedJurAddress);
        assertThat(organizationInfo.getFactAddress()).isEqualTo(isUrPersonType && !isSubclient ? "где-то на белом " +
                "свете" : null);
        String expectedAccNumber = isUrPersonType && !isSubclient ? "40702810340000003354" : null;
        assertThat(organizationInfo.getAccountNumber()).isEqualTo(expectedAccNumber);
        String expectedBik = isUrPersonType && !isSubclient ? "044525225" : null;
        assertThat(organizationInfo.getBik()).isEqualTo(expectedBik);
        assertThat(organizationInfo.getBankName()).isEqualTo(isUrPersonType && !isSubclient ? "Банк" : null);
        assertThat(organizationInfo.getCorrAccountNumber()).isEqualTo(isUrPersonType && !isSubclient ?
                "30101810400000000225" : null);
        assertThat(organizationInfo.getPostcode()).isEqualTo(isUrPersonType && !isSubclient ? "100003" : null);


        assertThat(prepayRequestForm.getContactInfo().getFirstName()).isEqualTo(contactInfo.getFirstName());
        assertThat(prepayRequestForm.getContactInfo().getLastName()).isEqualTo(contactInfo.getLastName());
        assertThat(prepayRequestForm.getContactInfo().getPhoneNumber()).isEqualTo("123-45-67");
        assertThat(prepayRequestForm.getContactInfo().getEmail()).isEqualTo("iemail@yandex.ru");


        long prepayRequest = transactionTemplate.execute(status -> prepayRequestService.createPrepayRequest(
                prepayRequestForm,
                RequestType.MARKETPLACE,
                123
        ));
        List<PrepayRequest> requests = prepayRequestService.getRequests(prepayRequest, replicatedPartnerId);
        assertThat(requests.size()).isEqualTo(1);
        PrepayRequest request = requests.get(0);

        assertThat(request.getOrganizationType()).isEqualTo(type);
        assertThat(request.getOgrn()).isEqualTo(ogrn);
        assertThat(request.getInn()).isEqualTo(expectedInn);
        assertThat(request.getKpp()).isEqualTo(expectedKpp);
        assertThat(request.getAccountNumber()).isEqualTo(expectedAccNumber);
        assertThat(request.getJurAddress())
                .isEqualTo(StringUtils.isEmpty(expectedJurAddress) ? null : expectedJurAddress);
        assertThat(request.getBik()).isEqualTo(expectedBik);

        assertThat(request.getStatus()).isEqualTo(PartnerApplicationStatus.NEW);

        List<ReturnContact> returnContacts = returnContactService.getReturnContacts(replicatedPartnerId);
        // check phone
        assertThat(returnContacts.stream()
                .filter(c -> c.getType() == ReturnContactType.PERSON)
                .map(ReturnContact::getPhoneNumber)
                .findFirst()
        ).contains(Objects.requireNonNullElse(shopPhone, "+7 0000000000"));
    }

    private void mockSparkResponse(String ogrn, String jurAddress, String inn, String expectedKpp) {
        Report report = new Report();
        report.setInn(inn);
        report.setKpp(expectedKpp);
        Address address = new Address();
        address.setAddress(jurAddress);
        report.setAddress(address);
        ReportInfo reportInfo = new ReportInfo(ResponseSparkStatus.OK);
        CompanyExtendedReport sparkReport = new CompanyExtendedReport(report, reportInfo);
        doReturn(sparkReport).when(aboPublicService).getOgrnInfo(eq(ogrn), anyLong());
    }

    @Test
    @DbUnitDataSet(after = "FromOrgInfoConverterServiceTest.after.csv")
    void checkPipeline() throws MalformedURLException {
        long childPartnerId = 555L;
        long donorPartnerId = 5451L;
        Report report = new Report();
        report.setInn("7710757670");
        report.setKpp("111111111");
        prepareToCheckPipeline(childPartnerId, donorPartnerId, report);
        transactionTemplate.execute(status -> {
            orgInfoReplicationService.copyOrgInfoAndCreateRequest(donorPartnerId, childPartnerId, 72L, "1-2-3", 111L);
            return null;
        });
    }

    @Test
    void checkPipelineWithProblemsFromSpark() throws MalformedURLException {
        long childPartnerId = 555L;
        long donorPartnerId = 5451L;
        prepareToCheckPipeline(childPartnerId, donorPartnerId, null);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> orgInfoReplicationService.copyOrgInfoAndCreateRequest(
                        donorPartnerId,
                        childPartnerId,
                        72L,
                        "1-2-3",
                        111L
                ));
    }

    private void prepareToCheckPipeline(long childPartnerId, long donorPartnerId, Report report) throws MalformedURLException {
        doReturn(new URL("http://mds.yandex.net/partner-application-documents/1/1/partner-application-doc-1"))
                .when(mdsS3Client).getUrl(any());
        when(asyncCheckouterService.pushPartnerSettingsToCheckout(eq(Set.of(childPartnerId)))).thenReturn(true);

        partnerLinkService.insertLink(donorPartnerId, childPartnerId, null);
        preparePersons(72L, "ur");
        ReportInfo reportInfo = new ReportInfo(ResponseSparkStatus.OK);

        CompanyExtendedReport sparkReport = new CompanyExtendedReport(report, reportInfo);
        doReturn(sparkReport).when(aboPublicService).getOgrnInfo(anyString(), anyLong());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("choosePersonStructureArgs")
    void checkChoosePersonStructure(
            String description,
            List<PersonStructure> persons,
            PersonStructure expected
    ) {
        PersonStructure actualPersonStructure = fromOrgInfoConverterService.choosePersonStructure(persons);
        assertThat(actualPersonStructure).isEqualTo(expected);
    }

    @Test
    void testGetLegalAddress() {
        PersonStructure person = new PersonStructure();
        person.setLegalAddress("Москва");

        Report orgInfoFromSpark = new Report();
        orgInfoFromSpark.setOgrn("123");
        assertThat(fromOrgInfoConverterService.getLegalAddress(orgInfoFromSpark, person)).isEqualTo("Москва");

        verifyNoInteractions(aboPublicService);
    }

    @Test
    @DbUnitDataSet(after = "OrgInfoReplicationServiceTestFailOnInit.after.csv")
    void failedRequestProcessingOnInit() throws MalformedURLException {
        doThrow(new RuntimeException())
                .when(prepayRequestValidatorService)
                .checkStatusChangeAllowed(any(), eq(PartnerApplicationStatus.INIT));
        long childPartnerId = 555L;
        long donorPartnerId = 5451L;
        Report report = new Report();
        report.setInn("7710757670");
        report.setKpp("111111111");
        prepareToCheckPipeline(childPartnerId, donorPartnerId, report);
        transactionTemplate.execute(status -> {
            orgInfoReplicationService.copyOrgInfoAndCreateRequest(donorPartnerId, childPartnerId, 72L, "1-2-3", 111L);
            return null;
        });
    }
}
