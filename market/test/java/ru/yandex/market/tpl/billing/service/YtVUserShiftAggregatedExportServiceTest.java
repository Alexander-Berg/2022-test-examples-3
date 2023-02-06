package ru.yandex.market.tpl.billing.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.billing.dao.view.VUserShiftAggregatedDao;
import ru.yandex.market.tpl.billing.model.yt.YtVUserShiftAggregatedDto;
import ru.yandex.market.tpl.billing.service.yt.YtService;
import ru.yandex.market.tpl.billing.service.yt.exports.YtBillingExportService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для бина ytVUserShiftAggregatedExportService
 */
// TODO: Replace with the DBUnit test (https://st.yandex-team.ru/MARKETTPLBILL-82)
@ExtendWith(SpringExtension.class)
public class YtVUserShiftAggregatedExportServiceTest {

    @Mock
    VUserShiftAggregatedDao vUserShiftAggregatedDao;

    @Mock
    YtService ytService;

    private static final String FOLDER = "COURIER";
    private static final String FOLDER_NAME = "v_user_shift_aggregated_folder";

    @Test
    void testExport() {
        when(vUserShiftAggregatedDao.getVUserShiftAggregated()).thenReturn(List.of());
        when(vUserShiftAggregatedDao.getDataClass()).thenReturn(YtVUserShiftAggregatedDto.class);

        YtBillingExportService<YtVUserShiftAggregatedDto> ytVUserShiftAggregatedExportService =
                new YtBillingExportService<>(
                        ytService,
                        vUserShiftAggregatedDao,
                        FOLDER,
                        FOLDER_NAME
                );

        ytVUserShiftAggregatedExportService.export(
                LocalDate.of(2021, Month.AUGUST, 29),
                LocalDate.of(2021, Month.SEPTEMBER, 1),
                true
        );

        verify(ytService, times(1)).export(
                List.of(),
                YtVUserShiftAggregatedDto.class,
                null,
                FOLDER + "/v_user_shift_aggregated_folder",
                "2021-08-29",
                true);
        verify(ytService, times(1)).export(
                List.of(),
                YtVUserShiftAggregatedDto.class,
                null,
                FOLDER + "/v_user_shift_aggregated_folder",
                "2021-08-30",
                true);
        verify(ytService, times(1)).export(
                List.of(),
                YtVUserShiftAggregatedDto.class,
                null,
                FOLDER + "/v_user_shift_aggregated_folder",
                "2021-08-31",
                true);
        verify(ytService, times(1)).export(
                List.of(),
                YtVUserShiftAggregatedDto.class,
                null,
                FOLDER + "/v_user_shift_aggregated_folder",
                "2021-09-01",
                true);
        verifyNoMoreInteractions(ytService);
    }
}
