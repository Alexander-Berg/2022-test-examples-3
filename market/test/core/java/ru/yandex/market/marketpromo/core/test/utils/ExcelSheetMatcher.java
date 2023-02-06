package ru.yandex.market.marketpromo.core.test.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.Nullable;

import static org.hamcrest.Matchers.nullValue;

public class ExcelSheetMatcher extends TypeSafeMatcher<byte[]> {

    private final List<CellMatcher<?>> cellMatchers;
    private final int sheetNo;

    public ExcelSheetMatcher(int sheetNo, List<CellMatcher<?>> cellMatchers) {
        this.cellMatchers = cellMatchers;
        this.sheetNo = sheetNo;
    }

    public static ExcelSheetMatcher sheetWithCells(CellMatcher<?>... cellMatchers) {
        return new ExcelSheetMatcher(0, Arrays.asList(cellMatchers));
    }

    public static ExcelSheetMatcher sheetWithCells(int sheetNo, CellMatcher<?>... cellMatchers) {
        return new ExcelSheetMatcher(sheetNo, Arrays.asList(cellMatchers));
    }

    @Override
    protected boolean matchesSafely(byte[] item) {
        try (XSSFWorkbook sheets = new XSSFWorkbook(new ByteArrayInputStream(item))) {
            XSSFSheet sheet = sheets.getSheetAt(sheetNo);
            for (CellMatcher<?> matcher : cellMatchers) {
                if (!matcher.matches(sheet)) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void describeMismatchSafely(byte[] item, Description description) {
        try (XSSFWorkbook sheets = new XSSFWorkbook(new ByteArrayInputStream(item))) {
            XSSFSheet sheet = sheets.getSheetAt(sheetNo);
            description
                    .appendList(" [", ", ", "]", cellMatchers.stream()
                            .map(cellMatcher -> cellMatcher.expectedDescribe(sheet))
                            .collect(Collectors.toList())
                    );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("excel containing")
                .appendList(" [", ", ", "]", cellMatchers);
    }


    public static class CellMatcher<T> implements SelfDescribing {

        private final int row;
        private final int column;
        private final Matcher<? super T> matcher;
        private final Type type;

        CellMatcher(int row, int column, Matcher<? super T> matcher, Type type) {
            this.row = row;
            this.column = column;
            this.matcher = matcher;
            this.type = type;
        }

        public static CellMatcher<String> textCell(int column, int row, Matcher<? super String> matcher) {
            return CellMatcher.<String>builder(matcher, Type.STRING).setRow(row).setColumn(column).build();
        }

        public static CellMatcher.Builder<String> textCell(int column, Matcher<? super String> matcher) {
            return CellMatcher.<String>builder(matcher, Type.STRING).setColumn(column);
        }

        public static CellMatcher<Double> numberCell(int column, int row, Matcher<? super Double> matcher) {
            return CellMatcher.<Double>builder(matcher, Type.NUMBER).setRow(row).setColumn(column).build();
        }

        public static CellMatcher.Builder<Double> numberCell(int column, Matcher<? super Double> matcher) {
            return CellMatcher.<Double>builder(matcher, Type.NUMBER).setColumn(column);
        }

        public static CellMatcher<Integer> intCell(int column, int row, Matcher<? super Integer> matcher) {
            return CellMatcher.<Integer>builder(matcher, Type.NUMBER).setRow(row).setColumn(column).build();
        }

        public static CellMatcher.Builder<Integer> intCell(int column, Matcher<? super Integer> matcher) {
            return CellMatcher.<Integer>builder(matcher, Type.NUMBER).setColumn(column);
        }

        public static CellMatcher<Object> emptyCell(int column, int row) {
            return builder(nullValue(), Type.EMPTY).setRow(row).setColumn(column).build();
        }

        public static CellMatcher.Builder<Object> emptyCell(int column) {
            return builder(nullValue(), Type.EMPTY).setColumn(column);
        }

        public static CellMatcher<?>[] rows(Row... rows) {
            return Arrays.stream(rows)
                    .map(Row::getCells)
                    .flatMap(Collection::stream)
                    .toArray(ExcelSheetMatcher.CellMatcher[]::new);
        }

        public static Row row(int row, CellMatcher.Builder<?>... cells) {
            return new Row(Arrays.stream(cells)
                    .map(builder -> builder.setRow(row).build())
                    .collect(Collectors.toList()));
        }

        private static <T> Builder<T> builder(Matcher<? super T> matcher, Type type) {
            return new Builder<>(matcher, type);
        }

        @Override
        public void describeTo(Description description) {
            description
                    .appendText(cellToString())
                    .appendDescriptionOf(matcher);
        }

        Type getType() {
            return type;
        }

        boolean matches(XSSFSheet sheet) {
            return matcher.matches(extractValue(sheet));
        }

        @Nullable
        private Object extractValue(XSSFSheet sheet) {
            XSSFRow row = sheet.getRow(this.row);
            if (row == null) {
                return null;
            }
            XSSFCell cell = row.getCell(column);
            if (cell == null) {
                return null;
            }
            try {
                switch (getType()) {
                    case STRING:
                        return cell.getStringCellValue();
                    case NUMBER:
                        return cell.getNumericCellValue();
                    case EMPTY:
                        return cell.getRawValue();
                    default:
                        throw new UnsupportedOperationException();
                }
            } catch (IllegalStateException e) {
                return cell.getRawValue();
            }
        }

        private SelfDescribing expectedDescribe(XSSFSheet sheet) {
            return description -> description
                    .appendText(cellToString())
                    .appendValue(extractValue(sheet));
        }

        private String cellToString() {
            return String.format("cell [column: %s; row: %s]", column, row);
        }

        private enum Type {
            STRING,
            NUMBER,
            EMPTY
        }

        public static class Row {

            private final List<CellMatcher<?>> cells;

            private Row(List<CellMatcher<?>> cells) {
                this.cells = cells;
            }

            private List<CellMatcher<?>> getCells() {
                return cells;
            }
        }

        public static class Builder<T> {

            private final Matcher<? super T> matcher;
            private final Type type;
            private int row;
            private int column;

            private Builder(Matcher<? super T> matcher, Type type) {
                this.matcher = matcher;
                this.type = type;
            }

            public Builder<T> setRow(int row) {
                this.row = row;
                return this;
            }

            public Builder<T> setColumn(int column) {
                this.column = column;
                return this;
            }

            public CellMatcher<T> build() {
                return new CellMatcher<>(row, column, matcher, type);
            }
        }
    }
}
