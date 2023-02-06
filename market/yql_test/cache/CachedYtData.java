package ru.yandex.market.yql_test.cache;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dbunit.dataset.IDataSet;

import ru.yandex.market.yql_test.checker.DbUnitCsvWriter;
import ru.yandex.market.yql_test.utils.YqlDbUnitUtils;

public class CachedYtData {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String cacheKey;
    private final IDataSet dataSet;

    public CachedYtData(String cacheKey, IDataSet dataSet) {
        this.cacheKey = cacheKey;
        this.dataSet = dataSet;
    }

    public static CachedYtData fromJsonNode(JsonNode jsonNode) throws JsonProcessingException {
        JsonCache jsonCache = MAPPER.treeToValue(jsonNode, JsonCache.class);
        if (jsonCache.key != null && jsonCache.base64Value != null) {
            String csv = new String(Base64.getDecoder().decode(jsonCache.base64Value), StandardCharsets.UTF_8);
            return new CachedYtData(jsonCache.key, YqlDbUnitUtils.parseCsv(csv));
        } else {
            return emptyCache();
        }
    }

    public static ObjectNode toJsonNode(CachedYtData cachedYtData) {
        String csv = cachedYtData.dataSet != null ?
                new DbUnitCsvWriter().toCsv(cachedYtData.dataSet) :
                null;
        String base64csv = csv != null ?
                Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8)) :
                null;
        JsonCache jsonCache = new JsonCache(cachedYtData.cacheKey, base64csv);
        return MAPPER.valueToTree(jsonCache);
    }

    public static CachedYtData fromDataSet(String cacheKey, IDataSet dataSet) {
        return new CachedYtData(cacheKey, dataSet);
    }

    public static CachedYtData emptyCache() {
        return new CachedYtData(null, null);
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public IDataSet getDataSet() {
        return dataSet;
    }

    private static class JsonCache {
        @JsonProperty("cacheKey")
        String key;

        @JsonProperty("base64Value")
        String base64Value;

        // for parser
        @SuppressWarnings("unused")
        JsonCache() {
        }

        JsonCache(String key, String base64Value) {
            this.key = key;
            this.base64Value = base64Value;
        }
    }
}
