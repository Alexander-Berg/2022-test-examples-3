package ru.yandex.market.logistics.management.controller.service;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.queue.producer.LogisticSegmentValidationProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.request;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("CRUD логистических сервисов через external API")
@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
@DatabaseSetup("/data/controller/logisticService/before/prepare_data_without_segments.xml")
public class LogisticServiceControllerTest extends AbstractContextualAspectValidationTest {

    private static final String URI = "/externalApi/logistic-services";

    private static final long SEGMENT_ID = 1;

    private static final long SERVICE_ID = 1;

    @Autowired
    private LogisticSegmentValidationProducer logisticSegmentValidationProducer;

    @BeforeEach
    void setup() {
        doNothing().when(logisticSegmentValidationProducer).produceTask(anyLong());
    }

    @Test
    @DisplayName("Создание логистического сервиса")
    @DatabaseSetup("/data/controller/logisticService/before/segment_without_services.xml")
    @ExpectedDatabase(
        value = "/data/controller/logisticService/after/segment_with_services_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreate() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, URI, "data/controller/logisticService/request/create_dto.json"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/logisticService/response/created.json", Option.IGNORING_EXTRA_FIELDS));
        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Обновление логистического сервиса")
    @DatabaseSetup("/data/controller/logisticService/before/segment_with_services.xml")
    @ExpectedDatabase(
        value = "/data/controller/logisticService/after/segment_with_services_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() throws Exception {
        mockMvc.perform(request(HttpMethod.PUT, URI, "data/controller/logisticService/request/update_dto.json"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/logisticService/response/updated.json", Option.IGNORING_EXTRA_FIELDS));
        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Удаление логистического сервиса")
    @DatabaseSetup("/data/controller/logisticService/before/segment_with_services.xml")
    @ExpectedDatabase(
        value = "/data/controller/logisticService/after/segment_with_services_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDelete() throws Exception {
        mockMvc.perform(request(HttpMethod.DELETE, URI + "/" + SERVICE_ID))
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Добавление метаинформации")
    @DatabaseSetup("/data/controller/logisticService/before/segment_with_services_and_metakey.xml")
    @ExpectedDatabase(
        value = "/data/controller/logisticService/after/segment_with_services_metainfo_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shouldAddMetainfo() throws Exception {
        mockMvc.perform(request(
                HttpMethod.PUT,
                URI + "/" + SERVICE_ID + "/metainfo" + "/" + "ROUTING_ENABLED",
                "data/controller/logisticService/request/create_meta_dto.json"
            ))
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Добавление возвратного СЦ к C2C точке, ошибка")
    @DatabaseSetup("/data/controller/logisticService/before/segment_with_services_and_metakey.xml")
    @DatabaseSetup(
        value = "/data/controller/logisticService/before/c2c_point.xml",
        type = DatabaseOperation.UPDATE
    )
    void addReturnSCToC2CPointError() throws Exception {
        mockMvc.perform(request(
                HttpMethod.PUT,
                URI + "/" + SERVICE_ID + "/metainfo" + "/" + "RETURN_SORTING_CENTER_ID",
                "data/controller/logisticService/request/create_meta_sc_id_dto.json"
            ))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Cannot add return sorting center to point available for C2C"));
        verifyNoInteractions(logisticSegmentValidationProducer);
    }

    @Test
    @DisplayName("Удаление метаинформации")
    @DatabaseSetup("/data/controller/logisticService/before/segment_with_services_with_metainfo.xml")
    @ExpectedDatabase(
        value = "/data/controller/logisticService/after/segment_with_services_metainfo_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shouldRemoveMetainfo() throws Exception {
        mockMvc.perform(request(
                HttpMethod.DELETE,
                URI + "/" + SERVICE_ID + "/metainfo" + "/" + "ROUTING_ENABLED"
            ))
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }
}
