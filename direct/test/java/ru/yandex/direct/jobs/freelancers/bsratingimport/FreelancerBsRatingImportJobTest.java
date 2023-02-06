package ru.yandex.direct.jobs.freelancers.bsratingimport;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerBase;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtTable;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.ENABLE_IMPORT_FREELANCERS_RATING;
import static ru.yandex.direct.jobs.freelancers.bsratingimport.FreelancerBsRatingImportJob.LAST_IMPORTED_TABLE_PROP_NAME;

class FreelancerBsRatingImportJobTest {
    private static final YtCluster CLUSTER = YtCluster.HAHN;
    private static final YtTable YT_TABLE = new YtTable("//home/bs/freelancers/direct/export/2018-11-19");
    private static final String JOB_ENABLE_STR = "1";
    private static final String LAST_IMPORTED_TABLE_NAME = "2018-11-18";
    private static final String TABLE_TO_IMPORT_NAME = "2018-11-19";
    private static final List<FreelancerBase> RATINGS_FROM_YT = emptyList();

    private PpcPropertiesSupport ppcPropertiesSupportMock;
    private FreelancerYtRatingService freelancerYtRatingServiceMock;

    private FreelancerBsRatingImportJob testedJob;
    private FreelancerUpdateRatingService freelancerUpdateRatingService;

    @BeforeEach
    void setUp() {
        freelancerYtRatingServiceMock = mock(FreelancerYtRatingService.class);
        when(freelancerYtRatingServiceMock.getFreshestRatingTableName(any(YtCluster.class), anyString()))
                .thenReturn(TABLE_TO_IMPORT_NAME);
        when(freelancerYtRatingServiceMock.readRatingsFromYt(any(YtCluster.class), any(YtTable.class)))
                .thenReturn(RATINGS_FROM_YT);

        ppcPropertiesSupportMock = mock(PpcPropertiesSupport.class);
        when(ppcPropertiesSupportMock.get(eq(ENABLE_IMPORT_FREELANCERS_RATING.getName())))
                .thenReturn(JOB_ENABLE_STR);
        when(ppcPropertiesSupportMock.get(eq(LAST_IMPORTED_TABLE_PROP_NAME)))
                .thenReturn(LAST_IMPORTED_TABLE_NAME);

        freelancerUpdateRatingService = mock(FreelancerUpdateRatingService.class);
        testedJob = new FreelancerBsRatingImportJob(
                ppcPropertiesSupportMock,
                freelancerYtRatingServiceMock,
                freelancerUpdateRatingService
        );
    }

    @Test
    void execute_noOp_whenDisabled() {
        when(ppcPropertiesSupportMock.get(eq(ENABLE_IMPORT_FREELANCERS_RATING.getName())))
                .thenReturn(null);
        testedJob.execute();

        verify(ppcPropertiesSupportMock, only()).get(eq(ENABLE_IMPORT_FREELANCERS_RATING.getName()));
        verifyZeroInteractions(freelancerUpdateRatingService);
        verifyZeroInteractions(freelancerUpdateRatingService);
    }

    @Test
    void execute_noUpdate_whenNoTable() {
        when(freelancerYtRatingServiceMock.getFreshestRatingTableName(any(YtCluster.class), anyString()))
                .thenReturn(null);

        testedJob.execute();

        verify(freelancerYtRatingServiceMock, only()).getFreshestRatingTableName(any(), any());
        verifyZeroInteractions(freelancerUpdateRatingService);
    }

    @Test
    void execute_noUpdate_whenTableTooOld() {
        when(freelancerYtRatingServiceMock.getFreshestRatingTableName(any(YtCluster.class), anyString()))
                .thenReturn(LAST_IMPORTED_TABLE_NAME);

        testedJob.execute();
        // Если таблица слишком стара, не должно быть вызова update'а
        verify(freelancerYtRatingServiceMock, only()).getFreshestRatingTableName(any(), any());
        verifyZeroInteractions(freelancerUpdateRatingService);
    }

    @Test
    void execute_success() {
        testedJob.execute();

        ArgumentCaptor<YtTable> ytTableCaptor = ArgumentCaptor.forClass(YtTable.class);
        verify(freelancerYtRatingServiceMock).getFreshestRatingTableName(any(), any());
        verify(freelancerYtRatingServiceMock).readRatingsFromYt(eq(YtCluster.HAHN), ytTableCaptor.capture());
        assertThat(ytTableCaptor.getValue().getPath()).isEqualTo("//home/bs/freelancers/direct/export/2018-11-19");

        verifyNoMoreInteractions(freelancerYtRatingServiceMock);
        verify(freelancerUpdateRatingService, only()).updateRatingForAll(same(RATINGS_FROM_YT));
    }

    /**
     * Проверка отдельного метода {@link FreelancerBsRatingImportJob#importFreelancerRatingToDirect}.
     * По большей части повторяет {@link #execute_success()}
     */
    @Test
    void importFreelancerRatingToDirect_success() {
        testedJob.importFreelancerRatingToDirect(CLUSTER, YT_TABLE);

        ArgumentCaptor<YtTable> ytTableCaptor = ArgumentCaptor.forClass(YtTable.class);
        verify(freelancerYtRatingServiceMock).readRatingsFromYt(eq(YtCluster.HAHN), ytTableCaptor.capture());
        assertThat(ytTableCaptor.getValue().getPath()).isEqualTo("//home/bs/freelancers/direct/export/2018-11-19");

        verifyNoMoreInteractions(freelancerYtRatingServiceMock);
        verify(freelancerUpdateRatingService, only()).updateRatingForAll(same(RATINGS_FROM_YT));
    }
}
