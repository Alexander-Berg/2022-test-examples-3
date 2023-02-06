package ru.yandex.market.sc.internal.controller.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.courier.model.InternalCourierDto;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@EmbeddedDbIntTest
public class InternalCourierServiceTest {

    @Autowired
    InternalCourierService internalCourierService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    CourierRepository courierRepository;

    Courier courier;

    @BeforeEach
    void init() {
        courier = testFactory.courier();
        courierRepository.save(courier);
    }

    @Test
    void updateCourierInfo_WhenTryUpdateExistCourier() {
        internalCourierService.updateCourierInfo(new InternalCourierDto(
                courier.getId(), "Иванов Илья", "А138ММ 02", "Лада ларгус", "+79295372774", "ЯндексТакси")
        );
        Courier savedCourier = courierRepository.findByIdOrThrow(courier.getId());
        assertThat(savedCourier.getName()).isEqualTo("Иванов Илья");
        assertThat(savedCourier.getCarNumber()).isEqualTo("А138ММ 02");
        assertThat(savedCourier.getCarDescription()).isEqualTo("Лада ларгус");
        assertThat(savedCourier.getPhone()).isEqualTo("+79295372774");
    }

    @Test
    void throwException_WhenTryUpdateNotExistsCourier() {
        thenThrownBy(() -> internalCourierService.updateCourierInfo(new InternalCourierDto(
                        -1L, "Иванов Илья", "А138ММ 02", "Лада ларгус", "+79295372774", null))
        ).isInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void throwException_WhenCourierIdIsNull() {
        thenThrownBy(() -> internalCourierService.updateCourierInfo(new InternalCourierDto(
                null, "Иванов Илья", "А138ММ 02", "Лада ларгус", "+79295372774", null))
        ).isInstanceOf(TplInvalidParameterException.class);
    }

}
