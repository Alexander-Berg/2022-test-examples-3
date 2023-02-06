package ru.yandex.market.yql_test.proxy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import ru.yandex.market.yql_test.YqlTablePathConverter;
import ru.yandex.market.yql_test.cache.CachedQuery;
import ru.yandex.market.yql_test.cache.YqlCache;

public class YqlResponseStorage {

    private final YqlCache yqlCache;
    private final String hashKey;
    private final YqlTablePathConverter yqlTablePathConverter;
    private final List<CachedQuery> allCacheQueries;
    private final Set<CachedQuery> usedCacheQueries = new HashSet<>();
    private final Set<CachedQuery> extractedYTQueries = new HashSet<>();

    public YqlResponseStorage(YqlCache yqlCache,
                              YqlTablePathConverter yqlTablePathConverter,
                              Map<String, String> schemas,
                              String csvContent) {
        this.yqlCache = yqlCache;
        this.yqlTablePathConverter = yqlTablePathConverter;
        this.hashKey = calculateHashKey(csvContent, schemas);
        this.allCacheQueries = yqlCache.getCachedQueries();
    }

    public boolean isUsedCacheQueriesEmpty() {
        return usedCacheQueries.isEmpty();
    }

    public void save(String query, String data) {
        extractedYTQueries.add(new CachedQuery(hashKey, yqlTablePathConverter.convertTestPathToNormal(query), data));
    }

    public Optional<String> getResponse(String query) {
        for (CachedQuery yqlQuery : allCacheQueries) {
            if (hashKey.equals(yqlQuery.getHashKey())
                    && yqlTablePathConverter.convertTestPathToNormal(query).equals(yqlQuery.getQuery())) {
                usedCacheQueries.add(yqlQuery);
                return Optional.ofNullable(yqlQuery.getData());
            }
        }
        return Optional.empty();
    }

    public void flush(boolean removeNotUsedQueries) {
        if (extractedYTQueries.isEmpty() && usedCacheQueries.size() == allCacheQueries.size()) {
            return;
        }
        var queries = new HashSet<>(removeNotUsedQueries ? usedCacheQueries : allCacheQueries);
        queries.addAll(extractedYTQueries);
        yqlCache.setCachedQueries(queries);
    }

    private String calculateHashKey(String csvContent, Map<String, String> schemas) {
        StringBuffer hashable = new StringBuffer(csvContent);
        schemas.keySet().stream().sorted().forEach(key -> {
            hashable.append(key);
            hashable.append(schemas.get(key));
        });
        return DigestUtils.md5Hex(hashable.toString());
    }
}
