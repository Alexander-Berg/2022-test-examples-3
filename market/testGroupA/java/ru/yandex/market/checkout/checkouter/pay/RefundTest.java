package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.pay.refund.ItemsRefundStrategy;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.httpclient.HttpStatus.SC_BAD_REQUEST;
import static org.apache.commons.httpclient.HttpStatus.SC_NO_CONTENT;
import static ru.yandex.market.checkout.checkouter.pay.RefundTestHelper.refundableItemsFromOrder;
import static ru.yandex.market.checkout.common.util.BigDecimalUtils.isPositive;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

/**
 * @author : poluektov
 * date: 10.07.17.
 */

public class RefundTest extends AbstractPaymentTestBase {

    @Autowired
    private ItemsRefundStrategy itemsRefundStrategy;

    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @DisplayName("Проверяем, что можно полностью зарефандить заказ")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testFullRefund(boolean divideBasketByItemsInBalance) throws Exception {
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
        //do
        refundTestHelper.checkRefundableItems();
        refundTestHelper.makeFullRefund();
    }

    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @DisplayName("Проверяем, что можно сделать частичный возврат по заказу")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testPartialRefundForItems(boolean divideBasketByItemsInBalance) throws Exception {
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
        //do
        RefundableItems refundedItems = refundItemsPartially();
        trustMockConfigurer.mockCheckBasket(buildPostAuth(), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("First check passed");
        });
        refundItemsFully(refundedItems);

        refundedItems = refundableItemsFromOrder(order());
        refundTestHelper.tryMakeRefundForItems(refundedItems, SC_NO_CONTENT);
    }


    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @DisplayName("Проверяем, что можно сделать частичный возврат с указанием суммы, если НК РФ это позволяет")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRefundForAmount(boolean divideBasketByItemsInBalance) throws Exception {
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
        //do
        RefundableItems refundableItems = refundTestHelper.checkRefundableItems();
        BigDecimal refundAmount = order().getBuyerTotal();
        if (refundableItems.canRefundAmount()) {
            refundTestHelper.makeRefundForAmount(refundAmount);
        } else {
            refundTestHelper.tryMakeRefundForAmount(refundAmount, SC_BAD_REQUEST);
        }
    }

    @Nonnull
    private RefundableItems refundItemsPartially() throws Exception {
        RefundableItems refundableItems = refundTestHelper.checkRefundableItems();
        refundableItems.getItems().forEach(i -> {
            int count = (i.getCount() + 1) / 2;
            i.setRefundableCount(count);
            i.setRefundableQuantity(BigDecimal.valueOf(count));
        });
        refundableItems.getItemServices().forEach(s -> s.setRefundableCount((s.getCount() + 1) / 2));
        refundableItems.getDelivery().setRefundable(false);
        if (!refundableItems.canRefundAmount()) {
            refundTestHelper.makeRefundForItems(refundableItems);
        } else {
            refundTestHelper.tryMakeRefundForItems(refundableItems, SC_BAD_REQUEST);
        }
        return refundableItems;
    }

    private void refundItemsFully(RefundableItems refundedItems) throws Exception {
        RefundableItems refundableItems = refundableItemsFromOrder(order());
        refundableItems.getItems().forEach(i -> {
            i.setRefundableCount(
                    i.getCount() - refundableItems.getItemById(i.getId()).getRefundableCount()
            );
            i.setRefundableQuantity(
                    i.getQuantityIfExistsOrCount().subtract(
                            refundableItems.getItemById(i.getId()).getRefundableQuantityIfExistsOrRefundableCount())
            );
        });
        refundableItems.setItems(
                refundableItems.getItems().stream()
                        .filter(i -> i.getRefundableCount() > 0 ||
                                isPositive(i.getRefundableQuantityIfExistsOrRefundableCount()))
                        .collect(toList())
        );
        refundableItems.getItemServices().forEach(s -> s.setRefundableCount(
                s.getCount() - refundableItems.getItemServiceById(s.getId()).getRefundableCount()
        ));
        refundableItems.setItemServices(
                refundableItems.getItemServices().stream().filter(s -> s.getRefundableCount() > 0).collect(toList())
        );
        refundableItems.getDelivery().setRefundable(
                refundableItems.getDelivery().isRefundable() && !refundedItems.getDelivery().isRefundable()
        );
        refundTestHelper.checkRefundableItems(refundableItems);
        if (!refundableItems.canRefundAmount()) {
            refundTestHelper.makeRefundForItems(refundableItems);
        } else {
            refundTestHelper.tryMakeRefundForItems(refundableItems, SC_BAD_REQUEST);
        }
    }

    private void tryRefundOverly() throws Exception {
        RefundableItems refundedItems;
        refundedItems = refundableItemsFromOrder(order());
        refundTestHelper.tryMakeRefundForItems(refundedItems, SC_BAD_REQUEST);
    }

    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @DisplayName("Проверяем, что отмена заклиренного платежа после полного рефанда не приводит к созданию нового " +
            "возвратного чека")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void cancelPaymentAfterFullRefund(boolean divideBasketByItemsInBalance) throws Exception {
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
        //do
        refundTestHelper.makeFullRefund();
        paymentTestHelper.cancelPayment();
    }
}
