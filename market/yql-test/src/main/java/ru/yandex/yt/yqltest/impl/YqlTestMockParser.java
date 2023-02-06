package ru.yandex.yt.yqltest.impl;

import java.util.Arrays;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.yt.yqltestable.YqlTestable;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.08.2021
 */
public class YqlTestMockParser {

    private YqlTestMockParser() {
    }

    public static YqlTestMock parseFiles(YPath basePath, String... resources) {
        return parseRaw(basePath, Stream.of(resources)
            .map(YqlTestable::readFile)
            .toArray(String[]::new));
    }

    public static YqlTestMock parseRaw(YPath basePath, String... sources) {
        YqlTestMock mock = new YqlTestMock(basePath);
        for (String source : sources) {
            processSource(mock, source);
        }
        mock.init();
        return mock;
    }

    private static void processSource(YqlTestMock mock, String content) {
        // Iterate through lines until next command is found
        CommandParser currentParser = null;
        for (String row : content.split("\n")) {
            row = normalizeRow(row);
            if (canSkip(row)) {
                continue;
            }

            CommandParser nextParser = findParser(row);
            if (currentParser == null && nextParser == null) {
                throw new IllegalArgumentException("Unknown command: " + row);
            }

            if (nextParser != null) {
                // new command started - finish previous
                if (currentParser != null) {
                    currentParser.finish(mock);
                }
                currentParser = nextParser;
            }

            currentParser.process(row);
        }

        if (currentParser != null) {
            currentParser.finish(mock);
        }
    }

    private static String normalizeRow(String row) {
        return row
            // remove duplicate spaces
            .replaceAll("[\\s]+", " ")
            .strip();
    }

    private static boolean canSkip(String row) {
        // ignore comments and empty lines
        return row.startsWith("#") || row.isBlank();
    }

    private static CommandParser findParser(String row) {
        // Root commands:
        // # - comment
        // MOCK TABLE - mock table
        // MOCK VAR_TABLE - mock variable select
        // MOCK DIR_TABLE - mock directory select
        // MOCK VAR - mock variable

        if (row.startsWith(TableCommandParser.PREFIX)) {
            return new TableCommandParser();
        } else if (row.startsWith(TableVarCommandParser.PREFIX)) {
            return new TableVarCommandParser();
        } else if (row.startsWith(TableDirCommandParser.PREFIX)) {
            return new TableDirCommandParser();
        } else if (row.startsWith(VarCommandParser.PREFIX)) {
            return new VarCommandParser();
        }
        return null;
    }

    private interface CommandParser {
        /**
         * Tries to process row. Fails when can't.
         */
        void process(String row);

        /**
         * Writes parsed data to mock.
         */
        void finish(YqlTestMock mock);
    }

    private abstract static class AbstractTableCommandParser implements CommandParser {
        private static final String SCHEMA_PREFIX = "SCHEMA";
        private static final String NAME_PREFIX = "NAME";
        private static final String DATA_PREFIX = "{";
        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final String mockPrefix;
        protected final YqlTestMockTable table = new YqlTestMockTable();

        protected AbstractTableCommandParser(String prefix) {
            this.mockPrefix = prefix;
        }

        @Override
        public final void process(String row) {
            if (row.startsWith(mockPrefix) && table.getPath() == null) {
                setPath(row.substring(mockPrefix.length()).strip());
            } else if (row.startsWith(SCHEMA_PREFIX)) {
                Arrays.stream(row.substring(SCHEMA_PREFIX.length()).split(","))
                    .filter(x -> !x.isBlank())
                    .map(String::strip)
                    .forEach(table::addSchema);
            } else if (row.startsWith(DATA_PREFIX)) {
                try {
                    table.getData().add(MAPPER.readTree(row));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid json data:" + row, e);
                }
            } else if (row.startsWith(NAME_PREFIX)) {
                table.setName(row.substring(NAME_PREFIX.length()).strip());
            } else {
                throw new IllegalArgumentException("Unknown command for table mock parser: " + row);
            }
        }

        protected void setPath(String parsed) {
            table.setPath(YPath.simple(parsed));
        }

        protected void checkTableParsed() {
            if (table.getPath() == null) {
                throw new IllegalArgumentException("No table input passed");
            }
        }
    }

    private static class TableCommandParser extends AbstractTableCommandParser {
        private static final String PREFIX = "MOCK TABLE ";

        protected TableCommandParser() {
            super(PREFIX);
        }

        @Override
        public void finish(YqlTestMock mock) {
            checkTableParsed();
            mock.getTableMockMap().put(table.getPath().toString(), table);
        }
    }

    private static class TableVarCommandParser extends AbstractTableCommandParser {
        private static final String PREFIX = "MOCK VAR_TABLE $";

        protected TableVarCommandParser() {
            super(PREFIX);
        }

        @Override
        public void finish(YqlTestMock mock) {
            checkTableParsed();
            mock.getVarTableMockMap().put(table.getPath().toString(), table);
        }

        @Override
        protected void checkTableParsed() {
            super.checkTableParsed();
            if (table.getName() == null) {
                throw new IllegalArgumentException("Error in var table parsing. Table: " + table.getPath());
            }
        }

        @Override
        protected void setPath(String parsed) {
            super.setPath("//tmp/yqltest/mocked_var/" + parsed);
            table.setName(parsed);
        }
    }

    private static class TableDirCommandParser extends AbstractTableCommandParser {
        private static final String PREFIX = "MOCK DIR_TABLE ";

        protected TableDirCommandParser() {
            super(PREFIX);
        }

        @Override
        public void finish(YqlTestMock mock) {
            checkTableParsed();
            String dirPath = table.getPath().toString();
            YqlTestMockDir dir = mock.getDirMockMap().computeIfAbsent(dirPath, key -> new YqlTestMockDir());
            dir.setPath(table.getPath());
            dir.getTables().put(table.getName(), table);
        }

        @Override
        protected void checkTableParsed() {
            super.checkTableParsed();
            if (table.getName() == null) {
                throw new IllegalArgumentException("Error in table parsing: empty NAME. Table: " + table.getPath());
            }
        }
    }

    private static class VarCommandParser implements CommandParser {
        private static final String PREFIX = "MOCK VAR $";
        private static final String SET_PREFIX = "SET ";

        private String varName;
        private String varValue;

        @Override
        public void process(String row) {
            if (row.startsWith(PREFIX)) {
                varName = row.substring(PREFIX.length());
            } else if (row.startsWith(SET_PREFIX)) {
                varValue = row.substring(SET_PREFIX.length());
            } else {
                throw new IllegalArgumentException("Unknown command for var mock parser: " + row);
            }
        }

        @Override
        public void finish(YqlTestMock mock) {
            mock.getVarMockMap().put(varName, varValue);
        }
    }
}
