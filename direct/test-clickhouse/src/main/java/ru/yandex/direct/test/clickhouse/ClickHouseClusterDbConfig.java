package ru.yandex.direct.test.clickhouse;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.clickhouse.ClickHouseClusterBuilder;
import ru.yandex.direct.clickhouse.ClickHouseContainer;
import ru.yandex.direct.process.DockerContainer;

@ParametersAreNonnullByDefault
public class ClickHouseClusterDbConfig {
    private static final String DEFAULT = "default";
    private static final String CHILDS = "CHILDS";

    private ClickHouseClusterDbConfig() {
    }

    /**
     * Патчит существующий dbconfig.json информацией о кластере и записывает в другой поток.
     * <p>
     * Предположим, {@code injectTo == "ppchouse:ual"}.
     * <p>
     * Таблицы Distributed: Для каждой реплики в каждом шарде будет создан dbconfig со следующим путём:
     * {@code ppchouse:ual:distributed:<метка кластера>:<номер шарда>:<номер реплики>}
     * <p>
     * Таблицы ReplicatedMergeTree: Для каждой реплики в каждом шарде будет создан dbconfig со следующим путём:
     * {@code ppchouse:ual:merge:<метка кластера>:<метка шарда>:<номер реплики>}
     * <p>
     * <b>Нумерация начинается с единицы.</b>
     *
     * @param builder      Информация о связях Distributed и ReplicatedMergeTree таблиц в кластере
     * @param cluster      Уже поднятый из {@code builder}, работающий кластер.
     * @param srcDbConfig  Исходный dbconfig в json
     * @param injectTo     Путь в нотации dbconfig, по которому нужно вставить информацию о кластере
     * @param hostName     Адрес хоста, который будет указан в dbconfig. Если null, то будет заменён на 127.0.0.1 или ::1.
     * @param dbName       Имя БД, которая будет прописана во всех dbconfig
     * @param destDbConfig Куда записать новый dbconfig в json
     */
    public static void generateDbConfig(ClickHouseClusterBuilder builder,
                                        ClickHouseCluster cluster, Reader srcDbConfig, String injectTo,
                                        @Nullable String hostName, String dbName, Writer destDbConfig) throws IOException {
        JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        JsonNode dbConfig = objectMapper.readTree(srcDbConfig);

        List<String> path = new ArrayList<>();
        path.add("db_config");
        for (String label : injectTo.split(":")) {
            path.add(CHILDS);
            path.add(label);
        }
        path.add(null);

        StringBuilder jqPath = new StringBuilder();
        JsonNode node = dbConfig;
        ObjectNode objectNode = null;
        for (String attr : path) {
            try {
                objectNode = (ObjectNode) node;
            } catch (ClassCastException ignored) {
                throw new IllegalArgumentException(String.format(
                        "Can't inject %s: Node with jq path `%s` is not an object",
                        injectTo, jqPath.length() == 0 ? "." : jqPath.toString()));
            }
            if (attr != null) {
                JsonNode childNode = objectNode.get(attr);
                if (childNode == null) {
                    childNode = new ObjectNode(jsonNodeFactory);
                    objectNode.set(attr, childNode);
                }
                jqPath.append('.').append(attr);
                node = childNode;
            }
        }
        if (hostName == null) {
            hostName = cluster.isUseIpv6() ? "[::1]" : "127.0.0.1";
        }
        Objects.requireNonNull(objectNode);
        ObjectNode newNode = clickHouseClusterDbConfigNode(builder, cluster, jsonNodeFactory, hostName, dbName);
        if (objectNode.fieldNames().hasNext() && !(
                objectNode.has(CHILDS) && objectNode.get(CHILDS).has("_"))) {
            // TODO(lagunov) test it
            ObjectNode replaceTo = new ObjectNode(jsonNodeFactory);
            ((ObjectNode) newNode.get(CHILDS)).set("_", replaceTo);
            ((ObjectNode) newNode.get(CHILDS).get("_")).setAll(objectNode);
        }
        Objects.requireNonNull(objectNode).removeAll();
        objectNode.setAll(newNode);

        objectMapper.writeValue(destDbConfig, dbConfig);
    }

    @SuppressWarnings("squid:S1313")   // IP addr hardcode
    private static ObjectNode clickHouseClusterDbConfigNode(ClickHouseClusterBuilder builder,
                                                            ClickHouseCluster cluster, JsonNodeFactory jsonNodeFactory, String hostName, String dbName) {
        ObjectNode root = jsonNodeFactory.objectNode();
        root.set("user", jsonNodeFactory.textNode(DEFAULT));
        root.set("pass", jsonNodeFactory.textNode(""));
        root.set("db", jsonNodeFactory.textNode(dbName));
        root.set("engine", jsonNodeFactory.textNode("clickhouse"));

        ChildMaker childMaker = new ChildMaker(jsonNodeFactory);

        Map<String, ClickHouseContainer> clickHousesByHostName = cluster.getClickHousesStream().collect(
                Collectors.toMap(DockerContainer::getHostname, c -> c));

        ObjectNode shardsNode = childMaker.make(root, CHILDS, "shards", CHILDS);
        builder.shardGroup().getShards().forEach((shardNumber, hosts) -> {
            if (hosts.isEmpty()) {
                return;
            }
            ObjectNode shardRootNode = childMaker.make(shardsNode, Integer.toString(shardNumber), CHILDS);
            ObjectNode replicasNode = null;
            int replicaCounter = 0;
            for (String host : hosts) {
                ObjectNode placeToInsert;
                if (replicaCounter == 0) {
                    placeToInsert = childMaker.make(shardRootNode, "_");
                } else {
                    if (replicasNode == null) {
                        replicasNode = childMaker.make(shardRootNode, "replicas", CHILDS);
                    }
                    placeToInsert = childMaker.make(replicasNode, Integer.toString(replicaCounter));
                }
                ++replicaCounter;
                ClickHouseContainer clickHouse = Objects.requireNonNull(clickHousesByHostName.get(host));
                placeToInsert.set("host", jsonNodeFactory.textNode(hostName));
                placeToInsert.set("port", jsonNodeFactory.numberNode(clickHouse.getHttpHostPort().getPort()));
                placeToInsert.set("read_allowed", jsonNodeFactory.booleanNode(true));
                placeToInsert.set("write_allowed", jsonNodeFactory.booleanNode(true));
            }
        });

        return root;
    }

    static class ChildMaker {
        private JsonNodeFactory jsonNodeFactory;

        ChildMaker(JsonNodeFactory jsonNodeFactory) {
            this.jsonNodeFactory = jsonNodeFactory;
        }

        private ObjectNode make(ObjectNode src, String... attrs) {
            ObjectNode parent = src;
            for (String attr : attrs) {
                ObjectNode node = jsonNodeFactory.objectNode();
                parent.set(attr, node);
                parent = node;
            }
            return parent;
        }
    }
}
