package ru.yandex.direct.core.entity.internalads.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ru.yandex.direct.core.entity.internalads.model.DirectTemplateResource;
import ru.yandex.direct.core.entity.internalads.model.DirectTemplateResourceWrapper;
import ru.yandex.direct.core.entity.internalads.model.TemplateResource;
import ru.yandex.direct.core.entity.internalads.repository.DirectTemplateResourceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplateResourceRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.internalads.service.TemplateResourceService.DIRECT_TEMPLATE_RESOURCE_STARTING_ID;

public class TemplateResourceServiceTest {
    private final DirectTemplateResourceRepository directTemplateResourceRepository =
            mock(DirectTemplateResourceRepository.class);
    private final TemplateResourceRepository templateResourceRepository =
            mock(TemplateResourceRepository.class);
    private final TemplateResourceService templateResourceService =
            new TemplateResourceService(directTemplateResourceRepository, templateResourceRepository);

    @Test
    public void wrapResourcesTest() {
        var templateId = 4_000_000L;
        var resource1Id = DIRECT_TEMPLATE_RESOURCE_STARTING_ID + 1;
        var resource1No = 1L;
        var resource1UnifiedResourceId = 5_678L;
        var resource1UnifiedResourceNo = 15_678L;
        var resource1 = new DirectTemplateResource()
                .withDirectTemplateResourceId(resource1Id)
                .withResourceNo(resource1No)
                .withUnifiedTemplateResourceId(resource1UnifiedResourceId)
                .withUnifiedResourceNo(resource1UnifiedResourceNo)
                .withOptions(Set.of())
                .withDirectTemplateId(templateId);

        var unifiedResource1Type = 3L;
        var unifiedResource1Description = "текст";
        var unifiedResource1 = new TemplateResource()
                .withId(resource1UnifiedResourceId)
                .withResourceType(unifiedResource1Type)
                .withDescription(unifiedResource1Description);

        var resource2Id = DIRECT_TEMPLATE_RESOURCE_STARTING_ID + 2;
        var resource2No = 2L;
        var resource2UnifiedResourceId = 5_679L;
        var resource2UnifiedResourceNo = 15_679L;
        var resource2 = new DirectTemplateResource()
                .withDirectTemplateResourceId(resource2Id)
                .withResourceNo(resource2No)
                .withUnifiedTemplateResourceId(resource2UnifiedResourceId)
                .withUnifiedResourceNo(resource2UnifiedResourceNo)
                .withOptions(Set.of())
                .withDirectTemplateId(templateId);

        var unifiedResource2Type = 2L;
        var unifiedResource2Description = "текст кнопки";
        var unifiedResource2 = new TemplateResource()
                .withId(resource2UnifiedResourceId)
                .withResourceType(unifiedResource2Type)
                .withDescription(unifiedResource2Description);

        // смигрированный ресурс, берёт часть данных от изначального ресурса
        var oldTemplateId = 2_000L;
        var resource3Id = DIRECT_TEMPLATE_RESOURCE_STARTING_ID - 2;
        var resource3No = 2L;
        var resource3UnifiedResourceId = 5_001L;
        var resource3UnifiedResourceNo = 15_001L;
        var resource3 = new DirectTemplateResource()
                .withDirectTemplateResourceId(resource3Id)
                .withResourceNo(resource3No)
                .withUnifiedTemplateResourceId(resource3UnifiedResourceId)
                .withUnifiedResourceNo(resource3UnifiedResourceNo)
                .withOptions(Set.of())
                .withDirectTemplateId(oldTemplateId);

        var unifiedResource3Type = 4L;
        var unifiedResource3 = new TemplateResource()
                .withId(resource3UnifiedResourceId)
                .withResourceType(unifiedResource3Type);

        var oldResource3Description = "картинка";
        var oldResource3Position = 14L;
        var oldResource3 = new TemplateResource()
                .withId(resource3Id)
                .withPosition(oldResource3Position)
                .withDescription(oldResource3Description);

        when(templateResourceRepository.getByIds(anySet()))
                .thenReturn(List.of(unifiedResource1, unifiedResource2, unifiedResource3, oldResource3));

        var result = templateResourceService.wrapResources(List.of(resource1, resource2, resource3));
        var expectedResource1 = new DirectTemplateResourceWrapper()
                .withResource(resource1)
                .withResourceType(unifiedResource1Type)
                .withDescription(unifiedResource1Description)
                .withPosition(resource1Id);
        var expectedResource2 = new DirectTemplateResourceWrapper()
                .withResource(resource2)
                .withResourceType(unifiedResource2Type)
                .withDescription(unifiedResource2Description)
                .withPosition(resource2Id);
        var expectedResource3 = new DirectTemplateResourceWrapper()
                .withResource(resource3)
                .withResourceType(unifiedResource3Type)
                .withDescription(oldResource3Description)
                .withPosition(oldResource3Position);
        var expected = Map.of(DIRECT_TEMPLATE_RESOURCE_STARTING_ID + 1, expectedResource1,
                DIRECT_TEMPLATE_RESOURCE_STARTING_ID + 2, expectedResource2,
                DIRECT_TEMPLATE_RESOURCE_STARTING_ID - 2, expectedResource3);
        assertThat(result).isEqualTo(expected);
    }
}
