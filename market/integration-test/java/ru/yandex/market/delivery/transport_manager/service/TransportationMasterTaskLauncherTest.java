package ru.yandex.market.delivery.transport_manager.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.TransportationMasterProducer;

import static org.mockito.Mockito.verify;

class TransportationMasterTaskLauncherTest extends AbstractContextualTest {
    @Autowired
    TransportationMasterTaskLauncher transportationMasterTaskLauncher;
    @Autowired
    TransportationMasterProducer transportationMasterProducer;


    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml",
        "/repository/metadata/old_and_new_methods_meta.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/transportation_task_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void startScheduledTransportationsTest_success() {
        clock.setFixed(
            LocalDateTime.of(2020, 7, 9, 21, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );
        transportationMasterTaskLauncher.startScheduledTransportations();
        verify(transportationMasterProducer).enqueue(1L);
        verify(transportationMasterProducer).enqueue(2L);
    }

}
