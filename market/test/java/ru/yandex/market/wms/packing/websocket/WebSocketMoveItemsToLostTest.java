package ru.yandex.market.wms.packing.websocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.async.MoveToLostProducer;
import ru.yandex.market.wms.packing.dto.MoveItemsToLostResponse;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.utils.PackingWebsocket;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock"}
)
public class WebSocketMoveItemsToLostTest extends PackingIntegrationTest {
    @MockBean
    @Autowired
    private MoveToLostProducer moveToLostProducer;
    private static final String USER = "TEST";

    @BeforeEach
    public void setUp() {
        reset(moveToLostProducer);
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/lost/sortable/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/lost/sortable/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testPackingSortableMoveToLost() throws Exception {
        PackingWebsocket socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        List<String> serialNumbers = List.of("UID0001", "UID0002");
        socket.moveItemsToLost(task.getTicket().getTicketId(), serialNumbers,
                new MoveItemsToLostResponse(Collections.emptySet()));
        verify(moveToLostProducer, atLeastOnce()).produce(eq(new HashSet<>(serialNumbers)), eq(USER));
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/lost/sortable/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/lost/sortable/after-partial.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testPackingSortableMoveToLostAndRemoveFromWavePartial() throws Exception {
        PackingWebsocket socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        List<String> serialNumbers = List.of("UID0001");
        socket.moveItemsToLost(task.getTicket().getTicketId(), serialNumbers,
                new MoveItemsToLostResponse(Set.of("CELL1")));
        verify(moveToLostProducer, atLeastOnce())
                .produce(eq(new HashSet<>(serialNumbers)), eq(USER));
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/lost/cons-cart/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/lost/cons-cart/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testPackingConsCartMoveToLost() throws Exception {
        PackingWebsocket socket = createSocket();
        socket.connect(USER, LocationsRov.NONSORT_TABLE_1);
        PackingTask task = socket.getTask("CART01");
        List<String> serialNumbers = List.of("UID0001", "UID0002");
        socket.moveItemsToLost(task.getTicket().getTicketId(), serialNumbers,
                new MoveItemsToLostResponse(Collections.emptySet()));
        verify(moveToLostProducer, atLeastOnce()).produce(eq(new HashSet<>(serialNumbers)), eq(USER));
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/lost/cons-cart/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/lost/cons-cart/after-partial.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testPackingConsCartPartialMoveToLost() throws Exception {
        PackingWebsocket socket = createSocket();
        socket.connect(USER, LocationsRov.NONSORT_TABLE_1);
        PackingTask task = socket.getTask("CART01");
        List<String> serialNumbers = List.of("UID0001");
        socket.moveItemsToLost(task.getTicket().getTicketId(), serialNumbers,
                new MoveItemsToLostResponse(Set.of("CART01")));
        verify(moveToLostProducer, atLeastOnce()).produce(eq(new HashSet<>(serialNumbers)), eq(USER));
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/lost/stuck/nsqlconfig-stuck-enabled.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/lost/stuck/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/lost/stuck/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testPackingStuckMoveToLost() throws Exception {
        PackingWebsocket socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        List<String> serialNumbers = List.of("UID0001", "UID0002");
        socket.moveItemsToLost(task.getTicket().getTicketId(), serialNumbers,
                new MoveItemsToLostResponse(Collections.emptySet()));
        verify(moveToLostProducer, atLeastOnce()).produce(eq(new HashSet<>(serialNumbers)), eq(USER));
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/lost/stuck/nsqlconfig-stuck-enabled.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/lost/stuck/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/lost/stuck/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testPackingStuckPartialMoveToLost() throws Exception {
        PackingWebsocket socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        List<String> serialNumbers = List.of("UID0001");
        socket.moveItemsToLost(task.getTicket().getTicketId(), serialNumbers,
                new MoveItemsToLostResponse(Set.of("CELL1")));
        verify(moveToLostProducer, atLeastOnce()).produce(eq(new HashSet<>(serialNumbers)), eq(USER));
    }
}
