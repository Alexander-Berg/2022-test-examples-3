package ru.yandex.market.pvz.core.domain.returns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.dispatch.model.ActType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReturnRequestReportServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TestableClock clock;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestReturnRequestFactory returnRequestFactory;
    private final ReturnRequestReportService requestReportService;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.EPOCH, ZoneId.systemDefault());
    }

    @Test
    @SneakyThrows
    void createReturnRequestClientAct() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var returnRequest = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(TestReturnRequestFactory.ReturnRequestTestParams.builder()
                                .items(List.of(TestReturnRequestFactory.ReturnRequestItemTestParams.builder().
                                        operatorComment("TEST")
                                        .build()))
                                .build())
                        .build());
        var pickupPointData = new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(),
                pickupPoint.getName(), 0L, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        var returnRequestAct = requestReportService.generateAct(
                returnRequest.getReturnId(), pickupPointData, ActType.CLIENT_RETURN);

        assertThat(buildExpectedAct(returnRequest))
                .isEqualToIgnoringWhitespace(MAPPER.writeValueAsString(returnRequestAct));
    }

    private String buildExpectedAct(ReturnRequestParams returnRequest) throws IOException {
        var returnActJson = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("json/return_request_client_act.json")), StandardCharsets.UTF_8);
        return String.format(returnActJson, returnRequest.getOrderId(), returnRequest.getReturnId(),
                returnRequest.getReturnId(), returnRequest.getOrderId(), returnRequest.getOrderId()
        );
    }
}
