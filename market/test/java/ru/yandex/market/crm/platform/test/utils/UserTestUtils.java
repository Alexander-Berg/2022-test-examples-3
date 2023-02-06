package ru.yandex.market.crm.platform.test.utils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.object.YTreeSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializerFactory;
import ru.yandex.market.crm.platform.domain.user.IdsGraph;
import ru.yandex.market.crm.platform.domain.user.Uid;
import ru.yandex.market.crm.platform.domain.user.User;
import ru.yandex.market.crm.platform.domain.user.UserIdRow;
import ru.yandex.market.crm.platform.yt.KvStorageClient;
import ru.yandex.market.crm.platform.yt.YtTables;
import ru.yandex.market.crm.platform.yt.YtUtils;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author apershukov
 */
@Component
public class UserTestUtils {

    public static class NodePair {
        final Uid node1;
        final Uid node2;

        NodePair(Uid node1, Uid node2) {
            this.node1 = node1;
            this.node2 = node2;
        }
    }

    public static UserIdRow userIdRow(Uid uid, String... userIds) {
        UserIdRow row = new UserIdRow();
        row.setUid(uid);
        row.setIds(ImmutableSet.copyOf(userIds));
        return row;
    }

    public static NodePair edge(Uid node1, Uid node2) {
        return new NodePair(node1, node2);
    }

    public static void assertIdsGraph(User user, NodePair... expectedEdges) {
        IdsGraph graph = user.getIdsGraph();
        assertNotNull("Graph is null", graph);

        List<IdsGraph.Edge> edges = graph.getEdges();
        assertEquals("Graph has unexpected number of edges", edges.size(), expectedEdges.length);

        for (NodePair expectedEdge : expectedEdges) {
            List<Uid> nodes = graph.getNodes();

            Uid node1 = expectedEdge.node1;
            int index1 = nodes.indexOf(node1);
            assertTrue("Graph doesn't contain node " + node1, index1 >= 0);

            Uid node2 = expectedEdge.node2;
            int index2 = nodes.indexOf(node2);
            assertTrue("Graph doesn't contain node " + node2, index2 >= 0);

            boolean edgeFound = edges.stream()
                    .anyMatch(edge ->
                            edge.getNode1() == index1 && edge.getNode2() == index2 ||
                                    edge.getNode1() == index2 && edge.getNode2() == index1
                    );

            assertTrue("Edge between nodes " + node1 + " and " + node2, edgeFound);
        }
    }

    private static final TableSchema USER_SCHEMA = YtUtils.loadSchema("/yt/schemas/users.yson");
    private static final TableSchema USER_IDS_SCHEMA = YtUtils.loadSchema("/yt/schemas/user_ids.yson");

    private static final YTreeSerializer<User> USER_SERIALIZER = YTreeObjectSerializerFactory.forClass(User.class);

    private static final YTreeSerializer<UserIdRow> USER_ID_SERIALIZER =
            YTreeObjectSerializerFactory.forClass(UserIdRow.class);

    private final KvStorageClient kvStorageClient;
    private final YtTables ytTables;
    private final YtClient ytClient;

    public UserTestUtils(KvStorageClient kvStorageClient, YtTables ytTables, YtClient ytClient) {
        this.kvStorageClient = kvStorageClient;
        this.ytTables = ytTables;
        this.ytClient = ytClient;
    }

    public void saveUsers(User... users) {
        kvStorageClient.doInTx(tx -> {
            ModifyRowsRequest insertUsersRequest = new ModifyRowsRequest(ytTables.getUsers().toString(), USER_SCHEMA);

            Stream.of(users)
                    .map(user -> YtUtils.serialize(user, USER_SERIALIZER))
                    .forEach(insertUsersRequest::addInsert);

            ModifyRowsRequest insertUserIdsRequest = new ModifyRowsRequest(
                    ytTables.getUserIds().toString(),
                    USER_IDS_SCHEMA
            );

            Stream.of(users)
                    .flatMap(user ->
                            user.getIdsGraph().getNodes().stream()
                                    .map(node -> {
                                        UserIdRow row = new UserIdRow();
                                        row.setUid(node);
                                        row.setIds(Collections.singleton(user.getId()));
                                        return row;
                                    })
                    )
                    .collect(Collectors.groupingBy(UserIdRow::getUid)).values().stream()
                    .map(group -> {
                        if (group.size() > 1) {
                            UserIdRow row = new UserIdRow();
                            row.setUid(group.get(0).getUid());
                            row.setIds(
                                    group.stream()
                                            .flatMap(x -> x.getIds().stream())
                                            .collect(Collectors.toSet())
                            );
                            return row;
                        } else {
                            return group.get(0);
                        }
                    })
                    .map(x -> YtUtils.serialize(x, USER_ID_SERIALIZER))
                    .forEach(insertUserIdsRequest::addInsert);

            return CompletableFuture.allOf(
                    tx.modifyRows(insertUsersRequest),
                    tx.modifyRows(insertUserIdsRequest)
            );
        }).join();
    }

    public List<User> readUsers() {
        return readAllRows(ytTables.getUsers(), USER_SERIALIZER);
    }

    public void assertUserIds(UserIdRow... expectedRows) {
        List<UserIdRow> userIdRows = readAllRows(ytTables.getUserIds(), USER_ID_SERIALIZER);
        assertEquals("Unexpected number of user id rows", expectedRows.length, userIdRows.size());

        for (UserIdRow expectedRow : expectedRows) {
            UserIdRow row = userIdRows.stream()
                    .filter(x -> Objects.equals(x.getUid(), expectedRow.getUid()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No row for " + expectedRow.getUid()));

            assertEquals("Unexpected ids for " + expectedRow.getUid(), expectedRow.getIds(), row.getIds());
        }
    }

    private <T> List<T> readAllRows(YPath path, YTreeSerializer<T> serializer) {
        return ytClient.selectRows("* FROM [" + path + "]").join()
                .getYTreeRows().stream()
                .map(serializer::deserialize)
                .collect(Collectors.toList());
    }
}
