package ru.yandex.market.crm.platform.reader.http.controllers.custom;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.platform.services.config.ConfigRepository;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.models.OrderMonitorings;
import ru.yandex.market.crm.platform.reader.test.ControllerTestConfig;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.platform.test.yt.FactsSaver;
import ru.yandex.market.crm.platform.yt.YtTables;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebAppConfiguration
@ContextConfiguration(classes = ControllerTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class OrderMonitoringsControllerTest {

    private static final String ORDER_MONITORINGS = "OrderMonitorings";

    @Inject
    private FactsSaver factsSaver;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private ConfigRepository configs;

    @Inject
    private ExecutorService asyncTasksExecutor;

    @Inject
    private YtSchemaTestUtils schemaTestUtils;

    @Inject
    private HttpEnvironment httpEnvironment;

    @Inject
    private MockMvc mockMvc;

    @Inject
    private YtClient ytClient;

    @Inject
    private YtTables ytTables;

    private FactConfig factConfig;

    @Before
    public void setUp() {
        factConfig = configRepository.getFact(ORDER_MONITORINGS);
        schemaTestUtils.prepareFactTable(factConfig);
    }

    @After
    public void after() {
        httpEnvironment.tearDown();
        schemaTestUtils.removeCreated();
    }

    /**
     * Все мониторинги сохранены, если в бд их нет.
     */
    @Test
    public void allNewMonitoringsAreSavedTest() throws Exception {
        doRequest();
        assertEquals(3, getSaved().size());
    }

    /**
     * Все батчи обработаны.
     */
    @Test
    public void allBatchesAreConsideredAndTheSameNotSaved() throws Exception {
        factsSaver.save(
                factConfig,
                OrderMonitorings.newBuilder()
                        .setOrderId(1)
                        .addMonitoring("DELAYED_DELIVERY_START")
                        .addMonitoring("DELAYED_PACKAGING")
                        .build()
        );

        factsSaver.save(
                factConfig,
                OrderMonitorings.newBuilder()
                        .setOrderId(2)
                        .addMonitoring("DELAYED_DELIVERY_START")
                        .build()
        );

        factsSaver.save(
                factConfig,
                OrderMonitorings.newBuilder()
                        .setOrderId(3)
                        .addMonitoring("DELAYED_DELIVERY_START")
                        .build()
        );

        doRequest();
        assertEquals(3, getSaved().size());
    }

    /**
     * Сохраняем только те мониторинги, которые отличаются от сохраненных.
     */
    @Test
    public void newMonitoringIsSavedIfNotPresentInStoredTest() throws Exception {
        factsSaver.save(
                factConfig,
                OrderMonitorings.newBuilder()
                        .setOrderId(1)
                        .addMonitoring("DELAYED_DELIVERY_START")
                        .addMonitoring("DELAYED_PACKAGING")
                        .build(),
                OrderMonitorings.newBuilder()
                        .setOrderId(2)
                        .addMonitoring("DELAYED_DELIVERY_START")
                        .build()
        );

        doRequest();
        assertEquals(3, getSaved().size());

        OrderMonitorings expected = OrderMonitorings.newBuilder()
                .setOrderId(3)
                .addMonitoring("DELAYED_DELIVERY_START")
                .addUids(Uids.create(UidType.MUID, 1152921504659116909L))
                .addUids(Uids.create(UidType.PUID, 259167221L))
                .addUids(Uids.create(UidType.UUID, "6da818c35b2381e2656d596a842e2c7d"))
                .addUids(Uids.create(UidType.EMAIL, "nursultan@mail.ru"))
                .addUids(Uids.create(UidType.PHONE, "333"))
                .build();

        assertEquals(expected, getSaved(3));
    }

    /**
     * Если актуальный мониторинг по заказу отличается от текущего в бд, сохраняем актуальный.
     */
    @Test
    public void onlyActualMonitoringSavedTest() throws Exception {
        factsSaver.save(
                configs.getFact("OrderMonitorings"),
                // сохранен 1 из 2 типов
                OrderMonitorings.newBuilder()
                        .setOrderId(1)
                        .addMonitoring("DELAYED_PACKAGING")
                        .build(),
                OrderMonitorings.newBuilder()
                        .setOrderId(2)
                        .addMonitoring("DELAYED_DELIVERY_START")
                        .build(),
                // сохранен другой тип
                OrderMonitorings.newBuilder()
                        .setOrderId(3)
                        .addMonitoring("SOME_DIFFERENT_MONITORING")
                        .build()
        );

        doRequest();

        assertEquals(3, getSaved().size());

        OrderMonitorings savedOrder1 = getSaved(1);
        assertEquals(2, savedOrder1.getMonitoringCount());
        assertEquals("DELAYED_DELIVERY_START", savedOrder1.getMonitoring(0));
        assertEquals("DELAYED_PACKAGING", savedOrder1.getMonitoring(1));

        OrderMonitorings savedOrder3 = getSaved(3);
        assertEquals(1, savedOrder3.getMonitoringCount());
        assertEquals("DELAYED_DELIVERY_START", savedOrder3.getMonitoring(0));
    }

    /**
     * Сохраняется пустой мониторинг для погашения, если его нет в списке пришедших в контроллер.
     */
    @Test
    public void monitoringTurnOffTest() throws Exception {
        factsSaver.save(
                factConfig,
                OrderMonitorings.newBuilder()
                        .setOrderId(4)
                        .addMonitoring("DELAYED_DELIVERY_START")
                        .build()
        );
        doRequest();

        List<OrderMonitorings> newState = getSaved();
        assertEquals(3, newState.size());
        assertTrue(newState.stream().noneMatch(m -> m.getOrderId() == 4));
    }

    private List<OrderMonitorings> getSaved() {
        YPath path = ytTables.getFactTable(ORDER_MONITORINGS);

        UnversionedRowset rowset = ytClient.selectRows("* FROM [" + path + "]").join();

        return rowset.getYTreeRows().stream()
                .map(row -> row.getBytes("fact"))
                .map(bytes -> {
                    try {
                        return OrderMonitorings.parseFrom(bytes);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private void doRequest() throws Exception {
        byte[] csv = IOUtils.toByteArray(getClass().getResource("monitorings.csv"));

        ResultActions actions = mockMvc.perform(
                post("/custom/order/monitorings")
                        .contentType("text/csv")
                        .content(csv)
        );

        MvcResult result = actions.andReturn();
        if (result.getRequest().isAsyncStarted()) {
            mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(result));
        }

        asyncTasksExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Nonnull
    private OrderMonitorings getSaved(long id) {
        return getSaved().stream()
                .filter(o -> o.getOrderId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No monitoring with id = " + id + " was saved"));
    }
}
