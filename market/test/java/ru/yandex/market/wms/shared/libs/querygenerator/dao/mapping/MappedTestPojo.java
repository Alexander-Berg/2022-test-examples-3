package ru.yandex.market.wms.shared.libs.querygenerator.dao.mapping;

public class MappedTestPojo {

    @MapToDbRow
    private String field1;

    @MapToDbRow("field_2_row_name")
    private String field2;

    private String field3;
}
