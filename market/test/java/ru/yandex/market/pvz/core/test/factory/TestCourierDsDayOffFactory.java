package ru.yandex.market.pvz.core.test.factory;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.core.domain.delivery_service.CourierDsDayOff;
import ru.yandex.market.pvz.core.domain.delivery_service.CourierDsDayOffCommandService;

@Transactional
public class TestCourierDsDayOffFactory extends TestObjectFactory {

    @Autowired
    private CourierDsDayOffCommandService courierDsDayOffCommandService;

    public CourierDsDayOff create(CourierDsDayOffParams params) {
        LocalDate dayOff = params.getDayOff() != null ? params.getDayOff() : LocalDate.now(clock);
        return courierDsDayOffCommandService.create(params.getCourierDeliveryServiceId(), dayOff);
    }

    public CourierDsDayOff create() {
        return create(CourierDsDayOffParams.builder().build());
    }

    @Data
    @Builder
    public static class CourierDsDayOffParams {

        @Builder.Default
        private long courierDeliveryServiceId = RandomUtils.nextLong();

        @Builder.Default
        private LocalDate dayOff;
    }
}
