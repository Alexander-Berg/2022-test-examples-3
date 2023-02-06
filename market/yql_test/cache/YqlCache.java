package ru.yandex.market.yql_test.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static ru.yandex.market.yql_test.utils.YqlIOUtils.write;
import static ru.yandex.market.yql_test.utils.YqlJsonUtils.serializeJson;

public class YqlCache {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TYPE_FIELD = "type";
    private static final CacheEntryType DEFAULT_CACHE_ENTRY_TYPE = CacheEntryType.YQL_QUERY;

    private final String cacheFile;

    private List<CachedQuery> cachedQueries = new ArrayList<>();
    private CachedYtData cachedYtData;
    private boolean changed;

    public YqlCache(InputStream cacheInputStream, @Nullable String cacheFile) {
        this.cacheFile = cacheFile;
        loadCache(cacheInputStream);
    }

    private void loadCache(InputStream cacheInputStream) {
        try {
            JsonNode rootNode = MAPPER.readTree(cacheInputStream);

            if (rootNode != null) {
                for (JsonNode cacheEntryNode : rootNode) {
                    CacheEntryType type = extractCacheEntryType(cacheEntryNode);

                    switch (type) {
                        case YT_DATA:
                            cachedYtData = CachedYtData.fromJsonNode(cacheEntryNode);
                            break;
                        case YQL_QUERY:
                            cachedQueries.add(CachedQuery.fromJsonNode(cacheEntryNode));
                            break;
                        default:
                            throw new IllegalStateException("unknown cache entry type");
                    }
                }
            }

            if (cachedYtData == null) {
                cachedYtData = CachedYtData.emptyCache();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can`t read cache. Create file that is specified in @YqlTest.yqlMock" +
                    " with \"[]\" content ant put near test class.", e);
        }
    }

    private CacheEntryType extractCacheEntryType(JsonNode cacheEntryNode) {
        String typeStr = cacheEntryNode.has(TYPE_FIELD) ?
                cacheEntryNode.get(TYPE_FIELD).textValue().toUpperCase(Locale.ENGLISH) :
                null;
        return typeStr != null ? CacheEntryType.valueOf(typeStr) : DEFAULT_CACHE_ENTRY_TYPE;
    }

    public List<CachedQuery> getCachedQueries() {
        return cachedQueries;
    }

    public void setCachedQueries(Collection<CachedQuery> cachedQueries) {
        this.cachedQueries = new ArrayList<>(cachedQueries != null ? cachedQueries : emptyList());
        this.changed = true;
    }

    public CachedYtData getCachedYtData() {
        return cachedYtData;
    }

    public void setCachedYtData(CachedYtData cachedYtData) {
        this.cachedYtData = cachedYtData;
        this.changed = true;
    }

    public void saveCache() {
        if (!changed) {
            return;
        }
        checkState(cacheFile != null, "ARCADIA_ROOT not found (add to environment)");
        JsonNode cacheRootNode = buildCacheRootNode();
        write(serializeJson(cacheRootNode), cacheFile);
    }

    private JsonNode buildCacheRootNode() {
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        cachedQueries.forEach(query -> {
            ObjectNode yqlQueryNode = CachedQuery.toJsonNode(query);
            yqlQueryNode.set(TYPE_FIELD, new TextNode(CacheEntryType.YQL_QUERY.toString()));
            arrayNode.add(yqlQueryNode);
        });

        ObjectNode ytDataNode = CachedYtData.toJsonNode(cachedYtData);
        ytDataNode.set(TYPE_FIELD, new TextNode(CacheEntryType.YT_DATA.toString()));
        arrayNode.add(ytDataNode);

        return arrayNode;
    }

    public void clearCache() {
        write("[]", cacheFile);
    }
}
