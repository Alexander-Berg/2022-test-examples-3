package ru.yandex.market.delivery.transport_manager.repository.mappers;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.XdocInbound;
import ru.yandex.market.delivery.transport_manager.model.enums.XdocInboundStatus;

import static org.assertj.core.api.Assertions.assertThat;


class XdocInboundMapperTest extends AbstractContextualTest {

    @Autowired
    XdocInboundMapper xdocInboundMapper;

    @Test
    @DatabaseSetup("/repository/view/xdock_transportation_view.xml")
    void findAll() {
        XdocInbound first = new XdocInbound();
        first.setStatus(XdocInboundStatus.SHIPPED_FROM_DC);
        first.setAxaptaMovementRequestId(null);
        first.setInformationListCode("0000000624");
        first.setDestinationId("2");
        first.setDestinationName("Томилино");
        first.setPalletNumber(3);

        XdocInbound second = new XdocInbound();
        second.setStatus(XdocInboundStatus.SHIPPED_FROM_DC);
        second.setAxaptaMovementRequestId("ЗПер0011951");
        second.setInformationListCode("Зп-370098316");
        second.setDestinationId("2");
        second.setBoxNumber(4);
        second.setDestinationName("Томилино");

        XdocInbound bbxd = new XdocInbound();
        bbxd.setInformationListCode("0000000625");
        bbxd.setStatus(XdocInboundStatus.SHIPPED_FROM_DC);
        bbxd.setDestinationId("2");
        bbxd.setPalletNumber(0);
        bbxd.setDestinationName("Томилино");

        assertThat(xdocInboundMapper.findAllInbounds()).hasSize(3).containsExactlyInAnyOrder(first, second, bbxd);
    }

}
