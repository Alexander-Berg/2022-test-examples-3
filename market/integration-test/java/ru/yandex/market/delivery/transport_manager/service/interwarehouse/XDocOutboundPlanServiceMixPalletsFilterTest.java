package ru.yandex.market.delivery.transport_manager.service.interwarehouse;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;
import ru.yandex.market.delivery.transport_manager.service.xdoc.XDocOutboundPlanService;

public class XDocOutboundPlanServiceMixPalletsFilterTest extends AbstractContextualTest {
    @Autowired
    private XDocOutboundPlanService xDocOutboundPlanService;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    @Test
    @DatabaseSetup({
        "/repository/distribution_unit_center/transportations_with_mix_pallets.xml",
    })
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/transportations_with_mix_pallets_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testMixPalletWithAxaptaFiltering() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_MOVEMENT_ID_INBOUND_FILTER))
            .thenReturn(true);
        xDocOutboundPlanService.processOutboundPlanRegister(7L);
    }
}
