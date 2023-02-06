package ru.yandex.market.admin.service.remote;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.magic.passport.model.PassportInfo;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.err.ClientIdNotMatchForBusinessException;
import ru.yandex.market.admin.ui.model.business.UIBusiness;
import ru.yandex.market.admin.ui.model.business.UIBusinessSearch;
import ru.yandex.market.admin.ui.model.business.UIBusinessService;
import ru.yandex.market.admin.ui.model.client.UIContactPassport;
import ru.yandex.market.admin.ui.model.client.UIPassport;
import ru.yandex.market.admin.ui.service.PassportUIService;
import ru.yandex.market.admin.ui.service.SortOrder;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.mbo.PartnerChangeDao;
import ru.yandex.market.core.mbo.model.PartnerChangeRecord;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.state.event.BusinessChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClientException;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstancesResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link RemoteBusinessUIService}.
 */
@DbUnitDataSet(before = "RemoteBusinessUIServiceTest.before.csv")
class RemoteBusinessUIServiceTest extends FunctionalTest {

    private static final UIBusiness BUSINESS_1 = business(100, "Business1", 1000, true);
    private static final UIBusiness BUSINESS_2 = business(101, "Business2", 2000, false);

    private static final UIBusinessService BUSINESS_SERVICE_1 = businessService(10, "SHOP_SERVICE_1", "SUPPLIER", -1);

    private static final UIContactPassport CONTACT_PASSPORT_1 = contactPassport(1004, "username1",
            "email1@yandex.ru", "login1", false);
    private static final UIContactPassport CONTACT_PASSPORT_2 = contactPassport(1006, "username2",
            "email2@yandex.ru", "login2", true);
    private static final UIContactPassport CONTACT_PASSPORT_3 = contactPassport(1003, "username3",
            "email3@yandex.ru", "login3", true);

    @Autowired
    BalanceContactService balanceContactService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private RemoteBusinessUIService businessUIService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private PassportUIService passportUIService;
    @Autowired
    private PartnerChangeDao partnerChangeDao;
    @Autowired
    private MbiBpmnClient mbiBpmnClient;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    @Qualifier("logbrokerBusinessChangesEventPublisher")
    private LogbrokerEventPublisher<BusinessChangesProtoLBEvent> logbrokerBusinessChangesEventPublisher;
    @Autowired
    private ReportsService<ReportsType> reportsService;
    @Autowired
    private PassportService passportService;

    private static UIBusiness business(long id, String name, long campaignId, boolean haveServices) {
        var business = new UIBusiness();
        business.setField(UIBusiness.ID, id);
        business.setField(UIBusiness.NAME, name);
        business.setField(UIBusiness.CAMPAIGN_ID, campaignId);
        business.setField(UIBusiness.HAS_ACTIVE_SERVICES, haveServices);
        business.setField(UIBusiness.ACTIVE, true);
        return business;
    }

    private static UIContactPassport contactPassport(long id, String name, String email,
                                                     String login, boolean isOwner) {
        var contactPassport = new UIContactPassport();
        contactPassport.setField(UIPassport.ID, id);
        contactPassport.setField(UIPassport.NAME, name);
        contactPassport.setField(UIPassport.EMAIL, email);
        contactPassport.setField(UIPassport.LOGIN, login);
        contactPassport.setField(UIContactPassport.BUSINESS_OWNER, isOwner);
        return contactPassport;
    }

    private static UIBusinessService businessService(long serviceId, String name, String type, long managerId) {
        var businessService = new UIBusinessService();
        businessService.setField(UIBusinessService.ID, serviceId);
        businessService.setField(UIBusinessService.NAME, name);
        businessService.setField(UIBusinessService.TYPE, type);
        businessService.setField(UIBusinessService.MANAGER_ID, managerId);
        return businessService;
    }

    private static UIBusinessSearch businessSearch(long id, String name, long campaignId) {
        var business = new UIBusinessSearch();
        business.setField(UIBusinessSearch.ID, id);
        business.setField(UIBusinessSearch.NAME, name);
        business.setField(UIBusinessSearch.CAMPAIGN_ID, campaignId);
        return business;
    }

