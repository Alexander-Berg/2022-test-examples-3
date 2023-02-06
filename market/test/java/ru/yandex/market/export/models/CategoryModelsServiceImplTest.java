package ru.yandex.market.export.models;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.mbo.core.guru.GuruCategoryService;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.ModelStopWordsDao;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.params.GLRulesService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.size.SizeChartStorageService;
import ru.yandex.market.mbo.db.size.SizeChartStorageServiceImpl;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.export.modelstorage.CategoryInfoLoaderImpl;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkSearchCriteria;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.common.model.KnownIds.VENDOR_PARAM_ID;
import static ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder.GLOBAL_VENDOR_1_ID;
import static ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder.GLOBAL_VENDOR_2_ID;
import static ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder.LOCAL_VENDOR_1_ID;
import static ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder.LOCAL_VENDOR_2_ID;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class CategoryModelsServiceImplTest {

    private static final String GURU_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v7893318 )],[(t0 ),(t0 )]]}";
    private static final String GURU_TEMPLATE_WITHOUT_VENDOR =
        "{\"delimiter\":\" \",\"values\":[[(t0 ),(t0 )]]}";

    @Mock
    private TovarTreeService tovarTreeService;

    @Mock
    private TitlemakerTemplateDao titlemakerTemplateDao;

    @Mock
    private IParameterLoaderService parameterLoaderService;

    @Mock
    private SizeMeasureService sizeMeasureService;

    @Mock
    private GuruCategoryService guruCategoryService;

    @Mock
    private ModelStorageService modelStorageService;

    @Mock
    private AutoUser autoUser;

    @Mock
    private ValueLinkServiceInterface valueLinkService;

    @Mock
    private ModelStopWordsDao modelStopWordsDao;

    private CategoryModelsServiceImpl service;

    private ModelStorage.Model parent1out;
    private ModelStorage.Model parent2out;
    private ModelStorage.Model modif3out;
    private ModelStorage.Model modif4out;
    private ModelStorage.Model sku5out;
    private ModelStorage.Model sku6out;
    private ModelStorage.Model sku7out;
    private ModelStorage.Model guru8out;
    private ModelStorage.Model sku8out;
    private ModelStorage.Model modelWithManufacturerParamOut;

    private Map<Long, ModelStorage.Model> modelsMap;

    @Before
    public void setUp() {
        TovarCategory category = new TovarCategory("test", 1L, 0L);
        category.setPublished(true);
        category.setGuruCategoryId(2L);
        category.setGroup(false);
        category.setShowModelTypes(Collections.singletonList(CommonModel.Source.GURU));

        TMTemplate template = new TMTemplate();
        template.setHasGuruTemplate(true);
        template.setGuruTemplate(GURU_TEMPLATE);
        template.setHasGuruTemplateWithoutVendor(true);
        template.setGuruTemplateWithoutVendor(GURU_TEMPLATE_WITHOUT_VENDOR);

        when(tovarTreeService.getCategoryByHid(anyLong())).thenReturn(category);
        when(tovarTreeService.loadCategoryByHid(anyLong())).thenReturn(category);
        when(titlemakerTemplateDao.loadTemplateByHid(anyLong())).thenReturn(template);

        ParametersBuilder<CommonModelBuilder<CommonModel>> parametersBuilder =
            ParametersBuilder.defaultBuilder();

        List<CategoryParam> parameters = parametersBuilder.getParameters();
        CategoryParam manufacturerParam = new Parameter();
        manufacturerParam.setXslName(XslNames.MANUFACTURER);
        parameters.add(manufacturerParam);

        CategoryEntities entities = new CategoryEntities(1L, Collections.emptyList());
        entities.addAllParameters(parameters);
        when(parameterLoaderService.loadCategoryEntitiesByHid(anyLong())).thenReturn(entities);

        when(sizeMeasureService.listSizeMeasures(anyLong())).thenReturn(Collections.emptyList());

        CategoryParam vendorParam = parameters.get(0);
        OptionImpl vendor1 = (OptionImpl) vendorParam.getOption(LOCAL_VENDOR_1_ID);
        vendor1.setPublished(true);
        OptionImpl vendor2 = (OptionImpl) vendorParam.getOption(LOCAL_VENDOR_2_ID);
        vendor2.setPublished(false);
        when(guruCategoryService.isGroupCategory(anyLong())).thenReturn(true);

        CommonModelBuilder<CommonModel> modelBuilder = parametersBuilder.endParameters();

        modelsMap = createModels(modelBuilder).stream()
            .collect(Collectors.toMap(CommonModel::getId, ModelProtoConverter::convert));

        doAnswer(i -> {
            MboIndexesFilter filter = i.getArgument(1);
            Consumer<ModelStorage.Model> consumer = i.getArgument(2);
            List<Long> ids = Collections.emptyList();
            if (CollectionUtils.isNonEmpty(filter.getModelIds())) {
                ids = new ArrayList<>(filter.getModelIds());
            }
            if (CollectionUtils.isNonEmpty(filter.getParentIds())) {
                Long parentId = filter.getParentIds().iterator().next();
                ids = modelsMap.values().stream()
                    .filter(m -> parentId == m.getParentId())
                    .map(ModelStorage.Model::getId)
                    .collect(Collectors.toList());
            }
            if (CollectionUtils.isNonEmpty(filter.getParentIds())) {
                ids = modelsMap.values().stream()
                    .filter(m -> filter.getParentIds().contains(m.getParentId()))
                    .map(ModelStorage.Model::getId)
                    .collect(Collectors.toList());
            }
            ids.stream()
                .filter(modelsMap::containsKey)
                .map(modelsMap::get)
                .distinct()
                .forEach(consumer);
            return null;
        }).when(modelStorageService).processQueryFullModels(anyLong(), any(MboIndexesFilter.class),
            any(Consumer.class));

        ValueLinkServiceInterface valueLinkServiceInterface = mock(ValueLinkServiceInterface.class);
        GLRulesService glRulesService = mock(GLRulesService.class);
        SizeChartStorageService sizeChartStorageService = mock(SizeChartStorageServiceImpl.class);

        CategoryInfoLoaderImpl categoryInfoLoader = new CategoryInfoLoaderImpl(
            sizeMeasureService,
            tovarTreeService,
            titlemakerTemplateDao,
            parameterLoaderService,
            valueLinkServiceInterface,
            glRulesService,
            sizeChartStorageService
        );
        service = new CategoryModelsServiceImpl(
            tovarTreeService,
            categoryInfoLoader,
            autoUser,
            guruCategoryService,
            modelStorageService,
            valueLinkService,
            null,
            null,
            modelStopWordsDao,
            true,
            false,
            false
        );
    }

    @Test
    public void queryNoType() {
        Collection<ModelStorage.Model> result = queryService(null, 1L, 4L, 6L, 8L);

        assertThat(result).containsExactlyInAnyOrder(parent1out, modif4out, sku6out, guru8out, sku8out);
    }

    @Test
    public void queryGuru() {
        Collection<ModelStorage.Model> result = queryService(CommonModel.Source.GURU, 2L, 3L, 5L, 7L, 8L);

        assertThat(result).containsExactlyInAnyOrder(parent2out, modif3out, guru8out);
    }

    @Test
    public void querySku() {
        Collection<ModelStorage.Model> result = queryService(CommonModel.Source.SKU, 2L, 3L, 5L, 7L, 8L);

        assertThat(result).containsExactlyInAnyOrder(sku5out, sku7out, sku8out);
    }

    @Test
    public void queryNoParent() {
        Collection<ModelStorage.Model> result = queryService(CommonModel.Source.SKU, 9L, 10L, 11L);

        assertThat(result).isEmpty();
    }

    @Test
    public void testSettingManufacturerParam() {
        ValueLinkSearchCriteria valueLinkSearchCriteria = new ValueLinkSearchCriteria();
        valueLinkSearchCriteria.setLinkDirection(LinkDirection.DIRECT);
        valueLinkSearchCriteria.setType(ValueLinkType.MANUFACTURER);
        valueLinkSearchCriteria.setSourceOptionIds(modelWithManufacturerParamOut.getVendorId());

        final long manufacturerId = 99L;

        doAnswer(i -> {
            ValueLink valueLink = new ValueLink();
            valueLink.setSourceParamId(VENDOR_PARAM_ID);
            valueLink.setSourceOptionId(modelWithManufacturerParamOut.getVendorId());
            valueLink.setTargetOptionId(manufacturerId);
            return Collections.singletonList(valueLink);
        }).when(valueLinkService).findValueLinks(eq(valueLinkSearchCriteria));

        List<ModelStorage.Model> result = (List<ModelStorage.Model>) queryService(CommonModel.Source.GURU, 12L);
        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).getParameterValuesList().stream()
            .filter(e -> e.getXslName().equals(XslNames.MANUFACTURER))
            .anyMatch(e -> e.getOptionId() == manufacturerId));
    }

    @Test
    public void testSettingManufacturerParamIfParamAlreadyExists() {
        ValueLinkSearchCriteria valueLinkSearchCriteria = new ValueLinkSearchCriteria();
        valueLinkSearchCriteria.setLinkDirection(LinkDirection.DIRECT);
        valueLinkSearchCriteria.setType(ValueLinkType.MANUFACTURER);
        valueLinkSearchCriteria.setSourceOptionIds(modelWithManufacturerParamOut.getVendorId());

        final int alreadySetManufacturer = 100;

        modelsMap.put(12L, modelsMap.get(12L).toBuilder()
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.MANUFACTURER)
                .setOptionId(alreadySetManufacturer)
                .build())
            .build());

        final long manufacturerId = 99L;

        doAnswer(i -> {
            ValueLink valueLink = new ValueLink();
            valueLink.setSourceParamId(VENDOR_PARAM_ID);
            valueLink.setSourceOptionId(modelWithManufacturerParamOut.getVendorId());
            valueLink.setTargetOptionId(manufacturerId);
            return Collections.singletonList(valueLink);
        }).when(valueLinkService).findValueLinks(eq(valueLinkSearchCriteria));

        List<ModelStorage.Model> result = (List<ModelStorage.Model>) queryService(CommonModel.Source.GURU, 12L);
        assertThat(result.size()).isEqualTo(1);
        assertTrue(result.get(0).getParameterValuesList().stream()
            .filter(e -> e.getXslName().equals(XslNames.MANUFACTURER))
            .anyMatch(e -> e.getOptionId() == alreadySetManufacturer));
    }

    @SuppressWarnings("checkstyle:methodlength")
    private List<CommonModel> createModels(CommonModelBuilder<CommonModel> modelBuilder) {
        CommonModelBuilder<CommonModel> builder =
            createModelTemplate(modelBuilder, CommonModel.Source.GURU, 1L, "model1", GLOBAL_VENDOR_1_ID)
                .param(3L).setNumeric(1);
        CommonModel parent1 = builder.endModel();
        parent1out = ModelProtoConverter.convert(builder.endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor1 model1"))
            .setTitleWithoutVendor(title("Model1"))
            .setPublishedOnMarket(false)
            .setPublishedOnBlueMarket(false)//internal group cat
            .setGroupSize(1)
            .build();

        builder = createModelTemplate(modelBuilder, CommonModel.Source.GURU, 2L, "model2", GLOBAL_VENDOR_2_ID)
            .param(3L).setNumeric(2)
            .startModelRelation()
            .categoryId(1L)
            .id(7L)
            .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation();
        CommonModel parent2 = builder.endModel();
        parent2out = ModelProtoConverter.convert(builder.endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor2 model2"))
            .setTitleWithoutVendor(title("Model2"))
            .setPublishedOnMarket(false)
            .setPublishedOnBlueMarket(false)//internal group cat
            .setGroupSize(1)
            .build();

        builder = createModelTemplate(modelBuilder, CommonModel.Source.GURU, 3L, "modif3", GLOBAL_VENDOR_1_ID)
            .param(4L).setOption(3L)
            .parentModelId(1L)
            .startModelRelation()
            .categoryId(1L)
            .id(5L)
            .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation();
        CommonModel modif3 = builder.endModel();
        modif3out = ModelProtoConverter.convert(builder
            .param(3L).setNumeric(1)
            .endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor1 modif3"))
            .setTitleWithoutVendor(title("Modif3"))
            .setPublishedOnMarket(true)
            .setPublishedOnBlueMarket(true)
            .build();

        builder = createModelTemplate(modelBuilder, CommonModel.Source.GURU, 4L, "modif4", GLOBAL_VENDOR_2_ID)
            .param(4L).setOption(3L)
            .parentModelId(2L)
            .startModelRelation()
            .categoryId(1L)
            .id(6L)
            .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .published(false);
        CommonModel modif4 = builder.endModel();
        modif4out = ModelProtoConverter.convert(builder
            .param(3L).setNumeric(2)
            .endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor2 modif4"))
            .setTitleWithoutVendor(title("Modif4"))
            .setPublishedOnMarket(false)
            .setPublishedOnBlueMarket(false)
            .build();


        builder = createModelTemplate(modelBuilder, CommonModel.Source.SKU, 5L, "sku5", GLOBAL_VENDOR_1_ID)
            .startModelRelation()
            .categoryId(1L)
            .id(3L)
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation();
        CommonModel sku5 = builder.endModel();
        sku5out = ModelProtoConverter.convert(builder
            .param(4L).setOption(3L)
            .param(3L).setNumeric(1)
            .endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor1 modif3"))
            .setPublishedOnMarket(true)
            .setPublishedOnBlueMarket(true)
            .build();

        builder = createModelTemplate(modelBuilder, CommonModel.Source.SKU, 6L, "sku6", GLOBAL_VENDOR_2_ID)
            .startModelRelation()
            .categoryId(1L)
            .id(4L)
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation();
        CommonModel sku6 = builder.endModel();
        sku6out = ModelProtoConverter.convert(builder
            .param(4L).setOption(3L)
            .param(3L).setNumeric(2)
            .endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor2 modif4"))
            .setPublishedOnMarket(false)
            .setPublishedOnBlueMarket(false)
            .build();

        builder = createModelTemplate(modelBuilder, CommonModel.Source.SKU, 7L, "sku7", GLOBAL_VENDOR_2_ID)
            .startModelRelation()
            .categoryId(1L)
            .id(2L)
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation();
        CommonModel sku7 = builder.endModel();
        sku7out = ModelProtoConverter.convert(builder
            .param(3L).setNumeric(2)
            .endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor2 model2"))
            .setPublishedOnMarket(false)
            .setPublishedOnBlueMarket(false)//internal group cat => parent is not visible outside
            .build();

        builder = createModelTemplate(modelBuilder, CommonModel.Source.GURU, 8L, "skuguru8", GLOBAL_VENDOR_1_ID)
            .param(5L).setBoolean(true).setOption(5L);
        CommonModel isSkuModel = builder.endModel();
        guru8out = ModelProtoConverter.convert(builder
            .startModelRelation()
            .categoryId(1L)
            .id(8L)
            .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor1 skuguru8"))
            .setTitleWithoutVendor(title("Skuguru8"))
            .setPublishedOnMarket(true)
            .setPublishedOnBlueMarket(true)
            .build();
        sku8out = ModelProtoConverter.convert(builder
            .currentType(CommonModel.Source.SKU)
            .clearRelations()
            .startModelRelation()
            .categoryId(1L)
            .id(8L)
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor1 skuguru8"))
            .setPublishedOnMarket(true)
            .setPublishedOnBlueMarket(true)
            .build();

        CommonModel noParentModif =
            createModelTemplate(modelBuilder, CommonModel.Source.GURU, 9L, "noparent", GLOBAL_VENDOR_1_ID)
                .parentModelId(20L)
                .endModel();

        CommonModel noParentModifSku =
            createModelTemplate(modelBuilder, CommonModel.Source.SKU, 10L, "noparent", GLOBAL_VENDOR_1_ID)
                .startModelRelation()
                .categoryId(1L)
                .id(9L)
                .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
                .endModel();

        CommonModel noParentSku =
            createModelTemplate(modelBuilder, CommonModel.Source.SKU, 11L, "noparent", GLOBAL_VENDOR_1_ID)
                .startModelRelation()
                .categoryId(1L)
                .id(20L)
                .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
                .endModel();

        builder = createModelTemplate(
            modelBuilder, CommonModel.Source.GURU, 12L, "modelWithManufacturerParam", GLOBAL_VENDOR_2_ID
        );
        CommonModel modelWithManufacturerParam9 = builder.endModel();
        modelWithManufacturerParamOut = ModelProtoConverter.convert(builder
            .endModel()).toBuilder()
            .clearTitles()
            .addTitles(title("Vendor2 modelWithManufacturerParam"))
            .setPublishedOnMarket(false)
            .setPublishedOnBlueMarket(false)
            .build();

        return Arrays.asList(parent1, parent2, modif3, modif4, sku5, sku6, sku7, isSkuModel,
            noParentModif, noParentModifSku, noParentSku, modelWithManufacturerParam9);
    }

    private CommonModelBuilder<CommonModel> createModelTemplate(
        CommonModelBuilder<CommonModel> modelBuilder, CommonModel.Source type,
        Long id, String name, Long vendorId) {
        return modelBuilder.startModel()
            .id(id)
            .category(1L)
            .source(type)
            .currentType(type)
            .published(true)
            .vendorId(vendorId)
            .param(2L).setString(name);
    }

    private ModelStorage.LocalizedString title(String title) {
        return ModelStorage.LocalizedString.newBuilder()
            .setIsoCode("ru")
            .setValue(title)
            .build();
    }

    private Collection<ModelStorage.Model> queryService(CommonModel.Source type, Long... modelIds) {
        MboExport.GetCategoryModelsRequest.Builder request = MboExport.GetCategoryModelsRequest.newBuilder()
            .setCategoryId(1L)
            .addAllModelId(Arrays.asList(modelIds));
        if (type != null) {
            request.setType(ModelStorage.ModelType.valueOf(type.name()));
        }

        return service.getModels(request.build()).getModelsList();
    }
}
