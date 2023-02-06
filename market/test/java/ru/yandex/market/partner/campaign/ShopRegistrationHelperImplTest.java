package ru.yandex.market.partner.campaign;

import java.util.HashSet;
import java.util.Set;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.core.agency.AgencyNotRegisteredException;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.client.remove.RemoveClientEnvironmentService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.partner.campaign.impl.ShopRegistrationHelperImpl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
@ExtendWith(MockitoExtension.class)
public class ShopRegistrationHelperImplTest {

    private static final long OWNER_ID = 1L;
    private static final long ACTION_ID = 2L;
    private static final long CAMPAIGN_ID = 3L;
    private static final long CLIENT_ID = 4L;
    private static final long UID = 5L;
    @Mock
    FeatureService featureService;
    @Mock
    FeedService feedService;
    @Mock
    PassportService passportService;
    @Mock
    RemoveClientEnvironmentService removeClientEnvironmentService;
    private ShopRegistrationHelper instance;
    @Mock
    private ContactService contactService;
    @Mock
    private BalanceService balanceService;
    @Mock
    private AgencyService agencyService;

    @BeforeEach
    public void setUp() {
        instance = new ShopRegistrationHelperImpl(
                contactService,
                balanceService,
                agencyService,
                featureService,
                feedService,
                passportService,
                removeClientEnvironmentService);
    }

    /**
     * Тест проверяет работу алгоритма по определению аналогичных email'ов в основном и
     * нотификационном контакте.
     */
    @Test
    public void saveContactsMatchEmails() {
        ContactWithEmail contact = createContactWithEmails("test@test.ru", "another_test@test.ru");
        ContactWithEmail notification = createContactWithEmails("test@test.ru");

        when(contactService.getContactWithEmailByUid(OWNER_ID)).thenReturn(contact);

        instance.saveContacts(2L, CAMPAIGN_ID, OWNER_ID, notification);

        Mockito.verify(contactService).addActiveEmail(contact, notification);
        Mockito.verify(contactService).updateContactDataForCampaign(ACTION_ID, contact, CAMPAIGN_ID);
        Mockito.verify(contactService, Mockito.never()).updateContactDataForCampaign(ACTION_ID, notification,
                CAMPAIGN_ID);
    }

    @Test
    public void saveContactsNotMatchEmails() {
        ContactWithEmail contact = createContactWithEmails("test@test.ru", "another_test@test.ru");
        ContactWithEmail notification = createContactWithEmails("not_matched_email@test.ru");

        when(contactService.getContactWithEmailByUid(OWNER_ID)).thenReturn(contact);

        instance.saveContacts(ACTION_ID, CAMPAIGN_ID, OWNER_ID, notification);

        Mockito.verify(contactService).updateContactDataForCampaign(ACTION_ID, contact, CAMPAIGN_ID);
        Mockito.verify(contactService).updateContactDataForCampaign(ACTION_ID, notification, CAMPAIGN_ID);
    }


    /**
     * Тест проверяет, что если пришел клиент-агентство, который еще не зарегистрирован (это ручной процесс через
     * саппорт), то мы отдаем AgencyNotRegisteredException.
     */
    @Test
    public void testGetAgencyIdNotRegisteredAgency() {
        ClientInfo clientInfo = new ClientInfo(CLIENT_ID, ClientType.OOO, true, CLIENT_ID);
        when(balanceService.getClientByUid(UID)).thenReturn(clientInfo);
        when(agencyService.getAgency(CLIENT_ID)).thenReturn(null);
        when(removeClientEnvironmentService.useNewRegistrationAgencyChecking()).thenReturn(true);
        assertThatThrownBy(() -> instance.getAgencyId(1L, UID, true))
                .isInstanceOf(AgencyNotRegisteredException.class);
    }

    private ContactWithEmail createContactWithEmails(String... emails) {
        ContactWithEmail contact = new ContactWithEmail();
        Set<ContactEmail> set = new HashSet<>();
        for (int i = 0; i < emails.length; i++) {
            set.add(new ContactEmail(i, emails[i], true, true));
        }
        contact.setEmails(set);
        return contact;
    }

}
