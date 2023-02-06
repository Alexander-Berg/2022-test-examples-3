package ru.yandex.market.logistics.lom.admin;

import java.time.LocalDate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminBusinessProcessStatus;
import ru.yandex.market.logistics.lom.admin.enums.AdminOrderStatus;
import ru.yandex.market.logistics.lom.admin.enums.AdminPartnerType;
import ru.yandex.market.logistics.lom.admin.filter.AdminBusinessProcessStateSearchFailedFilterDto;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение проблемных бизнес-процессов")
@DatabaseSetup("/controller/admin/business_process_errors/before/prepare.xml")
class GetBusinessProcessStateReportDataTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск бизнес-процессов")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminBusinessProcessStateSearchFailedFilterDto filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/business-processes/order-segment-errors").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminBusinessProcessStateSearchFailedFilterDto(),
                "controller/admin/business_process_errors/response/all.json"
            ),
            Arguments.of(
                "По идентификатору заказа",
                new AdminBusinessProcessStateSearchFailedFilterDto().setOrderId(1L),
                "controller/admin/business_process_errors/response/set_1.json"
            ),
            Arguments.of(
                "По идентификатору заказа в магазине",
                new AdminBusinessProcessStateSearchFailedFilterDto().setExternalOrderId("777"),
                "controller/admin/business_process_errors/response/set_1.json"
            ),
            Arguments.of(
                "По идентификатору отгрузки",
                new AdminBusinessProcessStateSearchFailedFilterDto().setWaybillSegmentId(3L),
                "controller/admin/business_process_errors/response/set_3.json"
            ),
            Arguments.of(
                "По Id службы партнера",
                new AdminBusinessProcessStateSearchFailedFilterDto().setPartnerId(48L),
                "controller/admin/business_process_errors/response/waybill_segment_2.json"
            ),
            Arguments.of(
                "По типу партнера",
                new AdminBusinessProcessStateSearchFailedFilterDto().setPartnerType(AdminPartnerType.DELIVERY),
                "controller/admin/business_process_errors/response/waybill_segment_2.json"
            ),
            Arguments.of(
                "Дата/время создания",
                new AdminBusinessProcessStateSearchFailedFilterDto()
                    .setCreated(LocalDate.of(2019, 11, 5)),
                "controller/admin/business_process_errors/response/set_2.json"
            ),
            Arguments.of(
                "Дата/время обновления",
                new AdminBusinessProcessStateSearchFailedFilterDto()
                    .setUpdated(LocalDate.of(2019, 11, 5)),
                "controller/admin/business_process_errors/response/set_2.json"
            ),
            Arguments.of(
                "Дата отгрузки",
                new AdminBusinessProcessStateSearchFailedFilterDto()
                    .setShipmentDate(LocalDate.of(2019, 11, 5)),
                "controller/admin/business_process_errors/response/with_shipment_date.json"
            ),
            Arguments.of(
                "По типу очереди",
                new AdminBusinessProcessStateSearchFailedFilterDto().setQueueType(QueueType.CREATE_ORDER_EXTERNAL),
                "controller/admin/business_process_errors/response/set_4.json"
            ),
            Arguments.of(
                "По статусу бизнес процесса",
                new AdminBusinessProcessStateSearchFailedFilterDto()
                    .setBusinessProcessStatus(AdminBusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED),
                "controller/admin/business_process_errors/response/set_4.json"
            ),
            Arguments.of(
                "По статусу заказа",
                new AdminBusinessProcessStateSearchFailedFilterDto().setOrderStatus(AdminOrderStatus.PROCESSING_ERROR),
                "controller/admin/business_process_errors/response/set_3.json"
            ),
            Arguments.of(
                "По всем параметрам",
                new AdminBusinessProcessStateSearchFailedFilterDto()
                    .setOrderId(1L)
                    .setExternalOrderId("777")
                    .setWaybillSegmentId(2L)
                    .setPartnerId(48L)
                    .setPartnerType(AdminPartnerType.DELIVERY)
                    .setQueueType(QueueType.CREATE_ORDER_EXTERNAL)
                    .setBusinessProcessStatus(AdminBusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED)
                    .setOrderStatus(AdminOrderStatus.PROCESSING),
                "controller/admin/business_process_errors/response/set_2.json"
            )
        );
    }

}
