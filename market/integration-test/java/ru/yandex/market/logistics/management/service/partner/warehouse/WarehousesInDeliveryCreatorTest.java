package ru.yandex.market.logistics.management.service.partner.warehouse;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayValidationException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.converter.lgw.LogisticsPointConverter;
import ru.yandex.market.logistics.management.domain.entity.PutReferenceWarehouseInDeliveryStatus;
import ru.yandex.market.logistics.management.domain.entity.type.WarehouseInDeliveryCreationStatus;
import ru.yandex.market.logistics.management.exception.EntityNotFoundException;
import ru.yandex.market.logistics.management.repository.PutReferenceWarehouseInDeliveryStatusRepository;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@CleanDatabase
class WarehousesInDeliveryCreatorTest extends AbstractContextualTest {

    private static final Map<String, String> PHONES = Map.of(
        "1", "999-999-99-91",
        "2", "999-999-99-92",
        "3", "999-999-99-93",
        "4", "999-999-99-94"
    );

    private static final Set<String> PARTNERS_WITHOUT_CONTACT = Set.of("4", "6");

    @Autowired
    private PutReferenceWarehouseInDeliveryStatusRepository repository;

    @Autowired
    private WarehousesInDeliveryCreator warehousesInDeliveryCreator;

    @Autowired
    private LogisticsPointConverter logisticsPointConverter;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private RegionService regionService;

    @BeforeEach
    void setup() {
        Region country = new Region(
            225,
            "Россия",
            RegionType.COUNTRY,
            null
        );
        Region region = new Region(
            1,
            "Москва и Московская область",
            RegionType.COUNTRY_DISTRICT,
            country
        );
        Region city = new Region(
            213,
            "Москва",
            RegionType.CITY,
            region
        );
        RegionTree<Region> regionTree = new RegionTree<>(country);

        Mockito.when(regionService.getRegionTree()).thenReturn(regionTree);
    }

    @Test
    @Sql("/data/service/point/data_for_extraction_test.sql")
    void createWarehouses() throws Exception {
        warehousesInDeliveryCreator.createWarehouses();

        Mockito.verify(deliveryClient, times(6)).putReferenceWarehouses(
            eq(buildPartner(3L)),
            anyList(),
            eq(false)
        );
        Mockito.verify(deliveryClient, times(5)).putReferenceWarehouses(
            eq(buildPartner(4L)),
            anyList(),
            eq(false)
        );

        softly.assertThat(repository.findAllByStatus(WarehouseInDeliveryCreationStatus.NEW))
            .as("Status is invalid")
            .isEmpty();
    }

    @Test
    @Sql("/data/service/point/data_for_extraction_test.sql")
    void createWarehouseWithoutOptionalFields() throws Exception {

        warehousesInDeliveryCreator.createWarehouses();

        verify(deliveryClient, Mockito.times(11))
            .putReferenceWarehouses(Mockito.any(), Mockito.any(), Mockito.anyBoolean());

        verify(deliveryClient, Mockito.times(1)).putReferenceWarehouses(
            buildPartner(3L),
            buildWarehouse("6"),
            false
        );

        WarehouseInDeliveryCreationStatus actualStatus = repository.findOneByPartnerIdAndWarehouseId(3L, 6L)
            .map(PutReferenceWarehouseInDeliveryStatus::getStatus)
            .orElseThrow(EntityNotFoundException::new);

        softly.assertThat(actualStatus)
            .as("Status is invalid")
            .isEqualTo(WarehouseInDeliveryCreationStatus.CREATING);
    }

    @Test
    @Sql("/data/service/point/data_for_extraction_test.sql")
    void createWarehousesWhenDeliveryClientThrowsGatewayApiException() throws Exception {
        String errorMessage = "Teeest";
        Mockito.doThrow(new GatewayApiException(errorMessage))
            .when(deliveryClient)
            .putReferenceWarehouses(
                buildPartner(3L),
                buildWarehouse("3"),
                false
            );

        warehousesInDeliveryCreator.createWarehouses();

        List<PutReferenceWarehouseInDeliveryStatus> errorRecords =
            repository.findAllByStatus(WarehouseInDeliveryCreationStatus.COULD_NOT_BE_PUSHED);

        PutReferenceWarehouseInDeliveryStatus expectedRecord = new PutReferenceWarehouseInDeliveryStatus();
        expectedRecord.setId(3L);
        expectedRecord.setPartnerId(3L);
        expectedRecord.setWarehouseId(3L);
        expectedRecord.setStatus(WarehouseInDeliveryCreationStatus.COULD_NOT_BE_PUSHED);
        expectedRecord.setComment(errorMessage);

        softly
            .assertThat(errorRecords)
            .as("DeliveryClient exception was not processed correctly")
            .isEqualTo(ImmutableList.of(expectedRecord));
    }

