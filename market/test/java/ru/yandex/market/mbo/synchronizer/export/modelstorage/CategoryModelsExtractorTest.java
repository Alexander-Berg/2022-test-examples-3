package ru.yandex.market.mbo.synchronizer.export.modelstorage;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Models;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Models.title;

/**
 * Tests of {@link CategoryModelsExtractor}.
 *
 * @author s-ermakov
 */
public class CategoryModelsExtractorTest extends BaseCategoryModelsExtractorTestClass {

    public static final int DAYS_FROM_DELETION = 5;

    /**
     * Тест проверяет, что модели, выгружаемые в Yt и выгружаемые файлы совпадают.
     */
    @Test
    public void testCorrectSeparationOfModelsInYtAndFiles() throws Exception {
        // arrange
        List<Model> models = Arrays.asList(
            // first group
            Models.M1, Models.SKU1_1, Models.SKU1_2, Models.SKU_MODIF_2,
            Models.MODIF1, Models.MODIF2,
            Models.SKU_MODIF_11, Models.SKU_MODIF_12,
            // second
            Models.M2,
            // third
            Models.M3,
            // fourth
            Models.C1,
            // fifth
            Models.PARTNER1, Models.PARTNER_SKU1_1, Models.PARTNER_SKU1_2,
            Models.PARTNER2,
            Models.PARTNER3, Models.PARTNER_SKU3_1,
            // exp group
            Models.EXP_M1, Models.EXP_SKU1_1,
            Models.EXP_MODIF1, Models.EXP_SKU_MODIF_11,
            // dummy guru
            Models.DUMMY_GURU
        );
        ytWrapper.createModelTable(tablePath, models);

        // act
        extractor.perform("");

        // assert
        assertNoFailedFiles(registry);

        // assert that models in category 1 and category 2 are of correct category
        SplitResult splitInCategory1 = splitInExtractor(1);
        SplitResult splitInCategory2 = splitInExtractor(2);
        assertModelsOfCorrectCategory(splitInCategory1, 1);
        assertModelsOfCorrectCategory(splitInCategory2, 2);

        // assert correct models on files and in yt
        SplitResult splitAllYtResults = splitInYt();
        assertSplitNotEmpty(splitAllYtResults);

        SplitResult splitAllFiles = SplitResult.concat(splitInCategory1, splitInCategory2);
        assertEqualModelsInSplit(splitAllYtResults, splitAllFiles);
    }

    @Test
    public void testFilesToBeExtractedIsCorrect() throws Exception {
        // arrange
        List<Model> models = Arrays.asList(
            // first group
            Models.M1, Models.SKU1_1, Models.SKU1_2, Models.SKU_MODIF_2,
            Models.MODIF1, Models.MODIF2,
            Models.SKU_MODIF_11, Models.SKU_MODIF_12,
            // second
            Models.M2,
            // third
            Models.M3,
            // fourth
            Models.C1,
            // fifth
            Models.PARTNER1, Models.PARTNER_SKU1_1, Models.PARTNER_SKU1_2,
            Models.PARTNER2,
            Models.PARTNER3, Models.PARTNER_SKU3_1,
            // exp group
            Models.EXP_M1, Models.EXP_SKU1_1,
            Models.EXP_MODIF1, Models.EXP_SKU_MODIF_11,
            // dummy guru
            Models.DUMMY_GURU
        );
        ytWrapper.createModelTable(tablePath, models);

        // act
        extractor.perform("");

        // assert
        assertNoFailedFiles(registry);

        File rootDir = registry.getRootDir();
        long expectedNumberOfExtractedFiles = Arrays.stream(rootDir.listFiles())
            .filter(file -> !Objects.equals(file.getName(), "dump_session_id.gz"))
            .count();
        int actualNumberOfExtractedFiles = extractor.filesToBeExtracted();
        Assert.assertEquals(expectedNumberOfExtractedFiles, actualNumberOfExtractedFiles);
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testBrokenModelsAreNotExtracting() throws Exception {
        List<Model> models = Arrays.asList(
            Models.M2,
            // model with nonexisting category
            Models.M2.toBuilder().setId(1001).setCategoryId(1001).build(),
            // modification with missing parent model
            Models.M2.toBuilder().setId(1002).setParentId(100500).build(),
            // sku with missing parent model
            Models.SKU1_1,
            // sku with empty relations
            Models.SKU1_2.toBuilder().clearRelations().build(),
            // modification with skus without parent model id
            Models.MODIF1, Models.SKU_MODIF_11
        );
        ytWrapper.createModelTable(tablePath, models, true);

        // act
        extractor.perform("");

        // assert
        assertNoFailedFiles(registry);

        // assert that models don't contain broken one
        SplitResult splitAllYtResults = splitInYt();
        Assertions.assertThat(splitAllYtResults.getAllModels())
            .containsExactlyInAnyOrder(Models.M2_ENRICHED);
        Assertions.assertThat(splitAllYtResults.getModels())
            .containsExactlyInAnyOrder(Models.M2_ENRICHED);
        Assertions.assertThat(splitAllYtResults.getSkus()).isEmpty();
        Assertions.assertThat(splitAllYtResults.getDeletedModels()).isEmpty();

        // assert equal models on disk and yt
        SplitResult splitInDisk = splitInExtractor(2);
        assertEqualModelsInSplit(splitAllYtResults, splitInDisk);
    }

    @Test
    public void testMovedModelWillBeExtractedCorrectly() throws Exception {
        List<Model> models = Arrays.asList(
            // Старая модель (удаленная)
            Models.M1.toBuilder().setCategoryId(2).addTitles(title("Deleted_M3"))
                .setDeleted(true)
                .setDeletedDate(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DAYS_FROM_DELETION))
                .build(),
            // Корректная модель
            Models.M1,
            // Корректная модификация
            Models.MODIF1,
            // Старая модификация (удаленная)
            Models.MODIF1.toBuilder()
                .setCategoryId(2).addTitles(title("Deleted_MODIF1"))
                .setDeleted(true)
                .setDeletedDate(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DAYS_FROM_DELETION))
                .build(),
            // Еще одна корректная модификация
            Models.MODIF2,
            // dummy guru
            Models.DUMMY_GURU
        );
        ytWrapper.createModelTable(tablePath, models, true);

