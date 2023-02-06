package ru.yandex.market.partner.contact;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerDataOuterClass;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ManageContactServantlet}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = {"ManageContactServantletFunctionalTest.csv",
        "ManageContactServantletFunctionalTest.links.csv"})
public class ManageContactServantletFunctionalTest extends FunctionalTest {
    private static final String LAST_CONTACT_IN_CAMPAIGN = "last-contact-in-campaign";

    @Autowired
    private PassportService passportService;
    @Autowired
    private BalanceContactService balanceContactService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;

    @BeforeEach
    void setUp() {
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any(PartnerChangesProtoLBEvent.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    /**
     * Проверяем, что из результатов ручки отфильтровывается линк к бизнесу.
     */
    @Test
    void testRequest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?_user_id=1&contact_id=10");
        String expectedLinks = "" +
                "<sorted-links>" +
                "<contact-link><campaign-id>102</campaign-id><id>12</id><role-ids><integer>1</integer></role-ids>" +
                "<roles><contact-role><id>12</id><role-id>1</role-id></contact-role></roles>" +
                "<sorted-roles><contact-role><id>12</id><role-id>1</role-id></contact-role></sorted-roles>" +
                "</contact-link>" +
                "<contact-link>" +
                "<campaign-id>103</campaign-id><id>13</id><role-ids><integer>1</integer></role-ids>" +
                "<roles><contact-role><id>13</id><role-id>1</role-id></contact-role></roles>" +
                "<sorted-roles><contact-role><id>13</id><role-id>1</role-id></contact-role></sorted-roles>" +
                "</contact-link>" +
                "<contact-link>" +
                "<campaign-id>104</campaign-id><id>15</id><role-ids><integer>1</integer></role-ids>" +
                "<roles><contact-role><id>14</id><role-id>1</role-id></contact-role></roles>" +
                "<sorted-roles><contact-role><id>14</id><role-id>1</role-id></contact-role></sorted-roles>" +
                "</contact-link>" +
                "</sorted-links>";
        assertThat(response.getBody(), containsString(expectedLinks));
    }

    /**
     * Проверяет обновление контакта:
     * <ol>
     *     <li>Удаляется линк 3, потому что отсутствует в запросе</li>
     *     <li>Остается линк 1 на бизнес, не смотря на то, что отсутствует в запросе</li>
     *     <li>Добавляется email</li>
     * </ol>
     */
    @Test
    @DbUnitDataSet(after = {"ManageContactServantletFunctionalTest.csv",
            "ManageContactServantletFunctionalTest.links.after.csv"})
    void testUpdate() {
        var partnerEventsCaptor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        String queryString = "_user_id=1&a=u&" +
                "contact.id=10&" +
                "contact.userId=1&" +
                "contact.login=pupkin&" +
                "contact.firstName=Василий&" +
                "contact.links.size=2&" +
                "contact.links1.id=12&" +
                "contact.links1.campaignId=102&" +
                "contact.links1.roles.size=1&" +
                "contact.links1.roles1.roleId=1&" +
                "contact.links2.campaignId=104&" +
                "contact.links2.roles.size=1&" +
                "contact.links2.roles1.roleId=1&" +
                "contact.emails.size=1&" +
                "contact.emails1.email=astr0x@yandex.ru&" +
                "contact.emails1.active=1&" +
                "contact.emails1.valid=1";
        FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        verify(logbrokerPartnerChangesEventPublisher, times(1)).publishEventAsync(partnerEventsCaptor.capture());
        List<PartnerChangesProtoLBEvent> partnerChangesToLB = partnerEventsCaptor.getAllValues();
        partnerChangesToLB.stream().map(PartnerChangesProtoLBEvent::getPayload).forEach(payload -> {
                    Assertions.assertEquals(3L, payload.getPartnerId());
                    Assertions.assertEquals(103L, payload.getCampaignId());
                    Assertions.assertEquals(PartnerDataOuterClass.PartnerType.SHOP, payload.getType());
                    Assertions.assertEquals(GeneralData.ActionType.UPDATE, payload.getGeneralInfo().getActionType());
                }
        );
    }

    /**
     * Проверяет создание приглашения с already-in-balance
     * </ol>
     */
    @Test
    void testCreateAlreadyInBalance() {
        Mockito.when(passportService.getUserInfo(Mockito.eq("petrov")))
                .thenReturn(new UserInfo(123, "Петр", "astr0x@yandex.ru", "petrov", false));
        Mockito.when(balanceContactService.getClientIdByUid(Mockito.anyLong())).thenReturn(10023L);
        Mockito.when(balanceService.getClient(Mockito.anyLong())).thenReturn(new ClientInfo(10023, ClientType.OOO));
        Mockito.when(balanceService.getClientByUid(Mockito.anyLong())).thenReturn(new ClientInfo(10023,
                ClientType.OOO));
        String queryString = "_user_id=1&a=c&" +
                "contact.userId=123&" +
                "added_login=petrov&" +
                "contact.firstName=Петр&" +
                "contact.links.size=2&" +
                "contact.links1.id=12&" +
                "contact.links1.campaignId=102&" +
                "contact.links1.roles.size=1&" +
                "contact.links1.roles1.roleId=1&" +
                "contact.links2.campaignId=104&" +
                "contact.links2.roles.size=1&" +
                "contact.links2.roles1.roleId=1&" +
                "contact.emails.size=1&" +
                "contact.emails1.email=astr0x@yandex.ru&" +
                "contact.emails1.active=1&" +
                "contact.emails1.valid=1";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        assertThat(response.getBody(), containsString("already-in-balance"));
    }


    /**
     * Проверяет создание приглашения с already-linked
     * </ol>
     */
    @Test
    void testCreateAlreadyLinked() {
        Mockito.when(passportService.getUserInfo(Mockito.eq("pupkin")))
                .thenReturn(new UserInfo(1, "Василий", "astr0x@yandex.ru", "pupkin", false));
        String queryString = "_user_id=1&a=c&" +
                "added_login=pupkin&" +
                "contact.id=10&" +
                "contact.userId=1&" +
                "contact.login=pupkin&" +
                "contact.firstName=Василий&" +
                "contact.links.size=2&" +
                "contact.links1.id=12&" +
                "contact.links1.campaignId=102&" +
                "contact.links1.roles.size=1&" +
                "contact.links1.roles1.roleId=1&" +
                "contact.links2.campaignId=104&" +
                "contact.links2.roles.size=1&" +
                "contact.links2.roles1.roleId=1&" +
                "contact.emails.size=1&" +
                "contact.emails1.email=astr0x@yandex.ru&" +
                "contact.emails1.active=1&" +
                "contact.emails1.valid=1";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        assertThat(response.getBody(), containsString("already-linked"));
    }

    /**
     * Проверяет редактирование e-mail'ов для главного представителя с пустыми линками.
     * Отправляем запрос без линков, тк фронт фильтрует пустые линки, восстанавлиаем их из базы.
     */
    @Test
    @DbUnitDataSet(after = "ManageContactServantletFunctionalTest.testUpdateSuperAdminEmails.after.csv")
    void testUpdateSuperAdminEmails() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact" + buildQueryString(1));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    @DbUnitDataSet(before = "ManageContactServantletFunctionalTest.lastLinkBusiness.before.csv")
    void testRemoveLastLinkBusiness() {
        String queryString = "_user_id=100&a=u&" +
                "contact.id=1110&" +
                "contact.userId=100&" +
                "contact.login=pupkin&" +
                "contact.firstName=Василий&" +
                "contact.links.size=1&" +
                "contact.links1.id=102&" +
                "contact.links1.campaignId=30001&" +
                "contact.links1.roles.size=1&" +
                "contact.links1.roles1.roleId=1";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        assertThat(response.getBody(), not(containsString(LAST_CONTACT_IN_CAMPAIGN)));
    }

    @Test
    @DbUnitDataSet(before = "ManageContactServantletFunctionalTest.lastLinkBusiness.before.csv")
    void testRemoveLastLinkBusinessService() {
        String queryString = "_user_id=111&a=u&" +
                "contact.id=1111&" +
                "contact.userId=111&" +
                "contact.login=pupkin&" +
                "contact.firstName=Василий&" +
                "contact.links.size=0";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        assertThat(response.getBody(), not(containsString(LAST_CONTACT_IN_CAMPAIGN)));
    }

    @Test
    @DbUnitDataSet(before = "ManageContactServantletFunctionalTest.lastLinkBusiness.before.csv")
    void testRemoveLastLinkNotBusinessService() {
        String queryString = "_user_id=100&a=u&" +
                "contact.id=1110&" +
                "contact.userId=100&" +
                "contact.login=pupkin&" +
                "contact.firstName=Василий&" +
                "contact.links.size=1&" +
                "contact.links1.id=101&" +
                "contact.links1.campaignId=1001&" +
                "contact.links1.roles.size=1&" +
                "contact.links1.roles1.roleId=6";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        assertThat(response.getBody(), containsString(LAST_CONTACT_IN_CAMPAIGN));
    }

    @Test
    @DbUnitDataSet(before = "ManageContactServantletFunctionalTest.lastLinkBusiness.before.csv")
    void testDeleteContactLastLinkBusiness() {
        String queryString = "_user_id=112&a=d&" +
                "contact_id=1112";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        assertThat(response.getBody(), not(containsString(LAST_CONTACT_IN_CAMPAIGN)));
    }

    @Test
    @DbUnitDataSet(before = "ManageContactServantletFunctionalTest.lastLinkBusiness.before.csv")
    void testDeleteContactLinkBusinessService() {
        String queryString = "_user_id=111&a=d&" +
                "contact_id=1111";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        assertThat(response.getBody(), not(containsString(LAST_CONTACT_IN_CAMPAIGN)));
    }

    @Test
    @DbUnitDataSet(before = "ManageContactServantletFunctionalTest.lastLinkBusiness.before.csv")
    void testDeleteContactNotBusinessService() {
        String queryString = "_user_id=100&a=d&" +
                "contact_id=1110";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/manageContact?" + queryString);
        assertThat(response.getBody(), containsString(LAST_CONTACT_IN_CAMPAIGN));
    }

    private static String buildQueryString(long contactId) {
        return UriComponentsBuilder.newInstance()
                .queryParam("a", "u")
                .queryParam("_user_id", 10)

                .queryParam("contact.id", contactId)
                .queryParam("contact.userId", 10)
                .queryParam("contact.login", "contact1")
                .queryParam("contact.marketOnly", false)

                .queryParam("contact.emails1.id", 100)
                .queryParam("contact.emails1.active", 1)
                .queryParam("contact.emails1.valid", 1)
                .queryParam("contact.emails1.email", "contact-1@yandex.ru")

                .queryParam("contact.emails2.active", 1)
                .queryParam("contact.emails2.valid", 1)
                .queryParam("contact.emails2.email", "contact-3@yandex.ru")

                .queryParam("contact.emails.size", 2)

                .toUriString();
    }
}
