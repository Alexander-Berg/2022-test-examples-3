package ru.yandex.direct.core.testing.steps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.placements.model1.BlockSize;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.testing.repository.TestPlacementRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_OUTDOOR_VIDEO_DURATION;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_OUTDOOR_VIDEO_FORMAT_HEIGHT;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_OUTDOOR_VIDEO_FORMAT_WIDTH;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithDefaultBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithDefaultBlock;
import static ru.yandex.direct.dbschema.ppcdict.tables.OutdoorOperators.OUTDOOR_OPERATORS;
import static ru.yandex.direct.dbschema.ppcdict.tables.PlacementBlocks.PLACEMENT_BLOCKS;
import static ru.yandex.direct.dbschema.ppcdict.tables.Placements.PLACEMENTS;
import static ru.yandex.direct.dbschema.ppcdict.tables.PlacementsOutdoorData.PLACEMENTS_OUTDOOR_DATA;

public class PlacementSteps {

    private final TestPlacementRepository placementRepository;
    private final DslContextProvider dslContextProvider;

    @Autowired
    public PlacementSteps(TestPlacementRepository placementRepository,
                          DslContextProvider dslContextProvider) {
        this.placementRepository = placementRepository;
        this.dslContextProvider = dslContextProvider;
    }

    public void addPlacement(Placement<?> placement) {
        placementRepository.createOrUpdatePlacements(singletonList(placement));
    }

    public void addPlacements(Placement<?>... placement) {
        placementRepository.createOrUpdatePlacements(Arrays.asList(placement));
    }

    public OutdoorPlacement addDefaultOutdoorPlacementWithOneBlock() {
        return addDefaultOutdoorPlacementWithOneBlock(placementRepository.getNextPageId(), 12L);
    }

    public OutdoorPlacement addDefaultOutdoorPlacementWithOneBlock(long pageId, long blockId) {
        OutdoorPlacement placement = outdoorPlacementWithDefaultBlock(pageId, blockId);
        addPlacement(placement);
        return placement;
    }

    public OutdoorPlacement addOutdoorPlacement(long pageId, List<OutdoorBlock> blocks) {
        OutdoorPlacement placement = outdoorPlacementWithBlocks(pageId, blocks);
        addPlacement(placement);
        return placement;
    }

    /**
     * создать OutdoorPlacement с блоками имеющими такое же разрешение как и разрешение креатива по умолчанию
     * и с такой же длительностью как длительность видео креатива по умолчанию
     */
    public OutdoorPlacement addOutdoorPlacementWithCreativeDefaults(long pageId, long... blockIds) {
        BlockSize defaultResolution = new BlockSize(DEFAULT_OUTDOOR_VIDEO_FORMAT_WIDTH,
                DEFAULT_OUTDOOR_VIDEO_FORMAT_HEIGHT);
        List<OutdoorBlock> outdoorBlocks = Arrays.stream(blockIds)
                .mapToObj(blockId -> outdoorBlockWithOneSize(pageId, blockId, defaultResolution,
                        DEFAULT_OUTDOOR_VIDEO_DURATION))
                .collect(toList());
        return addOutdoorPlacement(pageId, outdoorBlocks);
    }

    public IndoorPlacement addDefaultIndoorPlacementWithOneBlock() {
        return addDefaultIndoorPlacementWithOneBlock(placementRepository.getNextPageId(), 13L);
    }

    public IndoorPlacement addDefaultIndoorPlacementWithOneBlock(long pageId, long blockId) {
        IndoorPlacement placement = indoorPlacementWithDefaultBlock(pageId, blockId);
        addPlacement(placement);
        return placement;
    }

    public String getBackwardCompatibleBlocksValue(Long id) {
        return dslContextProvider.ppcdict()
                .select(PLACEMENTS.BLOCKS)
                .from(PLACEMENTS)
                .where(PLACEMENTS.PAGE_ID.eq(id))
                .fetchOne(PLACEMENTS.BLOCKS);
    }

    public Placement findPlacement(Long id) {
        Map<Long, Placement> actualPlacements = placementRepository.getPlacements(singletonList(id));
        return actualPlacements.get(id);
    }

    public void clearPlacements() {
        dslContextProvider.ppcdict()
                .deleteFrom(PLACEMENT_BLOCKS)
                .where(DSL.trueCondition())
                .execute();
        dslContextProvider.ppcdict()
                .deleteFrom(PLACEMENTS)
                .where(DSL.trueCondition())
                .execute();
        dslContextProvider.ppcdict()
                .deleteFrom(PLACEMENTS_OUTDOOR_DATA)
                .where(DSL.trueCondition())
                .execute();
    }

    public void clearOperators() {
        dslContextProvider.ppcdict()
                .deleteFrom(OUTDOOR_OPERATORS)
                .where(DSL.trueCondition())
                .execute();
    }

    public void addOutdoorOperator(String login, String operatorName) {
        dslContextProvider.ppcdict()
                .insertInto(OUTDOOR_OPERATORS)
                .set(OUTDOOR_OPERATORS.LOGIN, login)
                .set(OUTDOOR_OPERATORS.NAME, operatorName)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void addOperatorInternalName(Long pageId, String internalName) {
        dslContextProvider.ppcdict()
                .insertInto(PLACEMENTS_OUTDOOR_DATA)
                .set(PLACEMENTS_OUTDOOR_DATA.PAGE_ID, pageId)
                .set(PLACEMENTS_OUTDOOR_DATA.INTERNAL_NAME, internalName)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public List<String> getOperatorInternalNames(Long pageId) {
        return dslContextProvider.ppcdict()
                .select(PLACEMENTS_OUTDOOR_DATA.INTERNAL_NAME)
                .from(PLACEMENTS_OUTDOOR_DATA)
                .where(PLACEMENTS_OUTDOOR_DATA.PAGE_ID.eq(pageId))
                .fetch(PLACEMENTS_OUTDOOR_DATA.INTERNAL_NAME);
    }
}
