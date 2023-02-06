package ru.yandex.direct.core.entity.placements.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.repository.PlacementRepository;
import ru.yandex.direct.core.testing.steps.PlacementSteps;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public class UpdatePlacementsServiceBaseTest {

    protected static final Long PAGE_ID_1 = 4L;
    protected static final Long PAGE_ID_2 = 12L;
    protected static final Long PAGE_ID_3 = 44L;

    protected static final String LOGIN = "yandexlogin";

    @Autowired
    protected PlacementSteps placementSteps;

    @Autowired
    protected UpdatePlacementService updatePlacementService;

    @Autowired
    protected PlacementRepository placementRepository;

    @Before
    public void before() {
        placementSteps.clearPlacements();
    }

    protected void update(List<? extends Placement> placements, Map<Long, Set<Long>> dontDeleteBlocks) {
        updatePlacementService.addOrUpdatePlacementsAndMarkDeletedBlocks(placements, dontDeleteBlocks);
    }

    protected void update(List<? extends Placement> placements) {
        update(placements, emptyMap());
    }

    protected void update(Placement placement, Map<Long, Set<Long>> dontDeleteBlocks) {
        update(singletonList(placement), dontDeleteBlocks);
    }

    protected void update(Placement placement) {
        update(singletonList(placement));
    }

    protected Map<Long, Placement> get(Collection<Long> ids) {
        return placementRepository.getPlacements(ids);
    }

    protected Placement get(Long id) {
        return get(singleton(id)).get(id);
    }
}
