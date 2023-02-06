package ru.yandex.market.delivery.transport_manager.service.xdoc;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;

class XDocIncludedTransportationsServiceTest extends AbstractContextualTest {
    @Autowired
    private XDocIncludedTransportationsService xDocIncludedTransportationsService;

    @DatabaseSetup("/repository/transportation/xdoc_transport_with_slot.xml")
    @Test
    void getIncludedSuppliesInfo() {
        Map<String, List<String>> actual =
            xDocIncludedTransportationsService.getParentBarcodesByChildBarcodes(
                TagCode.FFWF_INCLUDED_REQUEST_ID_PLAN,
                new TransportationUnit().setId(12L)
            );

        softly.assertThat(actual).isEqualTo(Map.of(
            "abc0", List.of("multipallet1"),
            "abc1", List.of("multipallet2"),
            "abc2", List.of("multipallet2"),
            "abc3", List.of("multipallet1")
        ));
    }

    @DatabaseSetup("/repository/transportation/xdoc_transport_with_fact_items.xml")
    @Test
    void getTotalItemsQuantity() {
        softly.assertThat(
                xDocIncludedTransportationsService.getTotalItemsQuantity(
                    new Transportation()
                        .setId(1L)
                        .setOutboundUnit(new TransportationUnit().setId(12L)
                        ),
                    TagCode.FFWF_INCLUDED_REQUEST_ID_FACT
                )
            )
            .isEqualTo(17);
    }
}
