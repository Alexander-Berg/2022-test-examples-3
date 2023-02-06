package ru.yandex.direct.jobs.placements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.placements.model1.GeoBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlock;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlockKey;
import ru.yandex.direct.core.entity.placements.repository.PlacementBlockRepository;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSizeWithoutGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;
import static ru.yandex.direct.jobs.placements.EnrichPlacementsRegionJob.LIMIT;

@JobsTest
@ExtendWith(SpringExtension.class)
class EnrichPlacementsRegionJobTest {

    private static final Long PAGE_ID_1 = 12L;
    private static final Long PAGE_ID_2 = 18L;
    private static final Long PAGE_ID_3 = 34L;

    private static final PlacementBlockKey PAGE_2_BLOCK_1_KEY = PlacementBlockKey.of(PAGE_ID_2, 1L);
    private static final PlacementBlockKey PAGE_2_BLOCK_2_KEY = PlacementBlockKey.of(PAGE_ID_2, 2L);
    private static final PlacementBlockKey PAGE_2_BLOCK_3_KEY = PlacementBlockKey.of(PAGE_ID_2, 3L);
    private static final PlacementBlockKey PAGE_3_BLOCK_2_KEY = PlacementBlockKey.of(PAGE_ID_3, 2L);

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private PlacementSteps placementSteps;

    @Autowired
    private PlacementBlockRepository placementBlockRepository;

    private GeoIdDetector geoIdDetectorMock = Mockito.mock(GeoIdDetector.class);
    private EnrichPlacementsRegionJob job;

    @BeforeEach
    void before() {
        placementSteps.clearPlacements();
        job = new EnrichPlacementsRegionJob(ppcPropertiesSupport, dslContextProvider, placementBlockRepository,
                geoIdDetectorMock);

        // Для всех блоков возвращаем их хэш-код в качестве гео.
        // Так в любом тесте становится известен ожидаемый записанный в базу гео для любого блока,
        // а так же джоба обновляет все запрошенные гео, а значит по изменениям в базе можно понять,
        // правильные ли гео она обновляет.
        when(geoIdDetectorMock.detectGeoIds(any()))
                .thenAnswer(invocation -> {
                    Collection<GeoBlock> inputGeoBlocks = invocation.getArgument(0);
                    return StreamEx.of(inputGeoBlocks)
                            .mapToEntry(PlacementBlockKey::of, b -> (long) PlacementBlockKey.of(b).hashCode())
                            .toMap();
                });
    }

    @Test
    void lastUpdateTimeIsNullAndNoUpdatedBlocks_LastUpdateTimeIsChanged() throws Exception {
        setLastUpdateTime(null);

        job.execute();

        assertLastUpdateTimeUpdated();
    }

    @Test
    void lastUpdateTimeIsNullAndSeveralCommonBlocksAreUpdated_NoBlocksAreUpdatedAndLastUpdateTimeIsChanged()
            throws Exception
    {
        setLastUpdateTime(null);

        addCommonBlocks(PAGE_ID_2, asList(now().minusHours(3), now().minusHours(4)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).isEmpty();
        assertLastUpdateTimeUpdated();
    }

    @Test
    void lastUpdateTimeIsNullAndSeveralGeoBlocksAreUpdated_BlocksGeoAreSetAndLastUpdateTimeIsChanged()
            throws Exception
    {
        setLastUpdateTime(null);

        addOutdoorBlocksWithoutGeo(PAGE_ID_2, asList(now().minusMinutes(1), now().minusHours(4)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).hasSize(2);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_1_KEY);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_2_KEY);
        assertLastUpdateTimeUpdated();
    }

