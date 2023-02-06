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
import ru.yandex.direct.core.entity.placements.model1.IndoorBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlock;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlockKey;
import ru.yandex.direct.core.entity.placements.repository.PlacementBlockRepository;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;
import static ru.yandex.direct.jobs.placements.EnrichPlacementsRegionJob.LIMIT;

@JobsTest
@ExtendWith(SpringExtension.class)
public class EnrichPlacementsAddressJobTest {

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

    private EnrichPlacementsAddressDetector addressTranslator;

    private EnrichPlacementsAddressJob job;

    @BeforeEach
    public void before() {
        placementSteps.clearPlacements();
        addressTranslator = Mockito.mock(EnrichPlacementsAddressDetector.class);
        job = new EnrichPlacementsAddressJob(ppcPropertiesSupport, dslContextProvider, placementBlockRepository, addressTranslator);

        // Для всех блоков возвращаем результат зависимый от хэш-кода.
        // Так в любом тесте становится известен ожидаемые записанные в базу переводы для любого блока.
        when(addressTranslator.detectAddressesTranslations(any()))
                .thenAnswer(invocation -> {
                    Collection<GeoBlock> blocks = invocation.getArgument(0);
                    return StreamEx.of(blocks)
                            .mapToEntry(PlacementBlockKey::of, block -> ImmutableMap.of(
                                    Language.RU, "Address ru " + PlacementBlockKey.of(block).hashCode(),
                                    Language.EN, "Address en " + PlacementBlockKey.of(block).hashCode()
                            ))
                            .toMap();
                });
    }

    @Test
    public void lastUpdateTimeIsNullAndNoUpdatedBlocks_LastUpdateTimeIsChanged() throws Exception {
        setLastUpdateTime(null);

        job.execute();

        assertLastUpdateTimeUpdated();
    }

    @Test
    public void lastUpdateTimeIsNullAndSeveralCommonBlocksAreUpdated_NoBlocksAreUpdatedAndLastUpdateTimeIsChanged()
            throws Exception {
        setLastUpdateTime(null);

        addCommonBlocks(PAGE_ID_2, asList(now().minusHours(3), now().minusHours(4)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedRecently();

        assertThat(keyToUpdatedBlockMap).isEmpty();
        assertLastUpdateTimeUpdated();
    }

    @Test
    public void lastUpdateTimeIsNullAndSeveralGeoBlocksAreUpdated_BlocksTranslationsAreSetAndLastUpdateTimeIsChanged()
            throws Exception {
        setLastUpdateTime(null);

        addOutdoorBlocks(PAGE_ID_2, asList(now().minusMinutes(1), now().minusHours(4)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedRecently();

        assertThat(keyToUpdatedBlockMap).hasSize(2);
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_1_KEY);
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_2_KEY);
        assertLastUpdateTimeUpdated();
    }

    @Test
    public void noBlocksUpdatedSinceLastTranslation_NoBlocksAreUpdatedAndLastUpdateTimeIsChanged() throws Exception {
        LocalDateTime lastUpdateTime = now().minusHours(1);
        setLastUpdateTime(lastUpdateTime);

        addCommonBlocks(PAGE_ID_1, asList(now().minusHours(1).minusMinutes(1), now().minusHours(7)));
        addOutdoorBlocks(PAGE_ID_2, asList(now().minusHours(1).minusMinutes(2), now().minusHours(8)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedRecently();

        assertThat(keyToUpdatedBlockMap).isEmpty();
        assertLastUpdateTimeUpdated();
    }

    @Test
    public void onlyCommonBlocksUpdatedSinceLastTranslation_NothingUpdatedAndLastUpdateTimeIsChanged()
            throws Exception {
        setLastUpdateTime(now().minusHours(1));

        addCommonBlocks(PAGE_ID_1, asList(
                now().minusMinutes(59),
                now().minusMinutes(1),
                now().minusMinutes(30)));
        addOutdoorBlocks(PAGE_ID_2, asList(
                now().minusHours(1).minusMinutes(1),
                now().minusHours(4)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedRecently();

        assertThat(keyToUpdatedBlockMap).isEmpty();
        assertLastUpdateTimeUpdated();
    }

    @Test
    public void severalGeoAndCommonBlocksAreUpdatedSinceLastTranslation_SetTranslationsToUpdatedBlocks() throws Exception {
        setLastUpdateTime(now().minusHours(1));
        addCommonBlocks(PAGE_ID_1, asList(now().minusHours(2), now().minusMinutes(7)));
        addIndoorBlocks(PAGE_ID_2, asList(
                now().minusHours(1).minusMinutes(2),
                now().minusMinutes(58),
                now().minusMinutes(14)));
        addOutdoorBlocks(PAGE_ID_3, asList(
                now().minusHours(2),
                now().minusMinutes(1)));

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedRecently();

        assertThat(keyToUpdatedBlockMap).hasSize(3);
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_2_KEY);
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_2_BLOCK_3_KEY);
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PAGE_3_BLOCK_2_KEY);
    }

    @Test
    public void updatedGeoBlocksNumberIsEqualToLimit_AllBlocksAreUpdated()
            throws Exception {
        setLastUpdateTime(now().minusHours(1));

        List<OutdoorBlock> blocks = new ArrayList<>();
        for (int i = 0; i < EnrichPlacementsRegionJob.LIMIT; i++) {
            blocks.add(outdoorBlockWithOneSize(PAGE_ID_1, (long) i + 1, LocalDateTime.now()));
        }
        Placement outdoorPlacement = outdoorPlacementWithBlocks(PAGE_ID_1, blocks);
        placementSteps.addPlacement(outdoorPlacement);

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedRecently();

        assertThat(keyToUpdatedBlockMap).hasSize(EnrichPlacementsRegionJob.LIMIT);
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PlacementBlockKey.of(PAGE_ID_1, 1L));
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PlacementBlockKey.of(PAGE_ID_1, (long) LIMIT));
    }

    @Test
    public void updatedGeoBlocksNumberIsGreaterThanLimit_AllBlocksAreUpdated()
            throws Exception {
        setLastUpdateTime(now().minusHours(1));

        List<OutdoorBlock> blocks = new ArrayList<>();
        for (int i = 0; i < EnrichPlacementsRegionJob.LIMIT + 1; i++) {
            blocks.add(outdoorBlockWithOneSize(PAGE_ID_1, (long) i + 1, LocalDateTime.now()));
        }
        Placement outdoorPlacement = outdoorPlacementWithBlocks(PAGE_ID_1, blocks);
        placementSteps.addPlacement(outdoorPlacement);

        job.execute();

        Map<PlacementBlockKey, PlacementBlock> keyToUpdatedBlockMap = getBlocksUpdatedRecently();

        assertThat(keyToUpdatedBlockMap).hasSize(EnrichPlacementsRegionJob.LIMIT + 1);
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PlacementBlockKey.of(PAGE_ID_1, 1L));
        assertBlockAddressTranslationsIsUpdatedCorrectly(keyToUpdatedBlockMap, PlacementBlockKey.of(PAGE_ID_1, (long) LIMIT + 1));
    }