    @Test
    void createWarehousesWhenNoRecords() {
        warehousesInDeliveryCreator.createWarehouses();

        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @Sql("/data/service/point/data_for_extraction_test.sql")
    void createWarehousesConcurrently() throws Exception {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            threads.add(new Thread(() -> warehousesInDeliveryCreator.createWarehouses()));
        }

        threads.forEach(Thread::start);

        for (Thread thread : threads) {
            thread.join(2000);
        }

        List<WarehouseInDeliveryCreationStatus> expectedStatuses = repository.findAll().stream()
            .sorted(Comparator.comparing(PutReferenceWarehouseInDeliveryStatus::getId))
            .map(PutReferenceWarehouseInDeliveryStatus::getStatus)
            .collect(Collectors.toList());

        softly.assertThat(expectedStatuses)
            .as("Statuses are invalid")
            .containsExactly(
                WarehouseInDeliveryCreationStatus.CREATED,
                WarehouseInDeliveryCreationStatus.CREATED,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.ERROR,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.PREPARED,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING,
                WarehouseInDeliveryCreationStatus.CREATING
            );
    }

    @Test
    @Sql("/data/service/point/data_for_extraction_test.sql")
    void createWarehousesPartiallyWithErrors() throws Exception {
        String lmsError = "Some unexpected in LMS";
        String lgwError = "Some unexpected in LGW";
        doThrow(new NullPointerException(lmsError))
            .when(logisticsPointConverter)
            .convert(argThat(point -> point.getId().equals(3L)));
        doThrow(new GatewayValidationException(lgwError))
            .when(deliveryClient)
            .putReferenceWarehouses(eq(buildPartner(4L)), eq(buildWarehouse("2")), eq(false));

        warehousesInDeliveryCreator.createWarehouses();

        softly.assertThat(repository.findAll())
            .usingElementComparatorOnFields("partnerId", "warehouseId", "status", "comment")
            .contains(
                /* failed by error in converter */
                buildRecord(3L, 3L, WarehouseInDeliveryCreationStatus.COULD_NOT_BE_PUSHED, lmsError),
                buildRecord(3L, 4L, WarehouseInDeliveryCreationStatus.COULD_NOT_BE_PUSHED, lmsError),
                /* failed by error in LGW */
                buildRecord(2L, 4L, WarehouseInDeliveryCreationStatus.COULD_NOT_BE_PUSHED, lgwError),
                /* Other records processed correctly */
                buildRecord(4L, 3L, WarehouseInDeliveryCreationStatus.CREATING, null),
                buildRecord(5L, 3L, WarehouseInDeliveryCreationStatus.CREATING, null),
                buildRecord(6L, 3L, WarehouseInDeliveryCreationStatus.CREATING, null),
                buildRecord(7L, 3L, WarehouseInDeliveryCreationStatus.CREATING, null),
                buildRecord(9L, 3L, WarehouseInDeliveryCreationStatus.CREATING, null),
                buildRecord(1L, 4L, WarehouseInDeliveryCreationStatus.CREATING, null),
                buildRecord(5L, 4L, WarehouseInDeliveryCreationStatus.CREATING, null),
                buildRecord(6L, 4L, WarehouseInDeliveryCreationStatus.CREATING, null)
            );

    }

    private Partner buildPartner(Long partnerId) {
        return new Partner(partnerId);
    }

    private List<Warehouse> buildWarehouse(String yandexId) {
        return Collections.singletonList(
            new Warehouse(
                ResourceId.builder().setYandexId(yandexId).build(),
                getLocation(),
                null,
                getSchedule(),
                getContact(yandexId),
                getPhones(yandexId)
            )
        );
    }

    private PutReferenceWarehouseInDeliveryStatus buildRecord(
        Long warehouseId,
        Long partnerId,
        WarehouseInDeliveryCreationStatus status,
        String comment
    ) {
        var res = new PutReferenceWarehouseInDeliveryStatus();
        res.setWarehouseId(warehouseId);
        res.setPartnerId(partnerId);
        res.setStatus(status);
        res.setComment(comment);
        return res;
    }

    private Location getLocation() {
        return new Location.LocationBuilder("Россия", "", "Москва и Московская область")
            .setStreet("Уриекстес")
            .setHouse("14а")
            .setBuilding("")
            .setHousing("")
            .setRoom("")
            .setZipCode("1005")
            .setLat(new BigDecimal("56.948048"))
            .setLng(new BigDecimal("24.107018"))
            .setLocationId(213)
            .build();
    }

    private List<WorkTime> getSchedule() {
        return Arrays.asList(
            new WorkTime(
                1,
                Collections.singletonList(
                    TimeInterval.of(LocalTime.of(12, 0), LocalTime.of(14, 0))
                )
            ),
            new WorkTime(
                2,
                Arrays.asList(
                    TimeInterval.of(LocalTime.of(14, 0), LocalTime.of(16, 0)),
                    TimeInterval.of(LocalTime.of(13, 0), LocalTime.of(15, 0))
                )
            )
        );
    }

    private List<Phone> getPhones(String yandexId) {
        if (!PHONES.containsKey(yandexId)) {
            return Collections.emptyList();
        }
        return List.of(new Phone.PhoneBuilder(PHONES.get(yandexId)).build());
    }

    private Person getContact(String yandexId) {
        if (PARTNERS_WITHOUT_CONTACT.contains(yandexId)) {
            return null;
        }
        return new Person.PersonBuilder("contact", null).build();
    }
}
