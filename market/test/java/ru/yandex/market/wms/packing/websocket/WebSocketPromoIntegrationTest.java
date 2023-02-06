package ru.yandex.market.wms.packing.websocket;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.dao.entity.LotLocIdKey;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.dto.CheckParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseParcelRequest;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse.CreateSorterOrderState;
import ru.yandex.market.wms.packing.dto.PackingHintsDTO;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.utils.PackingAssertion;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.wms.packing.utils.TestCollectionUtils.head;
import static ru.yandex.market.wms.packing.utils.TestCollectionUtils.tail;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock"})
public class WebSocketPromoIntegrationTest extends PackingIntegrationTest {

    private static final String USER = "TEST";

    @Autowired
    private PackingAssertion assertion;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/promo/normal/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/promo/normal/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void promoFlow() throws Exception {
        String orderKey = "ORD0777";

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_PROMO_1);

        PackingTask task = socket.getTask();
        assertThat(task.getOrderTasks()).hasSize(1);
        assertThat(task.getOrderTasks().get(0).getOrderKey()).isEqualTo(orderKey);
        assertThat(task.getHints()).containsExactlyInAnyOrder(
                PackingHintsDTO.builder().code("CARRIER_HINT_1").data(Collections.emptyMap()).build(),
                PackingHintsDTO.builder().code("CARRIER_HINT_3").data(Collections.emptyMap()).build());

        Map<LotLocIdKey, Integer> items = task.getOrderTasks().get(0).getItems().stream()
                .collect(Collectors.toMap(i -> i.getLotLocIdKey(), i -> i.getQty().intValueExact()));
        assertThat(items).isEqualTo(Map.of(
                new LotLocIdKey("LOT1", "PRMPICK1", "PLT1"), 2,
                new LotLocIdKey("LOT32", "PRMPICK2", "PLT2"), 2,
                new LotLocIdKey("LOT33", "PRMPICK2", "PLT3"), 1
        ));

        assertDatabase("db/integration/websocket/promo/normal/after_get_task.xml");
        assertion.assertUserHasPromoTask(USER, orderKey);
        long ticketId = task.getTicket().getTicketId();

        List<String> uits = List.of("UID0001", "UID0002");
        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                "YMA",
                "P000000501",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false);
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId("YMA")
                .printer("P01")
                .uids(List.of("UID0001", "UID0002"))
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order, "P000000501", 1, false);
        assertDatabase("db/integration/websocket/promo/normal/after_parcel1.xml");

        uits = List.of("UID0011", "UID0016", "UID0012");
        socket.scanFirstItemIntoParcel(ticketId, head(uits), "YMA", "P000000502", false);
        socket.scanWrongItem(ticketId, "UID0007"); // другая партия
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000502")
                .recommendedCartonId("YMA")
                .selectedCartonId("YMB")
                .printer("P01")
                .uids(uits)
                .build();

        // UID0011 2 раза
        socket.closeParcelWithError(parcelRequest.toBuilder().uids(List.of("UID0011", "UID0016", "UID0011")).build());
        // UID0016 и UID0017 из партии, из которой нужна только 1 штука
        socket.closeParcelWithError(parcelRequest.toBuilder().uids(List.of("UID0011", "UID0016", "UID0017")).build());
        // UID0007 из другой партии
        socket.closeParcelWithError(parcelRequest.toBuilder().uids(List.of("UID0011", "UID0016", "UID0007")).build());

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        assertion.assertParcelLabel(order, "P000000502", 2, true);

        assertion.assertUserIsIdle(USER, LocationsRov.TABLE_PROMO_1);
        socket.disconnect();
    }

    /**
     * Отличие от исходного состояния только в статусе заказа (-1) и истории статусов
     */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/promo/normal/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/promo/normal/expected_get_task_and_rejected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void getAndRejectTask() throws Exception {
        String orderKey = "ORD0777";

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_PROMO_1);

        PackingTask task = socket.getTask();
        assertThat(task.getOrderTasks()).hasSize(1);
        assertThat(task.getOrderTasks().get(0).getOrderKey()).isEqualTo(orderKey);
        assertDatabase("db/integration/websocket/promo/normal/after_get_task.xml");
        assertion.assertUserHasPromoTask(USER, orderKey);

        socket.disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/promo/normal/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/promo/normal/expected_first_parcel_and_reject.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void packFirstParcelAndRejectTask() throws Exception {
        String orderKey = "ORD0777";

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_PROMO_1);

        PackingTask task = socket.getTask();
        assertThat(task.getOrderTasks()).hasSize(1);
        assertThat(task.getOrderTasks().get(0).getOrderKey()).isEqualTo(orderKey);
        assertDatabase("db/integration/websocket/promo/normal/after_get_task.xml");
        assertion.assertUserHasPromoTask(USER, orderKey);

        long ticketId = task.getTicket().getTicketId();
        List<String> uits = List.of("UID0001", "UID0002");
        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                "YMA",
                "P000000501",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false);
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId("YMA")
                .printer("P01")
                .uids(List.of("UID0001", "UID0002"))
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order, "P000000501", 1, false);
        assertDatabase("db/integration/websocket/promo/normal/after_parcel1.xml");

        socket.disconnect();
    }

}
