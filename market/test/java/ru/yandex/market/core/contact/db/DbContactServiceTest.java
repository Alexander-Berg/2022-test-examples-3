package ru.yandex.market.core.contact.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.mockito.SelfReturningAnswer;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.BalancePassportInfo;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.FullCampaignInfo;
import ru.yandex.market.core.client.remove.RemoveClientEnvironmentService;
import ru.yandex.market.core.client.remove.RemoveClientMigrationService;
import ru.yandex.market.core.contact.CheckContactException;
import ru.yandex.market.core.contact.ContactFilter;
import ru.yandex.market.core.contact.ContactHistoryService;
import ru.yandex.market.core.contact.EmailService;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.contact.event.UpdateContactEvent;
import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactLink;
import ru.yandex.market.core.contact.model.ContactRole;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.error.ErrorInfoException;
import ru.yandex.market.core.error.ErrorInfos;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbContactServiceTest {

    private static final long ACTION_ID = 1123L;
    private static final long ACTOR_ID = 1125L;
    @InjectMocks
    private DbContactService dbContactService;
    @Mock
    private ProtocolService protocolService;
    @Mock
    private ContactDao contactDao;
    @Mock
    private EmailService emailService;
    @Mock
    private CampaignService campaignService;
    @Mock
    private BalanceContactService balanceContactService;
    @Mock
    private PassportService passportService;
    @Mock
    private ContactHistoryService contactHistoryService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private BalanceService balanceService;
    @Mock
    private RemoveClientEnvironmentService removeClientEnvironmentService;
    @Mock
    private RemoveClientMigrationService removeClientMigrationService;

    @BeforeEach
    void setUp() {
        dbContactService.setBalanceService(balanceService);

        ActionInfo actionInfo = new ActionInfo(ACTION_ID, 1124L, ACTOR_ID, 1126, new Date(), "TEST");
        when(protocolService.getActionInfo(eq(ACTION_ID))).thenReturn(actionInfo);
        when(removeClientEnvironmentService.useSkipUpdateInBalanceDbContactService()).thenReturn(true);
    }

    //create
    @Test
    void testCreateBalanceContact() {
        ContactWithEmail contact = prepareContactToCreate();
        when(removeClientMigrationService.getClientsRelatedToBalanceByCampaignIds(any())).thenReturn(Set.of(10L));
        dbContactService.addContact(ACTION_ID, contact);
        verify(contactDao).createContact(contact);
        verify(balanceContactService).linkUid(eq(contact.getUserId()), anyLong(), anyLong(), eq(ACTION_ID));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(contactHistoryService).logCreatedContact(eq(ACTION_ID), any(ContactWithEmail.class));
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getNewContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        assertThat(updateContactEvent.getOldContact(), Matchers.nullValue());
    }

    //create not linking in balance
    @Test
    void testCreateNotLinkInBalanceContact() {
        ContactWithEmail contact = prepareContactToCreate();
        when(removeClientMigrationService.getClientsRelatedToBalanceByCampaignIds(any())).thenReturn(Set.of());
        dbContactService.addContact(ACTION_ID, contact);
        verify(contactDao).createContact(contact);
        verify(balanceContactService, never()).linkUid(eq(contact.getUserId()), anyLong(), anyLong(), eq(ACTION_ID));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(contactHistoryService).logCreatedContact(eq(ACTION_ID), any(ContactWithEmail.class));
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getNewContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        assertThat(updateContactEvent.getOldContact(), Matchers.nullValue());
    }

    private ContactWithEmail prepareContactToCreate() {
        ContactWithEmail contact = contact();
        contact.setMarketOnly(false);
        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setId(10);
        campaignInfo.setClientId(10);
        campaignInfo.setType(CampaignType.SHOP);
        contact.setLinks(Set.of(new ContactLink(1, Set.of(InnerRole.SHOP_ADMIN))));
        when(campaignService.getMarketCampaign(anyLong())).thenReturn(campaignInfo);
        when(campaignService.getMarketCampaigns(any())).thenReturn(Map.of(campaignInfo.getId(), campaignInfo));
        return contact;
    }

    @Test
    void testCreateNoLogin() {
        ContactWithEmail contact = contact();
        contact.setLogin(null);
        contact.setMarketOnly(false);
        Assertions.assertThrows(CheckContactException.class,
                () -> dbContactService.addContact(ACTION_ID, contact));
        verifyZeroInteractions(applicationEventPublisher);
    }

    @Test
    void testCreateNotBalanceContact() {
        ContactWithEmail contact = contact();
        contact.setMarketOnly(true);

        dbContactService.addContact(ACTION_ID, contact);
        verify(contactDao).createContact(contact);
        Mockito.verifyNoMoreInteractions(balanceContactService);

        verify(contactHistoryService).logCreatedContact(eq(ACTION_ID), any(ContactWithEmail.class));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getNewContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        assertThat(updateContactEvent.getOldContact(), Matchers.nullValue());
    }

    //delete
    @Test
    void testDeleteBalanceContact() {
        ContactWithEmail contact = contact();
        contact.setMarketOnly(false);
        when(contactDao.getContact(contact.getId())).thenReturn(contact);
        when(balanceContactService.getUidsByClient(anyLong())).thenReturn(Collections.singletonList(contact.getUserId()));
        when(contactDao.hasContact(contact.getId())).thenReturn(true);

        dbContactService.deleteContact(ACTION_ID, contact.getId());
        verify(contactDao).deleteContact(eq(contact.getId()));
        verify(balanceContactService).removeLink(eq(contact.getUserId()), anyLong(), anyLong(), eq(ACTION_ID));

        verify(contactHistoryService).logDeletedContact(eq(ACTION_ID), any(ContactWithEmail.class));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getOldContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        assertThat(updateContactEvent.getNewContact(), Matchers.nullValue());
    }

    @Test
    void testDeleteBalanceContactNotExistsInBalance() {
        ContactWithEmail contact = contact();
        contact.setMarketOnly(false);
        when(contactDao.getContact(contact.getId())).thenReturn(contact);
        when(balanceContactService.getUidsByClient(anyLong())).thenReturn(Collections.emptyList());
        when(contactDao.hasContact(contact.getId())).thenReturn(true);

        dbContactService.deleteContact(ACTION_ID, contact.getId());
        verify(contactDao).deleteContact(eq(contact.getId()));
        verify(balanceContactService, never()).removeLink(eq(contact.getUserId()), anyLong(), anyLong(), eq(ACTION_ID));

        verify(contactHistoryService).logDeletedContact(eq(ACTION_ID), any(ContactWithEmail.class));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getOldContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        assertThat(updateContactEvent.getNewContact(), Matchers.nullValue());
    }


    @Test
    void testDeleteNotBalanceContact() {
        ContactWithEmail contact = contact();
        contact.setMarketOnly(true);
        when(contactDao.getContact(contact.getId())).thenReturn(contact);
        when(contactDao.hasContact(contact.getId())).thenReturn(true);

        dbContactService.deleteContact(ACTION_ID, contact.getId());
        verify(contactDao).deleteContact(eq(contact.getId()));
        verifyNoMoreInteractions(balanceContactService);

        verify(contactHistoryService).logDeletedContact(eq(ACTION_ID), any(ContactWithEmail.class));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getOldContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        assertThat(updateContactEvent.getNewContact(), Matchers.nullValue());
    }

    @Test
    @DisplayName("Для проверки contact already linked используются только линки с ненулевым клиентом")
    void testUpdateMarketOnly() {
        ContactWithEmail contact = contact();
        contact.setMarketOnly(true);

        Set<ContactLink> links = new LinkedHashSet<>();
        ContactLink businessLink = new ContactLink(1, 100, Set.of(new ContactRole(1,
                InnerRole.BUSINESS_ADMIN.getCode())));
        links.add(businessLink);
        ContactLink shopLink = new ContactLink(2, 200, Set.of(new ContactRole(2, InnerRole.SHOP_ADMIN.getCode())));
        links.add(shopLink);
        contact.setLinks(links);

        ContactWithEmail oldContact = contact(links);
        oldContact.setMarketOnly(false);
        when(contactDao.getContact(contact.getId())).thenReturn(oldContact);
        when(contactDao.updateContact(oldContact, contact)).thenReturn(true);

        CampaignInfo businessCampaign = new CampaignInfo(100, 101, 0, 100);
        CampaignInfo shopCampaign = new CampaignInfo(200, 102, 1001, 100);
        when(campaignService.getMarketCampaigns(List.of(100L, 200L)))
                .thenReturn(Map.of(100L, businessCampaign, 200L, shopCampaign));

        BalancePassportInfo balancePassportInfo = BalancePassportInfo.builder()
                .setClientId(1001)
                .setUid(contact.getUserId())
                .setRepresentedClientIds(List.of())
                .build();

        when(balanceContactService.getPassportByUid(eq(contact.getUserId()))).thenReturn(balancePassportInfo);

        dbContactService.updateContactWithEmail(ACTION_ID, contact);

        verify(contactDao).updateContact(refEq(oldContact), refEq(contact));
        verify(removeClientMigrationService, times(1))
                .getClientsRelatedToBalanceByCampaignIds(List.of(100L, 200L));
    }

    //update
    @Test
    void testUpdateBalanceContact() {
        ContactWithEmail contact = contact();
        ContactWithEmail oldContact = contact();
        contact.setMarketOnly(false);
        oldContact.setMarketOnly(true);
        when(contactDao.getContact(contact.getId())).thenReturn(oldContact);
        when(contactDao.updateContact(any(ContactWithEmail.class), any(ContactWithEmail.class))).thenReturn(true);

        CampaignInfo campaignInfo = new CampaignInfo();
        when(campaignService.getMarketCampaign(anyLong())).thenReturn(campaignInfo);

        BalancePassportInfo balancePassportInfo = BalancePassportInfo.builder()
                .setClientId(0)
                .setUid(contact.getUserId())
                .setRepresentedClientIds(List.of())
                .build();

        when(balanceContactService.getPassportByUid(eq(contact.getUserId()))).thenReturn(balancePassportInfo);
        when(removeClientMigrationService.getClientsRelatedToBalanceByCampaignIds(Mockito.anyCollection()))
                .thenReturn(Set.of(1000L));

        dbContactService.updateContactWithEmail(ACTION_ID, contact);
        verify(contactDao).updateContact(refEq(oldContact), refEq(contact));
        verify(balanceContactService).linkUid(eq(contact.getUserId()), anyLong(), anyLong(), eq(ACTION_ID));

        verify(contactHistoryService).logUpdatedContact(eq(ACTION_ID), any(ContactWithEmail.class));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getNewContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        ReflectionAssert.assertReflectionEquals(updateContactEvent.getOldContact(),
                oldContact, ReflectionComparatorMode.LENIENT_ORDER);
    }

    @Test
    void testUpdateBalanceContactWithoutLinkage() {
        ContactWithEmail contact = contact();
        ContactWithEmail oldContact = contact();
        contact.setMarketOnly(false);
        oldContact.setMarketOnly(true);
        when(contactDao.getContact(contact.getId())).thenReturn(oldContact);
        when(contactDao.updateContact(oldContact, contact)).thenReturn(true);

        CampaignInfo campaignInfo = new CampaignInfo();
        when(campaignService.getMarketCampaign(anyLong())).thenReturn(campaignInfo);

       verifyNoInteractions(balanceContactService);
    }

    @Test
    void testUpdateBalanceRepresentedContact() {
        ContactWithEmail contact = contact();
        ContactWithEmail oldContact = contact();
        contact.setMarketOnly(false);
        oldContact.setMarketOnly(true);
        when(contactDao.getContact(contact.getId())).thenReturn(oldContact);
        when(contactDao.updateContact(oldContact, contact)).thenReturn(true);
        when(removeClientMigrationService.getClientsRelatedToBalanceByCampaignIds(Mockito.anyCollection()))
                .thenReturn(Set.of(1000L));
        CampaignInfo campaignInfo = new CampaignInfo();
        when(campaignService.getMarketCampaign(anyLong())).thenReturn(campaignInfo);


        BalancePassportInfo balancePassportInfo = BalancePassportInfo.builder()
                .setClientId(0)
                .setUid(contact.getUserId())
                .setRepresentedClientIds(List.of(5L))
                .build();

        when(balanceContactService.getPassportByUid(eq(contact.getUserId()))).thenReturn(balancePassportInfo);

        ErrorInfoException e = Assertions.assertThrows(ErrorInfoException.class,
                () -> dbContactService.updateContactWithEmail(ACTION_ID, contact));
        assertThat(e.getErrorInfo(), equalTo(ErrorInfos.ALREADY_LINKED_ERROR_INFO));
    }

    @Test
    void testUpdateNotBalanceContact() {
        long clientId = 100;
        ContactWithEmail contact = contact();
        ContactWithEmail oldContact = contact();
        contact.setMarketOnly(true);
        when(contactDao.getContact(contact.getId())).thenReturn(oldContact);
        when(contactDao.updateContact(refEq(oldContact), refEq(contact))).thenReturn(true);
        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setDatasourceId(774L);
        campaignInfo.setClientId(clientId);
        campaignInfo.setType(CampaignType.SHOP);
        when(campaignService.getMarketCampaigns(any())).thenReturn(Map.of(100L, campaignInfo));
        when(removeClientMigrationService.getClientsRelatedToBalanceByCampaignIds(any())).thenReturn(Set.of(clientId));
        when(balanceContactService.getClientIdByUid(eq(1L))).thenReturn(clientId);

        dbContactService.updateContactWithEmail(ACTION_ID, contact);
        verify(contactDao).updateContact(refEq(oldContact), refEq(contact));
        verify(balanceContactService).removeLink(eq(contact.getUserId()), eq(campaignInfo.getClientId()), anyLong(),
                eq(ACTION_ID));

        verify(contactHistoryService).logUpdatedContact(eq(ACTION_ID), any(ContactWithEmail.class));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getNewContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        ReflectionAssert.assertReflectionEquals(updateContactEvent.getOldContact(),
                oldContact, ReflectionComparatorMode.LENIENT_ORDER);
    }

    @Test
    void testUpdateNotBalanceContactWithoutClient() {
        ContactWithEmail contact = contact();
        ContactWithEmail oldContact = contact();
        contact.setMarketOnly(true);
        when(contactDao.getContact(contact.getId())).thenReturn(oldContact);
        when(contactDao.updateContact(refEq(oldContact), refEq(contact))).thenReturn(true);
        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setDatasourceId(774L);
        when(campaignService.getMarketCampaign(anyLong())).thenReturn(campaignInfo);

        dbContactService.updateContactWithEmail(ACTION_ID, contact);
        verify(contactDao).updateContact(refEq(oldContact), refEq(contact));
        verifyZeroInteractions(balanceContactService);

        verify(contactHistoryService).logUpdatedContact(eq(ACTION_ID), any(ContactWithEmail.class));

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getNewContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        ReflectionAssert.assertReflectionEquals(updateContactEvent.getOldContact(),
                oldContact, ReflectionComparatorMode.LENIENT_ORDER);
    }

    @Test
    void testChangeLogin() {
        ContactWithEmail contact = contact();
        ContactWithEmail oldContact = contact();
        oldContact.setLogin("aaa");
        contact.setLogin("bbb");
        when(contactDao.getContact(contact.getId())).thenReturn(oldContact);
        Assertions.assertThrows(IllegalArgumentException.class, () -> dbContactService.updateContactWithEmail(ACTION_ID,
                contact));
        verifyZeroInteractions(applicationEventPublisher);
    }

    @Test
    void testChangeUid() {
        ContactWithEmail contact = contact();
        ContactWithEmail oldContact = contact();
        oldContact.setUserId(123L);
        contact.setUserId(124L);
        when(contactDao.getContact(contact.getId())).thenReturn(oldContact);
        Assertions.assertThrows(IllegalArgumentException.class, () -> dbContactService.updateContactWithEmail(ACTION_ID,
                contact));
        verifyZeroInteractions(applicationEventPublisher);
    }

    @Test
    void testContactNotExists() {
        assertThat(dbContactService.getClientIdByUid(100L), equalTo(0L));
        verify(contactDao).getContactByUid(anyLong());
        verify(balanceContactService).getClientIdByUid(anyLong());
    }

    @Test
    void testContactExists() {
        when(contactDao.getContactByUid(anyLong())).thenReturn(contact());
        when(contactDao.getClientByContactId(anyLong())).thenReturn(5L);
        assertThat(dbContactService.getClientIdByUid(100L), equalTo(5L));
        verify(contactDao).getContactByUid(anyLong());
        verifyZeroInteractions(balanceContactService);
    }

    @Test
    void testAddUserToClient() {
        CampaignInfo ci1 = new CampaignInfo();
        ci1.setId(1001);
        ci1.setClientId(1);
        CampaignInfo ci2 = new CampaignInfo();
        ci2.setId(1002);
        when(passportService.getUserInfo(2))
                .thenReturn(new UserInfo(2, null, null, "v.pupkin"));
        when(balanceContactService.getClientIdByUid(anyLong())).thenReturn(-1L);
        dbContactService.addUidToClient(ACTION_ID, 1, 2);
        verify(contactDao).createContact(any(ContactWithEmail.class));


        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        assertThat(updateContactEvent.getOldContact(), Matchers.nullValue());
        assertThat(updateContactEvent.getNewContact(), Matchers.notNullValue());
    }

    @Test
    void testAddUserToClientAlreadyExistsInBalance() {
        CampaignInfo ci1 = new CampaignInfo();
        ci1.setId(1001);
        ci1.setClientId(1);
        CampaignInfo ci2 = new CampaignInfo();
        ci2.setId(1002);
        when(passportService.getUserInfo(2))
                .thenReturn(new UserInfo(2, null, null, "v.pupkin"));
        when(balanceContactService.getClientIdByUid(anyLong())).thenReturn(1L);
        when(removeClientMigrationService.getClientsRelatedToBalanceByCampaignIds(List.of(1001L, 1002L)))
                .thenReturn(Set.of(1L));
        dbContactService.addUidToClient(ACTION_ID, 1, 2);
        verify(contactDao).createContact(any(ContactWithEmail.class));
        verify(balanceContactService, never()).getClientIdByUid(anyLong());
        verify(balanceContactService, never()).linkUid(anyLong(), anyLong(), anyLong(), anyLong());

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        assertThat(updateContactEvent.getOldContact(), Matchers.nullValue());
        assertThat(updateContactEvent.getNewContact(), Matchers.notNullValue());
    }

    @Test
    void testAddUserToClientExistsUnlinked() {
        long userId = 2;
        long contactId = 3;

        ContactWithEmail contact = new ContactWithEmail();
        contact.setId(contactId);
        contact.setMarketOnly(true);

        when(campaignService.getMarketCampaignsByClient(1L)).thenReturn(Collections.emptyList());

        when(contactDao.getContactByUid(userId)).thenReturn(contact);

        LinkDao linkDao = mock(LinkDao.class);
        dbContactService.setLinkDao(linkDao);

        dbContactService.addUidToClient(ACTION_ID, 1, userId);

        verify(contactDao).getContactByUid(userId);
        verify(linkDao).createLinks(any(Contact.class));
        verify(linkDao).countContactLinks(contactId);
        verifyZeroInteractions(applicationEventPublisher);
    }

    @Test
    void testDeleteLinksByCampaignWhenCampaignNotExists() {
        final long campaignId = 1;
        final long clientId = 10;
        final long actionId = 666;
        prepareMocksForDeleteLinksByCampaign(List.of(campaignId), clientId, true,
                0, Collections.emptyList());

        dbContactService.deleteLinksByCampaign(campaignId, actionId);

        verify(contactDao).deleteLinksByCampaign(campaignId);
        verifyZeroInteractions(applicationEventPublisher);
    }

    @Test
    void testSettingMarketOnlyTrueWhenAnotherContactLinkExists() {
        List<Long> campaignIds = List.of(1L, 2L);
        final long clientId = 10;
        prepareMocksForDeleteLinksByCampaign(campaignIds, clientId, false,
                0, List.of(1L));
        Contact contact = prepareMocksForMarketOnly(true);
        dbContactService.deleteLinksByCampaign(1L, 0);
        verify(contactDao).deleteLinksByCampaign(1L);
        verify(contactDao, never()).setMarketOnly(1L, true);


        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getNewContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        ReflectionAssert.assertReflectionEquals(updateContactEvent.getOldContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
    }

    @Test
    void testSettingMarketOnlyTrueWhenAnotherContactLinkNotExists() {
        List<Long> campaignIds = List.of(1L);
        final long clientId = 10;
        prepareMocksForDeleteLinksByCampaign(campaignIds, clientId, false,
                0, List.of(1L));
        Contact contact = prepareMocksForMarketOnly(false);

        dbContactService.deleteLinksByCampaign(1L, 0);
        verify(contactDao).deleteLinksByCampaign(1L);
        verify(contactDao).setMarketOnly(1L, true);

        ArgumentCaptor<UpdateContactEvent> eventCaptor = ArgumentCaptor.forClass(UpdateContactEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        UpdateContactEvent updateContactEvent = eventCaptor.getValue();

        ReflectionAssert.assertReflectionEquals(updateContactEvent.getNewContact(),
                contact, ReflectionComparatorMode.LENIENT_ORDER);
        Assertions.assertEquals(updateContactEvent.getOldContact().isMarketOnly(), false);
    }

    private ContactWithEmail prepareMocksForMarketOnly(boolean withContactLink) {
        ContactWithEmail mockContact = new ContactWithEmail();
        mockContact.setId(1L);
        if (withContactLink) {
            mockContact.setLinks(Set.of(new ContactLink(1, 2L, Set.of(ContactRole.shopTechnical()))));
        }
        when(contactDao.getContacts(anyCollection())).thenReturn(List.of(mockContact));
        when(contactDao.getContacts(anyLong())).thenReturn(List.of(mockContact));
        return mockContact;
    }

    private void prepareMocksForDeleteLinksByCampaign(final List<Long> campaignIds, final long clientId,
                                                      final boolean setCampaignEnd, int indexOfCampaignForDelete,
                                                      List<Long> contactIds) {
        List<FullCampaignInfo> fullCampaignInfos = new ArrayList<>();
        campaignIds.forEach(campaignId -> {
            final CampaignInfo campaign = new CampaignInfo(campaignId, 0, clientId, 0);
            final FullCampaignInfo fullCampaign = new FullCampaignInfo(0, 0, clientId);
            fullCampaign.setId(campaignId);

            if (setCampaignEnd) {
                fullCampaign.setEndDate(new Date());
            }
            fullCampaignInfos.add(fullCampaign);
            when(campaignService.getAnyCampaign(campaignId)).thenReturn(campaign);
        });
        when(campaignService.getFullCampaignsByClient(clientId)).thenReturn(fullCampaignInfos);
        when(contactDao.deleteLinksByCampaign(campaignIds.get(indexOfCampaignForDelete))).thenReturn(contactIds);
    }

    private HistoryService.Record.Builder mockRecordBuilder() {
        return mock(HistoryService.Record.Builder.class, new SelfReturningAnswer());
    }

    private ContactWithEmail contact() {
        ContactLink contactLink = new ContactLink();
        contactLink.setId(1);
        contactLink.setCampaignId(100L);
        contactLink.setRoles(Collections.singleton(new ContactRole(1, InnerRole.SHOP_ADMIN.getCode())));
        return contact(Set.of(contactLink));
    }

    private ContactWithEmail contact(Set<ContactLink> contactLinks) {
        ContactWithEmail contact = new ContactWithEmail();
        contact.setFirstName("a");
        contact.setLastName("a");
        contact.setPhone("a");
        contact.setPosition("a");

        contact.setLinks(contactLinks);
        contact.setUserId(1L);
        contact.setLogin("aaa");
        return contact;
    }

    @Test
    public void testPartnerContacts() {
        List<Long> partnerIds = Arrays.asList(1L, 2L);
        when(campaignService.getMarketCampaignsByDatasourceIds(partnerIds)).thenReturn(
                Map.of(1L, new CampaignInfo(1L, 1L, 1L, 1L),
                        2L, new CampaignInfo(2L, 2L, 2L, 1L)));
        ContactWithEmail contact1 = createContact(1L, "900", List.of(1L), List.of("email1@test.ru"));
        ContactWithEmail contact2 = createContact(2L, "800", List.of(2L), List.of("email2@test.ru"));
        when(contactDao.getContacts(any(ContactFilter.class))).thenReturn(List.of(contact1, contact2));
        ContactEmail email = new ContactEmail(1, "email@mail", true, true);
        when(emailService.getEmailByContactIds(anyList())).thenReturn(Map.of(1L, Set.of(email), 2L, Set.of(email)));
        Map<Long, List<ContactWithEmail>> contacts = dbContactService.getPartnersContactsWithEmail(partnerIds);
        Map<Long, String> phones = new HashMap<>() {{
            put(1L, "900");
            put(2L, "800");
        }};
        Assert.assertEquals(contacts.size(), 2);
        contacts.keySet().forEach(id -> {
            Assert.assertEquals(contacts.get(id).size(), 1);
            Assert.assertEquals(contacts.get(id).get(0).getId(), id);
            Assert.assertEquals(contacts.get(id).get(0).getPhone(), phones.get(id));
            Assert.assertEquals(contacts.get(id).get(0).getEmails().size(), 1);
        });
    }

    private ContactWithEmail createContact(Long id, String phone, List<Long> campaigns, List<String> emails) {
        ContactWithEmail contact = new ContactWithEmail();
        contact.setId(id);
        contact.setPhone(phone);
        contact.setLinks(campaigns.stream().map(c -> new ContactLink(c, Set.of(InnerRole.SHOP_ADMIN)))
                .collect(Collectors.toSet()));
        return contact;
    }


    @Test
    void checkGetClientAndSubclientInBusiness() {
        CampaignInfo businessCampaign = new CampaignInfo(1L, 1L, 0L, 1L, CampaignType.BUSINESS);
        mockPartnersUnderBusiness(1L, 3L);
        when(campaignService.getMarketCampaigns(any())).thenReturn(Map.of(1L, businessCampaign));
        when(balanceService.getClients(Set.of(1L, 3L))).thenReturn(Map.of(1L, new ClientInfo(1L, ClientType.OOO),
                3L, new ClientInfo(3L, ClientType.OOO, false, 12L)));

        ContactWithEmail contact = createContact(111L, "789", List.of(1L, 2L), List.of("email"));
        Optional<Long> clientId = dbContactService.getClientId(contact);

        assertEquals(1L, clientId.get());
    }

    @Test
    void getContactWithAndWithoutEmails() {
        ContactWithEmail testContact = contact();
        when(contactDao.getContact(testContact.getId())).thenReturn(testContact);
        ContactWithEmail contactWithEmail = dbContactService.getContactWithEmail(testContact.getId());
        Contact contact = dbContactService.getContact(testContact.getId());
        assertEquals(contact.isMarketOnly(), contactWithEmail.isMarketOnly());
        assertEquals(contact.getLinks(), contactWithEmail.getLinks());
        assertEquals(contact.getId(), contactWithEmail.getId());
        assertEquals(contact.hasUserId(), contactWithEmail.hasUserId());
        assertEquals(contact.getUserId(), contactWithEmail.getUserId());
        assertEquals(contact.getLogin(), contactWithEmail.getLogin());
        assertEquals(contact.getPhone(), contactWithEmail.getPhone());
        assertEquals(contact.getFirstName(), contactWithEmail.getFirstName());
        assertEquals(contact.getSecondName(), contactWithEmail.getSecondName());
        assertEquals(contact.getLastName(), contactWithEmail.getLastName());
        assertEquals(contact.getFax(), contactWithEmail.getFax());
        assertEquals(contact.isFromInvite(), contactWithEmail.isFromInvite());
    }

    private void mockPartnersUnderBusiness(long firstClient, long secondClient) {
        long businessCampaignId = 1L;
        Set<Long> partnersUnderBusiness = Set.of(5L, 6L);
        when(campaignService.getPartnerInBusinessByCampaigns(eq(Set.of(businessCampaignId)), eq(null)))
                .thenReturn(Map.of(5L, 1L, 6L, 1L));

        CampaignInfo partner5Campaign = new CampaignInfo(5L, 5L, firstClient, 1L);
        CampaignInfo partner6Campaign = new CampaignInfo(6L, 6L, secondClient, 1L);
        when(campaignService.getMarketCampaignsByDatasourceIds(partnersUnderBusiness))
                .thenReturn(Map.of(5L, partner5Campaign, 6L, partner6Campaign));

        when(removeClientMigrationService.getClientsRelatedToBalanceByCampaignIds(anyCollection()))
                .thenReturn(Set.of(firstClient, secondClient));
    }

}
