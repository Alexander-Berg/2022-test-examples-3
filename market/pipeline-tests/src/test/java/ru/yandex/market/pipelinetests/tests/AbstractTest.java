package ru.yandex.market.pipelinetests.tests;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.report.OfferItem;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import step.CheckouterSteps;
import toolkit.extensions.TestWatcherExtension;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;

@ExtendWith(TestWatcherExtension.class)
public abstract class AbstractTest {

    @Property("checkouter.regionId")
    protected long regionId;

    protected static final CheckouterSteps ORDER_STEPS = new CheckouterSteps();
    protected CreateOrderParameters params;
    protected OfferItem item;
    protected Order order;
    protected Long lomOrderId;
    protected String orderExternalId;
    protected OrderDto lomOrder;

    protected AbstractTest() {
        PropertyLoader.newInstance().populate(this);
    }
}
