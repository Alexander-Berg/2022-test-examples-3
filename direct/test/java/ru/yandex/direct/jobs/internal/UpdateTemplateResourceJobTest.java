package ru.yandex.direct.jobs.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.internalads.model.TemplateResource;
import ru.yandex.direct.core.entity.internalads.repository.TemplateResourceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplateResourceYtRepository;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.common.db.PpcPropertyNames.TEMPLATE_RESOURCE_LAST_UPDATE_UNIX_TIME;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@ParametersAreNonnullByDefault
class UpdateTemplateResourceJobTest {

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private TemplateResourceRepository templateResourceRepository;

    @Mock
    private PpcProperty<Long> lastUpdateUnixTimeProperty;

    private TemplateResourceYtRepository ytRepository;
    private UpdateTemplateResourceJob job;
    private long ytTableLastUpdateUnixTime;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.openMocks(this);
        ytRepository = spy(TemplateResourceRepositoryMockUtils.createYtRepositoryMock());
        ytTableLastUpdateUnixTime = RandomNumberUtils.nextPositiveLong();
        doReturn(ytTableLastUpdateUnixTime)
                .when(ytRepository).getLastUpdateUnixTime();

        doReturn(lastUpdateUnixTimeProperty)
                .when(ppcPropertiesSupport).get(TEMPLATE_RESOURCE_LAST_UPDATE_UNIX_TIME);

        job = new UpdateTemplateResourceJob(ppcPropertiesSupport, ytRepository, templateResourceRepository);
    }


    @Test
    void checkJob() {
        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_RESOURCE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(templateResourceRepository).getAll();

        verify(templateResourceRepository).addOrUpdate(eq(ytRepository.getAll()));
        verify(templateResourceRepository).delete(eq(Collections.emptySet()));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenNotNeedUpdate() {
        doReturn(ytTableLastUpdateUnixTime)
                .when(lastUpdateUnixTimeProperty).get();

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_RESOURCE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(templateResourceRepository);
        verifyNoMoreInteractions(lastUpdateUnixTimeProperty);
    }

    @Test
    void checkJob_whenNothingUpdate_allRecordsFromYtAreEqualsWithAllRecordsFromMysql() {
        doReturn(ytRepository.getAll())
                .when(templateResourceRepository).getAll();
        clearInvocations(ytRepository);

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_RESOURCE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(templateResourceRepository).getAll();

        verify(templateResourceRepository).addOrUpdate(eq(Collections.emptyList()));
        verify(templateResourceRepository).delete(eq(Collections.emptySet()));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenDeleteOldResources() {
        List<TemplateResource> oldResourcesToDelete = ImmutableList.of(getOldResourcesForDelete());
        Set<Long> oldResourceIdsToDelete = listToSet(oldResourcesToDelete, TemplateResource::getId);
        doReturn(oldResourcesToDelete)
                .when(templateResourceRepository).getAll();

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_RESOURCE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(templateResourceRepository).getAll();

        verify(templateResourceRepository).addOrUpdate(eq(ytRepository.getAll()));
        verify(templateResourceRepository).delete(eq(oldResourceIdsToDelete));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenFetchedResourcesFromYt_isEmpty() {
        doReturn(Collections.emptyList())
                .when(ytRepository).getAll();

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fetched resources from YT can't be empty");

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_RESOURCE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();
        verify(ytRepository).getAll();

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(templateResourceRepository);
        verifyNoMoreInteractions(lastUpdateUnixTimeProperty);
    }

    private static TemplateResource getOldResourcesForDelete() {
        return new TemplateResource()
                .withId(Long.MAX_VALUE)
                .withTemplateId(RandomNumberUtils.nextPositiveLong());
    }

}
