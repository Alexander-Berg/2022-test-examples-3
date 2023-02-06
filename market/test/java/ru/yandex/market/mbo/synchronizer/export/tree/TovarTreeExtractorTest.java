package ru.yandex.market.mbo.synchronizer.export.tree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.Magics;
import ru.yandex.market.mbo.catalogue.CategoryFavoriteVendorService;
import ru.yandex.market.mbo.catalogue.CategoryMatcherParamService;
import ru.yandex.market.mbo.core.category.VendorGoodContentExclusionService;
import ru.yandex.market.mbo.core.guru.GuruCategoryService;
import ru.yandex.market.mbo.core.kdepot.saver.MatcherCategoryProperties;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.db.ModelStopWordsDao;
import ru.yandex.market.mbo.db.ReturnPoliciesService;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.VisualService;
import ru.yandex.market.mbo.db.category_wiki.CachedCategoryWikiService;
import ru.yandex.market.mbo.db.forms.ModelFormService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkService;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.db.params.CategoryParameterValuesService;
import ru.yandex.market.mbo.db.params.CategoryParametersExtractorService;
import ru.yandex.market.mbo.db.params.ParameterMigrationLog;
import ru.yandex.market.mbo.db.size.SizeChartTypeService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.category.CategoryAvgTitleSizePgService;
import ru.yandex.market.mbo.gwt.models.forms.model.FormType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueTestHelper;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.returnpolicy.CategoryGoodsReturnPolicy;
import ru.yandex.market.mbo.gwt.models.returnpolicy.ReturnPolicy;
import ru.yandex.market.mbo.gwt.models.tovartree.MarketView;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWiki;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;
import ru.yandex.market.mbo.synchronizer.export.ExporterUtils;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportMapReduceService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportModelsTableService;
import ru.yandex.market.mbo.tree.ExportTovarTree;
import ru.yandex.market.mbo.videoreview.VideoReviewService;
import ru.yandex.market.mbo.yt.TestYtWrapper;
import ru.yandex.market.protobuf.tools.MagicChecker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.params.ParameterProtoConverter.convertWordsProto;
import static ru.yandex.market.mbo.synchronizer.export.tree.IsWordMatcher.likeWord;

