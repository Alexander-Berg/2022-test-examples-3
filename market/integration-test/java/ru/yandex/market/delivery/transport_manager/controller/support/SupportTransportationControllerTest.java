package ru.yandex.market.delivery.transport_manager.controller.support;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.PutInboundRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer.ffwf.TransferRegisterFfwfProducer;
import ru.yandex.market.delivery.transport_manager.task.RefreshTransportationsByConfigTask;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@DbUnitConfiguration(
        databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
        dataSetLoader = ReplacementDataSetLoader.class
)
class SupportTransportationControllerTest extends AbstractContextualTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RefreshTransportationsByConfigTask transportationsByConfigTask;

    @Autowired
    private TransferRegisterFfwfProducer transferRegisterFfwfProducer;

    @Autowired
    private PutInboundRegisterProducer putInboundRegisterProducer;

    @Test
    void refresh() throws Exception {
        Instant datetime = Instant.parse("2020-11-29T19:00:00.00Z");
        clock.setFixed(datetime, ZoneOffset.UTC);
        mockMvc.perform(put("/support/transportation/refresh"))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(transportationsByConfigTask).run(eq(datetime.atZone(ZoneOffset.UTC).toLocalDateTime()));
    }

    @Test
    void refreshByDate() throws Exception {
        mockMvc.perform(put("/support/transportation/refresh/2020-11-28"))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(transportationsByConfigTask).run(eq(LocalDateTime.parse("2020-11-28T00:00:00")));
    }

    @DatabaseSetup("/repository/transportation/linehaul_failed_on_wms.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/linehaul_failed_on_wms_reparied.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
            value = "/repository/transportation/after/master_dbqueue.xml",
            connection = "dbUnitDatabaseConnectionDbQueue",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void startFailedTransportation() throws Exception {
        Instant datetime = Instant.parse("2020-11-29T19:00:00.00Z");
        clock.setFixed(datetime, ZoneOffset.UTC);
        mockMvc.perform(put("/support/transportation/start-particular-scheduled/11"))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @DatabaseSetup(value = "/repository/transportation/interwarehouse/interwarehouse_error_inbound.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/interwarehouse/interwarehouse_error_inbound_repair.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void startFailedInterwarehouseTransportation() throws Exception {
        Instant datetime = Instant.parse("2020-11-29T19:00:00.00Z");
        clock.setFixed(datetime, ZoneOffset.UTC);
        mockMvc.perform(put("/support/transportation/start-particular-scheduled/11"))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(transferRegisterFfwfProducer).produce(123L);
    }

    @DatabaseSetup(value = "/repository/transportation/interwarehouse/interwarehouse_error_inbound_with_plan.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/interwarehouse/interwarehouse_error_inbound_with_plan_repaired.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void startFailedInterwarehouseInboundRegisterFound() throws Exception {
        Instant datetime = Instant.parse("2020-11-29T19:00:00.00Z");
        clock.setFixed(datetime, ZoneOffset.UTC);
        mockMvc.perform(put("/support/transportation/start-particular-scheduled/11"))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(putInboundRegisterProducer).produce(12L);
    }
}
