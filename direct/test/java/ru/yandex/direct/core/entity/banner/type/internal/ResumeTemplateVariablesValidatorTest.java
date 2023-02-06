package ru.yandex.direct.core.entity.banner.type.internal;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.internalads.model.InternalTemplateInfo;
import ru.yandex.direct.core.entity.internalads.model.ResourceInfo;
import ru.yandex.direct.core.entity.internalads.model.ResourceType;
import ru.yandex.direct.core.entity.internalads.service.validation.defects.InternalAdDefects;
import ru.yandex.direct.core.service.urlchecker.UrlCheckResult;
import ru.yandex.direct.model.ModelChanges;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class ResumeTemplateVariablesValidatorTest {

    private static final long TEMPLATE_ID = 44L;
    private static final long TEMPLATE_RESOURCE_ID1 = 1001L;
    private static final String REACHABLE_URL = "https://yandex.ru/";
    private static final long URL_RESOURCE_ID1 = 1005L;
    private static final String UNREACHABLE_URL = "unreachable";
    private static final long URL_RESOURCE_ID2 = 1006L;
    private static final long BANNER_ID = 11L;

    private static final InternalTemplateInfo INTERNAL_TEMPLATE_INFO = new InternalTemplateInfo()
            .withTemplateId(TEMPLATE_ID)
            .withResources(List.of(
                    new ResourceInfo()
                            .withId(TEMPLATE_RESOURCE_ID1)
                            .withType(ResourceType.TEXT)
                            .withLabel("this is text")
                            .withValueRestrictions(List.of()),
                    new ResourceInfo()
                            .withId(URL_RESOURCE_ID1)
                            .withType(ResourceType.URL)
                            .withLabel("url1")
                            .withValueRestrictions(List.of()),
                    new ResourceInfo()
                            .withId(URL_RESOURCE_ID2)
                            .withType(ResourceType.URL)
                            .withLabel("url2")
                            .withValueRestrictions(List.of())
            ));

    private static final TemplateVariable TEMPLATE_VARIABLE_1 = new TemplateVariable()
            .withTemplateResourceId(TEMPLATE_RESOURCE_ID1)
            .withInternalValue("text");

    private static final TemplateVariable URL_TEMPLATE_VARIABLE_1 = new TemplateVariable()
            .withTemplateResourceId(URL_RESOURCE_ID1)
            .withInternalValue(REACHABLE_URL);

    private static final TemplateVariable URL_TEMPLATE_VARIABLE_2 = new TemplateVariable()
            .withTemplateResourceId(URL_RESOURCE_ID2)
            .withInternalValue(UNREACHABLE_URL);

    private InternalBanner banner;

    private ModelChanges<BannerWithSystemFields> modelChanges;

    @Mock
    private BannerUrlCheckService bannerUrlCheckService;

    private ResumeTemplateVariablesValidator validator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        banner = new InternalBanner().withBsBannerId(BANNER_ID).withTemplateId(TEMPLATE_ID);
        modelChanges = new ModelChanges<>(BANNER_ID, BannerWithSystemFields.class);

        when(bannerUrlCheckService.isUrlReachable(REACHABLE_URL))
                .thenReturn(new UrlCheckResult(true, null));

        when(bannerUrlCheckService.isUrlReachable(UNREACHABLE_URL))
                .thenReturn(new UrlCheckResult(false, UrlCheckResult.Error.HTTP_ERROR));

        validator = new ResumeTemplateVariablesValidator(banner, INTERNAL_TEMPLATE_INFO, bannerUrlCheckService);
    }

    @Test
    public void correct() {
        banner.withTemplateVariables(List.of(TEMPLATE_VARIABLE_1));

        var result = validator.apply(modelChanges);
        assertThat(result, hasNoErrorsAndWarnings());
    }

    @Test
    public void correctWithUrlResource() {
        banner.withTemplateVariables(List.of(TEMPLATE_VARIABLE_1, URL_TEMPLATE_VARIABLE_1));

        var result = validator.apply(modelChanges);
        assertThat(result, hasNoErrorsAndWarnings());
    }

    @Test
    public void unreachableUrl() {
        banner.withTemplateVariables(List.of(TEMPLATE_VARIABLE_1, URL_TEMPLATE_VARIABLE_1, URL_TEMPLATE_VARIABLE_2));

        var result = validator.apply(modelChanges);
        assertThat(result, hasDefectDefinitionWith(
                validationError(path(), InternalAdDefects.urlUnreachable(UrlCheckResult.Error.HTTP_ERROR.name()))));
    }
}
