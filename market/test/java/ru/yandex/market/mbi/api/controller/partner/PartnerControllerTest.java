package ru.yandex.market.mbi.api.controller.partner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.manager.dto.ManagerInfoDTO;
import ru.yandex.market.core.orginfo.model.OrganizationInfoSource;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.passport.model.ManagerInfo;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponseStatus;
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerBusinessDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerContactsDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInUnitedCatalogDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerIndexedWithBusinessDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerOrgInfoDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnersBusinessResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.CLICK_AND_COLLECT;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.CPC;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.CROSSDOCK;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP_BY_SELLER;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.FULFILLMENT;

/**
 * @author otedikova
 */
@DbUnitDataSet(before = "PartnerControllerTest.partnerInfo.before.csv")
class PartnerControllerTest extends FunctionalTest {

    private static final String URL = "http://url.com";
    private static final ManagerInfoDTO DEFAULT_MANAGER_DTO = new ManagerInfoDTO(
            ManagerInfo.YANDEX_SUPPORT_MANAGER.getId(),
            ManagerInfo.YANDEX_SUPPORT_MANAGER.getName(),
            ManagerInfo.YANDEX_SUPPORT_MANAGER.getPassportEmail(),
            ManagerInfo.YANDEX_SUPPORT_MANAGER.getPhone(),
            ManagerInfo.YANDEX_SUPPORT_MANAGER.getPassportEmail()
    );

    @Test
    public void testGetExistingRealSupplierByRsId() {
        var realSupplier = getMbiOpenApiClient().getRealSupplierByRsId("000231");

        assertNotNull(realSupplier);
        assertEquals(1100, realSupplier.getSupplierId());
    }

    @Test
    void testGetSupplierPartnerInfo() {
        var actualPartnerInfo = mbiApiClient.getPartnerInfo(1L);
        var expectedPartnerInfo = new PartnerInfoDTO(1L,
                10L,
                CampaignType.SUPPLIER,
                "бизнес",
                "sup.ru",
                "+74991234568",
                null,
                new PartnerOrgInfoDTO(OrganizationType.OAO,
                        "DMF",
                        "4455563445",
                        "shop contact addr",
                        "ул  Мира дом 3",
                        OrganizationInfoSource.YANDEX_MARKET,
                        null,
                        URL),
                false, expectedManager());
        checkPartnerInfo(expectedPartnerInfo, actualPartnerInfo);
    }

    @Test
    void testGetSupplierPartnerInfoWithExpress() {
        var actualPartnerInfo = mbiApiClient.getPartnerInfo(9902L);
        var expectedPartnerInfo = new PartnerInfoDTO(9902L,
                10L,
                CampaignType.SUPPLIER,
                "бизнес",
                "sup2.ru",
                "+74991234568",
                null,
                new PartnerOrgInfoDTO(OrganizationType.OAO,
                        "DMF2",
                        "4455563446",
                        "shop contact addr",
                        "ул  Мира дом 4",
                        OrganizationInfoSource.YANDEX_MARKET,
                        null,
                        URL),
                true, expectedManager());
        checkPartnerInfo(expectedPartnerInfo, actualPartnerInfo);
    }

    @Test
    void testGetShopPartnerInfo() {
        var actualPartnerInfo = mbiApiClient.getPartnerInfo(3L);
        var expectedPartnerInfo = new PartnerInfoDTO(1L,
                10L,
                CampaignType.SHOP,
                "бизнес",
                "shop-partner.ru",
                "8 800 7774433",
                "ул Льва толстого 4",
                new PartnerOrgInfoDTO(OrganizationType.CHP,
                        "Магазин",
                        "5678344",
                        "Москва пр. Строителей",
                        "Москва ул. Ленина 3",
                        OrganizationInfoSource.PARTNER_INTERFACE,
                        null,
                        URL),
                false, DEFAULT_MANAGER_DTO);
        checkPartnerInfo(expectedPartnerInfo, actualPartnerInfo);
    }

