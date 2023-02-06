package ru.yandex.market.checkout.checkouter.items;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.CashbackTestBase;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.common.pay.FinancialUtils;
import ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoodtechType;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.CANNOT_REMOVE_LAST_ITEM_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEM_HAS_AMBIGUOUS_QUANTITY;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.CASHBACK;
import static ru.yandex.market.checkout.common.pay.FinancialUtils.roundPrice;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ChangeOrderItemsTest extends CashbackTestBase {

    @Autowired
    private ChangeOrderItemsHelper changeOrderItemsHelper;

    @Autowired
    private OrderPayHelper orderPayHelper;

    @Autowired
    OrderWritingDao writingDao;

    @ParameterizedTest(name = "{index} - quantity for added item = {0}")
    @ArgumentsSource(QuantityProvider.class)
    @DisplayName("Добавление нового товара в едовом заказе")
    public void shouldAddNewItemIfIsEatsTrue(BigDecimal newQuantity) throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();
        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(3450);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(3);
        FeedOfferId feedOfferIdOfUnchangedItem = new FeedOfferId("roadToUnchange", 1L);
        fillOrderItem(firstItem, feedOfferIdOfUnchangedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);
        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);

        //добавляем
        BigDecimal addedItemQuantPrice = BigDecimal.valueOf(1325);
        FeedOfferId feedOfferIdOfAddedItem = new FeedOfferId("roadToAdded", 1L);
        OrderItem addedItem = changeOrderItemsHelper.addNewItem(order, feedOfferIdOfAddedItem,
                addedItemQuantPrice);
        prepareOrderItemForChangeRequest(orderItemChanges.get(feedOfferIdOfUnchangedItem),
                firstItemQuantity);
        prepareOrderItemForChangeRequest(addedItem, newQuantity);
        orderItemChanges.put(addedItem.getFeedOfferId(), addedItem);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order), order.getId());
        response.andExpect(status().isOk());

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem expectedUnchangedItem = buildOrderItem(feedOfferIdOfUnchangedItem,
                firstItemQuantity, firstItemQuantPrice, firstItemQuantPrice);

        BigDecimal expectedPrice = isIntegerValue(newQuantity) ? addedItemQuantPrice :
                FinancialUtils.roundPrice(addedItemQuantPrice.multiply(newQuantity));
        OrderItem expectedAddedItem = buildOrderItem(feedOfferIdOfAddedItem, newQuantity,
                addedItemQuantPrice, expectedPrice);

        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedAddedItem.getFeedOfferId(), expectedAddedItem,
                        expectedUnchangedItem.getFeedOfferId(), expectedUnchangedItem));
    }

    @Test
    @DisplayName("Добавление нового товара в едовом заказе с имеющимся кэшбеком не ломает старое")
    public void shouldNotRecalcCurrentItemPromoWhenOrderIsEatsAndAddNewItems()
            throws Exception {
        singleItemWithCashbackParams.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        singleItemWithCashbackParams.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        OrderItem firstItem = singleItemWithCashbackParams.getOrders().get(0).getItems().iterator().next();
        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(3200);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(7);
        FeedOfferId feedOfferIdOfUnchangedItem = new FeedOfferId("roadToUnchange", 1L);
        fillOrderItem(firstItem, feedOfferIdOfUnchangedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);

        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);
        cart.setSelectedCashbackOption(CashbackOption.EMIT);
        MultiOrder checkout = orderCreateHelper.checkout(cart, singleItemWithCashbackParams);
        Order order = checkout.getCarts().get(0);
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);

        OrderItem unchangedItem = orderItemChanges.get(feedOfferIdOfUnchangedItem);
        BigDecimal cashbackAmountForUnchangedItem =
                unchangedItem.getPromos().stream().filter(p -> p.getType() == CASHBACK).findFirst()
                        .get().getCashbackAccrualAmount();

        //добавляем
        BigDecimal addedItemQuantPrice = BigDecimal.valueOf(43000);
        FeedOfferId feedOfferIdOfAddedItem = new FeedOfferId("roadToAdded", 1L);
        OrderItem addedItem = changeOrderItemsHelper.addNewItem(order, feedOfferIdOfAddedItem,
                addedItemQuantPrice);
        BigDecimal addedQuantity = BigDecimal.valueOf(7.77);

        prepareOrderItemForChangeRequest(orderItemChanges.get(feedOfferIdOfUnchangedItem),
                firstItemQuantity);
        prepareOrderItemForChangeRequest(addedItem, addedQuantity);
        orderItemChanges.put(addedItem.getFeedOfferId(), addedItem);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order), order.getId());
        response.andExpect(status().isOk());

        Order actualOrder = orderService.getOrder(order.getId());
        unchangedItem = actualOrder.getItem(feedOfferIdOfUnchangedItem);
        assertThat(unchangedItem.getPromos().size(), is(2));
        assertThat(unchangedItem.getPromos(), hasItem(
                hasProperty("promoDefinition",
                        is(PromoDefinition.byType(CASHBACK, "promoKey")))));
        assertThat(unchangedItem.getPromos(), hasItem(
                hasProperty("cashbackAccrualAmount", is(cashbackAmountForUnchangedItem.setScale(2)))));
        assertThat(unchangedItem.getPromos(), hasItem(
                hasProperty("promoDefinition", is(PromoDefinition.byType(PromoType.MARKET_COUPON,
                        "some promo key")))));
    }


    @Test
    @DisplayName("Рассчет кэшбека для нового товара в едовом заказе")
    public void shouldCalcCashbackWhenOrderIsEatsAndAddNewItems()
            throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();
        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(823);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(4);
        FeedOfferId feedOfferIdOfUnchangedItem = new FeedOfferId("roadToUnchange", 1L);
        fillOrderItem(firstItem, feedOfferIdOfUnchangedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);
        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);

        OrderItem unchangedItem = orderItemChanges.get(feedOfferIdOfUnchangedItem);
        //добавляем
        BigDecimal addedItemQuantPrice = BigDecimal.valueOf(43000);
        FeedOfferId feedOfferIdOfAddedItem = new FeedOfferId("roadToAdded", 1L);
        OrderItem addedItem = changeOrderItemsHelper.addNewItem(order, feedOfferIdOfAddedItem,
                addedItemQuantPrice);
        BigDecimal addedItemQuantity = BigDecimal.valueOf(4.5);
        prepareOrderItemForChangeRequest(addedItem, addedItemQuantity);
        prepareOrderItemForChangeRequest(unchangedItem, firstItemQuantity);
        orderItemChanges.put(addedItem.getFeedOfferId(), addedItem);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order), order.getId());
        response.andExpect(status().isOk());

        Order actualOrder = orderService.getOrder(order.getId());
        addedItem = actualOrder.getItem(feedOfferIdOfAddedItem);
        assertThat(addedItem.getPromos().size(), is(1));
        assertThat(addedItem.getPromos(), hasItem(
                hasProperty("promoDefinition",
                        is(PromoDefinition.byType(CASHBACK, "promoKey")))));

        OrderItem expectedUnchangedItem = buildOrderItem(feedOfferIdOfUnchangedItem,
                firstItemQuantity, firstItemQuantPrice, firstItemQuantPrice);

        BigDecimal expectedAddedPrice = isIntegerValue(addedItemQuantity)
                ? addedItemQuantPrice
                : FinancialUtils.roundPrice(addedItemQuantPrice.multiply(addedItemQuantity));

        OrderItem expectedAddedItem = buildOrderItem(feedOfferIdOfAddedItem,
                addedItemQuantity, addedItemQuantPrice, expectedAddedPrice);

        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedUnchangedItem.getFeedOfferId(), expectedUnchangedItem,
                        expectedAddedItem.getFeedOfferId(), addedItem));


    }

    @Test
    @DisplayName("Добавление нового товара в НЕ едовом заказе")
    public void shouldNotAddNewItemIfIsEatsFalse() throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();
        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(3.2);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(3);
        int firstItemCount = 3;
        FeedOfferId feedOfferIdOfUnchangedItem = new FeedOfferId("roadToUnchange", 1L);
        fillOrderItem(firstItem, feedOfferIdOfUnchangedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);

        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);

        //добавляем
        BigDecimal addedItemQuantPrice = BigDecimal.valueOf(1325);
        FeedOfferId feedOfferIdOfAddedItem = new FeedOfferId("roadToAdded", 1L);
        OrderItem addedItem = changeOrderItemsHelper.addNewItem(order, feedOfferIdOfAddedItem,
                addedItemQuantPrice);
        int newCount = 5;
        prepareOrderItemForChangeRequest(orderItemChanges.get(feedOfferIdOfUnchangedItem),
                BigDecimal.valueOf(firstItemCount));
        prepareOrderItemForChangeRequest(addedItem, BigDecimal.valueOf(newCount));
        orderItemChanges.put(addedItem.getFeedOfferId(), addedItem);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order),
                order.getId());
        response.andExpect(status().is4xxClientError())
                .andExpect(jsonPath("code").value("ITEMS_ADDITION_NOT_SUPPORTED"));

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem expectedUnchangedItem = buildOrderItem(feedOfferIdOfUnchangedItem,
                firstItemQuantity, firstItemQuantPrice, firstItemQuantPrice);

        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedUnchangedItem.getFeedOfferId(), expectedUnchangedItem));
    }


    @ParameterizedTest(name = "{index} - Increased OrderItem with new quantity = {0}")
    @ArgumentsSource(QuantityProvider.class)
    @DisplayName("Увеличение целого количества позиции едового заказа")
    public void shouldIncreaseCountIfIsEatsTrue(BigDecimal newQuantity) throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();
        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(823);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(6);
        FeedOfferId feedOfferIdOfIncreasedItem = new FeedOfferId("roadToIncrease", 1L);
        fillOrderItem(firstItem, feedOfferIdOfIncreasedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);
        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);

        //увеличиваем
        OrderItem increasedItem = orderItemChanges.get(feedOfferIdOfIncreasedItem);
        prepareOrderItemForChangeRequest(increasedItem, newQuantity);
        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order), order.getId());
        response.andExpect(status().isOk());
        Order actualOrder = orderService.getOrder(order.getId());
        BigDecimal expectedPrice = isIntegerValue(newQuantity) ? firstItemQuantPrice :
                FinancialUtils.roundPrice(firstItemQuantPrice.multiply(newQuantity));
        OrderItem expectedIncreasedItem = buildOrderItem(feedOfferIdOfIncreasedItem,
                newQuantity, firstItemQuantPrice, expectedPrice);
        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedIncreasedItem.getFeedOfferId(), expectedIncreasedItem));
    }

    @Test
    @DisplayName("Увеличение количества позиции НЕ едового заказа")
    public void shouldNotIncreaseQuantityIfIsEatsFalse() throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();
        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(823);
        FeedOfferId feedOfferIdOfIncreasedItem = new FeedOfferId("roadToIncrease", 1L);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(3);
        fillOrderItem(firstItem, feedOfferIdOfIncreasedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);
        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);
        //увеличиваем
        int increasedItemCount = 8;
        OrderItem increasedItem = orderItemChanges.get(feedOfferIdOfIncreasedItem);
        prepareOrderItemForChangeRequest(increasedItem, BigDecimal.valueOf(increasedItemCount));
        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order),
                order.getId());
        response.andExpect(status().is4xxClientError()).andExpect(jsonPath("code").value(
                "ITEMS_ADDITION_NOT_SUPPORTED"));
        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem expectedUnchangedItem = buildOrderItem(feedOfferIdOfIncreasedItem,
                firstItemQuantity, firstItemQuantPrice, firstItemQuantPrice);

        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedUnchangedItem.getFeedOfferId(), expectedUnchangedItem));
    }

    @ParameterizedTest(name = "{index} - OrderItem with diff quantity = {0}")
    @ArgumentsSource(QuantityProvider.class)
    @DisplayName("Увеличение, добавление и уменьшение едового заказа")
    public void shouldIncreaseAndReduceAndAdded(BigDecimal newQuantity) throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();

        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(91);
        FeedOfferId feedOfferIdOfIncreasedItem = new FeedOfferId("roadToIncrease", 1L);

        BigDecimal firstItemQuantity = BigDecimal.valueOf(10);
        fillOrderItem(firstItem, feedOfferIdOfIncreasedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);

        FeedOfferId feedOfferIdOfReducedItem = new FeedOfferId("roadToReduce", 1L);
        BigDecimal secondItemQuantPrice = BigDecimal.valueOf(1503.3);
        BigDecimal secondItemQuantity = BigDecimal.valueOf(10);
        blueParams.addItem(feedOfferIdOfReducedItem, secondItemQuantity.intValue(),
                secondItemQuantity,
                secondItemQuantPrice);

        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);

        //увеличиваем
        OrderItem increasedItem = orderItemChanges.get(feedOfferIdOfIncreasedItem);
        BigDecimal increasedQuantity = newQuantity.add(newQuantity);
        prepareOrderItemForChangeRequest(increasedItem, increasedQuantity);

        //уменьшаем
        OrderItem reducedItem = orderItemChanges.get(feedOfferIdOfReducedItem);
        BigDecimal reducedItemQuantity =
                BigDecimal.valueOf(Math.max(secondItemQuantity.subtract(newQuantity).doubleValue(), 1));
        prepareOrderItemForChangeRequest(reducedItem, reducedItemQuantity);

        //добавляем
        BigDecimal addedItemQuantPrice = BigDecimal.valueOf(446.32);
        FeedOfferId feedOfferIdOfAddedItem = new FeedOfferId("roadToAdded", 1L);
        OrderItem addedItem = changeOrderItemsHelper.addNewItem(order, feedOfferIdOfAddedItem, addedItemQuantPrice);
        prepareOrderItemForChangeRequest(addedItem, newQuantity);
        orderItemChanges.put(addedItem.getFeedOfferId(), addedItem);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order), order.getId());
        response.andExpect(status().isOk());
        Order actualOrder = orderService.getOrder(order.getId());


        BigDecimal expectedIncreasedPrice = isIntegerValue(newQuantity)
                ? firstItemQuantPrice
                : FinancialUtils.roundPrice(firstItemQuantPrice.multiply(increasedQuantity));
        BigDecimal expectedReducedPrice = isIntegerValue(newQuantity)
                ? secondItemQuantPrice
                : FinancialUtils.roundPrice(secondItemQuantPrice.multiply(reducedItemQuantity));
        BigDecimal expectedAddedPrice = isIntegerValue(newQuantity)
                ? addedItemQuantPrice
                : FinancialUtils.roundPrice(addedItemQuantPrice.multiply(newQuantity));

        OrderItem expectedIncreasedItem = buildOrderItem(feedOfferIdOfIncreasedItem,
                increasedQuantity, firstItemQuantPrice, expectedIncreasedPrice);
        OrderItem expectedReducedItem = buildOrderItem(feedOfferIdOfReducedItem,
                reducedItemQuantity, secondItemQuantPrice, expectedReducedPrice);
        OrderItem expectedAddedItem = buildOrderItem(feedOfferIdOfAddedItem,
                newQuantity, addedItemQuantPrice, expectedAddedPrice);

        Map<FeedOfferId, OrderItem> actualOrderItems = getOrderItems(actualOrder);
        recalcItemPrices(increasedItem, increasedQuantity);
        recalcItemPrices(reducedItem, reducedItemQuantity);
        assertItemPrices(increasedItem, actualOrderItems.get(feedOfferIdOfIncreasedItem));
        assertItemPrices(reducedItem, actualOrderItems.get(feedOfferIdOfReducedItem));
        compareOrderItemsByPricesAndCount(actualOrderItems,
                Map.of(expectedAddedItem.getFeedOfferId(), expectedAddedItem,
                        expectedReducedItem.getFeedOfferId(), expectedReducedItem,
                        expectedIncreasedItem.getFeedOfferId(), expectedIncreasedItem));
    }

    @Test
    public void shouldThrowExceptionWhenRCContainsIllegalCountAndQuantity() throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();
        FeedOfferId feedOfferIdOfIncreasedItem = new FeedOfferId("roadToIncrease", 1L);
        firstItem.setFeedOfferId(feedOfferIdOfIncreasedItem);

        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);

        //увеличиваем но оставляем заполненными count/quantity
        OrderItem increasedItem = order.getItem(feedOfferIdOfIncreasedItem);
        BigDecimal increasedItemQuantity = BigDecimal.valueOf(12.8);
        increasedItem.setQuantity(increasedItemQuantity);
        increasedItem.setCount(3);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(Collections.singleton(increasedItem),
                ClientHelper.crmRobotFor(order),
                order.getId());

        response.andExpect(status().is4xxClientError()).andExpect(jsonPath("code").value(
                ITEM_HAS_AMBIGUOUS_QUANTITY));
    }

    @Test
    @DisplayName("Попытка удалить последний item из заказа")
    public void shouldThrowExceptionWhenDeleteLastItem() throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();
        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(91);
        FeedOfferId feedOfferIdOfDeletedItem = new FeedOfferId("roadToDelete", 1L);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(4);
        fillOrderItem(firstItem, feedOfferIdOfDeletedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);

        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);
        OrderItem deletedItem = orderItemChanges.get(feedOfferIdOfDeletedItem);
        prepareOrderItemForChangeRequest(deletedItem, BigDecimal.ZERO);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order), order.getId());
        response.andExpect(status().is4xxClientError()).andExpect(jsonPath("code").value(
                CANNOT_REMOVE_LAST_ITEM_CODE));

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem expectedUnchangedItem = buildOrderItem(feedOfferIdOfDeletedItem,
                firstItemQuantity, firstItemQuantPrice, firstItemQuantPrice);
        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedUnchangedItem.getFeedOfferId(), expectedUnchangedItem));
    }

    @Test
    public void shouldIncreaseWhenRCContainsOnlyCount() throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();

        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(91);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(4);
        FeedOfferId feedOfferIdOfIncreasedItem = new FeedOfferId("roadToIncrease", 1L);
        fillOrderItem(firstItem, feedOfferIdOfIncreasedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);

        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);

        OrderItem increasedItem = orderItemChanges.get(feedOfferIdOfIncreasedItem);
        int increasedItemCount = 15;
        prepareOrderItemForChangeRequest(increasedItem, BigDecimal.valueOf(increasedItemCount));
        increasedItem.setQuantity(null);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(Collections.singleton(increasedItem),
                ClientHelper.crmRobotFor(order),
                order.getId());
        response.andExpect(status().isOk());

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem expectedIncreasedItem = buildOrderItem(feedOfferIdOfIncreasedItem,
                BigDecimal.valueOf(increasedItemCount),
                firstItemQuantPrice, firstItemQuantPrice);

        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedIncreasedItem.getFeedOfferId(), expectedIncreasedItem));
    }

    @ParameterizedTest(name = "{index} - OrderItem with foodTechId = {0}")
    @ArgumentsSource(FoodTechProvider.class)
    @DisplayName("Удаление товара с признаком isEats и без")
    public void shouldDeleteWhenRCContainsCountEqualsZero(String foodTechId) throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(foodTechId);
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();

        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(91);
        FeedOfferId feedOfferIdOfDeletedItem = new FeedOfferId("roadToDelete", 1L);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(4);
        fillOrderItem(firstItem, feedOfferIdOfDeletedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);

        BigDecimal secondItemQuantPrice = BigDecimal.valueOf(1503.3);
        FeedOfferId feedOfferIdOfUnchangedItem = new FeedOfferId("roadToUnchange", 1L);
        BigDecimal secondItemQuantity = BigDecimal.valueOf(4);
        blueParams.addItem(feedOfferIdOfUnchangedItem, secondItemQuantity.intValue(),
                secondItemQuantity, secondItemQuantPrice);

        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);
        Map<FeedOfferId, OrderItem> orderItemChanges = getOrderItems(order);
        OrderItem deletedItem = orderItemChanges.get(feedOfferIdOfDeletedItem);
        prepareOrderItemForChangeRequest(deletedItem, BigDecimal.ZERO);
        prepareOrderItemForChangeRequest(orderItemChanges.get(feedOfferIdOfUnchangedItem),
                secondItemQuantity);

        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order), order.getId());
        response.andExpect(status().isOk());

        Order actualOrder = orderService.getOrder(order.getId());

        OrderItem expectedUnchangedItem = buildOrderItem(feedOfferIdOfUnchangedItem,
                secondItemQuantity, secondItemQuantPrice, secondItemQuantPrice);

        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedUnchangedItem.getFeedOfferId(), expectedUnchangedItem));
    }

    @ParameterizedTest(name = "{index} - OrderItem with foodTechId = {0}")
    @ArgumentsSource(FoodTechProvider.class)
    @DisplayName("Уменьшение количества товара с признаком isEats и без")
    public void shouldReduceCount(String foodTechId) throws Exception {
        Parameters blueParams = defaultBlueOrderParameters();
        blueParams.getReportParameters().setFoodtechType(foodTechId);
        OrderItem firstItem = blueParams.getOrders().get(0).getItems().iterator().next();
        BigDecimal firstItemQuantPrice = BigDecimal.valueOf(823);
        FeedOfferId feedOfferIdOfReducedItem = new FeedOfferId("roadToIncrease", 1L);
        BigDecimal firstItemQuantity = BigDecimal.valueOf(4);
        fillOrderItem(firstItem, feedOfferIdOfReducedItem, firstItemQuantity,
                firstItemQuantPrice, firstItemQuantPrice);
        Order order = orderService.getOrder(orderCreateHelper.createOrder(blueParams).getId());
        orderPayHelper.payForOrder(order);

        var orderItemChanges = getOrderItems(order);
        //уменьшаем
        int reducedItemCount = 1;
        OrderItem reducedItem = orderItemChanges.get(feedOfferIdOfReducedItem);
        prepareOrderItemForChangeRequest(reducedItem, BigDecimal.valueOf(reducedItemCount));

        ResultActions response = changeOrderItemsHelper.changeOrderItems(orderItemChanges.values(),
                ClientHelper.crmRobotFor(order), order.getId());
        response.andExpect(status().isOk());

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem expectedReducedItem = buildOrderItem(feedOfferIdOfReducedItem,
                BigDecimal.valueOf(reducedItemCount), firstItemQuantPrice, firstItemQuantPrice);

        compareOrderItemsByPricesAndCount(getOrderItems(actualOrder),
                Map.of(expectedReducedItem.getFeedOfferId(), expectedReducedItem));
    }

    private void compareOrderItemsByPricesAndCount(Map<FeedOfferId, OrderItem> actual, Map<FeedOfferId,
            OrderItem> expected) {
        assertThat(actual.size(), is(expected.size()));
        for (Map.Entry<FeedOfferId, OrderItem> expectedEntry : expected.entrySet()) {
            OrderItem actualItem = actual.get(expectedEntry.getKey());
            OrderItem expectedItem = expectedEntry.getValue();
            assertNotNull(actualItem);
            assertThat(actualItem.getCount(), is(expectedItem.getCount()));
            assertNullableBigDecimals(actualItem.getQuantity(), expectedItem.getQuantity());
            assertThat(actualItem.getPrice().stripTrailingZeros(),
                    is(expectedItem.getPrice().stripTrailingZeros()));
            assertThat(actualItem.getQuantPrice().stripTrailingZeros(),
                    is(expectedItem.getQuantPrice().stripTrailingZeros()));
            assertThat(actualItem.getBuyerPrice().stripTrailingZeros(),
                    is(expectedItem.getBuyerPrice().stripTrailingZeros()));
        }
    }

    private OrderItem buildOrderItem(FeedOfferId feedOfferId, BigDecimal quantity,
                                     BigDecimal quantPrice, BigDecimal buyerPrice) {
        OrderItem item = new OrderItem();
        fillOrderItem(item, feedOfferId, quantity, quantPrice, buyerPrice);
        return item;
    }

    private void fillOrderItem(OrderItem item, FeedOfferId feedOfferId, BigDecimal quantity,
                               BigDecimal quantPrice, BigDecimal buyerPrice) {
        item.setFeedOfferId(feedOfferId);
        item.setQuantPrice(quantPrice);
        item.setCount(isIntegerValue(quantity) ? quantity.intValue() : 1);
        item.setQuantity(quantity);
        item.setBuyerPrice(buyerPrice);
        item.setPrice(buyerPrice);
    }

    private void recalcItemPrices(OrderItem item, BigDecimal quantity) {
        if (isIntegerValue(quantity)) {
            return;
        }
        item.getPrices().setSubsidy(item.getPrices().getSubsidy() == null ? null :
                roundPrice(quantity.multiply(item.getPrices().getSubsidy())));
        item.getPrices().setPartnerPrice(item.getPrices().getPartnerPrice() == null ? null :
                roundPrice(quantity.multiply(item.getPrices().getPartnerPrice())));
        item.getPrices().setBuyerDiscount(item.getPrices().getBuyerDiscount() == null ? null :
                roundPrice(quantity.multiply(item.getPrices().getBuyerDiscount())));
        item.getPrices().setFeedPrice(item.getPrices().getFeedPrice() == null ? null :
                roundPrice(quantity.multiply(item.getPrices().getFeedPrice())));
    }

    private Map<FeedOfferId, OrderItem> getOrderItems(Order order) {
        return order.getItems().stream().collect(Collectors.toMap(OrderItem::getFeedOfferId, item -> item));
    }

    private void assertItemPrices(OrderItem actualItem, OrderItem expectedItem) {
        ItemPrices actualPrices = actualItem.getPrices();
        ItemPrices expectedPrices = expectedItem.getPrices();

        assertNullableBigDecimals(actualPrices.getSubsidy(), expectedPrices.getSubsidy());
        assertNullableBigDecimals(actualPrices.getBuyerDiscount(), expectedPrices.getBuyerDiscount());
        assertNullableBigDecimals(actualPrices.getFeedPrice(), expectedPrices.getFeedPrice());
        assertNullableBigDecimals(actualPrices.getPartnerPrice(), expectedPrices.getPartnerPrice());
    }

    private void prepareOrderItemForChangeRequest(OrderItem item, BigDecimal newQuantity) {
        if (isIntegerValue(newQuantity)) {
            item.setCount(newQuantity.intValue());
            item.setQuantity(newQuantity);
        } else {
            item.setCount(1);
            item.setQuantity(newQuantity);
        }
    }

    private void assertNullableBigDecimals(BigDecimal actual, BigDecimal expected) {
        if (expected == null) {
            assertNull(actual);
        } else {
            assertThat(actual.stripTrailingZeros(), is(expected.stripTrailingZeros()));
        }
    }

    private boolean isIntegerValue(BigDecimal bd) {
        return bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0;
    }
}

class QuantityProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(BigDecimal.ONE),
                Arguments.of(BigDecimal.valueOf(3.33)),
                Arguments.of(BigDecimal.valueOf(5))
        );
    }
}

class FoodTechProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(FoodtechType.EDA_RETAIL.getId()),
                Arguments.of("nullId"));
    }
}
