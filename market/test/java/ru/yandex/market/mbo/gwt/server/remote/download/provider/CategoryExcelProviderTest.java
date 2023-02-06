package ru.yandex.market.mbo.gwt.server.remote.download.provider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.MeasureService;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.VisualService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.GuruParamFilter;
import ru.yandex.market.mbo.gwt.models.params.Measure;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.Unit;
import ru.yandex.market.mbo.gwt.models.params.UnitAlias;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.server.remote.upload.ParametersExcelLoader;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings({"checkstyle:parameternumber", "checkstyle:magicNumber"})
public class CategoryExcelProviderTest {

    private static final long BASE_ID = 13908L;
    private static final int ID0 = 0;
    private static final int ID1 = 1;
    private static final int ID2 = 2;
    private static final int ID3 = 3;
    private static final int ID4 = 4;
    private static final List<Long> IDS = Arrays.asList(
        BASE_ID + ID0, BASE_ID + ID1, BASE_ID + ID2, BASE_ID + ID3, BASE_ID + ID4);
    private static final String COMMENT_VAL = "Some operator comment";
    private static final String DESCR_VAL = "Some description text";
    private static final CategoryEntities CATEGORY_ENTITY = new CategoryEntities(1, Collections.emptyList());
    private static final long MEASURE_ID = 1;
    private static final long UNIT_ID = 100;

    private IParameterLoaderService loaderService;
    private CategoryExcelProvider fileProvider;

    private MeasureService measureService;
    private VisualService visualService;
    private ParametersExcelLoader loader;

    @BeforeClass
    public static void init() {
        List<CategoryParam> params = generateParams();
        CATEGORY_ENTITY.setParameters(params);
    }

    @Before
    public void setUp() {
        loaderService = new ParameterLoaderServiceStub(CATEGORY_ENTITY);
        measureService = Mockito.mock(MeasureService.class);
        visualService = Mockito.mock(VisualService.class);

        Measure measure = new Measure(MEASURE_ID, UNIT_ID);
        measure.addMeasureUnit(new Unit("сантиметр", "сантиметр",
                new BigDecimal(0), MEASURE_ID, UNIT_ID));
        measure.addName(Language.RUSSIAN.getId(), "Длина");

        Mockito.when(measureService.getMeasures()).thenReturn(List.of(measure));
        fileProvider = new CategoryExcelProvider(measureService, visualService, loaderService);
    }

    @Test
    public void checkNonFilteredWorkbook() {
        List<CategoryParam> params = loaderService.loadCategoryEntitiesByHid(1).getParameters();
        Workbook book = fileProvider.createWorkbook(params);
        loader = new ParametersExcelLoader(book);
        List<CategoryParam> categoryParams = loader.loadParameters();
        assertEquals(params.size(), categoryParams.size());
        assertEquals(10, book.getSheet("Данные").getPhysicalNumberOfRows());

        CategoryParam expectedParam = params.get(0);
        CategoryParam actualParam = categoryParams.get(0);
        assertEquals(expectedParam.getId(), actualParam.getId());
        assertEquals(expectedParam.getName(), actualParam.getName());
        assertEquals(expectedParam.getLocalizedAliases(), actualParam.getLocalizedAliases());
        assertEquals(expectedParam.getXslName(), actualParam.getXslName());
        assertEquals(expectedParam.getType(), actualParam.getType());
        assertEquals(expectedParam.isUseForGuru(), actualParam.isUseForGuru());
        assertEquals(expectedParam.getCommentForOperator(), actualParam.getCommentForOperator());

        List<Option> expectedParamOptions = expectedParam.getOptions();
        List<Option> actualParamOptions = actualParam.getOptions();
        assertEquals(expectedParamOptions.size(), actualParamOptions.size());

        Option expectedOption = expectedParamOptions.get(0);
        Option actualOption = expectedParamOptions.get(0);
        assertEquals(expectedOption.getId(), actualOption.getId());
        assertEquals(expectedOption.getName(), actualOption.getName());
        assertEquals(expectedOption.getLocalizedAliases(), actualOption.getLocalizedAliases());
    }

    @Test
    public void checkFilteredByType() {
        GuruParamFilter filter = defaultFilter();
        filter.setType(Param.Type.ENUM);
        List<CategoryParam> params = loaderService.loadFilteredParameters(1, filter);
        params.forEach(param -> assertEquals(Param.Type.ENUM, param.getType()));
    }

    @Test
    public void checkFilteredByGuru() {
        GuruParamFilter filter = defaultFilter();
        filter.setUseForGuru(1);
        List<CategoryParam> params = loaderService.loadFilteredParameters(1, filter);
        params.forEach(param -> assertEquals(true, param.isUseForGuru()));
    }

    @Test
    public void checkFilteredByService() {
        GuruParamFilter filter = defaultFilter();
        filter.setService(0);
        List<CategoryParam> params = loaderService.loadFilteredParameters(1, filter);
        params.forEach(param -> assertEquals(false, param.isService()));
    }

    @Test
    public void checkFilteredByTypeAndGuru() {
        GuruParamFilter filter = defaultFilter();
        filter.setType(Param.Type.ENUM);
        filter.setUseForGuru(1);
        List<CategoryParam> params = loaderService.loadFilteredParameters(1, filter);
        params.forEach(param -> {
            assertEquals(Param.Type.ENUM, param.getType());
            assertEquals(true, param.isUseForGuru());
        });
    }

    @Test
    public void checkFilteredByTypeAndService() {
        GuruParamFilter filter = defaultFilter();
        filter.setType(Param.Type.ENUM);
        filter.setService(1);
        List<CategoryParam> params = loaderService.loadFilteredParameters(1, filter);
        params.forEach(param -> {
            assertEquals(Param.Type.ENUM, param.getType());
            assertEquals(true, param.isService());
        });
    }

