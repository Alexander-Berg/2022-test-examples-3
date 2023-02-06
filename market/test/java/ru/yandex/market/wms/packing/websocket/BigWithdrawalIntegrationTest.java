package ru.yandex.market.wms.packing.websocket;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.utils.PackingFlow;
import ru.yandex.market.wms.packing.utils.PackingTaskDataset;
import ru.yandex.market.wms.packing.utils.Parcel;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock", "warehouse-timezone = Europe/Moscow"}
)
public class BigWithdrawalIntegrationTest extends PackingIntegrationTest {

    private static final String USER = "TEST";

    @Test
    @DirtiesContext
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/big_withdrawal/setup_fit.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/big_withdrawal/expected_fit.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void packBigWithdrawal() throws Exception {
        PackingFlow flow = createPackingFlow().connect(USER, LocationsRov.NONSORT_TABLE_2);

        Parcel parcel = Parcel.builder()
                .orderKey("ORD0777")
                .parcelId("P000000501")
                .parcelNumber(1)
                .isLast(true)
                .carton("YMA")
                .uits(List.of("UID0001", "UID0002", "UID0003"))
                .shouldCloseParcel(false) // should not close withdrawal parcels automatically
                .build();

        flow.packBigWithdrawal("CART101", new PackingTaskDataset(List.of(parcel)),
                false, true);

        flow.disconnect();
    }

    @Test
    @DirtiesContext
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/big_withdrawal/setup_plan_util.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/big_withdrawal/expected_plan_util.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void packBigWithdrawalPlanUtil() throws Exception {
        PackingFlow flow = createPackingFlow().connect(USER, LocationsRov.NONSORT_TABLE_2);

        Parcel parcel = Parcel.builder()
                .orderKey("ORD0777")
                .uits(List.of("UID0001", "UID0002", "UID0003"))
                .build();

        flow.packBigWithdrawalByContainer("CART101", new PackingTaskDataset(List.of(parcel)));

        flow.disconnect();
    }


    @Test
    @DirtiesContext
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/big_withdrawal/setup_plan_util_extra_balance_in_container.xml",
            type = INSERT)
    @ExpectedDatabase(
            value = "/db/integration/websocket/big_withdrawal/expected_plan_util_extra_balance_in_container.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void packBigWithdrawalPlanUtilWithExtraBalances() throws Exception {
        PackingFlow flow = createPackingFlow().connect(USER, LocationsRov.NONSORT_TABLE_2);

        Parcel parcel = Parcel.builder()
                .orderKey("ORD0777")
                .parcelId("P000000501")
                .parcelNumber(1)
                .isLast(true)
                .carton("YMA")
                .uits(List.of("UID0001", "UID0002"))
                .shouldCloseParcel(false) // should not close withdrawal parcels automatically
                .build();

        flow.packBigWithdrawal("CART101", new PackingTaskDataset(List.of(parcel), Set.of("UID0003")),
                false, false);

        flow.disconnect();
    }
}
