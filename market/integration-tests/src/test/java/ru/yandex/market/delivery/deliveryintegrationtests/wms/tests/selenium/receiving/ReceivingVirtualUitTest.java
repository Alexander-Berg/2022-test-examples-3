package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Receiving - Приёмка без уит'ов")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class ReceivingVirtualUitTest extends AbstractUiTest {

    @Property("wms.ui.receiving.printer")
    private String printer;

    private final String virtualUitsku = "ReceivingVirtualUitTestItem";

    private String fromPalletId;
    private String toPalletId;

    @BeforeEach
    void setUp() {
        final String inbound = createInbound();
        fromPalletId = processSteps.Incoming().placeInboundToPallet(inbound);
        toPalletId = DatacreatorSteps.Label().createContainer(ContainerIdType.PLT);
    }

    @Step("Создаем поставку товаров")
    private String createInbound() {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry("wms/servicebus/putInboundRegistry/putInboundRegistry.xml",
                inbound,
                virtualUitsku,
                465852,
                false
        );
        return inbound.getFulfillmentId();
    }

    @RetryableTest
    @DisplayName("Приёмка товара")
    @ResourceLock("Приёмка товара")
    void receiveBasic() {
        uiSteps.Login().PerformLogin();
        log.info(inboundTable.toString());
        uiSteps.Receiving().receiveVirtualUitItem(
                Item.builder()
                        .vendorId(465852)
                        .article(virtualUitsku)
                        .name("Item Name")
                        .build(),
                fromPalletId,
                toPalletId,
                1,
                50
        );
    }
}