    private static Stream<Arguments> argsNotChangeOwner() {
        return Stream.of(
                Arguments.of("неизвестный бизнес", 1000L, 1003L, 100L),
                Arguments.of("пользователь с другим клиентом", 121L, 102L, 301211L)
        );
    }

    private static Stream<Arguments> failOnDeleteBusinessPartnerArgs() {
        return Stream.of(
                Arguments.of(100, 10, "Client id of removed partner must be 0"),
                Arguments.of(101, 10, "Wrong business id"),
                Arguments.of(102, 41, "Cannot find campaign for partnerId 41"),
                Arguments.of(102, 40, "Partner 40 is alive and has no cutoffs for longer than 30 days")
        );
    }

    @BeforeEach
    void initMock() {
        var response = new ProcessStartResponse();
        var processInstance = new ProcessStartInstance();
        processInstance.setBusinessKey("12345678");
        processInstance.setStatus(ProcessStatus.ACTIVE);
        processInstance.setProcessInstanceId("m1");
        response.setRecords(List.of(processInstance));
        doReturn(response)
                .when(mbiBpmnClient).postProcess(any());

        when(logbrokerBusinessChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    void getBusiness() {
        assertThat(businessUIService.getBusiness(100L)).isEqualTo(BUSINESS_1);
        assertThat(businessUIService.getBusiness(101L)).isEqualTo(BUSINESS_2);
    }

    @Test
    void getBusinessByServiceId() {
        assertThat(businessUIService.getBusinessByServiceId(10L)).isEqualTo(BUSINESS_1);
        assertThat(businessUIService.getBusinessByServiceId(20L)).isNull();
    }

    @Test
    void searchBusinessServices() {
        var actual = businessUIService.searchBusinessServices("SERVICE_1", 0, 10);
        assertThat(actual)
                .hasSize(2)
                .element(0).isEqualTo(BUSINESS_SERVICE_1);
    }

    @Test
    void getServices() {
        var actual = businessUIService.getServices(100L);
        assertThat(actual)
                .hasSize(2)
                .element(0).isEqualTo(BUSINESS_SERVICE_1);
        assertThat(businessUIService.getServices(101L)).isEmpty();
    }

    @Test
    void getContacts() {
        var passportInfo = new PassportInfo(1004L, "username1", "nickname1", "login1");
        passportInfo.setExtendedPassportInfo(Map.of("email", "email1@yandex.ru"));
        when(passportUIService.getPassportInfo(1004L))
                .thenReturn(passportInfo);
        passportInfo = new PassportInfo(1006L, "username2", "nickname2", "login2");
        passportInfo.setExtendedPassportInfo(Map.of("email", "email2@yandex.ru"));
        when(passportUIService.getPassportInfo(1006L))
                .thenReturn(passportInfo);

        var actual = businessUIService.getContacts(101L);
        assertThat(actual).containsExactlyInAnyOrder(
                CONTACT_PASSPORT_1,
                CONTACT_PASSPORT_2
        );
    }

    @Test
    @DbUnitDataSet(after = "RemoteBusinessUIServiceTest.saveServiceLink.after.csv")
    void saveServiceLink() {
        businessUIService.saveServiceLink(100L, 30L);
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(100L);
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.UPDATE);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteBusinessUIServiceTest.saveServiceLinkContactAlreadyLinked.before.csv",
            after = "RemoteBusinessUIServiceTest.saveServiceLinkContactAlreadyLinked.after.csv")
    void saveServiceLinkContactAlreadyLinked() {
        businessUIService.saveServiceLink(100L, 30L);
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(100L);
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.UPDATE);
    }

