package ru.yandex.market.wrap.infor.service.order.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.Delivery;
import ru.yandex.market.logistic.api.model.fulfillment.Email;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Location;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.Recipient;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.model.OrderDTO;
import ru.yandex.market.wrap.infor.client.model.OrderDetailDTO;
import ru.yandex.market.wrap.infor.entity.InforUnitId;
import ru.yandex.market.wrap.infor.model.OrderType;

/**
 * Unit тесты для {@link FulfilmentOrderToInforOrderConverter}.
 *
 * @author avetokhin 17.10.18.
 */
class OrderConverterTest extends SoftAssertionSupport {

    private static final String YANDEX_ID = "yandexId";
    private static final String PARTNER_ID = "partnerId";

    private static final String YANDEX_DELIVERY_ID = "yandexDeliveryId";
    private static final String PARTNER_DELIVERY_ID = "partnerDeliveryId";
    private static final DateTime SHIPMENT_DATE = DateTime.fromLocalDateTime(LocalDateTime.of(2018, 10, 10, 12, 0, 0));
    private static final String COUNTRY = "Российская Федерация";
    private static final String REGION = "Новосибирская область";
    private static final String CITY = "Бердск";
    private static final String LARGE_STREET = "Территория, изъятая из земель подсобного хозяйства Всесоюзного " +
        "центрального совета профессиональных союзов для организации. + еще немного текста, так как самая " +
        "длинная улица в России не достаточно длинная для нашего теста, тесты есть тесты, приходится выкручиваться";

    private static final UnitId UNIT_1 = new UnitId("100501", 10L, "sku1");
    private static final UnitId UNIT_2 = new UnitId("100502", 20L, "sku2");
    private static final Map<UnitId, InforUnitId> MAPPING =
        ImmutableMap.<UnitId, InforUnitId>builder()
            .put(UNIT_1, InforUnitId.of("inforSku1", 10L))
            .put(UNIT_2, InforUnitId.of("inforSku2", 20L))
            .build();

    private FulfilmentOrderToInforOrderConverter converter = new FulfilmentOrderToInforOrderConverter();

    private static final String LARGE_COMMENT_PART_1 = StringUtils.repeat("x", 2000);

    private static final String LARGE_COMMENT_PART_2 = StringUtils.repeat("y", 100);

