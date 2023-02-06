package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPoint;
import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointOwner;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PupEvent;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.logic.def.HasCreationTime;
import ru.yandex.market.jmf.utils.Maps;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointPaymentMethod.METHOD_CASH;
import static ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointPaymentMethod.METHOD_PREPAID;
import static ru.yandex.market.jmf.catalog.items.CatalogItem.CODE;

@B2bPickupPointTests
@ExtendWith(SpringExtension.class)
public class PickupPointPupEventProcessorTest {
    private static final PupEvent<?> EVENT;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        EVENT = Exceptions.sneakyRethrow(() ->
                objectMapper.readValue(
                        PickupPointPupEventProcessorTest.class.getResource(
                                "pickupPointPupEvent.json"),
                        PupEvent.class
                )
        );
    }

    @Inject
    private PickupPointPupEventProcessor processor;

    @Inject
    private DbService dbService;

    @Inject
    private BcpService bcpService;

    private PickupPointOwner pickupPointOwner;

    @BeforeEach
    public void setUp() {
        pickupPointOwner = bcpService.create(PickupPointOwner.FQN, Maps.of(
                PickupPointOwner.TITLE, "Владелец ПВЗ",
                PickupPointOwner.PUP_ID, 141L
        ));
    }

    @Test
    public void shouldCreateNewPickupPoint() {
        PickupPoint diffPupId = createPickupPoint(2L, "Отличный pupId");
        processor.process(EVENT);
        assertPickupPointAttributes(getLastPickupPoint());
        EntityCollectionAssert.assertThat(dbService.<PickupPoint>list(Query.of(PickupPoint.FQN)))
                .hasSize(2)

                .withFailMessage("ПВЗ с отличным 'ID ПО ПВЗ' должны остаться без изменений")
                .anyHasAttributes(PickupPoint.TITLE, diffPupId.getTitle());
    }

    @Test
    public void shouldEditExistingAccount() {
        PickupPoint pickupPoint = createPickupPoint(1L, "Должен измениться");
        processor.process(EVENT);
        assertPickupPointAttributes(pickupPoint);
    }

    private void assertPickupPointAttributes(PickupPoint pickupPoint) {
        EntityAssert.assertThat(pickupPoint)
                .hasAttributes(
                        PickupPoint.PICKUP_POINT_ID, "1",
                        PickupPoint.PUP_MARKET_ID, "1",
                        PickupPoint.DELIVERY_SERVICE_ID, "1005489",
                        PickupPoint.LEGAL_PARTNER_ID, "141",
                        PickupPoint.LOCATION_ID, 213L,
                        PickupPoint.PICKUP_POINT_OWNER, pickupPointOwner.getGid(),
                        PickupPoint.STATUS, PickupPoint.STATUS_ACTIVE,
                        PickupPoint.TITLE, "ПВЗ на Академической",
                        PickupPoint.PHONE, Phone.fromRaw("+7(965)127-89-48"),
                        PickupPoint.PAYMENT_METHODS, hasSize(2),
                        PickupPoint.PAYMENT_METHODS, hasItem(hasProperty(CODE, equalTo(METHOD_CASH))),
                        PickupPoint.PAYMENT_METHODS, hasItem(hasProperty(CODE, equalTo(METHOD_PREPAID))),
                        PickupPoint.ADDRESS, "Российская Федерация, Москва и Московская область, Москва, " +
                                "Ленинградский проспект, 5, строение 7, корпус 1, подъезд 3, этаж 1, офис 1",
                        PickupPoint.ADDRESS_METRO, "Белорусская",
                        PickupPoint.ADDRESS_ZIP_CODE, "125040",
                        PickupPoint.ADDRESS_INTERCOM, "1",
                        PickupPoint.LATITUDE, "55.77914",
                        PickupPoint.LONGITUDE, "37.577921",
                        PickupPoint.FLOOR, "1",
                        PickupPoint.ADDRESS_INSTRUCTION, "Вход с торца здания",
                        PickupPoint.ORDER_MAX_DIMENSIONS, "20.00, 90.00x90.00x90.00, 120.00",
                        PickupPoint.SQUARE, BigDecimal.valueOf(75.2),
                        PickupPoint.CEILING_HEIGHT, BigDecimal.valueOf(3.2),
                        PickupPoint.PHOTO_URL, new Hyperlink("https://disk.yandex.ru/i/6KGvEhmW1u-efw"),
                        PickupPoint.COMMENT, "Электричества нет, потолок протекает",
                        PickupPoint.HAS_WINDOWS, false,
                        PickupPoint.HAS_SEPARATE_ENTRANCE, false,
                        PickupPoint.HAS_STREET_ENTRANCE, false,
                        PickupPoint.POLYGON_ID, "А12332234342",
                        PickupPoint.SCORING, BigDecimal.valueOf(12.345),
                        PickupPoint.CAPACITY, 30L,
                        PickupPoint.STORAGE_PERIOD, 30L,
                        PickupPoint.CASH_COMPENSATION, BigDecimal.valueOf(0.003),
                        PickupPoint.CARD_COMPENSATION, BigDecimal.valueOf(0.019),
                        PickupPoint.ORDER_TRANSMISSION_REWARD, BigDecimal.valueOf(45),
                        PickupPoint.BRANDING_STATUS, "fullyBranded",
                        PickupPoint.BRANDED_SINCE, LocalDate.parse("2021-02-13"),
                        PickupPoint.BRANDING_REGION, "saintPetersburg",
                        PickupPoint.SCHEDULE, """
                                понедельник 08:00-20:00
                                вторник Выходной""",
                        PickupPoint.AVAILABLE_ON_HOLIDAYS, false,
                        PickupPoint.SCHEDULE_OVERRIDES, """
                                21.02.2021 Выходной
                                22.02.2021 Рабочий день""",
                        PickupPoint.WAREHOUSE_AREA, BigDecimal.valueOf(55.1),
                        PickupPoint.CLIENT_AREA, BigDecimal.valueOf(20.1)
                );
    }


    private PickupPoint createPickupPoint(Long pupId, String title) {
        return bcpService.create(
                PickupPoint.FQN,
                Maps.of(PickupPoint.TITLE, title, PickupPoint.PICKUP_POINT_ID, pupId)
        );
    }

    private PickupPoint getLastPickupPoint() {
        Query query = Query.of(PickupPoint.FQN)
                .withSortingOrder(SortingOrder.desc(HasCreationTime.CREATION_TIME));
        return dbService.<PickupPoint>list(query).get(0);
    }
}
