package ru.yandex.market.wms.reporter.reports;

import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestOneData extends ReportData {

    @Data
    public static class DataRow {
        Date col1;
        Integer col2;
        String col3;
        Number col4;
    }

    List<TestOneData.DataRow> data;
}
