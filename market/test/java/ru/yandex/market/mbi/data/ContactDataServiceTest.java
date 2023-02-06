package ru.yandex.market.mbi.data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactLink;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.state.DataChangesEvent;
import ru.yandex.market.mbi.data.outer.ContactDataOuterService;
import ru.yandex.market.mbi.data.outer.DataOuterServiceUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Тесты для {@link ContactDataService}.
 */
public class ContactDataServiceTest extends FunctionalTest {
    private static final Instant eventTime = Instant.now();

    private static final ContactDataOuterClass.ContactData CONTACT_DATA_1;
    private static final ContactDataOuterClass.ContactData CONTACT_DATA_2;
    private static final ContactWithEmail CONTACT_1;
    private static final ContactWithEmail CONTACT_2;

    private static final Map<Long, Long> CAMPAIGN_ID_TO_PARTNER_ID = Map.of(100L, 1000L, 200L, 2000L);

    static {
        CONTACT_DATA_1 = ContactDataOuterClass.ContactData.newBuilder()
                .setContactId(1L)
                .setLogin("test")
                .setUserId(123)
                .setPhone("+799510122")
                .setGeneralInfo(DataOuterServiceUtil.getGeneralDataInfo(eventTime,
                        DataChangesEvent.PartnerDataOperation.READ))
                .addContactLink(ContactDataOuterClass.ContactLinkData.newBuilder()
                        .setCampaignId(100L)
                        .setPartnerId(1000L)
                        .addAllRoles(Set.of(ContactDataOuterClass.ContactRole.SHOP_ADMIN,
                                ContactDataOuterClass.ContactRole.SHOP_TECHNICAL)))
                .addContactLink(ContactDataOuterClass.ContactLinkData.newBuilder()
                        .setCampaignId(200L)
                        .setPartnerId(2000L)
                        .addAllRoles(Set.of(ContactDataOuterClass.ContactRole.SHOP_OPERATOR)))
                .addEmail("a@ya.ru")
                .addEmail("b@ya.ru")
                .build();

        CONTACT_DATA_2 = ContactDataOuterClass.ContactData.newBuilder()
                .setContactId(2L)
                .setGeneralInfo(DataOuterServiceUtil.getGeneralDataInfo(eventTime,
                        DataChangesEvent.PartnerDataOperation.READ))
                .build();

        CONTACT_1 = new ContactWithEmail();
        CONTACT_1.setId(1L);
        CONTACT_1.setLogin("test");
        CONTACT_1.setUserId(123L);
        CONTACT_1.addContactLink(new ContactLink(100L, Set.of(InnerRole.SHOP_ADMIN, InnerRole.SHOP_TECHNICAL)));
        CONTACT_1.addContactLink(new ContactLink(200L, Set.of(InnerRole.SHOP_OPERATOR)));
        CONTACT_1.setEmails(Set.of(
                new ContactEmail(100L, "a@ya.ru", true, true),
                new ContactEmail(200L, "b@ya.ru", true, true),
                new ContactEmail(300L, "c@ya.ru", true, false)));
        CONTACT_1.setPhone("+799510122");

        CONTACT_2 = new ContactWithEmail();
        CONTACT_2.setId(2L);
    }

    @Autowired
    private ContactDataService contactDataService;

    @Test
    @DbUnitDataSet(before = "ContactDataServiceTest.csv")
    public void testProvideDataForYt() {
        Consumer<Pair<Long, ContactWithEmail>> mock = Mockito.mock(Consumer.class);
        contactDataService.provideDataForYt(mock);
        ArgumentCaptor<Pair<Long, ContactWithEmail>> requestCaptor = ArgumentCaptor.forClass(Pair.class);
        Mockito.verify(mock, times(1)).accept(requestCaptor.capture());
        List<Pair<Long, ContactWithEmail>> values = requestCaptor.getAllValues();
        Assertions.assertEquals(1, values.size());
        Set<Long> actual = values.stream().map(p -> p.getValue().getId()).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(1L), actual);

    }

    @Test
    public void testGetContactDataForExport() {
        assertThat(ContactDataOuterService.getContactDataForExport(CONTACT_1, eventTime,
                DataChangesEvent.PartnerDataOperation.READ, CAMPAIGN_ID_TO_PARTNER_ID))
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .ignoringFields("memoizedHashCode", "contactLink_")
                .isEqualTo(CONTACT_DATA_1);

        assertThat(ContactDataOuterService.getContactDataForExport(CONTACT_2, eventTime,
                DataChangesEvent.PartnerDataOperation.READ, CAMPAIGN_ID_TO_PARTNER_ID))
                .usingRecursiveComparison()
                .ignoringFields("memoizedHashCode", "contactLink_")
                .isEqualTo(CONTACT_DATA_2);
    }
}
