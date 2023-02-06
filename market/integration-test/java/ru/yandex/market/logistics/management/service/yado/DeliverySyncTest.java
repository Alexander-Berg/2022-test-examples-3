package ru.yandex.market.logistics.management.service.yado;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.CalendarDayDto;
import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.domain.entity.CalendarDay;
import ru.yandex.market.logistics.management.domain.entity.CargoType;
import ru.yandex.market.logistics.management.domain.entity.DeliveryInterval;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacity;
import ru.yandex.market.logistics.management.domain.entity.PartnerCourierSchedule;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamType;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamValue;
import ru.yandex.market.logistics.management.domain.entity.PartnerRoute;
import ru.yandex.market.logistics.management.domain.entity.PartnerShop;
import ru.yandex.market.logistics.management.domain.entity.PartnerTariff;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.repository.LogisticsPointRepository;
import ru.yandex.market.logistics.management.repository.PartnerExternalParamTypeRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.repository.yado.YadoDao;
import ru.yandex.market.logistics.management.service.calendar.CalendarService;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TransactionalUtils;

import static org.mockito.ArgumentMatchers.anyMap;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.DEFAULT_DROP_SHIP_DELIVERY;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.DEFAULT_DROP_SHIP_SORTING_CENTER;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.DEFAULT_SUPPLIER_FULFILLMENT;

@CleanDatabase
@Sql("/data/repository/partner_external_param_types.sql")
@SuppressWarnings({"unchecked", "checkstyle:MagicNumber"})
@Disabled("Ignore until DELIVERY-20177")
class DeliverySyncTest extends AbstractContextualTest {

    @Autowired
    private YadoService yadoService;

    @Autowired
    private TransactionalUtils transactionalUtils;

    @Autowired
    private YadoDao dao;

    @Autowired
    private PartnerExternalParamTypeRepository paramTypeRepository;

    @Autowired
    private PartnerRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LogisticsPointRepository logisticsPointRepository;

    @Autowired
    private CalendarService calendarService;

