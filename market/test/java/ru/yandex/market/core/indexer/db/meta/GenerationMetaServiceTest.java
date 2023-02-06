package ru.yandex.market.core.indexer.db.meta;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.indexer.model.GenerationMeta;
import ru.yandex.market.core.indexer.model.GenerationType;
import ru.yandex.market.core.indexer.model.IndexerType;
import ru.yandex.market.core.indexer.model.VirtualFeedLogHistoryNote;

class GenerationMetaServiceTest extends FunctionalTest {
    private static final String YELLOW_META_FILE_NAME = "data/feedlog-meta-with-color.meta";
    private static final String WHITE_META_FILE_NAME = "data/simple-white-feedlog-meta.meta";
    private static final String TEST_META_NAME = "test-name";
    private static final Set<FeedSiteType> UNUSED_TYPES = EnumSet.of(
            FeedSiteType.SITE_PARSING, FeedSiteType.SITE_PREVIEW, FeedSiteType.EXTERNAL_MDS_FILE
    );

    @Autowired
    private GenerationMetaService generationMetaService;

    @Test
    @DbUnitDataSet(after = "data/insertNewTest.after.csv")
    void insertNewTest() throws IOException {
        List<String> metaFilesNames = Arrays.asList(
                YELLOW_META_FILE_NAME,
                WHITE_META_FILE_NAME
        );
        for (String metaFileName : metaFilesNames) {
            InputStream stream = getClass().getResourceAsStream(metaFileName);
            String fileData = IOUtils.toString(stream, StandardCharsets.UTF_8);
            GenerationMeta generationMeta = GenerationMeta.fromFile(fileData, metaFileName);
            generationMetaService.insert(generationMeta);
        }
    }

