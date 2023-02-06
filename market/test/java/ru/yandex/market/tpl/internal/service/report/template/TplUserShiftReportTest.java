package ru.yandex.market.tpl.internal.service.report.template;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.shift.partner.PartnerUserShiftDto;

import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;
import static ru.yandex.market.tpl.internal.service.report.template.TplUserShiftReport.HEADER_CANCEL_OPERATOR;
import static ru.yandex.market.tpl.internal.service.report.template.TplUserShiftReport.HEADER_CANCEL_OTHER_REASON;
import static ru.yandex.market.tpl.internal.service.report.template.TplUserShiftReport.HEADER_CANCEL_SYSTEM;
import static ru.yandex.market.tpl.internal.service.report.template.TplUserShiftReport.HEADER_OTHER;
import static ru.yandex.market.tpl.internal.service.report.template.TplUserShiftReport.HEADER_POSTPONED;
import static ru.yandex.market.tpl.internal.service.report.template.TplUserShiftReport.HEADER_RESCHEDULE_OPERATOR;
import static ru.yandex.market.tpl.internal.service.report.template.TplUserShiftReport.HEADER_RESCHEDULE_OTHER_REASON;

class TplUserShiftReportTest {

    //todo Enable when fix font configuration with https://st.yandex-team.ru/DEVTOOLS-5000
    @Test
    @Disabled
    void checkFields() {
        //given
        var userShiftDto = OBJECT_GENERATOR.nextObject(PartnerUserShiftDto.class);

        //when
        Sheet sheet = new TplUserShiftReport(List.of(userShiftDto)).sheetIterator().next();

        //then
        Cell cancelOperatorHeader = getHeaderCell(sheet, HEADER_CANCEL_OPERATOR);
        Assertions.assertThat(sheet.getRow(1)
                        .getCell(cancelOperatorHeader.getColumnIndex()).getNumericCellValue())
                .isEqualTo(userShiftDto.getCountOrdersCancelledOperator());

        Cell cancelSystemHeader = getHeaderCell(sheet, HEADER_CANCEL_SYSTEM);
        Assertions.assertThat(sheet.getRow(1)
                        .getCell(cancelSystemHeader.getColumnIndex()).getNumericCellValue())
                .isEqualTo(userShiftDto.getCountOrdersCancelledSystem());

        Cell cancelOtherReasonHeader = getHeaderCell(sheet, HEADER_CANCEL_OTHER_REASON);
        Assertions.assertThat(sheet.getRow(1)
                        .getCell(cancelOtherReasonHeader.getColumnIndex()).getNumericCellValue())
                .isEqualTo(userShiftDto.getCountOrdersCancelledOtherReason());

        Cell rescheduleOtherReasonHeader = getHeaderCell(sheet, HEADER_RESCHEDULE_OTHER_REASON);
        Assertions.assertThat(sheet.getRow(1)
                        .getCell(rescheduleOtherReasonHeader.getColumnIndex()).getNumericCellValue())
                .isEqualTo(userShiftDto.getCountOrdersRescheduledOtherReason());

        Cell rescheduleOperatorHeader = getHeaderCell(sheet, HEADER_RESCHEDULE_OPERATOR);
        Assertions.assertThat(sheet.getRow(1)
                        .getCell(rescheduleOperatorHeader.getColumnIndex()).getNumericCellValue())
                .isEqualTo(userShiftDto.getCountOrdersRescheduledOperator());

        Cell postponedHeader = getHeaderCell(sheet, HEADER_POSTPONED);
        Assertions.assertThat(sheet.getRow(1)
                        .getCell(postponedHeader.getColumnIndex()).getNumericCellValue())
                .isEqualTo(userShiftDto.getCountOrdersPostponed());

        Cell otherHeader = getHeaderCell(sheet, HEADER_OTHER);
        Assertions.assertThat(sheet.getRow(1)
                        .getCell(otherHeader.getColumnIndex()).getNumericCellValue())
                .isEqualTo(userShiftDto.getCountOrdersOther());
    }

    @NotNull
    private Cell getHeaderCell(Sheet sheet, String headerCancelSystem) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(sheet.getRow(0)
                        .cellIterator(), Spliterator.ORDERED), false)
                .filter(cell -> cell.getStringCellValue().equals(headerCancelSystem))
                .findFirst().orElseThrow();
    }
}
