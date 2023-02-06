package ru.yandex.mbo.tool.jira.MBO14232;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.mbo.tool.dump.AutoCloseableIterator;
import ru.yandex.mbo.tool.dump.ModelDumpReader;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author ayratgdl
 * @date 02.02.18
 */
public class PublishedModelsCounterTest {
    private static final long CATEGORY1 = 101L;
    private static final long CATEGORY2 = 102L;
    private static final long MODEL1 = 201L;
    private static final long MODEL2 = 202L;
    private static final String GURU_TYPE = "GURU";
    private static final String VENDOR_TYPE = "VENDOR";

    @Test
    public void emptyDumps() {
        DumpReaderMock dumpReader1 = new DumpReaderMock();
        DumpReaderMock dumpReader2 = new DumpReaderMock();

        Statistics expectedResult = new Statistics();

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void appearsPublishedModel() {
        ModelDumpReader dumpReader1 = new DumpReaderMock();
        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, true));

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP2, Statistics.Property.PUBLISHED, GURU_TYPE, 1)
            .setCount(Statistics.Instant.NEW, Statistics.Property.PUBLISHED, GURU_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void appearsUnpublishedModel() {
        ModelDumpReader dumpReader1 = new DumpReaderMock();
        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, false));

        Statistics expectedResult = new Statistics();

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void publishModel() {
        ModelDumpReader dumpReader1 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, false));
        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, true));

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP2, Statistics.Property.PUBLISHED, GURU_TYPE, 1)
            .setCount(Statistics.Instant.NEW, Statistics.Property.PUBLISHED, GURU_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void disappearsPublishedModel() {
        ModelDumpReader dumpReader1 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, true));
        ModelDumpReader dumpReader2 = new DumpReaderMock();

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP1, Statistics.Property.PUBLISHED, GURU_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void unpublishModel() {
        ModelDumpReader dumpReader1 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, true));

        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, false));

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP1, Statistics.Property.PUBLISHED, GURU_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void withoutChanges() {
        ModelDumpReader dumpReader1 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, true));

        ModelDumpReader dumpReader2 = dumpReader1;

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP1, Statistics.Property.PUBLISHED, GURU_TYPE, 1)
            .setCount(Statistics.Instant.DUMP2, Statistics.Property.PUBLISHED, GURU_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void publishAndUnpublishModels() {
        ModelDumpReader dumpReader1 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, false))
            .addModel(CATEGORY1, buildGuruModel(MODEL2, GURU_TYPE, true));

        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, true))
            .addModel(CATEGORY1, buildGuruModel(MODEL2, GURU_TYPE, false));

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP1, Statistics.Property.PUBLISHED, GURU_TYPE, 1)
            .setCount(Statistics.Instant.DUMP2, Statistics.Property.PUBLISHED, GURU_TYPE, 1)
            .setCount(Statistics.Instant.NEW, Statistics.Property.PUBLISHED, GURU_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void changeCategory() {
        ModelDumpReader dumpReader1 = new DumpReaderMock()
            .addModel(CATEGORY1, buildGuruModel(MODEL1, GURU_TYPE, true));

        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY2, buildGuruModel(MODEL1, GURU_TYPE, true));

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP1, Statistics.Property.PUBLISHED, GURU_TYPE, 1)
            .setCount(Statistics.Instant.DUMP2, Statistics.Property.PUBLISHED, GURU_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void publishNonGuruModel() {
        ModelDumpReader dumpReader1 = new DumpReaderMock();
        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, buildModel(MODEL1, VENDOR_TYPE, VENDOR_TYPE, true));

        Statistics expectedResult = new Statistics();

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void skipModifications() {
        ModelStorage.Model modification = ModelStorage.Model.newBuilder()
            .setId(MODEL2)
            .setParentId(MODEL1)
            .setCurrentType(GURU_TYPE)
            .setSourceType(GURU_TYPE)
            .setPublished(true)
            .build();

        ModelDumpReader dumpReader1 = new DumpReaderMock();
        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, modification);

        Statistics expectedResult = new Statistics();

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void publishModelsWithDifferentSourceTypes() {
        ModelDumpReader dumpReader1 = new DumpReaderMock();
        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, buildModel(MODEL1, GURU_TYPE, GURU_TYPE, true))
            .addModel(CATEGORY1, buildModel(MODEL2, VENDOR_TYPE, GURU_TYPE, true));

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP2, Statistics.Property.PUBLISHED, GURU_TYPE, 1)
            .setCount(Statistics.Instant.DUMP2, Statistics.Property.PUBLISHED, VENDOR_TYPE, 1)
            .setCount(Statistics.Instant.NEW, Statistics.Property.PUBLISHED, GURU_TYPE, 1)
            .setCount(Statistics.Instant.NEW, Statistics.Property.PUBLISHED, VENDOR_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void publishOnMarket() {
        ModelDumpReader dumpReader1 = new DumpReaderMock();

        ModelStorage.Model publishedOnMarketModel = ModelStorage.Model.newBuilder()
            .setId(MODEL1)
            .setCurrentType(GURU_TYPE)
            .setSourceType(GURU_TYPE)
            .setPublishedOnMarket(true)
            .build();
        ModelDumpReader dumpReader2 = new DumpReaderMock()
            .addModel(CATEGORY1, publishedOnMarketModel);

        Statistics expectedResult = new Statistics()
            .setCount(Statistics.Instant.DUMP2, Statistics.Property.PUBLISHED_ON_MARKET, GURU_TYPE, 1)
            .setCount(Statistics.Instant.NEW, Statistics.Property.PUBLISHED_ON_MARKET, GURU_TYPE, 1);

        Statistics actualResult = new PublishedModelsCounter(dumpReader1, dumpReader2).count();
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test(expected = Exception.class)
    public void catchExceptionFromDumpReader() {
        ModelDumpReader dumpReader1 = new DumpReaderMock();
        ModelDumpReader dumpReader2 = new ModelDumpReader() {
            @Override
            public String getSessionId() {
                return "session-id";
            }

            @Override
            public List<Long> readAllCategoryIds() {
                return Arrays.asList(CATEGORY1);
            }

            @Override
            public boolean containsCategory(Long categoryId) {
                return Objects.equals(categoryId, CATEGORY1);
            }

            @Override
            public AutoCloseableIterator<ModelStorage.Model> getModelsIterator(Long categoryId) {
                return new AutoCloseableIterator<ModelStorage.Model>() {
                    @Override
                    public void close() throws Exception {
                    }

                    @Override
                    public boolean hasNext() {
                        throw new RuntimeException();
                    }

                    @Override
                    public ModelStorage.Model next() {
                        throw new RuntimeException();
                    }
                };
            }
        };

        new PublishedModelsCounter(dumpReader1, dumpReader2).count();
    }

    private static ModelStorage.Model buildModel(long modelId, String sourceType, String currentType,
                                                 boolean published) {
        return ModelStorage.Model.newBuilder()
            .setId(modelId)
            .setCurrentType(currentType)
            .setSourceType(sourceType)
            .setPublished(published)
            .build();
    }

    private static ModelStorage.Model buildGuruModel(long modelId, String sourceType, boolean published) {
        return buildModel(modelId, sourceType, GURU_TYPE, published);
    }
}
