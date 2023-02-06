package ru.yandex.direct.core.entity.internalads.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.internalads.model.TemplatePlace;
import ru.yandex.direct.core.entity.internalads.repository.DirectTemplatePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplatePlaceRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class TemplatePlaceServiceTest {
    private static final TemplatePlaceRepository TEMPLATE_PLACE_REPOSITORY =
            mock(TemplatePlaceRepository.class);
    private static final DirectTemplatePlaceRepository DIRECT_TEMPLATE_PLACE_REPOSITORY =
            mock(DirectTemplatePlaceRepository.class);

    private static final TemplatePlace PLACE_1_TEMPLATE_1 = new TemplatePlace().withPlaceId(1L).withTemplateId(1L);
    private static final TemplatePlace PLACE_2_TEMPLATE_1 = new TemplatePlace().withPlaceId(2L).withTemplateId(1L);
    private static final TemplatePlace PLACE_1_TEMPLATE_2 = new TemplatePlace().withPlaceId(1L).withTemplateId(2L);
    private static final TemplatePlace PLACE_3_TEMPLATE_2 = new TemplatePlace().withPlaceId(3L).withTemplateId(2L);

    private final TemplatePlaceService templatePlaceService =
            new TemplatePlaceService(TEMPLATE_PLACE_REPOSITORY, DIRECT_TEMPLATE_PLACE_REPOSITORY);

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public Set<Long> placeIds;

    @Parameterized.Parameter(2)
    public List<TemplatePlace> expected;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"getFirstPlace", Set.of(1L), List.of(PLACE_1_TEMPLATE_1, PLACE_1_TEMPLATE_2)},
                {"getSecondPlace", Set.of(2L), List.of(PLACE_2_TEMPLATE_1)},
                {"getAllPlaces", null, List.of(
                        PLACE_1_TEMPLATE_1, PLACE_1_TEMPLATE_2, PLACE_2_TEMPLATE_1, PLACE_3_TEMPLATE_2)},
        });
    }

    @BeforeClass
    public static void setup() {
        when(TEMPLATE_PLACE_REPOSITORY.getByPlaceIds(Set.of(1L))).thenReturn(List.of(PLACE_1_TEMPLATE_1));
        when(DIRECT_TEMPLATE_PLACE_REPOSITORY.getByPlaceIds(Set.of(1L)))
                .thenReturn(List.of(PLACE_1_TEMPLATE_1, PLACE_1_TEMPLATE_2));
        when(TEMPLATE_PLACE_REPOSITORY.getByPlaceIds(Set.of(2L))).thenReturn(List.of(PLACE_2_TEMPLATE_1));
        when(DIRECT_TEMPLATE_PLACE_REPOSITORY.getByPlaceIds(Set.of(2L)))
                .thenReturn(List.of());
        when(TEMPLATE_PLACE_REPOSITORY.getAll()).thenReturn(List.of(PLACE_2_TEMPLATE_1, PLACE_3_TEMPLATE_2));
        when(DIRECT_TEMPLATE_PLACE_REPOSITORY.getAll())
                .thenReturn(List.of(PLACE_1_TEMPLATE_1, PLACE_1_TEMPLATE_2, PLACE_2_TEMPLATE_1));
    }

    @Test
    public void getPlacesTest() {
        var result = templatePlaceService.getByPlaceIds(placeIds);
        assertThat(result).isEqualTo(expected);
    }
}
