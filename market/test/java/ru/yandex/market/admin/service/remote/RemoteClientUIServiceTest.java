package ru.yandex.market.admin.service.remote;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.magic.defender.DefenderDataStore;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.service.AdminBusinessService;
import ru.yandex.market.admin.ui.model.agency.UIOnboardingRewardType;
import ru.yandex.market.admin.ui.model.client.UIClient;
import ru.yandex.market.admin.ui.model.client.UIClientSearchFieldType;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.agency.program.purchase.ArpAgencyPartnerLinkService;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.cache.MemCachedCampaignService;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.data.PartnerDataOuterClass;
import ru.yandex.market.security.SecManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link RemoteClientUIService}.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class RemoteClientUIServiceTest extends FunctionalTest {

    private static final long BIND_CLIENT = 1111;
    private static final long BIND_CLIENT_ANOTHER = 2222;
    private static final long BIND_CLIENT_SUB = 325081;

    @Autowired
    private PassportService passportService;

    @Autowired
    private RemoteClientUIService remoteClientUIService;

    @Autowired
    private MemCachedCampaignService campaignService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private SecManager secManager;

    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private ArpAgencyPartnerLinkService arpAgencyPartnerLinkService;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;


    private DefenderDataStore defenderDataStore;

    @Autowired
    private AdminBusinessService adminBusinessService;
    @Autowired
    @Qualifier("logbrokerPartnerChangesEventPublisher")
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;

    @BeforeEach
    void beforeEach() {
        when(balanceService.getClient(eq(20L))).thenReturn(new ClientInfo(20L, ClientType.PHYSICAL));
        when(balanceService.getClient(eq(210L))).thenReturn(
                new ClientInfo(210L, ClientType.PHYSICAL, false, 10000));
        var response = new ProcessStartResponse();
        var processInstance = new ProcessStartInstance();
        processInstance.setBusinessKey("12345678");
        processInstance.setStatus(ProcessStatus.ACTIVE);
        processInstance.setProcessInstanceId("m1");
        response.setRecords(List.of(processInstance));
        doReturn(response)
                .when(mbiBpmnClient).postProcess(any());
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
        defenderDataStore = Mockito.mock(DefenderDataStore.class);
        remoteClientUIService.setDefenderInfoStore(defenderDataStore);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteClientUIServiceTest.bindNonAgencyClientToOrder.before.csv",
            after = "RemoteClientUIServiceTest.bindNonAgencyClientToOrder.after.csv")
    @DisplayName("Прикрепление заказа баланса к клиенту, не принадлежащему агентству")
    void bindNonAgencyClientToOrder() {
        //мы когда меняем кампанию, то старую закрываем (проставляем ей end_date в campaign_info)
        // И джоба DropClosedCampaignContactsHandler удаляет линки на такие закрытые кампании
        when(passportService.findUid(eq("test-login"))).thenReturn(123L);
        when(balanceContactService.getUidsByClient(eq(20L))).thenReturn(List.of(120L, 121L));
        remoteClientUIService.bindClientToOrder(20, 10L,
                UIClientSearchFieldType.LOGIN, "test-login", null);
        // проверяем вызов миграции в ЕОХ, т.к. у нас записана история отвязки от старого бизнеса в shops_web
        // .business-service_history
        verify(mbiBpmnClient, never()).postProcess(any());

        var captor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        verify(logbrokerPartnerChangesEventPublisher, atLeastOnce()).publishEventAsync(captor.capture());
        assertThat(captor.getValue().getPayload().hasAgency()).isFalse();
    }

    @Test
    @DbUnitDataSet(
            before = {"RemoteClientUIServiceTest.bindNonAgencyClientToOrder.before.csv",
                    "RemoteClientUIServiceTest.bindNonAgencyClientToOrderDeleted.before.csv"})
    @DisplayName("Прикрепление заказа баланса к клиенту, не принадлежащему агентству")
    void bindNonAgencyClientToOrderDeleted() {
        //мы когда меняем кампанию, то старую закрываем (проставляем ей end_date в campaign_info)
        // И джоба DropClosedCampaignContactsHandler удаляет линки на такие закрытые кампании
        when(passportService.findUid(eq("test-login"))).thenReturn(123L);
        when(balanceContactService.getUidsByClient(eq(20L))).thenReturn(List.of(120L, 121L));
        assertThatIllegalArgumentException()
                .as("Can't bind client to partner because partner is deleted")
                .isThrownBy(() -> remoteClientUIService.bindClientToOrder(20, 10L,
                        UIClientSearchFieldType.LOGIN, "test-login", null));
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteClientUIServiceTest.bindNonBusinessClientToOrder.before.csv",
            after = "RemoteClientUIServiceTest.bindNonBusinessClientToOrder.after.csv")
    @DisplayName("Прикрепление заказа баланса к клиенту, не принадлежащему агентству для TPL кампании")
    void bindClientToNonBusinessOrder() {
        //мы когда меняем кампанию, то старую закрываем (проставляем ей end_date в campaign_info)
        // И джоба DropClosedCampaignContactsHandler удаляет линки на такие закрытые кампании
        when(passportService.findUid(eq("test-login"))).thenReturn(123L);
        when(balanceContactService.getUidsByClient(eq(20L))).thenReturn(List.of(120L, 121L));
        remoteClientUIService.bindClientToOrder(20, 10L,
                UIClientSearchFieldType.LOGIN, "test-login", null);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteBusinessUIServiceTest.bindAlreadyExistingClientToOrder.before.csv",
            after = "RemoteBusinessUIServiceTest.bindAlreadyExistingClientToOrder.after.csv")
    @DisplayName("Привязка кампании к существующему бизнесу")
    void checkNotificationWhenBindingServiceToAlreadyExistedBusiness() {
        when(balanceService.getClient(eq(444L))).thenReturn(new ClientInfo(444L, ClientType.PHYSICAL));
        when(passportService.findUid(eq("test-login"))).thenReturn(123L);
        when(balanceContactService.getUidsByClient(eq(444L))).thenReturn(List.of(120L, 121L));
        when(secManager.canDo(any(),any())).thenReturn(Boolean.TRUE);
        remoteClientUIService.bindClientToOrder(444, 10L,
                UIClientSearchFieldType.LOGIN, "test-login", null);
        //не ожидаем взаимодествия с bpmnClient'ом тк бизнес остался прежним
        verifyNoMoreInteractions(mbiBpmnClient);

        verify(partnerNotificationClient, times(2)).sendNotification(any());
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteClientUIServiceTest.bindAgencyClientToOrder.before.csv",
            after = "RemoteClientUIServiceTest.bindAgencyClientToOrder.after.csv")
    @DisplayName("Прикрепление заказа баланса к клиенту агентства")
    void bindAgencyClientToOrder() {
        //мы когда меняем кампанию, то старую закрываем (проставляем ей end_date в campaign_info)
        // И джоба DropClosedCampaignContactsHandler удаляет линки на такие закрытые кампании
        when(passportService.findUid(eq("test-login"))).thenReturn(123L);
        when(balanceContactService.getUidsByClient(eq(210L))).thenReturn(List.of(120L, 121L));
        when(secManager.canDo(any(),any())).thenReturn(Boolean.TRUE);
        remoteClientUIService.bindClientToOrderWithAgencyReward(
                210,
                10L,
                UIClientSearchFieldType.LOGIN,
                "test-login",
                UIOnboardingRewardType.PARTIAL,
                Date.from(Instant.parse("2020-01-01T00:00:00.00Z")),
                null
        );
        //При привязке к субклиенту мы не производим миграцию бизнеса
        verify(mbiBpmnClient, never()).postProcess(any());

        var captor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        verify(logbrokerPartnerChangesEventPublisher, atLeastOnce()).publishEventAsync(captor.capture());
        PartnerDataOuterClass.PartnerData data = captor.getValue().getPayload();
        assertThat(data.hasAgency()).isTrue();
        Assertions.assertEquals(10000L, data.getAgency().getAgencyId());
        Assertions.assertEquals("Агентство 10000", data.getAgency().getName());
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteClientUIServiceTest.bindFromAgencyToSimpleClient.before.csv")
    @DisplayName("Отвязка заказа от агентства и прикрепление к обычному клиенту, " +
            "пользователь привязан к другому клиенту и нужен доступ в баланс")
    void bindFromAgencyToSimpleClientAlreadyLinked() {
        //мы когда меняем кампанию, то старую закрываем (проставляем ей end_date в campaign_info)
        // И джоба DropClosedCampaignContactsHandler удаляет линки на такие закрытые кампании
        when(passportService.findUid(eq("old-super-admin"))).thenReturn(120L);
        when(balanceContactService.getUidsByClient(eq(20L))).thenReturn(List.of(120L, 121L));
        when(balanceContactService.getClientIdByUid(eq(120L))).thenReturn(20L);

        ArpAgencyPartnerLinkService spyArpAgencyPartnerLinkService = spy(arpAgencyPartnerLinkService);
        campaignService.setArpAgencyPartnerLinkService(spyArpAgencyPartnerLinkService);

        when(secManager.canDo(any(),any())).thenReturn(Boolean.TRUE);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> remoteClientUIService.bindClientToOrder(
                20,
                10L,
                UIClientSearchFieldType.LOGIN,
                "old-super-admin",
                null
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteClientUIServiceTest.bindFromAgencyToSimpleClient.before.csv",
            after = "RemoteClientUIServiceTest.bindFromAgencyToSimpleClient.after.csv")
    @DisplayName("Отвязка заказа от агентства и прикрепление к обычному клиенту")
    void bindFromAgencyToSimpleClient() {
        //мы когда меняем кампанию, то старую закрываем (проставляем ей end_date в campaign_info)
        // И джоба DropClosedCampaignContactsHandler удаляет линки на такие закрытые кампании
        when(passportService.findUid(eq("new-owner"))).thenReturn(420L);
        when(passportService.getUserInfo(eq(420L))).thenReturn(
                new UserInfo(420, "new-owner", "new-owner@ya.ru", "new-owner"));
        when(balanceContactService.getUidsByClient(eq(20L))).thenReturn(List.of(120L, 121L));
        when(balanceContactService.getClientIdByUid(eq(120L))).thenReturn(20L);

        ArpAgencyPartnerLinkService spyArpAgencyPartnerLinkService = spy(arpAgencyPartnerLinkService);
        campaignService.setArpAgencyPartnerLinkService(spyArpAgencyPartnerLinkService);
        when(secManager.canDo(any(),any())).thenReturn(Boolean.TRUE);
        remoteClientUIService.bindClientToOrder(
                20,
                10L,
                UIClientSearchFieldType.LOGIN,
                "new-owner",
                null
        );
        //привязка не к субклиенту, поэтому для пуш-партнера производится миграция на бизнес
        verify(spyArpAgencyPartnerLinkService).unlinkPartnerFromCurrentAgency(eq(10L), any());

        var captor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        verify(logbrokerPartnerChangesEventPublisher, atLeastOnce()).publishEventAsync(captor.capture());
        assertThat(captor.getValue().getPayload().hasAgency()).isFalse();
    }

    @Test
    @DbUnitDataSet(before = "RemoteClientUIServiceTest.bindNonAgencyClientToOrder.before.csv")
    @DisplayName("Очистка кэша после привязки клиента к поставщику")
    void cleanCacheAfterBindingSupplierToClient() {
        var spyCampaignService = spy(campaignService);
        remoteClientUIService.setCampaignService(spyCampaignService);
        when(passportService.findUid(eq("test-login"))).thenReturn(123L);
        when(balanceContactService.getUidsByClient(eq(20L))).thenReturn(List.of(120L, 121L));
        assertThat(campaignService.getMarketCampaignsByClient(20)).isEmpty();
        remoteClientUIService.bindClientToOrder(20, 10L,
                UIClientSearchFieldType.LOGIN, "test-login", null);
        assertThat(campaignService.getMarketCampaignsByClient(20)).isNotEmpty();
        verify(spyCampaignService, times(1)).cleanCaches(campaignService.getMarketCampaign(200));
    }

    @Test
    @DisplayName("Не меняем бизнес, если новый клиент сабклиент, или в бизнесе есть сервисы на том же или " +
            "все на нулевых или саб клиентах")
    @DbUnitDataSet(before = {
            "RemoteBusinessUIServiceTest.before.csv",
            "RemoteBusinessUIServiceTest.bindServiceToNewBusinessEkatAlone.before.csv",
            "RemoteBusinessUIServiceTest.bindServiceAllOnSubClients.before.csv",
            "RemoteBusinessUIServiceTest.bindServiceStraightAndSub.before.csv"
    })
    void canBindTest() {
        //2 прямых клиента, меняем на сабклиента
        assertThat(adminBusinessService.needBusinessChange(10L, BIND_CLIENT_SUB)).isFalse();
        //один в бизнесе
        assertThat(adminBusinessService.needBusinessChange(201L, BIND_CLIENT)).isFalse();
        //2 агентских, меняем один на прямой
        assertThat(adminBusinessService.needBusinessChange(401L, BIND_CLIENT)).isFalse();
        //2 агентских, меням один на другой агентский
        assertThat(adminBusinessService.needBusinessChange(401L, BIND_CLIENT_SUB)).isFalse();
        //в бизнесе прямой и агентский, пытаемся привязать к другому магазину такой же прямой клиент
        assertThat(adminBusinessService.needBusinessChange(503L, BIND_CLIENT)).isFalse();
        //в бизнесе прямой и агентский, пытаемся привязать к другому магазину еще один агентский клиент
        assertThat(adminBusinessService.needBusinessChange(503L, BIND_CLIENT_SUB)).isFalse();
    }

    @Test
    @DisplayName("Не меняем бизнес, если новый клиент сабклиент, или в бизнесе все сервисы на том же или нулевых " +
            "или саб клиентах." +
            "Отрицательный сценарий")
    @DbUnitDataSet(before = {
            "RemoteBusinessUIServiceTest.before.csv",
            "RemoteBusinessUIServiceTest.bindServiceToNewBusinessEkatAlone.before.csv",
            "RemoteBusinessUIServiceTest.bindServiceToNewBusinessEkat.before.csv"
    })
    void canBindFalseTest() {
        //2 магазина на предоплате, меняем бизнес
        assertThat(adminBusinessService.needBusinessChange(11L, BIND_CLIENT)).isTrue();
        //1 магазин на постоплате, второй на предоплате - не меняем бизнес
        assertThat(adminBusinessService.needBusinessChange(202L, BIND_CLIENT_ANOTHER)).isFalse();
        //1 магазин на постоплате, второй на предоплате - не меняем бизнес
        assertThat(adminBusinessService.needBusinessChange(201L, BIND_CLIENT_ANOTHER)).isFalse();

    }

    @Test
    @DisplayName("Не разрешаем создать неагентского клиента без логина")
    void createNonAgencyClientWithoutLogin() {
        var client = new UIClient();
        client.setField(UIClient.ID, 12345);
        client.setField(UIClient.AGENCY_ID, null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> remoteClientUIService.createClient(client, null));
    }

    @Test
    @DisplayName("Разрешаем создать агентского клиента без логина")
    void createAgencyClientWithoutLogin() {
        var client = new UIClient();
        client.setField(UIClient.ID, 12345);
        client.setField(UIClient.AGENCY_ID, 11);

        remoteClientUIService.createClient(client, null);

        verify(balanceService, times(1)).createClient(any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Линкуем агентство с uid. Не создаем контакт в базе. Сразу идем в баланс")
    @DbUnitDataSet(
            after = "RemoteClientUIServiceTest.testLinkUidWithAgency.after.csv"
    )
    void testLinkUidWithAgency() {
        Mockito.when(balanceService.getClient(100L))
                .thenReturn(new ClientInfo(100L, ClientType.OOO, true, 100L));
        Mockito.when(passportService.getUserInfo(1000L))
                        .thenReturn(new UserInfo(1000L, "name", "email", "login"));

        remoteClientUIService.bindPassportToClient(100L, 1000L, true);

        Mockito.verify(balanceContactService)
                .linkUid(eq(1000L), eq(100L), anyLong(), anyLong());
    }
}
