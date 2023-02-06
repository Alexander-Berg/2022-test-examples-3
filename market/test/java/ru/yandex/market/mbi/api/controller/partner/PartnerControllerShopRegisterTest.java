package ru.yandex.market.mbi.api.controller.partner;

import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.ExternalBalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.mbi.api.client.entity.shops.NotificationContactDTO;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationRequest;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PartnerControllerShopRegisterTest extends FunctionalTest {
    @Autowired
    private PassportService passportService;

    @Autowired
    @Qualifier("balanceService")
    private ExternalBalanceService balanceService;
    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceContactService balanceContactService;
    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.registerShop.before.csv",
            after = "PartnerControllerTest.registerShop.after.csv")
    void testRegisterShop() {
        willAnswer(contact -> true)
                .given(marketIdGrpcService).updateContactAccesses(any());
        when(balanceContactService.getClientIdByUid(eq(7L))).thenReturn(700L);
        when(balanceContactService.getUidsByClient(eq(700L))).thenReturn(List.of(7L));
        when(balanceService.getClient(700)).thenReturn(new ClientInfo(700, ClientType.OOO));

        var dto = new SimpleShopRegistrationRequest();
        dto.setShopName("name");
        dto.setCampaignType(CampaignType.TPL_OUTLET);

        var contact = new NotificationContactDTO();
        contact.setFirstName("first");
        contact.setLastName("last");
        contact.setEmail("mail@mail.ru");
        contact.setPhone("phone");

        dto.setNotificationContact(contact);

        SimpleShopRegistrationResponse resp = mbiApiClient.simpleRegisterShop(7L, 7L, dto);

        Assertions.assertNotEquals(0, resp.getDatasourceId());
        Assertions.assertEquals(-2L, resp.getManagerId());
        Assertions.assertEquals(7L, resp.getOwnerId());
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.tryToRegisterShopIfBusinessExists.before.csv")
    void testTryToRegisterShopIfBusinessExists() {
        when(balanceContactService.getClientIdByUid(eq(7L))).thenReturn(700L);
        when(balanceService.getClient(700)).thenReturn(new ClientInfo(700, ClientType.OOO));

        var dto = new SimpleShopRegistrationRequest();
        dto.setShopName("name");
        dto.setCampaignType(CampaignType.TPL_OUTLET);

        var contact = new NotificationContactDTO();
        contact.setFirstName("first");
        contact.setLastName("last");
        contact.setEmail("mail@mail.ru");
        contact.setPhone("phone");

        dto.setNotificationContact(contact);

        HttpClientErrorException.BadRequest badRequestException =
                assertThrows(HttpClientErrorException.BadRequest.class,
                        () -> mbiApiClient.simpleRegisterShop(7L, 7L, dto));

        String expected = "<validation-error>\n" +
                "<message>Can't add contact link for contact with id 103, there are partners with different business " +
                "type on it</message><codes>\n" +
                "<code>contact-has-business-partners</code>\n" +
                "</codes>\n" +
                "</validation-error>";

        MbiAsserts.assertXmlEquals(expected, badRequestException.getResponseBodyAsString());
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.registerShop.before.csv")
    void tryToRegisterShopWithExistentNameForClient() {
        when(balanceService.getClient(700)).thenReturn(new ClientInfo(700, ClientType.OOO));

        var contact = new NotificationContactDTO();
        contact.setFirstName("first");
        contact.setLastName("last");
        contact.setEmail("mail@mail.ru");
        contact.setPhone("phone");

        var request = new SimpleShopRegistrationRequest();
        request.setShopName("PVZ");
        request.setCampaignType(CampaignType.TPL_OUTLET);
        request.setNotificationContact(contact);

        var response = mbiApiClient.simpleRegisterShop(8L, 8L, request);
        Assertions.assertEquals(1000, response.getDatasourceId());
        Assertions.assertEquals(700, response.getClientId());
        Assertions.assertEquals(200, response.getCampaignId());
        Assertions.assertEquals(-2L, response.getManagerId());
        Assertions.assertEquals(8L, response.getOwnerId());

    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.registerShop.before.csv")
    void testRegisterShopAsSystemManager() {
        when(balanceService.getClient(700)).thenReturn(new ClientInfo(700, ClientType.OOO));
        var dto = new SimpleShopRegistrationRequest();
        dto.setShopName("name");
        dto.setCampaignType(CampaignType.TPL_OUTLET);

        var contact = new NotificationContactDTO();
        contact.setFirstName("first");
        contact.setLastName("last");
        contact.setEmail("mail@mail.ru");
        contact.setPhone("phone");

        dto.setNotificationContact(contact);

        SimpleShopRegistrationResponse resp = mbiApiClient.simpleRegisterShop(7L, 7L, dto);

        Assertions.assertNotEquals(0, resp.getDatasourceId());
        Assertions.assertEquals(-2L, resp.getManagerId());
        Assertions.assertEquals(7L, resp.getOwnerId());
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.registerShop.before.csv",
            after = "PartnerControllerTest.registerShop.after.csv")
    void testRegisterShopForAnotherOwner() {
        when(balanceContactService.getClientIdByUid(eq(7L))).thenReturn(700L);
        when(balanceService.getClient(700)).thenReturn(new ClientInfo(700, ClientType.OOO));

        String ownerLogin = "ivanov@yandex.ru";
        when(passportService.findUid(ownerLogin)).thenReturn(7L);

        var dto = new SimpleShopRegistrationRequest();
        dto.setShopName("name");
        dto.setCampaignType(CampaignType.TPL_OUTLET);
        dto.setOwnerLogin(ownerLogin);

        var contact = new NotificationContactDTO();
        contact.setFirstName("first");
        contact.setLastName("last");
        contact.setEmail("mail@mail.ru");
        contact.setPhone("phone");

        dto.setNotificationContact(contact);

        SimpleShopRegistrationResponse resp = mbiApiClient.simpleRegisterShop(6L, 8L, dto);

        Assertions.assertNotEquals(0, resp.getDatasourceId());
        Assertions.assertEquals(6L, resp.getManagerId());
        Assertions.assertEquals(7L, resp.getOwnerId());
    }
}
