package ru.yandex.direct.core.testing.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;

import ru.yandex.direct.core.entity.internalads.model.InternalAdPlace;
import ru.yandex.direct.core.entity.internalads.repository.InternalYaBsClusterChooser;
import ru.yandex.direct.core.entity.internalads.repository.PlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.PlacesYtRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.MODERATED_PLACE_ID;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

public class PlaceRepositoryMockUtils {
    public static final InternalAdPlace PLACE_1 = createPlace(1L, 0L, "корень");
    private static final InternalAdPlace PLACE_2 = createPlace(2L, 1L, "потомок");
    private static final InternalAdPlace PLACE_3 = createPlace(3L, 2L, "подпотомок");
    private static final InternalAdPlace PLACE_4 = createPlace(4L, 0L, "ещё один корень");
    public static final InternalAdPlace MODERATED_PLACE_5 = createPlace(MODERATED_PLACE_ID, 0L, "модерируемый");

    public static PlacesYtRepository createYtRepositoryMock() {
        var list = asList(PLACE_1, PLACE_2, PLACE_3, PLACE_4, MODERATED_PLACE_5);
        return new PlacesYtRepository(mock(YtProvider.class), mock(InternalYaBsClusterChooser.class)) {
            @Override
            public List<InternalAdPlace> getAll() {
                return list;
            }
        };
    }

    public static PlaceRepository createMySqlRepositoryMock() {
        Map<Long, InternalAdPlace> placeMap = new HashMap<>();
        placeMap.put(PLACE_1.getId(), PLACE_1);
        placeMap.put(PLACE_2.getId(), PLACE_2);
        placeMap.put(PLACE_3.getId(), PLACE_3);
        placeMap.put(PLACE_4.getId(), PLACE_4);
        placeMap.put(MODERATED_PLACE_5.getId(), MODERATED_PLACE_5);

        return new PlaceRepository(mock(DslContextProvider.class)) {
            @Override
            public List<InternalAdPlace> getAll() {
                return new ArrayList<>(placeMap.values());
            }

            @Override
            public List<InternalAdPlace> getByPlaceIds(Collection<Long> placeIds) {
                return EntryStream.of(placeMap)
                        .filterKeys(placeIds::contains)
                        .values().toList();
            }

            @Override
            public void addOrUpdate(Collection<InternalAdPlace> records) {
                placeMap.putAll(listToMap(records, InternalAdPlace::getId));
            }
        };
    }

    private static InternalAdPlace createPlace(Long id, Long parentId, String description) {
        return new InternalAdPlace()
                .withId(id)
                .withParentId(parentId)
                .withDescription(description);
    }

}
