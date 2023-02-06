package ru.yandex.market.checkout.checkouter.tasks;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class UnfreezePreorderStocksTaskTest extends AbstractWebTestBase {

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @Test
    public void shouldUnfreezePreorderStocks() {
        Parameters parameters = defaultBlueOrderParameters(OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(true));
        }));
        parameters.getReportParameters().setIgnoreStocks(false);
        Order preorder = orderCreateHelper.createOrder(parameters);
        assertThat(preorder.isFulfilment(), is(true));
        assertThat(preorder.isPreorder(), is(true));

        orderStatusHelper.proceedOrderToStatus(preorder, OrderStatus.PENDING);

        stockStorageConfigurer.mockOkForUnfreezePreorder();
        orderStatusHelper.updateOrderStatus(preorder.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);

        List<ServeEvent> stockStorageCalls = stockStorageConfigurer.getServeEvents()
                .stream()
                .filter(se -> se.getRequest().getUrl().startsWith("/preorder/" + preorder.getId() + "?cancel=true"))
                .collect(Collectors.toList());

        assertThat(stockStorageCalls, hasSize(1));
    }
}
