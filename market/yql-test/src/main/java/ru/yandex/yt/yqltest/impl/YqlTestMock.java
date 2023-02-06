package ru.yandex.yt.yqltest.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.yt.yqltestable.YqlTestable;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.08.2021
 */
public class YqlTestMock {
    private final YPath basePath;

    private final Map<String, YqlTestMockTable> tableMockMap = new LinkedHashMap<>();
    private final Map<String, YqlTestMockTable> varTableMockMap = new LinkedHashMap<>();
    private final Map<String, YqlTestMockDir> dirMockMap = new LinkedHashMap<>();
    private final Map<String, String> varMockMap = new LinkedHashMap<>();

    private final List<YqlTestMockTable> tablesToCreate = new ArrayList<>();
    private final List<YqlTestMockTable> tablesToMock = new ArrayList<>();
    private final List<YPath> pathsToCleanup = new ArrayList<>();

    private boolean isInit = false;

    public YqlTestMock(YPath basePath) {
        this.basePath = basePath;
    }

    public void init() {
        if (isInit) {
            throw new IllegalStateException("Already initialized");
        }

        // store tables as $mockpath/uuid
        for (YqlTestMockTable table : tableMockMap.values()) {
            table.init(basePath);
            tablesToCreate.add(table);
            tablesToMock.add(table);
            pathsToCleanup.add(table.getMockPath());
        }

        for (YqlTestMockTable table : varTableMockMap.values()) {
            table.init(basePath);
            tablesToCreate.add(table);
            tablesToMock.add(table);
            pathsToCleanup.add(table.getMockPath());
        }

        // store dir tables as $mockpath/uuid/tablename
        for (YqlTestMockDir dir : dirMockMap.values()) {
            dir.init(basePath);
            for (YqlTestMockTable table : dir.getTables().values()) {
                table.setMockPath(dir.getMockPath().child(table.getName()));
                tablesToCreate.add(table);
            }

            pathsToCleanup.add(dir.getMockPath());
        }

        isInit = true;
    }

    public String mockYql(String yql) {
        // insert some useful pragma settings to simplify tests
        yql = "PRAGMA yt.DefaultMaxJobFails=\"1\";\n\n" + yql;

        // rename orig mockable header functions
        yql = yql.replace("DEFINE SUBQUERY $_table", "DEFINE SUBQUERY $_table_orig")
            .replace("$_dir = ", "$_dir_orig = ");

        String mockHeader = YqlTestable.readFile("/lib_yql_test/lib_yql_mock.sql")
            .replace("_TABLENAME_", buildTableMapping(tablesToMock))
            .replace("_DIRNAME_", buildDirMapping(dirMockMap.values()));
        yql = mockHeader + yql;
        yql = yql.replace(YqlTestable.INLINE_KEY, "with inline");

        // rename mocked variables
        for (YqlTestMockTable varTable : varTableMockMap.values()) {
            String varName = varTable.getName();
            yql = yql.replace("$" + varName + " =",
                "$" + varName + " = (select * from $_table('" + varTable.getPath() + "'));\n" +
                    "$" + varName + "___mocked =");
        }

        // rename mocked variables
        for (String varName : varMockMap.keySet()) {
            yql = yql.replace("$" + varName + " =",
                "$" + varName + " = " + varMockMap.get(varName) + ";\n" +
                    "$" + varName + "___mocked =");
        }

        return yql;
    }

    private String buildTableMapping(List<YqlTestMockTable> mapping) {
        return buildMapping("tablename", mapping);
    }

    private String buildDirMapping(Collection<YqlTestMockDir> mapping) {
        return buildMapping("path", mapping);
    }

    private String buildMapping(String prop, Collection<? extends YqlTestMockObject> mapping) {
        if (mapping.isEmpty()) {
            return "'empty_mock-'||$" + prop;
        }
        StringBuilder builder = new StringBuilder("case\n");
        mapping.forEach((value) ->
            builder.append("    when $").append(prop).append(" = '")
                .append(value.getPath())
                .append("' then '")
                .append(value.getMockPath())
                .append("'\n"));
        builder.append("    else 'unknown-'||$").append(prop).append(" end\n");
        return builder.toString();
    }

    public Map<String, YqlTestMockTable> getTableMockMap() {
        return tableMockMap;
    }

    public Map<String, YqlTestMockTable> getVarTableMockMap() {
        return varTableMockMap;
    }

    public Map<String, YqlTestMockDir> getDirMockMap() {
        return dirMockMap;
    }

    public Map<String, String> getVarMockMap() {
        return varMockMap;
    }

    public List<YqlTestMockTable> getTablesToCreate() {
        return tablesToCreate;
    }

    public List<YPath> getPathsToCleanup() {
        return pathsToCleanup;
    }
}
