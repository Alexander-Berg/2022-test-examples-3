package ru.yandex.market.tpl.core.domain.yago.history;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.CreateOrderDto;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrder;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderCommand;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderCommandService;
import ru.yandex.market.tpl.core.service.FlushManager;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class YandexGoOrderHistoryRepositoryTest {

    private final YandexGoOrderHistoryRepository repository;
    private final YandexGoOrderCommandService service;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;

    private final PartnerRepository<DeliveryService> partnerRepository;
    private final Clock clock;
    private final FlushManager flushManager;

    @Test
    void shouldSaveYandexGoRecordAsExpected() {
        // given
        DeliveryService partner = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
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
        YandexGoOrder yandexGoOrder = service.createOrder(command);

        YandexGoOrderHistoryRecord record =
                new YandexGoOrderHistoryRecord()
                        .setOrder(yandexGoOrder)
                        .setStatus(OrderStatusType.ORDER_CREATED);

        // when
        repository.save(record);
        flushManager.flush();

        // then
        assertThat(record.getId()).isNotNull();
        assertThat(record.getStatus()).isEqualTo(OrderStatusType.ORDER_CREATED);
        assertThat(record.getOrder().getId()).isEqualTo(yandexGoOrder.getId());
        assertThat(record.getUpdatedAt()).isNotNull();
    }

}
