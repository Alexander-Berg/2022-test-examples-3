package ru.yandex.market.logistics.management.repository;

import java.time.LocalTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Address;
import ru.yandex.market.logistics.management.domain.entity.Contact;
import ru.yandex.market.logistics.management.domain.entity.DeliveryInterval;
import ru.yandex.market.logistics.management.domain.entity.LegalInfo;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacity;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamType;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamValue;
import ru.yandex.market.logistics.management.domain.entity.PartnerRoute;
import ru.yandex.market.logistics.management.domain.entity.PartnerShop;
import ru.yandex.market.logistics.management.domain.entity.PartnerSubtype;
import ru.yandex.market.logistics.management.domain.entity.PartnerTariff;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;

@SuppressWarnings("checkstyle:MagicNumber")
class PartnerRepositorySaveTest extends AbstractContextualTest {

    private static final int LOCATION_FROM = 1;
    private static final int LOCATION_TO = 2;

    @Autowired
    private PartnerRepository repository;

    @Autowired
    private PartnerExternalParamTypeRepository partnerExternalParamTypeRepository;

    @Autowired
    private PartnerSubtypeRepository partnerSubtypeRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("/data/repository/partner/before/references_for_partner.xml")
    @ExpectedDatabase(
        value = "/data/repository/partner/after/references_for_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void save() {

        transactionTemplate.execute((status) -> {
                PartnerExternalParamType partnerExternalParamType = partnerExternalParamTypeRepository.getOne(1L);
                PartnerSubtype partnerSubtype = partnerSubtypeRepository.getOne(1L);

                LegalInfo legalInfo = new LegalInfo();
                PartnerShop partnerShop = new PartnerShop();
                DeliveryInterval deliveryInterval = new DeliveryInterval();
                PartnerRoute partnerRoute = new PartnerRoute()
                    .setLocationFrom(LOCATION_FROM)
                    .setLocationTo(LOCATION_TO);
                PartnerTariff partnerTariff = new PartnerTariff();
                LogisticsPoint logisticsPoint = new LogisticsPoint()
                    .setExternalId("CODE")
                    .setType(PointType.PICKUP_POINT)
                    .setAddress(new Address())
                    .setSchedule(new Schedule().addScheduledDay(
                        new ScheduleDay().setFrom(LocalTime.of(0, 0)).setTo(LocalTime.of(23, 59, 59))
                    ))
                    .setContact(new Contact());
                PartnerCapacity partnerCapacity = new PartnerCapacity()
                    .setLocationFrom(LOCATION_FROM)
                    .setLocationTo(LOCATION_TO)
                    .setType(CapacityType.REGULAR)
                    .setCountingType(CountingType.ORDER)
                    .setServiceType(CapacityService.DELIVERY)
                    .setValue(100L);
                PartnerExternalParamValue partnerExternalParamValue = new PartnerExternalParamValue()
                    .setParamType(partnerExternalParamType)
                    .setValue("value");
                Schedule schedule = new Schedule();

                Partner partner = new Partner()
                    .setId(1L)
                    .setMarketId(1L)
                    .setPartnerType(PartnerType.DELIVERY)
                    .setName("Partner0x32")
                    .setReadableName("Partner")
                    .setStatus(PartnerStatus.ACTIVE)
                    .setLocationId(1)
                    .setTrackingType("type1")
                    .setLogoUrl("https://logo.ru")
                    .addShop(partnerShop)
                    .setCalendarId(1L)
                    .addDeliveryInterval(deliveryInterval)
                    .addPartnerRoute(partnerRoute)
                    .addPartnerTariff(partnerTariff)
                    .addLogisticsPoint(logisticsPoint)
                    .addCapacity(partnerCapacity)
                    .setBillingClientId(1L)
                    .setRating(100)
                    .setLegalInfo(legalInfo)
                    .addExternalParam(partnerExternalParamValue)
                    .setDomain("domain")
                    .setIntakeSchedule(schedule)
                    .setPartnerSubtype(partnerSubtype);

                repository.save(partner);

                return null;
            }
        );
    }
}
