package ru.yandex.market.crm.platform.reader.services.logbroker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.domain.user.IdsGraph;
import ru.yandex.market.crm.platform.domain.user.Uid;
import ru.yandex.market.crm.platform.domain.user.CrmUidType;
import ru.yandex.market.crm.platform.domain.user.User;
import ru.yandex.market.crm.platform.domain.user.UserIdRow;
import ru.yandex.market.crm.platform.reader.test.AbstractServiceTest;
import ru.yandex.market.crm.platform.test.utils.UserTestUtils;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.util.LiluCollectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.platform.test.utils.UserTestUtils.assertIdsGraph;
import static ru.yandex.market.crm.platform.test.utils.UserTestUtils.edge;
import static ru.yandex.market.crm.platform.test.utils.UserTestUtils.userIdRow;

/**
 * @author apershukov
 */
public class SubscriptionsLogConsumerTest extends AbstractServiceTest {

    private static Map<String, String> subscription(Uid email, Uid puid, Uid uuid, Uid yuid) {
        return subscription(2, email, puid, uuid, yuid);
    }

    private static Map<String, String> subscription(long type, Uid email, Uid puid, Uid uuid, Uid yuid) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                .put("subscription_type", String.valueOf(type))
                .put("email", email.getValue());

        if (puid != null) {
            builder.put("puid", puid.getValue());
        }

        if (uuid != null) {
            builder.put("uuid", uuid.getValue());
        }

        if (yuid != null) {
            builder.put("yuid", yuid.getValue());
        }

