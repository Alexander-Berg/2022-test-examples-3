package ru.yandex.market.tpl.billing.service.yt.exports;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.billing.config.YtExportConfig;
import ru.yandex.market.tpl.billing.dao.PickupPointDao;
import ru.yandex.market.tpl.billing.model.yt.YtPickupPointDto;
import ru.yandex.market.tpl.billing.service.yt.YtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для бина {@link YtExportConfig#ytPickupPointExportService()}
 */
// TODO: Replace with the DBUnit test (https://st.yandex-team.ru/MARKETTPLBILL-82)
@ExtendWith(SpringExtension.class)
public class YtPickupPointExportTest {

    @Mock
    PickupPointDao mockPickupPointDao;
    @Mock
    private YtService mockYtService;

    private static String FOLDER = "pickup_point";
    private static String FOLDER_NAME = "pickup_point_folder";

    @Test
    void testExport() {
        doReturn(List.of()).when(mockPickupPointDao).getYtData(any(LocalDate.class));
        YtBillingExportService<YtPickupPointDto> ytPickupPointTariffDtoExportService =
                new YtBillingExportService<>(
                        mockYtService,
                        mockPickupPointDao,
                        FOLDER,
                        FOLDER_NAME
                );

        when(mockPickupPointDao.getDataClass()).thenReturn(YtPickupPointDto.class);

        ytPickupPointTariffDtoExportService.exportForDate(
                LocalDate.of(2021, 10, 3),
                true
        );

        verify(mockYtService, times(1))
                .export(List.of(),
                        YtPickupPointDto.class,
                        null,
                        FOLDER + "/" + FOLDER_NAME,
                        "2021-10-03",
                        true);
        verifyNoMoreInteractions(mockYtService);
    }
}