    @Test
    @DisplayName("Бпмн. Уже есть незавершённая операция.")
    @DbUnitDataSet(
            before = "RemoteBusinessUIServiceTest.saveServiceLinkBpmnAlreadyInProcessing.before.csv",
            after = "RemoteBusinessUIServiceTest.saveServiceLinkBpmnAlreadyInProcessing.after.csv")
    void saveServiceLinkBpmnAlreadyInProcessing() {
        businessUIService.saveServiceLink(100L, 30L);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DisplayName("Бпмн. Белый пуш в екате. Мигрируем.")
    @DbUnitDataSet(
            before = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmnWhiteUCPush.before.csv",
            after = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmnWhiteUCPush.after.csv")
    void bindServiceToNewBusinessBpmnWhiteUCPush() {
        businessUIService.bindServiceToNewBusiness(40L, null, "SHOP");
        verify(mbiBpmnClient).postProcess(any());
        ArgumentCaptor<Set<Long>> partnersCaptor = ArgumentCaptor.forClass(Set.class);
        verify(partnerChangeDao)
                .getEventForPartner(partnersCaptor.capture(), eq(PartnerChangeRecord.UpdateType.SERVICE_LINK));
        // 1 - созданный бизнес
        assertThat(partnersCaptor.getAllValues().stream().flatMap(Collection::stream))
                .containsOnly(1L);
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(1L);
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.CREATE);
    }

    @Test
    @DisplayName("Бпмн. Белый пуш в екате. Ошибка BPMN.")
    @DbUnitDataSet(
            before = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmnWhiteUCPush.before.csv",
            after = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmnWhiteUCPush.fail.after.csv")
    void bindServiceToNewBusinessBpmnWhiteUCPushFail() {
        var response = new ProcessStartResponse();
        var processInstance = new ProcessStartInstance();
        processInstance.setBusinessKey("12345678");
        processInstance.setStatus(ProcessStatus.SUSPENDED);
        processInstance.setProcessInstanceId("m1");
        response.setRecords(List.of(processInstance));
        doReturn(response)
                .when(mbiBpmnClient).postProcess(any());
        Assertions.assertThrows(IllegalStateException.class,
                () -> businessUIService.bindServiceToNewBusiness(40L, null, "SHOP"));
        verify(mbiBpmnClient).postProcess(any());

        doThrow(new MbiBpmnClientException("Bad request", 400, null))
                .when(mbiBpmnClient).postProcess(any());
        Assertions.assertThrows(IllegalStateException.class,
                () -> businessUIService.bindServiceToNewBusiness(40L, null, "SHOP"));
        verify(mbiBpmnClient, times(2)).postProcess(any());

        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(4)).publishEventAsync(businessEventsCaptor.capture());
        List<BusinessChangesProtoLBEvent> value = businessEventsCaptor.getAllValues();
        AssertionsForClassTypes.assertThat(value.get(0).getPayload().getBusinessId()).isEqualTo(1L);
        AssertionsForClassTypes.assertThat(value.get(0).getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.CREATE);

        AssertionsForClassTypes.assertThat(value.get(1).getPayload().getBusinessId()).isEqualTo(1L);
        AssertionsForClassTypes.assertThat(value.get(1).getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.DELETE);

        AssertionsForClassTypes.assertThat(value.get(2).getPayload().getBusinessId()).isEqualTo(2L);
        AssertionsForClassTypes.assertThat(value.get(2).getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.CREATE);

        AssertionsForClassTypes.assertThat(value.get(3).getPayload().getBusinessId()).isEqualTo(2L);
        AssertionsForClassTypes.assertThat(value.get(3).getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.DELETE);

        assertThat(businessService.getBusiness(1L).isDeleted()).isTrue();
        assertThat(businessService.getBusiness(2L).isDeleted()).isTrue();
        assertThat(businessService.getBusiness(100L).isDeleted()).isFalse();
        assertThat(businessService.getBusiness(101L).isDeleted()).isFalse();
        assertThat(businessService.getBusiness(104L).isDeleted()).isFalse();
    }

    @Test
    void saveServiceLinkClientIdException() {
        assertThatExceptionOfType(ClientIdNotMatchForBusinessException.class)
                .isThrownBy(() -> businessUIService.saveServiceLink(100L, 20L));
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteBusinessUIServiceTest.saveServiceLinkAgency.before.csv",
            after = "RemoteBusinessUIServiceTest.saveServiceLinkAgency.after.csv")
    void saveServiceLinkAgency() {
        businessUIService.saveServiceLink(100L, 20L);
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(100L);
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.UPDATE);
    }


