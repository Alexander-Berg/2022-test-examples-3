package ru.yandex.market.logistic.gateway.repository;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.model.dto.DeliveryServiceStatus;

public class ClientTaskJdbcRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    ClientTaskJdbcRepository clientTaskJdbcRepository;

    @Before
    public void before() {
        clock.setFixed(LocalDateTime.of(2118, 2, 1, 23, 55, 55).toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault());
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_tasks_to_analyze_availability_offline.xml")
    public void testIsOffline() {
        List<DeliveryServiceStatus> statuses = clientTaskJdbcRepository.analyzeDeliveryServicesAvailability();
        Assertions.assertThat(statuses).extracting(DeliveryServiceStatus::getIsOffline).containsOnly(true);
        Assertions.assertThat(statuses).extracting(DeliveryServiceStatus::getPartnerId).containsOnly(1L);
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_tasks_to_analyze_availability_online.xml")
    public void testIsOnline() {
        List<DeliveryServiceStatus> statuses = clientTaskJdbcRepository.analyzeDeliveryServicesAvailability();
        Assertions.assertThat(statuses).extracting(DeliveryServiceStatus::getIsOffline).containsOnly(false);
        Assertions.assertThat(statuses).extracting(DeliveryServiceStatus::getPartnerId).containsOnly(1L);
    }
}
