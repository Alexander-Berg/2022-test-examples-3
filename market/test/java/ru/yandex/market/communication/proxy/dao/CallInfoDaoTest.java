package ru.yandex.market.communication.proxy.dao;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;
import ru.yandex.mj.generated.server.model.CallInfoDto;
import ru.yandex.mj.generated.server.model.CallResolution;
import ru.yandex.mj.generated.server.model.CallsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author zilzilok
 */
@DbUnitDataSet(before = "CallInfoDaoTest.before.csv")
class CallInfoDaoTest extends AbstractCommunicationProxyTest {
    private static final long ORDER_ID = 32937030L;
    private static final long OTHER_ORDER_ID = 32937031L;

    @Autowired
    private CallInfoDao callInfoDao;

    @Test
    void getPageOfCallsTest() {
        CallsResponse response = callInfoDao.getPageOfCalls(ORDER_ID, List.of(CallResolution.CONNECTED,
                CallResolution.UNAVAILABLE), null, 0, 2);

        assertEquals(0, response.getPageNum());
        assertEquals(2, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getPageSize());

        List<CallInfoDto> calls = response.getCalls();
        assertEquals(2, calls.size());
        assertTrue(calls.stream().filter(c -> c.getOrderId() == OTHER_ORDER_ID).findFirst().isEmpty());

        // 2001-01-01T03:00:00+03:00
        OffsetDateTime expectedStarted = LocalDateTime.of(2001, 1, 1, 3, 0, 0)
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toOffsetDateTime();
        // 2001-01-01T03:01:01+03:00
        OffsetDateTime expectedEnded = LocalDateTime.of(2001, 1, 1, 3, 1, 1)
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toOffsetDateTime();
        CallInfoDto call = calls.stream()
                .filter(c -> c.getResolution() == CallResolution.CONNECTED)
                .findFirst()
                .orElseThrow();
        assertEquals(ORDER_ID, call.getOrderId());
        assertEquals(CallResolution.CONNECTED, call.getResolution());
        assertEquals(expectedStarted, call.getStarted());
        assertEquals(expectedEnded, call.getEnded());
        assertEquals("03d17e5a-9e1b-bac1-1b60-8247c7f00000", call.getRecordId());
    }

    @Test
    void getPageOfCallsStartedFromTest() {
        // 2001-01-01 04:00:00 MSK
        OffsetDateTime startedFrom = LocalDateTime.of(2001, 1, 1, 4, 0, 0)
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toOffsetDateTime();
        assertEquals(1, callInfoDao.getPageOfCalls(null, null, startedFrom, 0, 4).getTotalElements());

        // 2001-01-01 03:00:00 MSK
        startedFrom = LocalDateTime.of(2001, 1, 1, 3, 0, 0)
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toOffsetDateTime();
        assertEquals(4, callInfoDao.getPageOfCalls(null, null, startedFrom, 0, 4).getTotalElements());

        // 2001-01-01 01:00:00 UTC
        startedFrom = OffsetDateTime.of(LocalDateTime.of(2001, 1, 1, 1, 0, 0), ZoneOffset.UTC);
        assertEquals(1, callInfoDao.getPageOfCalls(null, null, startedFrom, 0, 4).getTotalElements());

        // 2001-01-01 00:00:00 UTC
        startedFrom = OffsetDateTime.of(LocalDateTime.of(2001, 1, 1, 0, 0, 0), ZoneOffset.UTC);
        assertEquals(4, callInfoDao.getPageOfCalls(null, null, startedFrom, 0, 4).getTotalElements());
    }

    @Test
    void getPageOfCallsWithoutResolutionTest() {
        CallsResponse response = callInfoDao.getPageOfCalls(ORDER_ID, null, null, 0, 3);
        assertEquals(0, response.getPageNum());
        assertEquals(3, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(3, response.getPageSize());
        assertEquals(3, response.getCalls().size());

        response = callInfoDao.getPageOfCalls(ORDER_ID, null, null, 0, 1);
        assertEquals(0, response.getPageNum());
        assertEquals(3, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(1, response.getPageSize());
        assertEquals(1, response.getCalls().size());

        response = callInfoDao.getPageOfCalls(ORDER_ID, null, null, 1, 1);
        assertEquals(1, response.getPageNum());
        assertEquals(3, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(1, response.getPageSize());
        assertEquals(1, response.getCalls().size());

        response = callInfoDao.getPageOfCalls(ORDER_ID, null, null, 3, 1);
        assertEquals(3, response.getPageNum());
        assertEquals(3, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(0, response.getPageSize());
        assertEquals(0, response.getCalls().size());
    }

    @Test
    void getPageOfCallsWithoutOrderIdTest() {
        CallsResponse response = callInfoDao.getPageOfCalls(null, null, null, 0, 4);
        assertEquals(4, response.getCalls().size());

        response = callInfoDao.getPageOfCalls(null, List.of(CallResolution.CONNECTED), null, 0, 4);
        assertEquals(1, response.getCalls().size());
    }
}
