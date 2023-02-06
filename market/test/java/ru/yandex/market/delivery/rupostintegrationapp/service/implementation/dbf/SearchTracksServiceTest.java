package ru.yandex.market.delivery.rupostintegrationapp.service.implementation.dbf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.dbf.DbfCodeRepository;
import ru.yandex.market.delivery.rupostintegrationapp.log.JobStatusLogger;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfCode;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfCodesSearcher;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchTracksServiceTest extends BaseTest {

    @Mock
    private DbfCodeRepository dbfCodeRepository;
    @Mock
    private DbfCodesSearcher dbfCodesSearcher;

    @Mock
    private JobStatusLogger jobStatusLogger;

    private static final Integer TOTAL_PER_RUN = 4;

    private static final Integer BATCH_FIND_SIZE = 2;

    private SearchTracksService searchTracksService;

    @BeforeEach
    void setUp() {
        searchTracksService = new SearchTracksService(
            jobStatusLogger,
            dbfCodeRepository,
            dbfCodesSearcher,
            TOTAL_PER_RUN,
            BATCH_FIND_SIZE
        );
    }

    @Test
    void execute() {
        searchTracksService.execute();
        verify(dbfCodeRepository, never()).save(any(DbfCode.class));
    }

    @Test
    void executeOkOnePage() {
        int page = 0;

        List<DbfCode> codes = Collections.singletonList(new DbfCode());

        PageRequest request = new PageRequest(page, BATCH_FIND_SIZE);
        when(dbfCodeRepository.findAllByOrderIdIsNullAndRetriesGreaterThanOrderByRetriesDesc(
            eq(request), eq(0)
            )
        ).thenReturn(codes);

        searchTracksService.execute();

        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(dbfCodeRepository, times(2)).saveAll(argumentCaptor.capture());


        List<List> capturedArgument = argumentCaptor.getAllValues();
        softly.assertThat(capturedArgument).hasSize(2);
        softly.assertThat(capturedArgument.get(0)).isEqualTo(codes);
        softly.assertThat(capturedArgument.get(0)).hasSize(1);
        softly.assertThat(capturedArgument.get(1)).isEqualTo(Collections.emptyList());
    }

    @Test
    void executeOkTwoPages() {

        List<DbfCode> codes1 = Arrays.asList(new DbfCode(), new DbfCode());
        List<DbfCode> codes2 = Collections.singletonList(new DbfCode());

        when(dbfCodeRepository.findAllByOrderIdIsNullAndRetriesGreaterThanOrderByRetriesDesc(
            eq(new PageRequest(0, BATCH_FIND_SIZE)), eq(0)
            )
        )
            .thenReturn(codes1);
        when(dbfCodeRepository.findAllByOrderIdIsNullAndRetriesGreaterThanOrderByRetriesDesc(
            eq(new PageRequest(1, BATCH_FIND_SIZE)), eq(0)
            )
        )
            .thenReturn(codes2);

        searchTracksService.execute();

        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(dbfCodeRepository, times(4)).saveAll(argumentCaptor.capture());

        List<List> capturedArgument = argumentCaptor.getAllValues();
        softly.assertThat(capturedArgument).hasSize(4);
        softly.assertThat(capturedArgument.get(0)).isEqualTo(codes1);
        softly.assertThat(capturedArgument.get(0)).hasSize(codes1.size());
        softly.assertThat(capturedArgument.get(1)).isEqualTo(Collections.emptyList());
        softly.assertThat(capturedArgument.get(2)).isEqualTo(codes2);
        softly.assertThat(capturedArgument.get(2)).hasSize(codes2.size());
        softly.assertThat(capturedArgument.get(3)).isEqualTo(Collections.emptyList());
    }


    @Test
    void executeOkTwoPagesWithOverLimit() {

        List<DbfCode> codes1 = Arrays.asList(new DbfCode(), new DbfCode());
        List<DbfCode> codes2 = Arrays.asList(new DbfCode(), new DbfCode());
        List<DbfCode> codes3 = Collections.singletonList(new DbfCode());

        when(dbfCodeRepository.findAllByOrderIdIsNullAndRetriesGreaterThanOrderByRetriesDesc(
            eq(new PageRequest(0, BATCH_FIND_SIZE)), eq(0)
            )
        )
            .thenReturn(codes1);
        when(dbfCodeRepository.findAllByOrderIdIsNullAndRetriesGreaterThanOrderByRetriesDesc(
            eq(new PageRequest(1, BATCH_FIND_SIZE)), eq(0)
            )
        )
            .thenReturn(codes2);
        when(dbfCodeRepository.findAllByOrderIdIsNullAndRetriesGreaterThanOrderByRetriesDesc(
            eq(new PageRequest(2, BATCH_FIND_SIZE)), eq(0)
            )
        )
            .thenReturn(codes3);

        searchTracksService.execute();

        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(dbfCodeRepository, times(4)).saveAll(argumentCaptor.capture());

        List<List> capturedArgument = argumentCaptor.getAllValues();
        softly.assertThat(capturedArgument).hasSize(4);
        softly.assertThat(capturedArgument.get(0)).isEqualTo(codes1);
        softly.assertThat(capturedArgument.get(0)).hasSize(codes1.size());
        softly.assertThat(capturedArgument.get(1)).isEqualTo(Collections.emptyList());
        softly.assertThat(capturedArgument.get(2)).isEqualTo(codes2);
        softly.assertThat(capturedArgument.get(2)).hasSize(codes2.size());
        softly.assertThat(capturedArgument.get(3)).isEqualTo(Collections.emptyList());
    }
}
