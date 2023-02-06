package ru.yandex.market.mbo.gwt.client.widgets.parameter.values.position.editor.data_helper;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.widgets.parameter.values.position.editor.DataHelper;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:magicNumber")
public class BooleanFunctionsTest {

    @Test
    public void isParameterCanHaveEnumShortListTest() {
        CategoryParam param = new Parameter();

        param.setShortEnumSortType(null);
        param.setShortEnumCount(null);
        assertFalse(DataHelper.isParameterCanHaveEnumShortList(param));

        param.setShortEnumSortType(Param.EnumSortType.ALPHABETICAL);
        param.setShortEnumCount(null);
        assertFalse(DataHelper.isParameterCanHaveEnumShortList(param));

        param.setShortEnumSortType(null);
        param.setShortEnumCount(10);
        assertFalse(DataHelper.isParameterCanHaveEnumShortList(param));

        param.setShortEnumSortType(Param.EnumSortType.ALPHABETICAL);
        param.setShortEnumCount(10);
        assertTrue(DataHelper.isParameterCanHaveEnumShortList(param));
    }

    @Test
    public void isParameterEnumSortTypeManualTest() {
        CategoryParam param = new Parameter();

        param.setShortEnumSortType(null);
        assertFalse(DataHelper.isParameterEnumSortTypeManual(param));

        param.setShortEnumSortType(Param.EnumSortType.ALPHABETICAL);
        assertFalse(DataHelper.isParameterEnumSortTypeManual(param));

        param.setShortEnumSortType(Param.EnumSortType.OFFERS_COUNT);
        assertFalse(DataHelper.isParameterEnumSortTypeManual(param));

        param.setShortEnumSortType(Param.EnumSortType.MANUAL);
        assertTrue(DataHelper.isParameterEnumSortTypeManual(param));
    }
}
