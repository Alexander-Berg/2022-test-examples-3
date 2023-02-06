package ru.yandex.direct.core.entity.internalads.repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.internalads.model.DirectTemplateResource;
import ru.yandex.direct.core.entity.internalads.model.DirectTemplateResourceOption;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DirectTemplateResourceRepositoryTest {
    private static final Long TEMPLATE_RESOURCE_ID_OLD = 1234L;
    private static final Long TEMPLATE_ID1 = 2345L;
    private static final Long TEMPLATE_ID2 = 2543L;
    private static final Long RESOURCE_NO1 = 1L;
    private static final Long UNIFIED_RESOURCE_NO1 = 25L;
    private static final Long UNIFIED_RESOURCE_NO2 = 80L;
    private static final Long UNIFIED_TEMPLATE_RESOURCE_ID1 = 6000L;
    private static final Long UNIFIED_TEMPLATE_RESOURCE_ID2 = 6053L;
    private static final Set<DirectTemplateResourceOption> OPTIONS1 =
            Set.of(DirectTemplateResourceOption.REQUIRED, DirectTemplateResourceOption.BANANA_URL);
    private static final Set<DirectTemplateResourceOption> OPTIONS2 =
            Set.of(DirectTemplateResourceOption.BANANA_IMAGE);

    private static final DirectTemplateResource TEMPLATE_RESOURCE_WITH_ID = new DirectTemplateResource()
            .withDirectTemplateResourceId(TEMPLATE_RESOURCE_ID_OLD)
            .withDirectTemplateId(TEMPLATE_ID1)
            .withResourceNo(RESOURCE_NO1)
            .withUnifiedResourceNo(UNIFIED_RESOURCE_NO1)
            .withUnifiedTemplateResourceId(UNIFIED_TEMPLATE_RESOURCE_ID1)
            .withOptions(OPTIONS1);
    private static final DirectTemplateResource TEMPLATE_RESOURCE_WITHOUT_ID = new DirectTemplateResource()
            .withDirectTemplateId(TEMPLATE_ID2)
            .withResourceNo(UNIFIED_RESOURCE_NO2)
            .withUnifiedResourceNo(UNIFIED_RESOURCE_NO2)
            .withUnifiedTemplateResourceId(UNIFIED_TEMPLATE_RESOURCE_ID2)
            .withOptions(OPTIONS2);

    @Autowired
    DirectTemplateResourceRepository directTemplateResourceRepository;

    @Before
    public void before() {
        var directTemplateResourceIds = mapList(
                directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID1, TEMPLATE_ID2)),
                DirectTemplateResource::getDirectTemplateResourceId);

        directTemplateResourceRepository.delete(directTemplateResourceIds);
        assertThat(directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID1, TEMPLATE_ID2)), hasSize(0));
    }

    @Test
    public void insertWithIdAndGet_compareData() {
        directTemplateResourceRepository.addOrUpdate(List.of(TEMPLATE_RESOURCE_WITH_ID));
        var templateResources = directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID1));
        assertThat(templateResources, hasSize(1));
        var dbTemplateResource = templateResources.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(dbTemplateResource.getDirectTemplateId()).isEqualTo(TEMPLATE_ID1);
            softly.assertThat(dbTemplateResource.getDirectTemplateResourceId()).isEqualTo(TEMPLATE_RESOURCE_ID_OLD);
            softly.assertThat(dbTemplateResource.getResourceNo()).isEqualTo(RESOURCE_NO1);
            softly.assertThat(dbTemplateResource.getUnifiedResourceNo()).isEqualTo(UNIFIED_RESOURCE_NO1);
            softly.assertThat(dbTemplateResource.getUnifiedTemplateResourceId()).isEqualTo(UNIFIED_TEMPLATE_RESOURCE_ID1);
            softly.assertThat(dbTemplateResource.getOptions()).isEqualTo(OPTIONS1);
        });
    }

    @Test
    public void insertAndGet_compareData() {
        directTemplateResourceRepository.addOrUpdate(List.of(TEMPLATE_RESOURCE_WITHOUT_ID));
        var templateResources = directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID2));
        assertThat(templateResources, hasSize(1));
        var dbTemplateResource = templateResources.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(dbTemplateResource.getDirectTemplateId()).isEqualTo(TEMPLATE_ID2);
            softly.assertThat(dbTemplateResource.getDirectTemplateResourceId()).isNotNull();
            softly.assertThat(dbTemplateResource.getResourceNo()).isEqualTo(UNIFIED_RESOURCE_NO2);
            softly.assertThat(dbTemplateResource.getUnifiedResourceNo()).isEqualTo(UNIFIED_RESOURCE_NO2);
            softly.assertThat(dbTemplateResource.getUnifiedTemplateResourceId()).isEqualTo(UNIFIED_TEMPLATE_RESOURCE_ID2);
            softly.assertThat(dbTemplateResource.getOptions()).isEqualTo(OPTIONS2);
        });
    }

    @Test
    public void testDelete() {
        directTemplateResourceRepository.addOrUpdate(List.of(TEMPLATE_RESOURCE_WITH_ID));
        var directTemplateResourceIds = mapList(
                directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID1)),
                DirectTemplateResource::getDirectTemplateResourceId);
        assertThat(directTemplateResourceIds, hasSize(1));
        directTemplateResourceRepository.delete(directTemplateResourceIds);
        assertThat(directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID1)), hasSize(0));
    }

    @Test
    public void testGetMethods() {
        directTemplateResourceRepository.addOrUpdate(List.of(TEMPLATE_RESOURCE_WITH_ID, TEMPLATE_RESOURCE_WITHOUT_ID));

        var templateResources = directTemplateResourceRepository.getByTemplateIds(List.of(TEMPLATE_ID1, TEMPLATE_ID2));
        assertThat(templateResources, hasSize(2));

        var directTemplateResourceIds = mapList(templateResources, DirectTemplateResource::getDirectTemplateResourceId);
        assertThat(directTemplateResourceRepository.getByIds(directTemplateResourceIds), hasSize(2));

        var newDirectTemplateResourceId = directTemplateResourceIds.stream()
                .filter(id -> !Objects.equals(id, TEMPLATE_RESOURCE_ID_OLD))
                .findAny().get();
        assertThat(directTemplateResourceRepository.getMapByTemplateIds(List.of(TEMPLATE_ID1, TEMPLATE_ID2)),
                equalTo(Map.of(
                        TEMPLATE_ID1, List.of(TEMPLATE_RESOURCE_WITH_ID),
                        TEMPLATE_ID2, List.of(TEMPLATE_RESOURCE_WITHOUT_ID
                                .withDirectTemplateResourceId(newDirectTemplateResourceId)))));
    }
}
