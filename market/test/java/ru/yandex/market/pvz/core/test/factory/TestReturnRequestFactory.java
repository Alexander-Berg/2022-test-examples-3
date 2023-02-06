package ru.yandex.market.pvz.core.test.factory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestCommandService;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestQueryService;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestRepository;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnClientType;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnStatus;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnType;
import ru.yandex.market.pvz.core.test.factory.mapper.ReturnRequestTestParamsMapper;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

/**
 * @author valeriashanti
 * @date 1/28/21
 */

@Transactional
public class TestReturnRequestFactory {

    public static final String BARCODE_PREFIX = "VOZVRAT_SF_PVZ_";

    @Autowired
    private TestPickupPointFactory pickupPointFactory;

    @Autowired
    private ReturnRequestCommandService returnRequestCommandService;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private ReturnRequestQueryService returnRequestQueryService;

    @Autowired
    private ReturnRequestTestParamsMapper requestTestParamsMapper;

    public ReturnRequestParams createReturnRequest() {
        return createReturnRequest(CreateReturnRequestBuilder.builder().build());
    }

    public ReturnRequestParams createReturnRequest(CreateReturnRequestBuilder builder) {
        if (builder.getPickupPoint() == null) {
            builder.setPickupPoint(pickupPointFactory.createPickupPoint());
        }
        ReturnRequestTestParams params = builder.getParams();
        params.setPickupPointId(builder.getPickupPoint().getId());
        if (params.getBarcode() == null) {
            params.setBarcode(BARCODE_PREFIX + params.getReturnId());
        }
        return returnRequestCommandService.create(requestTestParamsMapper.mapParams(params));
    }

    public ReturnRequestParams receiveReturnRequest(String returnId) {
        var returnRequest = returnRequestRepository.findByReturnId(returnId);
        return returnRequestCommandService.receive(returnId, returnRequest.get().getPickupPointId());
    }

    public ReturnRequestParams createReceivedReturn(PickupPoint pickupPoint) {
        var returnRequest = createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build());
        return receiveReturnRequest(returnRequest.getReturnId());
    }

    public ReturnRequestParams dispatchReturnRequest(String returnId) {
        var returnRequest = returnRequestRepository.findByReturnId(returnId);
        receiveReturnRequest(returnId);
        returnRequestCommandService.dispatch(List.of(returnRequest.get().getId()));
        return returnRequestQueryService.getByReturnId(returnId);
    }

    @Data
    @Builder
    public static class CreateReturnRequestBuilder {

        @Builder.Default
        private ReturnRequestTestParams params = ReturnRequestTestParams.builder().build();

        private PickupPoint pickupPoint;
    }

    @Data
    @Builder
    public static class ReturnRequestTestParams {

        public static final String DEFAULT_BUYER_NAME = "Райгородский Андрей Михайлович";
        public static final ReturnClientType DEFAULT_RETURN_CLIENT_TYPE = ReturnClientType.CLIENT;
        public static final ReturnStatus DEFAULT_RETURN_STATUS = ReturnStatus.NEW;
        public static final LocalDate DEFAULT_REQUEST_DATE = LocalDate.of(2020, 4, 1);
        public static final OffsetDateTime DEFAULT_ARRIVED_AT =
                DateTimeUtil.atStartOfDayWithOffset(DEFAULT_REQUEST_DATE);
        public static final List<ReturnRequestItemTestParams> DEFAULT_ITEMS =
                List.of(ReturnRequestItemTestParams.builder().build());

        private Long id;

        @Builder.Default
        private String returnId = "fake_" + RandomUtils.nextLong();

        @Builder.Default
        private String externalOrderId = String.valueOf(RandomUtils.nextLong());

        @Builder.Default
        private String buyerName = DEFAULT_BUYER_NAME;

        @Builder.Default
        private ReturnClientType clientType = DEFAULT_RETURN_CLIENT_TYPE;

        @Builder.Default
        private LocalDate requestDate = DEFAULT_REQUEST_DATE;

        @Builder.Default
        private ReturnStatus status = DEFAULT_RETURN_STATUS;

        private OffsetDateTime arrivedAt;

        private OffsetDateTime dispatchedAt;

        @Builder.Default
        private List<ReturnRequestItemTestParams> items = DEFAULT_ITEMS;

        private Long pickupPointId;

        private String barcode;

    }

    @Data
    @Builder
    public static class ReturnRequestItemTestParams {

        public static final String DEFAULT_NAME = "Комбинаторика и теория вероятностей";
        public static final ReturnType DEFAULT_RETURN_TYPE = ReturnType.WRONG;
        public static final String DEFAULT_RETURN_REASON = "Катарсис не случился (((";
        public static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(100);
        public static final Long DEFAULT_COUNT = 3L;

        private Long returnRequestId;

        @Builder.Default
        private String name = DEFAULT_NAME;

        @Builder.Default
        private ReturnType returnType = DEFAULT_RETURN_TYPE;

        @Builder.Default
        private String returnReason = DEFAULT_RETURN_REASON;

        @Builder.Default
        private BigDecimal price = DEFAULT_PRICE;

        @Builder.Default
        private Long count = DEFAULT_COUNT;

        private String operatorComment;
    }

}
