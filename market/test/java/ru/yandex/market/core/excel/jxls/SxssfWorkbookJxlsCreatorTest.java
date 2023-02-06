package ru.yandex.market.core.excel.jxls;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.excel.jxls.SxssfWorkbookJxlsCreator.SxssfWorkbookJxlsBuilder;

import static ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils.assertCellValues;

/**
 * {@link SxssfWorkbookJxlsCreator}
 */
class SxssfWorkbookJxlsCreatorTest {
    @Test
    void test_createWorkbook() throws IOException {
        SxssfWorkbookJxlsBuilder creator = new SxssfWorkbookJxlsBuilder(
                SxssfWorkbookJxlsCreatorTest.class,
                "./test_template.xlsx"
        );
        List<JxlsTestEntity> first = Collections.singletonList(new JxlsTestEntity(11, 12));
        List<JxlsTestEntity> second = Collections.singletonList(new JxlsTestEntity(99, 88));

        creator.enrichContext("first", first);
        creator.enrichContext("second", second);

        creator.addTemplateSheet(TemplateSheetItem.builder()
                .setTemplateSheetName("template_1")
                .setTargetSheetName("report_1")
                .build()
        );

        creator.addTemplateSheet(TemplateSheetItem.builder()
                .setTemplateSheetName("template_2")
                .setTargetSheetName("report_2")
                .build()
        );

        Workbook workbook = creator.build().createWorkbook();

        verifyReportList(workbook.getSheetAt(0), first);
        verifyReportList(workbook.getSheetAt(1), second);
    }

    private void verifyReportList(Sheet sheet, List<JxlsTestEntity> items) {
        List<List<Object>> expected = Collections.singletonList(
                Arrays.asList(
                        items.get(0).getKey(), items.get(0).getValue()
                )
        );
        assertCellValues(expected, sheet, 1, 0);
    }

    public static final class JxlsTestEntity {
        private int key;
        private int value;

        JxlsTestEntity(int key, int value) {
            this.key = key;
            this.value = value;
        }

        public int getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }
    }
}