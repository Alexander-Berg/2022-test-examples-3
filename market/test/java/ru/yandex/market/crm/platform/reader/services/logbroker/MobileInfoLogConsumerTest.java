package ru.yandex.market.crm.platform.reader.services.logbroker;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.market.crm.platform.domain.user.IdsGraph;
import ru.yandex.market.crm.platform.domain.user.Uid;
import ru.yandex.market.crm.platform.domain.user.User;
import ru.yandex.market.crm.platform.reader.test.AbstractServiceTest;
import ru.yandex.market.crm.platform.reader.yt.YtClients;
import ru.yandex.market.crm.platform.test.utils.UserTestUtils;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.platform.yt.YtTables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.platform.test.utils.UserTestUtils.assertIdsGraph;
import static ru.yandex.market.crm.platform.test.utils.UserTestUtils.edge;
import static ru.yandex.market.crm.platform.test.utils.UserTestUtils.userIdRow;

/**
 * @author apershukov
 */
public class MobileInfoLogConsumerTest extends AbstractServiceTest {

    private Map<String, String> line(Uid puid, Uid yuid, Long geoId) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                .put("UUID", MobileInfoLogConsumerTest.UUID.getValue());

        if (puid != null) {
            builder.put("PUID", puid.getValue());
        }

        if (yuid != null) {
            builder.put("YANDEX_UID", yuid.getValue());
        }

        if (geoId != null) {
            builder.put("GEO_ID", geoId.toString());
        }

        return builder.build();
    }

    private static final Uid UUID = Uid.ofUuid("uuid-111");
    private static final Uid PUID = Uid.ofPuid(111);
    private static final Uid YUID = Uid.ofYuid("111");

    @Inject
    private YtSchemaTestUtils ytSchemaTestUtils;

    @Inject
    private UserTestUtils userTestUtils;

    @Inject
    private MobileInfoLogConsumer consumer;

    @Inject
    private YtTables ytTables;

    @Inject
    private YtClients ytClients;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        ytSchemaTestUtils.prepareUserTables();
    }

    /**
     * В случае если приходит строка для которой у нас нет пользователя
     * сохраняется новый пользователь
     */
    @Test
    public void testSaveNewUser() {
        consumer.accept(Collections.singletonList(
                line(PUID, YUID, 54L)
        ));

        List<User> users = userTestUtils.readUsers();
        assertEquals(1, users.size());

        User user = users.get(0);
        assertEquals(54, user.getDefaultRegion());

        assertIdsGraph(
                user,
                edge(UUID, PUID),
                edge(UUID, YUID),
                edge(YUID, PUID)
        );

        userTestUtils.assertUserIds(
                userIdRow(UUID, user.getId()),
                userIdRow(PUID, user.getId()),
                userIdRow(YUID, user.getId())
        );
    }

    /**
     * В случае если приходит строка с полностью вылогиненным пользователем
     * для которой у нас уже есть пользователь со связями после обработки
     * строки имеющиеся данные не пропадают
     */
    @Test
    public void testProcessLoggedOutUser() {
        User existingUser = new User()
                .setId(java.util.UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(UUID)
                                .addNode(PUID)
                                .addEdge(0, 1)
                )
                .setDefaultRegion(54);

        userTestUtils.saveUsers(existingUser);

        consumer.accept(Collections.singletonList(
                line(null, null, null)
        ));

        List<User> users = userTestUtils.readUsers();
        assertEquals(1, users.size());

        User user = users.get(0);
        assertEquals(54, user.getDefaultRegion());

        assertIdsGraph(
                user,
                edge(UUID, PUID)
        );

        userTestUtils.assertUserIds(
                userIdRow(UUID, user.getId()),
                userIdRow(PUID, user.getId())
        );
    }

    /**
     * Искусственный yuid-заглушка игнорируется
     */
    @Test
    public void testProcessUserWithFakeYuid() {
        consumer.accept(Collections.singletonList(
                line(PUID, Uid.ofYuid("12345678901234567"), null)
        ));

        List<User> users = userTestUtils.readUsers();
        assertEquals(1, users.size());

        User user = users.get(0);

        assertIdsGraph(
                user,
                edge(UUID, PUID)
        );

        userTestUtils.assertUserIds(
                userIdRow(UUID, user.getId()),
                userIdRow(PUID, user.getId())
        );
    }

    /**
     * В случае если в качестве puid приходит "0" считается что puid
     * не заполнен и он игнорируется
     */
    @Test
    public void testIgnoreZeroAsPuid() {
        consumer.accept(Collections.singletonList(
                line(Uid.ofPuid(0), YUID, 54L)
        ));

        List<User> users = userTestUtils.readUsers();
        assertEquals(1, users.size());

        User user = users.get(0);
        assertEquals(54, user.getDefaultRegion());

        assertIdsGraph(
                user,
                edge(UUID, YUID)
        );
    }

    /**
     * В случае если в процессе обработки батча произошла ошибка из консьюмера
     * выбрасывается исключение
     */
    @Test
    public void testPropagateException() {
        expectedException.expect(Exception.class);

        // Провоцируем ошибку
        ytClients.getMetaCluster().getClient().cypress().remove(ytTables.getUserIds());

        consumer.accept(Collections.singletonList(
                line(PUID, YUID, 54L)
        ));
    }
}
