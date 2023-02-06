package ru.yandex.market.core.notification.service.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.message.dao.ShopMessageAccessDao;
import ru.yandex.market.core.message.model.AgencyMessageAccess;
import ru.yandex.market.core.message.model.ContactShopMessageRoles;
import ru.yandex.market.core.message.model.UserMessageAccess;
import ru.yandex.market.core.message.service.cache.MemCachedShopMessageAccessService;
import ru.yandex.market.core.notification.model.PersistentNotificationType;
import ru.yandex.market.core.notification.service.NotificationTypeService;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link MemCachedShopMessageAccessService}.
 *
 * @author avetokhin 11/01/17.
 */
public class MemCachedShopMessageAccessServiceTest {

    private static final long EXISTING_UID = 1L;
    private static final long NOT_EXISTING_UID = 2L;
    private static final long EXISTING_AGENCY_ID = 22L;
    private static final long NOT_EXISTING_AGENCY_ID = 23L;

    private static final long CONTACT_ID = 10L;

    private static final int ROLE_1 = 1;
    private static final int ROLE_2 = 2;
    private static final int ROLE_3 = 3;
    private static final int ROLE_5 = 5;
    private static final int ROLE_6 = 6;

    private static final long SHOP_ID_1 = 100L;
    private static final long SHOP_ID_2 = 200L;
    private static final long SHOP_ID_3 = 300L;
    private static final long SHOP_ID_4 = 400L;
    private static final long SHOP_ID_5 = 500L;
    private static final long BUSINESS_ID = 1000L;

    private static final PersistentNotificationType NNT_1 =
            new PersistentNotificationType(1L, null, NotificationPriority.LOW, Stream.of(ROLE_1)
                    .collect(Collectors.toSet()));

    private static final PersistentNotificationType NNT_2 =
            new PersistentNotificationType(2L, null, NotificationPriority.HIGH, Stream.of(ROLE_2, ROLE_3)
                    .collect(Collectors.toSet()));

    private static final PersistentNotificationType NNT_3 =
            new PersistentNotificationType(3L, null, NotificationPriority.HIGH, Stream.of(ROLE_3)
                    .collect(Collectors.toSet()));

    private static final PersistentNotificationType NNT_5 =
            new PersistentNotificationType(5L, null, NotificationPriority.LOW, Stream.of(ROLE_5)
                    .collect(Collectors.toSet()));

    private static final PersistentNotificationType NNT_6 =
            new PersistentNotificationType(6L, null, NotificationPriority.HIGH, Stream.of(ROLE_5, ROLE_6)
                    .collect(Collectors.toSet()));

    private static final AgencyMessageAccess AGENCY_MESSAGE_ACCESS = new AgencyMessageAccess(SHOP_ID_1, 0);

    /**
     * Проверить, что возвращается пустое множество для несуществующего UID.
     */
    @Test
    public void testForNotExistingUid() {
        final Set<UserMessageAccess> accessSet =
                createServiceForContact().getMessageAccessSetForContact(NOT_EXISTING_UID);

        assertThat(accessSet, equalTo(Collections.emptySet()));
    }


    /**
     * Проверить, что возвращается корректный набор прав для существующего UID.
     */
    @Test
    public void testForExistingUid() {
        final Set<UserMessageAccess> accessSet =
                createServiceForContact().getMessageAccessSetForContact(EXISTING_UID);

        assertThat(accessSet, equalTo(Stream.of(
                new UserMessageAccess(SHOP_ID_1, NNT_1.getId()),
                new UserMessageAccess(SHOP_ID_1, NNT_2.getId()),
                new UserMessageAccess(SHOP_ID_2, NNT_2.getId()),
                new UserMessageAccess(SHOP_ID_2, NNT_3.getId()),
                new UserMessageAccess(SHOP_ID_3, null),
                new UserMessageAccess(SHOP_ID_4, NNT_1.getId()),
                new UserMessageAccess(SHOP_ID_5, NNT_1.getId()),
                new UserMessageAccess(BUSINESS_ID, NNT_5.getId()),
                new UserMessageAccess(BUSINESS_ID, NNT_6.getId())
        ).collect(Collectors.toSet())));
    }

