package ru.yandex.market.mvc.controller.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ObjectUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.security.config.FunctionalTest;
import ru.yandex.market.security.migration.JavaSecMigrator;
import ru.yandex.market.security.migration.model.JavaSecMigrationResult;

/**
 * Тесты {@link MigrationController}.
 */
public class MigrationControllerTest extends FunctionalTest {

    @Autowired
    private String baseUrl;

    @Autowired
    private DataSource dataSource;

    @Test
    @DbUnitDataSet(after = "MigrationControllerTest.migrateTest.after.csv")
    @DbUnitDataSet(before = "MigrationControllerTest.migrateTest.before.csv")
    void migrateTest() {
        final JavaSecMigrator javaSecMigrator = new JavaSecMigrator(baseUrl);
        final JavaSecMigrationResult result = javaSecMigrator.migrate(JavaSecMigrator.createRequest("mbi-admin-test"));
        Assertions.assertEquals(result.getMsg(), "OK");
    }

    static Stream<String> testJavaSecData() {
        return JavaSecMigrator.DOMAINS.stream();
    }

    @MethodSource
    @ParameterizedTest
    void testJavaSecData(String domain) throws Exception {
        final JavaSecMigrator javaSecMigrator = new JavaSecMigrator(baseUrl);
        final JavaSecMigrationResult result = javaSecMigrator.migrate(JavaSecMigrator.createRequest(domain));
        Assertions.assertEquals(result.getMsg(), "OK");

        try (Connection conn = dataSource.getConnection()) {
            checkLoadedData(conn, domain);
        }

    }

    /**
     * Этот метод проверяет, что данные в БД налиты правильно и совпадают с исходником в csv.
     */
    private void checkLoadedData(Connection conn, String domain) throws Exception {
        final String domainFolder = domain.toLowerCase();
        checkLoadedData(conn,
                "select distinct op_name, description from java_sec.op_desc " +
                        "where domain = ? order by 1, 2",
                domain,
                "csv/" + domainFolder + "/op.csv",
                List.of("OP_NAME"));

        checkLoadedData(conn,
                "select distinct auth.name, dsc, checker.checker" +
                        " from java_sec.authority auth" +
                        "  join java_sec.authority_checker checker on auth.id = checker.authority_id" +
                        "  join java_sec.domain on domain.id = auth.domain_id" +
                        " where domain.name = ? order by 1, 2, 3",
                domain,
                "csv/" + domainFolder + "/auth.csv",
                List.of("NAME"));

        checkLoadedData(conn,
                "select distinct main.name as main, linked.name as linked, link.rel, link.linked_auth_param " +
                        "  from java_sec.authority main " +
                        "  join java_sec.auth_link link on link.main_auth_id = main.id" +
                        "  join java_sec.authority linked on link.linked_auth_id = linked.id" +
                        "  join java_sec.domain on main.domain_id = domain.id" +
                        " where domain.name = ? order by 1, 2, 3, 4",
                domain,
                "csv/" + domainFolder + "/auth_links.csv",
                List.of("MAIN", "LINKED", "REL", "LINKED_AUTH_PARAM"));

        checkLoadedData(conn,
                "select distinct opd.op_name, pa.auth_name as name, pa.param" +
                        " from java_sec.perm_auth pa" +
                        "  join java_sec.op_perm op on pa.op_perm_id = op.id" +
                        "  join java_sec.op_desc opd on opd.id = op.op_id" +
                        " where opd.domain = ? order by 1, 2, 3",
                domain,
                "csv/" + domainFolder + "/permissions.csv",
                List.of("OP_NAME", "NAME", "PARAM"));

    }

    private void checkLoadedData(
            Connection conn, String sql, String domain, String file, List<String> uniqueColumns
    ) throws Exception {
        Set<Map<String, String>> csvData = loadCsvData(file, uniqueColumns);

        Set<Map<String, String>> dbData = new HashSet<>();
        try (PreparedStatement prst = conn.prepareStatement(sql)) {
            prst.setString(1, domain);
            ResultSet rs = prst.executeQuery();
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.put(rs.getMetaData().getColumnLabel(i), ObjectUtils.defaultIfNull(rs.getString(i), ""));
                }
                dbData.add(row);
            }
        }

        for (Map<String, String> expected : csvData) {
            Assertions.assertTrue(dbData.contains(expected), expected.toString());
        }
    }

    private Set<Map<String, String>> loadCsvData(String file, List<String> uniqueColumns) throws IOException {
        try (CSVParser csvParser = new CSVParser(new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        JavaSecMigrator.class.getResourceAsStream(file),
                        String.format("File %s not found", file)))),
                CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            List<Map<String, String>> list = csvParser.getRecords().stream()
                    .map(CSVRecord::toMap)
                    .collect(Collectors.toList());

            List<List<String>> uniqueValues = list.stream()
                    .map(data -> uniqueColumns.stream().map(data::get).collect(Collectors.toList()))
                    .collect(Collectors.toList());

            Collection<Object> duplicates = CollectionUtils.subtract(uniqueValues, new HashSet<>(uniqueValues));
            MatcherAssert.assertThat(
                    String.format("Found duplicate entries in %s", file),
                    duplicates, Matchers.emptyIterable());

            return new HashSet<>(list);
        }
    }

}
