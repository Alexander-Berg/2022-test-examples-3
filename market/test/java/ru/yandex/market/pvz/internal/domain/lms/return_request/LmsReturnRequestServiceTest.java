package ru.yandex.market.pvz.internal.domain.lms.return_request;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestQueryService;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnStatus;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.domain.lms.return_request.dto.LmsReturnRequestFilterDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LmsReturnRequestServiceTest {

    private final TestReturnRequestFactory returnRequestFactory;
    private final LmsReturnRequestService lmsReturnRequestService;
    private final ReturnRequestQueryService requestQueryService;
    private final PickupPointQueryService pickupPointQueryService;


    @Test
    void getReturnRequestsByOrderId() {
        var returnRequestParams = returnRequestFactory.createReturnRequest();
        var pvzMarketId = pickupPointQueryService.getHeavy(returnRequestParams.getPickupPointId()).getPvzMarketId();

        GridData data = lmsReturnRequestService.getReturnRequests(LmsReturnRequestFilterDto.builder()
                .externalId(returnRequestParams.getOrderId())
                .build(), Pageable.unpaged());

        List<GridItem> items = data.getItems();
        assertThat(items.size()).isEqualTo(1);
        assertThat(items.get(0).getValues()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "externalId", returnRequestParams.getOrderId(),
                "pvzMarketId", pvzMarketId,
                "status", returnRequestParams.getStatus().getDescription(),
                "returnId", returnRequestParams.getReturnId()
        ));
    }

    @Test
    void getReturnRequestsByReturnRequestId() {
        var returnRequestParams = returnRequestFactory.createReturnRequest();
        var pvzMarketId = pickupPointQueryService.getHeavy(returnRequestParams.getPickupPointId()).getPvzMarketId();

        GridData data = lmsReturnRequestService.getReturnRequests(LmsReturnRequestFilterDto.builder()
                .returnId(returnRequestParams.getReturnId())
                .build(), Pageable.unpaged());

        List<GridItem> items = data.getItems();
        assertThat(items.size()).isEqualTo(1);
        assertThat(items.get(0).getValues()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "externalId", returnRequestParams.getOrderId(),
                "pvzMarketId", pvzMarketId,
                "status", returnRequestParams.getStatus().getDescription(),
                "returnId", returnRequestParams.getReturnId()
        ));
    }

    @Test
    void revertReceive() {
        var id = returnRequestFactory.createReturnRequest().getId();
        lmsReturnRequestService.revertReceive(id);
        assertStatus(id, ReturnStatus.NEW);
    }

    @Test
    void expire() {
        var id = returnRequestFactory.createReturnRequest().getId();
        lmsReturnRequestService.expire(id);
        assertStatus(id, ReturnStatus.EXPIRED);
    }

    private void assertStatus(Long id, ReturnStatus status) {
        var returnRequestParams = requestQueryService.getById(id);
        assertEquals(returnRequestParams.getStatus(), status);
        assertThat(returnRequestParams.getArrivedAt()).isNull();
    }
}
