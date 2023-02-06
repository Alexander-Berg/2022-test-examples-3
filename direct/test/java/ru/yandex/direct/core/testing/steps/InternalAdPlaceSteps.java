package ru.yandex.direct.core.testing.steps;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.internalads.model.InternalAdPlace;
import ru.yandex.direct.core.entity.internalads.repository.DirectTemplatePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.PlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplatePlaceRepository;

import static ru.yandex.direct.utils.CollectionUtils.flatToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class InternalAdPlaceSteps {
    private final TemplatePlaceRepository templatePlaceRepository;
    private final DirectTemplatePlaceRepository directTemplatePlaceRepository;
    private final PlaceRepository placeRepository;

    public InternalAdPlaceSteps(TemplatePlaceRepository templatePlaceRepository,
            DirectTemplatePlaceRepository directTemplatePlaceRepository,
            PlaceRepository placeRepository) {
        this.templatePlaceRepository = templatePlaceRepository;
        this.directTemplatePlaceRepository = directTemplatePlaceRepository;
        this.placeRepository = placeRepository;
    }

    public void ensurePlacesArePresent() {
        var allTemplatePlaces = flatToSet(
                List.of(templatePlaceRepository.getPlaces(), directTemplatePlaceRepository.getPlaces())
        );
        placeRepository.addOrUpdate(mapList(allTemplatePlaces, templatePlaceId -> new InternalAdPlace()
                .withId(templatePlaceId)
                .withDescription("place " + templatePlaceId)
                .withParentId(0L)));
    }
}
