package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.actualization.multicart.processor.JewelryValidatorAndPaymentOptionsUpdater.JEWELRY_COST_LIMIT_200K;
import static ru.yandex.market.checkout.checkouter.actualization.multicart.processor.JewelryValidatorAndPaymentOptionsUpdater.JEWELRY_PASSPORT_REQUIRED;


public class CreateJewelryOrderTest extends AbstractWebTestBase {

    private static final int JEWELRY_CATEGORY = 91275;
    @Autowired
    protected CheckouterProperties checkouterProperties;
    @Autowired
    private OrderCreateHelper orderCreateHelper;

    @Test
    public void createDbsJewelryPassportRequiredTest() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        OrderItem item = parameters.getOrder().getItems().iterator().next();
        item.setCount(200); //200 * 250 RUB
        item.setCategoryId(91280);

        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(item)
                .categoryId(JEWELRY_CATEGORY)
                .build()));
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        assertTrue(actualCart.getCarts().get(0).getValidationWarnings().stream()
                .anyMatch(w -> w.getCode().equals(JEWELRY_PASSPORT_REQUIRED)));
    }

    @Test
    public void createFbsJewelryCostLimitErrorTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        OrderItem item = parameters.getOrder().getItems().iterator().next();
        item.setCount(1000); // 1000 * 250
        item.setCategoryId(91280);

        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(item)
                .categoryId(JEWELRY_CATEGORY)
                .build()));
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("YANDEX_MARKET"));

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        assertTrue(actualCart.getCarts().get(0).getValidationErrors().stream()
                .anyMatch(w -> w.getCode().equals(JEWELRY_COST_LIMIT_200K)));
    }

    @Test
    public void createFbsJewelryFilterPostpaidTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem item = parameters.getOrder().getItems().iterator().next();
        item.setCount(200); //200 * 250 RUB
        item.setCategoryId(91280);

        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(item)
                .categoryId(JEWELRY_CATEGORY)
                .build()));
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("YANDEX_MARKET"));

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        assertTrue(actualCart.getCarts().get(0).getPaymentOptions().stream()
                .noneMatch(po -> po.getPaymentType() == PaymentType.POSTPAID));
    }

}
