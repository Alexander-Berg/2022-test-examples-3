package ru.yandex.market.checkout.checkouter.storage;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.tasks.unfreeze.UnfreezeStocksTask.CHUNK;

/**
 * Created by asafev on 01/11/2017.
 */
public class OrderReadingDaoUnfreezeStocksTest extends AbstractWebTestBase {

    @Autowired
    private OrderReadingDao orderReadingDao;

    @Test
    public void testGetOrdersWithUnfreezeStocksFlag() {
        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderCreateHelper.createOrder(createFulfilmentOrderWithUnfreezeStocks());
        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        List<Order> orders = orderReadingDao.getOrdersWithUnfreezeStocksFlag(LocalDateTime.now(), CHUNK);
        assertThat(orders, hasSize(1));
    }

    @Test
    public void testFilterByTime() throws Exception {
        orderCreateHelper.createOrder(createFulfilmentOrderWithUnfreezeStocks());
        LocalDateTime time = LocalDateTime.now();
        orderCreateHelper.createOrder(createFulfilmentOrderWithUnfreezeStocks());

        List<Order> orders = orderReadingDao.getOrdersWithUnfreezeStocksFlag(time, CHUNK);

        assertThat(orders, hasSize(1));
    }

    private Parameters createFulfilmentOrderWithUnfreezeStocks() {
        Parameters parametersWithUnfreezeStocks = BlueParametersProvider.defaultBlueOrderParameters();
        parametersWithUnfreezeStocks.setUnfreezeStocksTime(LocalDateTime.now());
        return parametersWithUnfreezeStocks;
    }
}
