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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminOrderStatus;
import ru.yandex.market.logistics.lom.admin.filter.AdminOrderSearchFilterDto;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentType;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.utils.LmsFactory.createPartnerResponse;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Скачивание списка заказов")
@DatabaseSetup("/controller/admin/order/before/orders.xml")
public class DownloadOrdersTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Скачивание заказов")
    void downloadOrdersFile(
        @SuppressWarnings("unused") String displayName,
        AdminOrderSearchFilterDto filter,
        String responsePath
    ) throws Exception {
        mockLmsClient();
        mockMvc.perform(get("/admin/orders/download/orders-file-csv").params(TestUtils.toParamWithCollections(filter)))
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent(responsePath)));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminOrderSearchFilterDto(),
                "controller/admin/order/download/orders_all.csv"
            ),
            Arguments.of(
                "По идентификатору партнера",
                new AdminOrderSearchFilterDto().setPartners(Set.of(2L)),
                "controller/admin/order/download/orders_id_1_2.csv"
            ),
            Arguments.of(
                "По нескольким идентификаторам партнера",
                new AdminOrderSearchFilterDto().setPartners(Set.of(2L, 5L)),
                "controller/admin/order/download/orders_id_2.csv"
            ),
            Arguments.of(
                "По идентификатору несуществующего партнера",
                new AdminOrderSearchFilterDto().setPartners(Set.of(100L)),
                "controller/admin/order/download/orders_empty.csv"
            ),
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
                "controller/admin/order/download/orders_id_2.csv"
            )
        );
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
