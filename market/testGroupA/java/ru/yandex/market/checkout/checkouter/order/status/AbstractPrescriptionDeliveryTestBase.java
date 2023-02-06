package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.shop.PrescriptionManagementSystem;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.common.report.model.specs.Specs;

abstract class AbstractPrescriptionDeliveryTestBase extends AbstractWebTestBase {

    protected static final String PRESCRIPTION = "prescription";
    protected static final int SHOP_ID_WITH_PMS = 670;
    protected static final long BUSINESS_ID = 670L;

    @Autowired
    protected OrderCreateHelper orderCreateHelper;
    @Autowired
    protected OrderStatusHelper orderStatusHelper;
    @Autowired
    protected CheckouterClient checkouterClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    protected Order createOrderWithPrescription(Parameters parameters,
                                                boolean isShopIdWithMedicataIntegration,
                                                boolean isOfferWithPrescription) {
        // Магазин, имеющий интеграцию с Медикатой
        if (isShopIdWithMedicataIntegration) {
            ShopMetaDataBuilder shopBuilder = ShopMetaDataBuilder.createCopy(
                    ShopSettingsHelper.createCustomNotFulfilmentMeta(SHOP_ID_WITH_PMS, BUSINESS_ID));
            shopBuilder.withPrescriptionManagementSystem(PrescriptionManagementSystem.MEDICATA);
            parameters.addShopMetaData((long) SHOP_ID_WITH_PMS, shopBuilder.build());

            parameters.configureMultiCart(multiCart ->
                    multiCart.getCarts().forEach(o -> o.setShopId((long) SHOP_ID_WITH_PMS))
            );
        }
        if (isOfferWithPrescription) {
            parameters.getItems().forEach(
                    it -> it.setMedicalSpecsInternal(Specs.fromSpecValues(Set.of(PRESCRIPTION)))
            );
        }

        // приходят GUID'ы с фронта
        Consumer<MultiCart> betweenCartAndCheckoutHook = multiCart ->
                multiCart.getCarts().forEach(order -> {
                    order.setBuyer(multiCart.getBuyer());
                    order.getItems().forEach(it -> {
                        if (isShopIdWithMedicataIntegration) {
                            it.setSupplierId((long) SHOP_ID_WITH_PMS);
                        }
                        if (isOfferWithPrescription) {
                            it.setPrescriptionGuids(Set.of("guid_1", "guid_2"));
                        }
                    });
                });

        return orderCreateHelper.createMultiOrder(parameters, betweenCartAndCheckoutHook)
                .getOrders().get(0);
    }
}
