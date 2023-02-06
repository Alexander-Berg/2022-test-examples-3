package ru.yandex.market.tpl.core.domain.partner;

import java.time.LocalTime;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeliveryServiceFactory {
    private final SortingCenterService sortingCenterService;
    private final PartnerRepository<DeliveryService> deliveryServicePartnerRepository;

    @Autowired
    public DeliveryServiceFactory(SortingCenterService sortingCenterService,
                                  PartnerRepository<DeliveryService> deliveryServicePartnerRepository) {
        this.sortingCenterService = sortingCenterService;
        this.deliveryServicePartnerRepository = deliveryServicePartnerRepository;
    }

    public void createScheduledIntervalsForDeliveryServiceTestMatchInterval() {
        DeliveryService deliveryService = getDeliveryService();

        Schedule schedule = deliveryService.getSchedule();
        List<ScheduleInterval> intervals = schedule.getIntervals();
        intervals.clear();

        intervals.add(new ScheduleInterval(schedule, LocalTime.parse("19:00"), LocalTime.parse("23:59"), true));
        intervals.add(new ScheduleInterval(schedule, LocalTime.parse("09:00"), LocalTime.parse("22:00"), true));
        intervals.add(new ScheduleInterval(schedule, LocalTime.parse("10:00"), LocalTime.parse("18:00"), true));
        intervals.add(new ScheduleInterval(schedule, LocalTime.parse("10:00"), LocalTime.parse("23:00"), true));
        intervals.add(new ScheduleInterval(schedule, LocalTime.parse("15:00"), LocalTime.parse("22:00"), true));

        deliveryServicePartnerRepository.save(deliveryService);
    }

    public void createScheduledIntervalsForDeliveryServiceAllTimeAvailable() {
        DeliveryService deliveryService = getDeliveryService();

        Schedule schedule = deliveryService.getSchedule();
        List<ScheduleInterval> intervals = schedule.getIntervals();
        intervals.clear();

        intervals.add(new ScheduleInterval(schedule, LocalTime.parse("00:00"), LocalTime.parse("23:59"), true));
        deliveryServicePartnerRepository.save(deliveryService);
    }

    public DeliveryService getDeliveryService() {
        return sortingCenterService
                .findDsById(DeliveryService.FAKE_DS_ID);
    }
}
