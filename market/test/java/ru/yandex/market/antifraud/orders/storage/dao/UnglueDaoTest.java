package ru.yandex.market.antifraud.orders.storage.dao;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerIdType;
import ru.yandex.market.antifraud.orders.storage.entity.unglue.UnglueEdge;
import ru.yandex.market.antifraud.orders.storage.entity.unglue.UnglueEntryOperation;
import ru.yandex.market.antifraud.orders.storage.entity.unglue.UnglueNode;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UnglueDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;


    @Test
    public void findNode() {
        UnglueDao unglueDao = new UnglueDao(jdbcTemplate);
        Instant now = Instant.now();
        UnglueNode node = UnglueNode.builder()
                .nodeValue("12345")
                .nodeType(BuyerIdType.UID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        node = unglueDao.saveNode(node);
        List<UnglueNode> findResult = unglueDao.findNode(node.getNodeType(), node.getNodeValue());
        assertThat(findResult).contains(node);
    }


    @Test
    public void findEdge() {
        UnglueDao unglueDao = new UnglueDao(jdbcTemplate);
        Instant now = Instant.now();
        UnglueEdge edge = UnglueEdge.builder()
                .node1Value("12345")
                .node1Type(BuyerIdType.UID)
                .node2Value("12345-uuid")
                .node2Type(BuyerIdType.UUID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        edge = unglueDao.saveEdge(edge);
        List<UnglueEdge> findResult = unglueDao.findEdgeByNode(edge.getNode1Type(), edge.getNode1Value());
        assertThat(findResult).contains(edge);
    }

    @Test
    public void checkNodeUnglued() {
        UnglueDao unglueDao = new UnglueDao(jdbcTemplate);
        Instant now = Instant.now();
        UnglueNode node = UnglueNode.builder()
                .nodeValue("23456")
                .nodeType(BuyerIdType.UID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        node = unglueDao.saveNode(node);
        Optional<UnglueNode> nodeO = unglueDao.checkNodeUnglued(MarketUserId.fromUid(23456L));
        assertThat(nodeO).isPresent();
        assertThat(nodeO.get()).isEqualTo(node);
    }

    @Test
    public void checkEdgeUnglued() {
        UnglueDao unglueDao = new UnglueDao(jdbcTemplate);
        Instant now = Instant.now();
        UnglueEdge edge = UnglueEdge.builder()
                .node1Value("333444")
                .node1Type(BuyerIdType.UID)
                .node2Value("uuid-333444")
                .node2Type(BuyerIdType.UUID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        edge = unglueDao.saveEdge(edge);
        Optional<UnglueEdge> edgeO = unglueDao.checkEdgeUnglued(
                MarketUserId.fromUuid("uuid-333444"),
                MarketUserId.fromUid(333444L)
        );
        assertThat(edgeO).isPresent();
        assertThat(edgeO.get()).isEqualTo(edge);

        now = Instant.now().plusSeconds(1L);
        UnglueEdge edge1 = edge.toBuilder()
                .id(null)
                .operation(UnglueEntryOperation.REMOVE)
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .build();
        unglueDao.saveEdge(edge1);

        Optional<UnglueEdge> edgeO1 = unglueDao.checkEdgeUnglued(
                MarketUserId.fromUid(333444L),
                MarketUserId.fromUuid("uuid-333444")
        );
        assertThat(edgeO1).isEmpty();
    }

    @Test
    public void getAllActiveNodes() {
        UnglueDao unglueDao = new UnglueDao(jdbcTemplate);
        Instant now = Instant.now();
        UnglueNode node1 = UnglueNode.builder()
                .nodeValue("34567")
                .nodeType(BuyerIdType.UID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        node1 = unglueDao.saveNode(node1);
        now = now.plusSeconds(1L);
        UnglueNode node2 = node1.toBuilder()
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.REMOVE)
                .build();
        node2 = unglueDao.saveNode(node2);
        now = now.plusSeconds(1L);
        UnglueNode node3 = UnglueNode.builder()
                .nodeValue("34568")
                .nodeType(BuyerIdType.UID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        node3 = unglueDao.saveNode(node3);
        now = now.plusSeconds(1L);
        UnglueNode node4 = UnglueNode.builder()
                .nodeValue("34569")
                .nodeType(BuyerIdType.UID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        node4 = unglueDao.saveNode(node4);

        List<UnglueNode> nodes = unglueDao.getAllActiveNodes();
        assertThat(nodes).contains(node3, node4);
        assertThat(nodes).doesNotContain(node1, node2);
    }

    @Test
    public void getAllActiveEdges() {
        UnglueDao unglueDao = new UnglueDao(jdbcTemplate);
        Instant now = Instant.now();
        UnglueEdge edge1 = UnglueEdge.builder()
                .node1Value("444555")
                .node1Type(BuyerIdType.UID)
                .node2Value("uuid-444555")
                .node2Type(BuyerIdType.UUID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        edge1 = unglueDao.saveEdge(edge1);
        now = now.plusSeconds(1L);
        UnglueEdge edge2 = edge1.toBuilder()
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.REMOVE)
                .build();
        edge2 = unglueDao.saveEdge(edge2);
        now = now.plusSeconds(1L);
        UnglueEdge edge3 = UnglueEdge.builder()
                .node1Value("444556")
                .node1Type(BuyerIdType.UID)
                .node2Value("uuid-444556")
                .node2Type(BuyerIdType.UUID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        edge3 = unglueDao.saveEdge(edge3);
        now = now.plusSeconds(1L);
        UnglueEdge edge4 = UnglueEdge.builder()
                .node1Value("444557")
                .node1Type(BuyerIdType.UID)
                .node2Value("uuid-444557")
                .node2Type(BuyerIdType.UUID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();
        edge4 = unglueDao.saveEdge(edge4);

        List<UnglueEdge> nodes = unglueDao.getAllActiveEdges();
        assertThat(nodes).contains(edge3, edge4);
        assertThat(nodes).doesNotContain(edge1, edge2);
    }

    @Test
    public void saveNode() {
        Instant now = Instant.now();

        UnglueDao dao = new UnglueDao(jdbcTemplate);
        UnglueNode node1Card = UnglueNode.builder()
                .nodeValue("1")
                .nodeType(BuyerIdType.CARD)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();

        UnglueNode node2Uid = UnglueNode.builder()
                .nodeValue("2")
                .nodeType(BuyerIdType.UID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();

        UnglueNode node3Uuid = UnglueNode.builder()
                .nodeValue("3")
                .nodeType(BuyerIdType.UUID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();

        UnglueNode node4CryptaId = UnglueNode.builder()
                .nodeValue("4")
                .nodeType(BuyerIdType.CRYPTA_ID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();

        UnglueNode node5Yandexuid = UnglueNode.builder()
                .nodeValue("5")
                .nodeType(BuyerIdType.YANDEXUID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();

        dao.saveNode(node1Card);
        dao.saveNode(node2Uid);
        dao.saveNode(node3Uuid);
        dao.saveNode(node4CryptaId);
        dao.saveNode(node5Yandexuid);

        assertThat(getNodeType("1")).isEqualTo(MarketUserId.CARD);
        assertThat(getNodeType("2")).isEqualTo(MarketUserId.UID_STR);
        assertThat(getNodeType("3")).isEqualTo(MarketUserId.UUID_STR);
        assertThat(getNodeType("4")).isEqualTo(MarketUserId.CRYPTA_ID_STR);
        assertThat(getNodeType("5")).isEqualTo(MarketUserId.YANDEX_UID_STR);
    }

    @Test
    public void saveEdges() {
        Instant now = Instant.now();

        UnglueDao dao = new UnglueDao(jdbcTemplate);
        UnglueEdge edge1CardToUid = UnglueEdge.builder()
                .node1Value("11")
                .node1Type(BuyerIdType.CARD)
                .node2Value("12")
                .node2Type(BuyerIdType.UID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();

        UnglueEdge edge2UuidToCryptaId = UnglueEdge.builder()
                .node1Value("21")
                .node1Type(BuyerIdType.UUID)
                .node2Value("22")
                .node2Type(BuyerIdType.CRYPTA_ID)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();

        UnglueEdge edge3YandexuidToCard = UnglueEdge.builder()
                .node1Value("31")
                .node1Type(BuyerIdType.YANDEXUID)
                .node2Value("32")
                .node2Type(BuyerIdType.CARD)
                .author("author")
                .description("description")
                .createdAt(now.toEpochMilli())
                .createdAtStr(now.toString())
                .operation(UnglueEntryOperation.ADD)
                .build();

        dao.saveEdge(edge1CardToUid);
        dao.saveEdge(edge2UuidToCryptaId);
        dao.saveEdge(edge3YandexuidToCard);

        var edge1 = getEdgeType(Pair.of("11", "12"));
        var edge2 = getEdgeType(Pair.of("21", "22"));
        var edge3 = getEdgeType(Pair.of("31", "32"));

        assertThat(edge1).extracting(Pair::getKey).isEqualTo(MarketUserId.CARD);
        assertThat(edge1).extracting(Pair::getValue).isEqualTo(MarketUserId.UID_STR);

        assertThat(edge2).extracting(Pair::getKey).isEqualTo(MarketUserId.UUID_STR);
        assertThat(edge2).extracting(Pair::getValue).isEqualTo(MarketUserId.CRYPTA_ID_STR);

        assertThat(edge3).extracting(Pair::getKey).isEqualTo(MarketUserId.YANDEX_UID_STR);
        assertThat(edge3).extracting(Pair::getValue).isEqualTo(MarketUserId.CARD);
    }

    private String getNodeType(String nodeId) {
        return jdbcTemplate.query("SELECT node_type FROM unglue_nodes WHERE node_value = :nodeId",
                ImmutableMap.<String, String>builder().put("nodeId", nodeId).build(),
                (ResultSet rs, int num) -> rs.getString("node_type")).get(0);
    }

    private Pair<String, String> getEdgeType(Pair<String, String> edgeIds) {
        return jdbcTemplate.query("SELECT node1_type, node2_type FROM unglue_edges " +
                        "WHERE node1_value = :node1Id AND node2_value = :node2Id",
                ImmutableMap.<String, String>builder()
                        .put("node1Id", edgeIds.getKey())
                        .put("node2Id", edgeIds.getValue())
                        .build(),
                (ResultSet rs, int num) -> Pair.of(
                        rs.getString("node1_type"),
                        rs.getString("node2_type"))
                )
                .get(0);
    }

}
