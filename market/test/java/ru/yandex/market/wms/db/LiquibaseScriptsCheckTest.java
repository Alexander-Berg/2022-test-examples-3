package ru.yandex.market.wms.db;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LiquibaseScriptsCheckTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static final String QUERY_FORMAT = "ALTER TABLE %s.%s ADD %s %s";
    private static final Pattern SCHEMA_TABLE_COLUMN_TYPE_PATTERN =
            Pattern.compile("ALTER TABLE (.*)\\.(.*)\\n?.*(?:ADD) (?!CONSTRAINT|DEFAULT|\\()(\\w+) ((\\w+)(\\(.*\\))" +
                    "?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHEMA_ALTER_COLUMN_PATTERN = Pattern.compile(
            "^ALTER TABLE (?:SCPRD\\.)?(?:SCPRDARC\\.)?(.*)\\.(.*) (?:ALTER COLUMN) (\\w+) " +
                    "([^\\s;]+\\s?(NULL)?(NOT NULL)?)",
                   Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final List<String> TABLES_IN_BOTH_DBS = List.of(
            "LOTXIDHEADER",
            "ITRNSERIAL",
            "LOTXIDDETAIL",
            "INVENTORYCOUNT_LOG",
            "ITRN",
            "HOLDTRN",
            "RECEIPTDETAILSTATUSHISTORY",
            "SERIALINVENTORY",
            "ORDERSTATUSHISTORY",
            "LOGISTIC_UNIT",
            "RECEIPTDETAIL",
            "SKU",
            "LOTXLOCXID",
            "TASKDETAIL",
            "USERACTIVITY",
            "PICKDETAIL",
            "TRANSMITLOG",
            "LOTATTRIBUTE",
            "LOT",
            "PICKDETAIL",
            "WAVE",
            "WAVEDETAIL",
            "SKUXLOC",
            "LOC",
            "RECEIPDETAILIDENTITY",
            "RECEIPTDETAILUIT",
            "RECEIPTDETAILITEM"
            );

    @Test
    public void checkIfScriptsWithNewColumnsAreContainedInScprdAndScprdarc() throws FileNotFoundException {
        File mainFolder = new File(this.getClass().getClassLoader().getResource("migrations").getFile());
        File arcFolder = new File(this.getClass().getClassLoader()
                .getResource("db/SCPRDARC/migrations").getFile());

        Map<String, String> scprdScriptsWithColumns = new HashMap<>(); //column, file
        Map<String, String> arcScriptsWithColumns = new HashMap<>();

        Arrays.stream(mainFolder.listFiles())
                .filter(item -> !item.isDirectory())
                .forEach(item -> retreiveAddingColumnStatementsFromScriptsAndWrite(item, scprdScriptsWithColumns));

        Arrays.stream(arcFolder.listFiles())
                .filter(item -> !item.isDirectory())
                .forEach(item -> retreiveAddingColumnStatementsFromScriptsAndWrite(item, arcScriptsWithColumns));


        scprdScriptsWithColumns.keySet().removeAll(arcScriptsWithColumns.keySet());
        if (!scprdScriptsWithColumns.isEmpty()) {
            log.error("The following columns are not migrated to SCPRDARC:");
            scprdScriptsWithColumns.forEach((line, file) -> {
                log.error("{},\n file: '{}'", line, file);
            });
            throw new AssertionError();
        }
    }

    public void retreiveAddingColumnStatementsFromScriptsAndWrite(File file,
                                                                  Map<String, String> scriptsWithColumns) {
        String extension = null;
        int index = file.getAbsolutePath().lastIndexOf('.');
        if (index > 0) {
            extension = file.getAbsolutePath().substring(index + 1);
        }
        try (InputStream is = Files.newInputStream(file.toPath())) {
            if (extension.equals("xml")) {
                readXmlScriptAndWriteRecords(file, scriptsWithColumns, is);
            } else {
                readSqlScriptAndWriteRecords(file, scriptsWithColumns, is);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            log.error(e.getMessage());
            throw new AssertionError();
        }

    }

    private void readSqlScriptAndWriteRecords(File file, Map<String, String> sqlScriptsWithColumns, InputStream is)
            throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        readSqlScriptAndWriteRecords(file.getCanonicalPath(), sqlScriptsWithColumns, content);
    }

    private void readSqlScriptAndWriteRecords(String filePath, Map<String, String> sqlScriptsWithColumns,
                                              String content) {
        if (content != null) {
            Matcher matcher = SCHEMA_TABLE_COLUMN_TYPE_PATTERN.matcher(content.toUpperCase());
            while (matcher.find()) {
                String schema = matcher.group(1).trim();
                String table = matcher.group(2).trim();
                String column = matcher.group(3).trim();
                String type = matcher.group(4).trim();
                String foundColumn = String.format(QUERY_FORMAT, schema, table, column, type);
                if (tableIsMigratedToArc(schema, table)) {
                    sqlScriptsWithColumns.put(foundColumn, filePath);
                }
            }
        }
    }

    private void readXmlScriptAndWriteRecords(File file, Map<String, String> xmlScriptsWithColumns,
                                              InputStream is)
            throws ParserConfigurationException, SAXException, IOException {
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        DocumentBuilder dBuilder = dbf.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        Element element = doc.getDocumentElement();

        NodeList nodeList = element.getElementsByTagName("addColumn");
        if (nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); ++i) {
                Element addColumn = (Element) nodeList.item(i);
                String schema = addColumn.getAttribute("schemaName").toUpperCase();
                String table = addColumn.getAttribute("tableName").toUpperCase();
                NodeList nl = addColumn.getElementsByTagName("column");
                Element column = (Element) nl.item(0);
                String name = column.getAttribute("name").toUpperCase();
                String type = column.getAttribute("type").toUpperCase();
                String foundColumn = String.format(QUERY_FORMAT, schema, table, name, type);
                if (tableIsMigratedToArc(schema, table)) {
                    xmlScriptsWithColumns.put(foundColumn, file.getCanonicalPath());
                }
            }
        }

        NodeList sqlList = element.getElementsByTagName("sql");
        for (int i = 0; i < sqlList.getLength(); i++) {
            Element sql = (Element) sqlList.item(i);
            readSqlScriptAndWriteRecords(file.getCanonicalPath(), xmlScriptsWithColumns, sql.getTextContent());
        }
    }

    private boolean tableIsMigratedToArc(String schema, String tableName) {
        if (schema.equals("WMWHSE1")
                && TABLES_IN_BOTH_DBS.contains(tableName)) {
            return true;
        }
        return false;
    }

    @Data
    @Builder
    private static class AlterMigration {
        File file;
        String type;
    }

    @Test
    public void checkIfScriptsWithAlterColumnsAreContainedInScprdAndScprdarc() {
        File mainFolder = new File(this.getClass().getClassLoader().getResource("migrations").getFile());
        File arcFolder = new File(this.getClass().getClassLoader()
                .getResource("db/SCPRDARC/migrations").getFile());

        // schema.table.column -> (file, type)
        Map<String, AlterMigration> scprdScripts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, AlterMigration> arcScripts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Arrays.stream(mainFolder.listFiles())
                .filter(item -> !item.isDirectory())
                .forEach(item -> fillAlterColumnScriptsMap(item, scprdScripts));

        Arrays.stream(arcFolder.listFiles())
                .filter(item -> !item.isDirectory())
                .forEach(item -> fillAlterColumnScriptsMap(item, arcScripts));


        scprdScripts.forEach((tableAndColumn, migration) -> {
            AlterMigration arcMigration = arcScripts.get(tableAndColumn);
            String[] tableAndColumnSplit = tableAndColumn.split("\\.");
            Assertions.assertNotNull(arcMigration,
                    String.format("Cannot find 'alter column' migration in SCPRDARC " +
                                    "for table %s column %s matching migration %s",
                            tableAndColumnSplit[1], tableAndColumnSplit[2], migration.file.getAbsolutePath()));
            Assertions.assertTrue(migration.type.equalsIgnoreCase(arcMigration.type),
                    String.format("Table %s column %s type definition in %s differs from %s \\n '%s' != '%s' ",
                            tableAndColumnSplit[1], tableAndColumnSplit[2],
                            migration.file.getAbsolutePath(), arcMigration.file.getAbsolutePath(),
                            migration.type, arcMigration.type));
        });
    }

    public void fillAlterColumnScriptsMap(File file, Map<String, AlterMigration> scriptsWithColumns) {
        String extension = null;
        int index = file.getAbsolutePath().lastIndexOf('.');
        if (index > 0) {
            extension = file.getAbsolutePath().substring(index + 1);
        }
        try (InputStream is = Files.newInputStream(file.toPath())) {
            if (extension.equals("xml")) {
                readAlterColumnXml(file, scriptsWithColumns, is);
            } else {
                readAlterColumnSql(file, scriptsWithColumns, is);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            log.error(e.getMessage());
            throw new AssertionError();
        }
    }

    // file is for path only, data is taken from is
    private void readAlterColumnSql(File file, Map<String, AlterMigration> sqlScriptsWithColumns, InputStream is)
            throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String content = br.lines().collect(Collectors.joining("\n"));
        if (!StringUtils.isEmpty(content)) {
            Matcher matcher = SCHEMA_ALTER_COLUMN_PATTERN.matcher(content.toUpperCase());
            while (matcher.find()) {
                String schema = matcher.group(1).trim();
                String table = matcher.group(2).trim();
                String column = matcher.group(3).trim();
                String type = matcher.group(4).trim();
                if (tableIsMigratedToArc(schema, table)) {
                    sqlScriptsWithColumns.put(String.format("%s.%s.%s", schema, table, column),
                            AlterMigration.builder().file(file).type(type).build());
                }
            }
        }
    }

    private void readAlterColumnXml(File file, Map<String, AlterMigration> xmlScriptsWithColumns, InputStream is)
            throws ParserConfigurationException, SAXException, IOException {
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        DocumentBuilder dBuilder = dbf.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        Element element = doc.getDocumentElement();

        NodeList nodeList = element.getElementsByTagName("modifyDataType");
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Element alterColumn = (Element) nodeList.item(i);
            String schema = alterColumn.getAttribute("schemaName").toUpperCase();
            String table = alterColumn.getAttribute("tableName").toUpperCase();
            String column = alterColumn.getAttribute("columnName").toUpperCase();
            String type = alterColumn.getAttribute("newDataType").toUpperCase();
            if (tableIsMigratedToArc(schema, table)) {
                xmlScriptsWithColumns.put(String.format("%s.%s.%s", schema, table, column),
                        AlterMigration.builder().file(file).type(type).build());
            }
        }

        NodeList sqlList = element.getElementsByTagName("sql");
        for (int i = 0; i < sqlList.getLength(); i++) {
            Element sql = (Element) sqlList.item(i);
            readAlterColumnSql(file, xmlScriptsWithColumns, new ByteArrayInputStream(sql.getTextContent().getBytes()));
        }
    }
}
