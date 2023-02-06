package ru.yandex.market.core.contact.db;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.contact.ContactAlreadyLinkedException;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactLink;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.contact.utils.ContactUtils;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты для {@link ContactService}.
 *
 * @author Vadim Lyalin
 */
class DbContactServiceFunctionalTest extends FunctionalTest {

    private static final Logger log = LoggerFactory.getLogger(DbContactServiceFunctionalTest.class);

    @Autowired
    private ContactService contactService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private BalanceContactService balanceContactService;

    public static Stream<Arguments> dataCheckGetLinkedCampaignIdsByUid() {
        return Stream.of(
                Arguments.of(1000L, List.of(200L)),
                Arguments.of(1001L, List.of(201L)),
                Arguments.of(1002L, List.of())
        );
    }

    public static Stream<Arguments> dataCheckGetLinkedCampaignIdsByUidWithBusiness() {
        return Stream.of(
                Arguments.of(99L, List.of(12345L)),
                Arguments.of(66L, List.of(12345L)),
                Arguments.of(33L, List.of(54321L)),
                Arguments.of(19L, List.of(10001L))
        );
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "ContactServiceTest.contacts.before.csv")
    @MethodSource("dataCheckGetLinkedCampaignIdsByUid")
    void checkGetLinkedCampaignIdsByUid(long uid, List<Long> campaignList) {
        assertThat(contactService.getLinkedCampaignIdsByUid(uid)).isEqualTo(campaignList);
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "BusinessOwnerDaoTest.before.csv")
    @MethodSource("dataCheckGetLinkedCampaignIdsByUidWithBusiness")
    void checkGetLinkedCampaignIdsByUidWithBusiness(long uid, List<Long> campaignList) {
        assertThat(contactService.getLinkedCampaignIdsByUid(uid)).isEqualTo(campaignList);
    }

    @Test
    @DbUnitDataSet(before = "ContactServiceTest.contacts.before.csv")
    void testGetPartnerContacts() {
        List<Long> partnerIds = List.of(500L, 501L);
        Map<Long, List<ContactWithEmail>> contacts = contactService.getPartnersContactsWithEmail(partnerIds);
        assertThat(partnerIds).hasSize(contacts.size());
        testPartnerContact(contacts.get(500L), 100L, "phone1", Set.of("mail1@test.ru", "mail2@test.ru"));
        testPartnerContact(contacts.get(501L), 101L, "phone2", Set.of("mail3@test.ru"));
    }

    private void testPartnerContact(List<ContactWithEmail> contactList, Long id, String phone, Set<String> emails) {
        assertThat(contactList).hasSize(1);
        ContactWithEmail contact = contactList.get(0);
        assertThat(contact.getId()).isEqualTo(id);
        assertThat(contact.getPhone()).isEqualTo(phone);
        assertThat(emails).isEqualTo(contact.getEmails()
                .stream()
                .map(ContactEmail::getEmail)
                .collect(Collectors.toSet()));
    }

    /**
     * Тестирует обновление контакта. В частности:
     * <ul>
     *     <li>Обновление логина, если новый и старый логины равны с точностью до нормализации</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(before = "DbContactServiceFunctionalTest.csv",
            after = "DbContactServiceFunctionalTest.update.after.csv")
    void testUpdateContact() {
        ContactWithEmail contact = buildContact();
        contact.setLogin("Vasily.Pupkin");

        contactService.updateContactWithEmail(1, contact);
    }

    @Test
    @DbUnitDataSet(before = "DbContactServiceFunctionalTest.updateAdvAgreeFlag.before.csv",
            after = "DbContactServiceFunctionalTest.updateAdvAgreeFlag.after.csv")
    void testUpdateAdvAgreeFlag() {
        ContactWithEmail contact = buildContact();
        contact.setLogin("vasily-pupkin");
        contact.setAdvAgree(true);

        contactService.updateContactWithEmail(1, contact);
    }

    /**
     * Тестирует обновление непаспортного контакта.
     */
    @Test
    @DbUnitDataSet(before = "ContactServiceTest.updateContactNonPassport.before.csv",
            after = "ContactServiceTest.updateContactNonPassport.after.csv")
    void testUpdateContactNonPassport() {
        ContactWithEmail contact = buildContact();
        contact.setUserId(null);
        contact.setFirstName("Василий");
        contact.setLastName("Пупкин");
        contact.setPhone("321");
        contact.setMarketOnly(true);

        contactService.updateContactWithEmail(1, contact);
    }