    /**
     * Проверить, что возвращается пустое множество для несуществующего агентства.
     */
    @Test
    public void testForNotExistingAgency() {
        final Set<AgencyMessageAccess> accessSet =
                createServiceForAgency().getMessageAccessSetForAgency(NOT_EXISTING_AGENCY_ID);

        assertThat(accessSet, equalTo(Collections.emptySet()));
    }

    /**
     * Проверить, что возвращается корректный набор прав для существующего агентства.
     */
    @Test
    public void testForExistingAgency() {
        final Set<AgencyMessageAccess> accessSet =
                createServiceForAgency().getMessageAccessSetForAgency(EXISTING_AGENCY_ID);

        assertThat(accessSet, equalTo(Collections.singleton(AGENCY_MESSAGE_ACCESS)));
    }

    private MemCachedShopMessageAccessService createServiceForContact() {
        final Set<ContactShopMessageRoles> rolesSet = new HashSet<>();
        rolesSet.add(new ContactShopMessageRoles(
                SHOP_ID_1,
                Stream.of(ROLE_1, ROLE_2).collect(Collectors.toSet()))
        );
        rolesSet.add(new ContactShopMessageRoles(
                SHOP_ID_2,
                Stream.of(ROLE_2, ROLE_3).collect(Collectors.toSet()))
        );
        rolesSet.add(new ContactShopMessageRoles(
                SHOP_ID_3,
                Stream.of(ROLE_1, ROLE_2, ROLE_3, ROLE_5, ROLE_6).collect(Collectors.toSet()))
        );
        rolesSet.add(new ContactShopMessageRoles(
                BUSINESS_ID,
                Set.of(ROLE_5, ROLE_6)
        ));
        rolesSet.add(new ContactShopMessageRoles(
                SHOP_ID_4,
                Set.of(ROLE_1)
        ));
        rolesSet.add(new ContactShopMessageRoles(
                SHOP_ID_5,
                Set.of(ROLE_1)
        ));
        final MemCachingService memCachingService = mock(MemCachingService.class);
        when(memCachingService.query(any(), any())).thenReturn(rolesSet);

        return createMemCachedShopMessageAccessService(memCachingService);
    }

    private MemCachedShopMessageAccessService createServiceForAgency() {
        final Set<AgencyMessageAccess> accessSet = Collections.singleton(AGENCY_MESSAGE_ACCESS);

        final MemCachingService memCachingService = mock(MemCachingService.class);
        when(memCachingService.query(any(), any())).thenReturn(Collections.emptySet());
        when(memCachingService.query(any(), eq(EXISTING_AGENCY_ID))).thenReturn(accessSet);

        return createMemCachedShopMessageAccessService(memCachingService);
    }

    private MemCachedShopMessageAccessService createMemCachedShopMessageAccessService(MemCachingService memCachingService) {
        final MemCachedServiceConfig memCachedServiceConfig = Mockito.mock(MemCachedServiceConfig.class);
        final ShopMessageAccessDao shopMessageAccessDao = Mockito.mock(ShopMessageAccessDao.class);
        final NotificationTypeService notificationTypeService = createNotificationTypeService();
        final ContactService contactService = createContactService();

        return new MemCachedShopMessageAccessService(
                memCachingService, memCachedServiceConfig, shopMessageAccessDao,
                notificationTypeService, contactService
        );
    }

    private NotificationTypeService createNotificationTypeService() {
        final Map<Integer, Collection<PersistentNotificationType>> nnTypes = new HashMap<>();
        nnTypes.put(ROLE_1, Collections.singletonList(NNT_1));
        nnTypes.put(ROLE_2, Collections.singletonList(NNT_2));
        nnTypes.put(ROLE_3, Arrays.asList(NNT_2, NNT_3));
        nnTypes.put(ROLE_5, Collections.singletonList(NNT_5));
        nnTypes.put(ROLE_6, Collections.singletonList(NNT_6));

        final NotificationTypeService notificationTypeService = mock(NotificationTypeService.class);
        when(notificationTypeService.findAllGroupedByRole()).thenReturn(nnTypes);
        return notificationTypeService;
    }

    private ContactService createContactService() {
        final ContactService contactService = mock(ContactService.class);
        when(contactService.getContactByUid(EXISTING_UID)).thenReturn(createContact());
        return contactService;
    }

    private ContactWithEmail createContact() {
        final ContactWithEmail contact = new ContactWithEmail();
        contact.setId(CONTACT_ID);
        return contact;
    }
}
