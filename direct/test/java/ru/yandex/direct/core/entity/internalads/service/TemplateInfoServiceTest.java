package ru.yandex.direct.core.entity.internalads.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.internalads.model.AbstractResourceInfo;
import ru.yandex.direct.core.entity.internalads.model.InternalTemplateInfo;
import ru.yandex.direct.core.entity.internalads.model.InternalTemplateOverrides;
import ru.yandex.direct.core.entity.internalads.model.ReadOnlyDirectTemplateResource;
import ru.yandex.direct.core.entity.internalads.model.ResourceChoice;
import ru.yandex.direct.core.entity.internalads.model.ResourceInfo;
import ru.yandex.direct.core.entity.internalads.model.ResourceRestriction;
import ru.yandex.direct.core.entity.internalads.model.ResourceType;
import ru.yandex.direct.core.entity.internalads.model.TemplateResource;
import ru.yandex.direct.core.entity.internalads.restriction.Restrictions;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.utils.FunctionalUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class TemplateInfoServiceTest {

    private static final long UNKNOWN_TEMPLATE_ID = 824L;
    private static final long TEMPLATE_ID = 22L;
    private static final List<String> CHOICES = asList("a", "b");

    private TemplateInfoService templateInfoService;
    private Map<Long, ReadOnlyDirectTemplateResource> templateResources;
    private TemplateInfoOverrides templateInfoOverrides;

    @Before
    public void before() {
        TemplateResourceService templateResourceService = mock(TemplateResourceService.class);

        templateResources = new HashMap<>();
        when(templateResourceService.getReadonlyByTemplateIds(any())).thenReturn(templateResources);

        templateInfoOverrides = mock(TemplateInfoOverrides.class);
        when(templateInfoOverrides.getOverrides(any())).thenReturn(Optional.empty());

        templateInfoService = new TemplateInfoService(templateResourceService,
                templateInfoOverrides);
    }

    @Test
    public void getByTemplateIds_EmptyReqTemplates_EmptyResult() {
        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(emptyList());
        assertThat(templateInfos).isEmpty();
    }

    @Test
    public void getByTemplateIds_UnknownTemplate_EmptyResult() {
        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(UNKNOWN_TEMPLATE_ID));
        assertThat(templateInfos).isEmpty();
    }

    @Test
    public void getByTemplateIds_CheckTemplateIdIsCorrect() {
        createTemplateResource(1L, 1, false, false, false);
        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateInfos).hasSize(1);
            softly.assertThat(templateInfos)
                    .element(0)
                    .extracting(InternalTemplateInfo::getTemplateId)
                    .isEqualTo(TEMPLATE_ID);
        });
    }

    @Test
    public void getByTemplateIds_CheckResourceIdsComplyPositions() {
        createTemplateResource(1L, 2, false, false, false);
        createTemplateResource(2L, 1, false, false, false);
        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateInfos).hasSize(1);
            softly.assertThat(templateInfos.get(0).getResources())
                    .extracting(AbstractResourceInfo::getId)
                    .containsExactly(2L, 1L);
        });
    }

    @Test
    public void getByTemplateIds_OneOptionalResource_CheckResourceRestrictions() {
        createTemplateResource(1L, 1, false, false, false);
        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateInfos).hasSize(1);
            softly.assertThat(templateInfos.get(0).getResourceRestrictions())
                    .is(matchedBy(beanDiffer(
                                    singletonList(
                                            new ResourceRestriction()
                                                    .withRequired(emptySet())
                                                    .withAbsent(emptySet()))
                            )
                    ));
        });
    }

    @Test
    public void getByTemplateIds_OneRequiredResource_CheckResourceRestrictions() {
        createTemplateResource(1L, 1, true, false, false);
        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateInfos).hasSize(1);
            softly.assertThat(templateInfos.get(0).getResourceRestrictions())
                    .is(matchedBy(beanDiffer(
                                    singletonList(
                                            new ResourceRestriction()
                                                    .withRequired(singleton(1L))
                                                    .withAbsent(emptySet()))
                            )
                    ));
        });
    }

    @Test
    public void getByTemplateIds_OneOptionalResourceWithOverrides_CheckResourceRestrictions() {
        createTemplateResource(1L, 1, false, false, false);
        when(templateInfoOverrides.getOverrides(TEMPLATE_ID))
                .thenReturn(Optional.of(new InternalTemplateOverrides().requiredVariant(singleton(1L), emptySet())));

        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateInfos).hasSize(1);
            softly.assertThat(templateInfos.get(0).getResourceRestrictions())
                    .is(matchedBy(beanDiffer(
                                    singletonList(
                                            new ResourceRestriction()
                                                    .withRequired(singleton(1L))
                                                    .withAbsent(emptySet()))
                            )
                    ));
        });
    }

    @Test
    public void getByTemplateIds_MakeSecondResourceAbsent_CheckResourceRestrictions() {
        createTemplateResource(1L, 1, true, false, false);
        createTemplateResource(2L, 2, true, false, false);
        when(templateInfoOverrides.getOverrides(TEMPLATE_ID))
                .thenReturn(Optional.of(new InternalTemplateOverrides().requiredVariant(singleton(1L), singleton(2L))));

        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateInfos).hasSize(1);
            softly.assertThat(templateInfos.get(0).getResourceRestrictions())
                    .is(matchedBy(beanDiffer(
                                    singletonList(
                                            new ResourceRestriction()
                                                    .withRequired(singleton(1L))
                                                    .withAbsent(singleton(2L)))
                            )
                    ));
        });
    }

    @Test
    public void getByTemplateIds_TwoResourceRestrictionVariants_CheckResourceRestrictions() {
        createTemplateResource(1L, 1, false, false, false);
        createTemplateResource(2L, 2, false, false, false);
        when(templateInfoOverrides.getOverrides(TEMPLATE_ID))
                .thenReturn(Optional.of(
                        new InternalTemplateOverrides()
                                .requiredVariant(singleton(1L), singleton(2L))
                                .requiredVariant(singleton(2L), singleton(1L))
                ));

        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateInfos).hasSize(1);
            softly.assertThat(templateInfos.get(0).getResourceRestrictions())
                    .is(matchedBy(beanDiffer(
                                    asList(
                                            new ResourceRestriction()
                                                    .withRequired(singleton(1L))
                                                    .withAbsent(singleton(2L)),
                                            new ResourceRestriction()
                                                    .withRequired(singleton(2L))
                                                    .withAbsent(singleton(1L)))
                            )
                    ));
        });
    }

    @Test
    public void getByTemplateIds_TemplateWithDifferentResources_CheckTypes() {
        createTemplateResource(1, 1, true, false, false);
        createTemplateResource(2, 2, true, true, false);
        createTemplateResource(3, 3, true, false, true);
        createTemplateResource(4, 4, true, false, false, "Возрастное ограничение");
        createTemplateResource(5, 5, true, false, false, "Счетчик закрытия");
        createTemplateResource(6, 6, true, false, false, "Счётчик закрытия");

        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(templateInfos.get(0).getResources()).hasSize(6);
            softly.assertThat(templateInfos.get(0).getResources())
                    .extracting(ResourceInfo::getType)
                    .containsExactly(ResourceType.TEXT, ResourceType.IMAGE, ResourceType.URL, ResourceType.AGE,
                            ResourceType.CLOSE_COUNTER, ResourceType.CLOSE_COUNTER);
        });
    }

    @Test
    public void getByTemplateIds_OverrideChoices_CheckResourceRestrictions() {
        createTemplateResource(1L, 1, true, false, false);
        when(templateInfoOverrides.getOverrides(TEMPLATE_ID))
                .thenReturn(Optional.of(new InternalTemplateOverrides().addResourceChoices(1L, CHOICES)));

        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        List<ResourceChoice> resultChoices = FunctionalUtils.mapList(CHOICES, ResourceChoice::from);

        assertThat(templateInfos.get(0).getResources().get(0).getChoices())
                .containsExactlyElementsOf(resultChoices);
    }

    @Test
    public void getByTemplateIds_CheckDefaultRestrictions() {
        createTemplateResource(1, 1, true, false, false);

        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        assertThat(templateInfos.get(0).getResources().get(0).getValueRestrictions())
                .is(matchedBy(beanDiffer(Restrictions.defaultText())));
    }

    @Test
    public void getByTemplateIds_OverrideRestrictions_CheckRestrictions() {
        createTemplateResource(1, 1, true, false, false);
        when(templateInfoOverrides.getOverrides(TEMPLATE_ID))
                .thenReturn(Optional.of(
                        new InternalTemplateOverrides().valueRestriction(1L, Restrictions.underSearchRetinaImg())
                ));

        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        assertThat(templateInfos.get(0).getResources().get(0).getValueRestrictions())
                .is(matchedBy(beanDiffer(Restrictions.underSearchRetinaImg())));
    }

    @Test
    public void getByTemplateIds_CheckHiddenResource() {
        long hiddenResourceId = 1;
        long resourceId = 2;
        createTemplateResource(hiddenResourceId, 1, true, false, false);
        createTemplateResource(resourceId, 2, true, false, false);
        when(templateInfoOverrides.getOverrides(TEMPLATE_ID))
                .thenReturn(Optional.of(
                        new InternalTemplateOverrides().addHiddenResources(hiddenResourceId)
                ));

        List<InternalTemplateInfo> templateInfos = templateInfoService.getByTemplateIds(singleton(TEMPLATE_ID));

        assertThat(templateInfos.get(0).getResources())
                .extracting(ResourceInfo::getId, ResourceInfo::isHidden)
                .containsExactly(new Tuple(hiddenResourceId, true), new Tuple(resourceId, false));
    }

    private void createTemplateResource(long resourceId, long position, boolean isRequired, boolean isImage,
                                        boolean isUrl) {
        createTemplateResource(resourceId, position, isRequired, isImage, isUrl,
                TemplateResourceRepositoryMockUtils.RESOURCE_DESCRIPTION);
    }

    private void createTemplateResource(long resourceId, long position, boolean isRequired, boolean isImage,
                                        boolean isUrl, String description) {
        TemplateResource resource = TemplateResourceRepositoryMockUtils.createResource(
                TEMPLATE_ID, resourceId, position, isRequired, isImage, isUrl, description);
        templateResources.put(resourceId, resource);
    }
}
