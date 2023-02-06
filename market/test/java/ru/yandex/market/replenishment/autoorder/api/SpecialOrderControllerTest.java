package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deepmind.client.ApiException;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.DemandStatus;
import ru.yandex.market.replenishment.autoorder.model.SpecialOrderDateType;
import ru.yandex.market.replenishment.autoorder.model.SupplyRouteType;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Environment;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrder;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.repository.postgres.EnvironmentRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SpecialOrderItemRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.WarehouseAvailabilityValidationService;
import ru.yandex.market.replenishment.autoorder.service.excel.core.reader.BaseExcelReader;
import ru.yandex.market.replenishment.autoorder.service.special_order.SpecialOrderFillingAndValidationService;
import ru.yandex.market.replenishment.autoorder.service.special_order.SpecialOrderService;
import ru.yandex.market.replenishment.autoorder.utils.Errors;
import ru.yandex.market.replenishment.autoorder.utils.OsChecker;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockBean(WarehouseAvailabilityValidationService.class)
@WithMockLogin
public class SpecialOrderControllerTest extends ControllerTest {
    private static final String[] HEADERS_EXPECTED = {
        "Склад",
        "id заявки",
        "Поставщик (информационное поле)",
        "SSKU",
        "Наименование (информационное поле)",
        "Тип",
        "Цена с НДС за единицу",
        "Квант, шт",
        "Дата заказа",
        "Счет",
        "Тип поставки",
        "w38\n14/09/2020",
        "w39\n21/09/2020",
        "w40\n28/09/2020",
        "w41\n05/10/2020",
        "w42\n12/10/2020",
        "w43\n19/10/2020",
        "w44\n26/10/2020",
        "w45\n02/11/2020",
        "w46\n09/11/2020",
        "w47\n16/11/2020",
        "w48\n23/11/2020",
        "w49\n30/11/2020",
        "w50\n07/12/2020",
        "w51\n14/12/2020",
        "w52\n21/12/2020",
        "w53\n28/12/2020",
        "w1\n04/01/2021",
        "w2\n11/01/2021",
        "w3\n18/01/2021",
        "w4\n25/01/2021",
        "w5\n01/02/2021",
        "w6\n08/02/2021",
        "w7\n15/02/2021",
        "w8\n22/02/2021",
        "w9\n01/03/2021",
        "w10\n08/03/2021",
        "w11\n15/03/2021",
        "w12\n22/03/2021",
        "w13\n29/03/2021",
        "w14\n05/04/2021",
        "w15\n12/04/2021",
        "w16\n19/04/2021",
        "w17\n26/04/2021",
        "w18\n03/05/2021",
        "w19\n10/05/2021",
        "w20\n17/05/2021",
        "w21\n24/05/2021",
        "w22\n31/05/2021",
        "w23\n07/06/2021",
        "w24\n14/06/2021",
        "w25\n21/06/2021",
        "w26\n28/06/2021",
        "w27\n05/07/2021",
        "w28\n12/07/2021",
        "w29\n19/07/2021",
        "w30\n26/07/2021",
        "w31\n02/08/2021",
        "w32\n09/08/2021",
        "w33\n16/08/2021",
        "w34\n23/08/2021",
        "w35\n30/08/2021"
    };

    @Autowired
    SpecialOrderService specialOrderService;

    @Autowired
    SpecialOrderFillingAndValidationService specialOrderFillingAndValidationService;

    @Autowired
    WarehouseAvailabilityValidationService warehouseAvailabilityValidationService;

    @Autowired
    EnvironmentRepository environmentRepository;

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @Autowired
    @Qualifier("deepmindHttpClient")
    CloseableHttpClient httpClient;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    @Before
    public void clearUsersCacheManger() {
        if (OsChecker.getOsType().equalsIgnoreCase("linux")) {
            if (environmentRepository.findById(Environment.DISABLE_DROPDOWNS_IN_SO_EXCEL_WRITER).isEmpty()) {
                Environment param = new Environment();
                param.setName(Environment.DISABLE_DROPDOWNS_IN_SO_EXCEL_WRITER);
                environmentRepository.save(param);
            }
        }

        setTestTime(LocalDateTime.of(2020, 9, 6, 0, 0));
    }

    @Before
    public void mockWarehouseAvailabilityCheck() {
        doReturn(Collections.emptySet()).when(warehouseAvailabilityValidationService)
            .validateWarehouseAvailability(anyList());
    }

