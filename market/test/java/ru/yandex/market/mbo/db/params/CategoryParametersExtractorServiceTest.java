package ru.yandex.market.mbo.db.params;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.catalogue.CategoryFavoriteVendorService;
import ru.yandex.market.mbo.catalogue.CategoryMatcherParamService;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.core.category.VendorGoodContentExclusionService;
import ru.yandex.market.mbo.core.guru.GuruCategoryService;
import ru.yandex.market.mbo.core.kdepot.saver.MatcherCategoryProperties;
import ru.yandex.market.mbo.core.matcher.export.CategoryFieldsProvider;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.db.MeasureService;
import ru.yandex.market.mbo.db.ReturnPoliciesService;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.db.VisualService;
import ru.yandex.market.mbo.db.category_wiki.CachedCategoryWikiService;
import ru.yandex.market.mbo.db.category_wiki.CategoryWikiProtoConverter;
import ru.yandex.market.mbo.db.forms.ModelFormService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.db.size.SizeChartStorageService;
import ru.yandex.market.mbo.db.size.SizeChartTypeService;
import ru.yandex.market.mbo.db.templates.OutputTemplateService;
import ru.yandex.market.mbo.db.tovartree.TovarCategoryDataFilter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.category.CategoryAvgTitleSizePgService;
import ru.yandex.market.mbo.gwt.models.forms.model.FormType;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Measure;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.ParamOptionsAccessType;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.Unit;
import ru.yandex.market.mbo.gwt.models.params.UnitAlias;
import ru.yandex.market.mbo.gwt.models.returnpolicy.CategoryGoodsReturnPolicy;
import ru.yandex.market.mbo.gwt.models.returnpolicy.ReturnPolicy;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.tovartree.FreezePartnerContent;
import ru.yandex.market.mbo.gwt.models.tovartree.IndexerModelEnrichType;
import ru.yandex.market.mbo.gwt.models.tovartree.MarketView;
import ru.yandex.market.mbo.gwt.models.tovartree.OutputType;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendorWithName;
import ru.yandex.market.mbo.gwt.models.videoreview.VideoReviewSettings;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWiki;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplates;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.Instructions;
import ru.yandex.market.mbo.utils.WordProtoUtils;
import ru.yandex.market.mbo.videoreview.VideoReviewService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.export.MboParameters.SKUParameterMode.SKU_NONE;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryParametersExtractorServiceTest {
    private static final Random RND = new Random(1);

    private CategoryParametersExtractorService categoryParametersExtractorService;

    @Mock
    private MeasureService measureService;

    @Mock
    IParameterLoaderService parameterLoaderService;

    @Mock
    NavigationTreeService navigationTreeService;

    @Mock
    TitlemakerTemplateDao titlemakerTemplateDao;

    @Mock
    ReturnPoliciesService returnPoliciesService;

    @Mock
    TovarCategory tovarCategory;

    @Mock
    ParameterLinkService parameterLinkService;

    @Mock
    OutputTemplateService outputTemplateService;

    @Mock
    GLRulesService glRulesService;

    @Mock
    CachedTreeService cachedTreeService;

    @Mock
    ValueLinkServiceInterface valueLinkService;

    @Mock
    VisualService visualService;

    @Mock
    CategoryMatcherParamService categoryMatcherParamService;

    @Mock
    ModelFormService modelFormService;

    @Mock
    CachedCategoryWikiService categoryWikiService;

    @Mock
    CategoryFieldsProvider categoryFieldsProvider;

    @Mock
    SizeMeasureService sizeMeasureService;

    @Mock
    VideoReviewService videoReviewService;

    @Mock
    CategoryParameterValuesService categoryParameterValuesService;

    @Mock
    CategoryFavoriteVendorService categoryFavoriteVendorService;

    @Mock
    GuruCategoryService guruCategoryService;

    @Mock
    ParameterMigrationLog parameterMigrationLog;

    @Mock
    VendorGoodContentExclusionService vendorGoodContentExclusionService;

    @Mock
    CategoryAvgTitleSizePgService categoryAvgTitleSizePgService;

    @Mock
    SizeChartStorageService sizeChartStorageService;

    @Mock
    SizeChartTypeService sizeChartTypeService;

    private static final long HID = 141L;
    private static final String GURU_TEMPLATE = String.format(
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v%d ),null,(true)],[(1 ),(t0 ),null,(true)]]}",
        KnownIds.VENDOR_PARAM_ID);
    private static final String GURU_TEMPLATE_WITHOUT_VENDOR =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(t0 ),null,(true)]]}";
    private static final double MIN_VIDEO_REVIEWS_RELEVANCE = 0.77d;

    @Before
    public void setUp() {
        categoryParametersExtractorService = new CategoryParametersExtractorService(
            categoryMatcherParamService, parameterLoaderService, cachedTreeService,
            titlemakerTemplateDao, parameterLinkService, null,
            categoryFieldsProvider, null, measureService,
            glRulesService, visualService, valueLinkService,
            outputTemplateService, null, returnPoliciesService,
            navigationTreeService, modelFormService, categoryWikiService,
            sizeMeasureService, videoReviewService, categoryParameterValuesService,
            categoryFavoriteVendorService, guruCategoryService, parameterMigrationLog,
            vendorGoodContentExclusionService, categoryAvgTitleSizePgService, sizeChartStorageService,
            false, sizeChartTypeService);

        when(parameterLoaderService.loadCategoryEntitiesByHid(HID)).thenReturn(new CategoryEntities());
        when(cachedTreeService.loadSchemeWholeTree()).thenReturn(new TovarTree(new TovarCategoryNode(tovarCategory)));
        when(cachedTreeService.loadCachedWholeTree()).thenReturn(new TovarTree(new TovarCategoryNode(tovarCategory)));
        when(outputTemplateService.getTemplates(HID)).thenReturn(new OutputTemplates());

        TMTemplate titleTemplates = mock(TMTemplate.class);
        when(titleTemplates.getEffectiveGuruTemplate()).thenReturn(GURU_TEMPLATE);
        when(titleTemplates.getEffectiveGuruTemplateWithoutVendor()).thenReturn(GURU_TEMPLATE_WITHOUT_VENDOR);
        when(titlemakerTemplateDao.loadTemplateByHid(HID)).thenReturn(titleTemplates);

        VideoReviewSettings vrs = new VideoReviewSettings();
        vrs.setType(VideoReviewSettings.Type.CATEGORY);
        vrs.setShowReviews(true);
        vrs.setMinRelevance(BigDecimal.valueOf(MIN_VIDEO_REVIEWS_RELEVANCE));
        when(videoReviewService.getWithoutManual(VideoReviewSettings.Type.CATEGORY, HID)).thenReturn(vrs);
    }

    @Test
    public void testCategoryFullNameExtraction() {
        TovarCategory tovarCategory1 = mockTovarCategory(
            WordUtil.defaultWord("имя1"),
            new Word(1, "name1"),
            new Word(2, "aaaa"));
        TovarCategory tovarCategory2 = mockTovarCategory(
            WordUtil.defaultWord("имя2"),
            new Word(1, "name2"));
        TovarCategory tovarCategory3 = mockTovarCategory(
            WordUtil.defaultWord("имя3"),
            new Word(1, "name3"),
            new Word(2, "bbbb"));

        TovarCategoryNode node1 = new TovarCategoryNode(tovarCategory1);
        TovarCategoryNode node2 = new TovarCategoryNode(tovarCategory2);
        TovarCategoryNode node3 = new TovarCategoryNode(tovarCategory3);
        node1.addChild(node2);
        node2.addChild(node3);
        TovarTree tovarTree = new TovarTree(node1);
        when(cachedTreeService.loadCachedWholeTree()).thenReturn(tovarTree);

        TovarCategoryDataFilter filter = new TovarCategoryDataFilter().setLoadFullName(true);
        MboParameters.Category category = categoryParametersExtractorService.extractCategory(tovarCategory3, filter);

        assertThat(category).isNotNull();
        assertThat(category.getFullNameList())
            .containsExactlyInAnyOrder(
                WordProtoUtils.defaultWord("имя1/имя2/имя3"),
                MboParameters.Word.newBuilder().setLangId(1).setName("name1/name2/name3").build()
            );
    }

    private TovarCategory mockTovarCategory(Word... names) {
        TovarCategory tc = mock(TovarCategory.class);
        when(tc.getOutputType()).thenReturn(OutputType.GURU);
        when(tc.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
        when(tc.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);
        when(tc.getMarketView()).thenReturn(MarketView.GRID);
        when(tc.getHid()).thenReturn(HID);
        when(tc.getGuruCategoryId()).thenReturn(HID + 1);
        when(tc.getCategoryNames()).thenReturn(Arrays.asList(names));
        return tc;
    }

    @Test
    public void testUnitsExtraction() {
        Unit defaultUnit1 = createUnit(1L, "unit1", 2L, "u1a1", "u1a2");
        Unit unit1 = createUnit(3L, "unit3", 2L, "u2a1", "u2a2");
        Measure measure1 = createMeasure(2L, "measure1", defaultUnit1);

        Unit defaultUnit2 = createUnit(11L, "unit11", 2L, "u1a11", "u1a21");
        Unit unit2 = createUnit(13L, "unit13", 12L, "u2a12", "u2a22");
        Measure measure2 = createMeasure(12L, "measure2", defaultUnit2);

        List<Unit> allUnits = Arrays.asList(defaultUnit1, unit1, defaultUnit2, unit2);
        when(measureService.getAllUnitsWithoutMeasures()).thenReturn(allUnits);

        List<Measure> allMeasures = Arrays.asList(measure1, measure2);
        when(measureService.getMeasures()).thenReturn(allMeasures);

        CategoryParam unitParam = new Parameter();
        unitParam.setUnit(unit1);
        CategoryParam noUnitParam = new Parameter();
        List<CategoryParam> parameters = Arrays.asList(unitParam, noUnitParam);

        List<MboParameters.Unit> result = categoryParametersExtractorService.extractCategoryUnits(parameters);

        MboParameters.Unit protoDefaultUnit = createProtoUnit(1L, "unit1", null, "u1a1", "u1a2", "unit1");
        MboParameters.Measure protoMeasure = createProtoMeasure(2L, "measure1", protoDefaultUnit);

        assertThat(result).containsExactlyInAnyOrder(
            createProtoUnit(1L, "unit1", protoMeasure, "u1a1", "u1a2", "unit1"),
            createProtoUnit(3L, "unit3", protoMeasure, "u2a1", "u2a2", "unit3")
        );
    }

    @Test
    public void testExtractCategoryReturnPolicies() {
        when(tovarCategory.getOutputType()).thenReturn(OutputType.VISUAL);
        when(tovarCategory.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
        when(tovarCategory.getMarketView()).thenReturn(MarketView.GRID);
        when(tovarCategory.getHid()).thenReturn(1L);
        when(tovarCategory.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);
        when(categoryWikiService.getCategoryWiki(any(Long.class))).thenReturn(new CategoryWiki());
        when(returnPoliciesService.getReturnPolicy(any(Integer.class)))
            .thenReturn(new ReturnPolicy(0, null, "Нет возврата"));
        when(tovarCategory.getReturnPolicies()).thenReturn(Collections.singletonList(
            new CategoryGoodsReturnPolicy(255, 0)
        ));

        when(navigationTreeService.getPrimaryNidByHid(any(Long.class), any(Long.class))).thenReturn(1L);
        when(navigationTreeService.getNids(any(Long.class))).thenReturn(Arrays.asList(2L, 1L, 3L));

        MboParameters.Category category = categoryParametersExtractorService
            .extractCategoryWithoutEntities(tovarCategory);

        Assertions.assertThat(category.getGoodsReturnPoliciesList()).hasSize(1);

        MboParameters.GoodsReturnPolicy policy = category.getGoodsReturnPoliciesList().get(0);
        Assertions.assertThat(policy.getRegionId()).isEqualTo(255);
        Assertions.assertThat(policy.hasPolicy()).isFalse();
    }

    @Test
    public void testExtractCategory() {
        when(tovarCategory.getOutputType()).thenReturn(OutputType.VISUAL);
        when(tovarCategory.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
        when(tovarCategory.getMarketView()).thenReturn(MarketView.GRID);
        when(tovarCategory.getHid()).thenReturn(1L);
        when(navigationTreeService.getPrimaryNidByHid(any(Long.class), any(Long.class))).thenReturn(1L);
        when(categoryWikiService.getCategoryWiki(any(Long.class))).thenReturn(new CategoryWiki());
        when(tovarCategory.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);
        when(vendorGoodContentExclusionService.getExcludedVendors(eq(1L))).thenReturn(
            Arrays.asList(
                new GlobalVendorWithName(1L, "test1"),
                new GlobalVendorWithName(2L, "test2")
            )
        );

        when(navigationTreeService.getNids(any(Long.class))).thenReturn(Arrays.asList(2L, 1L, 3L));
        MboParameters.Category category = categoryParametersExtractorService
            .extractCategoryWithoutEntities(tovarCategory);
        assertEquals(1L, (long) category.getNidList().get(0));

        when(navigationTreeService.getNids(any(Long.class))).thenReturn(Arrays.asList(4L, 2L, 3L, 1L));
        category = categoryParametersExtractorService
            .extractCategoryWithoutEntities(tovarCategory);
        assertEquals(1L, (long) category.getNidList().get(0));
        Assertions.assertThat(category.getVendorGoodContentExclusionList())
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void testInternalGrouped() {
        when(tovarCategory.getOutputType()).thenReturn(OutputType.GURU);
        when(tovarCategory.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
        when(tovarCategory.getMarketView()).thenReturn(MarketView.GRID);
        when(tovarCategory.getHid()).thenReturn(HID);
        when(tovarCategory.getGuruCategoryId()).thenReturn(HID + 1);
        when(tovarCategory.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);
        MboParameters.Category category = categoryParametersExtractorService
            .extractCategory(tovarCategory, new TovarCategoryDataFilter());
        assertEquals(false, category.getInternalGrouped());
        when(guruCategoryService.isGroupCategory(anyLong())).thenReturn(true);
        category = categoryParametersExtractorService
            .extractCategory(tovarCategory, new TovarCategoryDataFilter());
        assertEquals(true, category.getInternalGrouped());
    }

    @Test
    public void testExtractCategoryIncludeAllDataFilter() {
        when(tovarCategory.getOutputType()).thenReturn(OutputType.GURU);
        when(tovarCategory.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
        when(tovarCategory.getMarketView()).thenReturn(MarketView.GRID);
        when(tovarCategory.getHid()).thenReturn(HID);
        when(tovarCategory.getGuruCategoryId()).thenReturn(HID + 1);
        when(navigationTreeService.getPrimaryNidByHid(eq(HID), any(Long.class))).thenReturn(1L);
        when(categoryWikiService.getCategoryWiki(HID)).thenReturn(new CategoryWiki().setInCategory("in_category"));
        when(modelFormService.getPublishedModelFormJson(HID, FormType.CONTENT_LAB)).thenReturn("model_form");
        when(modelFormService.getPublishedModelFormJson(HID, FormType.MODEL_EDITOR)).thenReturn("model_editor_form");
        MatcherCategoryProperties matcherCategoryProperties = new MatcherCategoryProperties();
        matcherCategoryProperties.setMinPrice(1);
        matcherCategoryProperties.setMaxPrice(2);
        matcherCategoryProperties.setCutOffWord(Collections.emptyList());
        matcherCategoryProperties.setTovarCategoryId(Collections.emptyList());
        when(categoryMatcherParamService.getMatcherCategoryParameters(anyLong())).thenReturn(matcherCategoryProperties);
        when(categoryFieldsProvider.getCategoryFields(anyLong())).thenReturn(Collections.emptyList());
        when(tovarCategory.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);


        TovarCategoryDataFilter filter = new TovarCategoryDataFilter().includeAll();
        MboParameters.Category category = categoryParametersExtractorService.extractCategory(tovarCategory, filter);

        // test all additional data is in response
        assertThat(category.hasInCategory()).isTrue();
        assertThat(category.getInCategory()).isEqualTo("in_category");
        assertThat(category.getNidList()).containsExactlyInAnyOrder(1L);
        assertThat(category.hasContentLabForm()).isTrue();
        assertThat(category.getContentLabForm()).isEqualTo("model_form");
        assertThat(category.hasModelEditorForm()).isTrue();
        assertThat(category.getModelEditorForm()).isEqualTo("model_editor_form");
        assertThat(category.hasMatcherMinPrice()).isTrue();
        assertThat(category.getMatcherMinPrice()).isEqualTo(1);
        assertThat(category.hasMatcherMaxPrice()).isTrue();
        assertThat(category.getMatcherMaxPrice()).isEqualTo(2);
        assertThat(category.getGuruTitleTemplate()).isEqualTo(GURU_TEMPLATE);
        assertThat(category.getGuruTitleWithoutVendorTemplate()).isEqualTo(GURU_TEMPLATE_WITHOUT_VENDOR);
    }

    @Test
    public void testExtractCategoryEmptyDataFilter() {
        when(tovarCategory.getOutputType()).thenReturn(OutputType.GURU);
        when(tovarCategory.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
        when(tovarCategory.getMarketView()).thenReturn(MarketView.GRID);
        when(tovarCategory.getHid()).thenReturn(HID);
        when(tovarCategory.getGuruCategoryId()).thenReturn(HID + 1);
        when(tovarCategory.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);

        // empty filter: extract just category without loading any additional data
        TovarCategoryDataFilter filter = new TovarCategoryDataFilter();
        MboParameters.Category category = categoryParametersExtractorService.extractCategory(tovarCategory,
            filter);

        Assertions.assertThat(category.getHid()).isEqualTo(HID);
        Mockito.verifyZeroInteractions(categoryWikiService);
        Mockito.verifyZeroInteractions(navigationTreeService);
        Mockito.verifyZeroInteractions(visualService);
        Mockito.verifyZeroInteractions(parameterLoaderService);
        Mockito.verifyZeroInteractions(categoryMatcherParamService);
        Mockito.verifyZeroInteractions(modelFormService);
    }

    @Test
    public void testExtractModelHintsAndInstruction() {
        final String includedHint = "included-hint";
        final String excludedHint = "excluded-hint";
        final String modelNameComment = "model-name-comment";
        CategoryWiki categoryWiki = new CategoryWiki();
        categoryWiki.setIncludedHint(includedHint);
        categoryWiki.setExcludedHint(excludedHint);
        categoryWiki.setModelNameComment(modelNameComment);
        categoryWiki.setExportTitleToMbo(true);

        CategoryParam skuDefParam = createDefaultParam();
        skuDefParam.setNames(ImmutableList.of(WordUtil.defaultWord("sku def param")));
        skuDefParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.addParameter(skuDefParam);

        when(parameterLoaderService.loadCategoryEntitiesByHid(HID)).thenReturn(categoryEntities);
        when(tovarCategory.getOutputType()).thenReturn(OutputType.GURU);
        when(tovarCategory.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
        when(tovarCategory.getMarketView()).thenReturn(MarketView.GRID);
        when(tovarCategory.getHid()).thenReturn(HID);
        when(navigationTreeService.getPrimaryNidByHid(eq(HID), any(Long.class))).thenReturn(1L);
        when(categoryWikiService.getCategoryWiki(HID)).thenReturn(categoryWiki);
        when(tovarCategory.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);

        MboParameters.Category category = categoryParametersExtractorService.extractLeafCategory(tovarCategory);

        assertThat(category.hasInModel()).isTrue();
        assertThat(category.getInModel()).isEqualTo(includedHint);
        assertThat(category.hasOutOfModel()).isTrue();
        assertThat(category.getOutOfModel()).isEqualTo(excludedHint);
        assertThat(category.hasInstruction()).isTrue();
        Instructions.Instruction expectedInstruction = CategoryWikiProtoConverter.convert(categoryWiki,
            "sku def param");
        assertThat(category.getInstruction()).isEqualTo(expectedInstruction);
        assertThat(category.getGuruTitleTemplate()).isEqualTo(GURU_TEMPLATE);
        assertThat(category.getGuruTitleWithoutVendorTemplate()).isEqualTo(GURU_TEMPLATE_WITHOUT_VENDOR);
    }

    @Test
    public void testDefaultsExtraction() {
        CategoryParametersExtractorService.ParameterExtractionContext context = createDefaultParamContext();

        MboParameters.Parameter result = categoryParametersExtractorService.extractParameter(context);

        MboParameters.Parameter expected = createDefaultProtoParam()
            .build();

        assertEquals(expected, result);
    }

    @Test
    public void testHiddenExtraction() {
        CategoryParametersExtractorService.ParameterExtractionContext context = createDefaultParamContext();
        context.getOptionsContext()
            .getParam()
            .setHidden(true);

        MboParameters.Parameter result = categoryParametersExtractorService.extractParameter(context);

        MboParameters.Parameter expected = createDefaultProtoParam()
            .setHidden(true)
            .build();

        assertEquals(expected, result);
    }

    @Test
    public void testSimpleAccessExtraction() {
        CategoryParametersExtractorService.ParameterExtractionContext context = createDefaultParamContext();
        context.getOptionsContext()
            .getParam()
            .setAccess(ParamOptionsAccessType.SIMPLE);

        MboParameters.Parameter result = categoryParametersExtractorService.extractParameter(context);

        MboParameters.Parameter expected = createDefaultProtoParam()
            .setAccess(MboParameters.ParamOptionAccess.ACCESS_SIMPLE)
            .build();

        assertEquals(expected, result);
    }

    @Test
    public void testNotSpecifiedAccessExtraction() {
        CategoryParametersExtractorService.ParameterExtractionContext context = createDefaultParamContext();
        context.getOptionsContext()
            .getParam()
            .setAccess(ParamOptionsAccessType.NOT_SPECIFIED);

        MboParameters.Parameter result = categoryParametersExtractorService.extractParameter(context);

        MboParameters.Parameter expected = createDefaultProtoParam()
            .setAccess(MboParameters.ParamOptionAccess.ACCESS_SIMPLE)
            .build();

        assertEquals(expected, result);
    }

    @Test
    public void testOtherAllowedExtraction() {
        CategoryParametersExtractorService.ParameterExtractionContext context = createDefaultParamContext();
        context.getOptionsContext()
            .getParam()
            .setOtherAllowed(true);

        MboParameters.Parameter result = categoryParametersExtractorService.extractParameter(context);

        MboParameters.Parameter expected = createDefaultProtoParam()
            .setOtherAllowed(true)
            .build();

        assertEquals(expected, result);
    }

    @Test
    public void testCommentForPartner() {
        CategoryParametersExtractorService.ParameterExtractionContext paramContext = createDefaultParamContext();
        paramContext.getOptionsContext().getParam().setCommentForPartner("Yay!");

        MboParameters.Parameter parameter = categoryParametersExtractorService.extractParameter(paramContext);

        assertEquals("param", parameter.getXslName());
        assertEquals("Yay!", parameter.getCommentForPartner());
    }

    @Test
    public void testCommentForPartnerNoValue() {
        CategoryParametersExtractorService.ParameterExtractionContext paramContext = createDefaultParamContext();

        MboParameters.Parameter parameter = categoryParametersExtractorService.extractParameter(paramContext);

        assertEquals("param", parameter.getXslName());
        assertFalse(parameter.hasCommentForPartner());
    }

    @Test
    public void testValidatedFieldsConverted() {
        CategoryParam param = createDefaultParam();
        param.setAdvFilterIndex(9999);
        param.setCommonFilterIndex(9998);
        param.setModelFilterIndex(9997);
        param.setPrecision(RND.nextInt(99));

        MboParameters.Parameter.Builder protoParam = createDefaultProtoParam();
        protoParam.setAdvFilterIndex(param.getAdvFilterIndex());
        protoParam.setCommonFilterIndex(param.getCommonFilterIndex());
        protoParam.setModelFilterIndex(param.getModelFilterIndex());
        protoParam.setPrecision(param.getPrecision());

        CategoryParametersExtractorService.ParameterExtractionContext context =
            new CategoryParametersExtractorService.ParameterExtractionContext();
        CategoryParametersExtractorService.OptionsExtractionContext optionContext =
            new CategoryParametersExtractorService.OptionsExtractionContext();
        context.setOptionsContext(optionContext);
        optionContext.setParam(param);
        context.setFavoriteVendors(Collections.emptyList());

        MboParameters.Parameter parameter = categoryParametersExtractorService.extractParameter(context);
        assertEquals(protoParam.build(), parameter);
    }

    @Test(expected = OperationException.class)
    public void testFieldsValidationException() {
        CategoryParam param = createDefaultParam();
        param.setAdvFilterIndex(-2);

        CategoryParametersExtractorService.ParameterExtractionContext context =
            new CategoryParametersExtractorService.ParameterExtractionContext();
        CategoryParametersExtractorService.OptionsExtractionContext optionContext =
            new CategoryParametersExtractorService.OptionsExtractionContext();
        context.setOptionsContext(optionContext);
        optionContext.setParam(param);

        MboParameters.Parameter parameter = categoryParametersExtractorService.extractParameter(context);
    }

    @Test
    public void testExtractMarketView() {
        for (MarketView marketView : MarketView.values()) {
            when(tovarCategory.getMarketView()).thenReturn(marketView);
            when(tovarCategory.getOutputType()).thenReturn(OutputType.GURU);
            when(tovarCategory.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
            when(tovarCategory.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);

            TovarCategoryDataFilter filter = new TovarCategoryDataFilter();
            MboParameters.Category category = categoryParametersExtractorService.extractCategory(tovarCategory,
                filter);

            Assertions.assertThat(category.hasMarketView()).isTrue();
            Assertions.assertThat(category.getMarketView()).isNotNull();
        }
    }

    @Test
    public void testExtractDefaultTitleTemplates() {
        String expectedGuruTemplate = "{\"delimiter\":\" \",\"values\":[[(v14711863 ),(v14711863 )]]}";
        String expectedSkuTemplate = expectedGuruTemplate +
            ",{\"delimiter\":\" \",\"before\":\" \",\"values\":[[(v1 ),(v1 )]]}";
        String expectedBGTemplate = expectedGuruTemplate +
            ",{\"delimiter\":\" \",\"before\":\" \",\"values\":[[(v2 ),(v2 )]]}";
        TMTemplate tmTemplate = new TMTemplate();
        tmTemplate.setValue(expectedGuruTemplate);

        CategoryParam skuDefParam = createDefaultParam();
        skuDefParam.setXslName("skuDefParam");
        skuDefParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        CategoryParam blueGroupingParam = createDefaultParam();
        blueGroupingParam.setId(2L);
        blueGroupingParam.setXslName("bgParam");
        blueGroupingParam.setBlueGrouping(true);
        List<CategoryParam> categoryParams = Arrays.asList(skuDefParam, blueGroupingParam);

        MboParameters.Category.Builder categoryBuilder = MboParameters.Category.newBuilder();

        categoryParametersExtractorService.setTitleTemplates(tmTemplate, categoryParams, categoryBuilder);

        assertEquals(expectedGuruTemplate, categoryBuilder.getGuruTitleTemplate());
        assertEquals(expectedSkuTemplate, categoryBuilder.getSkuTitleTemplate());
        assertEquals(expectedBGTemplate, categoryBuilder.getBlueGroupingTitleTemplate());
    }

    @Test
    public void testExtractMinVideoRelevance() {
        when(tovarCategory.getOutputType()).thenReturn(OutputType.GURU);
        when(tovarCategory.getModelsEnrichType()).thenReturn(IndexerModelEnrichType.NONE);
        when(tovarCategory.getMarketView()).thenReturn(MarketView.GRID);
        when(tovarCategory.getHid()).thenReturn(HID);
        when(tovarCategory.getGuruCategoryId()).thenReturn(HID + 1);
        when(tovarCategory.getFreezePartnerContent()).thenReturn(FreezePartnerContent.NO_FREEZE);

        MboParameters.Category category = categoryParametersExtractorService
            .extractCategory(tovarCategory, new TovarCategoryDataFilter());
        assertTrue(category.getShowVideoReviews());
        assertEquals(MIN_VIDEO_REVIEWS_RELEVANCE, category.getMinVideoReviewRelevance(), 0d);
    }

    private Unit createUnit(long id, String name, long measureId, String... aliases) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setName(name);
        unit.setReportName(name);
        unit.setMeasureId(measureId);
        AtomicBoolean mainAlias = new AtomicBoolean(true);
        unit.setAliases(Stream.of(aliases)
            .map(a -> new UnitAlias(0, a, Word.DEFAULT_LANG_ID, mainAlias.getAndSet(false)))
            .collect(Collectors.toList()));
        return unit;
    }

    private Measure createMeasure(long id, String name, Unit defaultUnit) {
        Measure measure = new Measure();
        measure.setId(id);
        measure.addName(Word.DEFAULT_LANG_ID, name);
        measure.setDefaultUnit(defaultUnit);
        measure.setDefaultUnitId(defaultUnit.getId());
        defaultUnit.setMeasure(measure);
        defaultUnit.setMeasureId(measure.getId());
        return measure;
    }

    private MboParameters.Unit createProtoUnit(long id, String name, MboParameters.Measure measure, String... aliases) {
        MboParameters.Unit.Builder builder = MboParameters.Unit.newBuilder();
        builder
            .setId(id)
            .addName(WordProtoUtils.defaultWord(name))
            .addAllAlias(Stream.of(aliases).map(WordProtoUtils::defaultWord).collect(Collectors.toList()));
        if (measure != null) {
            builder.setMeasure(measure);
        }
        return builder.build();
    }

    private MboParameters.Measure createProtoMeasure(long id, String name, MboParameters.Unit defaultUnit) {
        return MboParameters.Measure.newBuilder()
            .setId(id)
            .addName(WordProtoUtils.defaultWord(name).toBuilder().setMorphologicalProcessing(true).build())
            .setDefaultUnit(defaultUnit)
            .build();
    }

    private CategoryParametersExtractorService.ParameterExtractionContext createDefaultParamContext() {
        CategoryParametersExtractorService.ParameterExtractionContext context =
            new CategoryParametersExtractorService.ParameterExtractionContext();
        CategoryParametersExtractorService.OptionsExtractionContext optionContext =
            new CategoryParametersExtractorService.OptionsExtractionContext();
        context.setOptionsContext(optionContext);
        optionContext.setParam(createDefaultParam());
        context.setFavoriteVendors(Collections.emptyList());
        return context;
    }

    private CategoryParam createDefaultParam() {
        CategoryParam param = new Parameter();
        param.setId(1L);
        param.setXslName("param");
        param.setLevel(CategoryParam.Level.MODEL);
        param.setType(Param.Type.NUMERIC);
        param.addName(WordUtil.defaultWord("ParamName"));
        return param;
    }

    private MboParameters.Parameter.Builder createDefaultProtoParam() {
        return MboParameters.Parameter.newBuilder()
            .setId(1L)
            .setXslName("param")
            .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .addName(WordProtoUtils.defaultWord("ParamName", true))
            .addAlias(WordProtoUtils.defaultWord("ParamName", true))
            // Default values start here
            .setCommonFilterIndex(-1)
            .setAdvFilterIndex(-1)
            .setModelFilterIndex(-1)
            .setSuperFilterIndex(-1)
            .setThrough(false)
            .setNoAdvFilters(false)
            .setTypeParameter(false)
            .setHasBoolNo(false)
            .setClusterBreaker(false)
            .setRequiredForIndex(false)
            .setHighlightOriginalValue(false)
            .setUseForGurulight(false)
            .setPublished(false)
            .setMandatoryForSignature(false)
            .setFillValueFromOffers(false)
            .setMultivalue(false)
            .setUseForImages(false)
            .setImportant(false)
            .setNotifyStores(false)
            .setQuotedInTitle(false)
            .setUseInSku(false)
            .setMandatory(false)
            .setExtractInSkubd(false)
            .setSkuMode(SKU_NONE)
            .setSkutchingType(MboParameters.SkutchingType.SKU_MANDATORY)
            .setCleanIfSkutchingFailed(true)
            .setClearIfAbsentInSku(false)
            .setCopyFirstSkuPictureToPicker(false)
            .setShowOnSkuTab(false)
            .setRequiredForModelCreation(false)
            .setDoNotFormalizePatterns(false)
            .setHidden(false)
            .setService(false)
            .setIsUseForGuru(false)
            .setMdmParameter(false)
            .setModificationState(MboParameters.ModificationState.NOT_SPECIFIED)
            .setGuruType(MboParameters.GuruType.GURU_TYPE_TEXT)
            .setClusterFilter(false)
            .setPrecision(2)
            .setSubType(MboParameters.SubType.NOT_DEFINED)
            .setAccess(MboParameters.ParamOptionAccess.ACCESS_SUPER)
            .setOtherAllowed(false)
            .setUseFormalization(false)
            .setReadOnly(false)
            .setMandatoryForPartner(false)
            .setUseDefaultValue1Pkg(false)
            .setValidateSkutchingByValue(false)
            .setConfidentFormalization(MboParameters.ConfidentFormalization.NOT_SELECTED);
    }
}
