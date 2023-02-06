    package ru.yandex.market.delivery.transport_manager.facade.transportation;

import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.distribution_center.axapta_request.SendAxaptaRequestToDcProducer;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class TransportationTagsFacadeTest extends AbstractContextualTest {
    @Autowired
    private TransportationTagsFacade tagsFacade;

    @Autowired
    private SendAxaptaRequestToDcProducer producer;

    @Test
    @DatabaseSetup("/repository/transportation/xdock_with_several_axapta_id.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_axapta_order_id_received_for_single.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSingleAxaptaId() {
        tagsFacade.setAxaptaMovementOrderNumber(2L, "ЗПер123");
    }

    @Test
    @DatabaseSetup("/repository/transportation/xdock_with_single_axapta_id.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_axapta_order_id_received_for_several.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSeveralAxaptaIds() {
        tagsFacade.setAxaptaMovementOrderNumber(2L, "ЗПер123");
    }

    @Test
    @DatabaseSetup("/repository/transportation/xdock_with_single_axapta_id.xml")
    @DatabaseSetup("/repository/transportation/partner_info.xml")
    @DatabaseSetup(
        value = "/repository/transportation/update/lgw_strategy_for_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void testCorrectInboundBuiltForDirectlyToLgwStrategy() {
        tagsFacade.setAxaptaMovementOrderNumber(2L, "ЗПер123");

        verify(producer).produce(
            argThat(dto -> Objects.equals(dto.getInboundYandexId(), "TMU6")),
            eq(3L),
            eq(2L)
        );
    }

}
