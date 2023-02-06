package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.List;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Приёмка с отменой товара")
@Epic("Selenium Tests")
@Resource.Classpath("wms/test.properties")
public class ReceivingCancelItemTest extends AbstractUiTest {
    private final Item ITEM = Item.builder().sku("INBITSTITEM14").vendorId(1559).article("INBITSTITEM14").name("Item Name").build();
    private final String CART = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

    @RetryableTest
    @DisplayName("Тест отмены товара в приемке")
    @ResourceLock("Тест отмены товара в приемке")
    public void receivingCancelItemTest() {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry("wms/servicebus/putInboundRegistry/putInboundRegistry.xml",
                inbound,
                ITEM.getArticle(),
                ITEM.getVendorId(),
                false
        );

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItem(inbound.getFulfillmentId(), 1);
        uiSteps.Login().PerformLogin();

        String pallet = uiSteps.Receiving().findPalletOfInbound(inbound.getFulfillmentId());
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(ITEM, pallet, CART, false);

        uiSteps.Login().PerformLogin();
        //TODO перевести на новые красивые балансы как они появятся
        List<String> snList = uiSteps.Balances().findUitByNzn(CART);
        String sn = snList.get(0);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().cancelUitReceiving(sn);

        uiSteps.Login().PerformLogin();
        uiSteps.Balances().findDeletedUit(sn);

    }
}
