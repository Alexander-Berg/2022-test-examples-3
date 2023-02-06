package ru.yandex.market.logistics.management.controller.combinator;

import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.emptyJsonList;
import static ru.yandex.market.logistics.management.util.TestUtil.hasResolvedExceptionContainingMessage;
import static ru.yandex.market.logistics.management.util.TestUtil.request;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("Получение расписания заборов")
class InboundServiceScheduleTest extends AbstractContextualTest {
    private static final String NOT_NULL_ERROR_FORMAT = "NotNull.logisticSegmentInboundScheduleFilter.%s";

    @Test
    @DisplayName("Пустое расписание")
    void getEmptySchedule() throws Exception {
        mockMvc.perform(request(HttpMethod.PUT, "/logistic-segments/search/schedule/inbound", getDefaultFilter()))
            .andExpect(status().isOk())
            .andExpect(content().json(emptyJsonList()));
    }

    @Test
    @DatabaseSetup({
        "/data/service/combinator/db/before/platform_client.xml",
        "/data/service/combinator/db/before/warehouses.xml",
        "/data/service/combinator/db/before/service_codes.xml",
        "/data/service/combinator/db/before/partner_relations.xml",
        "/data/service/combinator/db/before/partner_relations_segments.xml"
    })
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_relation_inbound_schedule.xml",
        type = DatabaseOperation.INSERT
    )
//    тест завязан на id, которые генерируются в зависимости от порядка вставки в исходном xml - хрупкий тест
    @DisplayName("Успешное получение расписания")
    void getSchedule() throws Exception {
        mockMvc.perform(request(HttpMethod.PUT, "/logistic-segments/search/schedule/inbound", getDefaultFilter()))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/combinator/response/inbound_schedule.json"));
    }

    @Test
    @DatabaseSetup({
        "/data/service/combinator/db/before/platform_client.xml",
        "/data/service/combinator/db/before/warehouses.xml",
        "/data/service/combinator/db/before/service_codes.xml",
        "/data/service/combinator/db/before/partner_relations.xml",
        "/data/service/combinator/db/before/partner_relations_segments.xml"
    })
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_relation_inbound_schedule_with_delivery_type.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Указан тип доставки")
    void getScheduleWithDeliveryType() throws Exception {
        mockMvc.perform(request(
            HttpMethod.PUT, "/logistic-segments/search/schedule/inbound",
            getDefaultFilter().setDeliveryType(DeliveryType.COURIER)
        ))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/combinator/response/inbound_schedule.json"));
    }

    @Test
    @DatabaseSetup({
        "/data/service/combinator/db/before/platform_client.xml",
        "/data/service/combinator/db/before/warehouses.xml",
        "/data/service/combinator/db/before/service_codes.xml",
        "/data/service/combinator/db/before/partner_relations.xml",
        "/data/service/combinator/db/before/partner_relations_segments.xml"
    })
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_relation_inbound_schedule_is_null.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Расписания нет")
    void scheduleIsNull() throws Exception {
        mockMvc.perform(request(HttpMethod.PUT, "/logistic-segments/search/schedule/inbound", getDefaultFilter()))
            .andExpect(status().isOk())
            .andExpect(content().json(emptyJsonList()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidArguments")
    @DisplayName("Некорректный фильтр")
    void getScheduleInvalidRequest(
        String displayName,
        LogisticSegmentInboundScheduleFilter filter,
        String errorMessage
    ) throws Exception {
        mockMvc.perform(request(HttpMethod.PUT, "/logistic-segments/search/schedule/inbound", filter))
            .andExpect(status().isBadRequest())
            .andExpect(hasResolvedExceptionContainingMessage(errorMessage));
    }

    private static Stream<Arguments> invalidArguments() {
        return Stream.of(
            Arguments.of(
                "toPartnerId - null",
                getDefaultFilter().setToPartnerId(null),
                getNotNullErrorMessage("toPartnerId")
            ),
            Arguments.of(
                "fromPartnerId - null",
                getDefaultFilter().setFromPartnerId(null),
                getNotNullErrorMessage("fromPartnerId")
            )
        );
    }

    private static String getNotNullErrorMessage(String fieldName) {
        return String.format(NOT_NULL_ERROR_FORMAT, fieldName);
    }

    private static LogisticSegmentInboundScheduleFilter getDefaultFilter() {
        return new LogisticSegmentInboundScheduleFilter().setFromPartnerId(1L).setToPartnerId(3000L);
    }
}
