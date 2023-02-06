package ru.yandex.market.mboc.common.services.excel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.mbo.excel.StreamExcelParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 19.12.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ColumnInfoParserTest {
    @Test
    public void testParseColumnInfo() {
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(
            getClass().getClassLoader().getResourceAsStream("excel/catalog-tovarov-dlya-yandexa.xlsm"));

        Map<String, ColumnInfo> columnInfoMap = ColumnInfoParser.readColumnInfo(sheets);
        assertThat(columnInfoMap).hasSize(28);

        Map<ColumnInfo.ColumnKind, Long> kindCount = columnInfoMap.values().stream()
            .collect(Collectors.groupingBy(ColumnInfo::getKind, Collectors.counting()));

        assertThat(kindCount.get(ColumnInfo.ColumnKind.IN)).isEqualTo(11);
        assertThat(kindCount.get(ColumnInfo.ColumnKind.INOUT)).isEqualTo(1);
        assertThat(kindCount.get(ColumnInfo.ColumnKind.OUT)).isEqualTo(16);

        ColumnInfo columnInfo = columnInfoMap.get("Описание товара".toLowerCase());
        assertThat(columnInfo.getTag()).isEqualTo("description");
        assertThat(columnInfo.getKind()).isEqualTo(ColumnInfo.ColumnKind.IN);
    }
}
