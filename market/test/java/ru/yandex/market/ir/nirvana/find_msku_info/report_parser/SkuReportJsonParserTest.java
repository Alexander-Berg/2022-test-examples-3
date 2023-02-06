package ru.yandex.market.ir.nirvana.find_msku_info.report_parser;

import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SkuReportJsonParserTest {
    @Test
    public void parse() throws IOException {
        String testReportResponse = "src/test/resources/sku2.json";
        ArrayList<Sku> result = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(new File(testReportResponse))) {
            SkuReportJsonParser skuReportJsonParser = new SkuReportJsonParser();
            result.addAll(skuReportJsonParser.parse(fileInputStream));
        }

        assertEquals(2, result.size());
    }
}