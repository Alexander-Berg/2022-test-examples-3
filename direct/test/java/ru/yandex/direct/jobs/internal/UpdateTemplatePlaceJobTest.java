package ru.yandex.direct.jobs.internal;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.internalads.model.TemplatePlace;
import ru.yandex.direct.core.entity.internalads.repository.TemplatePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplatePlaceYtRepository;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.common.db.PpcPropertyNames.TEMPLATE_PLACE_LAST_UPDATE_UNIX_TIME;

@ParametersAreNonnullByDefault
class UpdateTemplatePlaceJobTest {

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private TemplatePlaceRepository templatePlaceRepository;

    @Mock
    private PpcProperty<Long> lastUpdateUnixTimeProperty;

    private TemplatePlaceYtRepository ytRepository;
    private UpdateTemplatePlaceJob job;
    private long ytTableLastUpdateUnixTime;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.openMocks(this);
        ytRepository = spy(TemplatePlaceRepositoryMockUtils.createYtRepositoryMock());
        ytTableLastUpdateUnixTime = RandomNumberUtils.nextPositiveLong();
        doReturn(ytTableLastUpdateUnixTime)
                .when(ytRepository).getLastUpdateUnixTime();

        doReturn(lastUpdateUnixTimeProperty)
                .when(ppcPropertiesSupport).get(TEMPLATE_PLACE_LAST_UPDATE_UNIX_TIME);

        job = new UpdateTemplatePlaceJob(ppcPropertiesSupport, ytRepository, templatePlaceRepository);
    }


    @Test
    void checkJob() {
        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(templatePlaceRepository).getAll();

        verify(templatePlaceRepository).add(eq(ytRepository.getAll()));
        verify(templatePlaceRepository).delete(eq(Collections.emptyList()));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenNotNeedUpdate() {
        doReturn(ytTableLastUpdateUnixTime)
                .when(lastUpdateUnixTimeProperty).get();

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(templatePlaceRepository);
        verifyNoMoreInteractions(lastUpdateUnixTimeProperty);
    }

    @Test
    void checkJob_whenNothingUpdate_allRecordsFromYtAreEqualsWithAllRecordsFromMysql() {
        doReturn(ytRepository.getAll())
                .when(templatePlaceRepository).getAll();
        clearInvocations(ytRepository);

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(templatePlaceRepository).getAll();

        verify(templatePlaceRepository).add(eq(Collections.emptyList()));
        verify(templatePlaceRepository).delete(eq(Collections.emptyList()));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenDeleteOldRecords() {
        List<TemplatePlace> oldRecordsToDelete = ImmutableList.of(getOldRecordsForDelete());
        doReturn(oldRecordsToDelete)
                .when(templatePlaceRepository).getAll();

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(templatePlaceRepository).getAll();

        verify(templatePlaceRepository).add(eq(ytRepository.getAll()));
        verify(templatePlaceRepository).delete(eq(oldRecordsToDelete));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenFetchedRecordsFromYt_isEmpty() {
        doReturn(Collections.emptyList())
                .when(ytRepository).getAll();

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fetched records from YT can't be empty");

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(TEMPLATE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();
        verify(ytRepository).getAll();

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(templatePlaceRepository);
        verifyNoMoreInteractions(lastUpdateUnixTimeProperty);
    }

    private static TemplatePlace getOldRecordsForDelete() {
        return new TemplatePlace()
                .withTemplateId(Long.MAX_VALUE)
                .withPlaceId(RandomNumberUtils.nextPositiveLong());
    }

}
