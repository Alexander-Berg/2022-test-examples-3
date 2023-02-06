package ru.yandex.market.tpl.tms.executor.order;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.geocoder.Component;
import ru.yandex.common.util.geocoder.GeoClient;
import ru.yandex.common.util.geocoder.Kind;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.create_task.OrderCreateTaskPartnerService;
import ru.yandex.market.tpl.core.domain.order.create_task.OrderCreateTaskQueryService;
import ru.yandex.market.tpl.core.domain.order.create_task.model.OrderCreateTaskItemStatus;
import ru.yandex.market.tpl.core.domain.order.create_task.model.OrderCreateTaskStatus;
import ru.yandex.market.tpl.core.domain.order.create_task.params.OrderCreateTaskItemParams;
import ru.yandex.market.tpl.core.domain.order.create_task.params.OrderCreateTaskParams;
import ru.yandex.market.tpl.core.domain.order.create_task.parser.excel.model.ExcelOrderModelDto.ExcelOrderModelDto;
import ru.yandex.market.tpl.core.domain.order.create_task.parser.excel.model.v1.ExcelV1OrderModel;
import ru.yandex.market.tpl.core.domain.order.create_task.parser.excel.model.v2.ExcelV2OrderModel;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.tms.service.external.TplCheckouterExternalService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.order.create_task.model.OrderCreateTaskType.EXCEL_MODEL_DTO;

@RequiredArgsConstructor
class ProcessOrderCreationTasksExecutorTest extends TplTmsAbstractTest {

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long MARKET_ORDER_ID = 123L;
    private static final long CAMPAIGN_ID = 1L;

    private final OrderCreateTaskPartnerService partnerService;
    private final ProcessOrderCreationTasksExecutor executor;

    private final OrderCreateTaskQueryService queryService;

    private final OrderRepository orderRepository;
    private final ShiftManager shiftManager;
    private final UserShiftRepository userShiftRepository;
    private final PartnerRepository<DeliveryService> deliveryServiceRepository;

    private final TransactionTemplate transactionTemplate;

    private final CheckouterClient checkouterClient;

    private final TestUserHelper testUserHelper;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private GeoClient geoClient;

    @MockBean
    private TplCheckouterExternalService tplCheckouterExternalService;

    private TestUserHelper.CompanyGenerateParam baseCompanyGenerateParam;

    @BeforeEach
    void before() {
        baseCompanyGenerateParam = TestUserHelper.CompanyGenerateParam.builder().campaignId(CAMPAIGN_ID).companyName(
                "zaxarello company").login("zaxarello login").businessId(0L).build();
        Company company = testUserHelper.findOrCreateCompany(baseCompanyGenerateParam);
        when(tplCheckouterExternalService.getBusinessId(String.valueOf(CAMPAIGN_ID))).thenReturn(0L);
        this.clearAfterTest(company);
    }

    @AfterEach
    void cleanUp() {
        reset(geoClient);
    }

    @Test
    void testVersion2() {
        List<OrderCreateTaskParams<ExcelOrderModelDto>> createdTasks = uploadAndProcess("/order/creation_task" +
                "/version2.xlsx", DELIVERY_SERVICE_ID, CAMPAIGN_ID);
        var createdTask = createdTasks.get(0);
        executor.doRealJob(null);
        var processedTasks = queryService.getTask(createdTask.getId(), ExcelV2OrderModel.class);

        assertThat(processedTasks.getPartnerId()).isEqualTo(DELIVERY_SERVICE_ID);
        assertThat(processedTasks.getType()).isEqualTo(EXCEL_MODEL_DTO);
        assertThat(processedTasks.getStatus()).isEqualTo(OrderCreateTaskStatus.COMPLETE);
        assertThat(processedTasks.getAttemptsMade()).isEqualTo(1);
        assertThat(processedTasks.getErrorStacktrace()).isNull();
        assertThat(processedTasks.getItems()).hasSize(4);
    }

