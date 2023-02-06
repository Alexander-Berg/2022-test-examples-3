package ru.yandex.market.tpl.billing.service.yt.exports;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.billing.dao.view.VTransactionDao;
import ru.yandex.market.tpl.billing.model.yt.YtVTransactionDto;
import ru.yandex.market.tpl.billing.service.yt.YtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для бина ytVTransactionExportService
 */
// TODO: Replace with the DBUnit test (https://st.yandex-team.ru/MARKETTPLBILL-82)
@ExtendWith(SpringExtension.class)
public class YtVTransactionExportServiceTest {

    @Mock
    VTransactionDao mockTransactionDao;
    @Mock
    private YtService mockYtService;

    private static final String FOLDER = "PVZ";
    private static final String FOLDER_NAME = "v_transaction_folder";

    @Test
    void testExport() {
        when(mockTransactionDao.getTransactions()).thenReturn(List.of());
        YtBillingExportService<YtVTransactionDto> ytVTransactionExportService = new YtBillingExportService<>(
                mockYtService,
                mockTransactionDao,
                FOLDER,
                FOLDER_NAME
        );
        when(mockTransactionDao.getDataClass()).thenReturn(YtVTransactionDto.class);

        ytVTransactionExportService.export(
                LocalDate.of(2021, Month.AUGUST, 29),
                LocalDate.of(2021, Month.SEPTEMBER, 1),
                true
        );

        verify(mockYtService, times(1))
                .export(List.of(),
                        YtVTransactionDto.class,
                        null,
                        FOLDER + "/v_transaction_folder",
                        "2021-08-29",
                        true);
        verify(mockYtService, times(1))
                .export(List.of(),
                        YtVTransactionDto.class,
                        null,
                        FOLDER + "/v_transaction_folder",
                        "2021-08-30",
                        true);
        verify(mockYtService, times(1))
                .export(List.of(),
                        YtVTransactionDto.class,
                        null,
                        FOLDER + "/v_transaction_folder",
                        "2021-08-31",
                        true);
        verify(mockYtService, times(1))
                .export(List.of(),
                        YtVTransactionDto.class,
                        null,
                        FOLDER + "/v_transaction_folder",
                        "2021-09-01",
                        true);
        verifyNoMoreInteractions(mockYtService);
    }

    @Test
    void testReplaceTariffValue() {
        VTransactionDao vTransactionDao = new VTransactionDao(
                null,
                new ObjectMapper()
        );
        String jsonPayload = /*language=json*/ "{ " +
                "\"tariffs\": [ " +
                "  { " +
                "    \"id\": 28, " +
                "    \"code\": null, " +
                "    \"value\": 100, " +
                "    \"tariff\": { " +
                "      \"id\": 36, " +
                "      \"code\": \"BRANDED_MILLIONNIKI_3\", " +
                "      \"createdAt\": 1619424771.300234, " +
                "      \"description\": \"Выплата за выдачу заказов, полностью брендированные, Тверь, Тула и т.д.\", " +
                "      \"serviceType\": \"PVZ_REWARD\", " +
                "      \"tariffZoneId\": 8, " +
                "      \"pickupPointAgeTo\": null, " +
                "      \"pickupPointAgeFrom\": null, " +
                "      \"forBrandedPickupPoint\": false " +
                "    }, " +
                "    \"toDate\": [ " +
                "      2021, 7, 31 " +
                "    ], " +
                "    \"fromDate\": [ " +
                "      2020, 12, 1 " +
                "    ], " +
                "    \"valueType\": \"ABSOLUTE\", " +
                "    \"serviceType\": null, " +
                "    \"minDayOrders\": 40, " + // заберем это значение, т.к. у него тариф PVZ_REWARD
                "    \"tariffValueType\": null " +
                "  }, " +
                "  { " +
                "    \"id\": 30, " +
                "    \"code\": null, " +
                "    \"value\": 100, " +
                "    \"tariff\": { " +
                "      \"id\": 38, " +
                "      \"code\": \"DROPOFF_BRANDED_MILLIONNIKI_3\", " +
                "      \"createdAt\": 1619424771.300234, " +
                "      \"description\": \"Выплата за дропоффы, полностью брендированные, Тверь, Тула и т.д.\", " +
                "      \"serviceType\": \"PVZ_DROPOFF\", " +
                "      \"tariffZoneId\": 8, " +
                "      \"pickupPointAgeTo\": null, " +
                "      \"pickupPointAgeFrom\": null, " +
                "      \"forBrandedPickupPoint\": false " +
                "    }, " +
                "    \"toDate\": [ " +
                "      2021, 7, 31 " +
                "    ], " +
                "    \"fromDate\": [ " +
                "      2020, 12, 1 " +
                "    ], " +
                "    \"valueType\": \"ABSOLUTE\", " +
                "    \"serviceType\": null, " +
                "    \"minDayOrders\": 50, " +
                "    \"tariffValueType\": null " +
                "  }, " +
                "  { " +
                "    \"id\": 32, " +
                "    \"code\": null, " +
                "    \"value\": 100, " +
                "    \"tariff\": { " +
                "      \"id\": 40, " +
                "      \"code\": \"RETURN_BRANDED_MILLIONNIKI_3\", " +
                "      \"createdAt\": 1619424771.300234, " +
                "      \"description\": \"Выплата за возвраты, полностью брендированные, Тверь, Тула и т.д.\", " +
                "      \"serviceType\": \"PVZ_RETURN\", " +
                "      \"tariffZoneId\": 8, " +
                "      \"pickupPointAgeTo\": null, " +
                "      \"pickupPointAgeFrom\": null, " +
                "      \"forBrandedPickupPoint\": false " +
                "    }, " +
                "    \"toDate\": [ " +
                "      2021, 7, 31 " +
                "    ], " +
                "    \"fromDate\": [ " +
                "      2020, 12, 1 " +
                "    ], " +
                "    \"valueType\": \"ABSOLUTE\", " +
                "    \"serviceType\": null, " +
                "    \"minDayOrders\": 60, " +
                "    \"tariffValueType\": null " +
                "  } " +
                "], " +
                "\"workingDays\": [ " +
                "  [ " +
                "    2021, 7, 31 " +
                "  ] " +
                "], " +
                "\"pickupPointId\": 5801 " +
                "}"; // payload взял с прода, чутка укоротив

        YtVTransactionDto transaction = YtVTransactionDto.builder()
                .payload(jsonPayload)
                .build();
        assertNotNull(transaction.getPayload());

        YtVTransactionDto replacedTransaction = vTransactionDao.replaceTariffValueForPvzRewardAndCommit(
                transaction
        );

        assertEquals(40, transaction.getTariffValue());
        assertEquals(40, replacedTransaction.getTariffValue());
        assertNull(transaction.getPayload());
    }
}
