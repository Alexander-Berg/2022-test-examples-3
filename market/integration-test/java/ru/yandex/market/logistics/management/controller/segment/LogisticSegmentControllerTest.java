package ru.yandex.market.logistics.management.controller.segment;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.request;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("CRUD логистических сегментов через external API")
@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
@DatabaseSetup("/data/controller/logisticSegment/before/prepare_data_without_segments.xml")
public class LogisticSegmentControllerTest extends AbstractContextualAspectValidationTest {

    private static final String URI = "/externalApi/logistic-segments";

    private static final long SEGMENT_ID = 1;

    @Autowired
    private LogisticSegmentValidationProducer logisticSegmentValidationProducer;

    @BeforeEach
    void setup() {
        doNothing().when(logisticSegmentValidationProducer).produceTask(anyLong());
    }

    @Test
    @DisplayName("Создание логистического сегмента")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/segment_with_services_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreate() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, URI, "data/controller/logisticSegment/request/create_dto.json"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/logisticSegment/response/created.json", Option.IGNORING_EXTRA_FIELDS));
        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Создание логистического сегмента с известной связью партнеров")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/segment_with_services_and_partner_relation_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateWithPartnerRelation() throws Exception {
        mockMvc.perform(request(
            HttpMethod.POST,
            URI,
            "data/controller/logisticSegment/request/create_with_partner_relation_dto.json"
        ))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/logisticSegment/response/created.json", Option.IGNORING_EXTRA_FIELDS));
        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Обновление логистического сегмента")
    @DatabaseSetup("/data/controller/logisticSegment/before/segment_with_services.xml")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/segment_with_services_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() throws Exception {
        mockMvc.perform(request(HttpMethod.PUT, URI, "data/controller/logisticSegment/request/update_dto.json"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/logisticSegment/response/updated.json", Option.IGNORING_EXTRA_FIELDS));
        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Удаление логистического сегмента")
    @DatabaseSetup("/data/controller/logisticSegment/before/segment_with_services.xml")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/segment_with_services_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDelete() throws Exception {
        mockMvc.perform(request(HttpMethod.DELETE, URI + "/" + SEGMENT_ID))
            .andExpect(status().isOk());
    }
}
