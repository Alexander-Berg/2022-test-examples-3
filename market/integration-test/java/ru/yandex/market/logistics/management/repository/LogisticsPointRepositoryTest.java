package ru.yandex.market.logistics.management.repository;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Address;
import ru.yandex.market.logistics.management.domain.entity.Contact;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.Phone;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.domain.entity.ServiceCode;
import ru.yandex.market.logistics.management.domain.entity.type.PhoneType;
import ru.yandex.market.logistics.management.domain.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.domain.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.entity.type.ServiceType;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@SuppressWarnings("checkstyle:MagicNumber")
class LogisticsPointRepositoryTest extends AbstractContextualTest {

    private static final Address ADDRESS = new Address().setAddressString("NSK");

    private static final Contact CONTACT = new Contact().setName("LORD");

    private static final Phone PHONE_1 = new Phone().setComment("person").setType(PhoneType.PRIMARY);

    private static final Phone PHONE_2 = new Phone().setComment("work").setType(PhoneType.ADDITIONAL);

    private static final ScheduleDay SCHEDULE_DAY = new ScheduleDay()
        .setDay(1)
        .setFrom(LocalTime.of(12, 0))
        .setTo(LocalTime.of(13, 0));

    private static final Schedule SCHEDULE = new Schedule().setScheduleDays(Collections.singleton(SCHEDULE_DAY));

    private static final ServiceCode SERVICE_CODE_1 = new ServiceCode()
        .setName("Вознаграждение за перечисление денежных средств")
        .setCode(ServiceCodeName.CASH_SERVICE)
        .setType(ServiceType.INTERNAL)
        .setOptional(false);

    private static final ServiceCode SERVICE_CODE_2 = new ServiceCode()
        .setName("Ожидание курьера")
        .setCode(ServiceCodeName.WAIT_20)
        .setType(ServiceType.INTERNAL)
        .setOptional(false);

    private static final LogisticsPoint LOGISTICS_POINT = new LogisticsPoint()
        .setExternalId("ПВЗ")
        .setAddress(ADDRESS)
        .addAllPhones(new HashSet<>(Arrays.asList(PHONE_1, PHONE_2)))
        .setContact(CONTACT)
        .setActive(true)
        .setSchedule(SCHEDULE)
        .setType(PointType.PICKUP_POINT)
        .setCashAllowed(true)
        .setPrepayAllowed(true)
        .setCardAllowed(false)
        .setName("BEST_TERMINAL")
        .setPhotos(null)
        .setInstruction("100 шагов назад")
        .setReturnAllowed(true)
        .setPickupPointType(PickupPointType.TERMINAL)
        .addAllServiceCodes(new HashSet<>(Arrays.asList(SERVICE_CODE_1, SERVICE_CODE_2)))
        .setStoragePeriod(7)
        .setMaxWeight(7d)
        .setMaxLength(7)
        .setMaxWidth(7)
        .setMaxHeight(7)
        .setMaxSidesSum(7);

    @Autowired
    private LogisticsPointRepository logisticsPointRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @CleanDatabase
    void saveAndGet() {
        logisticsPointRepository.save(LOGISTICS_POINT);

        transactionTemplate.execute((status) -> {
            List<LogisticsPoint> logisticsPoints = logisticsPointRepository.findAll();

            softly.assertThat(logisticsPoints)
                .as("LogisticPoint should be loaded")
                .hasSize(1);

            LogisticsPoint logisticsPointFromDb = logisticsPoints.get(0);

            softly.assertThat(logisticsPointFromDb.getAddress())
                .as("Address should be loaded")
                .isNotNull();

            softly.assertThat(logisticsPointFromDb.getSchedule())
                .as("Schedule should be loaded")
                .isNotNull();

            softly.assertThat(logisticsPointFromDb.getContact())
                .as("Contact should be loaded")
                .isNotNull();

            softly.assertThat(logisticsPointFromDb.getContact())
                .isEqualTo(CONTACT);

            softly.assertThat(logisticsPointFromDb.getPhones())
                .as("Phones should be loaded")
                .hasSize(2);

            softly.assertThat(logisticsPointFromDb.getServiceCodes())
                .as("Services should be loaded")
                .hasSize(2);

            softly.assertThat(logisticsPointFromDb).isEqualTo(LOGISTICS_POINT);

            return null;
        });
    }

    @Test
    @DatabaseSetup("/data/repository/points/logistics_points.xml")
    void deleteLogisticsPoint() {
        transactionTemplate.execute((status) -> {
            logisticsPointRepository.delete(logisticsPointRepository.findAll().get(0));
            return null;
        });

        transactionTemplate.execute((status) -> {
            softly.assertThat(logisticsPointRepository.findAll()).hasSize(2);
            return null;
        });
    }

    @Test
    @DatabaseSetup("/data/repository/partner/partners_with_everything.xml")
    @Transactional
    void findWarehousesWithScheduleAndGatesByPartnerId() {
        List<LogisticsPoint> warehouses = logisticsPointRepository.findWarehousesScheduleByPartnerId(1L);
        softly.assertThat(warehouses).as("Has exactly one warehouse").hasSize(1);
        softly.assertThat(warehouses.get(0).getLogisticsPointGates()).as("Has exactly two gates").hasSize(2);
    }
}
