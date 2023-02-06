package ru.yandex.market.partner.contact.security;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * Тесты для {@link ManageContactSecurityService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ManageContactSecurityServiceTest extends FunctionalTest {

    @Autowired
    private ManageContactSecurityService manageContactSecurityService;

    @Autowired
    private ContactService contactService;

    @Test
    @DbUnitDataSet(before = "ManageContactSecurityServiceTest.admin.before.csv")
    void assertAdminOrSelf_isAdmin_pass() {
        Contact affectedContact = contactService.getContact(7L);
        manageContactSecurityService.assertAdminOrSelf(60000L, affectedContact);
    }

    @Test
    @DbUnitDataSet(before = "ManageContactSecurityServiceTest.admin.before.csv")
    void assertAdminOrSelf_isSelf_pass() {
        Contact affectedContact = contactService.getContact(6L);
        manageContactSecurityService.assertAdminOrSelf(60000L, affectedContact);
    }

    @Test
    @DbUnitDataSet(before = "ManageContactSecurityServiceTest.agency.before.csv")
    void assertAdminOrSelf_isSubclient_pass() {
        Contact affectedContact = contactService.getContact(6L);
        manageContactSecurityService.assertAdminOrSelf(676000L, affectedContact);
    }

    @Test
    @DbUnitDataSet(before = "ManageContactSecurityServiceTest.admin.before.csv")
    void assertAdminOrSelf_isNotAdmin_exception() {
        Contact affectedContact = contactService.getContact(6L);
        Assertions.assertThatThrownBy(() -> manageContactSecurityService.assertAdminOrSelf(60001L, affectedContact))
                .hasMessage("operation not allowed or contact not found");
    }

    @Test
    @DbUnitDataSet(before = "ManageContactSecurityServiceTest.agency.before.csv")
    void assertAdminForAllCampaigns_isSubclient_pass() {
        manageContactSecurityService.assertAdminForAllCampaigns(676000L, Set.of(60L));
    }

    @Test
    @DbUnitDataSet(before = "ManageContactSecurityServiceTest.admin.before.csv")
    void assertAdminForAllCampaigns_isSelf_pass() {
        manageContactSecurityService.assertAdminForAllCampaigns(60000L, Set.of(60L));
    }

    @Test
    @DbUnitDataSet(before = {
            "ManageContactSecurityServiceTest.admin.before.csv",
            "ManageContactSecurityServiceTest.anotherCampaign.before.csv"
    })
    void assertAdminForAllCampaigns_withoutLink_exception() {
        Assertions.assertThatThrownBy(() -> manageContactSecurityService.assertAdminForAllCampaigns(60000L, Set.of(80L)))
                .hasMessage("operation not allowed or contact not found");
    }
}