    @Test
    public void checkFilteredByTypeAndGuruAndImportant() {
        GuruParamFilter filter = defaultFilter();
        filter.setType(Param.Type.STRING);
        filter.setUseForGuru(1);
        filter.setImportant(1);
        List<CategoryParam> params = loaderService.loadFilteredParameters(1, filter);
        params.forEach(param -> {
            assertEquals(true, param.isUseForGuru());
            assertEquals(true, param.isImportant());
        });
    }

    @Test
    public void checkNoMatch() {
        GuruParamFilter filter = defaultFilter();
        filter.setType(Param.Type.NUMERIC_ENUM);
        List<CategoryParam> params = loaderService.loadFilteredParameters(1, filter);
        assertEquals(0, params.size());
    }

    /**
     * Генерируем параметры категории, в первом приближении похожие на настоящие.
     * @return набор разношёрстных параметров категории.
     */
    private static List<CategoryParam> generateParams() {
        List<CategoryParam> params = new ArrayList<>();

        List<String> aliases = List.of("first", "second", "third");

        String expectedAliasName = "Unit alias";
        Unit unit = new Unit("UnitName", expectedAliasName, BigDecimal.ONE, 1, 1);
        unit.getAliases().add(new UnitAlias(1, "wrong", Word.DEFAULT_LANG_ID, true));

        params.add(createCategoryParam(
                IDS.get(ID0),
                Param.Type.ENUM,
                CategoryParam.Level.MODEL,
                true,
                true,
                false,
                false,
                false,
                false,
                aliases,
                "param1",
                null,
                null,
                null,
                BigDecimal.ONE,
                "name1"
        ));

        params.add(createCategoryParam(
                IDS.get(ID1),
                Param.Type.STRING,
                CategoryParam.Level.OFFER,
                true,
                false,
                false,
                false,
                false,
                false,
                Collections.emptyList(),
                "param2",
                null,
                null,
                null,
                BigDecimal.ONE,
                "name2"
        ));

        params.add(createCategoryParam(
                IDS.get(ID2),
                Param.Type.ENUM,
                CategoryParam.Level.MODEL,
                true,
                true,
                true,
                true,
                false,
                false,
                aliases,
                "param3",
                null,
                null,
                null,
                BigDecimal.ZERO,
                "name3"
        ));

        params.add(createCategoryParam(
                IDS.get(ID3),
                Param.Type.NUMERIC,
                CategoryParam.Level.OFFER,
                true,
                false,
                true,
                true,
                true,
                true,
                Collections.singletonList(aliases.get(0)),
                "param4",
                unit,
                BigDecimal.valueOf(10L),
                BigDecimal.valueOf(255L),
                BigDecimal.ONE,
                "name4"
        ));
        params.add(createCategoryParam(
                IDS.get(ID4),
                Param.Type.STRING,
                CategoryParam.Level.OFFER,
                true,
                false,
                false,
                false,
                false,
                true,
                Collections.emptyList(),
                "param5",
                null,
                null,
                null,
                BigDecimal.ZERO,
                "name5"
        ));
        return params;
    }

    private static CategoryParam createCategoryParam(long id,
                                                     Param.Type type,
                                                     CategoryParam.Level level,
                                                     boolean useForGuru,
                                                     boolean useForGurulight,
                                                     boolean hidden,
                                                     boolean service,
                                                     boolean mandatoryForSignature,
                                                     boolean important,
                                                     List<String> aliases,
                                                     String xslName,
                                                     Unit unit,
                                                     BigDecimal minValue,
                                                     BigDecimal maxValue,
                                                     BigDecimal importance,
                                                     String name) {
        CategoryParam param = new Parameter();
        param.setId(id);
        param.setType(type);
        param.setLevel(level);
        param.setUseForGuru(useForGuru);
        param.setUseForGurulight(useForGurulight);
        param.setHidden(hidden);
        param.setService(service);
        param.setMandatoryForSignature(mandatoryForSignature);
        param.setImportant(important);
        param.setCommentForOperator(COMMENT_VAL);
        param.setDescription(DESCR_VAL);
        param.setLocalizedAliases(WordUtil.defaultWords(aliases));
        param.setXslName(xslName);
        param.setNames(WordUtil.defaultWords(name));
        if (type == Param.Type.ENUM) {
            Option firstOption = new OptionImpl();
            firstOption.setId(1L);
            firstOption.setNames(WordUtil.defaultWords("Option"));
            firstOption.addAlias(WordUtil.defaultEnumAlias("firstAlias"));
            firstOption.addAlias(WordUtil.defaultEnumAlias("secondAlias"));

            Option secondOption = new OptionImpl();
            secondOption.setId(2L);
            secondOption.setNames(WordUtil.defaultWords("Name"));
            secondOption.addAlias(WordUtil.defaultEnumAlias("anyWord"));
            param.setOptions(List.of(firstOption, secondOption));
        }
        if (type == Param.Type.NUMERIC) {
            param.setUnit(unit);
            param.setMeasureId(1L);
            param.setMinValue(minValue);
            param.setMaxValue(maxValue);
            param.setPrecision(6);
        } else {
            param.setPrecision(null);
        }
        param.setImportance(importance);
        return param;
    }

    private static GuruParamFilter defaultFilter() {
        GuruParamFilter filter = new GuruParamFilter();
        filter.setText(null);
        filter.setType(null);
        filter.setLevel(null);
        filter.setHidden(-1);
        filter.setService(-1);
        filter.setCommon(-1);
        filter.setUseForGuru(-1);
        filter.setUseForGurulight(-1);
        filter.setMandatoryForSignature(-1);
        filter.setImportant(-1);
        return filter;
    }
}
