package ru.yandex.market.export.models;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.modelstorage.IndexedModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.params.GLRulesService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.db.size.SizeChartStorageService;
import ru.yandex.market.mbo.db.size.SizeChartStorageServiceImpl;
import ru.yandex.market.mbo.export.modelstorage.CategoryInfoLoaderImpl;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.user.AutoUser;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.export.models.AbstractCategoryProtoServlet.ONLY_PUBLISHED;
import static ru.yandex.market.export.models.CategoryModelsProtoServlet.EXTERNAL;
import static ru.yandex.market.export.models.CategoryModelsProtoServlet.EXTRACT_TYPES;

/**
 * @author york
 * @since 03.04.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryModelsProtoServletTest {

    private static final Long CATEGORY_ID = 100L;
    private static final String TITLE_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(\"Title\" ),null,(false)]]}";

    private CategoryModelsProtoServlet servlet;
    private ModelStorage.Model model;
    private ModelStorage.Model modification1;
    private ModelStorage.Model modification2;
    private ModelStorage.Model sku1;
    private ModelStorage.Model sku2;

    @Before
    @SuppressWarnings("checkstyle:methodLength")
    public void setUp() {
        long i = 1;
        GuruService guruService = mock(GuruService.class);
        Mockito.when(guruService.isGroupCategory(anyLong())).thenReturn(true);
        TovarTreeService tovarTreeService = mock(TovarTreeService.class);
        TitlemakerTemplateDao titlemakerTemplateDao = mock(TitlemakerTemplateDao.class);
        when(tovarTreeService.loadCategoryByHid(anyLong())).thenAnswer(invocation -> {
            long hid = invocation.getArgument(0);
            TovarCategory category = new TovarCategory();
            category.setHid(hid);
            category.setGuruCategoryId(1L);
            category.setPublished(true);
            category.setShowModelTypes(Arrays.asList(CommonModel.Source.GURU));
            return category;
        });
        when(titlemakerTemplateDao.loadTemplateByHid(anyLong())).thenAnswer(invocation -> {
            TMTemplate template = new TMTemplate();
            template.setValue(TITLE_TEMPLATE);
            template.setHasGuruTemplate(false);
            template.setSkuTemplate(TITLE_TEMPLATE);
            return template;
        });

        ParametersBuilder<CommonModelBuilder<CommonModel>> parametersBuilder =
            ParametersBuilder.defaultBuilder();

        List<CategoryParam> parameters = parametersBuilder.getParameters();

        IParameterLoaderService parameterLoaderService = mock(IParameterLoaderService.class);
        when(parameterLoaderService.loadCategoryEntitiesByHid(anyLong())).thenAnswer(invocation -> {
            CategoryEntities categoryEntities = new CategoryEntities();
            categoryEntities.setHid(invocation.getArgument(0));
            categoryEntities.setParameters(parameters);
            return categoryEntities;
        });

        SizeMeasureService sizeMeasureService = Mockito.mock(SizeMeasureService.class);
        when(sizeMeasureService.listSizeMeasures()).thenReturn(Collections.emptyList());

        IndexedModelStorageService indexedModelStorageService = mock(IndexedModelStorageService.class);
        model = ModelStorage.Model.newBuilder() //1
            .setId(i++)
            .setCategoryId(CATEGORY_ID)
            .setVendorId(ParametersBuilder.GLOBAL_VENDOR_1_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .addTitles(ModelStorage.LocalizedString.getDefaultInstance())
            .setPublished(true)
            .build();

        long sku1Id = i++;
        modification1 = ModelStorage.Model.newBuilder() //3
            .setId(i++)
            .setCategoryId(CATEGORY_ID)
            .setVendorId(ParametersBuilder.GLOBAL_VENDOR_1_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .addTitles(ModelStorage.LocalizedString.getDefaultInstance())
            .setParentId(model.getId())
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(sku1Id)
            )
            .setPublished(true)
            .build();

        long sku2Id = i++;
        modification2 = ModelStorage.Model.newBuilder() //5
            .setId(i++)
            .setCategoryId(CATEGORY_ID)
            .setVendorId(ParametersBuilder.GLOBAL_VENDOR_1_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .addTitles(ModelStorage.LocalizedString.getDefaultInstance())
            .setParentId(model.getId())
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .setId(sku2Id)
            )
            .setPublished(false)
            .build();

        sku1 = ModelStorage.Model.newBuilder() //2
            .setId(sku1Id)
            .setCategoryId(CATEGORY_ID)
            .setVendorId(ParametersBuilder.GLOBAL_VENDOR_1_ID)
            .setCurrentType(CommonModel.Source.SKU.name())
            .addTitles(ModelStorage.LocalizedString.getDefaultInstance())
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(modification1.getId())
            )
            .setPublished(true)
            .build();
        sku2 = ModelStorage.Model.newBuilder() //4
            .setId(sku2Id)
            .setCategoryId(CATEGORY_ID)
            .setVendorId(ParametersBuilder.GLOBAL_VENDOR_1_ID)
            .setCurrentType(CommonModel.Source.SKU.name())
            .addTitles(ModelStorage.LocalizedString.getDefaultInstance())
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(modification2.getId())
            )
            .setPublished(true)
            .build();

        doAnswer(invocation -> {
            MboIndexesFilter filter = invocation.getArgument(1);
            Consumer<ModelStorage.Model> callback = invocation.getArgument(2);

            if (filter.getCurrentTypes().contains(CommonModel.Source.SKU)) {
                callback.accept(sku1);
                callback.accept(sku2);
            } else
            if (filter.getParentIdExists() != null && !filter.getParentIdExists()) {
                callback.accept(model);
            } else {
                callback.accept(modification1);
                callback.accept(modification2);
            }
            return null;
        }).when(indexedModelStorageService).processQueryFullModels(anyLong(), any(), any());

        ValueLinkServiceInterface valueLinkService = mock(ValueLinkServiceInterface.class);
        when(valueLinkService.findValueLinks(any())).thenReturn(Collections.emptyList());

        GLRulesService glRulesService = mock(GLRulesService.class);

        SizeChartStorageService sizeChartStorageService = mock(SizeChartStorageServiceImpl.class);

        CategoryInfoLoaderImpl categoryInfoLoader = new CategoryInfoLoaderImpl(
            sizeMeasureService,
            tovarTreeService,
            titlemakerTemplateDao,
            parameterLoaderService,
            valueLinkService,
            glRulesService,
            sizeChartStorageService
        );
        servlet = new CategoryModelsProtoServlet(indexedModelStorageService,
            guruService, tovarTreeService, categoryInfoLoader, new AutoUser(666), false, false);
    }

    @Test
    public void testDefault() throws Exception {
        List<ModelStorage.Model> result = doRun(new HashMap<>());
        Set<Long> ids = result.stream().map(m -> m.getId()).collect(Collectors.toSet());
        //model is skipped cause external not grouped
        compare(result, modification1, modification2);
    }

    @Test
    public void testNotExternal() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put(EXTERNAL, false);
        List<ModelStorage.Model> result = doRun(args);
        compare(result, model, modification1, modification2);
        Assert.assertEquals(false, findInResults(model, result).getPublishedOnMarket());
        Assert.assertEquals(false, findInResults(model, result).getPublishedOnBlueMarket());
        Assert.assertEquals(true, findInResults(modification1, result).getPublishedOnMarket());
        Assert.assertEquals(true, findInResults(modification1, result).getPublishedOnBlueMarket());
        Assert.assertEquals(false, findInResults(modification2, result).getPublishedOnMarket());
        Assert.assertEquals(false, findInResults(modification2, result).getPublishedOnBlueMarket());
    }

    @Test
    public void testAll() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put(EXTERNAL, false);
        Collection<String> types = Arrays.asList(CommonModel.Source.GURU.name(), CommonModel.Source.SKU.name());
        args.put(EXTRACT_TYPES, String.join(",", types));
        List<ModelStorage.Model> result = doRun(args);
        compare(result, model, modification1, modification2, sku1, sku2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllOnlyPublished() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put(EXTERNAL, false);
        args.put(ONLY_PUBLISHED, true);
        Collection<String> types = Arrays.asList(CommonModel.Source.GURU.name(), CommonModel.Source.SKU.name());
        args.put(EXTRACT_TYPES, String.join(",", types));
        List<ModelStorage.Model> result = doRun(args);
    }

    @Test
    public void testSKU() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put(EXTERNAL, false);
        args.put(EXTRACT_TYPES, CommonModel.Source.SKU.name());
        List<ModelStorage.Model> result = doRun(args);
        compare(result, sku1, sku2);
    }

    private List<ModelStorage.Model> doRun(Map<String, Object> args) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        servlet.writeProtobuf(mockRequest(args), bos);
        ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
        byte[] magic = new byte[4];
        ModelStorage.Model mdl;
        is.read(magic);
        List<ModelStorage.Model> result = new ArrayList<>();
        while ((mdl = ModelStorage.Model.PARSER.parseDelimitedFrom(is)) != null) {
            result.add(mdl);
        }
        return result;
    }

    private HttpServletRequest mockRequest(Map<String, Object> args) {
        args.put(AbstractCategoryProtoServlet.CATEGORY_ID, CATEGORY_ID);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getParameter(any())).thenAnswer(i -> {
            Object obj = args.get(i.getArgument(0));
            return obj == null ? obj : obj.toString();
        });
        return httpServletRequest;
    }

    private void compare(List<ModelStorage.Model> result, ModelStorage.Model... models) {
        Set<Long> ids = result.stream().map(m -> m.getId()).collect(Collectors.toCollection(HashSet::new));
        result.forEach(m -> {
            Assert.assertTrue("Excess id " + m.getId(), ids.remove(m.getId()));
        });
        Assert.assertTrue("Not in result: " + ids, ids.isEmpty());
    }

    private ModelStorage.Model findInResults(ModelStorage.Model origin, List<ModelStorage.Model> result) {
        return result.stream().filter(m -> m.getId() == origin.getId())
            .findFirst()
            .orElse(null);
    }
}