    /**
     * Сценарий #1:
     * <p>
     * Проверяет корректность конвертации комментария, который заполняет одно поле.
     */
    @Test
    void singleFieldComment() {
        final Order.OrderBuilder orderBuilder = orderBuilder();
        orderBuilder.setComment(LARGE_COMMENT_PART_1);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);
        softly.assertThat(orderDTO.getNotes()).isEqualTo(LARGE_COMMENT_PART_1);
    }

    /**
     * Сценарий #2:
     * <p>
     * Проверяет корректность конвертации комментария, который заполняет два поля целиком.
     */
    @Test
    void twoFieldsComment() {
        final Order.OrderBuilder orderBuilder = orderBuilder();
        orderBuilder.setComment(LARGE_COMMENT_PART_1 + " " + LARGE_COMMENT_PART_2);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);
        softly.assertThat(orderDTO.getNotes()).isEqualTo(LARGE_COMMENT_PART_1);
        softly.assertThat(orderDTO.getNotes2()).isEqualTo(LARGE_COMMENT_PART_2);
    }

    /**
     * Сценарий #3:
     * <p>
     * Проверяет корректность конвертации комментария, который не помещается в два поля. Не поместившиеся остатки
     * игнорируются.
     */
    @Test
    void tooLargeComment() {
        final Order.OrderBuilder orderBuilder = orderBuilder();
        orderBuilder.setComment(LARGE_COMMENT_PART_1 + " " + LARGE_COMMENT_PART_1 + " " + LARGE_COMMENT_PART_2);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);
        softly.assertThat(orderDTO.getNotes()).isEqualTo(LARGE_COMMENT_PART_1);
        softly.assertThat(orderDTO.getNotes2()).isEqualTo(LARGE_COMMENT_PART_1);
    }

    /**
     * Сценарий #4:
     * <p>
     * Проверяет корректность конвертации адреса, когда он полностью заполнен и занимает все поля.
     */
    @Test
    void fulfilledAddress() {
        Location locationTo = new Location.LocationBuilder(COUNTRY, REGION, CITY)
            .setStreet(LARGE_STREET)
            .setHouse("10")
            .setBuilding("2")
            .setHousing("4")
            .setPorch("1")
            .setFloor(12)
            .build();

        final Order.OrderBuilder orderBuilder = orderBuilder(locationTo);
        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);

        softly.assertThat(orderDTO.getCaddress1()).isEqualTo("Территория, изъятая из земель подсобного");
        softly.assertThat(orderDTO.getCaddress2()).isEqualTo("хозяйства Всесоюзного центрального совета");
        softly.assertThat(orderDTO.getCaddress3()).isEqualTo("профессиональных союзов для организации. +");
        softly.assertThat(orderDTO.getCaddress4()).isEqualTo("еще немного текста, так как самая длинная");
        softly.assertThat(orderDTO.getCaddress5()).isEqualTo("улица в России не достаточно длинная для");
        softly.assertThat(orderDTO.getCaddress6()).isEqualTo("нашего теста, тесты есть тесты, приходится выкручиваться, 10, 2 стр., 4 кор., 1 под., 12 эт.");
    }

    /**
     * Сценарий #5:
     * <p>
     * Проверяет корректность конвертации адреса, когда он частично заполнен.
     */
    @Test
    void partialAddress() {
        Location locationTo = new Location.LocationBuilder(COUNTRY, REGION, CITY)
            .setStreet("60 лет Октября")
            .setHouse("10")
            .setPorch("1")
            .setFloor(12)
            .build();
        final Order.OrderBuilder orderBuilder = orderBuilder(locationTo);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);
        softly.assertThat(orderDTO.getCaddress1()).isEqualTo("60 лет Октября, 10, 1 под., 12 эт.");
        softly.assertThat(orderDTO.getCaddress2()).isNull();
        softly.assertThat(orderDTO.getCaddress3()).isNull();
        softly.assertThat(orderDTO.getCaddress4()).isNull();
        softly.assertThat(orderDTO.getCaddress5()).isNull();
        softly.assertThat(orderDTO.getCaddress6()).isNull();

    }

    /**
     * Сценарий #6:
     * <p>
     * Проверяет корректность конвертации полностью заполненного получателя.
     */
    @Test
    void fulfilledRecipient() {
        final Person fio = new Person.PersonBuilder("Вольф")
            .setSurname("Мессинг")
            .setPatronymic("Григорьевич")
            .build();

        // Конвертируется только два номера. Слишком длинные номера игнорируются.
        final Recipient recipient = new Recipient.RecipientBuilder(
            fio,
            Arrays.asList(
                new Phone("555777", "03"),
                new Phone("555777123123123", "03123123"),
                new Phone("1234555777123123123", null),
                new Phone("+7 923-456-7799", null),
                new Phone("3535555", null)
            ))
            .setEmail(new Email("test@mail.ru"))
            .build();

        final Order.OrderBuilder orderBuilder = orderBuilder(recipient);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);

        softly.assertThat(orderDTO.getCcompany()).isEqualTo("Мессинг Вольф Григорьевич");
        softly.assertThat(orderDTO.getCemail1()).isEqualTo("test@mail.ru");
        softly.assertThat(orderDTO.getCphone1()).isEqualTo("555777 доп. 03");
        softly.assertThat(orderDTO.getCphone2()).isEqualTo("+7 923-456-7799");
    }

    /**
     * Сценарий #7.1:
     * <p>
     * Проверяет корректность конвертации получателя со слишком длинным отчеством, у него обрезается ФИО.
     */
    @Test
    void tooLargePatronymicRecipient() {
        final Person fio = new Person.PersonBuilder("Вольфффффф")
            .setSurname("Мессинггггггг")
            .setPatronymic("Григорьевиииииииииииииииииииич")
            .build();

        final Recipient recipient = new Recipient.RecipientBuilder(fio, Collections.emptyList()).build();
        final Order.OrderBuilder orderBuilder = orderBuilder(recipient);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);

        softly.assertThat(orderDTO.getCcompany()).isEqualTo("Мессинггггггг Вольфффффф Григорьевииииииии...");
    }

    /**
     * Сценарий #7.1:
     * <p>
     * Проверяет корректность конвертации получателя со слишком длинным именем, у него обрезается ФИО.
     */
    @Test
    void tooLargeNameRecipient() {
        final Person fio = new Person.PersonBuilder("Вольфффффффффффффффффффффффффффффффффффффф")
            .setSurname("Мессинггггггг")
            .setPatronymic("Григорьевиииииииииииииииииииич")
            .build();

        final Recipient recipient = new Recipient.RecipientBuilder(fio, Collections.emptyList()).build();
        final Order.OrderBuilder orderBuilder = orderBuilder(recipient);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);

        softly.assertThat(orderDTO.getCcompany()).isEqualTo("Мессинггггггг Вольфффффффффффффффффффффффф...");
    }

    /**
     * Сценарий #7.3:
     * <p>
     * Проверяет корректность конвертации получателя со слишком длинной фамилией, у него обрезается ФИО.
     */
    @Test
    void tooLargeSurnameRecipient() {
        final Person fio = new Person.PersonBuilder("Вольфффффф")
            .setSurname("Мессинггггггггггггггггггггггггггггггггггггг")
            .setPatronymic("Григорьевиииииииииииииииииииич")
            .build();

        final Recipient recipient = new Recipient.RecipientBuilder(fio, Collections.emptyList()).build();
        final Order.OrderBuilder orderBuilder = orderBuilder(recipient);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);

        softly.assertThat(orderDTO.getCcompany()).isEqualTo("Мессингггггггггггггггггггггггггггггггггггг...");
    }

    /**
     * Сценарий #8:
     * <p>
     * Проверяет корректность конвертации получателя с неполным именем.
     */
    @Test
    void notFullNameRecipient() {
        final Person fio = new Person.PersonBuilder("Вольф").setPatronymic("Григорьевич").build();

        final Recipient recipient = new Recipient.RecipientBuilder(fio, Collections.emptyList()).build();
        final Order.OrderBuilder orderBuilder = orderBuilder(recipient);

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);

        softly.assertThat(orderDTO.getCcompany()).isEqualTo("Вольф Григорьевич");

    }

    /**
     * Сценарий #9:
     * <p>
     * Преверяет корректность конвертации строк заказа.
     */
    @Test
    void items() {
        final Item item1 = new Item.ItemBuilder(null, 500, new BigDecimal("100.40"))
            .setUnitId(UNIT_1)
            .build();

        final Item item2 = new Item.ItemBuilder(null, 600, new BigDecimal("0.23"))
            .setUnitId(UNIT_2)
            .build();

        final Order.OrderBuilder orderBuilder = orderBuilder(Arrays.asList(item1, item2));

        final OrderDTO orderDTO = convert(orderBuilder.build());
        basicAssert(orderDTO);

        final List<OrderDetailDTO> orderDetails = orderDTO.getOrderdetails();
        softly.assertThat(orderDetails).isNotNull();
        softly.assertThat(orderDetails).hasSize(2);

        final OrderDetailDTO orderDetail1 = orderDetails.get(0);
        softly.assertThat(orderDetail1.getSku()).isEqualTo("inforSku1");
        softly.assertThat(orderDetail1.getStorerkey()).isEqualTo("10");
        softly.assertThat(orderDetail1.getOriginalqty()).isEqualTo(new BigDecimal("500"));
        softly.assertThat(orderDetail1.getRotation()).isEqualTo("1");
        softly.assertThat(orderDetail1.getSkurotation()).isEqualTo("Lot");
        softly.assertThat(orderDetail1.getLottable08()).isEqualTo("1");
        softly.assertThat(orderDetail1.getExtendedprice()).isEqualTo(100.4D);

        final OrderDetailDTO orderDetail2 = orderDetails.get(1);
        softly.assertThat(orderDetail2.getSku()).isEqualTo("inforSku2");
        softly.assertThat(orderDetail2.getStorerkey()).isEqualTo("20");
        softly.assertThat(orderDetail2.getOriginalqty()).isEqualTo(new BigDecimal("600"));
        softly.assertThat(orderDetail2.getRotation()).isEqualTo("1");
        softly.assertThat(orderDetail2.getSkurotation()).isEqualTo("Lot");
        softly.assertThat(orderDetail2.getLottable08()).isEqualTo("1");
        softly.assertThat(orderDetail2.getExtendedprice()).isEqualTo(0.23D);

        softly.assertThat(orderDTO.getStorerkey()).isEqualTo("10");
    }

    private OrderDTO convert(Order order) {
        return converter.convert(order, MAPPING, false);
    }

    private void basicAssert(final OrderDTO orderDTO) {
        softly.assertThat(orderDTO).isNotNull();
        softly.assertThat(orderDTO.getExternorderkey()).isEqualTo(YANDEX_ID);
        softly.assertThat(orderDTO.getScheduledshipdate()).isEqualTo(SHIPMENT_DATE.getOffsetDateTime());
        softly.assertThat(orderDTO.getType()).isEqualTo(OrderType.STANDARD.getInforCode());
        softly.assertThat(orderDTO.getCarriercode()).isEqualTo(YANDEX_DELIVERY_ID);
        softly.assertThat(orderDTO.getCcountry()).isEqualTo(COUNTRY);
        softly.assertThat(orderDTO.getCstate()).isEqualTo(REGION);
        softly.assertThat(orderDTO.getCcity()).isEqualTo(CITY);
    }

    private static Order.OrderBuilder orderBuilder(List<Item> items) {
        final Location locationTo = new Location.LocationBuilder(COUNTRY, REGION, CITY).build();
        return orderBuilder(items, null, locationTo);
    }

    private static Order.OrderBuilder orderBuilder(Recipient recipient) {
        final Location locationTo = new Location.LocationBuilder(COUNTRY, REGION, CITY).build();
        return orderBuilder(Collections.emptyList(), recipient, locationTo);
    }

    private static Order.OrderBuilder orderBuilder(Location locationTo) {
        return orderBuilder(Collections.emptyList(), null, locationTo);
    }

    private static Order.OrderBuilder orderBuilder() {
        final Location locationTo = new Location.LocationBuilder(COUNTRY, REGION, CITY).build();
        return orderBuilder(Collections.emptyList(), null, locationTo);
    }

    private static Order.OrderBuilder orderBuilder(List<Item> items, Recipient recipient, Location locationTo) {
        final Delivery delivery = new Delivery.DeliveryBuilder(
            null,
            null,
            null,
            null,
            null)
            .setDeliveryId(new ResourceId(YANDEX_DELIVERY_ID, PARTNER_DELIVERY_ID))
            .build();

        return new Order.OrderBuilder(new ResourceId(YANDEX_ID, PARTNER_ID),
            locationTo,
            items,
            null,
            null,
            null,
            null,
            delivery,
            null,
            null,
            null,
            null,
            null,
            recipient,
            null,
            null)
            .setShipmentDate(SHIPMENT_DATE);
    }

}
