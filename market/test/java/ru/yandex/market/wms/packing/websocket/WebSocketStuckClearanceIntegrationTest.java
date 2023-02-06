package ru.yandex.market.wms.packing.websocket;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.utils.PackingAssertion;
import ru.yandex.market.wms.packing.utils.PackingWebsocket;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock"})
public class WebSocketStuckClearanceIntegrationTest extends PackingIntegrationTest {

    private static final String USER = "TEST";

    @Autowired
    private PackingAssertion assertion;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/stuck/normal/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/stuck/normal/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void stuckClearanceFlow() throws Exception {
        List<String> stuckUits = Stream.iterate(1, i -> i + 1)
                .limit(10)
                .map(i -> String.format("STUCKUID%04d", i))
                .collect(Collectors.toList());
        String stuckCollectCart = "CART0099";

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();


        // Проверяем, что получили задание на чистку только по товарам из обеих ячеек
        Set<String> uitsPack = new HashSet<>(stuckUits);
        assertion.assertTaskHasCancelledUitsWithNullOrder(task, uitsPack);
        assertion.assertUserHasStuckTask(USER, "SS1");
        moveToCancelledByUitsAndCheckCartBalances(task, uitsPack, stuckCollectCart, socket, true);

        // Получаем второе задание, уже должно быть задание на упаковку, а не на чистку.
        String order = "ORD0777";
        Set<String> uits = Stream.iterate(1, i -> i + 1)
                .limit(20)
                .map(i -> String.format("UID%04d", i))
                .collect(Collectors.toSet());
        task = socket.getTask();
        assertion.assertTaskHasUits(task, Map.of(order, uits));
        assertion.assertUserHasSortableTask(USER, "SS1", order);

        socket.disconnect();
    }

    private void moveToCancelledByUitsAndCheckCartBalances(PackingTask task, Set<String> uitsPack,
                                                           String stuckCollectCart, PackingWebsocket socket,
                                                           boolean useMultiUits) {
        int itemsInCartBefore = getSerialsCountInContainer(stuckCollectCart);
        if (useMultiUits) {
            socket.scanCancelledItems(task.getTicket().getTicketId(), uitsPack, stuckCollectCart);
        } else {
            uitsPack.stream()
                    .forEach(v -> socket.scanCancelledItem(task.getTicket().getTicketId(), v, stuckCollectCart));
        }
        int itemsInCartAfter = getSerialsCountInContainer(stuckCollectCart);
        assertThat(itemsInCartAfter - itemsInCartBefore).isEqualTo(uitsPack.size());
    }

    private int getSerialsCountInContainer(String stuckCollectCart) {
         return jdbc.queryForObject(
                 "SELECT COALESCE(sum(qty),0) as sum FROM wmwhse1.SERIALINVENTORY WHERE ID = :container",
                 new MapSqlParameterSource().addValue("container", stuckCollectCart),
                 BigDecimal.class).intValue();
    }

    /*
     * Идеи для других тестов:
     * - Если задание на очистку зависелось в интерфейсе/кеше, и ячейка уже по факту стала с живым заказом,
     *   то нельзя её товары перемещать в отменёнку
     *
     * - Если у задания на очистку, по неизвестной причине нечего выполнять, хоть только что было много работы
     *   то завершаем задание.
     *
     * - Если задание уже завершено,
     *   то что будет, если попытаться его выполнить или ещё раз завершить?
     *
     * - Есть ли риск попасть на constraint UQ_PACKINGTASK_LOC_ORDERKEY unique (LOC, ORDERKEY, ID),
     *   т.к. для STUCK-заданий и IDLE-заданий будет ORDERKEY=null, ID=null.
     *   В LOC пишется то ли стол упаковки, то ли сортстанция, то ли линия консолидации
     */
}
