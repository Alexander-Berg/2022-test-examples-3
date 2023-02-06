package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Receiving - Приемка межсклад")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class InterwarehouseTest extends AbstractUiTest {

    private final String sku = UniqueId.getString();
    private final String palletId = UniqueId.getString();

    @RetryableTest
    @DisplayName("Приёмка товара по УИТ")
    @ResourceLock("Приёмка товара по УИТ")
    void receiveBasic() {
        final Inbound inbound = ApiSteps.Inbound().putInbound(
                "wms/servicebus/putInbound/putInbound.xml",
                InboundType.WH2WH
        );
        String uit = "99" + RandomUtil.randomStringNumbersOnly(10);
        String lot = RandomUtil.randomStringNumbersOnly(10);
        ApiSteps.Inbound().putInboundRegistry(
                "wms/servicebus/putInboundRegistry/putInboundRegistryWH2WH.xml",
                inbound,
                uit,
                sku,
                1559,
                lot,
                palletId,
                false
        );
        var cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                Item.builder()
                        .vendorId(1559)
                        .article(uit)
                        .name("Nutella, 180g")
                        .build(),
                palletId,
                cartId,
                false,
                false
        );
    }

}
