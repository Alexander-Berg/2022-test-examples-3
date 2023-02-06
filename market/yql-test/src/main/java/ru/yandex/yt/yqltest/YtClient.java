package ru.yandex.yt.yqltest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.bolts.collection.CollectorsF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.TableReaderOptions;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.TransactionalOptions;
import ru.yandex.yt.ytclient.tables.TableSchema;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 06.09.2021
 */
public class YtClient {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Yt yt;

    public YtClient(Yt yt) {
        this.yt = yt;
    }

    public List<JsonNode> readNodes(YPath ytPath) {
        try (CloseableIterator<JsonNode> iterator = yt.tables().read(
            Optional.empty(),
            false,
            ytPath,
            YTableEntryTypes.JACKSON_UTF8,
            new TableReaderOptions())) {
            return iterator.stream()
                .collect(CollectorsF.toArrayList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read table " + ytPath, e);
        }
    }

    public void createTable(YPath ytPath, TableSchema schema) {
        if (schema == null) {
            schema = TableSchema.builder()
                .setUniqueKeys(false)
                .setStrict(false)
                .build();
        }

        Map<String, YTreeNode> attributes = Collections.singletonMap("schema", schema.toYTree());

        if (exists(ytPath)) {
            remove(ytPath);
        }

        yt.cypress().create(new CreateNode(ytPath, CypressNodeType.TABLE, attributes)
            .setTransactionalOptions(new TransactionalOptions((GUID) null))
            .setRecursive(true)
            .setIgnoreExisting(false)
        );
    }

    public void append(YPath ytPath, List<?> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        yt.tables().write(
            Optional.empty(),
            false,
            ytPath.append(true),
            YTableEntryTypes.JACKSON_UTF8,
            items.stream()
                .filter(Objects::nonNull)
                .map(x -> (JsonNode) MAPPER.valueToTree(x)).iterator()
        );
    }

    public boolean exists(YPath ytPath) {
        return yt.cypress().exists(ytPath);
    }

    public void remove(YPath ytPath) {
        if (exists(ytPath)) {
            yt.cypress().remove(Optional.empty(), false, ytPath);
        }
    }
}
