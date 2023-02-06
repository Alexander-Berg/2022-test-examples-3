package ru.yandex.market.yql_test.service;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class QuerySuffixesService {

    private Map<String, String> querySuffixes;

    public String processQuery(String name, String query) {
        if (querySuffixes != null && querySuffixes.containsKey(name)) {
            int omitAfter = query.indexOf("--yql-test-omit-when-using-suffix");
            if (omitAfter > 0) {
                query = query.substring(0, omitAfter);
            }

            // NOTE: without this trimming, trailing whitespace will break next check
            query = query.trim();

            // NOTE: reusing omitAfter as endIndex so as not to run an extra allocation
            omitAfter = query.length() - 1;
            if (query.charAt(omitAfter) == ';') {
                query = query.substring(0, omitAfter);
            }

            return query + querySuffixes.get(name);
        }

        return query;
    }

    public void setQuerySuffixes(Map<String, String> querySuffixes) {
        this.querySuffixes = querySuffixes;
    }

    public boolean hasSuffixes() {
        return querySuffixes != null;
    }

}
