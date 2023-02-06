package ru.yandex.market.logistics.iris.controller;

import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.model.DimensionsDTO;
import ru.yandex.market.logistics.iris.service.audit.measurement.MeasurementEventAuditService;
import ru.yandex.market.logistics.iris.service.logbroker.producer.LogBrokerPushService;
import ru.yandex.market.logistics.iris.service.mdm.publish.measurement.MeasurementPublishServiceImpl;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class  PushApiControllerTest extends AbstractContextualTest {

    @MockBean
    private LogBrokerPushService logBrokerPushService;

    @SpyBean
    private MeasurementEventAuditService auditService;

    @SpyBean
    private MeasurementPublishServiceImpl publishService;

    @Captor
    private ArgumentCaptor<Map<ItemIdentifier, DimensionsDTO>> dimensionsCaptor;

    /**
     * Тест на успешный пуш 2-х айтемов:
     * - обновление лайфтайма sku_1
     * - создание нового sku_2 со всем перечнем полей
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/push_reference_items/1.xml")
    @ExpectedDatabase(
            value = "classpath:fixtures/expected/push_reference_items/1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void withUpdatingExistedItemAndCreatingNewOne() throws Exception {
        httpOperationWithResult(
                post("/push-api/reference-items")
                        .content(extractFileContent("fixtures/controller/request/reference-items/push/1.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk());
    }

    /**
     * Тест на валидацию дубликатов.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/push_reference_items/empty.xml")
    public void onDuplicateItems() throws Exception {
        httpOperationWithResult(
                post("/push-api/reference-items")
                        .content(extractFileContent("fixtures/controller/request/reference-items/push/2.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isBadRequest());
    }

    /**
     * Тест на фильтрацию пустых айтемов.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/push_reference_items/empty.xml")
    @ExpectedDatabase(
            value = "classpath:fixtures/setup/push_reference_items/empty.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void onEmptyItems() throws Exception {
        httpOperationWithResult(
                post("/push-api/reference-items")
                        .content(extractFileContent("fixtures/controller/request/reference-items/push/3.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk());
    }

    /**
     * Тест на успешный пуш измеренных ВГХ:
     * - логирование в таблицу аудита
     * - пуш в логброкер MDM
     *
     */
    @Test
    public void shouldSuccessPushOneMeasurementDimensions() throws Exception {
        httpOperationWithResult(
                post("/push-api/measurement-dimensions")
                        .content(extractFileContent(
                                "fixtures/controller/request/reference-items/push/measurement/1.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk());

        Mockito.verify(auditService, times(1))
                .asyncLogDimensions(any(), eq("172"));

        Mockito.verify(publishService, times(1))
                .publish(dimensionsCaptor.capture(), eq("172"));

        Map<ItemIdentifier, DimensionsDTO> expected = dimensionsCaptor.getValue();

        assertSoftly(assertions -> {
            DimensionsDTO firstDimensions = expected.get(ItemIdentifier.of("1", "sku_1"));
            assertions.assertThat(firstDimensions).isNotNull();

            assertions.assertThat(firstDimensions)
                    .isEqualTo(DimensionsDTO.builder()
                            .setWidth(toBigDecimal(110, 3))
                            .setHeight(toBigDecimal(220, 3))
                            .setLength(toBigDecimal(330, 3))
                            .setWeightGross(toBigDecimal(1200, 3))
                            .build());

            DimensionsDTO secondDimensions = expected.get(ItemIdentifier.of("2", "sku_2"));
            assertions.assertThat(secondDimensions).isNotNull();

            assertions.assertThat(secondDimensions)
                    .isEqualTo(DimensionsDTO.builder()
                            .setWidth(toBigDecimal(510, 3))
                            .setHeight(toBigDecimal(420, 3))
                            .setLength(toBigDecimal(140, 3))
                            .setWeightGross(toBigDecimal(1000, 3))
                            .build());
        });

    }
}
