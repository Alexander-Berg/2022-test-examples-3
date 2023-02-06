package ru.yandex.market.sc.tms.dbqueue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.dbqueue.metrics.UploadCourierShiftsProducer;
import ru.yandex.market.sc.core.domain.courier.model.PartnerCourierDto;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.shift.repository.CourierShift;
import ru.yandex.market.sc.core.domain.courier.shift.repository.CourierShiftRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.external.delivery_service.TplClient;
import ru.yandex.market.sc.core.external.delivery_service.model.TplCouriers;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * @author valter
 */
@EmbeddedDbTmsTest
class UploadCourierShiftsServiceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    DbQueueTestUtil dbQueueTestUtil;
    @Autowired
    UploadCourierShiftsProducer uploadCourierShiftsProducer;
    @Autowired
    Clock clock;
    @Autowired
    CourierShiftRepository courierShiftRepository;
    @MockBean
    TplClient tplClient;

    Courier courier;
    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        courier = testFactory.storedCourier();
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void singleCourier() {
        var shiftDate = LocalDate.now(clock);
        var shiftStartTime = LocalTime.now(clock);
        mockTplGetCouriers(courier.getId(), shiftDate, shiftStartTime);

        uploadCourierShiftsProducer.produce(sortingCenter, shiftDate);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.UPLOAD_COURIER_SHIFTS);

        List<CourierShift> courierShifts = courierShiftRepository
                .findAllByCourierInAndShiftDateAndSortingCenterOrderById(List.of(courier), shiftDate, sortingCenter);
        assertThat(courierShifts).hasSize(1);
        assertThat(courierShifts.get(0)).isEqualToIgnoringGivenFields(
                new CourierShift(sortingCenter, courier, shiftDate, shiftStartTime),
                "id", "createdAt", "updatedAt"
        );
    }

    @Test
    void notExistingCourier() {
        var shiftDate = LocalDate.now(clock);
        var shiftStartTime = LocalTime.now(clock);
        mockTplGetCouriers(courier.getId() + 1, shiftDate, shiftStartTime);

        uploadCourierShiftsProducer.produce(sortingCenter, shiftDate);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.UPLOAD_COURIER_SHIFTS);

        List<CourierShift> courierShifts = courierShiftRepository.findAll();
        assertThat(courierShifts).isEmpty();
    }

    @Test
    void updateCourierShiftStartTime() {
        var shiftDate = LocalDate.now(clock);
        var oldShiftStartTime = LocalTime.now(clock);
        mockTplGetCouriers(courier.getId(), shiftDate, oldShiftStartTime);
        uploadCourierShiftsProducer.produce(sortingCenter, shiftDate);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.UPLOAD_COURIER_SHIFTS);

        var newShiftStartTime = oldShiftStartTime.plusHours(1);
        mockTplGetCouriers(courier.getId(), shiftDate, newShiftStartTime);
        uploadCourierShiftsProducer.produce(sortingCenter, shiftDate);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.UPLOAD_COURIER_SHIFTS);

        List<CourierShift> courierShifts = courierShiftRepository
                .findAllByCourierInAndShiftDateAndSortingCenterOrderById(List.of(courier), shiftDate, sortingCenter);
        assertThat(courierShifts).hasSize(1);
        assertThat(courierShifts.get(0)).isEqualToIgnoringGivenFields(
                new CourierShift(sortingCenter, courier, shiftDate, newShiftStartTime),
                "id", "createdAt", "updatedAt"
        );
    }

    @Test
    void oneShiftToCreateAndOneToUpdate() {
        var shiftDate = LocalDate.now(clock);
        var oldShift1StartTime = LocalTime.now(clock);
        var shift2StartTime = oldShift1StartTime.plusHours(1);
        Courier courier2 = testFactory.storedCourier(courier.getId() + 1);

        mockTplGetCouriers(courier.getId(), shiftDate, oldShift1StartTime);
        uploadCourierShiftsProducer.produce(sortingCenter, shiftDate);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.UPLOAD_COURIER_SHIFTS);
        var newShift1StartTime = oldShift1StartTime.plusHours(1);
        mockTplGetCouriers(
                shiftDate,
                courier.getId(), newShift1StartTime,
                courier2.getId(), shift2StartTime
        );
        uploadCourierShiftsProducer.produce(sortingCenter, shiftDate);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.UPLOAD_COURIER_SHIFTS);

        List<CourierShift> courierShifts = courierShiftRepository
                .findAllByCourierInAndShiftDateAndSortingCenterOrderById(
                        List.of(courier, courier2), shiftDate, sortingCenter
                );
        assertThat(courierShifts).hasSize(2);
        assertThat(courierShifts.get(0)).isEqualToIgnoringGivenFields(
                new CourierShift(sortingCenter, courier, shiftDate, newShift1StartTime),
                "id", "createdAt", "updatedAt"
        );
        assertThat(courierShifts.get(1)).isEqualToIgnoringGivenFields(
                new CourierShift(sortingCenter, courier2, shiftDate, shift2StartTime),
                "id", "createdAt", "updatedAt"
        );
    }

    void mockTplGetCouriers(long courierUid, LocalDate shiftDate, LocalTime shiftStartTime) {
        TplCouriers tplCouriers = new TplCouriers(List.of(
                new TplCouriers.TplCourier(
                        courierUid, "Василий Петров",
                        new PartnerCourierDto.CourierCompany("ООО Ромашка-Курьер"),
                        shiftDate, shiftStartTime
                )
        ));
        doReturn(tplCouriers).when(tplClient).getCouriers(sortingCenter.getId(), sortingCenter.getToken(), shiftDate);
    }

    void mockTplGetCouriers(
            LocalDate shiftDate,
            long courier1Uid, LocalTime shift1StartTime,
            long courier2Uid, LocalTime shift2StartTime
    ) {
        TplCouriers tplCouriers = new TplCouriers(List.of(
                new TplCouriers.TplCourier(
                        courier1Uid, "Василий Петров",
                        new PartnerCourierDto.CourierCompany("ООО Ромашка-Курьер"),
                        shiftDate, shift1StartTime
                ),
                new TplCouriers.TplCourier(
                        courier2Uid, "Gtтр Васильев",
                        new PartnerCourierDto.CourierCompany("ООО Ромашка-Курьер"),
                        shiftDate, shift2StartTime
                )
        ));
        doReturn(tplCouriers).when(tplClient).getCouriers(sortingCenter.getId(), sortingCenter.getToken(), shiftDate);
    }


}
