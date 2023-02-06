package ru.yandex.market.partner.contact;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.common.framework.core.ErrorInfo;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.error.ErrorInfos;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.security.model.DualUidable;
import ru.yandex.market.core.servantlet.params.BeanServantletParam;
import ru.yandex.market.core.servantlet.params.err.InvalidParamException;
import ru.yandex.market.core.servantlet.params.err.ServantletParamException;
import ru.yandex.market.partner.contact.security.ManageContactSecurityService;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author kudrale
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageContactServantletTest {
    @InjectMocks
    private ManageContactServantlet manageContactServantlet;

    private ServRequest servRequest = Mockito.mock(ServRequest.class,
            withSettings().extraInterfaces(DualUidable.class));
    @Mock
    private ServResponse servResponse;
    @Mock
    private ContactService contactService;
    @Mock
    private PassportService passportService;
    @Mock
    private ManageContactSecurityService manageContactSecurityService;
    @Mock
    private CampaignService campaignService;


    @Before
    public void setUp() {
        BeanServantletParam<ContactWithEmail> contact = new BeanServantletParam<ContactWithEmail>() {
            @Override
            public ContactWithEmail fetchValue(ServRequest request, String name) throws ServantletParamException {
                ContactWithEmail c = new ContactWithEmail();
                c.setLinks(new HashSet<>());
                return c;
            }
        };
        contact.setClazz(ContactWithEmail.class);
        contact.setName("contact");
        manageContactServantlet.setContactServanletParam(contact);
        manageContactServantlet.init();
        when(((DualUidable) servRequest).getEffectiveUid()).thenReturn(111L);
    }

    @Test
    public void testRequest() {
        manageContactServantlet.request(servRequest, servResponse);
        ArgumentCaptor<?> errorCapture = ArgumentCaptor.forClass(ErrorInfo.class);
        verify(servResponse, times(1)).addErrorInfo((ErrorInfo) errorCapture.capture());
        assertTrue(errorCapture.getAllValues().get(0) == ErrorInfos.CONTACT_NOT_FOUND);
    }

    @Test
    public void testRequestByUid() {
        when(contactService.getContactWithEmailByUid(111L)).thenReturn(new ContactWithEmail());
        manageContactServantlet.request(servRequest, servResponse);

        ArgumentCaptor<?> contactCaptor = ArgumentCaptor.forClass(Object.class);
        verify(servResponse, times(1)).addData(contactCaptor.capture());
        assertTrue(contactCaptor.getAllValues().get(0).getClass().equals(ContactWithEmail.class));
    }

    @Test
    public void testRequestWithNotExistsContact() {
        when(servRequest.getParam(Mockito.eq("contact_id"), Mockito.eq(true))).thenReturn("123");
        manageContactServantlet.request(servRequest, servResponse);
        ArgumentCaptor<?> errorCapture = ArgumentCaptor.forClass(ErrorInfo.class);
        verify(servResponse, times(1)).addErrorInfo((ErrorInfo) errorCapture.capture());
        assertTrue(errorCapture.getAllValues().get(0) == ErrorInfos.CONTACT_NOT_FOUND);
    }

    @Test(expected = InvalidParamException.class)
    public void testRequestWithIllegalLogin() {
        ContactWithEmail euidContact = new ContactWithEmail();
        euidContact.setId(1234L);
        when(servRequest.getParam(Mockito.eq("added_login"), Mockito.eq(true))).thenReturn("asdf@");
        when(passportService.getUserInfo(eq("asdf@"))).thenThrow(IllegalArgumentException.class);
        manageContactServantlet.create(servRequest, servResponse);
    }

}
