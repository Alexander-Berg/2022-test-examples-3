package ru.yandex.market.billing.imports.globalorder.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.billing.imports.globalcontract.GlobalCheckouterShopContract;
import ru.yandex.market.billing.imports.globalorder.model.GlobalCheckouterOrder;
import ru.yandex.market.billing.imports.globalorder.model.GlobalCheckouterOrderItem;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderDeliveryStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderPaymentStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderShopStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderStatus;
import ru.yandex.market.core.currency.Currency;
import ru.yandex.market.core.partner.model.PartnerContractType;

import static ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree.booleanNode;
import static ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree.integerNode;
import static ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree.longNode;
import static ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree.longOrNullNode;
import static ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree.stringNode;

@ParametersAreNonnullByDefault
public class GlobalCheckouterTestUtil {
    private GlobalCheckouterTestUtil() {
    }

    public static GlobalCheckouterOrder.Builder defaultCheckouterOrder(long orderId) {
        return GlobalCheckouterOrder.builder()
                .setId(orderId)
                .setShopId(-7777777L)
                .setPaymentId(666L)
                .setSubsidyPaymentId(null)
                .setStatus(GlobalOrderStatus.FINISHED)
                .setDeliveryStatus(GlobalOrderDeliveryStatus.ORDER_DELIVERED)
                .setPaymentStatus(GlobalOrderPaymentStatus.CLEARED)
                .setShopStatus(GlobalOrderShopStatus.READY)
                .setCurrency(Currency.ILS)
                .setItemsTotal(10000L)
                .setSubsidyTotal(1000L)
                .setProcessInBillingAt(OffsetDateTime.parse("2021-11-02T03:00:00+03").toInstant())
                .setCreatedAt(OffsetDateTime.parse("2021-11-02T03:00:00+03").toInstant());
    }

    public static GlobalCheckouterOrderItem.Builder defaultCheckouterOrderItem(long itemId, long orderId) {
        return GlobalCheckouterOrderItem.builder()
                .setId(itemId)
                .setOrderId(orderId)
                .setOfferId("test-offer-" + itemId)
                .setOfferName("אבקת חלב רזה")
                .setPrice(1000)
                .setSubsidy(10)
                .setCount(1)
                .setMarketCategoryId(234);
    }

    public static GlobalCheckouterShopContract.Builder defaultShopContract(PartnerContractType contractType) {
        return GlobalCheckouterShopContract.builder()
                .setShopId(RandomUtils.nextLong())
                .setClientId(RandomUtils.nextLong())
                .setContractType(contractType)
                .setContractId(RandomUtils.nextLong())
                .setContractDate(Instant.now())
                .setPersonId(RandomUtils.nextLong());
    }

    public static GlobalCheckouterShopContract.Builder defaultShopContract(
            long shopId,
            PartnerContractType contractType,
            Instant contractDate
    ) {
        var clientId = 100 * shopId + 1;
        return GlobalCheckouterShopContract.builder()
                .setShopId(shopId)
                .setClientId(clientId)
                .setContractType(contractType)
                .setContractId(shopId * 1000 + (contractType.equals(PartnerContractType.INCOME) ? 609 : 610))
                .setContractDate(contractDate)
                .setPersonId(clientId * 100 + (contractType.equals(PartnerContractType.INCOME) ? 1 : 2));
    }

    public static YTreeMapNode toYTreeMapNode(GlobalCheckouterOrder order, LocalDate date) {
        var node = new YTreeMapNodeImpl(new HashMap<>());
        node.put("id", longNode(order.getId()));
        node.put("shop_id", longNode(order.getShopId()));
        node.put("payment_id", longNode(order.getPaymentId()));
        node.put("subsidy_payment_id", longOrNullNode(order.getSubsidyPaymentId()));
        node.put("created_at", stringNode(order.getCreatedAt().toString()));
        node.put("process_in_billing_at", stringNode(order.getProcessInBillingAt().toString()));
        node.put("total_sum", longNode(order.getItemsTotal()));
        node.put("total_subsidy", longNode(order.getSubsidyTotal()));
        node.put("delivery_cost_for_shop_is_zero", booleanNode(order.getFreeDeliveryForShop()));
        node.put("order_state", stringNode(order.getStatus().getId().toUpperCase()));
        node.put("delivery_state", stringNode(order.getDeliveryStatus().getId().toUpperCase()));
        node.put("payment_state", stringNode("CLEARED"));
        node.put("shop_state", stringNode("READY"));
        node.put("currency", stringNode(order.getCurrency().getId()));
        node.put("date", stringNode(date.toString()));
        return node;
    }

    public static YTreeMapNode toYTreeMapNode(GlobalCheckouterOrderItem item, LocalDate date) {
        var node = new YTreeMapNodeImpl(new HashMap<>());
        node.put("id", longNode(item.getId()));
        node.put("order_id", longNode(item.getOrderId()));
        node.put("price", longNode(item.getPrice()));
        node.put("subsidy", longNode(item.getSubsidy()));
        node.put("count", integerNode(item.getCount()));
        node.put("market_category_id", longNode(item.getMarketCategoryId()));
        node.put("shop_category_id", longOrNullNode(item.getShopCategoryId()));
        node.put("offer_id", stringNode(item.getOfferId()));
        node.put("offer_name", stringNode(item.getOfferName()));
        node.put("date", stringNode(date.toString()));
        return node;
    }

    public static YTreeMapNode toYTreeMapNode(GlobalCheckouterShopContract contract, LocalDate date) {
        var node = new YTreeMapNodeImpl(new HashMap<>());
        node.put("shop_id", longNode(contract.getShopId()));
        node.put("client_id", longNode(contract.getClientId()));
        node.put("person_id", longNode(contract.getPersonId()));
        node.put("contract_type", stringNode(contract.getContractType().getId()));
        node.put("contract_id", longNode(contract.getContractId()));
        node.put("contract_date", stringNode(contract.getContractDate().toString()));
        node.put("balance_contract", stringNode(contract.getBalanceContract()));
        node.put("date", stringNode(date.toString()));
        return node;
    }

}
