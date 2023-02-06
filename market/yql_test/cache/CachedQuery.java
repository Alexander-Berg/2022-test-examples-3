package ru.yandex.market.yql_test.cache;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CachedQuery {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    String hashKey;
    String query;
    String data;

    public CachedQuery() {
    }

    public CachedQuery(String hashKey, String query, String data) {
        this.hashKey = hashKey;
        this.query = query;
        this.data = data;
    }

    public static CachedQuery fromJsonNode(JsonNode node) throws JsonProcessingException {
        return MAPPER.treeToValue(node, CachedQuery.class);
    }

    public static ObjectNode toJsonNode(CachedQuery cachedQuery) {
        return MAPPER.valueToTree(cachedQuery);
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CachedQuery that = (CachedQuery) o;
        return Objects.equals(hashKey, that.hashKey) &&
                Objects.equals(query, that.query) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashKey, query, data);
    }
}
