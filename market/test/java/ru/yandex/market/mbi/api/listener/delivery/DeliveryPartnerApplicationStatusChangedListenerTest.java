package ru.yandex.market.mbi.api.listener.delivery;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.balance.BalanceConstants;
import ru.yandex.market.common.balance.xmlrpc.model.OfferStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.application.meta.PartnerApplication;
import ru.yandex.market.core.application.meta.PartnerApplicationService;
import ru.yandex.market.core.balance.BalanceFirm;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.FullClientInfoMatcher;
import ru.yandex.market.core.balance.model.FullClientInfo;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.feature.delivery.DeliveryPartnerApplicationStatusChangedListener;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.id.GetOrCreateMarketIdRequest;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.id.UpdateLegalInfoRequest;
import ru.yandex.market.id.UpdateLegalInfoResponse;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.enums.TaxSystem;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.balance.BalanceService.ROBOT_MBI_BALANCE_UID;

/**
 * Интеграционный тест для {@link DeliveryPartnerApplicationStatusChangedListener}.
 */
class DeliveryPartnerApplicationStatusChangedListenerTest extends FunctionalTest {

    private static final long ACTION_ID = 555L;
    private static final long REQUEST_ID = 1L;
    private static final long CLIENT_ID = 77L;
    private static final long PERSON_ID = 1L;
    private static final long OFFER_ID = 11L;
    private static final long MARKET_ID = 2000000L;
    private static final int DELIVERY_SUCCESSFUL_VALIDATION_NOTIFICATION_TYPE = 1575899103;
    private static final int DELIVERY_SALES_SUCCESSFUL_VALIDATION_NOTIFICATION_TYPE = 1581398794;
    private static final int DELIVERY_NEED_INFO_VALIDATION_NOTIFICATION_TYPE = 1576236886;
    private static final int DELIVERY_SALES_NEED_INFO_VALIDATION_NOTIFICATION_TYPE = 1581398792;
    private static final int DELIVERY_DECLINED_VALIDATION_NOTIFICATION_TYPE = 1576572062;
    private static final int DELIVERY_SALES_DECLINED_VALIDATION_NOTIFICATION_TYPE = 1581398793;


    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;
    @Autowired
    private PartnerApplicationService partnerApplicationService;
    @Autowired
    private NesuClient nesuClient;
    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;
    @Autowired
    private NotificationService notificationService;

    private static void assertContract(OfferStructure offer) {
        assertThat(offer, hasEntry("firm_id", BalanceFirm.YANDEX_MARKET_RUSSIA.getFirmId()));
        assertThat(offer, hasEntry("client_id", (int) CLIENT_ID));
        assertThat(offer, hasEntry("person_id", (int) PERSON_ID));
        assertThat(offer, hasEntry("manager_uid", String.valueOf(BalanceConstants.DEFAULT_DAAS_MANAGER_UID)));
        assertThat(offer, hasEntry("personal_account", 1));
        assertThat(offer, hasEntry("payment_type", BalanceConstants.POSTPAY_PAYMENT_TYPE));
        assertThat(offer, hasEntry("payment_term", BalanceConstants.DEFAULT_PAYMENT_TERM_DAYS));
        assertThat(offer, hasEntry("currency", "RUR"));
        assertThat(offer, hasEntry("services", Arrays.asList(
                BalanceConstants.DAAS_BALANCE_SERVICE,
                BalanceConstants.DAAS_SERVICES_BALANCE_SERVICE
        )));
        assertThat(offer, hasEntry("bank_details_id", BalanceConstants.DAAS_BANK_DETAILS_ID));
        assertThat(offer, hasEntry("minimal_payment_commission", BalanceConstants.DAAS_MINIMAL_PAYMENT_COMMISSION));
    }

    private static void assertPerson(
            PersonStructure person,
            String orgName,
            String orgNameLong,
            String personPosition,
            String docType
    ) {
        assertThat(person, not(hasKey("ID")));
        assertThat(person, not(hasKey("PERSON_ID")));
        assertThat(person, hasEntry("ACCOUNT", "88888888888888888888"));
        assertThat(person, hasEntry("AUTHORITY-DOC-TYPE", docType));
        assertThat(person, hasEntry("BIK", "333333333"));
        assertThat(person, hasEntry("CLIENT_ID", String.valueOf(CLIENT_ID)));
        assertThat(person, hasEntry("EMAIL", "vasiliev@yandex.ru"));
        assertThat(person, hasEntry("INN", "7736207543"));
        assertThat(person, hasEntry("IS_PARTNER", "0"));
        assertThat(person, hasEntry("KPP", "770401001"));
        assertThat(person, hasEntry("LEGALADDRESS", "Москва, ул. Ленина, д.17, кв.35"));
        assertThat(person, hasEntry("LONGNAME", orgNameLong));
        assertThat(person, hasEntry("NAME", orgName));
        assertThat(person, hasEntry("PHONE", "+79130000000"));
        assertThat(person, hasEntry("POSTADDRESS", "Москва, ул. Кирова, д.23, кв.37"));
        assertThat(person, hasEntry("POSTCODE", "123456"));
        assertThat(person, hasEntry("REPRESENTATIVE", "Васильев Василий Васильевич"));
        assertThat(person, hasEntry("SIGNER-PERSON-GENDER", "M"));
        assertThat(person, hasEntry("SIGNER-PERSON-NAME", "Петров Петр Петрович"));
        assertThat(person, hasEntry("SIGNER-POSITION-NAME", personPosition));
        assertThat(person, hasEntry("TYPE", "ur"));
    }

