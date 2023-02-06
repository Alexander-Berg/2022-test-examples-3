package ru.yandex.direct.grid.processing.service.constant;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService;
import ru.yandex.direct.core.entity.internalads.model.TemplateResource;
import ru.yandex.direct.core.entity.internalads.model.TemplateResourceOption;
import ru.yandex.direct.core.entity.internalads.service.TemplateResourceService;
import ru.yandex.direct.grid.processing.model.constants.GdInternalTemplateResource;
import ru.yandex.direct.grid.processing.model.constants.GdInternalTemplateResourcesContainer;
import ru.yandex.direct.grid.processing.model.constants.GdInternalTemplateResourcesFilter;
import ru.yandex.direct.grid.processing.service.offlinereport.OfflineReportValidationService;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class ConstantDataServiceGetInternalTemplateResourcesTest {
    private static final long TEMPLATE_ID = 1L;

    private static final TemplateResource TEMPLATE_RESOURCE = new TemplateResource()
            .withId(1001L)
            .withTemplateId(TEMPLATE_ID)
            .withTemplateCounterType(2L)
            .withTemplatePartNo(4L)
            .withDescription("aaa")
            .withResourceType(5L)
            .withResourceNo(6L)
            .withOptions(singleton(TemplateResourceOption.BANANA_URL))
            .withPosition(9L);

    private static final GdInternalTemplateResource EXPECTED_TEMPLATE_RESOURCE = new GdInternalTemplateResource()
            .withId(1001L)
            .withTemplateId(TEMPLATE_ID)
            .withDescription("aaa")
            .withResourceType(5L)
            .withOptionsRequired(false)
            .withOptionsBananaUrl(true)
            .withOptionsBananaImage(false)
            .withPosition(9L);

    @SuppressWarnings("unused")
    @Mock
    private AgencyOfflineReportParametersService agencyOfflineReportParametersService;

    @SuppressWarnings("unused")
    @Mock
    private OfflineReportValidationService offlineReportValidationService;

    @Mock
    private TemplateResourceService templateResourceService;

    @InjectMocks
    private ConstantDataService constantDataService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        doReturn(Map.of())
                .when(templateResourceService).getReadonlyByTemplateIds(Set.of());
        doReturn(Map.of(1001L, TEMPLATE_RESOURCE))
                .when(templateResourceService).getReadonlyByTemplateIds(singleton(TEMPLATE_ID));
    }

    // а null в фильтре быть не может, см. GdInternalTemplateResourcesFilter
    @Test
    public void getNoTemplateResources_WhenFilterIsEmpty() {
        List<GdInternalTemplateResource> templateResources =
                constantDataService.getInternalTemplateResources(createInput(Set.of()));

        verify(templateResourceService).getReadonlyByTemplateIds(Set.of());
        assertThat(templateResources).isEmpty();
    }

    @Test
    public void getOneTemplateResource_ByFilter() {
        List<GdInternalTemplateResource> templateResources = constantDataService.getInternalTemplateResources(
                createInput(ImmutableSet.of(TEMPLATE_ID)));

        verify(templateResourceService).getReadonlyByTemplateIds(singleton(TEMPLATE_ID));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateResources).hasSize(1);
            softly.assertThat(templateResources).first().is(
                    matchedBy(beanDiffer(EXPECTED_TEMPLATE_RESOURCE)));
        });
    }

    private GdInternalTemplateResourcesContainer createInput(Set<Long> templateIds) {
        return new GdInternalTemplateResourcesContainer()
                .withFilter(new GdInternalTemplateResourcesFilter().withTemplateIds(templateIds));
    }
}
