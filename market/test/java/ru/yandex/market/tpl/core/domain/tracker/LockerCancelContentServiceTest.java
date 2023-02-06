package ru.yandex.market.tpl.core.domain.tracker;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelContentService;
import ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelPayload;
import ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelTicketParams;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelTicketType.ORDER;
import static ru.yandex.market.tpl.core.domain.tracker.send.locker.LockerCancelTicketType.TASK;

@RequiredArgsConstructor
class LockerCancelContentServiceTest extends TplAbstractTest {

    private final LockerCancelContentService lockerCancelContentService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final CompanyRepository companyRepository;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final OrderRepository orderRepository;

    private static LockerCancelTicketParams orderLockerParams;
    private static LockerCancelTicketParams taskPvzParams;
    private static LockerCancelTicketParams taskLavkaParams;
    private static LockerCancelTicketParams taskMarketCourierParams;

    private Company company;
    private Order order1;
    private Order order2;
    private Order order3;
    private PickupPoint pickupPoint1;
    private PickupPoint pickupPoint2;

    @Value("${partner.interface.url}")
    private String partnerInterfaceUrl;
    @Value("${external.lms.admin.pp.url}")
    private String lmsPickupPointBaseUrl;
    @Value("${external.lms.admin.partner.url}")
    private String lmsPartnerBaseUrl;

    @BeforeEach
    void init() {
        var payload = new LockerCancelPayload("1", "LOCKER",
                null, List.of("456"), ORDER, "Нет доступа к постамату", "comment", List.of("https://ya.ru",
                "https://yandex.ru"), null);
        orderLockerParams = new LockerCancelTicketParams(payload);

        payload = new LockerCancelPayload("1", "PVZ",
                789L, List.of("789", "123"), TASK, "Заказ слишком большой", "comment", List.of("https://ya.ru"), null);
        taskPvzParams = new LockerCancelTicketParams(payload);

        payload = new LockerCancelPayload("1", "LAVKA",
                789L, List.of("789", "123"), TASK, "Другое", "comment", List.of("https://ya.ru", "https://yandex.ru"),
                null);
        taskLavkaParams = new LockerCancelTicketParams(payload);

        payload = new LockerCancelPayload("1", "LOCKER",
                789L, List.of("789", "123"), TASK, "Нет доступа к постамату", "comment", null, "MARKETCOURIER");
        taskMarketCourierParams = new LockerCancelTicketParams(payload);

        testUserHelper.findOrCreateSuperCompany(123L, null);
        Optional<Company> companyOpt = companyRepository.findCompanyByIsSuperCompanyTrue();
        assertThat(companyOpt).isNotEmpty();
        company = companyOpt.get();

        String address = "село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная улица, 9A, 2";

        pickupPoint1 = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 321L, 1L);
        pickupPoint1.setAddress(address);
        pickupPoint1 = pickupPointRepository.save(pickupPoint1);

