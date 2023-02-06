package ru.yandex.market.billing.returns;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.returns.model.ReturnRequestStatus;
import ru.yandex.market.billing.returns.model.ReturnRequestStatusHistory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReturnRequestStatusHistoryImportTest extends FunctionalTest {

    private static final LocalDate DATE_2021_10_19 = LocalDate.of(2021, 10, 19);
    private static final DateTimeFormatter YT_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    ReturnRequestStatusHistoryImportService returnRequestStatusHistoryImportService;

    @Mock
    private ReturnRequestStatusHistoryYtDao returnRequestStatusHistoryYtDao;

    @Autowired
    private ReturnRequestStatusHistoryDao oracleReturnRequestStatusHistoryDao;

    @Autowired
    private ReturnRequestStatusHistoryDao pgReturnRequestStatusHistoryDao;

    @Autowired
    private EnvironmentService environmentService;


    private void init() {
        when(returnRequestStatusHistoryYtDao.getReturnRequestsStatusHistory(DATE_2021_10_19))
                .thenReturn(getImportedRequests());

        returnRequestStatusHistoryImportService = new ReturnRequestStatusHistoryImportService(
                returnRequestStatusHistoryYtDao,
                oracleReturnRequestStatusHistoryDao,
                pgReturnRequestStatusHistoryDao,
                environmentService
        );
    }

    @DbUnitDataSet(
            before = "ReturnRequestStatusHistoryImportTest.before.csv",
            after = "ReturnRequestStatusHistoryImportTest.after.csv"
    )
    @DisplayName("Проверяем, что импортируется и не конфликтует со старыми")
    @Test
    void testReturnRequestImport() {
        init();
        returnRequestStatusHistoryImportService.process(DATE_2021_10_19);
    }

    private List<ReturnRequestStatusHistory> getImportedRequests() {
        return List.of(
                ReturnRequestStatusHistory.builder()
                        .setId(1L)
                        .setReturnId(1L)
                        .setDeliveryStatus("DELIVERED")
                        .setDeliveryStatusDate(
                                LocalDateTime.parse("2021-10-18T02:07:21.073812", YT_DATE_TIME_FORMAT)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                        )
                        .build(),
                ReturnRequestStatusHistory.builder()
                        .setId(2L)
                        .setReturnId(2L)
                        .setDeliveryStatus("READY_FOR_PICKUP")
                        .setDeliveryStatusDate(
                                LocalDateTime.parse("2021-10-18T03:07:21.073812", YT_DATE_TIME_FORMAT)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                        )
                        .build()
        );
    }
}
