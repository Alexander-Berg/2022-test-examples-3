package ru.yandex.market.delivery.transport_manager.facade.xdoc;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

class XDocRegisterFacadeTest extends AbstractContextualTest {
    @Autowired
    private XDocRegisterFacade xDocRegisterFacade;

    @DatabaseSetup("/repository/transportation/xdoc_to_ff_transportations.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/xdoc_copy_inbound_register_break_bulk_xdock.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void copyToOutbound() {
        xDocRegisterFacade.copyToOutbound(
            1L,
            100L
        );
    }
}
