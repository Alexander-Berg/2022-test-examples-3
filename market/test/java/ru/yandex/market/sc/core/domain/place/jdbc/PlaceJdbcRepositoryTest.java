package ru.yandex.market.sc.core.domain.place.jdbc;

import java.time.Clock;
import java.util.Comparator;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceHistoryRepository;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.domain.stage.repository.Stage;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.db.jpa.BaseJpaEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.configuration.ConfigurationProperties.ENABLE_SAVE_PLACE_HISTORY_FOR_PLACE_STAGE_UPDATE;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PlaceJdbcRepositoryTest {
    private final TestFactory testFactory;
    private final PlaceJdbcRepository placeJdbcRepository;
    private final PlaceRepository placeRepository;
    private final PlaceHistoryRepository placeHistoryRepository;
    private SortingCenter sortingCenter;
    private User user;
    @MockBean
    private Clock clock;
    @MockBean
    private ConfigurationService configurationService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(356352L);
        TestFactory.setupMockClock(clock);
        Mockito.when(configurationService.isBooleanEnabled(ENABLE_SAVE_PLACE_HISTORY_FOR_PLACE_STAGE_UPDATE))
                .thenReturn(true);
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.UPDATE_SORTABLE_ROUTES_IN_PLACE, true);
    }

    @Test
    void updateStageTest() {
        var places = testFactory.createForToday(
                order(sortingCenter)
                        .externalId("o1")
                        .places("p1", "p2")
                        .build()
        ).getPlaces().values();

        var stage = StageLoader.getById(Stages.FIRST_ACCEPT_DIRECT.getId());

        placeJdbcRepository.updateStage(places, stage, user);
        var placeIds = places.stream().map(Place::getId).collect(Collectors.toSet());
        places = placeRepository.findAllById(placeIds);

        places.forEach(place -> assertStageAndHistoryUpdated(place, stage));
    }

    private void assertStageAndHistoryUpdated(Place place, Stage stage) {
        assertThat(place.getStageId()).isEqualTo(stage.getId());

        var lastHistoryUpdate = StreamEx.of(placeHistoryRepository.findByPlaceId(place.getId()))
                .reverseSorted(Comparator.comparing(BaseJpaEntity.LongGenAud::getId))
                .findFirst()
                .orElseThrow();

        assertThat(lastHistoryUpdate.getMutableState().getCell()).isEqualTo(place.getCell());
        assertThat(lastHistoryUpdate.getMutableState().getStageId()).isEqualTo(stage.getId());
        assertThat(lastHistoryUpdate.getMutableState().getSortableStatus()).isEqualTo(stage.getSortableStatus());
        assertThat(lastHistoryUpdate.getMutableState().getPlaceStatus()).isEqualTo(place.getPlaceStatus());
        assertThat(lastHistoryUpdate.getMutableState().getLot()).isEqualTo(place.getLot());
        assertThat(lastHistoryUpdate.getUser()).isEqualTo(user);
    }
}
