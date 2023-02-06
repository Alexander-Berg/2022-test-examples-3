package ru.yandex.market.tpl.internal.service.report;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.shift.routingList.RoutingListDataDto;
import ru.yandex.market.tpl.api.model.shift.routingList.RoutingListRow;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserService;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.partner.PartnerShiftService;
import ru.yandex.market.tpl.internal.TplIntAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.CALL_REQUIRED;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.DO_NOT_CALL;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

@RequiredArgsConstructor
public class RoutingListReportServiceTest extends TplIntAbstractTest {

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserService userService;
    private final PartnerShiftService partnerShiftService;
    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftCommandDataHelper helper;
    private final Clock clock;
    private final RoutingListReportService routingListReportService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;
    private UserShift userShift;
    private User user;
    private CompanyPermissionsProjection company;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(1L);
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var order1 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId("34567890")
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .city("Крыжополь")
                                .apartment("23")
                                .entrance("4")
                                .entryPhone("+79991728283")
                                .floor(2)
                                .geoPoint(GeoPoint.DEFAULT_GEO_POINT)
                                .house("3")
                                .street("Авиамоторная")
                                .build())
                        .recipientFio("Константин Константиновский")
                        .recipientNotes(DO_NOT_CALL_DELIVERY_PREFIX + ", пишите сообщение, стучите в дверь")
                        .deliveryInterval(LocalTimeInterval.valueOf("12:00-14:00"))
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsCount(1)
                                .itemsItemCount(1)
                                .itemsPrice(BigDecimal.valueOf(4000.0))
                                .build()
                        )
                        .build()
        );

        var order2 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId("12345678")
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .city("Ставрополь")
                                .apartment("19")
                                .entrance("1")
                                .entryPhone("+79984728283")
                                .floor(5)
                                .geoPoint(GeoPoint.DEFAULT_GEO_POINT)
                                .house("3")
                                .street("Дубнинское")
                                .build())
                        .recipientFio("Сергей Быстров")
                        .recipientNotes("Домофон не работает, оплата картой при получении")
                        .deliveryInterval(LocalTimeInterval.valueOf("09:00-12:00"))
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsCount(1)
                                .itemsItemCount(1)
                                .itemsPrice(BigDecimal.valueOf(9000.0))
                                .build()
                        )
                        .build()
        );


        var order3 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId("12345671")
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .city("Default")
                                .apartment("109")
                                .entrance("18")
                                .entryPhone("+71114728283")
                                .floor(5)
                                .geoPoint(GeoPoint.DEFAULT_GEO_POINT)
                                .house("23")
                                .street("Дубнинское")
                                .build())
                        .recipientFio("Серёга")
                        .recipientNotes("Домофон не работает, оплата картой при получении")
                        .deliveryInterval(LocalTimeInterval.valueOf("09:00-12:00"))
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsCount(1)
                                .itemsItemCount(1)
                                .itemsPrice(BigDecimal.valueOf(1000.0))
                                .isFashion(true)
                                .build()
                        )
                        .build()
        );

        var order4 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId("111111")
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .city("Default")
                                .apartment("109")
                                .entrance("18")
                                .entryPhone("+71224728283")
                                .floor(5)
                                .geoPoint(GeoPoint.DEFAULT_GEO_POINT)
                                .house("123")
                                .street("Дубнинское")
                                .build())
                        .recipientFio("Серёга")
                        .deliveryInterval(LocalTimeInterval.valueOf("09:00-12:00"))
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsCount(1)
                                .itemsItemCount(1)
                                .itemsPrice(BigDecimal.valueOf(1000.0))
                                .isFashion(false)
                                .build()
                        )
                        .build()
        );

        order4.getDelivery().setRecipientNotes(null);
        orderRepository.save(order4);

        var addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .city("My City")
                .apartment("333")
                .entrance("222")
                .entryPhone("+71114721111")
                .floor(1)
                .geoPoint(GeoPoint.DEFAULT_GEO_POINT)
                .house("2")
                .street("Свободы")
                .build();

        var multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .buyerYandexUid(1L)
                .recipientFio("Фешен Модный")
                .recipientNotes("Не опаздывать")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .itemsPrice(BigDecimal.valueOf(10.0))
                        .isFashion(false)
                        .build()
                )
                .build());

        var multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321234")
                .buyerYandexUid(1L)
                .recipientFio("Dungeon Master")
                .recipientNotes("Жду тебя <3")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .itemsPrice(BigDecimal.valueOf(300.0))
                        .isFashion(true)
                        .build()
                )
                .build());

        var multiOrder3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("1321234")
                .buyerYandexUid(1L)
                .recipientFio("Billy")
                .recipientNotes("Fashion")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .itemsPrice(BigDecimal.valueOf(1.0))
                        .isFashion(true)
                        .build()
                )
                .build());

        company = CompanyPermissionsProjection.builder().isSuperCompany(true).id(1L).build();
        userHelper.addCouriersToCompany("COMPANY", Set.of(user));

        transactionTemplate.execute(ts -> {
            userShift = userShiftRepository.findById(commandService.createUserShift(
                    UserShiftCommand.Create.builder()
                            .userId(user.getId())
                            .shiftId(shift.getId())
                            .routePoint(helper.taskPrepaid("addr1", 9, order1.getId()))
                            .routePoint(helper.taskPrepaid("addr2", 12, order2.getId()))
                            .routePoint(helper.taskPrepaid("addr3", 15, order3.getId()))
                            .routePoint(helper.taskPrepaid("addr4", 15, order4.getId()))
                            .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                            .build())).orElseThrow();


            userShiftReassignManager.assign(userShift, multiOrder1);
            userShiftReassignManager.assign(userShift, multiOrder2);
            userShiftReassignManager.assign(userShift, multiOrder3);
            return null;
        });
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);
        configurationServiceAdapter.insertValue(ConfigurationProperties.ADD_FASHION_INFO_TO_ROUTING_LIST_ENABLED, true);
    }

    @Test
    @Disabled // падает в арке из-за проблем с установкой доп пакетов
    @DisplayName("Тест для отладки pdf маршрутного листа")
    void routingListPdfTest() throws IOException {
        configurationServiceAdapter.insertValue(ConfigurationProperties.DO_NOT_CALL_ENABLED, true);

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));

        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2ByUserShiftIds(List.of(userShift.getId()),
                        CompanyPermissionsProjection.builder().build());

        assertThat(routingListData).isNotEmpty();

        List<RoutingListRow> rows = routingListData.get(0).getOrders();

        assertThat(rows).isNotEmpty();
        assertThat(rows).extracting("orderNumber", "callRequirement")
                .contains(tuple("34567890", DO_NOT_CALL),
                        tuple("12345678", CALL_REQUIRED)
                );
        var multiOrderIds = rows.stream()
                .filter(RoutingListRow::isMultiOrder)
                .map(RoutingListRow::getMultiOrderId)
                .distinct()
                .collect(Collectors.toList());
        var replacements = new HashMap<String, String>();
        for (int i = 0; i < multiOrderIds.size(); i++) {
            replacements.put("{multiOrderId" + i + "}", multiOrderIds.get(i));
        }

        byte[] reportData = routingListReportService.getRoutingListPdf(List.of(userShift.getId()), company);

        assertThat(reportData).isNotEmpty();

        PdfHelper.assertPdf(
                new ByteArrayInputStream(reportData),
                "marshroute_list.txt", "/Комаров Пашка/", replacements
        );

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));
    }

    @Test
    @Disabled // падает в арке из-за проблем с установкой доп пакетов
    @DisplayName("Тест для верстки pdf маршрутного листа с большим количеством заказов")
    void bigRoutingListPdfTest() throws IOException {
        UserShift userShift = initUserShiftWithManyOrders();
        configurationServiceAdapter.insertValue(ConfigurationProperties.DO_NOT_CALL_ENABLED, true);

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));

        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2ByUserShiftIds(List.of(userShift.getId()), CompanyPermissionsProjection.builder().build());

        assertThat(routingListData).isNotEmpty();

        List<RoutingListRow> rows = routingListData.get(0).getOrders();

        assertThat(rows).isNotEmpty();

        byte[] reportData = routingListReportService.getRoutingListPdf(List.of(userShift.getId()), company);
        assertThat(reportData).isNotEmpty();
        String path = System.getProperty("user.home") + "/test/routing-list-report" + Instant.now() + ".pdf";
        FileOutputStream fos = new FileOutputStream(path);

        fos.write(reportData);

        fos.flush();
        fos.close();
    }

    @Test
    @Disabled // ошибка в арке из-за https://github.com/AdoptOpenJDK/openjdk-docker/issues/75
    @DisplayName("Тест для отладки pdf маршрутного листа с выключенным флагом не звонить")
    void routingListPdfTestNegative() throws IOException {
        configurationServiceAdapter.insertValue(ConfigurationProperties.DO_NOT_CALL_ENABLED, false);

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));

        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2ByUserShiftIds(List.of(userShift.getId()), CompanyPermissionsProjection.builder().build());

        assertThat(routingListData).isNotEmpty();

        List<RoutingListRow> rows = routingListData.get(0).getOrders();

        assertThat(rows).isNotEmpty();
        assertThat(rows).extracting("orderNumber", "callRequirement")
                .contains(tuple("34567890", null),
                        tuple("12345678", null));
        var multiOrderIds = rows.stream()
                .filter(RoutingListRow::isMultiOrder)
                .map(RoutingListRow::getMultiOrderId)
                .distinct()
                .collect(Collectors.toList());
        var replacements = new HashMap<String, String>();
        for (int i = 0; i < multiOrderIds.size(); i++) {
            replacements.put("{multiOrderId" + i + "}", multiOrderIds.get(i));
        }

        byte[] reportData = routingListReportService.getRoutingListPdf(List.of(userShift.getId()), company);

        assertThat(reportData).isNotEmpty();

        PdfHelper.assertPdf(
                new ByteArrayInputStream(reportData),
                "marshroute_list_negative.txt", "/Комаров Пашка/", replacements
        );

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));
    }

    @Test
    @Disabled // падает в арке из-за проблем с установкой доп пакетов
    @DisplayName("Тест для проверки работы при превышении количества маршрутных листов")
    void routingListSizeTest() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.MAX_SIZE_OF_ROUTING_LIST, 20);

        List<Long> userShiftIds = new ArrayList<>();
        for (long i = 0; i < 20; i++) {
            userShiftIds.add(i);
        }

        routingListReportService.getRoutingListPdf(userShiftIds, company);

        assertThrows(TplIllegalArgumentException.class, () -> {
            userShiftIds.add(21L);
            routingListReportService.getRoutingListPdf(userShiftIds, company);
        });

    }

    private UserShift initUserShiftWithManyOrders() {
        user = userHelper.findOrCreateUser(1L);
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < 51; i++) {
            OrderGenerateService.OrderGenerateParam draftOrder =
                    OrderGenerateService.OrderGenerateParam.builder().build();
            Order generateOrder = orderGenerateService.createOrder(draftOrder);
            orders.add(generateOrder);
        }

        company = CompanyPermissionsProjection.builder().isSuperCompany(true).id(2L).build();
        userHelper.addCouriersToCompany("COMPANY2", Set.of(user));

        UserShiftCommand.Create.CreateBuilder userShiftCreateBuilder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE);
        orders.forEach(order ->
                userShiftCreateBuilder.routePoint(helper.taskPrepaid("addr1", 9, order.getId())));
        UserShift userShift = userShiftRepository.findById(
                commandService.createUserShift(userShiftCreateBuilder.build())
        )
                .orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);

        return userShift;
    }
}