        return builder.build();
    }

    private static final Uid PUID_1 = Uid.ofPuid(111);
    private static final Uid PUID_2 = Uid.ofPuid(222);
    private static final Uid PUID_3 = Uid.ofPuid(333);

    private static final Uid EMAIL_1 = Uid.ofEmail("user-1@yandex.ru");
    private static final Uid EMAIL_2 = Uid.ofEmail("user-2@yandex.ru");

    private static final Uid UUID_1 = Uid.ofUuid("uuid-1");

    private static final Uid YUID_1 = Uid.ofYuid("111111");

    @Inject
    private SubscriptionsLogConsumer consumer;

    @Inject
    private YtSchemaTestUtils schemaTestUtils;

    @Inject
    private UserTestUtils userTestUtils;

    @Before
    public void setUp() {
        schemaTestUtils.prepareUserTables();
    }

    /**
     * Если приходит строка для которой у нас нет пользователей. Сохраняется новый пользователь
     */
    @Test
    public void testSaveNewUser() {
        consumer.accept(Collections.singletonList(
                subscription(EMAIL_1, PUID_1, null, YUID_1)
        ));

        List<User> users = readUsers();

        assertEquals(1, users.size());

        User user = users.get(0);
        assertNotNull(user.getId());

        assertIdsGraph(
                user,
                edge(PUID_1, EMAIL_1),
                edge(PUID_1, YUID_1),
                edge(YUID_1, EMAIL_1)
        );

        assertUserIds(
                userIdRow(PUID_1, user.getId()),
                userIdRow(EMAIL_1, user.getId()),
                userIdRow(YUID_1, user.getId())
        );
    }

    /**
     * В случае если в одной пачке приходит несколько строк, у которых
     * совпадает puid они объединяются в одного пользователя и сохраняются
     */
    @Test
    public void testGlueTwoNewUsers() {
        consumer.accept(Arrays.asList(
                subscription(EMAIL_1, PUID_1, null, null),
                subscription(EMAIL_2, PUID_1, null, null)
        ));

        List<User> users = readUsers();

        assertEquals(1, users.size());

        User user = users.get(0);
        assertNotNull(user.getId());

        assertIdsGraph(
                user,
                edge(PUID_1, EMAIL_1),
                edge(PUID_1, EMAIL_2)
        );

        assertUserIds(
                userIdRow(PUID_1, user.getId()),
                userIdRow(EMAIL_1, user.getId()),
                userIdRow(EMAIL_2, user.getId())
        );
    }

    /**
     * В случае если приходит строка, содержащая puid с которым у нас уже есть пользователь
     * новый пользователь не создается.
     * Информация из строки просто приписывается существующему пользователю.
     */
    @Test
    public void testMergeNewLineWithExistingUser() {
        User existingUser = new User()
                .setId(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(PUID_1)
                                .addNode(EMAIL_1)
                                .addEdge(0, 1)
                )
                .setDefaultRegion(54);

        saveUsers(existingUser);

        consumer.accept(Collections.singletonList(
                subscription(EMAIL_2, PUID_1, null, null)
        ));

        List<User> users = readUsers();

        assertEquals(1, users.size());

        User user = users.get(0);
        assertEquals(existingUser.getId(), user.getId());
        assertEquals(existingUser.getDefaultRegion(), user.getDefaultRegion());

        assertIdsGraph(
                user,
                edge(PUID_1, EMAIL_1),
                edge(PUID_1, EMAIL_2)
        );

        assertUserIds(
                userIdRow(PUID_1, user.getId()),
                userIdRow(EMAIL_1, user.getId()),
                userIdRow(EMAIL_2, user.getId())
        );
    }

    /**
     * Если приходит строка, содержащая email-адрес уже существующего пользователя,
     * её данные не мерждатся к нему (если между строкой и пользователям больше нет ничего общего).
     * Вместо этого создается новый пользователь.
     */
    @Test
    public void testDoNotMergeNewLineWithExistingUserIfEmailIsOnlyIdInCommon() {
        User existingUser = new User()
                .setId(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(PUID_1)
                                .addNode(EMAIL_1)
                                .addEdge(0, 1)
                );

        saveUsers(existingUser);

        consumer.accept(Arrays.asList(
                subscription(EMAIL_1, PUID_2, null, null),
                subscription(EMAIL_1, PUID_3, null, null)
        ));

        Map<Uid, User> users = readUsers().stream()
                .collect(LiluCollectors.index(user ->
                        user.getIdsGraph().getNodes().stream()
                                .filter(x -> x.getType() == CrmUidType.PUID)
                                .findFirst().orElseThrow()
                ));

        assertEquals(3, users.size());

        User user1 = users.get(PUID_1);
        assertIdsGraph(
                user1,
                edge(PUID_1, EMAIL_1)
        );

        User user2 = users.get(PUID_2);
        assertIdsGraph(
                user2,
                edge(PUID_2, EMAIL_1)
        );

        User user3 = users.get(PUID_3);
        assertIdsGraph(
                user3,
                edge(PUID_3, EMAIL_1)
        );

        assertUserIds(
                userIdRow(PUID_1, user1.getId()),
                userIdRow(PUID_2, user2.getId()),
                userIdRow(PUID_3, user3.getId()),
                userIdRow(EMAIL_1, user1.getId(), user2.getId(), user3.getId())
        );
    }

    /**
     * Если приходят две новые строки, содержащие одинаковый новый email,
     * они не сливаются в одного пользователя, а сохраняются отдельно
     */
    @Test
    public void testDoNotMergeNewLinesIfEmailIsAllTheyHaveInCommon() {
        consumer.accept(Arrays.asList(
                subscription(EMAIL_1, PUID_1, null, null),
                subscription(EMAIL_1, PUID_2, null, null)
        ));

        Map<Uid, User> users = readUsers().stream()
                .collect(LiluCollectors.index(user ->
                        user.getIdsGraph().getNodes().stream()
                                .filter(x -> x.getType() == CrmUidType.PUID)
                                .findFirst().orElseThrow()
                ));

        assertEquals(2, users.size());

        User user1 = users.get(PUID_1);
        assertIdsGraph(
                user1,
                edge(PUID_1, EMAIL_1)
        );

        User user2 = users.get(PUID_2);
        assertIdsGraph(
                user2,
                edge(PUID_2, EMAIL_1)
        );

        assertUserIds(
                userIdRow(PUID_1, user1.getId()),
                userIdRow(PUID_2, user2.getId()),
                userIdRow(EMAIL_1, user1.getId(), user2.getId())
        );
    }

    /**
     * В случае если у нас есть два сохраненных пользователя и приходит страка, которая объединает их,
     * по результатам обработки этой строки должен остаться один пользователь
     */
    @Test
    public void testMergeTwoSavedUsers() {
        User existingUser1 = new User()
                .setId(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(PUID_1)
                                .addNode(EMAIL_1)
                                .addEdge(0, 1)
                );

        User existingUser2 = new User()
                .setId(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(UUID_1)
                                .addNode(EMAIL_1)
                                .addEdge(0, 1)
                );

        saveUsers(existingUser1, existingUser2);

        consumer.accept(Collections.singletonList(
                subscription(EMAIL_2, PUID_1, UUID_1, null)
        ));

        List<User> users = readUsers();

        assertEquals(1, users.size());

        User user = users.get(0);

        assertIdsGraph(
                user,
                edge(PUID_1, EMAIL_1),
                edge(UUID_1, EMAIL_1),
                edge(PUID_1, EMAIL_2),
                edge(UUID_1, EMAIL_2),
                edge(PUID_1, UUID_1)
        );

        assertUserIds(
                userIdRow(PUID_1, user.getId()),
                userIdRow(UUID_1, user.getId()),
                userIdRow(EMAIL_1, user.getId()),
                userIdRow(EMAIL_2, user.getId())
        );
    }

    /**
     * Строки, относящиеся к подписке на красные пуши, пропускаются.
     */
    @Test
    public void testSkipRedPushSubscription() {
        consumer.accept(Arrays.asList(
                subscription(41, EMAIL_1, PUID_1, UUID_1, YUID_1),
                subscription(42, EMAIL_2, PUID_2, null, null),
                subscription(43, EMAIL_1, PUID_3, null, null)
        ));

        List<User> users = readUsers();
        assertTrue("There are users in table", users.isEmpty());
    }

    private void saveUsers(User... users) {
        userTestUtils.saveUsers(users);
    }

    private List<User> readUsers() {
        return userTestUtils.readUsers();
    }

    private void assertUserIds(UserIdRow... expectedRows) {
        userTestUtils.assertUserIds(expectedRows);
    }
}
