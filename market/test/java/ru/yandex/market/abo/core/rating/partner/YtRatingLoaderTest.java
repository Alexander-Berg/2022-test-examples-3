package ru.yandex.market.abo.core.rating.partner;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 21.10.2020
 */
class YtRatingLoaderTest {

    @InjectMocks
    private YtRatingLoader ytRatingLoader;

    @Mock
    private PartnerRatingRepo.RatingPartRepo ratingPartRepo;
    @Mock
    private JdbcTemplate yqlJdbcTemplate;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        ytRatingLoader.setRatingMetric(RatingMetric.FF_RETURN_RATE);
    }

    @Test
    void doNotUpdateRatingPartsIfAlreadyUpdatedToday() {
        var ratingPart = mock(PartnerRatingPart.class);
        when(ratingPartRepo.findAllByTypeInAndCalcTimeAfter(any(), any())).thenReturn(List.of(ratingPart));

        ytRatingLoader.updateFromYt();

        verifyNoMoreInteractions(yqlJdbcTemplate);
        verify(ratingPartRepo, never()).saveAll(anyCollection());
    }
}
