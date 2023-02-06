package ru.yandex.market.partner.contact;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.StubServRequest;
import ru.yandex.market.core.agency.ContactAndAgencyUserService;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.client.remove.RemoveClientEnvironmentService;
import ru.yandex.market.core.client.remove.RemoveClientMigrationService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.servantlet.params.err.InvalidParamException;

/**
 * @author kudrale
 */
class ContactCheckTest {
    private static final UserInfo USER_INFO = new UserInfo(100L, "asdasd", "a@b.com", "TEST", false);

    private ContactCheckServantlet contactCheckServantlet;
    private ContactService contactService;
    private PassportService passportService;
    private RemoveClientEnvironmentService removeClientEnvironmentService;
    private BalanceContactService balanceContactService;


    private final StubServRequest servRequest = new StubServRequest();
    private final MockServResponse servResponse = new MockServResponse();

    @BeforeEach
    public void setUp() throws Exception {
        contactService = Mockito.mock(ContactService.class);
        passportService = Mockito.mock(PassportService.class);
        removeClientEnvironmentService =
                Mockito.mock(RemoveClientEnvironmentService.class);
        balanceContactService = Mockito.mock(BalanceContactService.class);
        RemoveClientMigrationService removeClientMigrationService = Mockito.mock(RemoveClientMigrationService.class);
        ContactAndAgencyUserService contactAndAgencyUserService = Mockito.mock(ContactAndAgencyUserService.class);
        contactCheckServantlet = new ContactCheckServantlet(contactService, passportService,
                removeClientEnvironmentService, balanceContactService,
                removeClientMigrationService, contactAndAgencyUserService);
    }


    @Test
    void testNoInput() {
        contactCheckServantlet.process(servRequest, servResponse);
        Assertions.assertEquals(1, servResponse.getErrors().size());
        Mockito.verifyNoInteractions(contactService, passportService);
    }

    @Test
    void testFound() {
        servRequest.setParam("login", "TEST");
        Mockito.when(removeClientEnvironmentService.useNewContactCheckProcessing()).thenReturn(true);
        Mockito.when(passportService.getUserInfo("TEST")).thenReturn(USER_INFO);
        Mockito.when(contactService.getContactByUid(100L)).thenReturn(new Contact());
        contactCheckServantlet.process(servRequest, servResponse);
        Assertions.assertEquals(1, servResponse.getData().size());
    }

    @Test
    void testNotFound() {
        servRequest.setParam("login", "TEST");
        contactCheckServantlet.process(servRequest, servResponse);
        Assertions.assertEquals(1, servResponse.getErrors().size());
    }


    @Test
    void testNoContact() {
        servRequest.setParam("login", "TEST");
        Mockito.when(removeClientEnvironmentService.useNewContactCheckProcessing()).thenReturn(true);
        Mockito.when(passportService.getUserInfo(Mockito.anyString())).thenReturn(USER_INFO);
        contactCheckServantlet.process(servRequest, servResponse);
        Assertions.assertEquals(0, servResponse.getData().size());
    }

    @Test
    void testNoContactButClient() {
        servRequest.setParam("login", "TEST");
        Mockito.when(removeClientEnvironmentService.useNewContactCheckProcessing()).thenReturn(true);
        Mockito.when(passportService.getUserInfo(Mockito.anyString())).thenReturn(USER_INFO);
        Mockito.when(balanceContactService.getClientIdByUid(100L)).thenReturn(150L);
        contactCheckServantlet.process(servRequest, servResponse);
        Assertions.assertEquals(1, servResponse.getData().size());
    }

    @Test
    void testHosted() {
        servRequest.setParam("login", "TEST");
        Mockito.when(passportService.getUserInfo(Mockito.anyString())).thenReturn(new UserInfo(100L, "asd", "a@b.com"
                , "TEST", true));
        contactCheckServantlet.process(servRequest, servResponse);
        Assertions.assertEquals(1, servResponse.getErrors().size());
    }

    @Test
    void testPartEmail() {
        servRequest.setParam("login", "TEST@");
        Mockito.when(passportService.getUserInfo(Mockito.anyString())).thenThrow(IllegalArgumentException.class);
        Assertions.assertThrows(InvalidParamException.class,
                () -> contactCheckServantlet.process(servRequest, servResponse));
    }

    @Test
    void testEmpty() {
        servRequest.setParam("login", "");
        contactCheckServantlet.process(servRequest, servResponse);
        Assertions.assertEquals(1, servResponse.getErrors().size());
    }

}