    @Test
    void uploadAndProcessSuccessfullyWithoutOptionalField() {
        List<OrderCreateTaskParams<ExcelOrderModelDto>> createdTasks = uploadAndProcess("/order/creation_task" +
                "/three_good_orders.xlsx", DELIVERY_SERVICE_ID, CAMPAIGN_ID);
        var createdTask = createdTasks.get(0);
        executor.doRealJob(null);
        var processedTasks = queryService.getTask(createdTask.getId(), ExcelV1OrderModel.class);

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);

        verify(checkouterClient).updateOrderDelivery(
                eq(MARKET_ORDER_ID), eq(ClientRole.SYSTEM), eq(1L), eq(delivery));

        assertThat(processedTasks.getPartnerId()).isEqualTo(DELIVERY_SERVICE_ID);
        assertThat(processedTasks.getType()).isEqualTo(EXCEL_MODEL_DTO);
        assertThat(processedTasks.getStatus()).isEqualTo(OrderCreateTaskStatus.COMPLETE);
        assertThat(processedTasks.getAttemptsMade()).isEqualTo(1);
        assertThat(processedTasks.getErrorStacktrace()).isNull();
        assertThat(processedTasks.getItems()).hasSize(3);

        var orderWithFullAddress = findOrder(processedTasks.getItems(), "N14");
        var orderWithSplittedAddress = findOrder(processedTasks.getItems(), "N15");

