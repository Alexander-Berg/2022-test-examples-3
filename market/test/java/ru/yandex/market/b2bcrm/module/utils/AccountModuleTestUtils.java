package ru.yandex.market.b2bcrm.module.utils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.b2bcrm.module.account.Account;
import ru.yandex.market.b2bcrm.module.account.B2bAccountContactRelation;
import ru.yandex.market.b2bcrm.module.account.B2bContact;
import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.account.Supplier;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;

@Component
public class AccountModuleTestUtils {

    @Inject
    private BcpService bcpService;

    public <T extends Account> T createAccount(Fqn fqn, Map<String, Object> properties) {
        return bcpService.create(fqn, properties);
    }

    public Shop createShop(String title, String shopId, String campaignId, String... emails) {
        return createAccount(Shop.FQN, Map.of(
                Shop.TITLE, title,
                Shop.SHOP_ID, shopId,
                Shop.EMAILS, List.of(emails),
                Shop.CAMPAIGN_ID, campaignId
        ));
    }

    public Shop createShopWithBusinessID(
            String title,
            String shopId,
            String campaignId,
            String businessId,
            String... emails) {
        return createAccount(Shop.FQN, Map.of(
                Shop.TITLE, title,
                Shop.SHOP_ID, shopId,
                Shop.BUSINESS_ID, businessId,
                Shop.EMAILS, List.of(emails),
                Shop.CAMPAIGN_ID, campaignId
        ));
    }

    public Supplier createSupplier(String title, String campaignId, boolean topGmvPartner) {
        return createAccount(Supplier.FQN, Map.of(
                Supplier.TITLE, title,
                Supplier.CAMPAIGN_ID, campaignId,
                Supplier.TOP_GMV_PARTNER, topGmvPartner
        ));
    }

    public B2bContact createContact(String title, String sourceSystem, String... emails) {
        return bcpService.create(B2bContact.FQN, Map.of(
                B2bContact.TITLE, title,
                B2bContact.EMAILS, List.of(emails),
                B2bContact.SOURCE_SYSTEM, sourceSystem
        ));
    }

    public B2bContact createContact(String title, String sourceSystem, List<String> emails, List<String> phones) {
        return bcpService.create(B2bContact.FQN, Map.of(
                B2bContact.TITLE, title,
                B2bContact.EMAILS, emails,
                B2bContact.PHONES, phones,
                B2bContact.SOURCE_SYSTEM, sourceSystem
        ));
    }

    public B2bContact createContact(String title, String sourceSystem, long contactId) {
        return bcpService.create(B2bContact.FQN, Map.of(
                B2bContact.TITLE, title,
                B2bContact.CONTACT_ID, contactId,
                B2bContact.SOURCE_SYSTEM, sourceSystem
        ));
    }

    public B2bContact createMbiContact(String title, long contactId, boolean manuallyCreated) {
        return createContact(title, manuallyCreated ? "MANUALLY": "MBI", contactId);
    }

    public B2bContact createContactForAccount(Account account, String role, String title, String sourceSystem, String... emails) {
        B2bContact contact = createContact(title, sourceSystem, emails);
        bcpService.create(B2bAccountContactRelation.FQN, Map.of(
                B2bAccountContactRelation.CONTACT, contact.getGid(),
                B2bAccountContactRelation.ACCOUNT, account.getGid(),
                B2bAccountContactRelation.CONTACT_ROLE, role,
                B2bAccountContactRelation.SOURCE_SYSTEM, sourceSystem
        ));
        return contact;
    }

    public B2bContact createContactForAccount(Account account, String role, String title, String sourceSystem, List<String> emails, List<String> phones) {
        B2bContact contact = createContact(title, sourceSystem, emails, phones);
        bcpService.create(B2bAccountContactRelation.FQN, Map.of(
                B2bAccountContactRelation.CONTACT, contact.getGid(),
                B2bAccountContactRelation.ACCOUNT, account.getGid(),
                B2bAccountContactRelation.CONTACT_ROLE, role,
                B2bAccountContactRelation.SOURCE_SYSTEM, sourceSystem
        ));
        return contact;
    }

    public B2bContact createMbiContactForAccount(Account account, String role, String title, String... emails) {
        return createContactForAccount(account, role, title, "MBI", emails);
    }

    public B2bAccountContactRelation createRelation(Account account, B2bContact contact, String role, String sourceSystem) {
        return bcpService.create(B2bAccountContactRelation.FQN, Map.of(
                B2bAccountContactRelation.CONTACT, contact.getGid(),
                B2bAccountContactRelation.ACCOUNT, account.getGid(),
                B2bAccountContactRelation.CONTACT_ROLE, role,
                B2bAccountContactRelation.SOURCE_SYSTEM, sourceSystem
        ));
    }

    public B2bAccountContactRelation createMbiRelation(Account account, B2bContact contact, String role) {
        return createRelation(account, contact, role, "MBI");
    }

}
