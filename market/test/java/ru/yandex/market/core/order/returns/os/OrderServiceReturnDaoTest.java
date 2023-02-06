package ru.yandex.market.core.order.returns.os;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.orderservicereturn.OrderServiceReturnDao;
import ru.yandex.market.billing.imports.orderservicereturn.model.OrderServiceReturn;
import ru.yandex.market.billing.imports.orderservicereturn.model.OrderServiceReturnStatus;
import ru.yandex.market.billing.imports.orderservicereturn.model.OrderServiceReturnType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class OrderServiceReturnDaoTest extends FunctionalTest {
    private static final ZoneId LOCAL_ZONE_ID = TimeZone.getDefault().toZoneId();
    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final Instant IN_TRANSIT_UPDATED_AT = Instant.parse("2022-01-01T08:00:00.00Z");
    private static final Instant READY_FOR_PICKUP_UPDATED_AT = Instant.parse("2022-01-01T10:00:00.00Z");

    @Autowired
    private OrderServiceReturnDao orderServiceReturnDao;

    @BeforeAll
    static void beforeAll() {
        TimeZone.setDefault(DEFAULT_TIME_ZONE);
    }

    @AfterAll
    static void afterAll() {
        TimeZone.setDefault(TimeZone.getTimeZone(LOCAL_ZONE_ID));
    }

    @DbUnitDataSet(after = "OrderServiceReturnDao.persist.after.csv")
    @Test
    @DisplayName("Проверка правильности сохранения данных возвратов")
    public void testPersist() {
        orderServiceReturnDao.persist(build());
    }

    private List<OrderServiceReturn> build() {
        return List.of(
                OrderServiceReturn.builder()
                        .setOrderId(561782L)
                        .setPartnerId(2L)
                        .setCheckouterReturnId(1760969L)
                        .setLogisticReturnId(145434L)
                        .setReturnType(OrderServiceReturnType.RETURN)
                        .setLrlStatus(OrderServiceReturnStatus.IN_TRANSIT)
                        .setLrlEventId(1242L)
                        .setLrlStatusCommittedAt(IN_TRANSIT_UPDATED_AT.minus(10, ChronoUnit.MINUTES))
                        .setUpdatedAt(IN_TRANSIT_UPDATED_AT)
                        .build(),
                OrderServiceReturn.builder()
                        .setOrderId(561782L)
                        .setPartnerId(2L)
                        .setCheckouterReturnId(1760969L)
                        .setLogisticReturnId(145434L)
                        .setReturnType(OrderServiceReturnType.RETURN)
                        .setLrlStatus(OrderServiceReturnStatus.READY_FOR_PICKUP)
                        .setLrlEventId(1243L)
                        .setLrlStatusCommittedAt(
                                READY_FOR_PICKUP_UPDATED_AT.minus(10, ChronoUnit.MINUTES)
                        )
                        .setUpdatedAt(READY_FOR_PICKUP_UPDATED_AT)
                        .build()
        );
    }
}
