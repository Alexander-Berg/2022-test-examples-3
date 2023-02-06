package ru.yandex.market.tpl.core.domain.clientreturn.dbqueue.address;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static ru.yandex.market.tpl.core.dbqueue.model.QueueType.CLARIFY_CLIENT_RETURN_ADDRESS;
import static ru.yandex.market.tpl.core.dbqueue.model.QueueType.CLIENT_RETURN_VERIFY_COORDS;

@RequiredArgsConstructor
class ClientReturnVerifyCoordsProcessingServiceTest extends TplAbstractTest {

    private final DbQueueTestUtil dbQueueTestUtil;
    private final ClientReturnGenerator clientReturnGenerator;

    @BeforeEach
    void init() {
    }


    @Test
    @DisplayName("Проверка, что при создании возврата идет запись в очередь для проверки координат")
    void writeToQueueWhenClientReturnCreated() {
        clientReturnGenerator.generateReturnFromClient();
        dbQueueTestUtil.assertQueueHasSize(CLIENT_RETURN_VERIFY_COORDS, 1);
    }

    @Test
    @DisplayName("Проверка, что отправляется запись в нужную очередь при неверных координатах")
    void writeToQueueWhenWrongGeoPoint() {
        clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPoint.ofLatLon(0, 0))
                        .build())
                .build());
        dbQueueTestUtil.assertQueueHasSize(CLIENT_RETURN_VERIFY_COORDS, 1);
        dbQueueTestUtil.executeAllQueueItems(CLIENT_RETURN_VERIFY_COORDS);
        dbQueueTestUtil.assertQueueHasSize(CLARIFY_CLIENT_RETURN_ADDRESS, 1);
    }
}
