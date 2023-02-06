package ru.yandex.direct.grid.processing.service.constant;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService;
import ru.yandex.direct.core.entity.internalads.model.TemplatePlace;
import ru.yandex.direct.core.entity.internalads.service.TemplatePlaceService;
import ru.yandex.direct.grid.processing.model.constants.GdInternalTemplatePlace;
import ru.yandex.direct.grid.processing.model.constants.GdInternalTemplatePlacesContainer;
import ru.yandex.direct.grid.processing.model.constants.GdInternalTemplatePlacesFilter;
import ru.yandex.direct.grid.processing.service.offlinereport.OfflineReportValidationService;

import static freemarker.template.utility.Collections12.singletonList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class ConstantDataServiceGetInternalTemplatePlacesTest {
    private static final long PLACE_ID2 = 2L;
    private static final long TEMPLATE_ID_FOR2 = 201L;
    private static final TemplatePlace TEMPLATE_PLACE_2 =
            new TemplatePlace().withPlaceId(PLACE_ID2).withTemplateId(TEMPLATE_ID_FOR2);

    private static final ImmutableList<TemplatePlace> TEMPLATE_PLACES = ImmutableList.of(
            new TemplatePlace().withPlaceId(1L).withTemplateId(101L),
            new TemplatePlace().withPlaceId(1L).withTemplateId(102L),
            TEMPLATE_PLACE_2
    );

    @SuppressWarnings("unused")
    @Mock
    private AgencyOfflineReportParametersService agencyOfflineReportParametersService;

    @SuppressWarnings("unused")
    @Mock
    private OfflineReportValidationService offlineReportValidationService;

    @Mock
    private TemplatePlaceService templatePlaceService;

    @InjectMocks
    private ConstantDataService constantDataService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        doReturn(TEMPLATE_PLACES)
                .when(templatePlaceService).getVisibleTemplatesByPlaceIds(null);
        doReturn(singletonList(TEMPLATE_PLACE_2))
                .when(templatePlaceService).getVisibleTemplatesByPlaceIds(singleton(PLACE_ID2));
    }

    @Test
    public void getAllTemplatePlaces_WhenFilterIsEmpty() {
        List<GdInternalTemplatePlace> templatePlaces = constantDataService.getInternalTemplatePlaces(createInput(null));

        verify(templatePlaceService).getVisibleTemplatesByPlaceIds(null);
        assertThat(templatePlaces).hasSameSizeAs(TEMPLATE_PLACES);
    }

    @Test
    public void getOneTemplatePlace_ByFilter() {
        List<GdInternalTemplatePlace> templatePlaces = constantDataService.getInternalTemplatePlaces(
                createInput(ImmutableSet.of(PLACE_ID2)));

        verify(templatePlaceService).getVisibleTemplatesByPlaceIds(singleton(PLACE_ID2));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templatePlaces).hasSize(1);
            softly.assertThat(templatePlaces).first().is(
                    matchedBy(beanDiffer(
                            new GdInternalTemplatePlace().withPlaceId(PLACE_ID2).withTemplateId(TEMPLATE_ID_FOR2))));
        });
    }

    private GdInternalTemplatePlacesContainer createInput(@Nullable Set<Long> placesIds) {
        return new GdInternalTemplatePlacesContainer()
                .withFilter(new GdInternalTemplatePlacesFilter().withPlacesIds(placesIds));
    }
}
