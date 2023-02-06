package ru.yandex.market.delivery.transport_manager.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationComponentType;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.NewSchemeTransportationCallbackService;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.NewSchemeTransportationRouter;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;

public class NewSchemeTransportationRouterTest extends AbstractContextualTest {

    @Autowired
    private NewSchemeTransportationRouter router;

    @Autowired
    private NewSchemeTransportationCallbackService callbackService;

    @Autowired
    private TransportationMapper transportationMapper;

    @Test
    @DatabaseSetup(
        value = {
            "/repository/service/new_scheme_router/transportation.xml",
            "/repository/service/new_scheme_router/inbound_method.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/service/new_scheme_router/after/after_inbound_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testIgnoredNotRoutedTwice() {
        Transportation transportation = transportationMapper.getById(1);
        router.route(transportation);
        callbackService.processSuccessCallback(transportation, TransportationComponentType.INBOUND);
    }
}
