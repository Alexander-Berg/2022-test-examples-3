package ru.yandex.market.mbo.reactui.dto.parameters;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import static org.junit.Assert.assertEquals;
import static ru.yandex.common.util.db.SortingOrder.ASC;
import static ru.yandex.common.util.db.SortingOrder.DESC;
import static ru.yandex.market.mbo.reactui.dto.parameters.CategoryParameterComparator.comparator;
import static ru.yandex.market.mbo.reactui.dto.parameters.GlobalCategoryParameterFilter.OrderedColumn.ID;
import static ru.yandex.market.mbo.reactui.dto.parameters.GlobalCategoryParameterFilter.OrderedColumn.LEVEL;
import static ru.yandex.market.mbo.reactui.dto.parameters.GlobalCategoryParameterFilter.OrderedColumn.NAME;
import static ru.yandex.market.mbo.reactui.dto.parameters.GlobalCategoryParameterFilter.OrderedColumn.TYPE;
import static ru.yandex.market.mbo.reactui.dto.parameters.GlobalCategoryParameterFilter.OrderedColumn.XSL_NAME;

@SuppressWarnings("checkstyle:magicNumber")
public class CategoryParameterComparatorTest {

    private static final String NAME_STR = "name";

    private static final String CAT = "cat";
    private static final String BOOK = "book";
    private static final String APPLE = "apple";

    @Test
    public void comparatorByIdTest() {
        List<CategoryParam> params = Arrays.asList(
            parameter(2, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(1, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(3, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL)
        );

        final Comparator<CategoryParam> comparator = comparator(ID, null);

        final List<CategoryParam> collect = params.stream()
            .sorted(comparator)
            .collect(Collectors.toList());

        assertEquals(1, collect.get(0).getId());
        assertEquals(2, collect.get(1).getId());
        assertEquals(3, collect.get(2).getId());
    }

    @Test
    public void comparatorByNameTest() {
        List<CategoryParam> params = Arrays.asList(
            parameter(1, BOOK, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(1, CAT, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(1, APPLE, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL)
        );

        final Comparator<CategoryParam> comparator = comparator(NAME, ASC);

        final List<CategoryParam> collect = params.stream()
            .sorted(comparator)
            .collect(Collectors.toList());

        assertEquals(APPLE, collect.get(0).getName());
        assertEquals(BOOK, collect.get(1).getName());
        assertEquals(CAT, collect.get(2).getName());
    }

    @Test
    public void comparatorByXslNameDescTest() {
        List<CategoryParam> params = Arrays.asList(
            parameter(1, NAME_STR, BOOK, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(1, NAME_STR, CAT, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(1, NAME_STR, APPLE, Param.Type.BOOLEAN, CategoryParam.Level.MODEL)
        );

        final Comparator<CategoryParam> comparator = comparator(XSL_NAME, DESC);

        final List<CategoryParam> collect = params.stream()
            .sorted(comparator)
            .collect(Collectors.toList());

        assertEquals(CAT, collect.get(0).getXslName());
        assertEquals(BOOK, collect.get(1).getXslName());
        assertEquals(APPLE, collect.get(2).getXslName());
    }

    @Test
    public void comparatorByTypeTest() {
        List<CategoryParam> params = Arrays.asList(
            parameter(1, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(1, NAME_STR, NAME_STR, Param.Type.NUMERIC, CategoryParam.Level.MODEL),
            parameter(1, NAME_STR, NAME_STR, Param.Type.ENUM, CategoryParam.Level.MODEL),
            parameter(1, NAME_STR, NAME_STR, Param.Type.STRING, CategoryParam.Level.MODEL)
        );

        Comparator<CategoryParam> comparator = comparator(TYPE, ASC);

        List<CategoryParam> collect = params.stream()
            .sorted(comparator)
            .collect(Collectors.toList());

        assertEquals(Param.Type.BOOLEAN, collect.get(0).getType());
        assertEquals(Param.Type.ENUM, collect.get(1).getType());
        assertEquals(Param.Type.NUMERIC, collect.get(2).getType());
        assertEquals(Param.Type.STRING, collect.get(3).getType());
    }

    @Test
    public void comparatorByModelTest() {
        List<CategoryParam> params = Arrays.asList(
            parameter(1, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.OFFER),
            parameter(1, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.OFFER),
            parameter(1, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(1, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.MODEL),
            parameter(1, NAME_STR, NAME_STR, Param.Type.BOOLEAN, CategoryParam.Level.OFFER)
        );

        Comparator<CategoryParam> comparator = comparator(LEVEL, DESC);

        List<CategoryParam> collect = params.stream()
            .sorted(comparator)
            .collect(Collectors.toList());

        assertEquals(CategoryParam.Level.OFFER, collect.get(0).getLevel());
        assertEquals(CategoryParam.Level.OFFER, collect.get(1).getLevel());
        assertEquals(CategoryParam.Level.OFFER, collect.get(2).getLevel());
        assertEquals(CategoryParam.Level.MODEL, collect.get(3).getLevel());
        assertEquals(CategoryParam.Level.MODEL, collect.get(4).getLevel());
    }

    private CategoryParam parameter(long id, String name, String xslName, Param.Type type, CategoryParam.Level level) {
        final Parameter parameter = new Parameter();
        parameter.setId(id);
        parameter.setLocalizedNames(Arrays.asList(
            new Word(Word.DEFAULT_LANG_ID, name)
        ));
        parameter.setXslName(xslName);
        parameter.setType(type);
        parameter.setLevel(level);
        return parameter;
    }
}
