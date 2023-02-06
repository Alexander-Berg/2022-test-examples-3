package ru.yandex.market.mbo.synchronizer.export.modelstorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Consumer;

import Market.Gumoful.TemplateRendering.TModelRenderingResult;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.core.export.yt.YtExportCategoryInfoService;
import ru.yandex.market.mbo.db.ModelStopWordsDao;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkService;
import ru.yandex.market.mbo.export.modelstorage.CategoryInfoLoaderMock;
import ru.yandex.market.mbo.export.modelstorage.utils.SortedCategoriesIterableWrapper;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkSearchCriteria;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Categories;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Models;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.ExportModelsConfig;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportMapReduceService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportModelsTableService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.yt.YtGumofulRendererJobTestInitializer;
import ru.yandex.market.mbo.synchronizer.export.registry.RegistryWorkerTemplate;
import ru.yandex.market.mbo.synchronizer.export.report.ExportWarningsNotificationsException;
import ru.yandex.market.mbo.yt.ReplicatedTableTimestampService;
import ru.yandex.market.mbo.yt.TestYtWrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.common.model.KnownIds.VENDOR_PARAM_ID;
import static ru.yandex.market.mbo.synchronizer.export.modelstorage.BaseCategoryModelsExtractorTestClass.cleanExportTsFlag;

/**
 * @author s-ermakov
 */
@RunWith(MockitoJUnitRunner.class)
public class YtExportMapReduceServiceTest {
    private static final long ONE_HOUR_IN_MS = 60L * 60L * 1000L;
    private static final long DEFAULT_YT_TIMEOUT = 60L;
    private static final int MAX_JOB_DATA_SIZE = 64;
    private static final long JOB_COUNT = 10_000L;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private YPath tablePath;
    private YPath allModelsExportPath;
    private YPath exportPath;
    private YPath tmpPath;

    private long sessionTime;
    private TestYtWrapper ytWrapper;
    private YtExportModelsTableService ytExportModelsTableService;
    private YtExportCategoryInfoService ytExportCategoryInfoService;
    private YtGumofulRendererJobTestInitializer rendererInitializer;
    private YtExportMapReduceService ytExportMRService;
    private ExportRegistry registry;
    private ValueLinkService valueLinkService;
    private ModelStopWordsDao modelStopWordsDao;

    @Before
    public void setUp() throws IOException {
        this.tablePath = YPath.simple("//home/model-storage/models");
        this.allModelsExportPath = YPath.simple("//home/model-storage/export");
        this.exportPath = YPath.simple("//home/mbo/export");
        this.tmpPath = YPath.simple("//tmp");

        this.sessionTime = new Date().getTime();
        this.ytWrapper = new TestYtWrapper();
        this.rendererInitializer = new YtGumofulRendererJobTestInitializer(ytWrapper.yt(), testFolder.newFile());
        this.rendererInitializer.setDefaultRenderingResult(TModelRenderingResult.newBuilder().build());

        ReplicatedTableTimestampService replicatedTableTimestampService =
            Mockito.mock(ReplicatedTableTimestampService.class);
        when(
            replicatedTableTimestampService.getPathWithTimestamp(any(GUID.class), any(YPath.class)))
            .then(invocation -> invocation.getArgument(1));
        this.ytExportModelsTableService = new YtExportModelsTableService(ytWrapper, tablePath, tmpPath,
            replicatedTableTimestampService, GUID.create());
        this.ytExportCategoryInfoService = new YtExportCategoryInfoService(ytWrapper);

        this.valueLinkService = Mockito.mock(ValueLinkService.class);
        this.modelStopWordsDao = Mockito.mock(ModelStopWordsDao.class);

        this.registry = new ExportRegistry();
        this.registry.setRootPath(testFolder.newFolder().getAbsolutePath());
        this.registry.setYtExportPath(allModelsExportPath.toString());
        this.registry.setMuteMode(true);
        this.registry.afterPropertiesSet();

        CategoryInfoLoaderMock categoryInfoLoader = new CategoryInfoLoaderMock(
            Categories.CATEGORY_INFO_1, Categories.CATEGORY_INFO_2);

        this.ytExportMRService = new YtExportMapReduceService(ytWrapper, ytExportModelsTableService,
            ytExportCategoryInfoService, categoryInfoLoader, ytWrapper.pool(),
            MAX_JOB_DATA_SIZE, JOB_COUNT, JOB_COUNT,
            ytWrapper.ytAccount(),
            allModelsExportPath, rendererInitializer.getExecutableFile(), tmpPath,
            DEFAULT_YT_TIMEOUT, DEFAULT_YT_TIMEOUT, valueLinkService, modelStopWordsDao, true, false, false);
    }

