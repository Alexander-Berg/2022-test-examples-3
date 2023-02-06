package ru.yandex.market.yql_test;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;

@Component
public class YqlTablePathConverter {
    public static final String TESTS_SUBFOLDER = "yql-test";

    private final YPath testYtDir;

    public YqlTablePathConverter(@Value("${yql.test.table.prefix}") String testTablePrefix) {
        this.testYtDir = YPath.simple(testTablePrefix).child(TESTS_SUBFOLDER).child(UUID.randomUUID().toString());
    }

    public YPath toTestPath(String ytTablePath) {
        return testYtDir.child(ytTablePath.substring(2));
    }

    public YPath getTestYtDir() {
        return testYtDir;
    }

    public String convertTestPathToNormal(String query) {
        return query.replaceAll(testYtDir.toString(), "");
    }
}
