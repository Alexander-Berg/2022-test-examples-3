package ru.yandex.direct.core.entity.internalads.service;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.internalads.model.InternalAdPlace;
import ru.yandex.direct.core.entity.internalads.model.InternalAdPlaceInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@ParametersAreNonnullByDefault
public class PlaceServiceTest {
    @Test
    public void buildPlaceInfos() {
        List<InternalAdPlace> places = List.of(
                place(1L, 0L, "root 1"),
                place(2L, 0L, "root 2"),
                place(3L, 2L, "B: subplace 1"),
                place(4L, 2L, "A: subplace 2"),
                place(5L, 3L, "subsubplace 1")
        );

        List<Long> placeIds = List.of(1L, 4L, 5L);

        List<InternalAdPlaceInfo> placeInfos = PlaceService.buildPlaceInfos(places, placeIds);
        assertThat(placeInfos).isEqualTo(List.of(
                placeInfo(1L, "root 1"),
                placeInfo(4L, "root 2 / A: subplace 2"),
                placeInfo(5L, "root 2 / B: subplace 1 / subsubplace 1")
        ));
    }

    @Test
    public void modelToPlaceInfo_root() {
        InternalAdPlace root1 = place(1L, 0L, "root 1");
        List<InternalAdPlace> places = List.of(root1);
        Map<Long, InternalAdPlace> placeById = listToMap(places, InternalAdPlace::getId);
        InternalAdPlaceInfo placeInfo = PlaceService.modelToPlaceInfo(root1, placeById);
        assertThat(placeInfo).isEqualTo(placeInfo(1L, "root 1"));
    }

    @Test
    public void modelToPlaceInfo_leaf() {
        InternalAdPlace leaf = place(5L, 3L, "subsubplace 1");
        List<InternalAdPlace> places = List.of(
                place(2L, 0L, "root 2"),
                place(3L, 2L, "B: subplace 1"),
                leaf
        );
        Map<Long, InternalAdPlace> placeById = listToMap(places, InternalAdPlace::getId);
        InternalAdPlaceInfo placeInfo = PlaceService.modelToPlaceInfo(leaf, placeById);
        assertThat(placeInfo).isEqualTo(placeInfo(5L, "root 2 / B: subplace 1 / subsubplace 1"));
    }

    @Test
    public void modelToPlaceInfo_missingDescription() {
        InternalAdPlace leaf = place(5L, 3L, "subsubplace 1");
        List<InternalAdPlace> places = List.of(
                place(2L, 0L, "root 2"),
                place(3L, 2L, null),
                leaf
        );
        Map<Long, InternalAdPlace> placeById = listToMap(places, InternalAdPlace::getId);
        InternalAdPlaceInfo placeInfo = PlaceService.modelToPlaceInfo(leaf, placeById);
        assertThat(placeInfo).isEqualTo(placeInfo(5L, "root 2 / 3 / subsubplace 1"));
    }

    @Test
    public void modelToPlaceInfo_missingParent() {
        InternalAdPlace leaf = place(5L, 3L, "subsubplace 1");
        List<InternalAdPlace> places = List.of(
                place(3L, 2L, "B: subplace 1"),
                leaf
        );
        Map<Long, InternalAdPlace> placeById = listToMap(places, InternalAdPlace::getId);
        InternalAdPlaceInfo placeInfo = PlaceService.modelToPlaceInfo(leaf, placeById);
        assertThat(placeInfo).isEqualTo(placeInfo(5L, "B: subplace 1 / subsubplace 1"));
    }

    @Test
    public void modelToPlaceInfo_cycle() {
        InternalAdPlace leaf = place(5L, 3L, "subsubplace 1");
        List<InternalAdPlace> places = List.of(
                place(2L, leaf.getId(), "root 2"),
                place(3L, 2L, "B: subplace 1"),
                leaf
        );
        Map<Long, InternalAdPlace> placeById = listToMap(places, InternalAdPlace::getId);
        InternalAdPlaceInfo placeInfo = PlaceService.modelToPlaceInfo(leaf, placeById);
        assertThat(placeInfo).isEqualTo(placeInfo(5L, "root 2 / B: subplace 1 / subsubplace 1"));
    }

    private static InternalAdPlace place(Long id, Long parentId, @Nullable String description) {
        return new InternalAdPlace().withId(id).withParentId(parentId).withDescription(description);
    }

    private static InternalAdPlaceInfo placeInfo(Long id, String fullDescription) {
        return new InternalAdPlaceInfo(id, fullDescription);
    }
}
