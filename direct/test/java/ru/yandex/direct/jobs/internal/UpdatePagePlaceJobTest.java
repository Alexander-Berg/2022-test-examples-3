package ru.yandex.direct.jobs.internal;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.internalads.model.PagePlace;
import ru.yandex.direct.core.entity.internalads.repository.PagePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.PagePlaceYtRepository;
import ru.yandex.direct.core.entity.page.service.PageService;
import ru.yandex.direct.core.entity.pages.model.Page;
import ru.yandex.direct.core.testing.mock.PagePlaceRepositoryMockUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.common.db.PpcPropertyNames.PAGE_PLACE_LAST_UPDATE_UNIX_TIME;
import static ru.yandex.direct.core.testing.mock.PagePlaceRepositoryMockUtils.ALL_PAGE_IDS;
import static ru.yandex.direct.core.testing.mock.PagePlaceRepositoryMockUtils.PAGE_1;
import static ru.yandex.direct.core.testing.mock.PagePlaceRepositoryMockUtils.PAGE_2;

@ParametersAreNonnullByDefault
class UpdatePagePlaceJobTest {

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private PageService pageService;

    @Mock
    private PagePlaceRepository pagePlaceRepository;

    @Mock
    private PpcProperty<Long> lastUpdateUnixTimeProperty;

    private PagePlaceYtRepository ytRepository;
    private UpdatePagePlaceJob job;
    private long ytTableLastUpdateUnixTime;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);
        ytRepository = PagePlaceRepositoryMockUtils.createYtRepositoryMock();
        ytTableLastUpdateUnixTime = RandomNumberUtils.nextPositiveLong();

        doReturn(List.of(new Page().withId(PAGE_1).withOrigPageId(PAGE_1),
                new Page().withId(PAGE_2).withOrigPageId(PAGE_2)))
                .when(pageService).getAllInternalAdPages();

        doReturn(ytTableLastUpdateUnixTime)
                .when(ytRepository).getLastUpdateUnixTime();

        doReturn(lastUpdateUnixTimeProperty)
                .when(ppcPropertiesSupport).get(PAGE_PLACE_LAST_UPDATE_UNIX_TIME);

        job = new UpdatePagePlaceJob(ppcPropertiesSupport, pageService, ytRepository, pagePlaceRepository);
    }

    @Test
    void checkJob() {
        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PAGE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getPagePlaces(ALL_PAGE_IDS);
        verify(pagePlaceRepository).getAll();

        verify(pagePlaceRepository).add(eq(ytRepository.getPagePlaces(ALL_PAGE_IDS)));
        verify(pagePlaceRepository).delete(eq(List.of()));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenNotNeedUpdate() {
        doReturn(ytTableLastUpdateUnixTime)
                .when(lastUpdateUnixTimeProperty).get();

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PAGE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(pagePlaceRepository);
        verifyNoMoreInteractions(lastUpdateUnixTimeProperty);
    }

    @Test
    void checkJob_whenNothingUpdate_allRecordsFromYtAreEqualsWithAllRecordsFromMysql() {
        doReturn(ytRepository.getPagePlaces(ALL_PAGE_IDS)).when(pagePlaceRepository).getAll();
        clearInvocations(ytRepository);

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PAGE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getPagePlaces(ALL_PAGE_IDS);
        verify(pagePlaceRepository).getAll();

        verify(pagePlaceRepository).add(eq(List.of()));
        verify(pagePlaceRepository).delete(eq(List.of()));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenDeleteOldPagePlacePair() {
        var pagePlaceToDelete = new PagePlace()
                .withPageId(1234L)
                .withPlaceId(4321L);
        doReturn(List.of(pagePlaceToDelete)).when(pagePlaceRepository).getAll();

        job.execute();

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PAGE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();

        verify(ytRepository).getPagePlaces(ALL_PAGE_IDS);
        verify(pagePlaceRepository).getAll();

        verify(pagePlaceRepository).add(eq(ytRepository.getPagePlaces(ALL_PAGE_IDS)));
        verify(pagePlaceRepository).delete(eq(List.of(pagePlaceToDelete)));

        verify(lastUpdateUnixTimeProperty).set(eq(ytTableLastUpdateUnixTime));
    }

    @Test
    void checkJob_whenFetchedPagePlacesFromYt_isEmpty() {
        doReturn(List.of()).when(ytRepository).getPagePlaces(ALL_PAGE_IDS);

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fetched records from YT can't be empty");

        verify(ytRepository).getLastUpdateUnixTime();
        verify(ppcPropertiesSupport).get(PAGE_PLACE_LAST_UPDATE_UNIX_TIME);
        verify(lastUpdateUnixTimeProperty).get();
        verify(ytRepository).getPagePlaces(ALL_PAGE_IDS);

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(pagePlaceRepository);
        verifyNoMoreInteractions(lastUpdateUnixTimeProperty);
    }
}