    /**
     * Проверяет, что нельзя обновить логин контакта на отличный с учетом нормализации.
     */
    @Test
    @DbUnitDataSet(before = "DbContactServiceFunctionalTest.csv")
    void testUpdateContactError() {
        ContactWithEmail contact = buildContact();
        contact.setLogin("VasilyPupkin");

        assertThatThrownBy(() -> contactService.updateContactWithEmail(1, contact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Trying to change login for contact");
//        assertEquals("Trying to change login for contact", exception.getMessage());
    }

    private ContactWithEmail buildContact() {
        ContactWithEmail contact = new ContactWithEmail();
        contact.setId(1);
        contact.setUserId(2L);
        contact.setLinks(Set.of());
        contact.setEmails(Set.of());
        return contact;
    }

    /**
     * Проверяет удаление линка на поставщика.
     */
    @Test
    @DbUnitDataSet(before = "ContactServiceTest.deleteLink.before.csv",
            after = "ContactServiceTest.deleteLink.after.csv")
    void testDeleteLinkForSupplier() {
        contactService.deleteLinksByCampaign(100L, 0);
    }

    /**
     * Проверяет удаление линка на закрытую кампанию.
     */
    @Test
    @DbUnitDataSet(before = "ContactServiceTest.deleteClosedLink.before.csv",
            after = "ContactServiceTest.deleteClosedLink.after.csv")
    void testDeleteLinkForClosedCampaign() {
        contactService.deleteLinksByCampaign(100L, 0);
    }

    @Test
    void testContactLinks() {
        Set<Long> uidsToAdd = Set.of(18290890L);
        final List<ContactWithEmail> contacts = contactService.getContactWithEmailByUids(uidsToAdd);
        final Map<Long, ContactWithEmail> contactsMap = contacts.stream().collect(toMap(ContactWithEmail::getUserId,
                c -> c));
        final Map<Long, CampaignInfo> contactCampaigns = getContactCampaigns(contacts);

        for (final long uid : uidsToAdd) {
            final Contact contact = contactsMap.get(uid);

            if (contact != null) {
                final Set<ContactLink> links = contact.getLinks().stream()
                        .filter(cl -> !ContactUtils.isBusinessLink(cl))
                        .collect(Collectors.toSet());
                if (links.isEmpty()) {
                    log.error("Links is empty");

                } else {
                    final long campaignId = links.iterator().next().getCampaignId();
                    final CampaignInfo campaign = contactCampaigns.get(campaignId);

                    if (campaign == null) {
                        log.error("Campaign not found " + campaignId);
                    }
                    throw new ContactAlreadyLinkedException(
                            contact.getId(),
                            campaignId,
                            campaign != null ? campaign.getClientId() : null
                    );
                }
            } else {
                log.info("addUidToClient");
            }
        }
    }

    @Test
    @DbUnitDataSet(before = "ContactServiceTest.deleteContactLink.before.csv",
            after = "ContactServiceTest.deleteContactLink.after.csv")
    void testDeleteContactLink() {
        contactService.deleteContactLink(1, 10);
    }

    private Map<Long, CampaignInfo> getContactCampaigns(List<ContactWithEmail> contacts) {
        final Map<Long, Long> contactCampaignMap = contacts.stream()
                .filter(c -> !CollectionUtils.isEmpty(c.getLinks()))
                .collect(toMap(
                        ContactWithEmail::getUserId,
                        c -> c.getLinks().iterator().next().getCampaignId()
                ));

        return !contactCampaignMap.isEmpty()
                ? campaignService.getMarketCampaigns(contactCampaignMap.values())
                : Collections.emptyMap();
    }

    @Test
    @DisplayName("Синхронизация контакта с балансом")
    @DbUnitDataSet(
            before = "ContactServiceTest.testSyncContactWithBalance.before.csv",
            after = "ContactServiceTest.testSyncContactWithBalance.after.csv"
    )
    void testSyncContactWithBalance() {
        Mockito.when(balanceContactService.getUidsByClient(1L)).thenReturn(List.of(3L));
        contactService.syncContactWithBalance(1, 1L, Arrays.asList(2L, 3L), List.of(), false);
    }
}
