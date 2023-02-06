package ru.yandex.market.tpl.core.domain.yago;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.CreateOrderDto;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class YandexGoOrderCommandServiceTest {

    private final YandexGoOrderRepository repository;
    private final YandexGoOrderCommandService service;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final Clock clock;

    @Test
    void shouldPersistOrder_whenCreateOrder() {
        // given
        User user = testUserHelper.findOrCreateUser(101);
        SortingCenter sortingCenter = sortingCenterService.findSortCenterForDs(239);
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), sortingCenter.getId());
        Long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.of(new CreateOrderDto()));
        YandexGoOrderCommand.Create command =
                new YandexGoOrderCommand.Create(
                        order.getId(),
                        order.getExternalOrderId(),
                        userShiftId,
                        0,
                        OffsetDateTime.now());

        // when
        YandexGoOrder actualOrder = service.createOrder(command);

        // then
        assertThat(repository.findByIdOrThrow(actualOrder.getId())).isNotNull();
    }

}
