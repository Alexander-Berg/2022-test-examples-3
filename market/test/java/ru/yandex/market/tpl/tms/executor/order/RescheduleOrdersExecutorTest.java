package ru.yandex.market.tpl.tms.executor.order;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;

import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

@RequiredArgsConstructor
public class RescheduleOrdersExecutorTest extends TplTmsAbstractTest {

    private final RescheduleOrdersExecutor executor;
    private final TestUserHelper testUserHelper;
    private final DsRepository dsRepository;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;


    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

// TODO   Даже пустой тест ломает другие тесты MARKETTPL-6657

//    @DisplayName("Пропускаем обработку заказов и не падаем в НПЕ у СД, к которым не привязан ни один СЦ")
//    @Test
//    void shouldSkipRescheduleBecauseOfNullSc() {
//        var user = testUserHelper.findOrCreateUser(1234L);
//        testUserHelper.createEmptyShift(user, LocalDate.now());
//        var sc = testUserHelper.sortingCenter(123L);
//        var ds = new DeliveryService();
//        ds.setSortingCenter(sc);
//        ds.setId(111L);
//        ds.setName("NAME");
//        ds.setToken("token");
//        ds.setDeliveryAreaMarginWidth(0L);
//        dsRepository.saveAndFlush(ds);
//
//        Order order = orderGenerateService.createOrder(
//                OrderGenerateService.OrderGenerateParam.builder()
//                        .deliveryServiceId(ds.getId())
//                        .build());
//
//        ds.setSortingCenter(null);
//        dsRepository.saveAndFlush(ds);
//
//        assertDoesNotThrow(() -> executor.doRealJob(null));
//        Order orderCheck = orderRepository.findByIdOrThrow(order.getId());
//        assertThat(orderCheck.getDelivery().getInterval())
//                .isEqualTo(order.getDelivery().getInterval());
//        clearAfterTest(user);
//        clearAfterTest(sc);
//        clearAfterTest(ds);
//    }
}
