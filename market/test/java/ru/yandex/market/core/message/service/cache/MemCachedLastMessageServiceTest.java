package ru.yandex.market.core.message.service.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.market.core.message.dao.LastMessageDao;
import ru.yandex.market.core.message.model.LastShopMessage;
import ru.yandex.market.core.message.model.LastShopUserMessage;
import ru.yandex.market.core.message.model.UserMessageAccess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.util.matcher.NamedMemCacheableMatcher.hasName;

/**
 * Unit теты для {@link MemCachedLastMessageService}.
 *
 * @author avetokhin 17/01/17.
 */
public class MemCachedLastMessageServiceTest {

    private static final long SHOP_ID_1 = 66L;
    private static final long SHOP_ID_2 = 77L;
    private static final long SHOP_ID_3 = 88L;
    private static final long USER_ID = 145L;

    private static final long NN_TYPE_1 = 10L;
    private static final long NN_TYPE_2 = 20L;
    private static final long NN_TYPE_3 = 30L;

    private static final Long LAST_READ_MSG_ID = 500L;

    private static final Set<UserMessageAccess> ACCESS_SET = Stream.of(
            new UserMessageAccess(SHOP_ID_1, NN_TYPE_1),
            new UserMessageAccess(SHOP_ID_1, NN_TYPE_2),
            new UserMessageAccess(SHOP_ID_1, NN_TYPE_3),
            new UserMessageAccess(SHOP_ID_2, null),
            new UserMessageAccess(SHOP_ID_3, NN_TYPE_1),
            new UserMessageAccess(SHOP_ID_3, NN_TYPE_2)
    ).collect(Collectors.toSet());

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateLastReadMessage() {
        var maxNotificationId = LAST_READ_MSG_ID + 1;
        test((service, dao, memCachingService) -> {
            service.updateLastReadMessage(USER_ID, maxNotificationId);
            verify(dao).updateUserLastReadMessage(USER_ID, maxNotificationId);
            verify(memCachingService).clean(argThat(hasName("getLastReadUserMessage")), eq(USER_ID));
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveLastShopMessageUserOnly() {
        // Сохранение сообщения для магазина
        test((service, dao, memCachingService) -> {
            final LastShopUserMessage message = new LastShopUserMessage(1L, SHOP_ID_1, null, 15L);
            service.saveLastShopMessage(message);

            verify(dao).saveLastShopMessage(message);
            verify(memCachingService).clean(argThat(hasName("getLastMessagesForShop")), eq(SHOP_ID_1));
            verifyNoMoreInteractions(memCachingService);
        });

        // Сохранение личного сообщения пользователю
        test((service, dao, memCachingService) -> {
            final LastShopUserMessage message = new LastShopUserMessage(1L, null, USER_ID, 15L);
            service.saveLastShopMessage(message);

            verify(dao).saveLastShopMessage(message);
            verify(memCachingService).clean(argThat(hasName("getLastMessageIdForUser")), eq(USER_ID));
            verifyNoMoreInteractions(memCachingService);
        });

        // Сохранение сообщения для магазина и пользователя одновременно
        test((service, dao, memCachingService) -> {
            final LastShopUserMessage message = new LastShopUserMessage(1L, SHOP_ID_1, USER_ID, 15L);
            service.saveLastShopMessage(message);

            verify(dao).saveLastShopMessage(message);
            verify(memCachingService).clean(argThat(hasName("getLastMessagesForShop")), eq(SHOP_ID_1));
            verify(memCachingService).clean(argThat(hasName("getLastMessageIdForUser")), eq(USER_ID));
            verifyNoMoreInteractions(memCachingService);
        });
    }

    /**
     * У пользователя есть личное сообщение, которое новее последнего прочитанного.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testUserHasNewMessagesUserMessage() {
        test((service, dao, memCachingService) -> {
            final long lastMessageIdForUser = LAST_READ_MSG_ID + 1;
            doReturn(lastMessageIdForUser)
                    .when(memCachingService)
                    .query(argThat(hasName("getLastMessageIdForUser")), eq(USER_ID));

            final boolean result = service.userHasNewMessages(USER_ID, ACCESS_SET);

            verify(memCachingService).query(argThat(hasName("getLastReadUserMessage")), eq(USER_ID));
            verify(memCachingService).query(argThat(hasName("getLastMessageIdForUser")), eq(USER_ID));
            verifyNoMoreInteractions(memCachingService);

            assertThat(result, equalTo(true));
        });
    }

    /**
     * У магазина доступного пользователю есть сообщение, новее последнего прочитанного.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testUserHasNewMessagesShopMessageCase1() {
        final Map<Long, Set<LastShopMessage>> lastShopsMessages = new HashMap<>();
        lastShopsMessages.put(SHOP_ID_1, Stream.of(
                new LastShopMessage(LAST_READ_MSG_ID - 10, NN_TYPE_1),
                new LastShopMessage(LAST_READ_MSG_ID, NN_TYPE_2)
        ).collect(Collectors.toSet()));
        lastShopsMessages.put(SHOP_ID_2, Stream.of(
                new LastShopMessage(LAST_READ_MSG_ID + 1, NN_TYPE_1),
                new LastShopMessage(LAST_READ_MSG_ID - 1, NN_TYPE_2)
        ).collect(Collectors.toSet()));

        testUserHasNewMessagesWithShopMessage(lastShopsMessages, true);
    }

    /**
     * У магазина доступного пользователю есть сообщение, новее последнего прочитанного.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testUserHasNewMessagesShopMessageCase2() {
        final Map<Long, Set<LastShopMessage>> lastShopsMessages = new HashMap<>();
        lastShopsMessages.put(SHOP_ID_1, Stream.of(
                new LastShopMessage(LAST_READ_MSG_ID + 10, NN_TYPE_1),
                new LastShopMessage(LAST_READ_MSG_ID, NN_TYPE_2)
        ).collect(Collectors.toSet()));
        lastShopsMessages.put(SHOP_ID_2, Stream.of(
                new LastShopMessage(LAST_READ_MSG_ID, NN_TYPE_1),
                new LastShopMessage(LAST_READ_MSG_ID - 1, NN_TYPE_2)
        ).collect(Collectors.toSet()));

        testUserHasNewMessagesWithShopMessage(lastShopsMessages, true);
    }

    /**
     * У магазина доступного пользователю есть сообщение, новее последнего прочитанного.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testUserHasNewMessagesShopMessageCase3() {
        final Map<Long, Set<LastShopMessage>> lastShopsMessages = new HashMap<>();
        lastShopsMessages.put(SHOP_ID_3, Stream.of(
                new LastShopMessage(LAST_READ_MSG_ID - 1, NN_TYPE_1),
                new LastShopMessage(LAST_READ_MSG_ID + 1, NN_TYPE_2)
        ).collect(Collectors.toSet()));
        testUserHasNewMessagesWithShopMessage(lastShopsMessages, true);
    }

    /**
     * У магазина доступного пользователю нет сообщения, новее последнего прочитанного,
     * точнее оно есть, но у пользователя нет на данный тип уведомлений прав
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testUserHasNewMessagesNoShopMessage() {
        final Map<Long, Set<LastShopMessage>> lastShopsMessages = new HashMap<>();
        lastShopsMessages.put(SHOP_ID_1, Stream.of(
                new LastShopMessage(LAST_READ_MSG_ID, NN_TYPE_1),
                new LastShopMessage(LAST_READ_MSG_ID - 10, NN_TYPE_2)
        ).collect(Collectors.toSet()));
        lastShopsMessages.put(SHOP_ID_3, Stream.of(
                new LastShopMessage(LAST_READ_MSG_ID, NN_TYPE_1),
                new LastShopMessage(LAST_READ_MSG_ID + 10, NN_TYPE_3)
        ).collect(Collectors.toSet()));

        testUserHasNewMessagesWithShopMessage(lastShopsMessages, false);
    }

    @SuppressWarnings("unchecked")
    private void testUserHasNewMessagesWithShopMessage(final Map<Long, Set<LastShopMessage>> lastShopsMessages,
                                                       final boolean expected) {
        test((service, dao, memCachingService) -> {
            final long lastMessageIdForUser = LAST_READ_MSG_ID - 1;
            final Set<Long> availableShops = Stream.of(SHOP_ID_1, SHOP_ID_2, SHOP_ID_3).collect(Collectors.toSet());

            doReturn(lastMessageIdForUser)
                    .when(memCachingService)
                    .query(argThat(hasName("getLastMessageIdForUser")), eq(USER_ID));

            when(memCachingService.queryBulk(any(), any())).thenReturn((Map) lastShopsMessages);

            final boolean result = service.userHasNewMessages(USER_ID, ACCESS_SET);

            verify(memCachingService).query(argThat(hasName("getLastReadUserMessage")), eq(USER_ID));
            verify(memCachingService).query(argThat(hasName("getLastMessageIdForUser")), eq(USER_ID));
            verify(memCachingService).queryBulk(any(), eq(availableShops));
            verifyNoMoreInteractions(memCachingService);

            assertThat(result, equalTo(expected));
        });
    }

    @SuppressWarnings("unchecked")
    private void test(final TestCase testCase) {
        final MemCachingService memCachingService = mock(MemCachingService.class);
        final MemCachedServiceConfig memCachedServiceConfig = mock(MemCachedServiceConfig.class);

        when(memCachingService.query(argThat(hasName("getLastReadUserMessage")), eq(USER_ID)))
                .thenReturn(LAST_READ_MSG_ID);

        final LastMessageDao dao = mock(LastMessageDao.class);
        final MemCachedLastMessageService service =
                new MemCachedLastMessageService(memCachingService, memCachedServiceConfig, dao);

        testCase.test(service, dao, memCachingService);
    }

    private interface TestCase {
        void test(MemCachedLastMessageService service, LastMessageDao dao, MemCachingService memCachingService);
    }
}
