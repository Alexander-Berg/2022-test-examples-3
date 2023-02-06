package ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.yt.post_enrich_models;

import Market.Gumoful.TemplateRendering;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.export.MboExportValidation;
import ru.yandex.market.mbo.export.modelstorage.YtValidationResultsReader;
import ru.yandex.market.mbo.export.modelstorage.utils.SortedCategoriesIterableWrapper;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.BaseCategoryModelsExtractorTestClass;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Categories;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Models;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportMapReduceService;
import ru.yandex.market.mbo.synchronizer.export.registry.RegistryWorkerTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Tests of {@link YtModelsPostEnrichMapper}.
 *
 * @author moskovkin
 * @author s-ermakov
 * @since 01.06.18
 */
public class YtModelsPostEnrichMapperTest extends BaseCategoryModelsExtractorTestClass {

    private static final TemplateRendering.TModelRenderingResult ERRORS_IN_RENDERING_RESULT =
        TemplateRendering.TModelRenderingResult.newBuilder()
            .setModelId(1)
            .addTemplateRenderingResults(TemplateRendering.TTemplateRenderingResult.newBuilder()
                .setTemplateType(TemplateRendering.TTemplateRenderingResult.ETemplateType.MICRO_MODEL_SEARCH)
                .addErrors(TemplateRendering.TError.newBuilder()
                    .setMessage("MICRO_MODEL_SEARCH error")
                    .build()
                )
                .build()
            )
            .addTemplateRenderingResults(TemplateRendering.TTemplateRenderingResult.newBuilder()
                .setTemplateType(TemplateRendering.TTemplateRenderingResult.ETemplateType.BRIEF_MODEL)
                .addErrors(TemplateRendering.TError.newBuilder()
                    .setMessage("BRIEF_MODEL error")
                    .build()
                )
                .build()
            )
            .build();

    @Test
    public void testRenderingErrorsFiltering() {
        TemplateRendering.TModelRenderingResult expectedResult =
            TemplateRendering.TModelRenderingResult.newBuilder()
                .setModelId(1)
                .addTemplateRenderingResults(TemplateRendering.TTemplateRenderingResult.newBuilder()
                    .setTemplateType(TemplateRendering.TTemplateRenderingResult.ETemplateType.BRIEF_MODEL)
                    .addErrors(TemplateRendering.TError.newBuilder()
                        .setMessage("BRIEF_MODEL error")
                        .build()
                    )
                    .build()
                )
                .build();

        TemplateRendering.TModelRenderingResult actualResult = YtModelsPostEnrichMapper.filterModelRenderingResult(
            ERRORS_IN_RENDERING_RESULT
        );

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testValidationResultsWriteToSeparateTable() {
        // arrange
        List<ModelStorage.Model> models = Arrays.asList(Models.M1, Models.M2.toBuilder().setPublished(true).build());

        TemplateRendering.TTemplateRenderingResult.Builder error1 =
            TemplateRendering.TTemplateRenderingResult.newBuilder()
                .setTemplateType(TemplateRendering.TTemplateRenderingResult.ETemplateType.MICRO_MODEL)
                .addErrors(TemplateRendering.TError.newBuilder()
                    .setMessage("Test message").build());

        TemplateRendering.TTemplateRenderingResult.Builder error2 =
            TemplateRendering.TTemplateRenderingResult.newBuilder()
                .setTemplateType(TemplateRendering.TTemplateRenderingResult.ETemplateType.BRIEF_MODEL)
                .addErrors(TemplateRendering.TError.newBuilder()
                    .setMessage("Second error")
                    .setPosition(TemplateRendering.TPosition.newBuilder()
                        .setOffset(1)
                        .setLength(100)
                        .build())
                    .build());

        rendererInitializer.mockModelRenderingResult(Models.M1.getId(),
            TemplateRendering.TModelRenderingResult.newBuilder()
                .addTemplateRenderingResults(error1)
                .build());
        rendererInitializer.mockModelRenderingResult(Models.M2.getId(),
            TemplateRendering.TModelRenderingResult.newBuilder()
                .addTemplateRenderingResults(error1)
                .addTemplateRenderingResults(error2)
                .build());

        // act
        YtExportMapReduceService.EnrichModelsResult result = enrichModels(models);

        // assert
        List<MboExportValidation.ModelValidationResult> results = readResults(result.getValidationResultsTable());

        Assertions.assertThat(results)
            .containsExactlyInAnyOrder(
                MboExportValidation.ModelValidationResult.newBuilder()
                    .setCategoryId(Models.M1.getCategoryId())
                    .setModelId(Models.M1.getId())
                    .setCurrentType("GURU")
                    .addRenderingResults(MboExportValidation.RenderingValidationResult.newBuilder()
                        .setTemplateType(TemplateRendering.TTemplateRenderingResult.ETemplateType.MICRO_MODEL)
                        .setMessage("Test message")
                        .build())
                    .build(),
                MboExportValidation.ModelValidationResult.newBuilder()
                    .setCategoryId(Models.M2.getCategoryId())
                    .setModelId(Models.M2.getId())
                    .setCurrentType("GURU")
                    .addRenderingResults(MboExportValidation.RenderingValidationResult.newBuilder()
                        .setTemplateType(TemplateRendering.TTemplateRenderingResult.ETemplateType.MICRO_MODEL)
                        .setMessage("Test message")
                        .build())
                    .build(),
                MboExportValidation.ModelValidationResult.newBuilder()
                    .setCategoryId(Models.M2.getCategoryId())
                    .setModelId(Models.M2.getId())
                    .setCurrentType("GURU")
                    .addRenderingResults(MboExportValidation.RenderingValidationResult.newBuilder()
                        .setTemplateType(TemplateRendering.TTemplateRenderingResult.ETemplateType.BRIEF_MODEL)
                        .setMessage("Second error at offset: 1, length: 100")
                        .build())
                    .build()
            );
    }

    @NotNull
    private List<MboExportValidation.ModelValidationResult> readResults(YPath validationResults) {
        YtValidationResultsReader validationResultsReader = new YtValidationResultsReader(ytWrapper, validationResults);
        List<MboExportValidation.ModelValidationResult> results = new ArrayList<>();
        validationResultsReader.processValidationResults(results::add);
        return results;
    }

    private YtExportMapReduceService.EnrichModelsResult enrichModels(List<ModelStorage.Model> models) {
        ytWrapper.createModelTable(tablePath, models);

        // run enrichment
        RegistryWorkerTemplate workerTemplate = RegistryWorkerTemplate.newRegistryWorker(registry);
        SortedCategoriesIterableWrapper categories = Categories.listCategories(
            Categories.CATEGORY_1, Categories.CATEGORY_2);
        TreeSet<TovarCategory> set = new TreeSet<TovarCategory>(Comparator.comparing(TovarCategory::getHid)) {{
            add(TovarCategoryBuilder.newBuilder().setHid(1L).create());
            add(TovarCategoryBuilder.newBuilder().setHid(2L).create());
        }};

        return ytExportMRService.enrichModelAndGetRenderResult(workerTemplate, 0, categories,
            set, Models.UID, false);
    }
}
