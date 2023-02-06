package ru.yandex.market.olap2.ytreflect;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

// see test/resources/recreate_test_table.sh
public class YtTestTable {
    public static final String VIEW_NAME = "Все типы";
    public static final String TABLE_NAME = "olap2etl__all_types";
    public static final String TBL = "//home/market/testing/mstat/olap2etl/all_types";
    public static final Map<String, String> COLUMNS = ImmutableMap.<String,String>builder()
        .put("pk_int_col", "uint32")
        .put("datetime", "string")
        .put("col1_date", "string")
        .put("col1_dbtime", "string")
        .put("col1_numeric", "string")
        .put("col1_str", "string")
        .put("col1_int8", "int8")
        .put("col1_int16", "int16")
        .put("col1_int32", "int32")
        .put("col1_int64", "int64")
        .put("col1_uint8", "uint8")
        .put("col1_uint16", "uint16")
        .put("col1_uint32", "uint32")
        .put("col1_uint64", "uint64")
        .put("col1_double", "double")
        .put("col1_boolean", "boolean")
        .build();
}
