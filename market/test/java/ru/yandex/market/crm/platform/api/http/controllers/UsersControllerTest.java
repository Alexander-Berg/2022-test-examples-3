package ru.yandex.market.crm.platform.api.http.controllers;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.api.Edge;
import ru.yandex.market.crm.platform.api.UsersResponse;
import ru.yandex.market.crm.platform.api.test.AbstractControllerTest;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.domain.user.IdsGraph;
import ru.yandex.market.crm.platform.domain.user.Uid;
import ru.yandex.market.crm.platform.domain.user.User;
import ru.yandex.market.crm.platform.test.utils.UserTestUtils;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * @author apershukov
 */
public class UsersControllerTest extends AbstractControllerTest {

    @Inject
    private YtSchemaTestUtils schemaTestUtils;

    @Inject
    private UserTestUtils userTestUtils;

    @Before
    public void setUp() {
        schemaTestUtils.prepareUserTables();
    }

    /**
     * Если искомый пользователь не найден возвращается пустой список
     */
    @Test
    public void testReturnEmptyResponseIfNoUserIsFound() throws Exception {
        UsersResponse response = requestUsersById();

        assertNotNull(response);
        assertTrue(response.getUserList().isEmpty());

        response = requestUsersByEmail();

        assertNotNull(response);
        assertTrue(response.getUserList().isEmpty());
    }

    /**
     * Если у пользователь есть указанный идентификатор, он попадает в выдачу ручки
     */
    @Test
    public void testReturnUserIfHeHasSpecifiedId() throws Exception {
        prepareUsers();

        UsersResponse response = requestUsersById();

        assertThatUserFound(response);
    }

    /**
     * Если у пользователь есть указанный email, он попадает в выдачу ручки
     */
    @Test
    public void testReturnUserIfHeHasSpecifiedEmail() throws Exception {
        prepareUsers();

        UsersResponse response = requestUsersByEmail();

        assertThatUserFound(response);
    }

    private void prepareUsers() {
        User user1 = new User()
                .setId(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.ofPuid(111))
                                .addNode(Uid.ofEmail("user_1@yandex.ru"))
                                .addEdge(0, 1)
                );

        User user2 = new User()
                .setId(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.ofPuid(222))
                                .addNode(Uid.ofEmail("user_2@yandex.ru"))
                                .addEdge(0, 1)
                );

        userTestUtils.saveUsers(user1, user2);
    }

    private void assertThatUserFound(UsersResponse response) {
        List<ru.yandex.market.crm.platform.api.User> users = response.getUserList();
        assertEquals(1, users.size());

        ru.yandex.market.crm.platform.api.User user = users.get(0);

        assertFalse("User internal id is not specified", Strings.isNullOrEmpty(user.getId()));

        List<ru.yandex.market.crm.platform.commons.Uid> nodes = user.getIdsGraph().getNodeList();
        assertEquals(2, nodes.size());

        ru.yandex.market.crm.platform.commons.Uid puidNode = ru.yandex.market.crm.platform.commons.Uid.newBuilder()
                .setType(UidType.PUID)
                .setIntValue(111)
                .build();

        assertTrue(nodes.contains(puidNode));

        ru.yandex.market.crm.platform.commons.Uid emailNode = ru.yandex.market.crm.platform.commons.Uid.newBuilder()
                .setType(UidType.EMAIL)
                .setStringValue("user_1@yandex.ru")
                .build();

        assertTrue(nodes.contains(emailNode));

        assertEquals(1, user.getIdsGraph().getEdgeCount());

        Edge edge = user.getIdsGraph().getEdge(0);
        assertEquals(0, edge.getNode1());
        assertEquals(1, edge.getNode2());
    }

    private UsersResponse requestUsersById() throws Exception {
        return requestUsers(UidType.PUID, "111");
    }

    private UsersResponse requestUsersByEmail() throws Exception {
        return requestUsers(UidType.EMAIL, "user_1@yandex.ru");
    }

    private UsersResponse requestUsers(UidType idType, String idValue) throws Exception {
        return request(
                get("/users/{idType}/{idValue}", idType.toString().toLowerCase(), idValue),
                UsersResponse::parseFrom
        );
    }
}
