package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.GuruVendorsReaderStub;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.db.transfer.step.result.CopyLandingResultService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorServiceMock;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeSqlFilter;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.CopyLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.CopyLandingResult;
import ru.yandex.market.mbo.gwt.models.transfer.step.LandingEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.tms.modeltransfer.CopyLandingConfigBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.tms.modeltransfer.ResultInfoBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.ENUM1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getEnumParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getNameParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getOption;

/**
 * @author danfertev
 * @since 22.11.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CopyLandingStepProcessorTest {
    private static final long SOURCE_CATEGORY_ID = 1L;
    private static final long TARGET_CATEGORY_ID = 2L;
    private static final long SOURCE_LANDING_ID1 = 10L;
    private static final long SOURCE_LANDING_ID2 = 11L;
    private static final long TARGET_LANDING_ID1 = 20L;
    private static final long TARGET_LANDING_ID2 = 21L;

    private CopyLandingStepProcessor processor;
    private RecipeService recipeService;
    private ParameterLoaderServiceStub parameterLoaderService;
    private CopyLandingResultService resultService;
    private GuruVendorsReaderStub vendorsReaderStub;
    private GlobalVendorServiceMock globalVendorService;

    @Before
    public void setUp() {
        recipeService = mock(RecipeService.class);
        parameterLoaderService = new ParameterLoaderServiceStub();
        resultService = mock(CopyLandingResultService.class);
        vendorsReaderStub = new GuruVendorsReaderStub();
        globalVendorService = new GlobalVendorServiceMock();

        parameterLoaderService.addCategoryParam(getNameParam(SOURCE_CATEGORY_ID));
        parameterLoaderService.addCategoryParam(getNameParam(TARGET_CATEGORY_ID));

        processor = new CopyLandingStepProcessor(recipeService, parameterLoaderService, resultService,
            vendorsReaderStub, globalVendorService);
    }

    @Test
    public void unableToLoadLastCompletedResult() {
        ResultInfo completedResultInfo = ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
            .resultType(ResultInfo.Type.EXECUTION)
            .started(new Date())
            .completed(new Date())
            .build();
        ModelTransferStepInfo stepInfo = stepInfo(completedResultInfo);
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        doThrow(new OperationException("ERROR")).when(resultService).getResult(anyLong());

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, 0,
            "Невозможно загрузить результаты предыдущих запусков. Причина: ERROR.");
    }

    @Test
    public void allAlreadyCopied() {
        ResultInfo completedResultInfo = ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
            .resultType(ResultInfo.Type.EXECUTION)
            .started(new Date())
            .completed(new Date())
            .build();
        ModelTransferStepInfo stepInfo = stepInfo(completedResultInfo);
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        CopyLandingResult landingResult = new CopyLandingResult(completedResultInfo, Arrays.asList(
            landingEntry(SOURCE_LANDING_ID1, TARGET_LANDING_ID1),
            landingEntry(SOURCE_LANDING_ID2, TARGET_LANDING_ID2)));

        when(resultService.getResult(anyLong())).thenReturn(landingResult);
        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).then(args -> {
            RecipeSqlFilter filter = args.getArgument(0);

            return Stream.of(
                landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1),
                landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID2),
                landing(TARGET_CATEGORY_ID, TARGET_LANDING_ID1),
                landing(TARGET_CATEGORY_ID, TARGET_LANDING_ID2)
            ).filter(l -> filter.getIds().contains(l.getId())).collect(Collectors.toList());
        });

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.COMPLETED, 2);
        assertEntry(result, 0, ResultEntry.Status.SUCCESS, "Лендинг уже скопирован",
            SOURCE_LANDING_ID1, TARGET_LANDING_ID1);
        assertEntry(result, 1, ResultEntry.Status.SUCCESS, "Лендинг уже скопирован",
            SOURCE_LANDING_ID2, TARGET_LANDING_ID2);
    }

    @Test
    public void alreadyCopiedNotFound() {
        ResultInfo completedResultInfo = ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
            .resultType(ResultInfo.Type.EXECUTION)
            .started(new Date())
            .completed(new Date())
            .build();
        ModelTransferStepInfo stepInfo = stepInfo(completedResultInfo);
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        CopyLandingResult landingResult = new CopyLandingResult(completedResultInfo, Arrays.asList(
            landingEntry(SOURCE_LANDING_ID1, TARGET_LANDING_ID1),
            landingEntry(SOURCE_LANDING_ID2, TARGET_LANDING_ID2)));

        when(resultService.getResult(anyLong())).thenReturn(landingResult);
        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).then(args -> {
            RecipeSqlFilter filter = args.getArgument(0);

            return Stream.of(
                landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1),
                landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID2)
            ).filter(l -> filter.getIds().contains(l.getId())).collect(Collectors.toList());
        });

        doAnswer(args -> {
            List<Recipe> recipes = args.getArgument(0);
            recipes.get(0).setId(TARGET_LANDING_ID1);
            recipes.get(1).setId(TARGET_LANDING_ID2);
            return null;
        }).when(recipeService).saveRecipesWithinSingleTransaction(anyCollection(), anyBoolean());

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.COMPLETED, 2);
        assertEntry(result, 0, ResultEntry.Status.SUCCESS, "Лендинг успешно скопирован",
            SOURCE_LANDING_ID1, TARGET_LANDING_ID1);
        assertEntry(result, 1, ResultEntry.Status.SUCCESS, "Лендинг успешно скопирован",
            SOURCE_LANDING_ID2, TARGET_LANDING_ID2);
    }

    @Test
    public void unableToLoadAlreadyCopiedLandings() {
        ResultInfo completedResultInfo = ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
            .resultType(ResultInfo.Type.EXECUTION)
            .started(new Date())
            .completed(new Date())
            .build();
        ModelTransferStepInfo stepInfo = stepInfo(completedResultInfo);
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        CopyLandingResult landingResult = new CopyLandingResult(completedResultInfo, Arrays.asList(
            landingEntry(SOURCE_LANDING_ID1, TARGET_LANDING_ID1),
            landingEntry(SOURCE_LANDING_ID2, TARGET_LANDING_ID2)));

        when(resultService.getResult(anyLong())).thenReturn(landingResult);
        doThrow(new OperationException("ERROR")).when(recipeService).getSearchRecipes(any(), any(), anyBoolean());

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, 0,
            "Невозможно загрузить уже скопированные лендинги. Причина: ERROR.");
    }

    @Test
    public void noSourceParamAndSuccessBecomeWarning() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption1 = getOption("option1");
        sourceEnumParam.addOption(sourceOption1);

        ModelTransferStepInfo stepInfo = stepInfo();
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).thenReturn(Arrays.asList(
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1, landingFilter(sourceEnumParam, sourceOption1)),
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID2)
        ));

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, 2);
        assertEntry(result, 0, ResultEntry.Status.FAILURE,
            String.format("Невозможно найти параметр %d в исходной категории.", sourceEnumParam.getId()),
            SOURCE_LANDING_ID1);
        assertEntry(result, 1, ResultEntry.Status.WARNING,
            "Лендинг не скопирован из-за ошибок при копировании других лендингов",
            SOURCE_LANDING_ID2);
    }

    @Test
    public void noTargetParam() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption1 = getOption("option1");
        sourceEnumParam.addOption(sourceOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);

        ModelTransferStepInfo stepInfo = stepInfo();
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).thenReturn(Collections.singletonList(
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1, landingFilter(sourceEnumParam, sourceOption1))
        ));

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, 1);
        assertEntry(result, 0, ResultEntry.Status.FAILURE,
            String.format("Невозможно найти параметр %d %s в категории назначения.",
                sourceEnumParam.getId(), sourceEnumParam.getName()),
            SOURCE_LANDING_ID1);
    }

    @Test
    public void noSourceOption() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption1 = getOption("option1");
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        ModelTransferStepInfo stepInfo = stepInfo();
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).thenReturn(Collections.singletonList(
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1, landingFilter(sourceEnumParam, sourceOption1))
        ));

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, 1);
        assertEntry(result, 0, ResultEntry.Status.FAILURE,
            String.format("Невозможно найти опцию %d в параметре %d %s в исходной категории.",
                sourceOption1.getId(), sourceEnumParam.getId(), sourceEnumParam.getName()),
            SOURCE_LANDING_ID1);
    }

    @Test
    public void noTargetOption() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption1 = getOption("option1");
        sourceEnumParam.addOption(sourceOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        ModelTransferStepInfo stepInfo = stepInfo();
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).thenReturn(Collections.singletonList(
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1, landingFilter(sourceEnumParam, sourceOption1))
        ));

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, 1);
        assertEntry(result, 0, ResultEntry.Status.FAILURE,
            String.format("Невозможно найти опцию %d %s в категории назначения.",
                sourceOption1.getId(), sourceOption1.getName()),
            SOURCE_LANDING_ID1);
    }

    @Test
    public void unableToSaveLandings() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption1 = getOption("option1");
        sourceEnumParam.addOption(sourceOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        Option targetOption1 = getOption("option1");
        targetEnumParam.addOption(targetOption1);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        ModelTransferStepInfo stepInfo = stepInfo();
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).thenReturn(Arrays.asList(
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1, landingFilter(sourceEnumParam, sourceOption1)),
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID2)
        ));

        doThrow(new OperationException("ERROR"))
            .when(recipeService).saveRecipesWithinSingleTransaction(anyCollection(), anyBoolean());

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, 2);
        assertEntry(result, 0, ResultEntry.Status.FAILURE,
            "Невозможно скопировать лендинг из-за ошибки: ERROR", SOURCE_LANDING_ID1);
        assertEntry(result, 1, ResultEntry.Status.FAILURE,
            "Невозможно скопировать лендинг из-за ошибки: ERROR", SOURCE_LANDING_ID2);
    }

    @Test
    public void duplicateNameAndHeader() {
        ModelTransferStepInfo stepInfo = stepInfo();
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).thenReturn(Collections.singletonList(
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1)
        ));

        when(recipeService.validateRecipeNames(any(), eq(false))).thenReturn("ERROR");

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, 1);
        assertEntry(result, 0, ResultEntry.Status.FAILURE,
            String.format("Лендинг header скопировано из категории %d в категорию %d содержит ошибки: ERROR",
                SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID),
            SOURCE_LANDING_ID1);
    }

    @Test
    public void successCopyLandings() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption1 = getOption("option1");
        sourceEnumParam.addOption(sourceOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        Option targetOption1 = getOption("option1");
        targetEnumParam.addOption(targetOption1);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        ModelTransferStepInfo stepInfo = stepInfo();
        ModelTransferJobContext<CopyLandingConfig> context = jobContext(stepInfo);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean())).thenReturn(Arrays.asList(
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID1, landingFilter(sourceEnumParam, sourceOption1)),
            landing(SOURCE_CATEGORY_ID, SOURCE_LANDING_ID2)
        ));

        doAnswer(args -> {
            List<Recipe> recipes = args.getArgument(0);
            recipes.get(0).setId(TARGET_LANDING_ID1);
            recipes.get(1).setId(TARGET_LANDING_ID2);
            return null;
        }).when(recipeService).saveRecipesWithinSingleTransaction(anyCollection(), anyBoolean());

        CopyLandingResult result = processor.executeStep(resultInfo(), context);

        assertResult(result, ResultInfo.Status.COMPLETED, 2);
        assertEntry(result, 0, ResultEntry.Status.SUCCESS, "Лендинг успешно скопирован",
            SOURCE_LANDING_ID1, TARGET_LANDING_ID1);
        assertEntry(result, 1, ResultEntry.Status.SUCCESS, "Лендинг успешно скопирован",
            SOURCE_LANDING_ID2, TARGET_LANDING_ID2);
    }

    private ModelTransferStepInfo stepInfo(ResultInfo... executionResults) {
        ModelTransferStepInfo si = new ModelTransferStepInfo();
        si.setStepType(ModelTransferStep.Type.COPY_LANDINGS);
        for (ResultInfo ri : executionResults) {
            si.getExecutionResultInfos().add(ri);
        }
        return si;
    }

    private ModelTransferJobContext<CopyLandingConfig> jobContext(
        ModelTransferStepInfo stepInfo) {

        CopyLandingConfig config = CopyLandingConfigBuilder.newBuilder()
            .landings(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, SOURCE_LANDING_ID1, SOURCE_LANDING_ID2)
            .build();

        return new ModelTransferJobContext<>(new ModelTransfer(), stepInfo, Collections.singletonList(stepInfo),
            config, Collections.emptyList());
    }

    private ResultInfo resultInfo() {
        return ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED).resultType(ResultInfo.Type.EXECUTION).build();
    }

    private LandingEntry landingEntry(long sourceLandingId, Long targetLandingId) {
        LandingEntry entry = new LandingEntry();
        entry.setSourceCategoryId(SOURCE_CATEGORY_ID);
        entry.setSourceLandingId(sourceLandingId);
        entry.setTargetCategoryId(TARGET_CATEGORY_ID);
        if (targetLandingId != null) {
            entry.setTargetLandingId(targetLandingId);
        }
        return entry;
    }

    private void assertResult(CopyLandingResult result, ResultInfo.Status status, int size, String resultText) {
        ResultInfo ri = result.getResultInfo();
        assertThat(ri.getStatus()).isEqualTo(status);
        String expectedResultText;
        if (resultText == null) {
            switch (status) {
                case COMPLETED:
                    expectedResultText = "Лендинги успешно обработаны";
                    break;
                case FAILED:
                    expectedResultText = "Невозможно скопировать лендинги";
                    break;
                default:
                    expectedResultText = "";
            }
        } else {
            expectedResultText = resultText;
        }

        assertThat(ri.getResultText()).isEqualTo(expectedResultText);
        assertThat(result.getResultEntries()).hasSize(size);
    }

    private void assertResult(CopyLandingResult result, ResultInfo.Status status, int size) {
        assertResult(result, status, size, null);
    }

    private void assertEntry(CopyLandingResult result, int index, ResultEntry.Status status, String statusMessage,
                             long sourceLandingId, Long targetLandingId) {
        LandingEntry entry = result.getResultEntries().get(index);
        assertThat(entry.getStatus()).isEqualTo(status);
        assertThat(entry.getStatusMessage()).isEqualTo(statusMessage);
        assertThat(entry.getSourceCategoryId()).isEqualTo(SOURCE_CATEGORY_ID);
        assertThat(entry.getSourceLandingId()).isEqualTo(sourceLandingId);
        assertThat(entry.getTargetCategoryId()).isEqualTo(TARGET_CATEGORY_ID);
        if (targetLandingId != null) {
            assertThat(entry.getTargetLandingId()).isEqualTo(targetLandingId);
        }
    }

    private void assertEntry(CopyLandingResult result, int index, ResultEntry.Status status, String statusMessage,
                             long sourceLandingId) {
        assertEntry(result, index, status, statusMessage, sourceLandingId, null);
    }

    private Recipe landing(long categoryId, long landingId, RecipeFilter... filters) {
        Recipe landing = new Recipe();
        landing.setId(landingId);
        landing.setHid(categoryId);
        landing.setNavigation(false);
        landing.setHeader("header");
        landing.setName("name");
        landing.setFilters(Arrays.asList(filters));
        return landing;
    }

    private RecipeFilter landingFilter(CategoryParam param, Option... options) {
        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(param.getId());
        filter.setParamType(param.getType());
        filter.setValueIds(Arrays.stream(options).map(Option::getId).collect(Collectors.toList()));
        return filter;
    }
}
