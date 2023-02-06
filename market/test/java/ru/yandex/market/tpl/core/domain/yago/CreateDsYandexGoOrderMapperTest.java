package ru.yandex.market.tpl.core.domain.yago;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Validator;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.util.ResourceUtils;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistic.api.model.delivery.DeliveryType;
import ru.yandex.market.logistic.api.model.delivery.Item;
import ru.yandex.market.logistic.api.model.delivery.Korobyte;
import ru.yandex.market.logistic.api.model.delivery.Location;
import ru.yandex.market.logistic.api.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.api.model.delivery.Person;
import ru.yandex.market.logistic.api.model.delivery.Phone;
import ru.yandex.market.logistic.api.model.delivery.Recipient;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.Sender;
import ru.yandex.market.logistic.api.model.delivery.Warehouse;
import ru.yandex.market.logistic.api.model.delivery.WorkTime;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.TimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.ds.DsOrderManager;
import ru.yandex.market.tpl.core.domain.ds.DsOrderMapper;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.region.RegionDao;
import ru.yandex.market.tpl.core.domain.region.TplRegion;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CreateDsYandexGoOrderMapperTest {

    public static final String EXPECTED_PHONE_NUMBER = "88000000000";
    private final CreateDsYandexGoOrderMapper dsOrderMapper;
    private final DsOrderManager dsOrderManager;
    private final TestDataFactory testDataFactory;
    private final ShiftManager shiftManager;
    private final UserShiftRepository userShiftRepository;
    private final TestUserHelper testUserHelper;
    private final PartnerRepository<DeliveryService> partnerRepository;
    private final Validator validator;
    private final OrderRepository orderRepository;

    @MockBean
    private RegionDao regionDao;

    @SpyBean
    private YandexGoOrderProperties yandexGoOrderProperties;

    private final Dimensions ORDER_DIMENSIONS = new Dimensions(
            DsOrderMapper.DEFAULT_MAX_WEIGHT_FOR_ORDER_PLACE.subtract(BigDecimal.valueOf(10)),
            30,
            50,
            60
    );
    private final String ORDER_YANDEX_ID = "123";

    private static final BigDecimal ZERO_GEO_VALUE = new BigDecimal("0.000000");

    @Test
    void isInvalidPlaceDimensions() {
        Korobyte overWeighted = new Korobyte.KorobyteBuilder()
                .setWeightGross(DsOrderMapper.DEFAULT_MAX_WEIGHT_FOR_ORDER_PLACE.add(new BigDecimal(1)))
                .setHeight(DsOrderMapper.DEFAULT_MAX_SIDE_LENGTH)
                .setWidth(1)
                .setLength(1)
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overWeighted, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();

        Korobyte overHeight = new Korobyte.KorobyteBuilder()
                .setWeightGross(DsOrderMapper.DEFAULT_MAX_WEIGHT_FOR_ORDER_PLACE)
                .setHeight(DsOrderMapper.DEFAULT_MAX_SIDE_LENGTH + 1)
                .setWidth(1)
                .setLength(1)
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overHeight, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();

        Korobyte overWidth = new Korobyte.KorobyteBuilder()
                .setWeightGross(DsOrderMapper.DEFAULT_MAX_WEIGHT_FOR_ORDER_PLACE)
                .setHeight(1)
                .setWidth(DsOrderMapper.DEFAULT_MAX_SIDE_LENGTH + 1)
                .setLength(1)
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overWidth, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();

        Korobyte overLength = new Korobyte.KorobyteBuilder()
                .setWeightGross(DsOrderMapper.DEFAULT_MAX_WEIGHT_FOR_ORDER_PLACE)
                .setHeight(1)
                .setWidth(1)
                .setLength(DsOrderMapper.DEFAULT_MAX_SIDE_LENGTH + 1)
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overLength, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();

        Korobyte overSumSideLength = new Korobyte.KorobyteBuilder()
                .setWeightGross(DsOrderMapper.DEFAULT_MAX_WEIGHT_FOR_ORDER_PLACE)
                .setHeight(DsOrderMapper.DEFAULT_MAX_SIDE_LENGTH)
                .setWidth(DsOrderMapper.DEFAULT_MAX_SIDE_LENGTH * 2)
                .setLength(DsOrderMapper.DEFAULT_MAX_SIDE_LENGTH)
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overSumSideLength, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();

        Korobyte overOrderWeight = new Korobyte.KorobyteBuilder()
                .setWeightGross(ORDER_DIMENSIONS.getWeight().add(BigDecimal.ONE))
                .setHeight(ORDER_DIMENSIONS.getHeight())
                .setWidth(ORDER_DIMENSIONS.getWidth())
                .setLength(ORDER_DIMENSIONS.getLength())
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overOrderWeight, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();
        Korobyte overOrderHeight = new Korobyte.KorobyteBuilder()
                .setWeightGross(ORDER_DIMENSIONS.getWeight())
                .setHeight(ORDER_DIMENSIONS.getHeight() + 1)
                .setWidth(ORDER_DIMENSIONS.getWidth())
                .setLength(ORDER_DIMENSIONS.getLength())
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overOrderHeight, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();
        Korobyte overOrderWidth = new Korobyte.KorobyteBuilder()
                .setWeightGross(ORDER_DIMENSIONS.getWeight())
                .setHeight(ORDER_DIMENSIONS.getHeight())
                .setWidth(ORDER_DIMENSIONS.getWidth() + 1)
                .setLength(ORDER_DIMENSIONS.getLength())
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overOrderWidth, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();
        Korobyte overOrderLength = new Korobyte.KorobyteBuilder()
                .setWeightGross(ORDER_DIMENSIONS.getWeight())
                .setHeight(ORDER_DIMENSIONS.getHeight())
                .setWidth(ORDER_DIMENSIONS.getWidth())
                .setLength(ORDER_DIMENSIONS.getLength() + 1)
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(overOrderLength, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isTrue();

        Korobyte normal = new Korobyte.KorobyteBuilder()
                .setWeightGross(ORDER_DIMENSIONS.getWeight())
                .setHeight(ORDER_DIMENSIONS.getHeight())
                .setWidth(ORDER_DIMENSIONS.getWidth())
                .setLength(ORDER_DIMENSIONS.getLength())
                .build();
        assertThat(dsOrderMapper.isInvalidOrderPlaceDimensions(normal, ORDER_DIMENSIONS, ORDER_YANDEX_ID))
                .isFalse();
    }

    protected final String getFileContent(String filename) throws IOException {
        return Files.readString(ResourceUtils.getFile("classpath:" + filename).toPath(), StandardCharsets.UTF_8);
    }

    @Test
    void shouldReturnExpectedLogisticOrder_whenGetDsOrder() {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        Clock clock = Clock.fixed(ZonedDateTime.of(LocalDateTime.of(2021, 12, 31, 12, 58), zoneId).toInstant(), zoneId);
        DateTime deliveryDate = DateTime.fromLocalDateTime(LocalDateTime.now(clock));
        testOrderMapping(clock, deliveryDate);
    }

    @Test
    void shouldReturnExpectedLogisticOrder_whenGetDsOrder_ifOrderDeliveryDateInThePast() {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        Clock clock = Clock.fixed(ZonedDateTime.of(LocalDateTime.of(2021, 12, 31, 12, 58), zoneId).toInstant(), zoneId);
        DateTime deliveryDate = DateTime.fromLocalDateTime(LocalDateTime.now(clock).minusDays(1));
        testOrderMapping(clock, deliveryDate);
    }

    private void testOrderMapping(Clock clock, DateTime deliveryDate) {
        // given
        String ogrn = "ogrn";
        String incorporation = "incorporation";
        String RUSSIA = "Россия";
        String MOSCOW_REGION = "Москва (регион)";
        String MOSCOW_CITY = "Москва";

        String itemName = "item-name";
        BigDecimal itemPrice = new BigDecimal("212.00");
        int itemCount = 2;
        BigDecimal cargoTotalCost = itemPrice.multiply(BigDecimal.valueOf(itemCount));
        BigDecimal deliveryCost = new BigDecimal("1000.00");
        BigDecimal totalCost = new BigDecimal("1424.00");

        TestUserHelper.UserGenerateParam userParam =
                TestUserHelper.UserGenerateParam.builder().workdate(LocalDate.now(clock)).build();
        User user = testUserHelper.getOrCreateUser(userParam);

        Company company = user.getCompany();
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        SortingCenter sortingCenter = company.getSortingCenters().stream().findFirst().get();
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(TestDataFactory.DELIVERY_SERVICE_ID);

        Person person = new Person.PersonBuilder("person-name", "person-surname").build();
        Recipient recipient =
                new Recipient.RecipientBuilder(person, List.of(new Phone.PhoneBuilder("7915123456").build())).build();
        ResourceId senderId = new ResourceId.ResourceIdBuilder().setYandexId(ORDER_YANDEX_ID).build();
        Sender sender = new Sender.SenderBuilder(senderId, incorporation, ogrn).build();

        Item item = new Item.ItemBuilder(itemName, itemCount, itemPrice).setArticle("item-article").build();


        ru.yandex.market.logistic.api.model.delivery.Order sourceOrder =
                new ru.yandex.market.logistic.api.model.delivery.Order.OrderBuilder(
                        new ResourceId.ResourceIdBuilder().setYandexId(ORDER_YANDEX_ID).build(),
                        new Location.LocationBuilder(RUSSIA, MOSCOW_REGION, MOSCOW_CITY)
                                .setLat(ZERO_GEO_VALUE)
                                .setLng(ZERO_GEO_VALUE)
                                .build(),
                        new Location.LocationBuilder(RUSSIA, MOSCOW_REGION, MOSCOW_CITY).build(),
                        OrderGenerateService.DEFAULT_DIMENSIONS.getWeight(),
                        OrderGenerateService.DEFAULT_DIMENSIONS.getLength(),
                        OrderGenerateService.DEFAULT_DIMENSIONS.getWidth(),
                        OrderGenerateService.DEFAULT_DIMENSIONS.getHeight(),
                        cargoTotalCost,
                        cargoTotalCost,
                        PaymentMethod.PREPAID,
                        "default",
                        DeliveryType.COURIER,
                        deliveryCost,
                        totalCost,
                        List.of(item),
                        recipient,
                        sender)
                        .setDeliveryDate(deliveryDate)
                        .build();
        assertThat(validator.validate(sourceOrder)).isEmpty();

        CreateOrderResponse orderResp = dsOrderManager.createOrder(sourceOrder, deliveryService);
        Order order = orderRepository.findById(Long.parseLong(orderResp.getOrderId().getPartnerId())).orElseThrow();

        Shift shift = shiftManager.findOrCreate(LocalDate.now(clock), sortingCenter.getId());
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        UserShift userShift = userShiftRepository.getOne(userShiftId);
        testUserHelper.createDeliveryTaskFactory(user, userShift, order).create();

        Long defaultRegionId = TestUserHelper.SortCenterGenerateParam.DEFAULT_REGION_ID;
        List<TplRegion> regionList = List.of(
                TplRegion.builder()
                        .id(Math.toIntExact(defaultRegionId))
                        .name(MOSCOW_CITY)
                        .type(RegionType.CITY)
                        .build(),
                TplRegion.builder()
                        .id(Math.toIntExact(1))
                        .name(MOSCOW_REGION)
                        .type(RegionType.SUBJECT_FEDERATION)
                        .build(),
                TplRegion.builder()
                        .id(Math.toIntExact(2))
                        .name(RUSSIA)
                        .type(RegionType.COUNTRY)
                        .build());

        when(regionDao.getParentRegions(defaultRegionId)).thenReturn(regionList);
        when(regionDao.getParentRegions(deliveryService.getSortingCenter().getRegionId()))
                .thenReturn(regionList);

        doReturn(EXPECTED_PHONE_NUMBER).when(yandexGoOrderProperties).getWarehousePhoneNumber();

        // when
        ru.yandex.market.logistic.api.model.delivery.Order actualOrder =
                dsOrderMapper.toDsOrder(userShift.getDeliveryTaskForOrder(order.getId()));

        // then
        assertThat(validator.validate(actualOrder)).isEmpty();

        Sender expectedSender =
                new Sender.SenderBuilder(
                        new ResourceId.ResourceIdBuilder()
                                .setYandexId(ORDER_YANDEX_ID)
                                .setDeliveryId(order.getSender().getId().toString())
                                .setPartnerId(order.getSender().getId().toString())
                                .build(),
                        incorporation,
                        ogrn
                ).build();

        Location scLocation = new Location.LocationBuilder(RUSSIA, MOSCOW_REGION, MOSCOW_CITY)
                .setLat(deliveryService.getSortingCenter().getLatitude())
                .setLng(deliveryService.getSortingCenter().getLongitude())
                .setStreet(sortingCenter.getAddress())
                .build();

        Warehouse warehouse =
                new Warehouse.WarehouseBuilder(
                        new ResourceId.ResourceIdBuilder().setYandexId("47819").build(),
                        scLocation,
                        Stream.of(1, 2, 3, 4, 5, 6, 7)
                                .map(day -> new WorkTime(day, List.of(new TimeInterval("09:00+03:00/22:00+03:00"))))
                                .collect(Collectors.toList()))
                        .setPhones(List.of(new Phone(EXPECTED_PHONE_NUMBER, null)))
                        .build();
        TimeInterval deliveryInterval =
                dsOrderMapper.getDeliveryInterval(
                        shift,
                        deliveryService.getId(),
                        order.getPickupPoint(),
                        order.getDelivery().getInterval());

        DateTime expectedDeliveryDate = DateTime.fromOffsetDateTime(OffsetDateTime.parse("2021-12-31T00:00:00+03"));
        ru.yandex.market.logistic.api.model.delivery.Order expectedOrder =
                new ru.yandex.market.logistic.api.model.delivery.Order.OrderBuilder(
                        new ResourceId.ResourceIdBuilder()
                                .setYandexId(order.getExternalOrderId())
                                .setPartnerId(order.getId().toString())
                                .build(),
                        scLocation,
                        new Location.LocationBuilder(
                                sourceOrder.getLocationTo().getCountry(),
                                RUSSIA,
                                sourceOrder.getLocationTo().getLocality())
                                .setLng(ZERO_GEO_VALUE)
                                .setLat(ZERO_GEO_VALUE)
                                .setLocationId(-1L)
                                .build(),
                        sourceOrder.getWeight(),
                        sourceOrder.getLength(),
                        sourceOrder.getWidth(),
                        sourceOrder.getHeight(),
                        sourceOrder.getCargoCost(),
                        sourceOrder.getAssessedCost(),
                        sourceOrder.getPaymentMethod(),
                        sourceOrder.getTariff(),
                        sourceOrder.getDeliveryType(),
                        deliveryCost,
                        totalCost,
                        sourceOrder.getItems(),
                        recipient,
                        expectedSender
                )
                        .setWarehouseFrom(warehouse)
                        .setWarehouse(warehouse)
                        .setDeliveryDate(expectedDeliveryDate)
                        .setDeliveryInterval(deliveryInterval)
                        .setAmountPrepaid(order.isPrepaid() ? totalCost : null)
                        .build();

        assertThat(actualOrder).isEqualTo(expectedOrder);
    }

}
