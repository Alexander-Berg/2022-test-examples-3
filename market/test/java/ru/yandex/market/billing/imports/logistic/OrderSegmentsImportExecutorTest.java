package ru.yandex.market.billing.imports.logistic;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.logistic.dao.OrderSegmentDao;
import ru.yandex.market.billing.imports.logistic.dao.OrderSegmentsYtDao;
import ru.yandex.market.billing.imports.logistic.model.OrderSegment;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.DbOrderDao;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;


class OrderSegmentsImportExecutorTest extends FunctionalTest {

    private static final LocalDateTime TEST_IMPORT_TIME = LocalDateTime.of(2022, 01, 02, 14, 0, 0);

    private static final List<OrderSegment> ORDER_SEGMENT_LIST = List.of(
            new OrderSegment(SegmentStatus.RETURN_ARRIVED, SegmentType.SORTING_CENTER, "2022-01-01T12:00:00+03:00", 1,
                    1, PartnerType.SORTING_CENTER, 1L),
            new OrderSegment(SegmentStatus.RETURN_ARRIVED, SegmentType.COURIER, "2022-01-01T12:00:00+03:00", 1, 2,
                    PartnerType.SORTING_CENTER, 1L),
            new OrderSegment(SegmentStatus.RETURN_ARRIVED, SegmentType.SORTING_CENTER, "2022-01-01T12:00:00+03:00", 2,
                    3, PartnerType.SORTING_CENTER, 1L),
            new OrderSegment(SegmentStatus.RETURN_ARRIVED, SegmentType.COURIER, "2022-01-01T12:00:00+03:00", 2, 4,
                    PartnerType.SORTING_CENTER, 1L),
            // следующие две строки добавятся
            new OrderSegment(SegmentStatus.RETURN_ARRIVED, SegmentType.POST, "2022-01-01T12:00:00+03:00", 1, 5,
                    PartnerType.SORTING_CENTER, 1L),
            new OrderSegment(SegmentStatus.RETURN_ARRIVED, SegmentType.POST, "2022-01-01T12:00:00+03:00", 2, 6,
                    PartnerType.SORTING_CENTER, 1L),
            // следующие две строки заменят информацию во второй и четвертой соответственно
            new OrderSegment(SegmentStatus.RETURN_ARRIVED, SegmentType.COURIER, "2022-01-01T12:00:00+03:00", 1, 7,
                    PartnerType.SUPPLIER, 1L),
            new OrderSegment(SegmentStatus.RETURN_ARRIVED, SegmentType.COURIER, "2022-01-01T12:00:00+03:00", 2, 8,
                    PartnerType.SUPPLIER, 1L)
    );

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private DbOrderDao dbOrderDao;
    @Autowired
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;
    @Mock
    private OrderSegmentsYtDao orderSegmentsYtDao;

    private OrderSegmentsImportExecutor orderSegmentsImportExecutor;

    @BeforeEach
    public void init() {
        OrderSegmentDao orderSegmentDao = new OrderSegmentDao(pgNamedParameterJdbcTemplate,
                Clock.fixed(TEST_IMPORT_TIME.atZone(DateTimes.MOSCOW_TIME_ZONE).toInstant(), DateTimes.MOSCOW_TIME_ZONE)
        );
        OrderSegmentsImportService orderSegmentsImportService =
                new OrderSegmentsImportService(orderSegmentsYtDao, orderSegmentDao, dbOrderDao);
        orderSegmentsImportExecutor = new OrderSegmentsImportExecutor(orderSegmentsImportService, environmentService);
        Mockito.when(orderSegmentsYtDao.getSegmentsFromYt(eq(3L), any(), any())).thenReturn(ORDER_SEGMENT_LIST);
    }


    @Test
    @DisplayName("Перезапись уже существующих сегментов")
    @DbUnitDataSet(
            before = "OrderSegmentsImportExecutorTest.before.csv",
            after = "OrderSegmentsImportExecutorTest.after.csv"
    )
    public void testAlreadyCreatedSegments() {
        orderSegmentsImportExecutor.doJob();
    }
}