        checkTaskItem(orderWithFullAddress, OrderPaymentType.CARD);
        checkTaskItem(orderWithSplittedAddress, OrderPaymentType.CASH);
    }

    @Test
    void uploadAndProcessSuccessfullyWithOptionalField() {
        testUserHelper.createOrFindDbsDeliveryService();
        User userIvan =
                testUserHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                        .firstName("Иван")
                        .secondName("Иванов")
                        .email("ivanov@mail.ru")
                        .userId(100L)
                        .company(baseCompanyGenerateParam)
                        .workdate(LocalDate.now())
                        .build());
        User userAnton =
                testUserHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                        .firstName("Антон")
                        .secondName("Антонов")
                        .email("anton@mail.ru")
                        .userId(200L)
                        .company(baseCompanyGenerateParam)
                        .workdate(LocalDate.now())
                        .build());
        User userArtem =
                testUserHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                        .firstName("Артем")
                        .secondName("Артемов")
                        .userId(300L)
                        .company(TestUserHelper.CompanyGenerateParam.builder().campaignId(2L).businessId(1L).login(
                                "loginnigol").companyName(
                                "veryGoodTestName").build())
                        .email("artem@mail.ru")
                        .workdate(LocalDate.now())
                        .build());
        List<OrderCreateTaskParams<ExcelOrderModelDto>> createdTasks = uploadAndProcess("/order/creation_task" +
                "/three_good_orders_with_optional_columns.xlsx", TestUserHelper.DBS_DELIVERY_SERVICE_ID, CAMPAIGN_ID);
        var createdTask = createdTasks.get(0);

        executor.doRealJob(null);
        var processedTasks = queryService.getTask(createdTask.getId(), ExcelV1OrderModel.class);

        var ivanFullNameBadOrder = findOrder(processedTasks.getItems(), "N14");
        var antonSurname = findOrder(processedTasks.getItems(), "N15");
        var antonEmail = findOrder(processedTasks.getItems(), "N16");
        var antonFullNameGoodOrder = findOrder(processedTasks.getItems(), "N17");
        var antonFullNameBadOrder = findOrder(processedTasks.getItems(), "N18");
        var ivanBadEmailGoodFullName = findOrder(processedTasks.getItems(), "N19");
        var artemFromOtherCompany = findOrder(processedTasks.getItems(), "N20");
        checkTaskItem(ivanFullNameBadOrder, OrderPaymentType.CARD);
        checkTaskItem(antonSurname, OrderPaymentType.CASH);
        checkTaskItem(antonEmail, OrderPaymentType.CARD);
        checkTaskItem(antonFullNameGoodOrder, OrderPaymentType.CARD);
        checkTaskItem(antonFullNameBadOrder, OrderPaymentType.CARD);
        checkTaskItem(ivanBadEmailGoodFullName, OrderPaymentType.CARD);
        checkTaskItem(artemFromOtherCompany, OrderPaymentType.CARD);
        checkOderCreateOnRightCourier(CAMPAIGN_ID + "-N14", userIvan.getId());
        checkOderCreateOnRightCourier(CAMPAIGN_ID + "-N15", userAnton.getId());
        checkOderCreateOnRightCourier(CAMPAIGN_ID + "-N16", userAnton.getId());
        checkOderCreateOnRightCourier(CAMPAIGN_ID + "-N17", userAnton.getId());
        checkOderCreateOnRightCourier(CAMPAIGN_ID + "-N18", userAnton.getId());
        checkOderCreateOnRightCourier(CAMPAIGN_ID + "-N19", userIvan.getId());
        assertThrows(TplEntityNotFoundException.class,
                () -> checkOderCreateOnRightCourier(CAMPAIGN_ID + "-N20", artemFromOtherCompany.getId()));
    }

    private List<OrderCreateTaskParams<ExcelOrderModelDto>> uploadAndProcess(String path, Long deliveryServiceId,
                                                                             Long campaignId) {
        byte[] testXlsx = getFile(path);
        when(geoClient.findFirst(anyString()).getPoint()).thenReturn("0 0");
        when(geoClient.findFirst(anyString()).getComponents()).thenReturn(List.of(
                new Component("Москва", List.of(Kind.LOCALITY)),
                new Component("Новинский бульвар", List.of(Kind.STREET)),
                new Component("8", List.of(Kind.HOUSE))
        ));

        partnerService.createFromExcel(deliveryServiceId, testXlsx, campaignId);

        return queryService.getUnprocessedTasks(EXCEL_MODEL_DTO, ExcelOrderModelDto.class);
    }

    private OrderCreateTaskItemParams<ExcelV1OrderModel> findOrder(
            List<OrderCreateTaskItemParams<ExcelV1OrderModel>> items,
            String externalOrderId
    ) {
        return items.stream()
                .filter(item -> item.getPayload().getExternalOrderId().equalsIgnoreCase(externalOrderId))
                .findFirst()
                .orElseThrow();
    }

    private void checkOderCreateOnRightCourier(String externalId, long userId) throws TplInvalidParameterException {
        transactionTemplate.execute(s -> {
            Optional<Order> optional = orderRepository.findByExternalOrderId(externalId);
            Order order = optional.orElseThrow();
            Long sortingCenterId = deliveryServiceRepository.findByIdOrThrow(order.getDeliveryServiceId())
                    .getSortingCenter().getId();
            Shift shift = shiftManager.findOrThrow(
                    order.getDelivery().getDeliveryDate(ZoneId.systemDefault()),
                    sortingCenterId
            );
            Optional<UserShift> userShift = userShiftRepository.findByShiftIdAndUserId(shift.getId(), userId);
            assertThat(userShift.isPresent()).isTrue();
            return null;
        });
    }

    private void checkTaskItem(OrderCreateTaskItemParams<ExcelV1OrderModel> item, OrderPaymentType paymentType) {
        assertThat(item.getStatus()).isEqualTo(OrderCreateTaskItemStatus.COMPLETE);
        assertThat(item.getCreatedOrderId()).isNotNull();

        transactionTemplate.execute(s -> {
            Order order = orderRepository.findByIdOrThrow(item.getCreatedOrderId());

            assertThat(order.getExternalOrderId()).isEqualTo(CAMPAIGN_ID + "-" + item.getPayload().getExternalOrderId());
            assertThat(order.getDelivery().getDeliveryAddress().getCity()).isEqualTo("Москва");
            assertThat(order.getDelivery().getDeliveryAddress().getStreet()).isEqualTo("Новинский бульвар");
            assertThat(order.getDelivery().getDeliveryAddress().getHouse()).isEqualTo("8");

            assertThat(order.getDelivery().getRecipientFio()).isEqualTo("Иванов Петр");
            assertThat(order.getDelivery().getRecipientEmail()).isEqualTo("yaafedorova@gmail.com");
            assertThat(order.getDelivery().getRecipientPhone()).isEqualTo("+78005553535");

            assertThat(order.getDimensions().getWeight().doubleValue()).isEqualTo(10.0);
            assertThat(order.getDimensions().getLength().doubleValue()).isEqualTo(1.0);
            assertThat(order.getDimensions().getWidth().doubleValue()).isEqualTo(2.0);
            assertThat(order.getDimensions().getHeight().doubleValue()).isEqualTo(3.0);

            assertThat(order.getPaymentType()).isEqualTo(paymentType);

            assertThat(order.getTotalPrice().doubleValue()).isEqualTo(400.0);
            assertThat(order.getItems()).hasSize(2);

            OrderItem firstItem = order.getItems().stream()
                    .filter(i -> i.getName().equalsIgnoreCase("Название товара"))
                    .findFirst().orElseThrow();

            OrderItem secondItem = order.getItems().stream()
                    .filter(i -> i.getName().equalsIgnoreCase("Ещё один товар"))
                    .findFirst().orElseThrow();

            assertThat(firstItem.getCount()).isEqualTo(2);
            assertThat(firstItem.getPrice().doubleValue()).isEqualTo(50.0);
            assertThat(firstItem.getSumPrice().doubleValue()).isEqualTo(100.0);

            assertThat(secondItem.getCount()).isEqualTo(1);
            assertThat(secondItem.getPrice().doubleValue()).isEqualTo(300.0);
            assertThat(secondItem.getSumPrice().doubleValue()).isEqualTo(300.0);

            return null;
        });
    }

    @Test
    void uploadAndProcessWithErrors() {
        var createdTasks = uploadAndProcess("/order/creation_task/one_good_and_one_bad_order.xlsx",
                DELIVERY_SERVICE_ID, CAMPAIGN_ID);
        assertThat(createdTasks).hasSize(1);
        var createdTask = createdTasks.get(0);

        executor.doRealJob(null);
        var processedTasksA1 = queryService.getTask(createdTask.getId(), ExcelV1OrderModel.class);
        assertThat(processedTasksA1.getStatus()).isEqualTo(OrderCreateTaskStatus.PROCESSING);
        assertThat(processedTasksA1.getAttemptsMade()).isEqualTo(1);

        executor.doRealJob(null);
        var processedTasksA2 = queryService.getTask(createdTask.getId(), ExcelV1OrderModel.class);
        assertThat(processedTasksA2.getStatus()).isEqualTo(OrderCreateTaskStatus.PROCESSING);
        assertThat(processedTasksA2.getAttemptsMade()).isEqualTo(2);

        executor.doRealJob(null);
        var processedTasksA3 = queryService.getTask(createdTask.getId(), ExcelV1OrderModel.class);
        assertThat(processedTasksA3.getStatus()).isEqualTo(OrderCreateTaskStatus.COMPLETE_WITH_ERRORS);
        assertThat(processedTasksA3.getAttemptsMade()).isEqualTo(3);

        executor.doRealJob(null);
        var processedTasksA4 = queryService.getTask(createdTask.getId(), ExcelV1OrderModel.class);
        assertThat(processedTasksA4.getAttemptsMade()).isEqualTo(processedTasksA3.getAttemptsMade());

        var badOrder = findOrder(processedTasksA3.getItems(), "N14");
        var goodOrder = findOrder(processedTasksA3.getItems(), "N15");

        assertThat(badOrder.getStatus()).isEqualTo(OrderCreateTaskItemStatus.FORMAT_ERROR);

        assertThat(badOrder.getErrorStacktrace()).contains("No one valid item");

        assertThat(goodOrder.getStatus()).isEqualTo(OrderCreateTaskItemStatus.COMPLETE);
    }

    @SneakyThrows
    private byte[] getFile(String name) {
        URI path = Objects.requireNonNull(getClass()
                        .getResource(name))
                .toURI();
        File file = Paths.get(path).toFile();
        return FileUtils.readFileToByteArray(file);
    }

}
