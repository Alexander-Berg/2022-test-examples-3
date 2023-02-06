package ru.yandex.market.logistic.gateway.client;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;

import ru.yandex.market.logistic.gateway.client.config.ProxySettings;
import ru.yandex.market.logistic.gateway.client.config.SqsProperties;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Supplier;
import ru.yandex.market.logistic.gateway.common.model.delivery.Tax;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaxType;
import ru.yandex.market.logistic.gateway.common.model.delivery.VatValue;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistic.gateway.utils.DeliveryDtoFactory;

import static ru.yandex.market.logistic.gateway.utils.DeliveryDtoFactory.createOrderBuilder;

public class DeliverySqsClientTest extends AbstractSqsClientTest {

    private static final int BIG_PAYLOAD_COMMENT_LEN = 300 * 1024;

    private static final String PROCESS_ID = "processIdABC123";

    private static final ClientRequestMeta CLIENT_REQUEST_META = new ClientRequestMeta(PROCESS_ID);

    private DeliveryClient deliveryClient;

    @Before
    public void setup() {
        deliveryClient = DeliveryClientFactory.getDeliveryClient(new SqsProperties()
            .setRegion(DEFAULT_REGION)
            .setS3EndpointHost(getS3ConnectionString())
            .setSqsEndpointHost(getSqsConnectionString())
            .setS3BucketName(BUCKET_NAME)
            .setS3AccessKey("")
            .setS3SecretKey("")
            .setSqsAccessKey("")
            .setSqsSecretKey("")
            .setSqsSessionToken("")
            .setProxySettings(new ProxySettings(DockerClientFactory.instance().dockerHostIpAddress(),
                sqsContainer.getMappedPort(SQS_PORT))),
            null,
            null);
    }

    @Test
    public void testCreateOrderSuccessSent() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createOrder();
        Partner partner = DeliveryDtoFactory.createPartner();

        deliveryClient.createOrder(order, partner);
    }

    @Test
    public void testCreateOrderSuccessSentWithProcessId() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createOrder();
        Partner partner = DeliveryDtoFactory.createPartner();

        deliveryClient.createOrder(order, partner, CLIENT_REQUEST_META);
    }

    @Test
    public void testCreateOrderWithBigPayloadSuccessSent() throws GatewayApiException {
        Order order = createOrderBuilder()
            .setComment(RandomStringUtils.randomAlphanumeric(BIG_PAYLOAD_COMMENT_LEN))
            .build();
        Partner partner = DeliveryDtoFactory.createPartner();

        deliveryClient.createOrder(order, partner);
    }

    @Test
    public void testCreateOrderWithBigPayloadSuccessSentWithProcessId() throws GatewayApiException {
        Order order = createOrderBuilder()
            .setComment(RandomStringUtils.randomAlphanumeric(BIG_PAYLOAD_COMMENT_LEN))
            .build();
        Partner partner = DeliveryDtoFactory.createPartner();

        deliveryClient.createOrder(order, partner, CLIENT_REQUEST_META);
    }

    @Test
    public void testCreateOrderWithItemSupplier() throws GatewayApiException {
        Order order = createOrderBuilder(Collections.singletonList(
            new Item.ItemBuilder("Бутылка", 3, BigDecimal.valueOf(50.0))
                .setSupplier(Supplier.builder().setInn("2342342342356").build())
                .build()
        ))
            .build();
        Partner partner = DeliveryDtoFactory.createPartner();

        deliveryClient.createOrder(order, partner, CLIENT_REQUEST_META);
    }

    @Test
    public void testUpdateOrderItemsSuccess() throws GatewayApiException {

        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(654L);
        List<Item> items = Collections.singletonList(new Item.ItemBuilder("Пеленальный комод Ведрусс Мишка №2 слоновая кость-венге",
            1,
            BigDecimal.valueOf(135))
            .setArticle("475690200345480.Checkouter-test-20")
            .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.EIGHTEEN)))
            .setItemDescriptionEnglish("Changing dresser Bucket Bear No. 2 ivory-wenge." +
                "Consists of: Dresser, Bucket, Bear, Ivory, Your Parcel." +
                "Age of consumer: 21." +
                "Gender: male." +
                "Manufacturer: Made in China.")
            .build());

        deliveryClient.updateOrderItems(
            new OrderItems(
                orderId,
                BigDecimal.valueOf(1100),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1),
                120,
                70,
                30,
                items),
            partner,
            CLIENT_REQUEST_META
        );
    }
}
