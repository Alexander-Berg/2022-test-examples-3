package ru.yandex.market.checkout.checkouter.storage.shipment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_ROUTE;

public class ParcelRouteDaoTest extends AbstractWebTestBase {

    @Autowired
    private DSLContext dsl;
    @Autowired
    private ParcelRouteDao routeDao;


    @Test
    public void deleteTest() {
        LocalDateTime pastTime = LocalDateTime.now().minusDays(5);
        LocalDateTime now = LocalDateTime.now();


        insert(pastTime, JSONB.valueOf("{}"));
        insert(pastTime, JSONB.valueOf("{}"));
        UUID routeNowId1 = insert(now, JSONB.valueOf("{}"));

        transactionTemplate.execute(status -> {
            int count = routeDao.deleteBefore(now.minusDays(1), 10);
            assertEquals(2, count);
            return null;
        });

        List<String> routeId = dsl.select(PARCEL_ROUTE.UUID).from(PARCEL_ROUTE).fetch(PARCEL_ROUTE.UUID);

        assertEquals(1, routeId.size());
        assertEquals(routeNowId1.toString(), routeId.get(0));
    }


    private UUID insert(LocalDateTime time, JSONB route) {
        UUID id = UUID.randomUUID();
        transactionTemplate.execute(status -> {
            dsl.insertInto(PARCEL_ROUTE)
                    .set(PARCEL_ROUTE.UUID, id.toString())
                    .set(PARCEL_ROUTE.CREATED_AT, time)
                    .set(PARCEL_ROUTE.ROUTE, route)
                    .execute();
            return null;
        });
        return id;
    }
}
