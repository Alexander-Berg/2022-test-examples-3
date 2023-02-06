package ru.yandex.market.checkout.util.fulfillment;

import java.math.BigDecimal;
import java.util.Random;

import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;

import static ru.yandex.market.checkout.providers.FulfilmentProvider.FF_SHOP_ID;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.getDefaultMeta;

/**
 * @author Nikolai Iusiumbeli
 * date: 26/01/2018
 */
@TestComponent
public class FulfillmentConfigurer {

    private static final Random RANDOM = new Random(100500L);

    private static void applyFulfilmentRandomParams(ReportGeneratorParameters reportParameters, OrderItem item) {
        reportParameters.overrideItemInfo(item.getFeedOfferId()).setFulfilment(
                new ItemInfo.Fulfilment(Math.abs(RANDOM.nextLong()),
                        String.valueOf(Math.abs(RANDOM.nextLong())),
                        String.valueOf(Math.abs(RANDOM.nextLong())))
        );
    }

    public static void applyFulfilmentParams(ReportGeneratorParameters reportParameters, OrderItem item) {
        reportParameters.overrideItemInfo(item.getFeedOfferId()).setFulfilment(
                new ItemInfo.Fulfilment(
                        FF_SHOP_ID,
                        item.getMsku().toString(),
                        item.getShopSku(),
                        null,
                        false
                )
        );
    }

    public void configure(Parameters parameters) {
        configure(parameters, true);
    }

    public void configure(Parameters parameters, boolean freeDelivery) {
        Order order = parameters.getOrder();
        ReportGeneratorParameters reportParameters = parameters.getReportParameters();
        order.setFulfilment(true);
        for (OrderItem item : order.getItems()) {
            applyFulfilmentRandomParams(reportParameters, item);
        }

        parameters.setWeight(BigDecimal.valueOf(1));
        parameters.setDimensions("10", "10", "10");

        parameters.addShopMetaData(order.getShopId(), getDefaultMeta());
        order.getItems().stream().map(item -> reportParameters.overrideItemInfo(item.getFeedOfferId())
                .getFulfilment().supplierId)
                .forEach(shopId -> {
                    parameters.addShopMetaData(shopId, getDefaultMeta());
                });

    }
}
