package ru.yandex.market.deliveryintegrationtests.delivery.tests.capacity;

import factory.OfferItems;
import step.CapacityStorageSteps;
import step.LmsSteps;
import step.LomOrderSteps;
import org.junit.jupiter.params.provider.Arguments;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.deliveryintegrationtests.delivery.tests.AbstractTest;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CountingType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public abstract class AbstractCapacityTest extends AbstractTest {

    protected static final LmsSteps LMS_STEPS = new LmsSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final CapacityStorageSteps CAPACITY_STORAGE_STEPS = new CapacityStorageSteps();

    protected final List<Order> orders = Collections.synchronizedList(new ArrayList<>());

    public static Stream<Arguments> getParams() {
        return Stream.of(
                arguments(47924L, OfferItems.DROPSHIP_SC, CapacityService.SHIPMENT, CountingType.ORDER), //DROPSHIP
                arguments(47924L, OfferItems.DROPSHIP_SC, CapacityService.SHIPMENT, CountingType.ITEM),//DROPSHIP
                arguments(300L, OfferItems.FF_300_UNFAIR_STOCK_EXPRESS, CapacityService.SHIPMENT, CountingType.ORDER), //FF
                arguments(300L, OfferItems.FF_300_UNFAIR_STOCK_EXPRESS, CapacityService.SHIPMENT, CountingType.ITEM), //FF
                arguments(100136L, OfferItems.DROPSHIP_SC, CapacityService.SHIPMENT, CountingType.ORDER), //SC
                arguments(100136L, OfferItems.DROPSHIP_SC, CapacityService.SHIPMENT, CountingType.ITEM) //SC
        );
    }

}
