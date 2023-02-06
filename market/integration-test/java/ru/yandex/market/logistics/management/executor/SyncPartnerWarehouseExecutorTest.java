package ru.yandex.market.logistics.management.executor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.exception.ExecutorSubTaskFailedException;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.service.point.sync.ImportPartnerWarehousesService;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.util.TestRegions.MOSCOW_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;

@DatabaseSetup(
    value = "/data/executor/before/sync_partner_warehouses.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
@SuppressWarnings({"checkstyle:MagicNumber"})
class SyncPartnerWarehouseExecutorTest extends AbstractContextualTest {

    private static final long PARTNER_ID_1 = 1;
    private static final long PARTNER_ID_2 = 2;
    private static final long PARTNER_ID_3 = 3;
    private static final long PARTNER_ID_4 = 4;
    private static final long PARTNER_ID_5 = 5;
    private static final String ERROR_MESSAGE = "internal error";

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private ImportPartnerWarehousesService importPartnerWarehousesService;

    @Autowired
    private HttpGeobase httpGeobase;

    @Autowired
    private RegionService regionService;

    private SyncPartnerWarehousesExecutor executor;

    @BeforeEach
    void init() {
        executor = new SyncPartnerWarehousesExecutor(
            Executors.newSingleThreadExecutor(), partnerRepository, deliveryClient, importPartnerWarehousesService);
        when(httpGeobase.getRegionId(anyDouble(), anyDouble()))
            .thenReturn(MOSCOW_ID);
        when(regionService.get())
            .thenReturn(buildRegionTree());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_warehouses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void syncWithException() {
        // По данному партнеру один склад будет обновлен, один будет деактивирован (не пришел от deliveryClient)
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_1))))
            .thenReturn(Collections.singletonList(warehouse1()));

        // По данному партнеру один склад будет обновлен с переопределением координат,
        // один будет обновлен без переопределения координат (придут в запросе),
        // один в заморозке и не изменится,
        // еще один не изменится (так как значение external_hash в базе совпадет с хэшом пришедшего склада),
        // один новая добавится
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_2))))
            .thenReturn(Arrays.asList(warehouse3(), warehouse4(), warehouse9(), warehouse10(), warehouse11()));

        // Данные партнеры в неподходящем статусе, по ним ничего не поменяется
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_3))))
            .thenReturn(Collections.singletonList(warehouse5()));
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_4))))
            .thenReturn(Collections.singletonList(warehouse6()));

        // По данному партнеру будет исключение
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_5))))
            .thenThrow(new RuntimeException(ERROR_MESSAGE));

        // Проверить исключение
        ExecutorSubTaskFailedException exception =
            Assertions.assertThrows(ExecutorSubTaskFailedException.class, () -> executor.doJob(null));

        softly.assertThat(exception.getMessage()).isEqualTo("Executor job failed with errors");
        Throwable[] suppressed = exception.getSuppressed();
        softly.assertThat(suppressed).isNotNull();
        softly.assertThat(suppressed.length).isEqualTo(1);
        softly.assertThat(suppressed[0].getMessage()).isEqualTo(ERROR_MESSAGE);
        verify(httpGeobase, times(4)).getRegionId(anyDouble(), anyDouble());
        checkBuildWarehouseSegmentTask(1L, 2L, 4L, 10L, 11L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_warehouses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void successfulSync() {
        // По данному партнеру один склад будет обновлен, один будет отключен (не пришел от deliveryClient)
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_1))))
            .thenReturn(Collections.singletonList(warehouse1()));

        // По данному партнеру один склад будет обновлен с переопределением координат,
        // один будет обновлен без переопределения координат (придут в запросе),
        // один в заморозке и не изменится,
        // еще один не изменится (так как значение external_hash в базе совпадет с хэшом пришедшего склада),
        // один новая добавится
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_2))))
            .thenReturn(Arrays.asList(warehouse3(), warehouse4(), warehouse9(), warehouse10(), warehouse11()));

        // Данные партнеры в неподходящем статусе, по ним ничего не поменяется
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_3))))
            .thenReturn(Collections.singletonList(warehouse5()));
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_4))))
            .thenReturn(Collections.singletonList(warehouse6()));

        // Нет данных по данному партнеру
        when(deliveryClient.getReferenceWarehouses(eq(lgwPartner(PARTNER_ID_5))))
            .thenReturn(Collections.emptyList());

        executor.doJob(null);
        verify(httpGeobase, times(4)).getRegionId(anyDouble(), anyDouble());
        checkBuildWarehouseSegmentTask(1L, 2L, 4L, 10L, 11L);
    }

    private Warehouse warehouse1() {
        return new Warehouse(
            ResourceId.builder()
                .setYandexId("1")
                .setPartnerId("8530-47e5-93c5-d44320e55dc1")
                .build(),
            location1(),
            "instruction 1",
            Arrays.asList(workTime1(), workTime2()),
            person1(),
            Arrays.asList(phone1(), phone2())
        );
    }

    private Warehouse warehouse3() {
        return new Warehouse(
            ResourceId.builder()
                .setYandexId("3")
                .setPartnerId("8530-47e5-93c5-d44320e55dc3")
                .build(),
            location1(),
            "instruction 3",
            Arrays.asList(workTime1(), workTime2()),
            person1(),
            Arrays.asList(phone1(), phone2())
        );
    }

    private Warehouse warehouse4() {
        return new Warehouse(
            ResourceId.builder()
                .setYandexId("4")
                .setPartnerId("8530-47e5-93c5-d44320e55dc4")
                .build(),
            location2(),
            "instruction 4",
            Collections.singletonList(workTime1()),
            person2(),
            Collections.singletonList(phone2())
        );
    }

    private Warehouse warehouse5() {
        return new Warehouse(
            ResourceId.builder()
                .setYandexId("5")
                .setPartnerId("8530-47e5-93c5-d44320e55dc5")
                .build(),
            location1(),
            "instruction 5",
            Arrays.asList(workTime1(), workTime2()),
            person1(),
            Arrays.asList(phone1(), phone2())
        );
    }

    private Warehouse warehouse6() {
        return new Warehouse(
            ResourceId.builder()
                .setYandexId("6")
                .setPartnerId("8530-47e5-93c5-d44320e55dc6")
                .build(),
            location1(),
            "instruction 6",
            Arrays.asList(workTime1(), workTime2()),
            person1(),
            Arrays.asList(phone1(), phone2())
        );
    }

    private Warehouse warehouse9() {
        return new Warehouse(
            ResourceId.builder()
                .setYandexId("9")
                .setPartnerId("8530-47e5-93c5-d44320e55dc9")
                .build(),
            location2(),
            "instruction9",
            Arrays.asList(workTime1(), workTime2()),
            person2(),
            Arrays.asList(phone1(), phone2())
        );
    }

    private Warehouse warehouse10() {
        return new Warehouse(
            ResourceId.builder()
                .setYandexId("10")
                .setPartnerId("8530-47e5-93c5-d44320e55dc10")
                .build(),
            location2(),
            "created_instruction",
            Arrays.asList(workTime1(), workTime2()),
            person2(),
            Arrays.asList(phone1(), phone2())
        );
    }

    private Warehouse warehouse11() {
        return new Warehouse(
            ResourceId.builder()
                .setYandexId("11")
                .setPartnerId("8530-47e5-93c5-d44320e55dc11")
                .build(),
            location3(),
            "instruction 11",
            Collections.singletonList(workTime1()),
            person2(),
            Collections.singletonList(phone2())
        );
    }

    private static Location location1() {
        return new Location.LocationBuilder("Россия", "Ульяновск", "Ульяновская область")
            .setStreet("Московское шоссе")
            .setHouse("11Г")
            .setHousing("3")
            .setBuilding("2")
            .setRoom("2")
            .setZipCode("654215")
            .setLat(new BigDecimal("120"))
            .setLng(new BigDecimal("220"))
            .setLocationId(null)
            .build();
    }

    private static Location location2() {
        return new Location.LocationBuilder("Россия", "Уфа", "Башкирия")
            .setStreet("Центральная")
            .setHouse("8")
            .setHousing("3")
            .setBuilding("2")
            .setRoom("1")
            .setZipCode("419829")
            .setLat(new BigDecimal("104"))
            .setLng(new BigDecimal("205"))
            .setLocationId(null)
            .build();
    }

    private static Location location3() {
        return new Location.LocationBuilder("Россия", "Белгород", "Белгородская область")
            .setStreet("Сталина")
            .setHouse("12")
            .setHousing("3")
            .setBuilding("2")
            .setRoom("1")
            .setZipCode("555666")
            .setLat(new BigDecimal("110"))
            .setLng(new BigDecimal("210"))
            .setLocationId(12346)
            .build();
    }

    private Person person1() {
        return new Person("Иван", "Иванов", "Иванович");
    }

    private Person person2() {
        return new Person("Петр", "Петров", "Петрович");
    }

    private static Phone phone1() {
        return new Phone.PhoneBuilder("555777")
            .setAdditional("333555")
            .build();
    }

    private static Phone phone2() {
        return new Phone.PhoneBuilder("666777")
            .build();
    }

    private static WorkTime workTime1() {
        return new WorkTime(1, Arrays.asList(
            TimeInterval.of(LocalTime.of(9, 0), LocalTime.of(12, 0)),
            TimeInterval.of(LocalTime.of(13, 0), LocalTime.of(19, 0))
        ));
    }

    private static WorkTime workTime2() {
        return new WorkTime(2, Arrays.asList(
            TimeInterval.of(LocalTime.of(10, 0), LocalTime.of(13, 0)),
            TimeInterval.of(LocalTime.of(14, 0), LocalTime.of(20, 0))
        ));
    }

    private static Partner lgwPartner(long id) {
        return new Partner(id);
    }

}