    private void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        PpcProperty<LocalDateTime> lastUpdateTimeProp =
                ppcPropertiesSupport.get(PpcPropertyNames.ENRICH_PLACEMENTS_ADDRESS_LAST_UPDATE_TIME);
        if (lastUpdateTime != null) {
            lastUpdateTimeProp.set(lastUpdateTime);
        } else {
            lastUpdateTimeProp.remove();
        }
    }

    private LocalDateTime getLastUpdateTime() {
        PpcProperty<LocalDateTime> lastUpdateTimeProp =
                ppcPropertiesSupport.get(PpcPropertyNames.ENRICH_PLACEMENTS_ADDRESS_LAST_UPDATE_TIME);
        return lastUpdateTimeProp.get();
    }

    private void addCommonBlocks(Long pageId, List<LocalDateTime> updateTimes) {
        List<PlacementBlock> blocks = new ArrayList<>();
        for (int i = 0; i < updateTimes.size(); i++) {
            blocks.add(
                    commonBlockWithOneSize(pageId, (long) i + 1, updateTimes.get(i)));
        }
        Placement commonPlacement = commonYandexPlacementWithBlocks(pageId, blocks);
        placementSteps.addPlacement(commonPlacement);
    }

    private void addOutdoorBlocks(Long pageId, List<LocalDateTime> updateTimes) {
        List<OutdoorBlock> blocks = new ArrayList<>();
        for (int i = 0; i < updateTimes.size(); i++) {
            blocks.add(outdoorBlockWithOneSize(pageId, (long) i + 1, updateTimes.get(i)));
        }
        Placement placement = outdoorPlacementWithBlocks(pageId, blocks);
        placementSteps.addPlacement(placement);
    }

    private void addIndoorBlocks(Long pageId, List<LocalDateTime> updateTimes) {
        List<IndoorBlock> blocks = new ArrayList<>();
        for (int i = 0; i < updateTimes.size(); i++) {
            blocks.add(indoorBlockWithOneSize(pageId, (long) i + 1, updateTimes.get(i)));
        }
        Placement placement = indoorPlacementWithBlocks(pageId, blocks);
        placementSteps.addPlacement(placement);
    }

    private Map<PlacementBlockKey, PlacementBlock> getBlocksUpdatedRecently() {
        List<PlacementBlockKey> updatedBlockKeys =
                placementBlockRepository.getPlacementBlockKeysUpdatedSince(now().minusSeconds(20));
        List<PlacementBlock> updatedBlocks =
                placementBlockRepository.getPlacementBlocks(updatedBlockKeys);
        return StreamEx.of(updatedBlocks)
                .mapToEntry(PlacementBlockKey::of, identity())
                .toMap();
    }

    private void assertBlockAddressTranslationsIsUpdatedCorrectly(Map<PlacementBlockKey, PlacementBlock> actualBlocks,
                                                                  PlacementBlockKey blockKeyToCheck) {
        GeoBlock<?> actualBlockToCheck = (GeoBlock<?>) actualBlocks.get(blockKeyToCheck);
        assertThat(actualBlockToCheck.getAddressTranslations())
                .isEqualTo(ImmutableMap.of(
                        Language.RU, "Address ru " + PlacementBlockKey.of(actualBlockToCheck).hashCode(),
                        Language.EN, "Address en " + PlacementBlockKey.of(actualBlockToCheck).hashCode()
                ));
    }

    private void assertLastUpdateTimeUpdated() {
        assertThat(getLastUpdateTime())
                .isAfter(LocalDateTime.now().minusMinutes(11))
                .isBefore(LocalDateTime.now().minusMinutes(9));
    }
}
