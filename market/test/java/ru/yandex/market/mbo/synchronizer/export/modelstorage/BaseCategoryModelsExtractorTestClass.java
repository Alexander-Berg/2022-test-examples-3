package ru.yandex.market.mbo.synchronizer.export.modelstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import Market.Gumoful.TemplateRendering;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.Magics;
import ru.yandex.market.mbo.core.export.yt.YtExportCategoryInfoService;
import ru.yandex.market.mbo.db.ModelStopWordsDao;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkService;
import ru.yandex.market.mbo.db.params.TovarCategoriesListProvider;
import ru.yandex.market.mbo.export.category.CategoryParametersOutputExtractor;
import ru.yandex.market.mbo.export.modelstorage.CategoryInfoLoader;
import ru.yandex.market.mbo.export.modelstorage.CategoryInfoLoaderMock;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;
import ru.yandex.market.mbo.synchronizer.export.YtSwitcher;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Categories;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportMapReduceService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportModelsTableService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.yt.YtGumofulRendererJobTestInitializer;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.pipe.ValidateSkusPipePartFactory;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.user.TestAutoUser;
import ru.yandex.market.mbo.yt.ReplicatedTableTimestampService;
import ru.yandex.market.mbo.yt.TestYtWrapper;
import ru.yandex.market.protobuf.readers.VarIntDelimerMessageReader;
import ru.yandex.market.protobuf.tools.MagicChecker;
import ru.yandex.market.protobuf.tools.MessageIterator;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;

/**
 * @author s-ermakov
 */
public abstract class BaseCategoryModelsExtractorTestClass {
    protected static final int UID = 20;
    protected static final long DEFAULT_YT_TIMEOUT = 60L;
    private static final int ENRICHMENT_MAX_DATA_SIZE = 64;
    private static final long ENRICHMENT_JOB_COUNT = 10_000L;
    private static final long REDUCE_JOB_COUNT = 10_000L;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected YPath tablePath;
    protected YPath allModelsExportPath;
    protected YPath tmpPath;

    protected AutoUser autoUser;
    protected TestYtWrapper ytWrapper;
    protected YtExportModelsTableService ytExportModelsTableService;
    protected YtExportCategoryInfoService ytExportCategoryInfoService;
    protected YtGumofulRendererJobTestInitializer rendererInitializer;
    protected YtExportMapReduceService ytExportMRService;
    protected CategoryInfoLoader categoryInfoLoader;
    protected TovarTreeServiceMock tovarTreeService;
    protected CategoryParametersOutputExtractor categoryParametersOutputExtractor;
    protected SmtpCategoryValidationResultsSender emailService;
    protected TovarCategoriesListProvider tovarCategoriesListProvider;
    protected ValidateSkusPipePartFactory validateSkusPipePartFactory;
    protected YtSwitcher ytSwitcher;
    protected ValueLinkService valueLinkService;
    protected ModelStopWordsDao modelStopWordsDao;

    protected CategoryModelsExtractor extractor;
    protected ExportRegistry registry;
    protected ExtractorWriterService extractorWriterService;


