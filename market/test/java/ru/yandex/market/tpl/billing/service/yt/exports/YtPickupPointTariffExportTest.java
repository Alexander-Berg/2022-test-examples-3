package ru.yandex.market.tpl.billing.service.yt.exports;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.billing.config.YtExportConfig;
import ru.yandex.market.tpl.billing.dao.PickupPointTariffDao;
import ru.yandex.market.tpl.billing.model.yt.YtPickupPointTariffDto;
import ru.yandex.market.tpl.billing.service.yt.YtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для бина {@link YtExportConfig#ytPickupPointTariffExportService()}
 */
@ExtendWith(SpringExtension.class)
public class YtPickupPointTariffExportTest {

    @Mock
    PickupPointTariffDao mockPickupPointTariffDao;
    @Mock
    private YtService mockYtService;

    private static final String FOLDER = "pickup_point";
    private static final String FOLDER_NAME = "pickup_point_tariff_folder";

    @Test
    void testExport() {
        doReturn(List.of()).when(mockPickupPointTariffDao).getYtData(any(LocalDate.class));
        YtBillingExportService<YtPickupPointTariffDto> ytPickupPointTariffDtoExportService =
                new YtBillingExportService<>(
                        mockYtService,
                        mockPickupPointTariffDao,
                        FOLDER,
                        FOLDER_NAME
                );

        when(mockPickupPointTariffDao.getDataClass()).thenReturn(YtPickupPointTariffDto.class);

        ytPickupPointTariffDtoExportService.export(
                LocalDate.of(2021, 10, 1),
                LocalDate.of(2021, 10, 3),
                true
        );

        verify(mockYtService, times(1))
                .export(List.of(),
                        YtPickupPointTariffDto.class,
                        null,
                        FOLDER + "/" + FOLDER_NAME,
                        "2021-10-01",
                        true);
        verify(mockYtService, times(1))
                .export(List.of(),
                        YtPickupPointTariffDto.class,
                        null,
                        FOLDER + "/" + FOLDER_NAME,
                        "2021-10-02",
                        true);
        verify(mockYtService, times(1))
                .export(List.of(),
                        YtPickupPointTariffDto.class,
                        null,
                        FOLDER + "/" + FOLDER_NAME,
                        "2021-10-03",
                        true);
        verifyNoMoreInteractions(mockYtService);
    }
}