    private List<PartnerExternalParamType> paramTypes;
    private List<Partner> deliveries;
    private Partner serviceToChange;
    private Set<PartnerExternalParamValue> paramValuesToChange;
    private PartnerExternalParamValue paramValueToChange;
    private PartnerExternalParamValue paramValueToDelete;
    private Set<PartnerRoute> partnerRoutes;
    private DeliveryInterval intervalToChange;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        initDeliveryServices();
        Mockito.when(dao.findDeliveryServices(anyMap())).thenReturn(deliveries);
        yadoService.syncDatabases();
    }

    @Test
    void testUpdateService() {
        serviceToChange
            .setBillingClientId(100500L)
            .setRating(1212)
            .setReadableName("NewReadableName")
            .setStatus(PartnerStatus.INACTIVE)
            .setDomain("www.fulfillment-service-1-updated.com");
        yadoService.syncDatabases();
        Partner partner = transactionalUtils.loadFullPartner(1L);
        softly.assertThat(partner).as("Partner should be updated")
            .extracting(Partner::getBillingClientId, Partner::getReadableName, Partner::getRating, Partner::getDomain)
            .containsExactly(100500L, "NewReadableName", 1212, "www.fulfillment-service-1-updated.com");
    }

    @Test
    void testPartnerExternalParamValuesUpdated() {
        paramValueToChange.setValue("test-value-for-type-0-updated");
        paramValuesToChange.remove(paramValueToDelete);
        paramValuesToChange.add(
            new PartnerExternalParamValue()
                .setParamType(paramTypes.get(2))
                .setValue("test-value-for-type-2-created")
                .setPartner(serviceToChange)
        );

        yadoService.syncDatabases();
        Partner partner = transactionalUtils.loadFullPartner(1L);
        Set<PartnerExternalParamValue> externalParamValues = partner.getExternalParamValues();

        softly.assertThat(externalParamValues.size())
            .as("External param values count should increase")
            .isEqualTo(2);

        softly.assertThat(externalParamValues)
            .as("Param values should update")
            .isEqualTo(paramValuesToChange);

        softly.assertThat(partner)
            .as("Other partner fields should not change")
            .extracting(
                Partner::getPartnerRoutes,
                Partner::getBillingClientId,
                Partner::getCalendarId
            )
            .containsExactly(partnerRoutes, 123L, null);
    }

    @Test
    void testRemovedShopStillExist() {
        jdbcTemplate.execute(
            "INSERT INTO partner_shop (partner_id, shop_id, is_default) " +
                "VALUES (1, 123, TRUE), (1, 456, FALSE)");
        serviceToChange.getShops().clear();
        serviceToChange.setRating(12);
        yadoService.syncDatabases();

        List<Partner> saved = transactionalUtils.loadFullPartners();
        softly.assertThat(saved).flatExtracting(Partner::getShops).as("Shops are not deleted")
            .extracting(
                ps -> ps.getPartner().getId(),
                PartnerShop::getShopId,
                PartnerShop::getDefault)
            .containsExactlyInAnyOrder(
                new Tuple(1L, 123, true),
                new Tuple(1L, 456, false));
    }

    @Test
    void testRemovedCalendarsStillExist() {
        Calendar calendar = createCalendar();
        transactionalUtils.saveInTransaction(calendar);
        transactionalUtils.saveInTransaction(
                repository.findById(serviceToChange.getId()).orElseThrow().setCalendarId(calendar.getId()));
        serviceToChange.setCalendarId(null);
        serviceToChange.setRating(12);
        yadoService.syncDatabases();

        Partner returned = transactionalUtils.loadFullPartner(1L);
        List<CalendarDayDto> days = Optional.ofNullable(returned.getCalendarId())
            .map(calendarService::getAllDaysByCalendarId)
            .stream()
            .flatMap(Set::stream)
            .collect(Collectors.toList());
        softly.assertThat(days).extracting(
            CalendarDayDto::getDay
        ).containsExactlyInAnyOrder(
            LocalDate.of(2008, 12, 31),
            LocalDate.of(2008, 10, 15)
        );
    }

    @Test
    void testRemovedCargoTypesStillExist() {
        transactionTemplate.execute(c -> {
            Partner p = repository.findById(serviceToChange.getId()).orElseThrow();
            addForbiddenCargoTypesToPartner(p, createCargoTypes());
            return null;
        });
        serviceToChange.getForbiddenCargoTypes().clear();
        serviceToChange.setRating(12);
        yadoService.syncDatabases();

        Partner returned = transactionalUtils.loadFullPartner(1L);
        Set<CargoType> returnedCargoTypes = returned.getForbiddenCargoTypes();
        softly.assertThat(returnedCargoTypes).isNotNull();
        softly.assertThat(returnedCargoTypes).hasSize(1);
        Optional<CargoType> theOneInside = returnedCargoTypes.stream().findAny();
        softly.assertThat(theOneInside).isPresent();
        CargoType returnedCargoType = theOneInside.get();
        softly.assertThat(returnedCargoType.getCargoType()).isEqualTo(1337);
        softly.assertThat(returnedCargoType.getDescription()).isEqualTo("CargoTypeLeet");
    }

    @Test
    void testCourierScheduleStillExist() {
        transactionTemplate.execute(c -> {
            Partner p = repository.findById(serviceToChange.getId()).orElseThrow();
            p.addCourierSchedule(createCourierSchedule());
            return null;
        });
        serviceToChange.getCourierSchedule().clear();
        serviceToChange.setRating(12);
        yadoService.syncDatabases();

        Partner returned = transactionalUtils.loadFullPartner(1L);
        Set<PartnerCourierSchedule> courierSchedules = returned.getCourierSchedule();
        softly.assertThat(courierSchedules).isNotNull();
        softly.assertThat(courierSchedules).hasSize(1);
        Optional<PartnerCourierSchedule> oneSchedule = courierSchedules.stream().findAny();
        softly.assertThat(oneSchedule).isPresent();
        softly.assertThat(oneSchedule.map(PartnerCourierSchedule::getLocationId).orElse(null)).isEqualTo(213L);
        softly.assertThat(oneSchedule.map(PartnerCourierSchedule::getPartner).map(Partner::getId).orElse(null))
            .isEqualTo(serviceToChange.getId());
        Optional<Set<ScheduleDay>> scheduleDays = oneSchedule
            .map(PartnerCourierSchedule::getSchedule)
            .map(Schedule::getScheduleDays);
        softly.assertThat(scheduleDays.orElseGet(ImmutableSet::of)).hasSize(2);
    }

    @Test
    void testIntakeScheduleStillExist() {
        transactionalUtils.saveInTransaction(
                repository.findById(serviceToChange.getId()).orElseThrow().setIntakeSchedule(createSchedule()));
        serviceToChange.setIntakeSchedule(null);
        serviceToChange.setRating(12);
        yadoService.syncDatabases();

        Partner returned = transactionalUtils.loadFullPartner(1L);
        Schedule intakeSchedule = returned.getIntakeSchedule();
        softly.assertThat(intakeSchedule).isNotNull();

        Set<ScheduleDay> scheduleDays = intakeSchedule.getScheduleDays();
        softly.assertThat(scheduleDays).hasSize(2);
    }

    @Test
    void testStatusNotChanged() {
        transactionalUtils.saveInTransaction(
                repository.findById(serviceToChange.getId()).orElseThrow().setStatus(PartnerStatus.FROZEN));
        serviceToChange.setStatus(PartnerStatus.TESTING);
        serviceToChange.setRating(12);
        yadoService.syncDatabases();

        Partner returned = transactionalUtils.loadFullPartner(1L);
        PartnerStatus status = returned.getStatus();
        softly.assertThat(status).isNotNull();

        softly.assertThat(status).isEqualByComparingTo(PartnerStatus.FROZEN);
    }

    @Test
    void testLogisticPointsInherited() {
        jdbcTemplate.execute("INSERT INTO address (id, location_id) VALUES (1, 255)");
        jdbcTemplate.execute("INSERT INTO logistics_point (market_id, external_id, type, address_id, active) " +
            "VALUES (456, '100500', 'WAREHOUSE', 1, TRUE)");

        yadoService.syncDatabases();
        LogisticsPoint logisticsPoint = logisticsPointRepository.findAll().get(0);
        softly.assertThat(logisticsPoint.getExternalId()).as("Logistics point remains ")
            .isEqualTo("100500");
    }

    @Test
    void testInsert() {
        List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
        performAssertions(saved);
    }

    @Test
    void testUpdateDeliveryService() {
        attachCalendars();
        serviceToChange.setBillingClientId(100500L)
            .setStatus(PartnerStatus.INACTIVE)
            .setRating(123);
        yadoService.syncDatabases();
        List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
        performAssertions(saved, 2, 6, 6, 84, 6, 6, 1);
    }

    @Test
    void testUpdateDeliveryServiceWithNameChange() {
        attachCalendars();
        serviceToChange.setName("123");
        yadoService.syncDatabases();
        List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
        performAssertions(saved, 2, 6, 6, 84, 6, 6, 1);
    }

    @Test
    void testUpdateDeliveryServiceDomain() {
        serviceToChange.setDomain("www.test-domain-1-changed.com");
        yadoService.syncDatabases();
        List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
        performAssertions(saved);
    }

    @Test
    void testRemoveDeliveryInterval() {
        attachCalendars();
        boolean removed = serviceToChange.getDeliveryIntervals().remove(intervalToChange);
        yadoService.syncDatabases();
        List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
        softly.assertThat(removed).as("Interval removed from collection")
            .isTrue();
        performAssertions(saved, 2, 5, 5, 70, 5, 5, 1);
    }

    @Test
    void testDbRecordsNotIncreasedAfterSyncingSynced() {
        yadoService.syncDatabases();
        yadoService.syncDatabases();
        yadoService.syncDatabases();
        List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
        performAssertions(saved);
    }

    @Test
    void testRemovedPartnerRoutesStillExist() {
        jdbcTemplate.execute(
            "INSERT INTO partner_route (partner_id, location_from, location_to) " +
                "VALUES (1, 10, 20)");
        serviceToChange.setRating(12);
        yadoService.syncDatabases();
        transactionTemplate.execute(c -> {
            List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
            softly.assertThat(saved).flatExtracting(Partner::getPartnerRoutes).as("Partner Routes are not deleted")
                .extracting(
                    pr -> pr.getPartner().getId(),
                    PartnerRoute::getLocationFrom,
                    PartnerRoute::getLocationTo)
                .containsExactlyInAnyOrder(
                    new Tuple(1L, 10, 20),
                    new Tuple(2L, 1, 2));
            return null;
        });
    }

    @Test
    void testRemovedTariffsStillExist() {
        jdbcTemplate.execute(
            "INSERT INTO partner_tariff (id, partner_id, tariff_id) " +
                "VALUES (1, 1, 4)");

        jdbcTemplate.execute(
            "INSERT INTO cargo_type (id, cargo_type, description) " +
                "VALUES (1, 5, 'new cargo type')");

        jdbcTemplate.execute(
            "INSERT INTO partner_tariff_cargo_type (partner_tariff_id, cargo_type_id) " +
                "VALUES (1, 1)");

        serviceToChange.setRating(12);
        yadoService.syncDatabases();
        transactionTemplate.execute(c -> {
            Partner partner = repository.findById(1L).orElse(null);
            softly.assertThat(partner).as("Partner should not be null").isNotNull();
            Set<PartnerTariff> tariffs = partner.getPartnerTariffs();
            softly.assertThat(tariffs).as("Tariffs should remain").hasSize(1);
            Set<CargoType> cargoTypes = tariffs.stream().map(PartnerTariff::getCargoTypes).flatMap(Set::stream)
                .collect(Collectors.toSet());
            softly.assertThat(cargoTypes).as("Cargo types should remain").hasSize(1);
            return null;
        });
    }

    @Test
    void testUpdateInterval() {
        attachCalendars();
        intervalToChange.setLocationId(-1);
        yadoService.syncDatabases();
        List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
        performAssertions(saved, 2, 6, 6, 84, 5, 5, 1);
    }

    @Test
    void testRemovedLogoUrlStillExists() {
        jdbcTemplate.execute(
            "UPDATE partner p SET logo_url = concat('http://test_logo_url/', p.id);"
        );
        softly.assertThat(serviceToChange.getLogoUrl())
            .as("ya.do cannot provide logoUrl of partners")
            .isNull();
        yadoService.syncDatabases();
        repository.findAllByPartnerType(PartnerType.DELIVERY)
            .forEach(d -> softly.assertThat(
                d.getLogoUrl()).isEqualTo("http://test_logo_url/" + d.getId())
            );
    }

    @Test
    void testTheSamePartnerWillNotBeUpdated() {
        jdbcTemplate.execute("UPDATE partner SET status = 'inactive' WHERE id = 1");
        yadoService.syncDatabases();
        Partner partner = repository.findByIdOrThrow(1L);
        softly.assertThat(partner.getStatus()).as("Status should not be changed after sync")
            .isEqualTo(PartnerStatus.INACTIVE);
    }

    @Test
    void marketIdIsNotChanged() {
        jdbcTemplate.execute("UPDATE partner SET market_id = 100500 WHERE id = 1");
        yadoService.syncDatabases();
        List<Partner> partners = repository.findAll();
        softly.assertThat(partners).hasSize(2);
        Partner changedPartner = repository.findByIdOrThrow(1L);
        softly.assertThat(changedPartner.getMarketId()).isEqualTo(100500L);
    }

    @Test
    void testExternalParamsNotDeletedWithUpdateTypes() {
        insertExternalParamsWithUpdateTypes();
        serviceToChange.setRating(12);
        yadoService.syncDatabases();
        List<Partner> saved = repository.findAllByPartnerType(PartnerType.DELIVERY);
        performAssertions(saved);
        transactionTemplate.execute(c -> {
            Partner partner = repository.findById(1L).orElse(null);
            softly.assertThat(partner).as("Partner should not be null").isNotNull();
            Set<PartnerExternalParamValue> externalParamValues = partner.getExternalParamValues();

            softly.assertThat(externalParamValues)
                .filteredOn(paramValue -> DEFAULT_DROP_SHIP_DELIVERY.name().equals(paramValue.getParamType().getKey()))
                .as("DEFAULT_DROP_SHIP_DELIVERY external param should remain")
                .hasSize(1);

            softly.assertThat(externalParamValues)
                .filteredOn(
                    paramValue -> DEFAULT_DROP_SHIP_SORTING_CENTER.name().equals(paramValue.getParamType().getKey()))
                .as("DEFAULT_DROP_SHIP_SORTING_CENTER external param should remain")
                .hasSize(1);

            softly.assertThat(externalParamValues)
                .filteredOn(
                    paramValue -> DEFAULT_SUPPLIER_FULFILLMENT.name().equals(paramValue.getParamType().getKey())
                )
                .as("DEFAULT_SUPPLIER_WAREHOUSE external param should remain")
                .hasSize(1);

            return null;
        });
    }

    @Test
    void platformClientsNotDeletedDuringSync() {
        jdbcTemplate.execute("INSERT INTO platform_client_partners (partner_id, platform_client_id, status)" +
            " VALUES (1,1,'ACTIVE')");

        serviceToChange.setName("New name");
        yadoService.syncDatabases();

        transactionTemplate.execute(c -> {
            Partner partner = repository.findByIdOrThrow(1L);
            softly.assertThat(partner.getPlatformClients())
                .as("Platform client should remain unchanged")
                .hasSize(1);
            return null;
        });
    }

    private void performAssertions(List<Partner> saved) {
        performAssertions(saved, 2, 6, 6, 84, 1);
    }

    private void performAssertions(List<Partner> saved,
                                   int partnerCount,
                                   int deliveryIntervalCount,
                                   int scheduleCount,
                                   int scheduleDaysCount,
                                   int capacityCount) {
        performAssertions(
            saved,
            partnerCount,
            deliveryIntervalCount,
            scheduleCount,
            scheduleDaysCount,
            0,
            0,
            capacityCount
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void performAssertions(List<Partner> saved,
                                   int partnerCount,
                                   int deliveryIntervalCount,
                                   int scheduleCount,
                                   int scheduleDaysCount,
                                   int calendarCount,
                                   int calendarDaysCount,
                                   int capacityCount) {
        int dsRowCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "partner");
        int diRowCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery_interval");
        int scheduleRowCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "schedule");
        int sdRowCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "schedule_day");
        int calendarRowCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "calendar");
        int cdRowCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "calendar_day");
        int pcRowCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "partner_capacity");

        List<ScheduleDay> days = selectAllScheduleDays(jdbcTemplate);
        softly.assertThat(saved)
            .as("Services should be equal")
            .hasSize(partnerCount).hasSameElementsAs(deliveries);
        deliveries.forEach(partner -> {
            softly.assertThat(selectAllParamValuesOfPartner(partner))
                .as(String.format("Param values of partner with id = %d should be equal", partner.getId()))
                .usingElementComparatorIgnoringFields("id", "partner")
                .containsAll(partner.getExternalParamValues());
            softly.assertThat(selectDomainOfPartner(partner)
            ).isEqualTo(partner.getDomain());
        });
        softly.assertThat(days)
            .as("ScheduleDays should be equal")
            .hasSameElementsAs(extractDays(deliveries));
        softly.assertThat(dsRowCount).as("Delivery service count should not increase")
            .isEqualTo(partnerCount);
        softly.assertThat(diRowCount).as("Delivery interval count should not increase")
            .isEqualTo(deliveryIntervalCount);
        softly.assertThat(scheduleRowCount).as("Schedule count should not increase")
            .isEqualTo(scheduleCount);
        softly.assertThat(sdRowCount).as("Schedule days count should not increase")
            .isEqualTo(scheduleDaysCount);
        softly.assertThat(calendarRowCount).as("Calendar count should not be changed")
            .isEqualTo(calendarCount);
        softly.assertThat(cdRowCount).as("Calendar days count should not be changed")
            .isEqualTo(calendarDaysCount);
        softly.assertThat(pcRowCount).as("Capacity count should not be changed")
            .isEqualTo(capacityCount);
    }

    private String selectDomainOfPartner(Partner partner) {
        return jdbcTemplate.queryForObject(
            "SELECT domain FROM partner WHERE id = ?",
            new Object[] {partner.getId()},
            String.class);
    }

    private PartnerCourierSchedule createCourierSchedule() {
        return new PartnerCourierSchedule()
            .setLocationId(213)
            .setSchedule(createSchedule());
    }

    private static Schedule createSchedule() {
        return new Schedule()
            .addScheduledDay(
                new ScheduleDay().setDay(1)
                    .setFrom(LocalTime.of(1, 1))
                    .setTo(LocalTime.of(2, 2))
            ).addScheduledDay(
                new ScheduleDay()
                    .setDay(2).setFrom(LocalTime.of(2, 1))
                    .setTo(LocalTime.of(3, 2))
            );
    }

    private List<PartnerExternalParamValue> selectAllParamValuesOfPartner(Partner partner) {
        return jdbcTemplate.query("" +
                "SELECT * " +
                "FROM partner_external_param_value " +
                "JOIN partner_external_param_type " +
                "ON partner_external_param_value.type_id = partner_external_param_type.id " +
                "WHERE partner_id = ?",
            new Object[] {partner.getId()},
            (rs, rowNum) -> new PartnerExternalParamValue(
                new PartnerExternalParamType()
                    .setId(rs.getLong("id"))
                    .setKey(rs.getString("key"))
                    .setDescription(rs.getString("description")),
                rs.getString("value")
            )
        );
    }

    private static List<ScheduleDay> selectAllScheduleDays(JdbcTemplate template) {
        return template.query("SELECT day.day, day.time_from, day.time_to " +
                "FROM schedule_day day",
            (rs, rowNum) -> new ScheduleDay()
                .setTo(rs.getTime("time_to").toLocalTime())
                .setFrom(rs.getTime("time_from").toLocalTime())
                .setDay(rs.getObject("day", Integer.class)));
    }

    private static List<ScheduleDay> extractDays(List<Partner> services) {
        return services.stream()
            .map(Partner::getDeliveryIntervals)
            .flatMap(Set::stream)
            .map(DeliveryInterval::getSchedule)
            .map(Schedule::getScheduleDays)
            .flatMap(Set::stream)
            .collect(Collectors.toList());
    }

    private PartnerCapacity createCapacity() {
        PartnerCapacity partnerCapacity = new PartnerCapacity();
        partnerCapacity.setType(CapacityType.REGULAR);
        partnerCapacity.setDay(LocalDate.of(2018, 1, 1));
        partnerCapacity.setLocationFrom(1);
        partnerCapacity.setLocationTo(2);
        partnerCapacity.setValue(1000L);
        partnerCapacity.setCountingType(CountingType.ORDER);
        partnerCapacity.setServiceType(CapacityService.DELIVERY);
        return partnerCapacity;
    }

    private List<DeliveryInterval> createIntervals(int random) {
        DeliveryInterval int1 = new DeliveryInterval()
            .setSchedule(new Schedule().setScheduleDays(generateScheduleDays()))
            .setLocationId(110 + random);
        DeliveryInterval int2 = new DeliveryInterval()
            .setSchedule(new Schedule().setScheduleDays(generateScheduleDays()))
            .setLocationId(120 + random);
        DeliveryInterval int3 = new DeliveryInterval()
            .setSchedule(new Schedule().setScheduleDays(generateScheduleDays()))
            .setLocationId(130 + random);
        return new ArrayList<>(Arrays.asList(int1, int2, int3));
    }

    private static Set<ScheduleDay> generateScheduleDays() {
        Set<ScheduleDay> result = new HashSet<>();
        for (int i = 1; i < 15; i++) {
            ScheduleDay day = new ScheduleDay()
                .setDay(i % 7)
                .setFrom(LocalTime.of(i, i))
                .setTo(LocalTime.of(1 + i, 1 + i));
            result.add(day);
        }
        return result;
    }

    private void attachCalendars() {
        TestUtil.executeSqlScript("/data/service/yado/delivery_interval_calendars.sql", jdbcTemplate.getDataSource());
    }

    private void insertExternalParamsWithUpdateTypes() {
        TestUtil.executeSqlScript("/data/service/yado/partner_external_params_update_types.sql",
            jdbcTemplate.getDataSource());
    }

    private void initDeliveryServices() {
        paramTypes = paramTypeRepository.findAll();

        serviceToChange = new Partner()
            .setId(1L)
            .setMarketId(1L)
            .setPartnerType(PartnerType.DELIVERY)
            .setName("Delivery service 1")
            .setReadableName("ReadableName")
            .setRating(1)
            .setBillingClientId(123L)
            .setTrackingType("status1")
            .setDomain("www.test-domain-1.com")
            .addExternalParams(createExternalParams())
            .addDeliveryIntervals(createIntervals(1000))
            .addCapacity(createCapacity())
            .setStatus(PartnerStatus.ACTIVE);

        paramValuesToChange = serviceToChange.getExternalParamValues();
        partnerRoutes = serviceToChange.getPartnerRoutes();

        intervalToChange = serviceToChange.getDeliveryIntervals().stream().findFirst().orElse(null);

        Partner delivery2 = new Partner()
            .setId(2L)
            .setPartnerType(PartnerType.DELIVERY)
            .setName("Delivery service 2")
            .setRating(2)
            .setBillingClientId(12L)
            .setTrackingType("status2")
            .setDomain("www.test-domain-2.com")
            .addDeliveryIntervals(createIntervals(2000))
            .addPartnerRoute(createPartnerRoute())
            .setStatus(PartnerStatus.ACTIVE);

        deliveries = List.of(serviceToChange, delivery2);
    }

    private PartnerRoute createPartnerRoute() {
        return new PartnerRoute()
            .setLocationFrom(1)
            .setLocationTo(2);
    }

    private Set<PartnerExternalParamValue> createExternalParams() {
        paramValueToChange = new PartnerExternalParamValue(paramTypes.get(0), "test-value-for-type-0-created");
        paramValueToDelete = new PartnerExternalParamValue(paramTypes.get(1), "test-value-for-type-1-created");
        return Set.of(paramValueToChange, paramValueToDelete);
    }

    private static Calendar createCalendar() {
        return new Calendar().addCalendarDays(Sets.newHashSet(
            new CalendarDay().setDay(LocalDate.of(2008, 12, 31)),
            new CalendarDay().setDay(LocalDate.of(2008, 10, 15))
        ));
    }

    private static Set<CargoType> createCargoTypes() {
        return Collections.singleton(createCargoType());
    }

    private static CargoType createCargoType() {
        CargoType newOne = new CargoType();
        newOne.setCargoType(1337);
        newOne.setDescription("CargoTypeLeet");
        return newOne;
    }

    private void addForbiddenCargoTypesToPartner(Partner partner, Collection<CargoType> cargoTypes) {
        cargoTypes.forEach(ct -> {
            ct.getPartners().add(partner);
            partner.getForbiddenCargoTypes().add(ct);
        });
    }
}
