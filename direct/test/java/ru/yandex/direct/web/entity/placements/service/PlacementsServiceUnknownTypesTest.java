package ru.yandex.direct.web.entity.placements.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.outdoor.model.OutdoorOperator;
import ru.yandex.direct.core.entity.outdoor.repository.OutdoorOperatorRepository;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.PlacementType;
import ru.yandex.direct.core.entity.placements.model1.PlacementsFilter;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.placement.model.PlacementsResponse;
import ru.yandex.direct.web.core.entity.placement.service.PlacementsService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithFacilityType;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithZoneCategory;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithFacilityType;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithDefaultBlock;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PlacementsServiceUnknownTypesTest {

    @Autowired
    private PlacementsService placementsService;

    @Autowired
    private OutdoorOperatorRepository outdoorOperatorRepository;

    @Autowired
    private Steps steps;

    private ClientId clientId;

    @Before
    public void before() {
        clientId = steps.clientSteps().createDefaultClient().getClientId();

        steps.placementSteps().clearPlacements();
        steps.placementSteps().clearOperators();
    }

    @Test
    public void getPlacements_OutdoorContainsNullFacilityType_FiltersIt() {
        OutdoorPlacement placement = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithFacilityType(12L, 1L, null),
                        outdoorBlockWithOneSize(12L, 2L)));
        steps.placementSteps().addPlacement(placement);

        OutdoorOperator operator = new OutdoorOperator().withLogin(placement.getLogin())
                .withName("name_of_operator");
        outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(1, placementsResponse.getPlacementBlocks().size());
    }

    @Test
    public void getPlacements_OutdoorContainsUnknownFacilityType_FiltersIt() {
        OutdoorPlacement placement = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithFacilityType(12L, 1L, 12345),
                        outdoorBlockWithOneSize(12L, 2L)));
        steps.placementSteps().addPlacement(placement);

        OutdoorOperator operator = new OutdoorOperator().withLogin(placement.getLogin())
                .withName("name_of_operator");
        outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(1, placementsResponse.getPlacementBlocks().size());
    }

    @Test
    public void getPlacements_OutdoorContainsUnknownOperator_FiltersIt() {
        OutdoorPlacement placement1 = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithFacilityType(12L, 1L, 1),
                        outdoorBlockWithOneSize(12L, 2L)));
        OutdoorPlacement placement2 = outdoorPlacementWithDefaultBlock(18L, 1L);
        steps.placementSteps().addPlacement(placement1);
        steps.placementSteps().addPlacement(placement2);

        OutdoorOperator operator = new OutdoorOperator().withLogin(placement1.getLogin())
                .withName("name_of_operator");
        outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(2, placementsResponse.getPlacementBlocks().size());
    }

    @Test
    public void getPlacements_OutdoorContainsUnknownOperatorAndUnknownFacilityType_FiltersIt() {
        OutdoorPlacement placement1 = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithFacilityType(12L, 1L, 12345),
                        outdoorBlockWithOneSize(12L, 2L)));
        OutdoorPlacement placement2 = outdoorPlacementWithDefaultBlock(18L, 1L);
        steps.placementSteps().addPlacement(placement1);
        steps.placementSteps().addPlacement(placement2);

        OutdoorOperator operator = new OutdoorOperator().withLogin(placement1.getLogin())
                .withName("name_of_operator");
        outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(1, placementsResponse.getPlacementBlocks().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPlacements_OnlyPlacementsWithoutOperatorFound_FiltersIt() {
        OutdoorPlacement placement1 = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithFacilityType(12L, 1L, 1),
                        outdoorBlockWithOneSize(12L, 2L)));
        OutdoorPlacement placement2 = outdoorPlacementWithDefaultBlock(18L, 1L);
        steps.placementSteps().addPlacement(placement1);
        steps.placementSteps().addPlacement(placement2);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertThat(placementsResponse.getPlacementBlocks()).isEmpty();
    }

    @Test
    public void getPlacements_IndoorContainsNullFacilityType_FiltersIt() {
        IndoorPlacement placement = indoorPlacementWithBlocks(12L,
                asList(indoorBlockWithFacilityType(12L, 1L, null),
                        indoorBlockWithOneSize(12L, 2L)));
        steps.placementSteps().addPlacement(placement);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(1, placementsResponse.getPlacementBlocks().size());
    }

    @Test
    public void getPlacements_IndoorContainsUnknownFacilityType_FiltersIt() {
        IndoorPlacement placement = indoorPlacementWithBlocks(12L,
                asList(indoorBlockWithFacilityType(12L, 1L, 12345),
                        indoorBlockWithOneSize(12L, 2L)));

        steps.placementSteps().addPlacement(placement);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(1, placementsResponse.getPlacementBlocks().size());
    }

    @Test
    public void getPlacements_IndoorContainsNullZoneCategory_FiltersIt() {
        IndoorPlacement placement = indoorPlacementWithBlocks(12L,
                asList(indoorBlockWithZoneCategory(12L, 1L, null),
                        indoorBlockWithOneSize(12L, 2L)));

        steps.placementSteps().addPlacement(placement);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(1, placementsResponse.getPlacementBlocks().size());
    }

    @Test
    public void getPlacements_IndoorContainsUnknownZoneCategory_FiltersIt() {
        IndoorPlacement placement = indoorPlacementWithBlocks(12L,
                asList(indoorBlockWithZoneCategory(12L, 1L, 12345),
                        indoorBlockWithOneSize(12L, 2L)));

        steps.placementSteps().addPlacement(placement);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(1, placementsResponse.getPlacementBlocks().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPlacements_FiltrationDoesNotOnEmptyResultSet() {
        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertThat(placementsResponse.getPlacementBlocks()).isEmpty();
        assertThat(placementsResponse.getPlacements()).isEmpty();
    }
}
