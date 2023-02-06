package ru.yandex.market.wms.packing.websocket;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.model.enums.EmptyToteAction;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.utils.PackingFlow;
import ru.yandex.market.wms.packing.utils.PackingTaskDataset;
import ru.yandex.market.wms.packing.utils.Parcel;
import ru.yandex.market.wms.transportation.client.TransportationClient;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock"}
)
public class SplitOrderPackingTest extends PackingIntegrationTest {

    private static final String USER1 = "TEST1";
    private static final String USER2 = "TEST2";
    private static final String USER3 = "TEST3";
    private static final String USER4 = "TEST4";

    @MockBean
    @Autowired
    private TransportationClient transportationClient;

    /**
     * 4 части одного разделенного клиентского заказа упаковываются на разных столах паковки
     */
    @Test
    @DirtiesContext
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/combined/split4/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/combined/split4/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void packSplitOrder() throws Exception {
        Mockito.when(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.NO_ACTION);

        var flow1 = createPackingFlow().connect(USER1, LocationsRov.TABLE_1);
        var flow2 = createPackingFlow().connect(USER2, LocationsRov.NONSORT_TABLE_1);
        var flow3 = createPackingFlow().connect(USER3, LocationsRov.NONSORT_TABLE_2);
        var flow4 = createPackingFlow().connect(USER4, LocationsRov.TABLE_2);

        flow1.packSortable(new PackingTaskDataset(List.of(
                Parcel.builder().orderKey("ORD0777").parcelId("P000000501").parcelNumber(1)
                        .carton("YMA").uits(List.of("UID0001", "UID0002")).shouldCloseParcel(false).build()
        )));
        flow2.packNonsort("CART101", new PackingTaskDataset(List.of(
                Parcel.builder().orderKey("ORD0778").parcelId("P000000502").parcelNumber(2)
                        .carton("NONPACK").uits(List.of("UID0003"))
                        .shouldCloseParcel(true).build() // oversize parcels should be closed automatically
        )));
        flow3.packNonsort(new PackingTaskDataset(List.of(
                Parcel.builder().orderKey("ORD0779").parcelId("P000000503").parcelNumber(3)
                        .carton("NONPACK").uits(List.of("UID0005")).shouldCloseParcel(true).build()
        )));
        flow2.packNonsort("CART102", new PackingTaskDataset(List.of(
                Parcel.builder().orderKey("ORD0778").parcelId("P000000504").parcelNumber(4)
                        .carton("NONPACK").uits(List.of("UID0004")).shouldCloseParcel(true).build()
        )));
        flow4.packSortable(new PackingTaskDataset(List.of(
                Parcel.builder().orderKey("ORD0780").parcelId("P000000505").parcelNumber(5)
                        .carton("YMA").uits(List.of("UID0006")).shouldCloseParcel(false).build(),
                Parcel.builder().orderKey("ORD0780").parcelId("P000000506").parcelNumber(6).isLast(true)
                        .carton("YMA").uits(List.of("UID0007")).shouldCloseParcel(false).build()
        )));

        List.of(flow1, flow2, flow3, flow4).forEach(PackingFlow::disconnect);
    }
}