/**
 * Tests of {@link TovarTreeExtractor}.
 *
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 25.01.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class TovarTreeExtractorTest {
    private static final long DEFAULT_YT_TIMEOUT = 60L;

    private static final byte[] MAGIC_BYTES = MagicChecker.magicToBytes(Magics.MagicConstants.MBOC.name());

    private static final AtomicInteger ID_SOURCE = new AtomicInteger(29319);

    private static final long CATEGORY_HID_1 = uniqId();
    private static final long CATEGORY_HID_2 = uniqId();
    private static final long CATEGORY_HID_3 = uniqId();
    private static final long CATEGORY_HID_4 = uniqId();
    private static final long GURU_CATEGORY_ID_1 = uniqId();
    private static final int TOVAR_CATEGORY_ID_1 = uniqId();
    private static final int TOVAR_CATEGORY_ID_2 = uniqId();
    private static final int TOVAR_CATEGORY_ID_3 = uniqId();
    private static final int TOVAR_CATEGORY_ID_4 = uniqId();
    private static final long NID_1 = uniqId();
    private static final long NID_2 = uniqId();
    private static final Word WORD_RU_1 = new Word(uniqId(), Word.DEFAULT_LANG_ID, "NAME_1");
    private static final Word WORD_RU_2 = new Word(uniqId(), Word.DEFAULT_LANG_ID, "NAME_2");
    private static final Word WORD_RU_3 = new Word(uniqId(), Word.DEFAULT_LANG_ID, "NAME_3");
    private static final Word WORD_RU_4 = new Word(uniqId(), Word.DEFAULT_LANG_ID, "NAME_4");
    private static final Word WORD_5 = new Word(uniqId(), Word.DEFAULT_LANG_ID + 1, "NAME_5");
    private static final Word WORD_6 = new Word(uniqId(), Word.DEFAULT_LANG_ID + 1, "NAME_6");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CategoryWiki categoryWiki;

    private TovarTreeExtractor extractor;
    private TestYtWrapper ytWrapper;
    private ExportRegistry registry;
    private TovarTreeServiceMock tovarTreeService;

    @Mock
    private ReturnPoliciesService returnPoliciesService;
    @Mock
    private CategoryMatcherParamService categoryMatcherParamService;
    @Mock
    private VisualService visualService;
    @Mock
    private NavigationTreeService navigationTreeService;
    @Mock
    private CachedTreeService treeService;
    @Mock
    private ModelFormService modelFormService;
    @Mock
    private CachedCategoryWikiService categoryWikiService;
    @Mock
    private VideoReviewService videoReviewService;
    @Mock
    private CategoryParameterValuesService categoryParameterValuesService;
    @Mock
    private CategoryFavoriteVendorService categoryFavoriteVendorService;
    @Mock
    private GuruCategoryService guruCategoryService;
    @Mock
    private ParameterMigrationLog parameterMigrationLog;
    @Mock
    private ValueLinkService valueLinkService;
    @Mock
    private ModelStopWordsDao modelStopWordsDao;
    @Mock
    private VendorGoodContentExclusionService vendorGoodContentExclusionService;
    @Mock
    private CategoryAvgTitleSizePgService categoryAvgTitleSizePgService;
    @Mock
    private SizeChartTypeService sizeChartTypeService;

    private static final int ENRICHMENT_MAX_DATA_SIZE = 64;
    private static final long ENRICHMENT_JOB_COUNT = 10_000L;
    private static final long REDUCE_JOB_COUNT = 10_000L;

    @Before
    public void before() throws Exception {
        CategoryParametersExtractorService metaExtractorService = new CategoryParametersExtractorService(
            categoryMatcherParamService, null,
            treeService, null,
            null, null,
            null, null,
            null, null,
            visualService, null,
            null, null,
            returnPoliciesService, navigationTreeService,
            modelFormService, categoryWikiService,
            null, videoReviewService, categoryParameterValuesService,
            categoryFavoriteVendorService, guruCategoryService, parameterMigrationLog,
            vendorGoodContentExclusionService, categoryAvgTitleSizePgService, null, false,
            sizeChartTypeService);

        this.categoryWiki = new CategoryWiki();
        when(categoryWikiService.getCategoryWiki(any(Long.class))).thenReturn(categoryWiki);

        YPath tmpPath = YPath.simple("//tmp");

        this.ytWrapper = new TestYtWrapper();
        this.ytWrapper.cypress().create(tmpPath, CypressNodeType.MAP);

        YtExportModelsTableService ytExportModelsTableService = new YtExportModelsTableService(
            ytWrapper, null, tmpPath, null, GUID.create());
        YtExportMapReduceService ytExportMapReduceService = new YtExportMapReduceService(ytWrapper,
            ytExportModelsTableService, null, null,
            ytWrapper.pool(), ENRICHMENT_MAX_DATA_SIZE, ENRICHMENT_JOB_COUNT, REDUCE_JOB_COUNT, ytWrapper.ytAccount(),
            null, null, null, DEFAULT_YT_TIMEOUT, DEFAULT_YT_TIMEOUT,
            valueLinkService, modelStopWordsDao, true, false, false);

        this.tovarTreeService = new TovarTreeServiceMock();
        when(treeService.loadCachedWholeTree())
            .thenAnswer(__ -> tovarTreeService.loadTovarTree());

        this.extractor = new TovarTreeExtractor(tovarTreeService, metaExtractorService, ytExportMapReduceService);
        this.extractor.setYtExtractorPath("tovar-tree");
        this.extractor.setOutputFileName("tovar-tree.pb");

        this.registry = new ExportRegistry();
        this.registry.setRootPath(folder.newFolder().getAbsolutePath());
        this.registry.setYtExportPath("//home");
        this.registry.setMuteMode(true);
        this.registry.afterPropertiesSet();
        this.registry.processStart();
        this.extractor.setRegistry(registry);
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        extractorWriterService.setYt(ytWrapper);
        this.extractor.setExtractorWriterService(extractorWriterService);
        this.extractor.afterPropertiesSet();
    }

    /**
     * Проверяет, что категории в файлы выгружаются от корня к листовым категориям.
     * Проверяет, что в yt данные выгружаются в отсортированом по hid виду.
     */
    @Test
    public void testExtractedToRightOrder() throws Exception {
        TovarCategory root = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);
        TovarCategory category2 = newCategory(TOVAR_CATEGORY_ID_2, CATEGORY_HID_2);
        TovarCategory category3 = newCategory(TOVAR_CATEGORY_ID_3, CATEGORY_HID_3);
        TovarCategory category4 = newCategory(TOVAR_CATEGORY_ID_4, CATEGORY_HID_4);

        category2.setParentHid(root.getHid());
        category4.setParentHid(root.getHid());
        category3.setParentHid(category4.getHid());

        tovarTreeService.addCategories(root, category2, category4, category3);

        extractor.perform("");

        // assert correct order in file
        List<ExportTovarTree.TovarCategory> fileCategories = read();
        Assertions.assertThat(fileCategories)
            .extracting(ExportTovarTree.TovarCategory::getHid)
            .containsExactly(CATEGORY_HID_1, CATEGORY_HID_2, CATEGORY_HID_4, CATEGORY_HID_3);

        // assert correct order in yt
        YPath outputDir = YPath.simple("//home/" + registry.getFolderName() + "/tovar-tree");
        List<YTreeMapNode> values = Lists.newArrayList(ytWrapper.tables().read(outputDir, YTableEntryTypes.YSON));
        Assertions.assertThat(values)
            .extracting(s -> s.getLong(TovarTreeYtExportService.HID))
            .containsExactly(CATEGORY_HID_1, CATEGORY_HID_2, CATEGORY_HID_3, CATEGORY_HID_4);
    }

    @Test
    public void commonCategoryFieldsAreExtracted() throws Exception {
        TovarCategory category = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);
        Date modificationDate = new Date();
        category.setShowModelTypes(Collections.singletonList(CommonModel.Source.GURU));
        category.setPublished(true);
        category.setParentHid(CATEGORY_HID_2);
        category.setCategoryNames(Arrays.asList(WORD_RU_1, WORD_5));
        category.setAliases(Arrays.asList(WORD_RU_2, WORD_RU_4));
        category.setUniqueNames(Arrays.asList(WORD_RU_3, WORD_6));
        category.setModificationDate(modificationDate);
        category.setClusterize(false);
        category.setShowOffers(true);
        category.setSkuEnabled(true);
        category.setSkuExported(false);
        category.setAcceptPartnerModels(true);
        category.setAcceptPartnerSkus(true);
        category.setAcceptGoodContent(true);
        category.setAllowNonWhiteFirstPictureBackground(true);
        category.setShowModelTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.CLUSTER));
        categoryWiki.setInCategory("in-category-text");
        categoryWiki.setOutOfCategory("out-category-text");
        tovarTreeService.addCategory(category);

        extractor.perform("");

        List<ExportTovarTree.TovarCategory> categories = read();
        assertThat(categories, hasSize(1));
        ExportTovarTree.TovarCategory extracted = categories.get(0);

        assertTrue(extracted.hasHid());
        assertEquals(extracted.getHid(), CATEGORY_HID_1);

        assertTrue(extracted.hasTovarId());
        assertEquals(extracted.getTovarId(), TOVAR_CATEGORY_ID_1);

        assertTrue(extracted.hasPublished());
        assertThat(extracted.getPublished(), is(true));

        assertTrue(extracted.hasParentHid());
        assertEquals(extracted.getParentHid(), CATEGORY_HID_2);

        assertThat("contains names and russian is on top", convertWordsProto(extracted.getNameList()),
            contains(likeWord(WORD_RU_1), likeWord(WORD_5)));
        assertThat(convertWordsProto(extracted.getAliasList()),
            containsInAnyOrder(likeWord(WORD_RU_2), likeWord(WORD_RU_4)));
        assertThat(convertWordsProto(extracted.getUniqueNameList()),
            containsInAnyOrder(likeWord(WORD_RU_3), likeWord(WORD_6)));

        assertTrue(extracted.hasModificationDate());
        assertThat(extracted.getModificationDate(), is(modificationDate.getTime()));

        assertTrue(extracted.hasClusterize());
        assertThat(extracted.getClusterize(), is(false));

        assertTrue(extracted.hasOutOfCategory());
        assertThat(extracted.getOutOfCategory(), is("out-category-text"));

        assertTrue(extracted.hasInCategory());
        assertThat(extracted.getInCategory(), is("in-category-text"));

        assertThat(extracted.getShowModelTypeList(), containsInAnyOrder("GURU", "CLUSTER"));

        assertTrue(extracted.hasOutputType());
        assertThat(extracted.getOutputType(), is(MboParameters.OutputType.SIMPLE));

        assertTrue(extracted.hasShowOffers());
        assertThat(extracted.getShowOffers(), is(true));

        assertTrue(extracted.getAcceptPartnerModels());
        assertTrue(extracted.getAcceptPartnerSkus());

        assertTrue(extracted.getAcceptGoodContent());
        assertTrue(extracted.getAllowNonWhiteFirstPictureBackground());
    }

    @Test
    public void nidsAreExtracted() throws Exception {
        TovarCategory category = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);
        tovarTreeService.addCategory(category);

        when(navigationTreeService.getNids(CATEGORY_HID_1)).thenReturn(Arrays.asList(NID_1, NID_2));
        when(navigationTreeService.getPrimaryNidByHid(any(Long.class), any(Long.class))).thenReturn(NID_1);

        extractor.perform("");

        List<ExportTovarTree.TovarCategory> categories = read();
        assertThat(categories.get(0).getNidList(), containsInAnyOrder(NID_1, NID_2));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void parameterValuesAreExtracted() throws Exception {
        TovarCategory category = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);

        ParameterValue pv1 = ParameterValueTestHelper.string(1L, XslNames.COMMENT, WordUtil.defaultWords("value"));
        ParameterValue pv2 = ParameterValueTestHelper.bool(2L, "is_something", true, 64);
        ParameterValue pv3 = ParameterValueTestHelper.numeric(3L, "numeric_something", 542);

        when(categoryParameterValuesService.loadCategoryParameterValues(anyLong()))
            .thenReturn(ImmutableList.of(ParameterValues.of(pv1), ParameterValues.of(pv2), ParameterValues.of(pv3)));

        tovarTreeService.addCategory(category);

        extractor.perform("");

        List<ExportTovarTree.TovarCategory> categories = read();
        ExportTovarTree.TovarCategory extracted = categories.get(0);

        assertEquals(3, extracted.getParameterValueCount());
        MboParameters.ParameterValue extractedParameterValue1 = extracted.getParameterValue(0);
        MboParameters.ParameterValue extractedParameterValue2 = extracted.getParameterValue(1);
        MboParameters.ParameterValue extractedParameterValue3 = extracted.getParameterValue(2);

        assertEquals(pv1.getXslName(), extractedParameterValue1.getXslName());
        assertEquals(pv1.getParamId(), extractedParameterValue1.getParamId());
        assertEquals(pv1.getStringValue().get(0).getWord(), extractedParameterValue1.getStrValue(0).getValue());

        assertEquals(pv2.getXslName(), extractedParameterValue2.getXslName());
        assertEquals(pv2.getParamId(), extractedParameterValue2.getParamId());
        assertEquals(pv2.getBooleanValue(), extractedParameterValue2.getBoolValue());

        assertEquals(pv3.getXslName(), extractedParameterValue3.getXslName());
        assertEquals(pv3.getParamId(), extractedParameterValue3.getParamId());
        assertEquals(pv3.getNumericValue(), new BigDecimal(extractedParameterValue3.getNumericValue()));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void returnPolicyIsExtracted() throws Exception {
        TovarCategory category = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);
        List<CategoryGoodsReturnPolicy> policies = Lists.newArrayList(
            new CategoryGoodsReturnPolicy(225, 2), new CategoryGoodsReturnPolicy(149, 3));
        category.setReturnPolicies(policies);
        tovarTreeService.addCategory(category);
        when(returnPoliciesService.getReturnPolicy(2))
            .thenReturn(new ReturnPolicy(2, "7d-report-text", "Возврат в течении 7 дней"));
        when(returnPoliciesService.getReturnPolicy(3))
            .thenReturn(new ReturnPolicy(3, "14d-report-text", "Возврат в течении 14 дней"));

        extractor.perform("");


        List<ExportTovarTree.TovarCategory> categories = read();
        ExportTovarTree.TovarCategory extracted = categories.get(0);
        assertTrue(extracted.hasGoodsReturnPolicy());
        assertThat(extracted.getGoodsReturnPolicy(), is("7d-report-text"));

        assertEquals(2, extracted.getGoodsReturnPoliciesCount());
        List<MboParameters.GoodsReturnPolicy> extractedPolicies = extracted.getGoodsReturnPoliciesList();
        assertThat(extractedPolicies, contains(MboParameters.GoodsReturnPolicy.newBuilder()
            .setPolicy("7d-report-text")
            .setRegionId(225)
            .build(), MboParameters.GoodsReturnPolicy.newBuilder()
            .setPolicy("14d-report-text")
            .setRegionId(149)
            .build()));
    }

    @Test
    public void notPublishedAreExtracted() throws Exception {
        TovarCategory root = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);
        TovarCategory child = newCategory(TOVAR_CATEGORY_ID_1 + 1, CATEGORY_HID_2);
        TovarCategory grandchild = newCategory(TOVAR_CATEGORY_ID_1 + 2, CATEGORY_HID_3);
        child.setParentHid(CATEGORY_HID_1);
        grandchild.setParentHid(CATEGORY_HID_2);

        root.setPublished(false);
        child.setPublished(false);
        grandchild.setPublished(true);

        tovarTreeService.addCategories(root, child, grandchild);

        extractor.perform("");

        List<ExportTovarTree.TovarCategory> categories = read();

        Assertions.assertThat(categories)
            .extracting(ExportTovarTree.TovarCategory::getHid)
            .containsExactlyInAnyOrder(CATEGORY_HID_1, CATEGORY_HID_2, CATEGORY_HID_3);
    }

    @Test
    public void testPessimizeOffers() throws Exception {
        TovarCategory mixed = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);
        mixed.setShowModelTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.CLUSTER));
        mixed.setShowOffers(false);

        TovarCategory guruDefault = newCategory(TOVAR_CATEGORY_ID_2, CATEGORY_HID_2);
        guruDefault.setShowModelTypes(Arrays.asList(CommonModel.Source.GURU));
        guruDefault.setShowOffers(false);

        TovarCategory guruOverriden = newCategory(TOVAR_CATEGORY_ID_3, CATEGORY_HID_3);
        guruOverriden.setShowModelTypes(Arrays.asList(CommonModel.Source.GURU));
        guruOverriden.setShowOffers(true);

        TovarCategory gurulight = newCategory(TOVAR_CATEGORY_ID_4, CATEGORY_HID_4);
        gurulight.setShowModelTypes(Collections.emptyList());
        gurulight.setShowOffers(true);

        guruDefault.setParentHid(mixed.getHid());
        guruOverriden.setParentHid(mixed.getHid());
        gurulight.setParentHid(mixed.getHid());

        tovarTreeService.addCategories(mixed, guruDefault, gurulight, guruOverriden);

        extractor.perform("");

        Map<Long, Boolean> results = new HashMap<>();
        read().forEach(c -> {
            if (c.hasPessimizeOffers()) {
                results.put(c.getHid(), c.getPessimizeOffers());
            }
        });
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Boolean.FALSE, results.get(guruOverriden.getHid()));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void matcherParametersExtracted() throws Exception {
        TovarCategory category = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);
        category.setShowModelTypes(Collections.singletonList(CommonModel.Source.GURU));
        category.setGuruCategoryId(GURU_CATEGORY_ID_1);

        MatcherCategoryProperties properties = new MatcherCategoryProperties();
        properties.setMinPrice(444L);
        properties.setMaxPrice(999L);
        properties.setParamMatcherUsed(true);
        properties.setMatcherUsesDescription(false);
        properties.setMatcherUsesParams(true);
        properties.setMatcherUsesBarcode(false);
        properties.setMatcherUsesVendorCode(true);
        properties.setCutOffWord(Arrays.asList("cut-off-1", "cut-off-2", "cut-off-last"));
        properties.setTovarCategoryId(Arrays.asList(1414L, 1515L));

        tovarTreeService.addCategory(category);
        when(categoryMatcherParamService.getMatcherCategoryParameters(GURU_CATEGORY_ID_1)).thenReturn(properties);

        extractor.perform("");

        List<ExportTovarTree.TovarCategory> categories = read();
        ExportTovarTree.TovarCategory extracted = categories.get(0);
        assertTrue(extracted.hasMatcherMinPrice());
        assertThat(extracted.getMatcherMinPrice(), is(444L));
        assertTrue(extracted.hasMatcherMaxPrice());
        assertThat(extracted.getMatcherMaxPrice(), is(999L));
        assertTrue(extracted.hasParamMatcherUsed());
        assertThat(extracted.getParamMatcherUsed(), is(true));
        assertTrue(extracted.hasMatcherUsesDescription());
        assertThat(extracted.getMatcherUsesDescription(), is(false));
        assertTrue(extracted.hasMatcherUsesParams());
        assertThat(extracted.getMatcherUsesParams(), is(true));
        assertTrue(extracted.hasMatcherUsesBarcode());
        assertThat(extracted.getMatcherUsesBarcode(), is(false));
        assertTrue(extracted.hasMatcherUsesVendorCode());
        assertThat(extracted.getMatcherUsesVendorCode(), is(true));

        assertThat(extracted.getCutOffWordList(), containsInAnyOrder("cut-off-1", "cut-off-2", "cut-off-last"));
        assertThat(extracted.getLinkedTovarCategoryIdList(), containsInAnyOrder(1414L, 1515L));
    }

    @Test
    public void contentLabFormExtracted() throws Exception {
        TovarCategory category = newCategory(TOVAR_CATEGORY_ID_1, CATEGORY_HID_1);
        tovarTreeService.addCategory(category);

        when(modelFormService.getPublishedModelFormJson(CATEGORY_HID_1, FormType.CONTENT_LAB))
            .thenReturn("MODEL_FORM_CONTENT");

        extractor.perform("");

        List<ExportTovarTree.TovarCategory> categories = read();
        ExportTovarTree.TovarCategory extracted = categories.get(0);
        Assertions.assertThat(extracted.getContentLabForm()).isEqualTo("MODEL_FORM_CONTENT");
    }

    private static int uniqId() {
        return ID_SOURCE.incrementAndGet();
    }

    private List<ExportTovarTree.TovarCategory> read() throws IOException {
        File file = registry.getFile("tovar-tree.pb");
        try (InputStream stream = ExporterUtils.getInputStream(file)) {
            List<ExportTovarTree.TovarCategory> categories = new ArrayList<>();

            byte[] magic = new byte[MAGIC_BYTES.length];
            int read = stream.read(magic);
            Assertions.assertThat(read).isEqualTo(MAGIC_BYTES.length);
            Assertions.assertThat(magic).isEqualTo(MAGIC_BYTES);

            ExportTovarTree.TovarCategory exported;
            while ((exported = ExportTovarTree.TovarCategory.parseDelimitedFrom(stream)) != null) {
                categories.add(exported);
            }
            return categories;
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private TovarCategory newCategory(int tovarId, long hid) {
        TovarCategory result = new TovarCategory(tovarId);
        result.setMarketView(MarketView.GRID);
        result.setReturnPolicies(Collections.singletonList(new CategoryGoodsReturnPolicy(225, 0)));
        result.setHid(hid);
        return result;
    }
}