    @Test
    void testSupplierXmlRepresentation() {
        var responseEntity = sendRequest(1);
        MbiAsserts.assertXmlEquals(
                "<partner>\n" +
                        "    <id>1</id>\n" +
                        "    <business-id>10</business-id>\n" +
                        "    <type>SUPPLIER</type>\n" +
                        "    <name>бизнес</name>\n" +
                        "    <domain>sup.ru</domain>\n" +
                        "    <phone-number>+74991234568</phone-number>\n" +
                        "    <org-info>\n" +
                        "        <type>7</type>\n" +
                        "        <name>DMF</name>\n" +
                        "        <ogrn>4455563445</ogrn>\n" +
                        "        <fact-address>shopAddress1</fact-address>\n" +
                        "        <juridical-address>ул  Мира дом 3</juridical-address>\n" +
                        "        <info-source>2</info-source>\n" +
                        "        <info-url></info-url>\n" +
                        "    </org-info>\n" +
                        "    <is-express>false</is-express>\n" +
                        "    <manager>\n" +
                        "        <uid>10001</uid>\n" +
                        "        <full-name>Василий Пупкин</full-name>\n" +
                        "        <email>vpupkin@org.ru</email>\n" +
                        "        <phone>1234</phone>\n" +
                        "        <staff-email>vpupkin@yandex-team.ru</staff-email>\n" +
                        "    </manager>" +
                        "</partner>",
                responseEntity.getBody()
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.partnerInfo.testAgencyName.before.csv")
    void testAgencyName() {
        var responseEntity = sendRequest(1);
        MbiAsserts.assertXmlEquals(
                "<partner>\n" +
                        "  <id>1</id>\n" +
                        "  <business-id>10</business-id>\n" +
                        "  <type>SUPPLIER</type>\n" +
                        "  <name>бизнес</name>\n" +
                        "  <domain>sup.ru</domain>\n" +
                        "  <phone-number>+74991234568</phone-number>\n" +
                        "  <org-info>\n" +
                        "    <type>7</type>\n" +
                        "    <name>DMF</name>\n" +
                        "    <ogrn>4455563445</ogrn>\n" +
                        "    <fact-address>shopAddress1</fact-address>\n" +
                        "    <juridical-address>ул  Мира дом 3</juridical-address>\n" +
                        "    <info-source>2</info-source>\n" +
                        "    <info-url></info-url>\n" +
                        "  </org-info>\n" +
                        "  <is-express>false</is-express>\n" +
                        "  <manager>\n" +
                        "    <uid>10</uid>\n" +
                        "    <full-name>agencyName</full-name>\n" +
                        "  </manager>\n" +
                        "</partner>",
                responseEntity.getBody()
        );
    }

    @Test
    void testShopXmlRepresentation() {
        var responseEntity = sendRequest(3);
        MbiAsserts.assertXmlEquals(
                "<partner>\n" +
                        "    <id>3</id>\n" +
                        "    <business-id>10</business-id>\n" +
                        "    <type>SHOP</type>\n" +
                        "    <name>бизнес</name>\n" +
                        "    <domain>shop-partner.ru</domain>\n" +
                        "    <phone-number>8 800 7774433</phone-number>\n" +
                        "    <return-address>ул Льва толстого 4</return-address>\n" +
                        "    <org-info>\n" +
                        "        <type>5</type>\n" +
                        "        <name>Магазин</name>\n" +
                        "        <ogrn>5678344</ogrn>\n" +
                        "        <fact-address></fact-address>\n" +
                        "        <juridical-address>Москва ул. Ленина 3</juridical-address>\n" +
                        "        <info-source>0</info-source>\n" +
                        "        <info-url>http://stroiteley.com</info-url>\n" +
                        "    </org-info>\n" +
                        "    <is-express>false</is-express>\n" +
                        "    <manager>" +
                        "        <uid>-2</uid>" +
                        "        <full-name>Служба Яндекс.Маркет</full-name>" +
                        "    </manager>\n" +
                        "</partner>",
                responseEntity.getBody()
        );
    }

    @Test
    void testGetDeliveryPartnerInfo() {
        var actualPartnerInfo = mbiApiClient.getPartnerInfo(4L);
        var expectedPartnerInfo = new PartnerInfoDTO(4L,
                null,
                CampaignType.DELIVERY,
                "delivery-partner",
                "delivery-partner.com",
                "+79610000000",
                null,
                new PartnerOrgInfoDTO(OrganizationType.OOO,
                        "delivery",
                        "33333",
                        "Москва, ул.Кирова, д.23, кв./офис 37",
                        "Москва, ул.Ленина, д.17, кв./офис 35",
                        OrganizationInfoSource.PARTNER_INTERFACE,
                        null,
                        URL),
                false,
                DEFAULT_MANAGER_DTO);
        checkPartnerInfo(expectedPartnerInfo, actualPartnerInfo);
    }

    @Test
    void testDeliveryXmlRepresentation() {
        var responseEntity = sendRequest(4);
        MbiAsserts.assertXmlEquals(
                "<partner>\n" +
                        "    <id>4</id>\n" +
                        "    <type>DELIVERY</type>\n" +
                        "    <name>delivery-partner</name>\n" +
                        "    <domain>delivery-partner.com</domain>\n" +
                        "    <phone-number>+79610000000</phone-number>\n" +
                        "    <org-info>\n" +
                        "        <type>1</type>\n" +
                        "        <name>delivery</name>\n" +
                        "        <ogrn>33333</ogrn>\n" +
                        "        <fact-address>Москва, ул.Кирова, д.23, кв./офис 37</fact-address>\n" +
                        "        <juridical-address>Москва, ул.Ленина, д.17, кв./офис 35</juridical-address>\n" +
                        "        <info-source>0</info-source>\n" +
                        "        <info-url></info-url>\n" +
                        "    </org-info>\n" +
                        "    <is-express>false</is-express>\n" +
                        "    <manager>" +
                        "        <uid>-2</uid>" +
                        "        <full-name>Служба Яндекс.Маркет</full-name>" +
                        "    </manager>\n" +
                        "</partner>",
                responseEntity.getBody()
        );
    }

    @Test
    @DisplayName("Проверка ответа с инфо FMCG-партнера")
    void testGetFmcgPartnerInfo() {
        var actualPartnerInfo = mbiApiClient.getPartnerInfo(5L);
        var expectedPartnerInfo = new PartnerInfoDTO(5L,
                null,
                CampaignType.FMCG,
                "fmcg-partner",
                "fmcg-partner.ru",
                null,
                null,
                new PartnerOrgInfoDTO(OrganizationType.OOO,
                        "ИНТЕРНЕТ РЕШЕНИЯ",
                        "1027733132195",
                        "125373,Москва г,Походный проезд, стр.2",
                        "г. Москва, пер. Чапаевский",
                        OrganizationInfoSource.PARTNER_INTERFACE,
                        null,
                        URL),
                false, DEFAULT_MANAGER_DTO);
        checkPartnerInfo(expectedPartnerInfo, actualPartnerInfo);
    }

    @Test
    @DisplayName("Проверка ответа с инфо FMCG-партнера в xml-формате")
    void testFmcgXmlRepresentation() {
        var responseEntity = sendRequest(5);
        MbiAsserts.assertXmlEquals(
                "<partner>\n" +
                        "    <id>5</id>\n" +
                        "    <type>FMCG</type>\n" +
                        "    <name>fmcg-partner</name>\n" +
                        "    <domain>fmcg-partner.ru</domain>\n" +
                        "    <org-info>\n" +
                        "        <type>1</type>\n" +
                        "        <name>ИНТЕРНЕТ РЕШЕНИЯ</name>\n" +
                        "        <ogrn>1027733132195</ogrn>\n" +
                        "        <fact-address>125373,Москва г,Походный проезд, стр.2</fact-address>\n" +
                        "        <juridical-address>г. Москва, пер. Чапаевский</juridical-address>\n" +
                        "        <info-source>0</info-source>\n" +
                        "        <info-url></info-url>\n" +
                        "    </org-info>\n" +
                        "    <is-express>false</is-express>\n" +
                        "    <manager>" +
                        "        <uid>-2</uid>" +
                        "        <full-name>Служба Яндекс.Маркет</full-name>" +
                        "    </manager>\n" +
                        "</partner>",
                responseEntity.getBody()
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.superAdmin.before.csv")
    void testGetPartnerSuperAdmin() {
        var expected = new BusinessOwnerDTO(
                501,
                502,
                "devnull",
                Set.of("test@mail.com")
        );
        assertThat(mbiApiClient.getPartnerSuperAdmin(5), equalTo(expected));
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.contacts.before.csv")
    void testGetPartnersContacts() {
        var partnerIds = List.of(500L, 501L);
        var contactsList = mbiApiClient.getPartnersContacts(partnerIds);
        assertThat(contactsList, hasSize(partnerIds.size()));
        var contacts = contactsList.stream().filter(c -> c.getPartnerId() == 500L).
                findFirst().orElse(null);
        assertNotNull(contacts);
        testPartnerContact(contacts, 100L, "phone1", Set.of("mail1@test.ru", "mail2@test.ru"),
                100L);
        contacts = contactsList.stream().filter(c -> c.getPartnerId() == 501L).findFirst().orElse(null);
        assertNotNull(contacts);
        testPartnerContact(contacts, 101L, "phone2", Set.of("mail3@test.ru"), null);
    }

    private ResponseEntity<String> sendRequest(long partnerId) {
        return FunctionalTestHelper.get(
                URL_PREFIX + port + "/partners/" + partnerId,
                String.class
        );
    }

    @Test
    @DisplayName("Нет данных о партнере в индексах")
    void noPartnerIndexState() {
        var partnerId = 10L;
        var indexState = mbiApiClient.getPartnerIndexState(partnerId);
        assertThat(indexState.getPartnerId(), equalTo(partnerId));
        assertThat(indexState.isInProdIdx(), equalTo(false));
        assertThat(indexState.isInTestIdx(), equalTo(false));
    }

    @Test
    @DisplayName("Есть данные о партнере в индексах")
    void partnerIndexState() {
        var partnerId = 100L;
        var indexState = mbiApiClient.getPartnerIndexState(partnerId);
        assertThat(indexState.getPartnerId(), equalTo(partnerId));
        assertThat(indexState.isInProdIdx(), equalTo(false));
        assertThat(indexState.isInTestIdx(), equalTo(true));
    }

    private void checkPartnerInfo(PartnerInfoDTO expectedPartnerInfo, PartnerInfoDTO actualPartnerInfo) {
        assertThat(actualPartnerInfo.getName(), equalTo(expectedPartnerInfo.getName()));
        assertThat(actualPartnerInfo.getDomain(), equalTo(expectedPartnerInfo.getDomain()));
        assertThat(actualPartnerInfo.getType(), equalTo(expectedPartnerInfo.getType()));
        assertThat(actualPartnerInfo.getPhoneNumber(), equalTo(expectedPartnerInfo.getPhoneNumber()));
        assertThat(actualPartnerInfo.getReturnAddress(), equalTo(expectedPartnerInfo.getReturnAddress()));

        var actualOrgInfo = actualPartnerInfo.getPartnerOrgInfo();
        var expectedOrgInfo = expectedPartnerInfo.getPartnerOrgInfo();
        checkPartnerOrgInfo(actualOrgInfo, expectedOrgInfo);
        checkManagerInfo(expectedPartnerInfo.getManager(), actualPartnerInfo.getManager());
    }

    private void checkPartnerOrgInfo(PartnerOrgInfoDTO expectedOrgInfo, PartnerOrgInfoDTO actualOrgInfo) {
        assertThat(actualOrgInfo.getName(), equalTo(expectedOrgInfo.getName()));
        assertThat(actualOrgInfo.getType(), equalTo(expectedOrgInfo.getType()));
        assertThat(actualOrgInfo.getOgrn(), equalTo(expectedOrgInfo.getOgrn()));
        assertThat(actualOrgInfo.getJuridicalAddress(), equalTo(expectedOrgInfo.getJuridicalAddress()));
        assertThat(actualOrgInfo.getInfoSource(), equalTo(expectedOrgInfo.getInfoSource()));
        assertThat(actualOrgInfo.getRegistrationNumber(), equalTo(expectedOrgInfo.getRegistrationNumber()));
    }

    private void checkManagerInfo(ManagerInfoDTO expectedManagerInfo, ManagerInfoDTO actualManagerInfo) {
        if (expectedManagerInfo == null) {
            assertNull(actualManagerInfo);
        } else {
            assertNotNull(actualManagerInfo);
            assertThat(actualManagerInfo.getUid(), equalTo(expectedManagerInfo.getUid()));
            assertThat(actualManagerInfo.getEmail(), equalTo(expectedManagerInfo.getEmail()));
            assertThat(actualManagerInfo.getFullName(), equalTo(expectedManagerInfo.getFullName()));
            assertThat(actualManagerInfo.getPhone(), equalTo(expectedManagerInfo.getPhone()));
            assertThat(actualManagerInfo.getStaffEmail(), equalTo(expectedManagerInfo.getStaffEmail()));
        }
    }

    private ManagerInfoDTO expectedManager() {
        return new ManagerInfoDTO(
                10001,
                "Василий Пупкин",
                "vpupkin@org.ru",
                "1234",
                "vpupkin@yandex-team.ru"
        );
    }

    private void testPartnerContact(
            PartnerContactsDTO partnerContacts, long id, String phone, Set<String> emails, Long onboardingContactId
    ) {
        assertThat(partnerContacts.getContacts(), hasSize(1));
        var contact = partnerContacts.getContacts().get(0);
        assertThat(contact.getId(), equalTo(id));
        assertThat(contact.getPhone(), equalTo(phone));
        assertThat(new HashSet<>(contact.getEmails()), equalTo(emails));
        assertThat(partnerContacts.getOnboardingContact(), equalTo(onboardingContactId));
    }

    @DisplayName("Не нашли партнера по partnerId -> падаем с NOT FOUND")
    @Test
    void isPartnerIndexedWithBusiness_partnerNotFound_fail500() {
        assertThatExceptionOfType(HttpClientErrorException.NotFound.class)
                .isThrownBy(() -> mbiApiClient.isPartnerIndexedWithBusiness(22L, 404040L));
    }

    @DisplayName("Тип кампании (CampaignType) не {SHOP/SUPPLIER} -> падаем с BAD_REQUEST")
    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.isPartnerIndexedWithBusiness.irrelevantCampaignType.csv")
    void isPartnerIndexedWithBusiness_irrelevantCampaignType_fail500() {
        assertThatExceptionOfType(HttpClientErrorException.BadRequest.class)
                .isThrownBy(() -> mbiApiClient.isPartnerIndexedWithBusiness(66L, 404040L));
    }

    @DisplayName("Не включена функциональность удаления офферов -> возвращаем true")
    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.isPartnerIndexedWithBusiness.supplier.csv")
    void isPartnerIndexedWithBusiness_removeNotEnabled_okTrue() {
        checkPartnerFeedBusinessId(22L, true);
    }

    @DisplayName("Поставщик не включен -> возвращаем true")
    @Test
    @DbUnitDataSet(before = {
            "PartnerControllerTest.isPartnerIndexedWithBusiness.targetBusiness.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplier.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplierNotEnabled.csv"
    })
    void isPartnerIndexedWithBusiness_supplierIsNotEnabled_okTrue() {
        checkPartnerFeedBusinessId(22L, true);
    }

    @DisplayName("Нет ff-линок -> возвращаем true")
    @Test
    @DbUnitDataSet(before = {
            "PartnerControllerTest.isPartnerIndexedWithBusiness.targetBusiness.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplier.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplierWithoutLinks.csv"
    })
    void isPartnerIndexedWithBusiness_supplierWithoutLinks_okTrue() {
        checkPartnerFeedBusinessId(22L, true);
    }

    @DisplayName("У поставщика ни одного фида проиндексированного с целевым businessId -> возвращаем false")
    @Test
    @DbUnitDataSet(before = {
            "PartnerControllerTest.isPartnerIndexedWithBusiness.targetBusiness.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplier.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplierEnabled.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.noSupplierFeedWithTargetBusiness.csv"
    })
    void isPartnerIndexedWithBusiness_noSupplierFeedWithTargetBusinessId_okFalse() {
        checkPartnerFeedBusinessId(22L, false);
    }

    @DisplayName("У поставщика есть хотя бы один фид проиндексированный с целевым businessId -> возвращаем true")
    @Test
    @DbUnitDataSet(before = {
            "PartnerControllerTest.isPartnerIndexedWithBusiness.targetBusiness.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplier.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplierEnabled.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.supplierFeedWithTargetBusiness.csv"
    })
    void isPartnerIndexedWithBusiness_atLeastOneSupplierFeedWithTargetBusinessId_okTrue() {
        checkPartnerFeedBusinessId(22L, true);
    }

    @DisplayName("Магазин не включен -> возвращаем true")
    @Test
    @DbUnitDataSet(before = {
            "PartnerControllerTest.isPartnerIndexedWithBusiness.targetBusiness.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.shop.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.shopNotEnabled.csv",
    })
    void isPartnerIndexedWithBusiness_shopIsNotEnabled_okTrue() {
        checkPartnerFeedBusinessId(33L, true);
    }

    @DisplayName("У магазина ни одного фида проиндексированного с целевым businessId -> возвращаем false")
    @Test
    @DbUnitDataSet(before = {
            "PartnerControllerTest.isPartnerIndexedWithBusiness.targetBusiness.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.shop.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.shopEnabled.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.noShopFeedWithTargetBusiness.csv"
    })
    void isPartnerIndexedWithBusiness_noShopFeedWithTargetBusinessId_okFalse() {
        checkPartnerFeedBusinessId(33L, false);
    }

    @DisplayName("У магазина есть хотя бы один фид проиндексированный с целевым businessId -> возвращаем true")
    @Test
    @DbUnitDataSet(before = {
            "PartnerControllerTest.isPartnerIndexedWithBusiness.targetBusiness.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.shop.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.shopEnabled.csv",
            "PartnerControllerTest.isPartnerIndexedWithBusiness.shopFeedWithTargetBusiness.csv"
    })
    void isPartnerIndexedWithBusiness_atLeastOneShopFeedWithTargetBusinessId_okTrue() {
        checkPartnerFeedBusinessId(33L, true);
    }

    private void checkPartnerFeedBusinessId(long partnerId, boolean indexedWithNewBusiness) {
        var expected =
                new PartnerIndexedWithBusinessDTO(partnerId, 404040L, indexedWithNewBusiness);
        var actual = mbiApiClient.isPartnerIndexedWithBusiness(partnerId, 404040L);
        assertThat(actual, equalTo(expected));
    }

    @DisplayName("Партнер не в ЕКате")
    @Test
    void isPartnerInUnitedCatalog_no_okFalse() {
        var expected = PartnerInUnitedCatalogDTO.no(33L);
        assertThat(mbiApiClient.isPartnerInUnitedCatalog(33L), equalTo(expected));
    }

    @DisplayName("Партнер в ЕКате")
    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.isPartnerInUnitedCatalog.yes.csv")
    void isPartnerInUnitedCatalog_yes_okTrue() {
        var expected = PartnerInUnitedCatalogDTO.no(33L);
        assertThat(mbiApiClient.isPartnerInUnitedCatalog(33L), equalTo(expected));
    }


    @DisplayName("Отправка в ЛБ снепшота поставщика")
    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.isPartnerInUnitedCatalog.yes.csv")
    void sendPartnerSnapshot() {
        var responseEntity = FunctionalTestHelper.get(
                URL_PREFIX + port + "/partners/10/send-to-lb",
                GenericCallResponse.class
        );

        assertNotNull(responseEntity.getBody());
        assertThat(responseEntity.getBody().getStatus(), equalTo(GenericCallResponseStatus.OK));
    }

    @DisplayName("Получение бизнесов по идентификаторам партнеров.")
    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.getBusinessesForPartners.before.csv")
    void getBusinessesForPartners() {
        var businessesForPartners = mbiApiClient.getBusinessesForPartners(Set.of(11L, 12L, 13L, 14L));

        var expectedResponse = new PartnersBusinessResponse(List.of(
                new PartnerBusinessDTO(11, 101L),
                new PartnerBusinessDTO(12, 102L),
                new PartnerBusinessDTO(13, 103L)
        ));
        assertThat(businessesForPartners, equalTo(expectedResponse));
    }

    @DisplayName("Получить типы программ размещения для списка партнеров.")
    @Test
    void getPartnerPlacementProgramTypesForPartners() {
        var placementTypes = mbiApiClient.getPartnerPlacementProgramTypesForPartners(Set.of(1L, 2L, 3L, 4L));

        assertThat(placementTypes.getPartnersPlacementProgramTypes(), containsInAnyOrder(
                allOf(
                        hasProperty("partnerId", equalTo(1L)),
                        hasProperty("partnerPlacementProgramTypes",
                                containsInAnyOrder(CROSSDOCK, DROPSHIP, FULFILLMENT))
                ),
                allOf(
                        hasProperty("partnerId", equalTo(2L)),
                        hasProperty("partnerPlacementProgramTypes",
                                containsInAnyOrder(CLICK_AND_COLLECT, CPC, DROPSHIP, DROPSHIP_BY_SELLER))
                )
        ));
    }
}