    @Test
    public void testEnrichmentOfModel() {
        List<ModelStorage.Model> models = Collections.singletonList(Models.M2);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(Models.M2_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfModelWithModifications() {
        List<ModelStorage.Model> models = Arrays.asList(Models.M1, Models.MODIF1, Models.MODIF2);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(
                Models.M1_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build(),
                Models.MODIF1_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build(),
                Models.MODIF2_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfModelWithSku() {
        List<ModelStorage.Model> models = Arrays.asList(Models.M1, Models.SKU1_1, Models.SKU1_2);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(
                Models.M1_ENRICHED.toBuilder().clearGroupSize().build(),
                Models.SKU1_1_ENRICHED, Models.SKU1_2_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfModelWithModifAndSku() {
        List<ModelStorage.Model> models = Arrays.asList(
            Models.M1, Models.SKU1_1, Models.SKU1_2, Models.SKU_MODIF_2,
            Models.MODIF1, Models.MODIF2,
            Models.SKU_MODIF_11, Models.SKU_MODIF_12);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(
                Models.M1_ENRICHED, Models.SKU1_1_ENRICHED, Models.SKU1_2_ENRICHED, Models.SKU_MODIF_2_ENRICHED,
                Models.MODIF1_ENRICHED, Models.MODIF2_ENRICHED,
                Models.SKU_MODIF_11_ENRICHED, Models.SKU_MODIF_12_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfGuruWithIsSkuFlag() {
        List<ModelStorage.Model> models = Collections.singletonList(Models.M3);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(
                Models.M3_ENRICHED, Models.M3_SKU_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfDeletedModels() {
        long oneHourAgo = new Date().getTime() - ONE_HOUR_IN_MS;
        List<ModelStorage.Model> models = Arrays.asList(
            Models.M2.toBuilder().setDeleted(true).setDeletedDate(oneHourAgo).build(),
            Models.PARTNER1.toBuilder().setDeleted(true).setDeletedDate(oneHourAgo).build(),
            Models.PARTNER_SKU1_1.toBuilder().setDeleted(true).setDeletedDate(oneHourAgo).build(),
            Models.PARTNER2,
            Models.C1);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(Models.C1_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfPartnerModels() {
        List<ModelStorage.Model> models = Collections.singletonList(Models.PARTNER1);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .extracting(Models::removeIsPartnerModificationDate)
            .containsExactlyInAnyOrder(Models.PARTNER1_ENRICHED
                .toBuilder()
                .setPublishedOnBlueMarket(false)
                .build());

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfPartnerModelsWithPartnerSku() {
        List<ModelStorage.Model> models = Arrays.asList(Models.PARTNER1,
            Models.PARTNER_SKU1_1, Models.PARTNER_SKU1_2);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .extracting(Models::removeIsPartnerModificationDate)
            .containsExactlyInAnyOrder(Models.PARTNER1_ENRICHED, Models.PARTNER_SKU1_1_ENRICHED,
                Models.PARTNER_SKU1_2_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfPartnerSku() {
        List<ModelStorage.Model> models = Collections.singletonList(Models.PARTNER_SKU1_1_ENRICHED);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels).isEmpty();

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfExperimentalModels() {
        List<ModelStorage.Model> models = Arrays.asList(
            Models.EXP_M1, Models.EXP_SKU1_1,
            Models.EXP_MODIF1, Models.EXP_SKU_MODIF_11);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(
                Models.EXP_M1_ENRICHED, Models.EXP_SKU1_1_ENRICHED,
                Models.EXP_MODIF1_ENRICHED, Models.EXP_SKU_MODIF_11_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfExperimentalWithIsSkuFlag() {
        List<ModelStorage.Model> models = Collections.singletonList(Models.EXP_M2);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(
                Models.EXP_M2_ENRICHED, Models.EXP_M2_SKU_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testEnrichmentOfGuruDummyModel() {
        List<ModelStorage.Model> models = Collections.singletonList(Models.DUMMY_GURU);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .containsExactlyInAnyOrder(Models.DUMMY_GURU_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testModelsAllTogether() {
        List<ModelStorage.Model> models = Arrays.asList(
            // first group
            Models.M1, Models.SKU1_1, Models.SKU1_2, Models.SKU_MODIF_2,
            Models.MODIF1, Models.MODIF2,
            Models.SKU_MODIF_11, Models.SKU_MODIF_12,
            // others
            Models.M2,
            Models.M3,
            Models.C1,
            Models.PARTNER1, Models.PARTNER_SKU1_1, Models.PARTNER_SKU1_2,
            Models.PARTNER2,
            Models.PARTNER3, Models.PARTNER_SKU3_1,
            // exp group
            Models.EXP_M1, Models.EXP_SKU1_1,
            Models.EXP_MODIF1, Models.EXP_SKU_MODIF_11
        );

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(models);

        Assertions.assertThat(enrichedModels)
            .extracting(Models::removeIsPartnerModificationDate)
            .containsExactlyInAnyOrder(
                // first group
                Models.M1_ENRICHED, Models.SKU1_1_ENRICHED, Models.SKU1_2_ENRICHED, Models.SKU_MODIF_2_ENRICHED,
                Models.MODIF1_ENRICHED, Models.MODIF2_ENRICHED,
                Models.SKU_MODIF_11_ENRICHED, Models.SKU_MODIF_12_ENRICHED,
                // second
                Models.M2_ENRICHED,
                // third
                Models.M3_ENRICHED, Models.M3_SKU_ENRICHED,
                // fourth
                Models.C1_ENRICHED,
                // fifth
                Models.PARTNER1_ENRICHED, Models.PARTNER_SKU1_1_ENRICHED, Models.PARTNER_SKU1_2_ENRICHED,
                // sixth
                Models.PARTNER3_ENRICHED, Models.PARTNER_SKU3_1_ENRICHED,
                // exp group
                Models.EXP_M1_ENRICHED, Models.EXP_SKU1_1_ENRICHED,
                Models.EXP_MODIF1_ENRICHED, Models.EXP_SKU_MODIF_11_ENRICHED
            );
    }

    @Test
    public void testEnrichAndSplitModelsToSeveralTables() throws ExportWarningsNotificationsException {
        List<ModelStorage.Model> models = Arrays.asList(
            // first group
            Models.M1, Models.SKU1_1, Models.SKU1_2, Models.SKU_MODIF_2,
            Models.MODIF1, Models.MODIF2,
            Models.SKU_MODIF_11, Models.SKU_MODIF_12,
            // other groups
            Models.M2,
            Models.M3,
            Models.C1,
            Models.PARTNER1, Models.PARTNER_SKU1_1, Models.PARTNER_SKU1_2,
            Models.PARTNER2,
            Models.PARTNER3, Models.PARTNER_SKU3_1,
            // exp group
            Models.EXP_M1, Models.EXP_SKU1_1,
            Models.EXP_MODIF1, Models.EXP_SKU_MODIF_11,
            // guru dummy
            Models.DUMMY_GURU
        );

        YPath sortedTable = enrichModels(models);

        ExportModelsConfig config = new ExportModelsConfig(
            exportPath.child("all_models"),
            exportPath.child("models"),
            exportPath.child("deleted_models"),
            exportPath.child("skus_models")
        );

        ytExportMRService.exportModels(sessionTime, sortedTable, config);

        List<ModelStorage.Model> allModels = cleanExportTsFlag(
            ytWrapper.readModelTable(config.getAllModelsOutputTable()));
        List<ModelStorage.Model> modelsTable = cleanExportTsFlag(
            ytWrapper.readModelTable(config.getModelsOutputTable()));
        List<ModelStorage.Model> deletedModels = cleanExportTsFlag(
            ytWrapper.readModelTable(config.getDeletedModelsOutputTable()));
        List<ModelStorage.Model> skuModels = cleanExportTsFlag(
            ytWrapper.readModelTable(config.getSkusOutputTable()));

        Assertions.assertThat(allModels)
            .extracting(Models::removeIsPartnerModificationDate)
            .containsExactlyInAnyOrder(
                Models.M1_ENRICHED, Models.MODIF1_ENRICHED, Models.MODIF2_ENRICHED,
                Models.M3_ENRICHED, Models.M2_ENRICHED,
                Models.PARTNER1_ENRICHED,
                Models.PARTNER3_ENRICHED,
                Models.EXP_M1_ENRICHED,
                Models.EXP_MODIF1_ENRICHED,
                Models.DUMMY_GURU_ENRICHED);

        Assertions.assertThat(modelsTable)
            .extracting(Models::removeIsPartnerModificationDate)
            .containsExactlyInAnyOrder(
                Models.M1_ENRICHED, Models.MODIF1_ENRICHED,
                Models.M3_ENRICHED, Models.M2_ENRICHED,
                Models.PARTNER1_ENRICHED,
                Models.PARTNER3_ENRICHED,
                Models.EXP_M1_ENRICHED,
                Models.EXP_MODIF1_ENRICHED,
                Models.DUMMY_GURU_ENRICHED);

        Assertions.assertThat(deletedModels)
            .extracting(Models::removeIsPartnerModificationDate)
            .containsExactlyInAnyOrder(
                Models.C1_ENRICHED);

        Assertions.assertThat(skuModels)
            .extracting(Models::removeIsPartnerModificationDate)
            .containsExactlyInAnyOrder(
                Models.SKU1_1_ENRICHED, Models.SKU1_2_ENRICHED,
                Models.SKU_MODIF_11_ENRICHED, Models.SKU_MODIF_12_ENRICHED,
                Models.SKU_MODIF_2_ENRICHED,
                Models.M3_SKU_ENRICHED,
                Models.PARTNER_SKU1_1_ENRICHED, Models.PARTNER_SKU1_2_ENRICHED,
                Models.PARTNER_SKU3_1_ENRICHED,
                Models.EXP_SKU1_1_ENRICHED,
                Models.EXP_SKU_MODIF_11_ENRICHED);

        verifyGettingValueLinks();
    }

    @Test
    public void testSettingManufacturerParameter() {
        final long manufacturerId = 99L;

        List<ValueLink> manufacturerValueLinks = new ArrayList<>();
        ValueLink valueLink = new ValueLink();
        valueLink.setSourceParamId(VENDOR_PARAM_ID);
        valueLink.setSourceOptionId(Models.M1.getVendorId());
        valueLink.setTargetOptionId(manufacturerId);
        manufacturerValueLinks.add(valueLink);

        ValueLinkSearchCriteria valueLinkSearchCriteria = new ValueLinkSearchCriteria();
        valueLinkSearchCriteria.setType(ValueLinkType.MANUFACTURER);

        when(valueLinkService.findValueLinks(valueLinkSearchCriteria)).thenReturn(manufacturerValueLinks);

        List<ModelStorage.Model> enrichedModels = enrichAndGetModels(Collections.singletonList(Models.M1));

        Optional<ModelStorage.ParameterValue> parameter = enrichedModels.get(0).getParameterValuesList()
            .stream()
            .filter(e -> e.getXslName().equals(XslNames.MANUFACTURER))
            .findAny();

        Assertions.assertThat(parameter).isNotEmpty();
        Assertions.assertThat(parameter.get())
            .extracting(ModelStorage.ParameterValue::getOptionId)
            .isEqualTo((int) manufacturerId);
    }

    private YPath enrichModels(List<ModelStorage.Model> models) {
        ytWrapper.createModelTable(tablePath, models);

        // run enrichment
        RegistryWorkerTemplate workerTemplate = RegistryWorkerTemplate.newRegistryWorker(registry);
        SortedCategoriesIterableWrapper categories = Categories.listCategories(
            Categories.CATEGORY_1, Categories.CATEGORY_2);
        TreeSet<TovarCategory> set = new TreeSet<TovarCategory>(Comparator.comparing(TovarCategory::getHid)) {{
            add(TovarCategoryBuilder.newBuilder().setHid(1L).create());
            add(TovarCategoryBuilder.newBuilder().setHid(2L).create());
        }};

        return ytExportMRService.enrichModelAndGetRenderResult(workerTemplate, sessionTime, categories,
            set, Models.UID, false)
            .getModelsResultTable();
    }

    @Test
    public void testReplacedTitleWithLineBreak() {
        List<ModelStorage.Model> enrichedModels =
            enrichAndGetModels(Collections.singletonList(Models.M_WITH_LINE_BREAK_IN_TITLES));

        Consumer<ModelStorage.LocalizedString> assertFunction = string ->
            Assertions.assertThat(string.getValue().contains("\n")).isFalse();

        enrichedModels.forEach(model -> {
            model.getTitlesList().forEach(assertFunction);
            model.getAliasesList().forEach(assertFunction);
            model.getParameterValuesList().stream()
                .map(ModelStorage.ParameterValue::getStrValueList)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .forEach(assertFunction);
        });
    }

    private List<ModelStorage.Model> enrichAndGetModels(List<ModelStorage.Model> allModels) {
        YPath resultPath = enrichModels(allModels);
        return cleanExportTsFlag(ytWrapper.readModelTable(resultPath));
    }

    private void verifyGettingValueLinks() {
        ValueLinkSearchCriteria valueLinkSearchCriteria = new ValueLinkSearchCriteria();
        valueLinkSearchCriteria.setType(ValueLinkType.MANUFACTURER);
        verify(valueLinkService, times(1)).findValueLinks(eq(valueLinkSearchCriteria));

    }
}
