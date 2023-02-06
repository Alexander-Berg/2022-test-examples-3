package ru.yandex.market.mbo.mdm.common.masterdata.model.msku;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author albina-gima
 * @date 10/1/20
 */
public class MdmMskuSearchFilterTest {
    private static final long MSKU_ID_1 = 42;
    private static final long MSKU_ID_2 = 43;
    private static final long MSKU_ID_3 = 123;
    private static final long MSKU_ID_4 = 888;

    @Test
    public void whenParseSearchStringShouldGetSingleValidMskuId() {
        Assertions.assertThat(parse("42    123")).containsExactly(MSKU_ID_1, MSKU_ID_3);
        Assertions.assertThat(parse("42, 123")).containsExactly(MSKU_ID_1, MSKU_ID_3);
        Assertions.assertThat(parse(" 42 123\t")).containsExactly(MSKU_ID_1, MSKU_ID_3);
        Assertions.assertThat(parse("\n\n\n\n123,42")).containsExactly(MSKU_ID_3, MSKU_ID_1);
        Assertions.assertThat(parse("42 123, 860_19987, ")).containsExactly(MSKU_ID_1, MSKU_ID_3);
    }

    @Test
    public void whenParseSearchStringShouldGetAllValidMskuIds() {
        Assertions.assertThat(parse("42 123, 48_QWE, 42 888  ,123, 146.890  43"))
            .containsExactly(MSKU_ID_1, MSKU_ID_3, MSKU_ID_4, MSKU_ID_2);
    }

    @Test
    public void whenParseSearchStringShouldAcceptComma() {
        Assertions.assertThat(parse("42, 43 ,123")).containsExactly(MSKU_ID_1, MSKU_ID_2, MSKU_ID_3);
    }

    @Test
    public void whenParseSearchStringShouldSkipInvalidMskuId() {
        Assertions.assertThat(parse("888\n42 123, abc,42, 42,")).containsExactly(MSKU_ID_4, MSKU_ID_1, MSKU_ID_3);
        Assertions.assertThat(parse("42, -9, abc, 0, 123, 888")).containsExactly(MSKU_ID_1, MSKU_ID_3, MSKU_ID_4);
    }

    public List<Long> parse(String searchString) {
        MdmMskuSearchFilter filter = new MdmMskuSearchFilter();
        filter.setSearchString(searchString);
        return filter.getMergedMskuIds();
    }
}
