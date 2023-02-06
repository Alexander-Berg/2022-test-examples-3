package ru.yandex.market.sc.internal.controller.manual.xdoc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Data;
import one.util.streamex.StreamEx;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

public class TopologyGenerator {

    private static final int ROWS_LIMIT = 299;
    private static final int CELLS_LIMIT_PER_ROW = 99;

    private static final int ODD_MAX = 48;
    private static final int EVEN_MAX = 36;

    public static CreateTopologyRequest createTopologyRequest(
            CellSubType subType,
            String warehouseYandexId,
            Long zoneId,
            List<String> cellNames
    ) {
        var createCellRequests = StreamEx.of(cellNames)
                .map(cellName ->
                        CellRequestDto.builder()
                                .number(cellName)
                                .status(CellStatus.ACTIVE)
                                .type(inferType(subType))
                                .subType(subType)
                                .warehouseYandexId(warehouseYandexId)
                                .zoneId(zoneId)
                                .build())
                .toList();

        return new CreateTopologyRequest(createCellRequests);
    }

    public static List<String> evenOddCellNames(
            int from,
            int to,
            String prefix,
            String delimiter,
            String suffix
    ) {
        var topology = evenOddRows(from, to);
        validate(topology);
        return rowsToCellNames(topology, prefix, delimiter, suffix);
    }

    public static List<String> rectangularCellNames(
            int start,
            int end,
            int depth,
            String prefix,
            String delimiter,
            String suffix
    ) {
        return rectangularCellNames(start, end, depth, prefix, delimiter, suffix, rowNumber -> true);
    }

    public static List<String> rectangularCellNames(
            int start,
            int end,
            int depth,
            String prefix,
            String delimiter,
            String suffix,
            Predicate<Integer> includeRow
    ) {
        var topology = rectangularRows(start, end, depth, includeRow);
        validate(topology);
        return rowsToCellNames(topology, prefix, delimiter, suffix);
    }

    private static List<String> rowsToCellNames(List<TRow> rows, String prefix, String delimiter, String suffix) {
        List<String> cellNames = new ArrayList<>();
        for (TRow row : rows) {
            int rowNumber = row.getRowNumber();
            for (Integer cellNumber : row.getCellNumbers()) {
                cellNames.add(getCellName(prefix, delimiter, rowNumber, cellNumber, suffix));
            }
        }
        return cellNames;
    }

    private static CellType inferType(CellSubType subType) {
        if (CellType.BUFFER.getSubTypes().contains(subType)) {
            return CellType.BUFFER;
        }
        throw new IllegalArgumentException(String.format("CellSubType %s is not acceptable", subType));
    }

    private static List<TRow> evenOddRows(int from, int to) {
        List<TRow> rows = new ArrayList<>();

        for (int i = from; i <= to; i++) {
            List<Integer> cellNumbers = new ArrayList<>();
            if (i % 2 != 0) {
                for (int j = 1; j <= ODD_MAX; j++) {
                    cellNumbers.add(j);
                }
            } else {
                for (int j = 1; j <= EVEN_MAX; j++) {
                    cellNumbers.add(j);
                }
            }
            var row = new TRow();
            row.setRowNumber(i);
            row.setCellNumbers(cellNumbers);
            rows.add(row);
        }
        return rows;
    }

    private static List<TRow> rectangularRows(int start, int end, int depth, Predicate<Integer> include) {
        if (start < 1 || start > end) {
            throw new TplInvalidActionException("start must be >= 1 and <= end but was " + start);
        }
        List<TRow> rows = new ArrayList<>();
        IntStream.rangeClosed(start, end)
                .forEach(rowNumber -> {
                            TRow row = new TRow();
                            row.setRowNumber(rowNumber);
                            row.setCellNumbers(
                                    IntStream.rangeClosed(1, depth)
                                            .boxed()
                                            .toList()
                            );
                            if (include.test(rowNumber)) {
                                rows.add(row);
                            }
                        }
                );
        return rows;
    }

    private static void validate(List<TRow> topology) {
        var rowStats = topology.stream()
                .peek(TopologyGenerator::validateRow)
                .mapToInt(TRow::getRowNumber)
                .summaryStatistics();

        if (rowStats.getMin() < 1 || rowStats.getMax() > ROWS_LIMIT) {
            throw new IllegalArgumentException("row number must be from 1 to " + ROWS_LIMIT);
        }

        var cellStats = topology.stream()
                .flatMap(row -> row.getCellNumbers().stream())
                .mapToInt(Integer::intValue)
                .summaryStatistics();

        if (cellStats.getMin() < 1 || cellStats.getMax() > CELLS_LIMIT_PER_ROW) {
            throw new IllegalArgumentException("cell number must be from 1 to " + CELLS_LIMIT_PER_ROW);
        }
    }

    private static void validateRow(TRow row) {
        if (CollectionUtils.isEmpty(row.getCellNumbers())) {
            throw new IllegalArgumentException("row number not be empty");
        }
    }

    private static String getCellName(String prefix, String delimiter, int rowNumber, int cellNumber, String suffix) {
        String template = String.valueOf(rowNumber).length() > 2 ? "%03d" : "%02d";
        return prefix + String.format(template + delimiter + "%02d" + suffix, rowNumber, cellNumber);
    }

    @Data
    private static class TRow {
        private int rowNumber;
        private List<Integer> cellNumbers;
    }

}
