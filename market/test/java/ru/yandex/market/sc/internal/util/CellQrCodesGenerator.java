package ru.yandex.market.sc.internal.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.internal.domain.cell.CellIdAndName;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.tpl.report.core.ReportRequest;
import ru.yandex.market.tpl.report.core.ReportService;
import ru.yandex.market.tpl.report.core.ReportType;

/**
 * @author valter
 */
@EmbeddedDbIntTest
public class CellQrCodesGenerator {

    @Autowired
    ReportService reportService;

    /*
    allowed cells.csv format:
    sc_number,id
    S01-A-01,140365
    S01-B-01,140366
    ...
     */
    @Disabled("util")
    @Test
    @SneakyThrows
    void generateFileWithCellQrCodes() {
        AtomicInteger index = new AtomicInteger();
        List<CellIdToCellName> cells = StreamEx.of(Files.lines(Paths.get("/tmp/cells.csv")))
                .zipWith(IntStream.generate(index::getAndIncrement))
                .mapKeyValue(this::mapToCellObject)
                .filter(Objects::nonNull)
                .toList();

        byte[] bytes = getCellsQRCodesBy(cells, "cell_qr_codes_template");
        Files.deleteIfExists(Paths.get("/tmp/cells_pallets.pdf"));
        Files.write(Paths.get("/tmp/cells_pallets.pdf"), bytes);

        bytes = getCellsQRCodesBy(cells, "cell_qr_codes_template_shelf");
        Files.deleteIfExists(Paths.get("/tmp/cells_shelf.pdf"));
        Files.write(Paths.get("/tmp/cells_shelf.pdf"), bytes);
    }

    @Nullable
    private CellIdToCellName mapToCellObject(String line, int lineNum) {
        var parts = line.split(",");
        if (parts.length != 2) {
            throw new RuntimeException("Wrong file format at line " + lineNum + ": " + line);
        }
        String cellName = StringUtils.trimToEmpty(parts[0]);
        if (cellName.isEmpty()) {
            throw new RuntimeException("Wrong file format at line " + lineNum + ": " + line);
        }
        long cellId;
        try {
            cellId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            if (lineNum != 0) {
                throw new RuntimeException("Wrong file format at line " + lineNum + ": " + line);
            } else {
                // header
                return null;
            }
        }
        return new CellIdToCellName(cellId, cellName);
    }

    public byte[] getCellsQRCodesBy(List<CellIdToCellName> cellIdToCellNames, String templateName) {
        var cellIdsAndNames = cellIdToCellNames.stream()
                .map(
                        source -> new CellIdAndName(source.id(), source.sc_number())
                ).toList();

        var params = new HashMap<String, Object>();
        params.put("cellIdsAndNames", cellIdsAndNames);
        return reportService.makeReport(
                ReportType.PDF,
                Collections.singletonList(new ReportRequest(templateName, params, cellIdsAndNames))
        );
    }

    private static record CellIdToCellName(long id, String sc_number) {

    }

}
