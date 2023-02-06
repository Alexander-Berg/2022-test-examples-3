package ru.yandex.direct.jobs.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.internalads.model.InternalAdPlace;
import ru.yandex.direct.core.entity.internalads.repository.PlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.PlacesYtRepository;
import ru.yandex.direct.core.testing.mock.PlaceRepositoryMockUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.common.db.PpcPropertyNames.PLACES_LAST_UPDATE_UNIX_TIME;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@ParametersAreNonnullByDefault
class UpdatePlacesJobTest {

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PpcProperty<Long> lastUpdateUnixTimeProperty;

    private PlacesYtRepository ytRepository;
    private UpdatePlacesJob job;
    private long ytTableLastUpdateUnixTime;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);
        ytRepository = spy(PlaceRepositoryMockUtils.createYtRepositoryMock());
        ytTableLastUpdateUnixTime = RandomNumberUtils.nextPositiveLong();
        doReturn(ytTableLastUpdateUnixTime)
                .when(ytRepository).getLastUpdateUnixTime();

        doReturn(lastUpdateUnixTimeProperty)
                .when(ppcPropertiesSupport).get(PLACES_LAST_UPDATE_UNIX_TIME);

        job = new UpdatePlacesJob(ppcPropertiesSupport, ytRepository, placeRepository);
    }


    @Test
    void checkJob() {
        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PLACES_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(placeRepository).getAll();

        verify(placeRepository).addOrUpdate(eq(ytRepository.getAll()));
        verify(placeRepository).delete(eq(Collections.emptySet()));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenNotNeedUpdate() {
        doReturn(ytTableLastUpdateUnixTime)
                .when(lastUpdateUnixTimeProperty).get();

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PLACES_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(placeRepository);
        verifyNoMoreInteractions(lastUpdateUnixTimeProperty);
    }

    @Test
    void checkJob_whenNothingUpdate_allRecordsFromYtAreEqualsWithAllRecordsFromMysql() {
        doReturn(ytRepository.getAll())
                .when(placeRepository).getAll();
        clearInvocations(ytRepository);

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PLACES_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(placeRepository).getAll();

        verify(placeRepository).addOrUpdate(eq(Collections.emptyList()));
        verify(placeRepository).delete(eq(Collections.emptySet()));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenDeleteOldPlaces() {
        List<InternalAdPlace> oldPlacesToDelete = ImmutableList.of(getOldPlacesToDelete());
        Set<Long> oldPlaceIdsToDelete = listToSet(oldPlacesToDelete, InternalAdPlace::getId);
        doReturn(oldPlacesToDelete)
                .when(placeRepository).getAll();

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PLACES_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getAll();
        verify(placeRepository).getAll();

        verify(placeRepository).addOrUpdate(eq(ytRepository.getAll()));
        verify(placeRepository).delete(eq(oldPlaceIdsToDelete));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenFetchedPlacesFromYt_isEmpty() {
        doReturn(Collections.emptyList())
                .when(ytRepository).getAll();

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fetched places from YT can't be empty");

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PLACES_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();
        verify(ytRepository).getAll();

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(placeRepository);
        verifyNoMoreInteractions(lastUpdateUnixTimeProperty);
    }

    private static InternalAdPlace getOldPlacesToDelete() {
        return new InternalAdPlace()
                .withId(Long.MAX_VALUE)
                .withParentId(0L)
                .withDescription(RandomStringUtils.randomAlphanumeric(100));
    }

}
