package ru.yandex.direct.web.entity.placements.service;

import java.util.Arrays;
import java.util.List;

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
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.placement.model.PlacementsResponse;
import ru.yandex.direct.web.core.entity.placement.service.PlacementsService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.gradusnikPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithDefaultBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithDefaultBlock;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PlacementsServiceTest {

    @Autowired
    private PlacementsService placementsService;
    @Autowired
    private OutdoorOperatorRepository outdoorOperatorRepository;

    @Autowired
    private Steps steps;

    private IndoorPlacement indoorPlacement;
    private OutdoorPlacement outdoorPlacement;
    private ClientId clientId;

    @Before
    public void before() {
        steps.placementSteps().clearPlacements();
        steps.placementSteps().clearOperators();

        for (int i = 0; i < 4; ++i) {
            indoorPlacement = indoorPlacementWithDefaultBlock(108L + i, 113L + i);
            steps.placementSteps().addPlacement(indoorPlacement);
        }

        for (int i = 0; i < 6; ++i) {
            outdoorPlacement = outdoorPlacementWithDefaultBlock(208L + i, 213L + i);
            addOutdoorPlacements(outdoorPlacement);
        }

        steps.placementSteps().addPlacement(gradusnikPlacement(301L));
        steps.placementSteps().addPlacement(emptyPlacement("kudagradusnik.ru", 302L));
        steps.placementSteps().addPlacement(emptyPlacement("gradusplus.com", 303L));
        steps.placementSteps().addPlacement(emptyPlacement("gradusy.com", 304L));
        steps.placementSteps().addPlacement(emptyPlacement("gradusdel.com", 305L, true));

        clientId = steps.clientSteps().createDefaultClient().getClientId();
    }

    private void addOutdoorPlacements(OutdoorPlacement... placements) {
        Arrays.stream(placements).forEach(placement -> {
            steps.placementSteps().addPlacement(placement);

            OutdoorOperator operator = new OutdoorOperator().withLogin(placement.getLogin())
                    .withName("name_of_operator_" + randomAlphanumeric(5));
            outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));
        });
    }

    @Test
    public void getPlacements_ByType() {
        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(6, placementsResponse.getPlacementBlocks().size());
        assertEquals(6, placementsResponse.getPlacements().size());

        assertThat(placementsResponse.getPlacements().get(0).getOperatorName()).startsWith("name_of_operator");
    }

    @Test
    public void getPlacements_ByType_AllValidRegionsPresents() {
        steps.placementSteps().clearPlacements();

        OutdoorPlacement placement1 = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithOneSize(12L, 1L, Region.MOSCOW_REGION_ID),
                        outdoorBlockWithOneSize(12L, 2L, Region.MOSCOW_REGION_ID)));
        OutdoorPlacement placement2 = outdoorPlacementWithBlocks(18L,
                singletonList(outdoorBlockWithOneSize(18L, 1L, Region.SAINT_PETERSBURG_REGION_ID)));

        addOutdoorPlacements(placement1, placement2);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(3, placementsResponse.getPlacementBlocks().size());
        assertEquals(2, placementsResponse.getPlacements().size());

        assertThat(placementsResponse.getRegionDictionary())
                .containsOnlyKeys(Region.MOSCOW_REGION_ID, Region.SAINT_PETERSBURG_REGION_ID);
    }

    @Test
    public void getPlacements_ByTypeAndGeoId_AllValidRegionsPresents() {
        steps.placementSteps().clearPlacements();

        OutdoorPlacement placement1 = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithOneSize(12L, 1L, Region.MOSCOW_REGION_ID),
                        outdoorBlockWithOneSize(12L, 2L, Region.MOSCOW_REGION_ID)));
        OutdoorPlacement placement2 = outdoorPlacementWithBlocks(18L,
                singletonList(outdoorBlockWithOneSize(18L, 1L, Region.SAINT_PETERSBURG_REGION_ID)));

        addOutdoorPlacements(placement1, placement2);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        placementsFilter.setGeoId(Region.MOSCOW_REGION_ID);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(2, placementsResponse.getPlacementBlocks().size());
        assertEquals(1, placementsResponse.getPlacements().size());

        assertThat(placementsResponse.getRegionDictionary())
                .containsOnlyKeys(Region.MOSCOW_REGION_ID);
    }

    @Test
    public void getPlacements_DontFailOnUnknownRegion() {
        steps.placementSteps().clearPlacements();

        OutdoorPlacement placement1 = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithOneSize(12L, 1L, Region.MOSCOW_REGION_ID),
                        outdoorBlockWithOneSize(12L, 2L, Region.MOSCOW_REGION_ID)));
        OutdoorPlacement placement2 = outdoorPlacementWithBlocks(18L,
                singletonList(outdoorBlockWithOneSize(18L, 1L)));

        addOutdoorPlacements(placement1, placement2);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(3, placementsResponse.getPlacementBlocks().size());
        assertEquals(2, placementsResponse.getPlacements().size());

        assertThat(placementsResponse.getRegionDictionary())
                .containsOnlyKeys(Region.MOSCOW_REGION_ID);
    }

    @Test
    public void getPlacements_DontFailOnNullRegions() {
        steps.placementSteps().clearPlacements();

        OutdoorPlacement placement1 = outdoorPlacementWithBlocks(12L,
                asList(outdoorBlockWithOneSize(12L, 1L, (Long) null),
                        outdoorBlockWithOneSize(12L, 2L, Region.MOSCOW_REGION_ID)));
        OutdoorPlacement placement2 = outdoorPlacementWithBlocks(18L,
                singletonList(outdoorBlockWithOneSize(18L, 1L, 123456789L)));
        OutdoorPlacement placement3 = outdoorPlacementWithBlocks(20L,
                singletonList(outdoorBlockWithOneSize(20L, 1L, (Long) null)));

        addOutdoorPlacements(placement1, placement2, placement3);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(4, placementsResponse.getPlacementBlocks().size());
        assertEquals(3, placementsResponse.getPlacements().size());

        assertThat(placementsResponse.getRegionDictionary())
                .containsOnlyKeys(Region.MOSCOW_REGION_ID);
    }

    @Test
    public void getPlacements_ByPlaceId() {
        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        placementsFilter.setPageIds(asList(108L, 109L));
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(2, placementsResponse.getPlacementBlocks().size());
        assertEquals(2, placementsResponse.getPlacements().size());
    }

    @Test
    public void getPlacements_ByPlaceIdWithWrongType() {
        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        placementsFilter.setPageIds(singletonList(outdoorPlacement.getId()));
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(0, placementsResponse.getPlacementBlocks().size());
        assertEquals(0, placementsResponse.getPlacements().size());
    }

    @Test
    public void getPlacements_ByTypeAndFacilityType() {
        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        placementsFilter.setFacilityType(1);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(4, placementsResponse.getPlacementBlocks().size());
        assertEquals(4, placementsResponse.getPlacements().size());

        placementsFilter.setFacilityType(100);
        placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(0, placementsResponse.getPlacementBlocks().size());
        assertEquals(0, placementsResponse.getPlacements().size());

    }

    @Test
    public void getPlacements_ByTypeAndGeoId() {
        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.INDOOR);
        placementsFilter.setGeoId(MOSCOW_REGION_ID);
        PlacementsResponse placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(4, placementsResponse.getPlacementBlocks().size());
        assertEquals(4, placementsResponse.getPlacements().size());

        placementsFilter.setGeoId(SAINT_PETERSBURG_REGION_ID);
        placementsResponse = placementsService.getPlacements(placementsFilter, clientId);
        assertEquals(0, placementsResponse.getPlacementBlocks().size());
        assertEquals(0, placementsResponse.getPlacements().size());

    }

    @Test
    public void findPlacements() {
        var placements = placementsService.findPlacements("gradus", clientId);
        assertThat(placements).isNotEmpty();
        assertThat(placements.size()).isEqualTo(3);//kudagradusnik нет в выборке
        var gradusnik = placements.get(0);
        assertThat(gradusnik.getDomain()).isEqualTo("gradusnik.ru");
        assertThat(placements.get(1).getDomain()).isEqualTo("gradusplus.com");
        assertThat(placements.get(2).getDomain()).isEqualTo("gradusy.com");

        assertThat(gradusnik.getMirrors()).hasSize(2);
        assertThat(gradusnik.getMirrors()).containsExactlyInAnyOrder("mirror_" + gradusnik.getId(), "mirror2_" + gradusnik.getId());
    }

    @Test
    public void findPlacementsWWW() {
        var placements = placementsService.findPlacements("www.gradusnik.ru", clientId);
        assertThat(placements).isNotEmpty();
    }

    @Test
    public void getPlacementsByIds() {
        var placements = placementsService.getPlacementsByIds(List.of(301L));
        var gradusnik = placements.get(0);
        assertThat(gradusnik.getDomain()).isEqualTo("gradusnik.ru");
    }
}