    private static void assertShopNotification(NotificationSendContext notification, int typeId, List expectedData) {
        assertThat(notification.getShopId(), equalTo(1001L));
        assertNotification(notification, typeId, expectedData);
    }

    private static void assertNotification(NotificationSendContext notification, int typeId, List expectedData) {
        assertThat(notification.getTypeId(), equalTo(typeId));
        ReflectionAssert.assertReflectionEquals(expectedData, notification.getData());
    }

    @Test
    @DbUnitDataSet(
            before = "on-application-submit.before.csv",
            after = "on-application-completed.after.csv"
    )
    void onCompletedSubmit() {
        testSubmit(
                "delivery",
                "ООО delivery",
                "Директора",
                "Устав",
                true,
                1000L
        );
    }

    @Test
    @DbUnitDataSet(
            before = "on-application-submit-with-market-id.before.csv",
            after = "on-application-completed.after.csv"
    )
    void onCompletedSubmitWithLinkMarketId() {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(MARKET_ID).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        })
                .when(marketIdServiceImplBase).linkMarketIdRequest(any(), any());

        doAnswer(invocation -> {
            StreamObserver<UpdateLegalInfoResponse> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(UpdateLegalInfoResponse.newBuilder().setMessage("ok").build());
            marketAccountStreamObserver.onCompleted();
            return null;
        })
                .when(marketIdServiceImplBase)
                .updateLegalInfo(
                        refEq(
                                UpdateLegalInfoRequest.newBuilder()
                                        .setMarketId(MARKET_ID)
                                        .setTimestamp(Timestamp.newBuilder().setSeconds(1529341200L).build())
                                        .setLegalInfo(LegalInfo.newBuilder()
                                                .setLegalName("delivery")
                                                .setType("OOO")
                                                .setRegistrationNumber("1027700229193")
                                                .setLegalAddress("Москва, ул. Ленина, д.17, кв.35")
                                                .setPhysicalAddress("Москва, ул. Кирова, д.23, кв.37")
                                                .setInn("7736207543")
                                                .build())
                                        .build()
                        ),
                        any()
                );

        testSubmit(
                "delivery",
                "ООО delivery",
                "Директора",
                "Устав",
                false,
                1000L
        );
    }

    @Test
    @DbUnitDataSet(
            before = "on-application-completed-manual-org-fields.before.csv",
            after = "on-application-completed-manual-org-fields.after.csv"
    )
    void onCompletedSubmitWithManualOrgFields() {
        testSubmit(
                "delivery",
                "MyOOO delivery",
                "Странного сотрудника",
                "Клятва кровью",
                true,
                1000L
        );
    }

    @Test
    @DbUnitDataSet(
            before = "on-application-submit.before.csv",
            after = "on-application-need-info.after.csv"
    )
    void onNeedInfoSubmit() {
        partnerApplicationService.changeStatus(
                ACTION_ID,
                REQUEST_ID,
                PartnerApplicationStatus.NEED_INFO,
                "В связи с неактивным статусом предпринимателя."
        );

        ArgumentCaptor<NotificationSendContext> notificationCaptor =
                ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService, times(2)).send(notificationCaptor.capture());

        List<NotificationSendContext> notifications = notificationCaptor.getAllValues();

        assertThat(notifications, Matchers.hasSize(2));

        CampaignInfo expectedCampaignInfo = new CampaignInfo();
        expectedCampaignInfo.setId(11001L);
        PartnerApplication expectedPartnerApplication = new PartnerApplication(
                1001L,
                PartnerApplicationStatus.NEED_INFO,
                "В связи с неактивным статусом предпринимателя."
        );

        assertShopNotification(
                notifications.get(0),
                DELIVERY_NEED_INFO_VALIDATION_NOTIFICATION_TYPE,
                List.of(expectedCampaignInfo, expectedPartnerApplication)
        );

        assertNotification(
                notifications.get(1),
                DELIVERY_SALES_NEED_INFO_VALIDATION_NOTIFICATION_TYPE,
                List.of(
                        getExpectedDatasourceInfo(),
                        new NamedContainer("domain", "site.ru"),
                        new NamedContainer("comment", "В связи с неактивным статусом предпринимателя.")
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "on-application-submit.before.csv",
            after = "on-application-declined.after.csv"
    )
    void onDeclinedSubmit() {
        partnerApplicationService.changeStatus(
                ACTION_ID,
                REQUEST_ID,
                PartnerApplicationStatus.DECLINED,
                "В связи с неактивным статусом предпринимателя."
        );

        ArgumentCaptor<NotificationSendContext> notificationCaptor =
                ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService, times(2)).send(notificationCaptor.capture());

        List<NotificationSendContext> notifications = notificationCaptor.getAllValues();

        assertThat(notifications, Matchers.hasSize(2));

        assertShopNotification(
                notifications.get(0),
                DELIVERY_DECLINED_VALIDATION_NOTIFICATION_TYPE,
                List.of()
        );

        assertNotification(
                notifications.get(1),
                DELIVERY_SALES_DECLINED_VALIDATION_NOTIFICATION_TYPE,
                List.of(
                        getExpectedDatasourceInfo(),
                        new NamedContainer("domain", "site.ru"),
                        new NamedContainer("comment", "В связи с неактивным статусом предпринимателя.")
                )
        );
    }

    private void testSubmit(
            String orgName,
            String orgNameLong,
            String personPosition,
            String docType,
            boolean marketIdCreation,
            Long businessId
    ) {
        when(balanceService.createClient(any(FullClientInfo.class), eq(ROBOT_MBI_BALANCE_UID), eq(ACTION_ID)))
                .thenReturn(CLIENT_ID);
        when(balanceService.createOrUpdatePerson(any(PersonStructure.class), eq(ROBOT_MBI_BALANCE_UID)))
                .thenReturn(PERSON_ID);
        when(balanceService.createOffer(any(OfferStructure.class), eq(ROBOT_MBI_BALANCE_UID)))
                .thenReturn(OFFER_ID);
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(MARKET_ID).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(MARKET_ID).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());

        partnerApplicationService.changeStatus(
                ACTION_ID, REQUEST_ID,
                PartnerApplicationStatus.COMPLETED,
                "Проверено"
        );
        if (marketIdCreation) {
            ArgumentCaptor<GetOrCreateMarketIdRequest> requestCaptor =
                    ArgumentCaptor.forClass(GetOrCreateMarketIdRequest.class);
            verify(marketIdServiceImplBase).getOrCreateMarketId(requestCaptor.capture(), ArgumentMatchers.any());
        }
        verify(nesuClient).registerShop(eq(
                RegisterShopDto.builder()
                        .id(1001L)
                        .businessId(businessId)
                        .marketId(MARKET_ID)
                        .balanceClientId(CLIENT_ID)
                        .balanceContractId(OFFER_ID)
                        .balancePersonId(PERSON_ID)
                        .name("delivery")
                        .role(ShopRole.DAAS)
                        .taxSystem(TaxSystem.USN_INCOME_OUTCOME)
                        .siteUrl("site.ru")
                        .build()
        ));

        // проверяем создание клиента
        ArgumentCaptor<FullClientInfo> fullClientInfoCaptor = ArgumentCaptor.forClass(FullClientInfo.class);
        verify(balanceService).createClient(fullClientInfoCaptor.capture(), eq(ROBOT_MBI_BALANCE_UID), anyLong());
        assertThat(fullClientInfoCaptor.getValue(), new FullClientInfoMatcher(orgNameLong, "+79999999999", null));

        // проверяем создание плательщика
        ArgumentCaptor<PersonStructure> personCaptor = ArgumentCaptor.forClass(PersonStructure.class);
        verify(balanceService).createOrUpdatePerson(personCaptor.capture(), eq(ROBOT_MBI_BALANCE_UID));
        assertPerson(personCaptor.getValue(), orgName, orgNameLong, personPosition, docType);

        //проверяем создание договора
        ArgumentCaptor<OfferStructure> contractCaptor = ArgumentCaptor.forClass(OfferStructure.class);
        verify(balanceService).createOffer(contractCaptor.capture(), eq(ROBOT_MBI_BALANCE_UID));
        assertContract(contractCaptor.getValue());

        verifyNoMoreInteractions(balanceService, nesuClient);

        ArgumentCaptor<NotificationSendContext> notificationCaptor =
                ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService, times(2)).send(notificationCaptor.capture());

        List<NotificationSendContext> notifications = notificationCaptor.getAllValues();

        assertThat(notifications, Matchers.hasSize(2));

        CampaignInfo expectedCampaignInfo = new CampaignInfo();
        expectedCampaignInfo.setId(11001L);

        assertShopNotification(
                notifications.get(0),
                DELIVERY_SUCCESSFUL_VALIDATION_NOTIFICATION_TYPE,
                List.of(expectedCampaignInfo)
        );

        assertNotification(
                notifications.get(1),
                DELIVERY_SALES_SUCCESSFUL_VALIDATION_NOTIFICATION_TYPE,
                List.of(
                        getExpectedDatasourceInfo(),
                        new NamedContainer("domain", "site.ru")
                )
        );
    }

    @Nonnull
    private DatasourceInfo getExpectedDatasourceInfo() {
        DatasourceInfo expectedDatasourceInfo = new DatasourceInfo();
        expectedDatasourceInfo.setId(1001);
        expectedDatasourceInfo.setManagerId(-2);
        expectedDatasourceInfo.setInternalName("test delivery shop 1");
        return expectedDatasourceInfo;
    }
}