    @Test
    @DbUnitDataSet(
            before = "RemoteBusinessUIServiceTest.saveServiceLinkForBusinessWithSubclient.before.csv",
            after = "RemoteBusinessUIServiceTest.saveServiceLinkAgency.after.csv")
    void saveServiceLinkForBusinessWithSubclient() {
        businessUIService.saveServiceLink(100L, 20L);
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(100L);
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.UPDATE);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmn.before.csv",
            after = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmn.after.csv")
    void bindServiceToNewBusinessBpmn() {
        businessUIService.bindServiceToNewBusiness(10L, null, "SUPPLIER");
        verify(mbiBpmnClient).postProcess(any());
        ArgumentCaptor<Set<Long>> partnersCaptor = ArgumentCaptor.forClass(Set.class);
        verify(partnerChangeDao)
                .getEventForPartner(partnersCaptor.capture(), eq(PartnerChangeRecord.UpdateType.SERVICE_LINK));
        // 1 - созданный бизнес
        assertThat(partnersCaptor.getAllValues().stream().flatMap(Collection::stream))
                .containsOnly(1L);
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(1L);
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.CREATE);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmnAlreadyLocked.before.csv",
            after = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmnAlreadyLocked.after.csv")
    void bindServiceToNewBusinessBpmnAlreadyLockedOldBusiness() {
        var response = new ProcessInstancesResponse();
        var processInstance = new ProcessInstance();
        processInstance.setBusinessKey("12345678");
        processInstance.setStatus(ProcessStatus.ACTIVE);
        processInstance.setProcessInstanceId("m1");
        response.setRecords(List.of(processInstance));
        businessUIService.bindServiceToNewBusiness(10L, null, "SUPPLIER");
        verifyNoMoreInteractions(mbiBpmnClient);
        verifyNoMoreInteractions(partnerChangeDao);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmnAlreadyLocked.before.csv",
            after = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessBpmnAlreadyLocked.after.csv")
    void bindServiceToNewBusinessBpmnAlreadyLocked() {
        var response = new ProcessInstancesResponse();
        var processInstance = new ProcessInstance();
        processInstance.setBusinessKey("12345678");
        processInstance.setStatus(ProcessStatus.ACTIVE);
        processInstance.setProcessInstanceId("m1");
        response.setRecords(List.of(processInstance));
        businessUIService.saveServiceLink(101L, 10L);
        verifyNoMoreInteractions(mbiBpmnClient);
        verifyNoMoreInteractions(partnerChangeDao);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DisplayName("Не Екат. Старый синий. Но бизнес залочен. Нельзя")
    @DbUnitDataSet(
            before = "RemoteBusinessUIServiceTest.bindServiceToExistingBusinessOldBlueLockedBusiness.before.csv",
            after = "RemoteBusinessUIServiceTest.bindServiceToExistingBusinessOldBlueLockedBusiness.after.csv"
    )
    void bindServiceToExistingBusinessOldBlueLockedBusiness() {
        businessUIService.saveServiceLink(202L, 201L);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.saveServiceLinkAgency.before.csv",
            after = "RemoteBusinessUIServiceTest.bindServiceToNewBusinessAgency.after.csv")
    void bindServiceToNewBusinessAgency() {
        businessUIService.bindServiceToNewBusiness(20L, null, "SHOP");
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(2)).publishEventAsync(businessEventsCaptor.capture());
        List<BusinessChangesProtoLBEvent> value = businessEventsCaptor.getAllValues();
        AssertionsForClassTypes.assertThat(value.get(0).getPayload().getBusinessId()).isEqualTo(1L);
        AssertionsForClassTypes.assertThat(value.get(0).getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.CREATE);

        AssertionsForClassTypes.assertThat(value.get(1).getPayload().getBusinessId()).isEqualTo(1L);
        AssertionsForClassTypes.assertThat(value.get(1).getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.UPDATE);
    }

    @Test
    @DbUnitDataSet(after = "RemoteBusinessUIServiceTest.bindServiceToPullPartner.after.csv")
    void bindServiceToPullPartner() {
        businessUIService.bindServiceToNewBusiness(20L, null, "SHOP");
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(2)).publishEventAsync(businessEventsCaptor.capture());
        List<BusinessChangesProtoLBEvent> value = businessEventsCaptor.getAllValues();
        AssertionsForClassTypes.assertThat(value.get(0).getPayload().getBusinessId()).isEqualTo(1L);
        AssertionsForClassTypes.assertThat(value.get(0).getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.CREATE);

        AssertionsForClassTypes.assertThat(value.get(1).getPayload().getBusinessId()).isEqualTo(1L);
        AssertionsForClassTypes.assertThat(value.get(1).getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.UPDATE);
    }