    @Before
    public void mockHttpClientForDeepmind() {
        mockDeepmindClientForSpecialOrder("ok");
    }

    /*------------------------------- BEGIN VALIDATIONS --------------------------------------------------*/
    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.hasNotRight.before.csv")
    public void testUserHasNotRightsForFastTrack() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.empty_warehouse.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("У пользователя boris нет прав на обход согласования с АК"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testEmptyWarehouse() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.empty_warehouse.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заявки на SSKU 000124.8714100866806 не указано название склада, номер строки: 3\\n" +
                    "Для заявки на SSKU 000124.8714100866806 не указано название склада, номер строки: 2"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testNotMonday() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.not_monday.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("В файле неверно указаны отгрузки заказов: дата 25/08/2020 не понедельник, номер колонки: 12"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testNonEmptyId() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.non_empty_id.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Не найдены спецзаказы с идентификаторами: 1, 2. " +
                    "Если вы хотите создать эти заказы, колонка идентификатора должна быть пустой."));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testEmptySsku() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.empty_ssku.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("В файле должны быть указаны SSKU, номер строки: 3\\n" +
                    "В файле должны быть указаны SSKU, номер строки: 2"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testWrongSsku() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.wrong_ssku.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("SSKU 000124.8714100868086 не был найден в базе, номер строки: 2\\n" +
                    "SSKU 000124.8714100868086 не был найден в базе, номер строки: 3"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testWrongType() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.wrong_type.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заявки на SSKU 000124.8714100866806 неверно указан тип, валидные значения: [лот нов сез " +
                    "вал доп.об промо перекуп], номер строки: 3\\n" +
                    "Для заявки на SSKU 000124.8714100866806 неверно указан тип, валидные значения: [лот нов сез вал " +
                    "доп.об промо перекуп], номер строки: 2"
                )
            );
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testNegativePrice() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.wrong_price.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Цена товара с SSKU 000124.8714100866806 не может быть отрицательной, номер строки: 3\\n" +
                    "Цена товара с SSKU 000124.8714100866806 не может быть отрицательной, номер строки: 2"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testWrongShipmentQuantum() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.wrong_quantum.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заявки на SSKU 000124.8714100866806 неверно указан квант, номер строки: 3\\n" +
                    "Для заявки на SSKU 000124.8714100866806 неверно указан квант, номер строки: 2"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testEmptyWeekHeader() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.empty_week_header.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("В файле должны быть указаны недели отгрузки заказов"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testMissingWeekSeparator() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.missing_week_separator.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("В файле неверно указаны отгрузки заказов: не найдено свободное пространство после " +
                    "идентификатора недели, номер колонки: 12"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testWrongWeekNumber() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.wrong_week_number.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("В файле неверно указаны отгрузки заказов: номер недели (69) не совпадает с реальным " +
                    "(35), номер колонки: 12"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testDuplicateWeeks() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.duplicate_weeks.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заказа (SSKU '000124.8714100866806', склад 'Маршрут') есть пересечение по неделям " +
                    "2020-08-24, номер строки: 3"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.duplicate_weeks_db.before.csv")
    public void testDuplicateWeeksFromDB() throws Exception {
        setTestTime(LocalDate.of(2020, 8, 28));
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.duplicate_weeks_db.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для MSKU 1337 (SSKU 000124.667711) нет ответственного, номер строки: 3\\n" +
                    "Для MSKU 1337420 (SSKU 000124.667722) нет ответственного, номер строки: 2\\n" +
                    "Для заказа (SSKU '000124.667722', склад '145') есть пересечение по неделям 24/08/2020, номер " +
                    "строки: 2"));
    }

    // TODO: add successful for current week test here

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testFractionalItemQuantity() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.fractional_item_quantity.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заявки на SSKU 000124.8714100866806 указано некорректное значение количества `5000.3`, " +
                    "номер строки: 2"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testNegativeItemQuantity() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.negative_item_quantity.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заявки на SSKU 000124.8714100866806 указано " +
                    "отрицательное количество, номер строки: 2"
                )
            );
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testWrongItemQuantity() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.wrong_item_quantity.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заявки на SSKU 000124.8714100866806 для недели №37 количество отгрузки (5001)"
                    + " не кратно кванту (100), номер строки: 2\\n"
                    + "Для заявки на SSKU 000124.8714100866806 для недели №38 количество отгрузки (111)"
                    + " не кратно кванту (100), номер строки: 2"
                )
            );
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testEmptyItems() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.empty_items.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заявки на SSKU 000124.8714100866806 " +
                    "не указано ни одной недели отгрузки, номер строки: 2")
            );
    }
    /*------------------------------- END VALIDATIONS --------------------------------------------------*/

    /*------------------------------- BEGIN ADMIN --------------------------------------------------*/
    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testAddSpecialOrder403() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/user/feedor/special-order/excel",
                "SpecialOrderControllerTest.simple_positive.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message")
                .value("Пользователь 'boris' не владеет ролью 'ROLE_SPECIAL_ORDERS_ADMIN'"));
    }

    @Test
    @WithMockLogin("special-orders-admin")
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get_for_table.before.csv")
    public void testGetSpecialOrdersForTableAdminSentDemand() throws Exception {
        setTestTime(LocalDate.of(2020, 8, 20));

        mockMvc.perform(get("/api/v1/user/boris/special-order?page=1&count=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.specialOrders", hasSize(7)))
            .andExpect(jsonPath("$.countAll").value(7L))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].adjustedPurchQuantity").value(4))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2 && @.week=='2020-09-14')].ticketId")
                .value("ticket1"));
    }

    @Test
    @WithMockLogin("special-orders-admin")
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testUserDoesntExist() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/user/not-exists/special-order/excel",
                "SpecialOrderControllerTest.simple_positive.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Пользователь 'not-exists' не существует"));
    }

    @Test
    @WithMockLogin("special-orders-admin")
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.simple.after_positive.csv")
    public void testAddSpecialOrderAdmin() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/user/boris/special-order/excel",
                "SpecialOrderControllerTest.simple_positive.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("special-orders-admin")
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple_no_autoorder.before.csv",
        after = "SpecialOrderControllerTest.simple_no_autoorder.after_positive.csv")
    public void testAddSpecialOrderNoAutoorder() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/user/boris/special-order/excel",
                "SpecialOrderControllerTest.simple_positive.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get.before.csv")
    public void testGetAdmin403() throws Exception {
        mockMvc.perform(get("/api/v1/user/boris/special-order/excel"))
            .andExpect(status().isForbidden());
    }

    /*------------------------------- END ADMIN --------------------------------------------------*/

    /*------------------------------- BEGIN CURRENT-USER --------------------------------------------------*/
    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get_for_table.before.csv")
    public void testGetSpecialOrdersForTableSentDemand() throws Exception {
        setTestTime(LocalDate.of(2020, 8, 20));

        mockMvc.perform(get("/api/v1/current-user/special-order?page=1&count=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.specialOrders", hasSize(7)))
            .andExpect(jsonPath("$.countAll").value(7L))

            .andExpect(jsonPath("$.specialOrders[?(@.id==1)]").isNotEmpty())
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].ssku").value("000124.8714100866806"))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].title").value("шапка"))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].purchQuantity").value(5000))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].adjustedPurchQuantity").value(4))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].week").value("2020-08-31"))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].orderDate").value("2020-08-20"))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].deliveryDate").value("2020-08-23"))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].demandId").value(1))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].status").value(DemandStatus.ORDER_CREATED.toString()))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].responsible").value("notboris"))
            .andExpect(jsonPath("$.specialOrders[?(@.id==1)].account", everyItem(nullValue())));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get_for_table.before.csv")
    public void testGetSpecialOrdersForTableDateFrom() throws Exception {
        setTestTime(LocalDate.of(2020, 8, 20));

        String urlString = String.format(
            "/api/v1/current-user/special-order?page=1&count=10&dateFrom=%s&dateTo=%s",
            LocalDate.of(2020, 8, 25).format(DateTimeFormatter.ISO_DATE),
            LocalDate.of(2020, 9, 2).format(DateTimeFormatter.ISO_DATE)
        );

        mockMvc.perform(get(urlString))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.specialOrders", hasSize(5)))
            .andExpect(jsonPath("$.countAll").value(5L))

            .andExpect(jsonPath("$.specialOrders[?(@.id==2)]").isNotEmpty())
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].orderDate", everyItem(is("2020-09-01"))))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].demandId", everyItem(is(15))));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get_for_table.before.csv")
    public void testGetSpecialOrdersForTableNotSentDemand() throws Exception {
        setTestTime(LocalDate.of(2020, 8, 20));

        mockMvc.perform(get("/api/v1/current-user/special-order?page=1&count=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.specialOrders", hasSize(7)))
            .andExpect(jsonPath("$.countAll").value(7L))

            .andExpect(jsonPath("$.specialOrders[?(@.id==2)]").isNotEmpty())
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].ssku", everyItem(is("000124.8714100866807"))))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].title", everyItem(is("кепка"))))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].purchQuantity", containsInAnyOrder(100, 500, 1000,
                1000, 900)))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].adjustedPurchQuantity", everyItem(nullValue())))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].week",
                containsInAnyOrder("2020-09-14", "2020-09-21", "2020-09-21", "2020-09-28", "2020-10-05")))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].orderDate", everyItem(is("2020-09-01"))))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].deliveryDate", everyItem(is("2020-09-12"))))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].demandId", everyItem(is(15))))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].status",
                everyItem(is(DemandStatus.ORDER_CREATED.toString()))))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].responsible", everyItem(is("notboris"))))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].account", hasItems("account", "account1",
                "account2")));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get_for_table.before.csv")
    public void testGetSpecialOrdersForTableWithPagination() throws Exception {
        setTestTime(LocalDate.of(2020, 8, 20));

        mockMvc.perform(get("/api/v1/current-user/special-order?page=3&count=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.specialOrders", hasSize(1)))
            .andExpect(jsonPath("$.countAll").value(7L))

            .andExpect(jsonPath("$.specialOrders[?(@.id==2)]").isNotEmpty())
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].ssku").value("000124.8714100866807"))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].title").value("кепка"))
            .andExpect(jsonPath("$.specialOrders[?(@.id==2)].week").value("2020-09-21"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get_for_table.before.csv")
    public void testGetSpecialOrdersForTableWithPagination_inRightOrder() throws Exception {
        setTestTime(LocalDate.of(2020, 8, 20));

        mockMvc.perform(get("/api/v1/current-user/special-order?page=1&count=200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.specialOrders", hasSize(7)))
            .andExpect(jsonPath("$.countAll").value(7L))

            .andExpect(jsonPath("$.specialOrders[0].id").value("1"))
            .andExpect(jsonPath("$.specialOrders[0].purchQuantity").value(5000))

            .andExpect(jsonPath("$.specialOrders[1].id").value("2"))
            .andExpect(jsonPath("$.specialOrders[1].purchQuantity").value(100))
            .andExpect(jsonPath("$.specialOrders[2].id").value("2"))
            .andExpect(jsonPath("$.specialOrders[2].purchQuantity").value(500))
            .andExpect(jsonPath("$.specialOrders[3].id").value("2"))
            .andExpect(jsonPath("$.specialOrders[3].purchQuantity").value(1000))
            .andExpect(jsonPath("$.specialOrders[3].account").value("account2"))
            .andExpect(jsonPath("$.specialOrders[4].id").value("2"))
            .andExpect(jsonPath("$.specialOrders[4].purchQuantity").value(1000))
            .andExpect(jsonPath("$.specialOrders[4].account").isEmpty())
            .andExpect(jsonPath("$.specialOrders[5].id").value("2"))
            .andExpect(jsonPath("$.specialOrders[5].purchQuantity").value(900))

            .andExpect(jsonPath("$.specialOrders[6].id").value("3"))
        ;
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.simple.after_positive.csv")
    public void testAddSpecialOrdersPositive() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.simple_positive.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.simple.slow_track.csv")
    public void testAddSpecialOrdersSlowTrack_isOk() throws Exception {
        mockDeepmindClientForSpecialOrder("{\"ticketName\": \"ticket1\"}");
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.simple_positive_slow.xlsx"
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$").value("ticket1"));
    }

    private void mockDeepmindClientForSpecialOrder(String result) {
        var response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(
            new NStringEntity(result, ContentType.APPLICATION_JSON));
        when(response.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("https", 0, 0), HttpStatus.SC_OK, "fake reason")
        );
        try {
            when(httpClient.execute(any())).thenReturn(response);
        } catch (IOException ignored) {
        }
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.simple.after_positive_null_quantity.csv")
    public void testAddSpecialOrdersPositiveNullQuantity() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.simple_positive_null_quantity.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get.before.csv")
    public void testGetExcel() throws Exception {
        setTestTime(LocalDateTime.of(2020, 9, 20, 0, 0));

        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/special-order/excel"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertGetResult(excelData);
    }

    private void assertGetResult(byte[] excelData) throws IOException {
        List<List<Object>> lists = BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelData),
            HEADERS_EXPECTED.length
        );

        assertEquals(6, lists.size());
        assertThat(lists.get(0), is(Arrays.asList(HEADERS_EXPECTED)));

        lists.remove(0);
        lists.sort(Comparator.comparing(row -> Double.parseDouble(String.valueOf(row.get(1)))));

        List<Object> row = lists.get(0);
        assertEquals("Маршрут", row.get(0));
        assertEquals(1.0, row.get(1));
        assertEquals("Хаскел", row.get(2));
        assertEquals("000124.8714100866806", row.get(3));
        assertEquals("шапка", row.get(4));
        assertEquals("нов", row.get(5));
        assertEquals("по лог параметрам", row.get(8));
        assertNull(row.get(9));

        row = lists.get(2);
        assertEquals("Маршрут", row.get(0));
        assertEquals(3.0, row.get(1));
        assertEquals("Хаскел", row.get(2));
        assertEquals("000124.8714100866806", row.get(3));
        assertEquals("шапка", row.get(4));
        assertEquals("сегодня", row.get(8));
        assertNull(row.get(9));

        row = lists.get(3);
        assertEquals("Маршрут", row.get(0));
        assertEquals(4.0, row.get(1));
        assertEquals("Хаскел", row.get(2));
        assertEquals("000124.8714100866806", row.get(3));
        assertEquals("шапка", row.get(4));
        assertEquals("по лог параметрам", row.get(8));
        assertEquals("account1", row.get(9));

        row = lists.get(4);
        assertEquals("Маршрут", row.get(0));
        assertEquals(5.0, row.get(1));
        assertEquals("Хаскел", row.get(2));
        assertEquals("000124.8714100866806", row.get(3));
        assertEquals("шапка", row.get(4));
        assertEquals("сегодня", row.get(8));
        assertEquals("account2", row.get(9));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.get.before.csv")
    public void testGetExample() throws Exception {
        setTestTime(LocalDateTime.of(2020, 9, 20, 0, 0));

        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/special-order/excel-example"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        List<List<Object>> lists = BaseExcelReader.extractFromExcel(new ByteArrayInputStream(excelData),
            HEADERS_EXPECTED.length);
        assertEquals(2, lists.size());
        assertThat(lists.get(0), is(Arrays.asList(HEADERS_EXPECTED)));
        List<Object> row = lists.get(1);
        assertEquals("Софьино", row.get(0));
        assertNull(row.get(1));
        assertNull(row.get(2));
        assertEquals("000017.667700", row.get(3));
        assertNull(row.get(4));
        assertEquals("нов", row.get(5));
        assertEquals(10.0, row.get(6));
        assertEquals(100.0, row.get(7));
        assertEquals(SpecialOrderDateType.LOG_PARAM.getReadableValue(), row.get(8));
        assertNull(row.get(9));
        assertEquals(SupplyRouteType.DIRECT.getDisplayName(), row.get(10));
        assertEquals(200.0, row.get(11));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testExampleUpload() throws Exception {
        setTestTime(LocalDateTime.of(2020, 9, 20, 0, 0));

        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/special-order/excel-example"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        excelTestingHelper.uploadWithHeaderAndParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "excel.xlsx",
            excelData,
            Map.of("useFastTrack", "true"),
            null
        ).andExpect(status().isOk());

        excelData = mockMvc.perform(get("/api/v1/current-user/special-order/excel"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        List<List<Object>> lists = BaseExcelReader.extractFromExcel(new ByteArrayInputStream(excelData),
            HEADERS_EXPECTED.length);

        assertEquals(2, lists.size());
        assertThat(lists.get(0), is(Arrays.asList(HEADERS_EXPECTED)));
        List<Object> row = lists.get(1);
        assertEquals("Софьино", row.get(0));
        assertEquals(1.0, row.get(1));
        assertEquals("ООО «Хаскел»", row.get(2));
        assertEquals("000017.667700", row.get(3));
        assertEquals("нов", row.get(5));
        assertEquals(10.0, row.get(6));
        assertEquals(100.0, row.get(7));
        assertEquals(SpecialOrderDateType.LOG_PARAM.getReadableValue(), row.get(8));
        assertNull(row.get(9));
        assertEquals(SupplyRouteType.DIRECT.getDisplayName(), row.get(10));
        assertEquals(200.0, row.get(11));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.with-generate.before.csv",
        after = "SpecialOrderControllerTest.with-generate.after.csv")
    public void testAddSpecialOrdersWithGenerateDemands() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.with-generate.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.with-generate.before.csv",
        after = "SpecialOrderControllerTest.with-generate.after.csv")
    public void testAddSpecialOrdersWithGenerateDemandsNoSupplyRoute() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.with-generate-no-route.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.with-generate-two-items-to-demand.before.csv",
        after = "SpecialOrderControllerTest.with-generate-two-items-to-demand.before.csv")
    public void testAddSpecialOrdersWithGenerateDemandsTwoItemsInOneWeek() throws Exception {
        setTestTime(LocalDateTime.of(2020, 10, 21, 0, 0));
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.with-generate-two-items-to-demand.xlsx",
                Map.of("useFastTrack", "true")
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заказа (SSKU '000101.1001', склад '172') уже есть СЗ на дату заказа 24/10/2020, номер " +
                    "строки: 2"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.with-generate-xdock.before.csv",
        after = "SpecialOrderControllerTest.with-generate-xdock.after.csv")
    public void testAddSpecialOrdersWithGenerateCrossDockDemands() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.with-generate-xdock.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.with-generate-mono-xdock.before.csv",
        after = "SpecialOrderControllerTest.with-generate-mono-xdock.after.csv")
    public void testAddSpecialOrdersWithGenerateMonoXDockDemands() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.with-generate-mono-xdock.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.with-generate-mono-xdock-existed.before.csv",
        after = "SpecialOrderControllerTest.with-generate-mono-xdock-existed.after.csv")
    public void testAddSpecialOrdersWithGenerateMonoXDockDemandsWithExisted() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.with-generate-mono-xdock.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.generate-with-date-and-account.before.csv",
        after = "SpecialOrderControllerTest.generate-with-date-and-account.after.csv")
    public void testAddSpecialOrdersGenerateDemandsWithDateAndAccount() throws Exception {
        setTestTime(LocalDateTime.of(2020, 10, 22, 0, 0));
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.generate-with-date-and-account.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.simple.after_null_cells.csv")
    public void testWithNullCellsExcel() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.null_cells_workbook.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update_simple.before.csv",
        after = "SpecialOrderControllerTest.update_simple.after.csv")
    public void testUpdateSimple() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.update_simple.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update-with-date-and-account.before.csv",
        after = "SpecialOrderControllerTest.update-with-date-and-account.after.csv")
    public void testUpdateWithDateAndAccount() throws Exception {
        setTestTime(LocalDateTime.of(2020, 10, 22, 0, 0));
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.update-with-date-and-account.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update_delete.before.csv",
        after = "SpecialOrderControllerTest.update_delete.after.csv")
    public void testUpdateDeleteItem() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.update_delete.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update_delete_adjusted.before.csv",
        after = "SpecialOrderControllerTest.update_delete.after.csv")
    public void testUpdateDeleteAdjustedItem() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.update_delete.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update-with-date-and-account.before.csv",
        after = "SpecialOrderControllerTest.delete-with-date-and-account.after.csv")
    public void testUpdateDeleteWithDateAndAccount() throws Exception {
        setTestTime(LocalDateTime.of(2020, 10, 22, 0, 0));
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.delete-with-date-and-account.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update.before.csv",
        after = "SpecialOrderControllerTest.update.before.csv")
    public void testUpdateSpecialOrdersWithError() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.update-with-error.xlsx",
                Map.of("useFastTrack", "true")
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заявки на SSKU 000101.1003 для недели №39 количество отгрузки (61)"
                    + " не кратно кванту (3), номер строки: 4"
                )
            );
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update.before.csv",
        after = "SpecialOrderControllerTest.update.before.csv")
    public void testUpdateSpecialOrdersWithEmptyWarehouseValidationError() throws Exception {
        Errors<SpecialOrder> errors = new Errors<>();
        errors.add(new SpecialOrder(),
            "Согласно матрице блокировок следующие SSKU нельзя возить на выбранный склад: 1001");
        doReturn(errors)
            .when(warehouseAvailabilityValidationService).validateWarehouseAvailability(
                anyList(), any(), eq(WarehouseAvailabilityValidationService.CheckType.STRONG));

        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.update-with-error1.xlsx",
                Map.of("useFastTrack", "true")
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Согласно матрице блокировок следующие SSKU нельзя возить на выбранный склад: 1001"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update.before.csv",
        after = "SpecialOrderControllerTest.update.before.csv")
    public void testUpdateSpecialOrdersWithWarehouseValidationError() throws Exception {
        Errors<SpecialOrder> errors = new Errors<>();
        errors.add(new SpecialOrder(),
            "Согласно матрице блокировок следующие SSKU нельзя возить на выбранный склад: 1001");
        doReturn(errors)
            .when(warehouseAvailabilityValidationService).validateWarehouseAvailability(
                anyList(), any(), eq(WarehouseAvailabilityValidationService.CheckType.STRONG));

        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.update-with-error2.xlsx",
                Map.of("useFastTrack", "true")
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Согласно матрице блокировок следующие SSKU нельзя возить на выбранный склад: 1001"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.simple.after_null_price.csv")
    public void testWithNullPriceExcel() throws Exception {
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.null_price.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.no_responsible.csv")
    public void testWithNoResponsibleExcel() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.no_responsible.xlsx",
                Map.of("useFastTrack", "true")
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для MSKU 107990 (SSKU 000017.667700) нет ответственного, номер строки: 2"));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update.before.csv",
        after = "SpecialOrderControllerTest.update.before.csv")
    public void testReloadLoadedOrders() throws ApiException {
        List<SpecialOrder> orders = new ArrayList<>();
        specialOrderService.findSpecialOrdersByLogin(2L, orders::add);
        specialOrderService.addSpecialOrderByFastPipe(ImmutableList.copyOf(orders), false);
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update_cur_week.before.csv",
        after = "SpecialOrderControllerTest.update_cur_week.before.csv")
    public void testReloadLoadedFileWithCurrentWeek() {
        List<SpecialOrder> orders = new ArrayList<>();
        specialOrderService.findSpecialOrdersByLogin(2L, orders::add);
        specialOrderService.addSpecialOrderByFastPipe(ImmutableList.copyOf(orders), false);
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update_past_week.before.csv")
    public void testUpdatePastWeek() {
        List<SpecialOrder> orders = new ArrayList<>();
        specialOrderService.findSpecialOrdersByLogin(2L, orders::add);

        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            SpecialOrderItemRepository orderItemRepository = sqlSession.getMapper(SpecialOrderItemRepository.class);
            SpecialOrderItem item = orderItemRepository.getSpecialOrderItemById(1001L);
            item.setQuantity(100);
            orderItemRepository.updateSpecialOrderItem(item);
            sqlSession.flushStatements();
            sqlSession.commit();
        }

        UserWarningException exception = assertThrows(
            UserWarningException.class,
            () -> specialOrderService.addSpecialOrderByFastPipe(ImmutableList.copyOf(orders), false)
        );

        assertEquals("Нельзя изменить спец заказ 1001 на прошедшую неделю", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update_cur_week.before.csv",
        after = "SpecialOrderControllerTest.update_cur_week.after.csv")
    public void testUpdateCurrentWeek() {
        List<SpecialOrder> orders = new ArrayList<>();
        specialOrderService.findSpecialOrdersByLogin(2L, orders::add);

        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            SpecialOrderItemRepository orderItemRepository = sqlSession.getMapper(SpecialOrderItemRepository.class);
            SpecialOrderItem item = orderItemRepository.getSpecialOrderItemById(1001L);
            item.setQuantity(100);
            orderItemRepository.updateSpecialOrderItem(item);
            sqlSession.commit();
        }

        assertDoesNotThrow(() ->
            specialOrderService.addSpecialOrderByFastPipe(ImmutableList.copyOf(orders), false));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.update_exported_demand.before.csv")
    public void testUpdateExportedDemand() {
        List<SpecialOrder> orders = new ArrayList<>();
        specialOrderService.findSpecialOrdersByLogin(2L, orders::add);

        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            SpecialOrderItemRepository orderItemRepository = sqlSession.getMapper(SpecialOrderItemRepository.class);
            SpecialOrderItem item = orderItemRepository.getSpecialOrderItemById(1001L);
            item.setQuantity(100);
            orderItemRepository.updateSpecialOrderItem(item);
            sqlSession.flushStatements();
            sqlSession.commit();
        }

        UserWarningException exception = assertThrows(UserWarningException.class, () ->
            specialOrderService.addSpecialOrderByFastPipe(ImmutableList.copyOf(orders), false));

        assertEquals(
            "Невозможно обновить спецзаказы: потребности с номерами 1001 уже отправлены в AX",
            exception.getMessage()
        );
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.with_date_or_account.after.csv")
    public void testAddSpecialOrdersWithDateOrAccount() throws Exception {
        setTestTime(LocalDate.of(2020, 9, 8));
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.with_date_or_account.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.with_date_or_account_auto.after.csv")
    public void testAddSpecialOrdersWithDateOrAccountToday() throws Exception {
        setTestTime(LocalDate.of(2020, 9, 8));
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.with_date_or_account.xlsx",
            Map.of("useFastTrack", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.with_date_or_account_auto_disabled.after.csv")
    public void testAddSpecialOrdersWithDateOrAccountTodayDisabled() throws Exception {
        setTestTime(LocalDate.of(2020, 9, 8));
        excelTestingHelper.uploadWithParams(
            "POST",
            "/api/v1/current-user/special-order/excel",
            "SpecialOrderControllerTest.with_date_or_account.xlsx",
            Map.of("useFastTrack", "true",
                "disableAutoProcess", "true")
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.simple.before.csv")
    public void testAddSpecialOrdersWrongSeveralWeeks() throws Exception {
        setTestTime(LocalDate.of(2020, 9, 8));
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/current-user/special-order/excel",
                "SpecialOrderControllerTest.wrong_several_weeks.xlsx",
                Map.of("useFastTrack", "true")
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value(
                    "Заявке на SSKU 000124.8714100866806 со счетом account1" +
                        " соответствует несколько недель отгрузки, номер строки: 3\\n" +
                        "Заявке на SSKU 000124.8714100866806 со счетом account2" +
                        " соответствует несколько недель отгрузки, номер строки: 4"));
    }

    /*------------------------------- END CURRENT-USER --------------------------------------------------*/

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.getSpecialOrdersIdsByMsku.before.csv")
    public void testGetSpecialOrdersIdsByMsku() throws Exception {
        mockMvc.perform(get("/api/v1/msku/152407378/special-order?warehouseId=172&date=2020-09-01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@==1)]").isNotEmpty())
            .andExpect(jsonPath("$[?(@==2)]").isNotEmpty())
            .andExpect(jsonPath("$[?(@==3)]").isNotEmpty());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderControllerTest.getSpecialOrdersIdsByMsku.before.csv")
    public void testGetSpecialOrderById() throws Exception {
        mockMvc.perform(get("/api/v1/special-order/4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))

            .andExpect(jsonPath("$[0].ssku").value("12315"))
            .andExpect(jsonPath("$[0].purchQuantity").value("100"))
            .andExpect(jsonPath("$[0].adjustedPurchQuantity").value("4"))
            .andExpect(jsonPath("$[0].week").value("2020-09-14"));
    }

    @Test
    @WithMockLogin("special-orders-admin")
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv",
        after = "SpecialOrderControllerTest.simple.after_promo.csv")
    public void testAddSpecialOrderPromo_isOkFastTrack() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/user/boris/special-order/excel",
                "SpecialOrderControllerTest.simple_promo.xlsx",
                Map.of("useFastTrack", "false")
            )
            .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("special-orders-admin")
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testAddSpecialOrderMixedPromo_isIAmATeapot() throws Exception {
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/user/boris/special-order/excel",
                "SpecialOrderControllerTest.mixed_promo.xlsx",
                Map.of("useFastTrack", "false")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("В промо спец. закупках присутствуют не промо. строки."));
    }

    @Test
    @WithMockLogin("special-orders-admin")
    @DbUnitDataSet(before = "SpecialOrderControllerTest.simple.before.csv")
    public void testAddSpecialOrderWithSimilarComputedOrderDate_isIAmATeapot() throws Exception {
        setTestTime(LocalDateTime.of(2020, 8, 27, 0, 0));
        excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v1/user/boris/special-order/excel",
                "SpecialOrderControllerTest.similarComputedOrderDate.xlsx",
                Map.of("useFastTrack", "true")
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для заказа (SSKU '000124.8714100866806', склад '145') уже есть СЗ на дату заказа 31/08/2020, " +
                    "номер строки: 2"));
    }
}
