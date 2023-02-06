package ru.yandex.market.communication.proxy.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;
import ru.yandex.market.communication.proxy.dao.RealNumberDao;
import ru.yandex.market.communication.proxy.dao.RedirectInfoDao;
import ru.yandex.mj.generated.server.model.CallsFilter;
import ru.yandex.mj.generated.server.model.CallsRequest;
import ru.yandex.mj.generated.server.model.CallsResponse;
import ru.yandex.mj.generated.server.model.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.mj.generated.server.model.CallResolution.NO_ANSWER;


/**
 * @author zilzilok
 */
public class CallsApiTest extends AbstractCommunicationProxyTest {

    private static final Long ORDER_ID = 1L;

    @Autowired
    RedirectInfoDao redirectInfoDao;

    @Autowired
    RealNumberDao realNumberDao;
    @Autowired
    CallsApiService callsApiService;

    @Test
    @DbUnitDataSet(before = "CallsApiTest.before.csv")
    void getCallsTest() {
        ResponseEntity<CallsResponse> response = callsApiService.getCalls(
                new CallsRequest().callsFilter(new CallsFilter())
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        CallsResponse callsResponse = Objects.requireNonNull(response.getBody());
        assertEquals(CallsApiService.DEFAULT_PAGE_NUM, callsResponse.getPageNum());
        assertEquals(1, callsResponse.getPageSize());
        assertEquals(1, callsResponse.getTotalElements());
        assertEquals(1, callsResponse.getTotalPages());
    }

    @Test
    @DbUnitDataSet(before = "CallsApiTest.before.csv")
    void getCallsWithOrderTest() {
        CallsFilter callsFilter = new CallsFilter();
        callsFilter.setOrderId(1L);

        ResponseEntity<CallsResponse> response = callsApiService.getCalls(
                new CallsRequest().callsFilter(callsFilter)
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        CallsResponse callsResponse = Objects.requireNonNull(response.getBody());
        assertEquals(CallsApiService.DEFAULT_PAGE_NUM, callsResponse.getPageNum());
        assertEquals(1, callsResponse.getPageSize());
        assertEquals(1, callsResponse.getTotalElements());
        assertEquals(1, callsResponse.getTotalPages());
    }

    @Test
    @DbUnitDataSet(before = "CallsApiTest.before.csv")
    void getCallsWithoutOrderTest() {
        CallsFilter callsFilter = new CallsFilter();
        callsFilter.setOrderId(2L);
        ResponseEntity<CallsResponse> response = callsApiService.getCalls(
                new CallsRequest().callsFilter(callsFilter)
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DbUnitDataSet(before = "CallsApiTest.before.csv")
    public void returnEmptyListWhenNotLinked() {
        var result = callsApiService.getCalls(createRequest());
        Assertions.assertThat(result.getBody().getCalls()).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "noRealNumberUsed.before.csv")
    public void noRealNumberUsed() {
        var result = callsApiService.getCalls(createRequest());
        Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(result.getBody().getCalls()).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "realNumberUsed.before.csv")
    public void realNumberUsed() {
        var result = callsApiService.getCalls(createRequest());
        Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static CallsRequest createRequest() {
        var filter = new CallsFilter();
        filter.setOrderId(ORDER_ID);
        filter.setCallResolutions(List.of(NO_ANSWER));
        filter.setStartedFrom(OffsetDateTime.now().minusSeconds(10));
        var request = new CallsRequest();
        request.setCallsFilter(filter);
        request.setPageRequest(new PageRequest());
        return request;
    }
}