    @Test
    @DbUnitDataSet(after = "RemoteBusinessUIServiceTest.removeBusinessWithLinks.after.csv")
    void removeBusinessWithServiceLinks() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> businessUIService.removeBusiness(100));
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DbUnitDataSet(after = "RemoteBusinessUIServiceTest.removeBusiness.after.csv")
    void removeBusiness() {
        businessUIService.removeBusiness(101);
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(101L);
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.DELETE);
    }

    /**
     * Проверяем, что не можем пометить бизнес с активной услугой удаленным.
     */
    @Test
    void tryDeleteActiveBusiness() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> businessUIService.markBusinessDeleted(100, true))
                .withMessage("Business 100 cannot be marked as deleted. It has active services");
    }

    @Test
    void searchBusinesses() {
        var businesses = businessUIService.searchBusinesses("bus", UIBusinessSearch.ID,
                SortOrder.DESC, 0, 1);
        assertThat(businesses)
                .hasSize(1)
                .element(0).isEqualTo(businessSearch(101, "Business2", 2000));
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    /**
     * Проверяет назначение роли владельца для админа бизнеса. Проверяем, что у линка 20 роль поменялась с 6 на 5
     */
    @Test
    @DbUnitDataSet(after = "RemoteBusinessUIServiceTest.changeAdminToOwner.after.csv")
    void changeAdminToOwnerTest() {
        //существующий пользователь - кампания
        businessUIService.changeOwnerToBusiness(101, 1004L);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    /**
     * Проверяет назначение бизнес овнером неимпортированного контакта
     */
    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.changeOwnerNewUserTest.before.csv",
            after = "RemoteBusinessUIServiceTest.changeOwnerNewUserTest.after.csv")
    void changeOwnerNewUserTest() {
        when(passportService.getUserInfo(eq(662L)))
                .thenReturn(new UserInfo(662L, "Ivan", "ivan200@yandex.ru", "ivan200"));
        when(balanceService.getClients(anyCollection()))
                .thenReturn(Map.of(325076L, new ClientInfo(325076, ClientType.OOO)));
        businessUIService.changeOwnerToBusiness(101, 662L);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @ParameterizedTest
    @MethodSource("argsNotChangeOwner")
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.getBusinessByOwner.before.csv",
            after = "RemoteBusinessUIServiceTest.notChangeBusinessOwner.after.csv")
    void notChangeBusinessOwnerWithExceptionTest(String description, long businessId, long userId, long clientId) {
        when(balanceService.getClientByUid(userId)).thenReturn(new ClientInfo(clientId, ClientType.OOO, false, 0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> businessUIService.changeOwnerToBusiness(businessId, userId));
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.getBusinessByOwner.before.csv",
            after = "RemoteBusinessUIServiceTest.getBusinessByOwner.after.csv")
    void changeOwnerToBusinessTest() {
        var userId = 1001L;
        var clientId = 201211L;
        when(balanceContactService.getClientIdByUid(userId)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.OOO, false, 0));
        businessUIService.changeOwnerToBusiness(121L, userId);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.getBusinessByOwner.before.csv",
            after = "RemoteBusinessUIServiceTest.changeOwnerToUidWithBusinessCampaignsTest.after.csv")
    void changeOwnerToUidWithBusinessCampaignsTest() {
        var userId = 103L;
        var clientId = 1000004L;

        when(balanceService.getClientByUid(userId)).thenReturn(new ClientInfo(clientId, ClientType.OOO, false, 0));
        businessUIService.changeOwnerToBusiness(124L, userId);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.getBusinessByOwner.before.csv",
            after = "RemoteBusinessUIServiceTest.changeOwnerToUidWithNoClientsRelatedToBalanceTest.after.csv")
    void changeOwnerToUidWithNoClientsRelatedToBalanceTest() {
        var userId = 104L;
        var clientId = 1000004L;

        when(balanceService.getClientByUid(userId)).thenReturn(new ClientInfo(clientId, ClientType.OOO, false, 0));
        businessUIService.changeOwnerToBusiness(126L, userId);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    /**
     * Проверяет, что удалился контакт старого владельца бизнеса, так как у него не осталось линков.
     */
    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.getBusinessByOwner.before.csv",
            after = {"RemoteBusinessUIServiceTest.addBusinessOwner.after.csv",
                    "RemoteBusinessUIServiceTest.addOwnerToBusiness.after.csv"})
    void addOwnerToBusinessTest() {
        var userId = 1001L;
        var clientId = 201231L;
        when(balanceContactService.getClientIdByUid(userId)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.OOO, false, 0));
        businessUIService.changeOwnerToBusiness(123L, userId);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.getBusinessByOwner.before.csv")
    void getBusinessByOwnerUidTest() {
        //не овнер
        var userId = 1004L;
        var businessByOwnerUid = businessUIService.getBusinessByOwnerUid(userId);
        assertThat(businessByOwnerUid).isEmpty();

        //овнер
        userId = 1006L;
        businessByOwnerUid = businessUIService.getBusinessByOwnerUid(userId);
        assertThat(businessByOwnerUid)
                .hasSize(3)
                .element(0).satisfies(e ->
                        assertThat(e.getLongField(UIBusiness.ID)).isEqualTo(101L)
                );

        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    void isBusinessNameUnique() {
        Assertions.assertFalse(businessUIService.isBusinessNameUnique(2, "BUSINESS2"));
        Assertions.assertFalse(businessUIService.isBusinessNameUnique(2, "business1"));
        Assertions.assertTrue(businessUIService.isBusinessNameUnique(2, "DUFF"));
    }

    @Test
    @DbUnitDataSet(after = "RemoteBusinessUIServiceTest.renameBusiness.after.csv")
    void renameBusiness() {
        businessUIService.renameBusiness(101, "KRUSTY");
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(101L);
        AssertionsForClassTypes.assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.UPDATE);
    }

    @Test
    void getBusinessOwnerByServiceIdTest() {
        var passportInfo = new PassportInfo(1003L, "username3", "nickname3", "login3");
        passportInfo.setExtendedPassportInfo(Map.of("email", "email3@yandex.ru"));
        when(passportUIService.getPassportInfo(1003L))
                .thenReturn(passportInfo);
        UIContactPassport uiContactPassport = businessUIService.getBusinessOwnerByServiceId(10L);
        assertThat(uiContactPassport).isEqualTo(CONTACT_PASSPORT_3);
        verifyNoMoreInteractions(logbrokerBusinessChangesEventPublisher);
    }

    @Test
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.deleteBusinessPartner.before.csv", after =
            "RemoteBusinessUIServiceTest.deleteBusinessPartner.after.csv")
    void deleteBusinessPartner() {
        businessUIService.deleteBusinessPartner(102, 42);
    }

    @ParameterizedTest
    @MethodSource("failOnDeleteBusinessPartnerArgs")
    @DbUnitDataSet(before = "RemoteBusinessUIServiceTest.deleteBusinessPartner.before.csv")
    void failOnDeleteBusinessPartner(long businessId, long partnerId, String message) {
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> businessUIService.deleteBusinessPartner(businessId, partnerId));
        Assertions.assertEquals(message, exception.getMessage());
    }
}
