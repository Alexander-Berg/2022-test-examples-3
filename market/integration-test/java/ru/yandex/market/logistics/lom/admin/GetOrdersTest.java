package ru.yandex.market.logistics.lom.admin;

import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminOrderStatus;
import ru.yandex.market.logistics.lom.admin.enums.AdminPlatformClient;
import ru.yandex.market.logistics.lom.admin.filter.AdminOrderSearchFilterDto;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentType;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.lom.utils.LmsFactory.createPartnerResponse;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение списка заказов")
@DatabaseSetup("/controller/admin/order/before/orders.xml")
class GetOrdersTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Поиск заказов по идентификатору несуществующего партнера")
    void searchByUnknownPartner() throws Exception {
        mockLmsClient();
        mockMvc.perform(get("/admin/orders").params(TestUtils.toParamWithCollections(
            new AdminOrderSearchFilterDto().setPartners(Set.of(100L)))
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/response/empty.json"));
    }

    @JpaQueriesCount(4)
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({
        "searchArgument",
        "shipmentArguments",
        "deliveryDateArguments",
        "partnersArguments",
        "platformClientArguments",
        "fullFilterArguments",
    })
    @DisplayName("Поиск заказов")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminOrderSearchFilterDto filter,
        String responsePath
    ) throws Exception {
        mockLmsClient();

        mockMvc.perform(get("/admin/orders").params(TestUtils.toParamWithCollections(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminOrderSearchFilterDto(),
                "controller/admin/order/response/all.json"
            ),
            Arguments.of(
                "По статусу",
                new AdminOrderSearchFilterDto().setStatus(AdminOrderStatus.PROCESSING),
                "controller/admin/order/response/id_2_and_3.json"
            ),
            Arguments.of(
                "По дате создания от (включительно)",
                new AdminOrderSearchFilterDto().setCreatedFrom(LocalDate.of(2019, Month.JUNE, 2)),
                "controller/admin/order/response/id_4_5_6_and_7.json"
            ),
            Arguments.of(
                "По дате создания до (включительно)",
                new AdminOrderSearchFilterDto().setCreatedTo(LocalDate.of(2019, Month.JUNE, 1)),
                "controller/admin/order/response/id_1_2_and_3.json"
            ),
            Arguments.of(
                "По дате интервалу дат создания",
                new AdminOrderSearchFilterDto()
                    .setCreatedFrom(LocalDate.of(2019, Month.JUNE, 1))
                    .setCreatedTo(LocalDate.of(2019, Month.JUNE, 1)),
                "controller/admin/order/response/id_2_and_3.json"
            ),
            Arguments.of(
                "По идентификатору заказа",
                new AdminOrderSearchFilterDto().setOrderId(4L),
                "controller/admin/order/response/id_4.json"
            ),
            Arguments.of(
                "По идентификатору сендера",
                new AdminOrderSearchFilterDto().setSenderId(1L),
                "controller/admin/order/response/id_1_and_4.json"
            ),
            Arguments.of(
                "По признаку создания в последней миле",
                new AdminOrderSearchFilterDto().setCreatedAtLastMile(true),
                "controller/admin/order/response/id_1_2_3_and_7.json"
            ),
            Arguments.of(
                "По идентификатору заказа в системе магазина",
                new AdminOrderSearchFilterDto().setExternalId("  \t 1002    \n "),
                "controller/admin/order/response/id_2.json"
            ),
            Arguments.of(
                "По типу доставки",
                new AdminOrderSearchFilterDto().setDeliveryType(DeliveryType.COURIER),
                "controller/admin/order/response/id_1_2_and_3.json"
            ),
            Arguments.of(
                "По типу отгрузки",
                new AdminOrderSearchFilterDto().setShipmentType(ShipmentType.IMPORT),
                "controller/admin/order/response/id_2_and_7.json"
            ),
            Arguments.of(
                "По дате отгрузки",
                new AdminOrderSearchFilterDto().setShipmentDate(LocalDate.of(2019, Month.JULY, 4)),
                "controller/admin/order/response/id_4.json"
            ),
            Arguments.of(
                "По идентификатору заказа в СД",
                new AdminOrderSearchFilterDto().setDeliveryServiceExternalId("  \t order_2_from_partner_id_2   \n"),
                "controller/admin/order/response/id_2.json"
            ),
            Arguments.of(
                "По идентификатору заказа в СЦ",
                new AdminOrderSearchFilterDto().setSortingCenterExternalId("   \torder_6_from_partner_id_9\n\r\n  "),
                "controller/admin/order/response/id_6.json"
            ),
            Arguments.of(
                "По идентификатору места товара из заказа",
                new AdminOrderSearchFilterDto().setUnits("place-1"),
                "controller/admin/order/response/id_1.json"
            ),
            Arguments.of(
                "По штрихкоду",
                new AdminOrderSearchFilterDto().setBarcode("2-LOtesting-1002"),
                "controller/admin/order/response/id_2.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> fullFilterArguments() {
        return Stream.of(
            Arguments.of(
                "По всем полям",
                new AdminOrderSearchFilterDto()
                    .setOrderId(2L)
                    .setExternalId("1002")
                    .setBarcode("2-LOtesting-1002")
                    .setCreatedFrom(LocalDate.of(2019, Month.JUNE, 1))
                    .setCreatedTo(LocalDate.of(2019, Month.JUNE, 1))
                    .setCreatedAtLastMile(true)
                    .setStatus(AdminOrderStatus.PROCESSING)
                    .setPartners(Set.of(2L, 5L))
                    .setSortingCenterExternalId("order_2_from_partner_id_5")
                    .setDeliveryServiceExternalId("order_2_from_partner_id_2")
                    .setDeliveryType(DeliveryType.COURIER)
                    .setShipmentId(2L)
                    .setShipmentType(ShipmentType.IMPORT)
                    .setShipmentDate(LocalDate.of(2019, Month.JULY, 2))
                    .setDeliveryDateMin(LocalDate.of(2019, Month.JUNE, 6))
                    .setDeliveryDateMax(LocalDate.of(2019, Month.JUNE, 6))
                    .setSenderId(2L),
                "controller/admin/order/response/id_2.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> shipmentArguments() {
        return Stream.of(
            Arguments.of(
                "По идентификатору отгрузки",
                new AdminOrderSearchFilterDto().setShipmentId(5L),
                "controller/admin/order/response/id_6.json"
            ),
            Arguments.of(
                "По идентификатору отгрузки не из первого сегмента",
                new AdminOrderSearchFilterDto().setShipmentId(7L),
                "controller/admin/order/response/id_7.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> platformClientArguments() {
        return Stream.of(
            Arguments.of(
                "По платформенному клиенту",
                new AdminOrderSearchFilterDto().setPlatformClient(AdminPlatformClient.BERU),
                "controller/admin/order/response/id_7.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> partnersArguments() {
        return Stream.of(
            Arguments.of(
                "По одному идентификатору партнера",
                new AdminOrderSearchFilterDto().setPartners(Set.of(2L)),
                "controller/admin/order/response/id_1_and_2.json"
            ),
            Arguments.of(
                "По нескольким идентификаторам партнера",
                new AdminOrderSearchFilterDto().setPartners(Set.of(2L, 5L)),
                "controller/admin/order/response/id_2.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> deliveryDateArguments() {
        return Stream.of(
            Arguments.of(
                "По дате доставки (от и до)",
                new AdminOrderSearchFilterDto()
                    .setDeliveryDateMin(LocalDate.of(2019, Month.JUNE, 7))
                    .setDeliveryDateMax(LocalDate.of(2019, Month.JUNE, 8)),
                "controller/admin/order/response/id_7.json"
            ),
            Arguments.of(
                "По дате доставки (от)",
                new AdminOrderSearchFilterDto()
                    .setDeliveryDateMin(LocalDate.of(2019, Month.JUNE, 7)),
                "controller/admin/order/response/id_7.json"
            ),
            Arguments.of(
                "По дате доставки (до)",
                new AdminOrderSearchFilterDto()
                    .setDeliveryDateMax(LocalDate.of(2019, Month.JUNE, 8)),
                "controller/admin/order/response/id_7.json"
            )
        );
    }

    @Test
    @DisplayName("Поиск несуществующего заказа")
    void searchNonExistOrder() throws Exception {
        mockMvc.perform(get("/admin/orders")
            .params(toParams(
                new AdminOrderSearchFilterDto()
                    .setCreatedFrom(LocalDate.of(1970, Month.JANUARY, 1))
                    .setCreatedTo(LocalDate.of(1970, Month.JANUARY, 1))
                )
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/response/empty.json"));
    }

    @Test
    @DisplayName("Поиск с указанием размера страницы")
    void pageSize() throws Exception {
        mockLmsClient();

        mockMvc.perform(get("/admin/orders").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/response/id_7.json"));
    }

    private void mockLmsClient() {
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class)))
            .thenAnswer(
                invocation -> LongStream.range(1, 10).boxed()
                    .map(id -> createPartnerResponse(id, "partner_" + id))
                    .collect(Collectors.toList())
            );
    }
}
