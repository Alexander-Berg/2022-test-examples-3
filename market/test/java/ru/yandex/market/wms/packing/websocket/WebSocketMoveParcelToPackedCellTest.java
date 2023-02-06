package ru.yandex.market.wms.packing.websocket;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.utils.PackingWebsocket;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock"}
)
public class WebSocketMoveParcelToPackedCellTest extends PackingIntegrationTest {

    private static final String USER = "TEST";

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/move_to_packed_cell/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/move_to_packed_cell/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void happyPath() throws Exception {
        final String parcelId = "PARCEL1";
        final String packedLoc = "PACKED1";

        PackingWebsocket socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);

        socket.moveParcelToPackedCell(parcelId, packedLoc);
    }
}
