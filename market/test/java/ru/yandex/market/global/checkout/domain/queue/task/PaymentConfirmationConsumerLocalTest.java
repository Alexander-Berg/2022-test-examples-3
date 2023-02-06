package ru.yandex.market.global.checkout.domain.queue.task;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.queue.task.receipt.PaymentCancellationConsumer;
import ru.yandex.market.global.checkout.domain.queue.task.receipt.PaymentConfirmationConsumer;
import ru.yandex.market.global.checkout.domain.shop.ShopQueryService;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.db.jooq.embeddables.pojos.Address;
import ru.yandex.market.global.db.jooq.enums.EOrderPaymentCurrency;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;

import static ru.yandex.market.global.db.jooq.enums.EPlusActionType.SPEND;

@Disabled
public class PaymentConfirmationConsumerLocalTest extends BaseLocalTest {
    @Autowired
    private QueueShard<DatabaseAccessLayer> shard;

    @Autowired
    private PaymentConfirmationConsumer paymentConfirmationConsumer;
    @Autowired
    private PaymentCancellationConsumer paymentCancellationProducer;

    @Autowired
    private TestOrderFactory testOrderFactory;

    @MockBean
    private ShopQueryService shopQueryService;


    private static final String EZ_COUNT_API_KEY = "1326a5e4af4051ea5e18ed51c7ccea290e18b08a586a2960c6b040ee8f26f79f";
    private static final Long UID = 4092490744L;

    @BeforeEach
    public void beforeTest() {
        Mockito.when(shopQueryService.loadEzEzcountApiKeyByShop(Mockito.anyLong()))
                .thenReturn(EZ_COUNT_API_KEY);
    }

    private OrderItem someItem(String offerId, String name, long count, long price, long plusPent) {
        return new OrderItem()
                .setBusinessId(1L)
                .setShopId(1L)
                .setCategoryId(1L)
                .setOfferId(offerId)
                .setName(name)
                .setCount(count)
                .setPrice(price)
                .setPlusSpent(plusPent)
                .setTotalCost(price*count)
                .setTotalCostWithoutPromo(price*count)
                .setVendor("123")
                .setCurrency(EOrderPaymentCurrency.ILS)
                .setAdult(false)
                .setVat(BigDecimal.valueOf(17L));
    }


    // ezcount имеет ключи идемпотентности, который у нас генерится на основе orderId
    // чтоб ezcount выбил новый чек нужно послать совершенно новый orderId
    private OrderModel createOrder(long orderId) {
        Address address = new Address()
                .setCountry("Israel")
                .setRegion("Tel Aviv")
                .setLocality("Tel Aviv")
                .setStreet("Dizengoff")
                .setHouse("50");
        return testOrderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .setupOrder(it -> it
                                .setUid(UID)
                                .setId(orderId)
                                .setTotalCost(1209_00L)
                                .setTotalItemsCost(1200_00L)
                                .setTotalItemsCostWithPromo(1200_00L)
                                .setDeliveryCostForRecipient(9_00L)
                                .setDeliveryCostForShop(20_00L)
                                .setPlusAction(SPEND)
                                .setPlusAvailableAmount(1000_00L)
                                .setPlusSpent(100_00L)
                        )
                        .setupDelivery(it -> it
                                .setRecipientFirstName("Alexey")
                                .setRecipientLastName("Alexeev")
                                .setRecipientPhone("+972012345678")
                                .setRecipientAddress(address)
                                .setShopAddress(address)
                                .setShopName("Test Shop"))
                        .setupPayment(it -> it
                                .setPaymentCancellationUrl(null)
                                .setPaymentConfirmationUrl(null)
                                .setTrustPaymethodId("cardx-" + "-555-123456-12314141") //apple-token-* for Apple Pay
                                .setCardType("visa") //null for Apple Pay
                                .setUserAccount("12345*******4433") // null for Apple Pay
                                .setPaymentMethodDisplayName("cardXXXX")
                        )
                        .setupItems(it -> List.of(
                                someItem("SHOP1-DRTS", "Doritos 100g", 30L, 20_00L, 22_20),
                                someItem("SHOP1-MTNDEW", "Mountain Dew 1,5L", 15L, 40_00L, 33_30)
                        ))
                        .build()
        );
    }

    /*
    Ez count по умолчанию выбивает чеки на иврите. Для того чтоб получить чек на английском необходимо в создании
    документа createDoc в объекте createDocRequest заслать поле lang = en
    После чего - нужно получать PDF которая будет приходить в респонсе в поле pdfLinkCopyForeign
    Полезно при отладке
     */

    public static final long ORDER_ID = 123472L;

    @Test
    public void testConfirmation() {
        OrderModel order = createOrder(ORDER_ID);
        TestQueueTaskRunner.runTaskThrowOnFail(paymentConfirmationConsumer, order.getOrder().getId());
    }

    @Test
    public void testCancellation() {
        OrderModel order = createOrder(ORDER_ID);
        TestQueueTaskRunner.runTaskThrowOnFail(paymentCancellationProducer, order.getOrder().getId());
    }
}