    @Before
    public void setUp() throws Exception {
        this.autoUser = TestAutoUser.create();
        this.tablePath = YPath.simple("//home/model-storage/models");
        this.allModelsExportPath = YPath.simple("//home/model-storage/export");
        this.tmpPath = YPath.simple("//tmp");

        this.ytWrapper = new TestYtWrapper();
        this.ytWrapper.cypress().create(allModelsExportPath, CypressNodeType.MAP);
        this.ytWrapper.cypress().create(tmpPath, CypressNodeType.MAP);

        ReplicatedTableTimestampService replicatedTableTimestampService =
            mock(ReplicatedTableTimestampService.class);
        Mockito.when(
            replicatedTableTimestampService.getPathWithTimestamp(Mockito.any(GUID.class), Mockito.any(YPath.class)))
            .then(invocation -> invocation.getArgument(1));
        this.ytExportModelsTableService = new YtExportModelsTableService(ytWrapper, tablePath, tmpPath,
            replicatedTableTimestampService, GUID.create());
        this.ytExportCategoryInfoService = new YtExportCategoryInfoService(ytWrapper);
        this.rendererInitializer = new YtGumofulRendererJobTestInitializer(ytWrapper.yt(),
            folder.newFile("cpp_binary"));
        this.rendererInitializer.setDefaultRenderingResult(
            TemplateRendering.TModelRenderingResult.newBuilder().build());

        this.categoryInfoLoader = new CategoryInfoLoaderMock(Categories.CATEGORY_INFO_1, Categories.CATEGORY_INFO_2);
        this.tovarTreeService = new TovarTreeServiceMock();
        this.valueLinkService = Mockito.mock(ValueLinkService.class);
        this.tovarTreeService.addCategory(TovarCategoryBuilder.newBuilder()
            .setHid(TovarCategory.ROOT_HID)
            .create());
        this.tovarTreeService.addCategory(TovarCategoryBuilder.newBuilder()
            .setHid(Categories.CATEGORY_INFO_1.getCategoryId())
            .setParentHid(TovarCategory.ROOT_HID)
            .create());
        this.tovarTreeService.addCategory(TovarCategoryBuilder.newBuilder()
            .setHid(Categories.CATEGORY_INFO_2.getCategoryId())
            .setParentHid(TovarCategory.ROOT_HID)
            .create());
        this.modelStopWordsDao = Mockito.mock(ModelStopWordsDao.class);

        this.ytExportMRService = new YtExportMapReduceService(ytWrapper, ytExportModelsTableService,
            ytExportCategoryInfoService, categoryInfoLoader, ytWrapper.pool(),
            ENRICHMENT_MAX_DATA_SIZE, ENRICHMENT_JOB_COUNT, REDUCE_JOB_COUNT,
            ytWrapper.ytAccount(),
            allModelsExportPath, rendererInitializer.getExecutableFile(), tmpPath,
            DEFAULT_YT_TIMEOUT, DEFAULT_YT_TIMEOUT, valueLinkService, modelStopWordsDao, true, false, false);

        this.tovarCategoriesListProvider = mock(TovarCategoriesListProvider.class);
        Mockito.doAnswer(invok -> {
            TovarTree tovarTree = this.tovarTreeService.loadTovarTree();
            TovarCategoryNode category1 = tovarTree.byHid(Categories.CATEGORY_INFO_1.getCategoryId());
            TovarCategoryNode category2 = tovarTree.byHid(Categories.CATEGORY_INFO_2.getCategoryId());
            return Stream.of(category1, category2)
                .map(TreeNode::getData)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(TovarCategory::getHid))));
        }).when(tovarCategoriesListProvider).getLeafCategoriesWithModels();
        this.emailService = mock(SmtpCategoryValidationResultsSender.class);

        this.categoryParametersOutputExtractor = mock(CategoryParametersOutputExtractor.class);
        Mockito.when(categoryParametersOutputExtractor.extractCategoriesParameters(anyCollection(), anyCollection()))
            .thenReturn(Categories.listCategories(Categories.CATEGORY_1, Categories.CATEGORY_2));

        this.validateSkusPipePartFactory = new ValidateSkusPipePartFactory();
        this.validateSkusPipePartFactory.setAlertThresholdInHours(1);

        this.ytSwitcher = mock(YtSwitcher.class);

        this.extractor = new CategoryModelsExtractor(ytWrapper, tovarCategoriesListProvider,
            categoryParametersOutputExtractor, emailService, validateSkusPipePartFactory,
            ytExportMRService, autoUser, DEFAULT_YT_TIMEOUT, DEFAULT_YT_TIMEOUT);
        this.extractor.setYtSwitcher(ytSwitcher);

        this.registry = new ExportRegistry();
        this.registry.setRootPath(folder.newFolder().getAbsolutePath());
        this.registry.setYtExportPath(allModelsExportPath.toString());
        this.registry.setMuteMode(true);
        this.registry.afterPropertiesSet();
        this.registry.processStart();
        this.extractorWriterService = new ExtractorWriterService();
        this.extractorWriterService.setYt(ytWrapper);
        this.extractor.setExtractorWriterService(extractorWriterService);
        this.extractor.setRegistry(registry);
        this.extractor.afterPropertiesSet();
    }

    protected static void assertNoFailedFiles(ExportRegistry exportRegistry) {
        Assertions.assertThat(exportRegistry.getFailedFiles())
            .describedAs("Expected no failed files in extractor. If it happened, check before logs for error message")
            .isEmpty();
    }

    public static List<ModelStorage.Model> cleanExportTsFlag(List<ModelStorage.Model> models) {
        return models.stream().map(it -> it.toBuilder().clearExportTs().build()).collect(Collectors.toList());
    }

    public static ModelStorage.Model cleanExportTsFlag(ModelStorage.Model model) {
        return cleanExportTsFlag(Collections.singletonList(model)).get(0);
    }

    private List<ModelStorage.Model> readModelsFromFile(File file) {
        List<ModelStorage.Model> result = new ArrayList<>();

        try (GZIPInputStream inputStream = new GZIPInputStream(new FileInputStream(file))) {
            MagicChecker.checkMagic(inputStream, Magics.MagicConstants.MBEM.name());
            MessageIterator<ModelStorage.Model> iterator = new MessageIterator<>(
                new VarIntDelimerMessageReader<>(ModelStorage.Model.PARSER, inputStream)
            );

            while (iterator.hasNext()) {
                result.add(iterator.next());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    protected static class SplitResult {
        private final List<ModelStorage.Model> allModels;
        private final List<ModelStorage.Model> models;
        private final List<ModelStorage.Model> deletedModels;
        private final List<ModelStorage.Model> skus;

        SplitResult(List<ModelStorage.Model> allModels,
                    List<ModelStorage.Model> models,
                    List<ModelStorage.Model> deletedModels,
                    List<ModelStorage.Model> skus) {
            this.allModels = allModels;
            this.models = models;
            this.deletedModels = deletedModels;
            this.skus = skus;
        }

        SplitResult(SplitResult splitResult) {
            this.allModels = new ArrayList<>(splitResult.allModels);
            this.models = new ArrayList<>(splitResult.models);
            this.deletedModels = new ArrayList<>(splitResult.deletedModels);
            this.skus = new ArrayList<>(splitResult.skus);
        }

        public void add(SplitResult splitResult) {
            this.allModels.addAll(splitResult.allModels);
            this.models.addAll(splitResult.models);
            this.deletedModels.addAll(splitResult.deletedModels);
            this.skus.addAll(splitResult.skus);
        }

        public List<ModelStorage.Model> getAllModels() {
            return allModels;
        }

        public List<ModelStorage.Model> getModels() {
            return models;
        }

        public List<ModelStorage.Model> getDeletedModels() {
            return deletedModels;
        }

        public List<ModelStorage.Model> getSkus() {
            return skus;
        }

        public static SplitResult concat(SplitResult... splits) {
            SplitResult result = new SplitResult(splits[0]);
            for (int i = 1; i < splits.length; i++) {
                result.add(splits[i]);
            }
            return result;
        }
    }

    protected SplitResult splitInYt() {
        YPath exportDir = allModelsExportPath.child(registry.getFolderName()).child("models");

        return new SplitResult(
            cleanExportTsFlag(ytWrapper.readModelTable(exportDir.child("all_models"))),
            cleanExportTsFlag(ytWrapper.readModelTable(exportDir.child("models"))),
            cleanExportTsFlag(ytWrapper.readModelTable(exportDir.child("deleted_models"))),
            cleanExportTsFlag(ytWrapper.readModelTable(exportDir.child("sku")))
        );
    }

    protected SplitResult splitInExtractor(long categoryId) {
        File rootDir = registry.getRootDir();

        return new SplitResult(
            cleanExportTsFlag(readModelsFromFile(new File(rootDir, String.format("all_models_%d.pb.gz", categoryId)))),
            cleanExportTsFlag(readModelsFromFile(new File(rootDir, String.format("models_%d.pb.gz", categoryId)))),
            cleanExportTsFlag(readModelsFromFile(new File(rootDir, String.format("deleted_models_%d.pb.gz",
                categoryId)))),
            cleanExportTsFlag(readModelsFromFile(new File(rootDir, String.format("sku_%d.pb.gz", categoryId))))
        );
    }
}