    @Test
    void noBlocksUpdatedSinceLastGeoDetection_NoBlocksAreUpdatedAndLastUpdateTimeIsChanged() throws Exception {
        LocalDateTime lastUpdateTime = now().minusHours(1);
        setLastUpdateTime(lastUpdateTime);

        addCommonBlocks(PAGE_ID_1, asList(now().minusHours(1).minusMinutes(1), now().minusHours(7)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_2, asList(now().minusHours(1).minusMinutes(2), now().minusHours(8)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).isEmpty();
        assertLastUpdateTimeUpdated();
    }

    @Test
    void onlyCommonBlocksUpdatedSinceLastGeoDetection_NoGeosUpdatedAndLastUpdateTimeIsChanged()
            throws Exception
    {
        setLastUpdateTime(now().minusHours(1));

        addCommonBlocks(PAGE_ID_1, asList(
                now().minusMinutes(59),
                now().minusMinutes(1),
                now().minusMinutes(30)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_2, asList(
                now().minusHours(1).minusMinutes(1),
                now().minusHours(4)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).isEmpty();
        assertLastUpdateTimeUpdated();
    }

    @Test
    void severalGeoAndCommonBlocksAreUpdatedSinceLastGeoDetection_SetGeoToUpdatedBlocks() throws Exception {
        setLastUpdateTime(now().minusHours(1));
        addCommonBlocks(PAGE_ID_1, asList(now().minusHours(2), now().minusMinutes(7)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_2, asList(
                now().minusHours(1).minusMinutes(2),
                now().minusMinutes(58),
                now().minusMinutes(14)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_3, asList(
                now().minusHours(2),
                now().minusMinutes(1)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).hasSize(3);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_2_KEY);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_3_KEY);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_3_BLOCK_2_KEY);
    }

    @Test
    void severalUpdatedGeoBlocksHasGeoAndSomeAreNot_OnlyBlocksWithoutGeoAreUpdated() throws Exception {
        setLastUpdateTime(now().minusHours(1));
        addCommonBlocks(PAGE_ID_1, asList(now().minusHours(2), now().minusMinutes(7)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_2, asList(
                now().minusHours(1).minusMinutes(2),
                now().minusMinutes(58),
                now().minusMinutes(14)));
        addOutdoorBlocksWithGeo(PAGE_ID_3, asList(
                now().minusHours(2),
                now().minusMinutes(1)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).hasSize(2);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_2_KEY);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_3_KEY);
    }

    @Test
    void severalUpdatedGeoBlocksHasGeoAndSomeAreNotWithGeoIgnoringEnabled_AllBlocksAreUpdated()
            throws Exception
    {
        setIgnoreGeo();
        setLastUpdateTime(now().minusHours(1));
        addCommonBlocks(PAGE_ID_1, asList(now().minusHours(2), now().minusMinutes(7)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_2, asList(
                now().minusHours(1).minusMinutes(2),
                now().minusMinutes(58),
                now().minusMinutes(14)));
        addOutdoorBlocksWithGeo(PAGE_ID_3, asList(
                now().minusHours(2),
                now().minusMinutes(1)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).hasSize(3);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_2_KEY);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_3_KEY);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_3_BLOCK_2_KEY);
    }

    @Test
    void oneGeoBlockIsUpdatedButGeoIdIsNotDetected_NoBlocksUpdatedAndLastUpdateTimeIsChanged() throws Exception {
        geoIdDetectorMock = mock(GeoIdDetector.class);
        when(geoIdDetectorMock.detectGeoIds(any())).thenReturn(ImmutableMap.of());
        job = new EnrichPlacementsRegionJob(ppcPropertiesSupport, dslContextProvider, placementBlockRepository,
                geoIdDetectorMock);

        LocalDateTime lastUpdateTime = now().minusHours(1);
        setLastUpdateTime(lastUpdateTime);

        addCommonBlocks(PAGE_ID_1, asList(now().minusHours(5), now().minusMinutes(7)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_2, asList(now().minusHours(5), now().minusMinutes(1)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).isEmpty();
        assertLastUpdateTimeUpdated();
    }

    @Test
    void oneGeoIdIsDetectedAndOneIsNot_OneBlockIsUpdatedAndLastChangeIsChanged() throws Exception {
        geoIdDetectorMock = mock(GeoIdDetector.class);
        when(geoIdDetectorMock.detectGeoIds(any()))
                .thenReturn(ImmutableMap.of(PAGE_3_BLOCK_2_KEY, (long) PAGE_3_BLOCK_2_KEY.hashCode()));
        job = new EnrichPlacementsRegionJob(ppcPropertiesSupport, dslContextProvider, placementBlockRepository,
                geoIdDetectorMock);

        LocalDateTime lastUpdateTime = now().minusHours(1);
        setLastUpdateTime(lastUpdateTime);

        addCommonBlocks(PAGE_ID_1, asList(now().minusHours(5), now().minusMinutes(7)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_2, asList(now().minusHours(7), now().minusMinutes(58)));
        addOutdoorBlocksWithoutGeo(PAGE_ID_3, asList(now().minusHours(8), now().minusMinutes(1)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).hasSize(1);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_3_BLOCK_2_KEY);
        assertLastUpdateTimeUpdated();
    }

    @Test
    void updatedGeoBlocksNumberIsEqualToLimit_AllBlocksAreUpdated()
            throws Exception
    {
        setLastUpdateTime(now().minusHours(1));

        List<OutdoorBlock> blocks = new ArrayList<>();
        for (int i = 0; i < EnrichPlacementsRegionJob.LIMIT; i++) {
            blocks.add(outdoorBlockWithOneSizeWithoutGeo(PAGE_ID_1, (long) i + 1, LocalDateTime.now()));
        }
        Placement outdoorPlacement = outdoorPlacementWithBlocks(PAGE_ID_1, blocks);
        placementSteps.addPlacement(outdoorPlacement);

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).hasSize(EnrichPlacementsRegionJob.LIMIT);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PlacementBlockKey.of(PAGE_ID_1, 1L));
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PlacementBlockKey.of(PAGE_ID_1, (long) LIMIT));
    }

    @Test
    void updatedGeoBlocksNumberIsGreaterThanLimit_AllBlocksAreUpdated()
            throws Exception
    {
        setLastUpdateTime(now().minusHours(1));

        List<OutdoorBlock> blocks = new ArrayList<>();
        for (int i = 0; i < EnrichPlacementsRegionJob.LIMIT + 1; i++) {
            blocks.add(outdoorBlockWithOneSizeWithoutGeo(PAGE_ID_1, (long) i + 1, LocalDateTime.now()));
        }
        Placement outdoorPlacement = outdoorPlacementWithBlocks(PAGE_ID_1, blocks);
        placementSteps.addPlacement(outdoorPlacement);

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedSince(now().minusSeconds(20));

        assertThat(keyToUpdatedBlockMap).hasSize(EnrichPlacementsRegionJob.LIMIT + 1);
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PlacementBlockKey.of(PAGE_ID_1, 1L));
        assertBlockGeoIsUpdatedCorrectly(keyToUpdatedBlockMap, PlacementBlockKey.of(PAGE_ID_1, (long) LIMIT + 1));
    }

    private void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        PpcProperty<LocalDateTime> lastUpdateTimeProp =
                ppcPropertiesSupport.get(PpcPropertyNames.ENRICH_PLACEMENTS_GEO_LAST_UPDATE_TIME);
        if (lastUpdateTime != null) {
            lastUpdateTimeProp.set(lastUpdateTime);
        } else {
            lastUpdateTimeProp.remove();
        }
    }

    private void setIgnoreGeo() {
        PpcProperty<Boolean> ignoreGeoProp =
                ppcPropertiesSupport.get(PpcPropertyNames.ENRICH_PLACEMENTS_GEO_IGNORE_OLD_GEO);
        ignoreGeoProp.set(true);
    }

    private LocalDateTime getLastUpdateTime() {
        PpcProperty<LocalDateTime> lastUpdateTimeProp =
                ppcPropertiesSupport.get(PpcPropertyNames.ENRICH_PLACEMENTS_GEO_LAST_UPDATE_TIME);
        return lastUpdateTimeProp.get();
    }

    private void addCommonBlocks(Long pageId, List<LocalDateTime> updateTimes) {
        List<PlacementBlock> blocks = new ArrayList<>();
        for (int i = 0; i < updateTimes.size(); i++) {
            blocks.add(commonBlockWithOneSize(pageId, (long) i + 1, updateTimes.get(i)));
        }
        Placement commonPlacement = commonYandexPlacementWithBlocks(pageId, blocks);
        placementSteps.addPlacement(commonPlacement);
    }

    private void addOutdoorBlocksWithoutGeo(Long pageId, List<LocalDateTime> updateTimes) {
        List<OutdoorBlock> blocks = new ArrayList<>();
        for (int i = 0; i < updateTimes.size(); i++) {
            blocks.add(outdoorBlockWithOneSizeWithoutGeo(pageId, (long) i + 1, updateTimes.get(i)));
        }
        Placement commonPlacement = outdoorPlacementWithBlocks(pageId, blocks);
        placementSteps.addPlacement(commonPlacement);
    }

    private void addOutdoorBlocksWithGeo(Long pageId, List<LocalDateTime> updateTimes) {
        List<OutdoorBlock> blocks = new ArrayList<>();
        for (int i = 0; i < updateTimes.size(); i++) {
            blocks.add(outdoorBlockWithOneSize(pageId, (long) i + 1, updateTimes.get(i)));
        }
        Placement commonPlacement = outdoorPlacementWithBlocks(pageId, blocks);
        placementSteps.addPlacement(commonPlacement);
    }

    private Map<PlacementBlockKey, PlacementBlock> getBlocksUpdatedSince(LocalDateTime lastChange) {
        List<PlacementBlockKey> updatedBlockKeys =
                placementBlockRepository.getPlacementBlockKeysUpdatedSince(lastChange);
        List<PlacementBlock> updatedBlocks =
                placementBlockRepository.getPlacementBlocks(updatedBlockKeys);
        return StreamEx.of(updatedBlocks)
                .mapToEntry(PlacementBlockKey::of, identity())
                .toMap();
    }

    private void assertBlockGeoIsUpdatedCorrectly(Map<PlacementBlockKey, PlacementBlock> actualBlocks,
            PlacementBlockKey blockKeyToCheck)
    {
        assertThat(actualBlocks.get(blockKeyToCheck))
                .hasFieldOrPropertyWithValue("geoId", (long) blockKeyToCheck.hashCode());
    }

    private void assertLastUpdateTimeUpdated() {
        assertThat(getLastUpdateTime())
                .isAfter(LocalDateTime.now().minusMinutes(11))
                .isBefore(LocalDateTime.now().minusMinutes(9));
    }
}
