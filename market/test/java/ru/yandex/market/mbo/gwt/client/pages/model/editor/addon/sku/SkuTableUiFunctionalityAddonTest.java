package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuTableSortedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractModelTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.SkuRelationWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.widgets.SkuTableFilterStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:magicNumber")
public class SkuTableUiFunctionalityAddonTest extends AbstractModelTest {

    private static final int TOTAL_SKUS = 100;
    private static final long CATEGORY_ID = 1002093L;

    private List<CommonModel> skus;
    private CommonModel guru;

    private CategoryParam booleanParam;
    private CategoryParam stringParam;
    private CategoryParam numericParam;
    private CategoryParam enumParam;

    private SkuRelationWidgetStub skuWidget;

    @Before
    public void prepareSkus() {
        prepareParams();
        guru = CommonModelBuilder.newBuilder()
            .id(TOTAL_SKUS)
            .category(CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .title("НАБОР ДОСПЕХИ И ОРУЖИЕ \"ЧИВАЛРИ-БОЙ\"")
            .endModel();
        modelData.setModel(guru);
        modelData.addParam(booleanParam);
        modelData.addParam(stringParam);
        modelData.addParam(numericParam);
        modelData.addParam(enumParam);
        skus = new ArrayList<>();
        for (int i = 0; i < TOTAL_SKUS; i++) {
            CommonModel sku = CommonModelBuilder.newBuilder()
                .id(i)
                .category(CATEGORY_ID)
                .currentType(CommonModel.Source.SKU)
                .source(CommonModel.Source.SKU)
                .startModelRelation()
                .id(TOTAL_SKUS)
                .categoryId(CATEGORY_ID)
                .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .model(guru)
                .endModelRelation()
                .endModel();
            sku.addParameterValue(getParameterValue(booleanParam, i));
            sku.addParameterValue(getParameterValue(stringParam, i));
            sku.addParameterValue(getParameterValue(numericParam, i));
            sku.addParameterValue(getParameterValue(enumParam, i));
            ModelRelation toSku = new ModelRelation(i, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL);
            toSku.setModel(sku);
            guru.addRelation(toSku);
            skus.add(sku);
        }
        run();
        skuWidget = (SkuRelationWidgetStub) view.getTab(EditorTabs.SKU.getDisplayName());
    }

    @Test
    public void testPaging() {
        int pagemax = SkuTableUiFunctionalityAddon.SKU_PER_PAGE_LIMIT;

        //Убедимся, что сразу после загрузки аддона, он раскидал данные по страницам.
        assertEquals(pagemax, skuWidget.getSkus().size());
        assertEquals(skus.get(0), skuWidget.getSkus().get(0)); //первая ску
        assertEquals(skus.get(pagemax - 1),
            skuWidget.getSkus().get(skuWidget.getSkus().size() - 1)); //конец страницы - n-ная ску.

        skuWidget.setPage(2);

        //Теперь мы должны оказаться на второй странице
        assertEquals(pagemax, skuWidget.getSkus().size());
        assertEquals(skus.get(pagemax), skuWidget.getSkus().get(0)); //первая ску
        assertEquals(skus.get(2 * pagemax - 1),
            skuWidget.getSkus().get(skuWidget.getSkus().size() - 1)); //конец страницы - n-ная ску.
    }

    @Test
    public void testBooleanFilter() {
        SkuTableFilterStub filter = skuWidget.getFilter(booleanParam);
        List suggestOptions = filter.click(); //для булей нет опций
        assertTrue(suggestOptions.isEmpty());

        //Оставляем только комплекты с конём:
        filter.applyBooleanFilter(true);
        skuWidget.getSkus().forEach(sku -> {
            boolean horseIncluded = sku.getSingleParameterValue(booleanParam.getId()).getBooleanValue();
            assertTrue(horseIncluded);
        });

        //Оставляем комплекты без коня:
        filter.applyBooleanFilter(false);
        skuWidget.getSkus().forEach(sku -> {
            boolean horseIncluded = sku.getSingleParameterValue(booleanParam.getId()).getBooleanValue();
            assertFalse(horseIncluded);
        });

        //Поищем комплекты, где этот параметр не установлен. У нас таких нет:
        filter.applyBooleanFilter(new Boolean[] {null});
        assertTrue(skuWidget.getSkus().isEmpty());

        //Сбросим
        filter.partialReset();
        boolean skuWithHorseExists = false;
        boolean skuWithoutHorseExists = false;
        for (CommonModel sku : skuWidget.getSkus()) {
            boolean horseIncluded = sku.getSingleParameterValue(booleanParam.getId()).getBooleanValue();
            skuWithHorseExists |= horseIncluded;
            skuWithoutHorseExists |= !horseIncluded;
        }
        assertTrue(skuWithHorseExists && skuWithoutHorseExists);
    }

    @Test
    public void testStringFilter() {
        SkuTableFilterStub filter = skuWidget.getFilter(stringParam);
        List suggestOptions = filter.click(); //для строк нет опций
        assertTrue(suggestOptions.isEmpty());

        //Оставляем только комплекты с клеймом мастеров, номера которых входят в список:
        filter.applyStringFilter("10", "12", "97", "62");
        assertEquals(4, skuWidget.getSkus().size());
        skuWidget.getSkus().forEach(sku -> {
            String brand = WordUtil.getDefaultWord(sku.getSingleParameterValue(stringParam.getId()).getStringValue());
            assertTrue(brand.contains("10") || brand.contains("12") || brand.contains("97") || brand.contains("62"));
        });

        //Немного поменяем
        filter.applyStringFilter("10", "12", "57");
        assertEquals(3, skuWidget.getSkus().size());
        skuWidget.getSkus().forEach(sku -> {
            String brand = WordUtil.getDefaultWord(sku.getSingleParameterValue(stringParam.getId()).getStringValue());
            assertTrue(brand.contains("10") || brand.contains("12") || brand.contains("57"));
        });

        //Сбросим
        filter.partialReset();
        assertEquals(SkuTableUiFunctionalityAddon.SKU_PER_PAGE_LIMIT, skuWidget.getSkus().size());
    }

    @Test
    public void testNumericFilter() {
        SkuTableFilterStub filter = skuWidget.getFilter(numericParam);
        List suggestOptions = filter.click(); //для числовых нет опций
        assertTrue(suggestOptions.isEmpty());

        //Оставляем только комплекты с возрастом, номера которых входят в список:
        filter.applyNumericFilter(10, 12, 97, 62);
        assertEquals(4, skuWidget.getSkus().size());
        skuWidget.getSkus().forEach(sku -> {
            int useYears = sku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            assertThat(useYears, isOneOf(10, 12, 97, 62));
        });

        //Немного поменяем
        filter.applyNumericFilter(10, 12, 57);
        assertEquals(3, skuWidget.getSkus().size());
        skuWidget.getSkus().forEach(sku -> {
            int useYears = sku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            assertThat(useYears, isOneOf(10, 12, 57));
        });

        //Сбросим
        filter.partialReset();
        assertEquals(SkuTableUiFunctionalityAddon.SKU_PER_PAGE_LIMIT, skuWidget.getSkus().size());
    }

    @Test
    public void testEnumFilter() {
        SkuTableFilterStub filter = skuWidget.getFilter(enumParam);
        List suggestOptions = filter.click();
        assertEquals(4, suggestOptions.size());

        //Оставляем только комплекты с большим нагрудником и наплечниками:
        filter.applyStringFilter("А Нагрудник XXL", "Б Стальные наплечники");
        skuWidget.getSkus().forEach(sku -> {
            long optionId = sku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            assertThat(optionId, isOneOf(124L, 125L));
        });

        //Немного поменяем
        filter.applyStringFilter("А Нагрудник XXL", "В Баклер", "Г Доп. инкрустация");
        skuWidget.getSkus().forEach(sku -> {
            long optionId = sku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            assertThat(optionId, isOneOf(124L, 126L, 127L));
        });

        //Сбросим
        filter.partialReset();
        assertEquals(SkuTableUiFunctionalityAddon.SKU_PER_PAGE_LIMIT, skuWidget.getSkus().size());
    }

    @Test
    public void testSeveralFilters() {
        SkuTableFilterStub numericFilter = skuWidget.getFilter(numericParam);
        SkuTableFilterStub enumFilter = skuWidget.getFilter(enumParam);

        enumFilter.applyStringFilter("А Нагрудник XXL", "Б Стальные наплечники");
        skuWidget.getSkus().forEach(sku -> {
            long optionId = sku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            assertThat(optionId, isOneOf(124L, 125L));
        });

        numericFilter.applyNumericFilter(10, 11, 12, 13);
        assertTrue(skuWidget.getSkus().size() <= 4);
        skuWidget.getSkus().forEach(sku -> {
            int useYears = sku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            long optionId = sku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            assertThat(useYears, isOneOf(10, 11, 12, 13));
            assertThat(optionId, isOneOf(124L, 125L));
        });

        enumFilter.partialReset();
        boolean otherOptionsExist = false;
        assertTrue(skuWidget.getSkus().size() <= 4);
        for (CommonModel sku : skuWidget.getSkus()) {
            int useYears = sku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            assertThat(useYears, isOneOf(10, 11, 12, 13));

            long optionId = sku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            if (optionId == 126L || optionId == 127L) {
                otherOptionsExist = true;
            }
        }
        assertTrue(otherOptionsExist);

        enumFilter.fullReset();
        assertEquals(SkuTableUiFunctionalityAddon.SKU_PER_PAGE_LIMIT, skuWidget.getSkus().size());
    }

    @Test
    public void testStringSorting() {
        SkuTableFilterStub filter = skuWidget.getFilter(stringParam);
        filter.applySorting(SkuTableSortedEvent.Direction.ASC);

        Iterator<CommonModel> iSku = skuWidget.getSkus().iterator();
        CommonModel currentSku;
        CommonModel nextSku = iSku.next();
        while (iSku.hasNext()) {
            currentSku = nextSku;
            nextSku = iSku.next();
            String brand1 = WordUtil.getDefaultWord(
                currentSku.getSingleParameterValue(stringParam.getId()).getStringValue());
            String brand2 = WordUtil.getDefaultWord(
                nextSku.getSingleParameterValue(stringParam.getId()).getStringValue());
            assertTrue(brand2.compareTo(brand1) >= 0);
        }

        filter.applySorting(SkuTableSortedEvent.Direction.DESC);
        iSku = skuWidget.getSkus().iterator();
        nextSku = iSku.next();
        while (iSku.hasNext()) {
            currentSku = nextSku;
            nextSku = iSku.next();
            String brand1 = WordUtil.getDefaultWord(
                currentSku.getSingleParameterValue(stringParam.getId()).getStringValue());
            String brand2 = WordUtil.getDefaultWord(
                nextSku.getSingleParameterValue(stringParam.getId()).getStringValue());
            assertTrue(brand2.compareTo(brand1) <= 0);
        }
    }

    @Test
    public void testNumericSorting() {
        SkuTableFilterStub filter = skuWidget.getFilter(numericParam);
        filter.applySorting(SkuTableSortedEvent.Direction.ASC);

        Iterator<CommonModel> iSku = skuWidget.getSkus().iterator();
        CommonModel currentSku;
        CommonModel nextSku = iSku.next();
        while (iSku.hasNext()) {
            currentSku = nextSku;
            nextSku = iSku.next();
            int useYears1 = currentSku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            int useYears2 = nextSku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            assertTrue(useYears2 >= useYears1);
        }

        filter.applySorting(SkuTableSortedEvent.Direction.DESC);
        iSku = skuWidget.getSkus().iterator();
        nextSku = iSku.next();
        while (iSku.hasNext()) {
            currentSku = nextSku;
            nextSku = iSku.next();
            int useYears1 = currentSku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            int useYears2 = nextSku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            assertTrue(useYears2 <= useYears1);
        }
    }

    @Test
    public void testEnumSorting() {
        SkuTableFilterStub filter = skuWidget.getFilter(enumParam);
        filter.applySorting(SkuTableSortedEvent.Direction.ASC);

        //Для енумов сортировка происходит по тексту опций, но у нас нарочно идшники и тесты идут по возрастанию,
        //так что можно просто сравнивать ИД
        Iterator<CommonModel> iSku = skuWidget.getSkus().iterator();
        CommonModel currentSku;
        CommonModel nextSku = iSku.next();
        while (iSku.hasNext()) {
            currentSku = nextSku;
            nextSku = iSku.next();
            long optionId1 = currentSku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            long optionId2 = nextSku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            assertTrue(optionId2 >= optionId1);
        }

        filter.applySorting(SkuTableSortedEvent.Direction.DESC);
        iSku = skuWidget.getSkus().iterator();
        nextSku = iSku.next();
        while (iSku.hasNext()) {
            currentSku = nextSku;
            nextSku = iSku.next();
            long optionId1 = currentSku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            long optionId2 = nextSku.getParameterValues(enumParam.getId()).getOptionIds().get(0);
            assertTrue(optionId2 <= optionId1);
        }
    }

    @Test
    public void testFiltersAndSorting() {
        //Оставляем только комплекты с конём:
        skuWidget.getFilter(booleanParam).applyBooleanFilter(true);
        skuWidget.getSkus().forEach(sku -> {
            boolean horseIncluded = sku.getSingleParameterValue(booleanParam.getId()).getBooleanValue();
            assertTrue(horseIncluded);
        });

        SkuTableFilterStub filter = skuWidget.getFilter(numericParam);
        filter.applySorting(SkuTableSortedEvent.Direction.DESC);
        Iterator<CommonModel> iSku = skuWidget.getSkus().iterator();
        CommonModel currentSku;
        CommonModel nextSku = iSku.next();
        while (iSku.hasNext()) {
            currentSku = nextSku;
            nextSku = iSku.next();
            int useYears1 = currentSku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            int useYears2 = nextSku.getSingleParameterValue(numericParam.getId()).getNumericValue().intValue();
            assertTrue(useYears2 <= useYears1);
            boolean horseIncluded1 = currentSku.getSingleParameterValue(booleanParam.getId()).getBooleanValue();
            boolean horseIncluded2 = nextSku.getSingleParameterValue(booleanParam.getId()).getBooleanValue();
            assertTrue(horseIncluded1 && horseIncluded2);
        }
    }

    private void prepareParams() {
        booleanParam = CategoryParamBuilder.newBuilder()
            .setId(1)
            .setCategoryHid(CATEGORY_ID)
            .setName("В комплект входит конь")
            .setType(Param.Type.BOOLEAN)
            .setXslName("")
            .addOption(new OptionImpl())
            .setSkuParameterMode(SkuParameterMode.SKU_DEFINING)
            .build();

        stringParam = CategoryParamBuilder.newBuilder()
            .setId(2)
            .setCategoryHid(CATEGORY_ID)
            .setName("Клеймо мастера")
            .setType(Param.Type.STRING)
            .setXslName("")
            .setSkuParameterMode(SkuParameterMode.SKU_DEFINING)
            .build();

        numericParam = CategoryParamBuilder.newBuilder()
            .setId(3)
            .setCategoryHid(CATEGORY_ID)
            .setName("Возраст, лет")
            .setType(Param.Type.NUMERIC)
            .setXslName("")
            .setSkuParameterMode(SkuParameterMode.SKU_DEFINING)
            .build();

        enumParam = CategoryParamBuilder.newBuilder()
            .setId(4)
            .setCategoryHid(CATEGORY_ID)
            .setName("Дополнительные опции")
            .setType(Param.Type.ENUM)
            .setXslName("")
            .setMultifield(true)
            .addOption(new OptionImpl(124, "А Нагрудник XXL"))
            .addOption(new OptionImpl(125, "Б Стальные наплечники"))
            .addOption(new OptionImpl(126, "В Баклер"))
            .addOption(new OptionImpl(127, "Г Доп. инкрустация"))
            .setSkuParameterMode(SkuParameterMode.SKU_DEFINING)
            .build();
    }

    private ParameterValue getParameterValue(CategoryParam param, int idx) {
        if (param == booleanParam) {
            ParameterValue p = new ParameterValue(booleanParam);
            p.setBooleanValue(idx % 2 == 0);
            p.setOptionId(100500L); // not used, for isEmpty checks only
            return p;
        }
        if (param == stringParam) {
            return new ParameterValue(stringParam, WordUtil.defaultWords(idx + "-й своего имени, " +
                "королевский кузнец"));
        }
        if (param == numericParam) {
            return new ParameterValue(numericParam, BigDecimal.valueOf(idx));
        }
        if (param == enumParam) {
            long optionId = 124 + (idx % 4);
            return new ParameterValue(enumParam, optionId);
        }
        return null;
    }

}
