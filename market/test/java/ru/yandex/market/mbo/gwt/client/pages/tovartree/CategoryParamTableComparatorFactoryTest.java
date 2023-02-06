package ru.yandex.market.mbo.gwt.client.pages.tovartree;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author yuramalinov
 * @created 04.10.18
 */
public class CategoryParamTableComparatorFactoryTest {
    @Test
    public void testComparatorByXslNameComparator() {
        Comparator<CategoryParam> comparator =
            CategoryParamTableComparatorFactory.forTableFilter(BaseParametersViewTable.SORT_BY_XSL_NAME, true);
        assertEquals(-1, comparator.compare(param("aa"), param("bb")));
        assertEquals(0, comparator.compare(param("aa"), param("aa")));
        assertEquals(1, comparator.compare(param("bb"), param("aa")));

        assertEquals(1, comparator.compare(param("XLPicture02"), param("zz")));
        assertEquals(1, comparator.compare(param("XLPicture02"), param("aa")));
        assertEquals(1, comparator.compare(param("XLPicture02"), param("aa")));
        assertEquals(0, comparator.compare(param("XLPicture02"), param("XLPicture02")));
        assertEquals(1, comparator.compare(param("XLPicture02"), param("XLPicture01")));
    }

    @Test
    public void testComparatorByNameComparator() {
        Comparator<CategoryParam> comparator =
            CategoryParamTableComparatorFactory.forTableFilter(BaseParametersViewTable.SORT_BY_NAME, true);
        assertThat(comparator.compare(param("aa", "zz"), param("bb", "aa"))).isGreaterThan(0);
        assertThat(comparator.compare(param("aa"), param("aa"))).isEqualTo(0);
        assertThat(comparator.compare(param("bb", "aa"), param("aa", "zz"))).isLessThan(0);

        assertEquals(1, comparator.compare(param("XLPicture02", "aa"), param("zz", "zz")));
        assertEquals(1, comparator.compare(param("XLPicture02"), param("aa")));
        assertEquals(1, comparator.compare(param("XLPicture02"), param("aa")));
        assertEquals(0, comparator.compare(param("XLPicture02"), param("XLPicture02")));
        assertEquals(0, comparator.compare(param("XLPicture02", "aa"), param("XLPicture02", "aa")));
        assertThat(comparator.compare(param("XLPicture02", "aa"), param("XLPicture02", "zz"))).isLessThan(0);
        assertThat(comparator.compare(param("XLPicture02", "zz"), param("XLPicture02", "aa"))).isGreaterThan(0);
        assertEquals(1, comparator.compare(param("XLPicture02"), param("XLPicture01")));
    }

    @Test
    public void testComparatorByIdComparator() {
        Comparator<CategoryParam> comparator =
            CategoryParamTableComparatorFactory.forTableFilter(BaseParametersViewTable.SORT_BY_ID, true);
        assertThat(comparator.compare(paramWithId(2), paramWithId(1))).isGreaterThan(0);
        assertThat(comparator.compare(paramWithId(1), paramWithId(1))).isEqualTo(0);
        assertThat(comparator.compare(paramWithId(1), paramWithId(2))).isLessThan(0);
    }

    @Test
    public void testReverse() {
        Comparator<CategoryParam> comparator =
            CategoryParamTableComparatorFactory.forTableFilter(BaseParametersViewTable.SORT_BY_ID, false);
        assertThat(comparator.compare(paramWithId(2), paramWithId(1))).isLessThan(0);
        assertThat(comparator.compare(paramWithId(1), paramWithId(1))).isEqualTo(0);
        assertThat(comparator.compare(paramWithId(1), paramWithId(2))).isGreaterThan(0);
    }

    @Test
    public void testNullsXslName() {
        Comparator<CategoryParam> comparator =
            CategoryParamTableComparatorFactory.forTableFilter(BaseParametersViewTable.SORT_BY_XSL_NAME, true);
        assertThat(comparator.compare(param(null), param("not-null"))).isGreaterThan(0);
        assertThat(comparator.compare(param(null), param(null))).isEqualTo(0);
        assertThat(comparator.compare(param("not-null"), param(null))).isLessThan(0);
    }

    @Test
    public void testNullsName() {
        Comparator<CategoryParam> comparator =
            CategoryParamTableComparatorFactory.forTableFilter(BaseParametersViewTable.SORT_BY_NAME, true);
        assertThat(comparator.compare(param(null), param("not-null"))).isGreaterThan(0);
        assertThat(comparator.compare(param(null, "not-null"), param("not-null"))).isEqualTo(0);
        assertThat(comparator.compare(param(null, null), param(null, null))).isEqualTo(0);
        assertThat(comparator.compare(param(null, "not-null"), param("not-null"))).isEqualTo(0);
        assertThat(comparator.compare(param("not-null"), param(null))).isLessThan(0);
    }

    private CategoryParam param(String xslName) {
        return param(xslName, xslName);
    }

    private CategoryParam paramWithId(int id) {
        Parameter parameter = new Parameter();
        parameter.setId(id);
        return parameter;
    }

    private CategoryParam param(String xslName, String name) {
        CategoryParam param = new Parameter();
        if (name != null) {
            param.setNames(WordUtil.defaultWords(name));
        }
        param.setXslName(xslName);
        return param;
    }
}