    @Test
    void insertNotNewTest() throws IOException {
        InputStream stream = getClass().getResourceAsStream(YELLOW_META_FILE_NAME);
        String fileData = IOUtils.toString(stream, StandardCharsets.UTF_8);
        GenerationMeta generationMeta = GenerationMeta.fromFile(fileData, YELLOW_META_FILE_NAME);
        generationMetaService.insert(generationMeta);
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> generationMetaService.insert(generationMeta)
        );
        Assertions.assertEquals(
                String.format("Generation meta %s was loaded more than once.", generationMeta.toString()), ex.getMessage()
        );
    }

    @Test
    @DbUnitDataSet(before = "data/insertNewTest.after.csv")
    void getNotImportedBySiteTypeTest() {
        Set<GenerationMeta> yellowMetas = generationMetaService.getNotImportedBySiteType(FeedSiteType.YELLOW_MARKET);
        Assertions.assertEquals(1, yellowMetas.size());
        Assertions.assertEquals(1, yellowMetas.iterator().next().getId());
        Set<GenerationMeta> whiteMetas = generationMetaService.getNotImportedBySiteType(FeedSiteType.MARKET);
        Assertions.assertEquals(1, whiteMetas.size());
        Assertions.assertEquals(2, whiteMetas.iterator().next().getId());
    }

    @Test
    @DbUnitDataSet(before = "data/getForgetGenerationIdTest.before.csv")
    void getForgetGenerationIdTest() {
        Random rnd = new Random();

        long yellowForgetGenerationId = generationMetaService.getForgetGenerationId(
                new GenerationMeta.Builder()
                        .setId(rnd.nextLong())
                        .setName(TEST_META_NAME)
                        .setFeedType(FeedSiteType.YELLOW_MARKET)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .setReleaseDate(Instant.now())
                        .setMitype(RandomStringUtils.randomAlphabetic(10))
                        .setKey(RandomStringUtils.randomAlphabetic(80))
                        .build()
        );
        Assertions.assertEquals(5, yellowForgetGenerationId);

        long whiteForgetGenerationId = generationMetaService.getForgetGenerationId(
                new GenerationMeta.Builder()
                        .setId(rnd.nextLong())
                        .setName(TEST_META_NAME)
                        .setFeedType(FeedSiteType.MARKET)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.MAIN)
                        .setReleaseDate(Instant.now())
                        .setMitype(RandomStringUtils.randomAlphabetic(10))
                        .setKey(RandomStringUtils.randomAlphabetic(80))
                        .build()
        );
        Assertions.assertEquals(8, whiteForgetGenerationId);

        long whitePlaneshiftForgetGenerationId = generationMetaService.getForgetGenerationId(
                new GenerationMeta.Builder()
                        .setId(rnd.nextLong())
                        .setName(TEST_META_NAME)
                        .setFeedType(FeedSiteType.MARKET)
                        .setGenerationType(GenerationType.FULL)
                        .setIndexerType(IndexerType.PLANESHIFT)
                        .setReleaseDate(Instant.now())
                        .setMitype(RandomStringUtils.randomAlphabetic(10))
                        .setKey(RandomStringUtils.randomAlphabetic(80))
                        .build()
        );
        Assertions.assertEquals(0, whitePlaneshiftForgetGenerationId);
    }

    @Test
    @DbUnitDataSet(before = "data/insertNewTest.after.csv", after = "data/markImportedTest.after.csv")
    void markImportedTest() {
        generationMetaService.markImported(getYellowMeta().getId());
    }

    @Test
    @DbUnitDataSet(before = "data/markImportedTest.after.csv")
    void getAllImportedTest() {
        Set<GenerationMeta> allImported = generationMetaService.getByImported(true);
        MatcherAssert.assertThat(allImported, Matchers.hasSize(1));
        MatcherAssert.assertThat(allImported, Matchers.containsInAnyOrder(getYellowMeta()));
    }

    @Test
    @DbUnitDataSet(before = "data/markImportedTest.after.csv", after = "data/removeTest.after.csv")
    void removeTest() {
        generationMetaService.remove(getYellowMeta().getId());
    }

    @Test
    @DbUnitDataSet(before = "data/insertNewTest.after.csv")
    void getTest() {
        final GenerationMeta whiteMeta = getWhiteMeta();
        final GenerationMeta yellowMeta = getYellowMeta();
        Assertions.assertEquals(whiteMeta, generationMetaService.get(whiteMeta.getId()));
        Assertions.assertEquals(yellowMeta, generationMetaService.get(yellowMeta.getId()));
    }

    @Test
    void getInsertedTest() {
        final GenerationMeta yellowMeta = getYellowMeta();
        final GenerationMeta whiteMeta = getWhiteMeta();
        generationMetaService.insert(yellowMeta);
        generationMetaService.insert(whiteMeta);
        Assertions.assertEquals(yellowMeta, generationMetaService.get(yellowMeta.getId()));
        Assertions.assertEquals(whiteMeta, generationMetaService.get(whiteMeta.getId()));
    }

    @Test
    @DbUnitDataSet(after = "data/VirtualFeedLogTest.csv")
    void insertVirtualFeedLogTest() {
        final VirtualFeedLogHistoryNote virtualFeedLogHistoryNote = getVirtualFeedHistoryNote();
        generationMetaService.insert(virtualFeedLogHistoryNote);
    }

    @Test
    @DbUnitDataSet(before = "data/getLatestVirtualFeedLogTest.before.csv")
    void getLatestVirtualFeedLogTest() {
        final VirtualFeedLogHistoryNote expected = getVirtualFeedHistoryNote();
        final Optional<VirtualFeedLogHistoryNote> actual = generationMetaService.getLatestVirtualFeedLog(
                expected.getFeedId(),
                expected.getSiteType(),
                expected.getReleaseDate(),
                expected.getIndexerType());
        Assertions.assertEquals(expected, actual.get());
    }

    @Test
    @DbUnitDataSet(before = "data/VirtualFeedLogTest.csv")
    void getVirtualFeedLogTest() {
        final VirtualFeedLogHistoryNote expected = getVirtualFeedHistoryNote();
        final Optional<VirtualFeedLogHistoryNote> actual = generationMetaService.getVirtualFeedLog(
                expected.getFeedId(),
                expected.getGenerationName(),
                expected.getClusterName(),
                expected.getSiteType());
        Assertions.assertEquals(expected, actual.get());
    }

    @Test
    @DbUnitDataSet(before = "data/updateVirtualFeedLogHistoryTest.before.csv",
            after = "data/updateVirtualFeedLogHistoryTest.after.csv")
    void updateVirtualFeedLogHistoryTest() {
        final VirtualFeedLogHistoryNote virtualFeedLogHistoryNote = getVirtualFeedHistoryNote();
        generationMetaService.updateFeedLogHistory(virtualFeedLogHistoryNote);
    }

    private GenerationMeta getYellowMeta() {
        return new GenerationMeta.Builder()
                .setId(1L)
                .setName("20190319_2235")
                .setFeedType(FeedSiteType.YELLOW_MARKET)
                .setGenerationType(GenerationType.FULL)
                .setIndexerType(IndexerType.MAIN)
                .setReleaseDate(Instant.ofEpochSecond(1552228302L))
                .setMitype("yellow.gibson")
                .setKey(YELLOW_META_FILE_NAME)
                .build();
    }


    private GenerationMeta getWhiteMeta() {
        return new GenerationMeta.Builder()
                .setId(2L)
                .setName(TEST_META_NAME)
                .setFeedType(FeedSiteType.MARKET)
                .setGenerationType(GenerationType.DELTA)
                .setIndexerType(IndexerType.PLANESHIFT)
                .setReleaseDate(Instant.ofEpochSecond(1499063875))
                .setMitype("planeshift.stratocaster")
                .setKey(WHITE_META_FILE_NAME)
                .build();
    }

    private VirtualFeedLogHistoryNote getVirtualFeedHistoryNote() {
        return new VirtualFeedLogHistoryNote.Builder()
                .setTotalOffers(11)
                .setValidOffers(13)
                .setWarnOffers(17)
                .setErrorOffers(19)
                .setFeedId(5)
                .setClusterName("planeshift")
                .setGenerationName("20070101")
                .setReleaseDate(Instant.parse("2007-01-01T00:00:00Z"))
                .setShopId(7)
                .setSiteType(FeedSiteType.BLUE_MARKET)
                .setIndexerType(IndexerType.PLANESHIFT)
                .build();
    }

    @Test
    @DbUnitDataSet(before = "data/getLastFullGenerationsIdTest.before.csv")
    void getLastFullGenerationsIdTest() {
        List<FeedSiteType> feedSiteTypes = Arrays.stream(FeedSiteType.values())
                .filter(t -> !UNUSED_TYPES.contains(t))
                .collect(Collectors.toList());
        for (FeedSiteType siteType : feedSiteTypes) {
            for (IndexerType indexerType : IndexerType.values()) {
                GenerationMeta meta = generationMetaService.getLastFullGeneration(siteType, indexerType);
                Assertions.assertEquals(indexerType.getId() * 10 + siteType.getId() + 1, meta.getId());
            }
        }
    }
}