        pickupPoint2 = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 432L, 2L);
        pickupPoint2.setAddress(address);
        pickupPoint2 = pickupPointRepository.save(pickupPoint2);

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("123")
                .pickupPoint(pickupPoint1)
                .deliveryServiceId(239L)
                .build());
        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(198L)
                .externalOrderId("456")
                .build());
        order3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(198L)
                .pickupPoint(pickupPoint2)
                .externalOrderId("789")
                .build());
    }

    @AfterEach
    void afterEach() {
    }

    @Test
    void getSummaryForCancelLockerOrderTest() {
        String summary = lockerCancelContentService.getSummary(orderLockerParams);
        assertThat(summary).isEqualTo("Отмена заказа 456 по причине Нет доступа к постамату");
    }

    @Test
    void getSummaryForCancelLockerTaskTest() {
        String summary = lockerCancelContentService.getSummary(taskPvzParams);
        assertThat(summary).isEqualTo("Отмена задания 789 по причине Заказ слишком большой");
    }

    @Test
    void getSummaryForCancelLockerLavkaTest() {
        String summary = lockerCancelContentService.getSummary(taskLavkaParams);
        assertThat(summary).isEqualTo("Отмена задания 789 по причине Другое");
    }

    @Test
    void getDescriptionForCancelLockerOrderTest() {
        String summary = lockerCancelContentService.getDescription(
                orderLockerParams,
                orderRepository.findAllByExternalOrderIdIn(List.of(order2.getExternalOrderId()))
        );
        assertThat(summary).isEqualTo("ID заказа: 456\n" +
                "Ссылка на заказ в ПИ: ((" + partnerInterfaceUrl + "/tpl/" + company.getId() + "/orders/456))\n" +
                "Комментарий: comment\n" +
                "Фото:\n" +
                "((https://ya.ru/orig Фото_1))\n" +
                "((https://yandex.ru/orig Фото_2))\n");
    }

    @Test
    void getDescriptionForCancelLockerTaskTest() {
        String summary = lockerCancelContentService.getDescription(
                taskPvzParams,
                orderRepository.findAllByExternalOrderIdIn(List.of(order1.getExternalOrderId(),
                        order3.getExternalOrderId()))
        );
        assertThat(summary).isEqualTo("ID задания: 789\n" +
                "ID заказов: 789, 123\n" +
                "Ссылки на заказ в ПИ:\n" +
                "((" + partnerInterfaceUrl + "/tpl/" + company.getId() + "/orders/789))\n" +
                "((" + partnerInterfaceUrl + "/tpl/" + company.getId() + "/orders/123))\n" +
                "Логистические точки:\n" +
                "((" + lmsPickupPointBaseUrl + "/321 село Зудово, Болотнинский район, Новосибирская область, Россия, " +
                "Солнечная улица, 9A, 2))\n" +
                "((" + lmsPickupPointBaseUrl + "/432 село Зудово, Болотнинский район, Новосибирская область, Россия, " +
                "Солнечная улица, 9A, 2))\n" +
                "Партнеры:\n" +
                "((" + lmsPartnerBaseUrl + "/1 Партнер 1))\n" +
                "((" + lmsPartnerBaseUrl + "/2 Партнер 2))\n" +
                "Комментарий: comment\n" +
                "Фото:\n" +
                "((https://ya.ru/orig Фото_1))\n");
    }

    @Test
    void getDescriptionForCancelLavkaTaskTest() {
        String summary = lockerCancelContentService.getDescription(
                taskLavkaParams,
                orderRepository.findAllByExternalOrderIdIn(List.of(order1.getExternalOrderId(),
                        order3.getExternalOrderId()))
        );
        assertThat(summary).isEqualTo("ID задания: 789\n" +
                "ID заказов: 789, 123\n" +
                "Ссылки на заказ в ПИ:\n" +
                "((" + partnerInterfaceUrl + "/tpl/" + company.getId() + "/orders/789))\n" +
                "((" + partnerInterfaceUrl + "/tpl/" + company.getId() + "/orders/123))\n" +
                "Логистические точки:\n" +
                "((" + lmsPickupPointBaseUrl + "/321 село Зудово, Болотнинский район, Новосибирская область, Россия, " +
                "Солнечная улица, 9A, 2))\n" +
                "((" + lmsPickupPointBaseUrl + "/432 село Зудово, Болотнинский район, Новосибирская область, Россия, " +
                "Солнечная улица, 9A, 2))\n" +
                "Партнеры:\n" +
                "((" + lmsPartnerBaseUrl + "/1 Партнер 1))\n" +
                "((" + lmsPartnerBaseUrl + "/2 Партнер 2))\n" +
                "Комментарий: comment\n" +
                "Фото:\n" +
                "((https://ya.ru/orig Фото_1))\n" +
                "((https://yandex.ru/orig Фото_2))\n");
    }

    @Test
    void getDescriptionForCancelCourierTaskTest() {
        String summary = lockerCancelContentService.getDescription(
                taskMarketCourierParams,
                orderRepository.findAllByExternalOrderIdIn(List.of(order1.getExternalOrderId(),
                        order3.getExternalOrderId()))
        );
        assertThat(summary).isEqualTo("ID задания: 789\n" +
                "ID заказов: 789, 123\n" +
                "Ссылки на заказ в ПИ:\n" +
                "((" + partnerInterfaceUrl + "/tpl/" + company.getId() + "/orders/789))\n" +
                "((" + partnerInterfaceUrl + "/tpl/" + company.getId() + "/orders/123))\n" +
                "Логистические точки:\n" +
                "((" + lmsPickupPointBaseUrl + "/321 село Зудово, Болотнинский район, Новосибирская область, Россия, " +
                "Солнечная улица, 9A, 2))\n" +
                "((" + lmsPickupPointBaseUrl + "/432 село Зудово, Болотнинский район, Новосибирская область, Россия, " +
                "Солнечная улица, 9A, 2))\n" +
                "Партнеры:\n" +
                "((" + lmsPartnerBaseUrl + "/1 Партнер 1))\n" +
                "((" + lmsPartnerBaseUrl + "/2 Партнер 2))\n" +
                "Комментарий: comment\n");
    }

    @Test
    void getLockerComponentIdTest() {
        Optional<Long> componentIdOpt = lockerCancelContentService.getComponentId(orderLockerParams);
        assertThat(componentIdOpt).isNotEmpty();
        assertThat(componentIdOpt.get()).isEqualTo(100087L);
    }

    @Test
    void getPvzComponentIdTest() {
        Optional<Long> componentIdOpt = lockerCancelContentService.getComponentId(taskPvzParams);
        assertThat(componentIdOpt).isNotEmpty();
        assertThat(componentIdOpt.get()).isEqualTo(100088L);
    }

    @Test
    void getLavkaComponentIdTest() {
        Optional<Long> componentIdOpt = lockerCancelContentService.getComponentId(taskLavkaParams);
        assertThat(componentIdOpt).isNotEmpty();
        assertThat(componentIdOpt.get()).isEqualTo(100091L);
    }

    @Test
    void getMarketCourierComponentIdTest() {
        Optional<Long> componentIdOpt = lockerCancelContentService.getComponentId(taskMarketCourierParams);
        assertThat(componentIdOpt).isEmpty();
    }

    @Test
    void getDeliveryServiceForCancelLockerLavkaTest() {
        String deliveryServiceNames = lockerCancelContentService.getDeliveryService(List.of(order1, order2));
        assertThat(deliveryServiceNames).isEqualTo("Маркет ПВЗ, Маркет Курьер");
    }

    @Test
    void getLockerTag() {
        assertThat(orderLockerParams.getTag()).isEqualTo("нет_доступа_к_постамату");
    }

    @Test
    void getLavkaTag() {
        assertThat(taskLavkaParams.getTag()).isEqualTo("другое");
    }

    @Test
    void getPvzTag() {
        assertThat(taskPvzParams.getTag()).isEqualTo("заказ_слишком_большой");
    }
}