        // act
        extractor.perform("");

        // assert
        assertNoFailedFiles(registry);

        // assert that models don't contain broken one
        SplitResult splitAllYtResults = splitInYt();

        Assertions.assertThat(splitAllYtResults.getAllModels())
            .containsExactlyInAnyOrder(
                Models.M1_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build(),
                Models.MODIF1_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build(),
                Models.MODIF2_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build(),
                Models.DUMMY_GURU_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build()
            );
        Assertions.assertThat(splitAllYtResults.getModels())
            .containsExactlyInAnyOrder(
                Models.M1_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build(),
                Models.MODIF1_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build(),
                Models.DUMMY_GURU_ENRICHED.toBuilder().setPublishedOnBlueMarket(false).build()
            );
        Assertions.assertThat(splitAllYtResults.getSkus()).isEmpty();
        Assertions.assertThat(splitAllYtResults.getDeletedModels()).isEmpty();

        // assert equal models on disk and yt
        SplitResult splitInDisk = splitInExtractor(1);
        assertEqualModelsInSplit(splitAllYtResults, splitInDisk);
    }

    private void assertModelsOfCorrectCategory(SplitResult splitResult, int categoryId) {
        Assertions.assertThat(splitResult.getAllModels()).allMatch(model -> model.getCategoryId() == categoryId);
        Assertions.assertThat(splitResult.getDeletedModels()).allMatch(model -> model.getCategoryId() == categoryId);
        Assertions.assertThat(splitResult.getSkus()).allMatch(model -> model.getCategoryId() == categoryId);
        Assertions.assertThat(splitResult.getModels()).allMatch(model -> model.getCategoryId() == categoryId);
    }

    private void assertEqualModelsInSplit(SplitResult splitAllYtResults, SplitResult splitAllFiles) {
        Assertions.assertThat(splitAllYtResults.getAllModels())
            .containsExactlyInAnyOrderElementsOf(splitAllFiles.getAllModels());
        Assertions.assertThat(splitAllYtResults.getDeletedModels())
            .containsExactlyInAnyOrderElementsOf(splitAllFiles.getDeletedModels());
        Assertions.assertThat(splitAllYtResults.getSkus())
            .containsExactlyInAnyOrderElementsOf(splitAllFiles.getSkus());
        Assertions.assertThat(splitAllYtResults.getModels())
            .containsExactlyInAnyOrderElementsOf(splitAllFiles.getModels());
    }

    private void assertSplitNotEmpty(SplitResult splitAllYtResults) {
        Assertions.assertThat(splitAllYtResults.getAllModels()).isNotEmpty();
        Assertions.assertThat(splitAllYtResults.getDeletedModels()).isNotEmpty();
        Assertions.assertThat(splitAllYtResults.getSkus()).isNotEmpty();
        Assertions.assertThat(splitAllYtResults.getModels()).isNotEmpty();
    }
}
