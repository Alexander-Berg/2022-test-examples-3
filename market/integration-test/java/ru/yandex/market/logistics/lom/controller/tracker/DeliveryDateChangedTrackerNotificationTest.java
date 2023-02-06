package ru.yandex.market.logistics.lom.controller.tracker;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.jobs.model.NotifyOrderErrorToMqmPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessSegmentCheckpointsPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.enums.RecallCourierReason;

import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;

@DisplayName("Обработка чп по переносу даты доставки")
@ParametersAreNonnullByDefault
@DatabaseSetup({
    "/billing/before/billing_service_products.xml",
    "/controller/tracker/before/setup_delivery_date_changed.xml",
})
public class DeliveryDateChangedTrackerNotificationTest extends AbstractTrackerNotificationControllerTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DisplayName("Параметр из настроек")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_enabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromSettings(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerNeedToProcessArguments")
    @DisplayName("Экспресс. Параметр из настроек, дата должна быть обработана")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_enabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromSettingsExpressNeedToProcess(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Экспресс. Параметр из настроек, дату не нужно обрабатывать")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_enabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromSettingsExpress(boolean newFlowEnabled) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handleAndCheckDeliveryDateUpdated(
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            "controller/tracker/request/47/lo1_order_delivery_date_updated_47.json",
            false,
            newFlowEnabled
        );
        softly.assertThat(log47Checkpoint()).isTrue();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DisplayName("У партнёра отключено получение даты доставки по API")
    @ExpectedDatabase(
        value = "/controller/tracker/after/no_requests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerWithDisabledPartnerParam(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handlePartnerWithoutDeliveryDateUpdateApi(status, request, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DisplayName("Экспресс. У партнёра отключено получение даты доставки по API, нужно обрабатывать изменение даты")
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/no_requests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerWithDisabledPartnerParamExpressNeedToProcess(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handlePartnerWithoutDeliveryDateUpdateApi(status, request, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DisplayName("Экспресс. У партнёра отключено получение даты доставки по API, не нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/no_requests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerWithDisabledPartnerParamExpress(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handlePartnerWithoutDeliveryDateUpdateApi(status, request, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DisplayName("Средняя миля - параметр из партнера, нет настроек")
    @DatabaseSetup("/controller/tracker/before/setup_delivery_date_middle_mile.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromPartner(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();

        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerNeedToProcessArguments")
    @DisplayName("Экспресс. Средняя миля - параметр из партнера, нет настроек, дата должна быть обработана")
    @DatabaseSetup("/controller/tracker/before/setup_delivery_date_middle_mile.xml")
    @DatabaseSetup(value = "/controller/tracker/before/express_tag.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromPartnerExpressNeedToProcessDate(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(
            status,
            request,
            status == OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            newFlowEnabled
        );
        softly.assertThat(log47Checkpoint()).isFalse();

        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Экспресс. Средняя миля - параметр из партнера, нет настроек, дату не нужно обрабатывать")
    @DatabaseSetup("/controller/tracker/before/setup_delivery_date_middle_mile.xml")
    @DatabaseSetup(value = "/controller/tracker/before/express_tag.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromPartnerExpress(boolean newFlowEnabled) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            "controller/tracker/request/47/lo1_order_delivery_date_updated_47.json",
            false,
            newFlowEnabled
        );

        softly.assertThat(log47Checkpoint()).isTrue();
        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DisplayName("Средняя миля - параметр из партнера, в настройках нет значения параметра")
    @DatabaseSetup("/controller/tracker/before/setup_delivery_date_middle_mile.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromPartnerSettingsWithoutValue(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();

        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerNeedToProcessArguments")
    @DisplayName(
        "Экспресс. Средняя миля - параметр из партнера, в настройках нет значения параметра, нужно обрабатывать дату"
    )
    @DatabaseSetup("/controller/tracker/before/setup_delivery_date_middle_mile.xml")
    @DatabaseSetup(value = "/controller/tracker/before/express_tag.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromPartnerSettingsWithoutValueExpressNeedToProcessDate(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(
            status,
            request,
            status == OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            newFlowEnabled
        );
        softly.assertThat(log47Checkpoint()).isFalse();

        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName(
        "Экспресс. Средняя миля - параметр из партнера, в настройках нет значения параметра, не нужно обрабатывать дату"
    )
    @DatabaseSetup("/controller/tracker/before/setup_delivery_date_middle_mile.xml")
    @DatabaseSetup(value = "/controller/tracker/before/express_tag.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromPartnerSettingsWithoutValueExpress(boolean newFlowEnabled) {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            "controller/tracker/request/47/lo1_order_delivery_date_updated_47.json",
            false,
            newFlowEnabled
        );
        softly.assertThat(log47Checkpoint()).isTrue();

        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DisplayName("Средняя миля - параметр из настроек")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_enabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromSettings(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerNeedToProcessArguments")
    @DisplayName("Экспресс. Средняя миля - параметр из настроек, нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_enabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(value = "/controller/tracker/before/express_tag.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromSettingsExpressNeedToProcessDate(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @Test
    @DisplayName("Экспресс. Средняя миля - параметр из настроек, не нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_enabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(value = "/controller/tracker/before/express_tag.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/47/send_recall_courier_to_mqm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromSettingsExpress() {
        deliveryDateUpdatedHandlerMiddleMileParamFromSettingsExpress(false);
    }

    @Test
    @DisplayName("Экспресс. Средняя миля - параметр из настроек, не нужно обрабатывать дату, новый флоу чекпоинтов")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_enabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(value = "/controller/tracker/before/express_tag.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/47/send_recall_courier_to_mqm_new_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerMiddleMileParamFromSettingsExpressNewFlow() {
        deliveryDateUpdatedHandlerMiddleMileParamFromSettingsExpress(true);
    }

    @SneakyThrows
    private void deliveryDateUpdatedHandlerMiddleMileParamFromSettingsExpress(boolean newFlowEnabled) {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handleAndCheckDeliveryDateUpdated(
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            "controller/tracker/request/47/lo1_order_delivery_date_updated_47.json",
            false,
            newFlowEnabled
        );
        softly.assertThat(log47Checkpoint()).isTrue();
        recallCourierToMqm();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_null.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Параметр из партнера, есть настройки без параметра")
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromPartnerSettingsWithoutParam(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerNeedToProcessArguments")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_null.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Экспресс. Параметр из партнера, есть настройки без параметра, нужно обрабатывать дату")
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromPartnerSettingsWithoutParamExpressNeedToProcessDate(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();

        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
    }

    @Test
    @DisplayName("Экспресс. Параметр из партнера, есть настройки без параметра, не нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_null.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/47/send_recall_courier_to_mqm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromPartnerSettingsWithoutParamExpress() {
        deliveryDateUpdatedHandlerFromPartnerSettingsWithoutParamExpress(false);
    }

    @Test
    @DisplayName("Экспресс. Параметр из партнера, есть настройки без параметра, не нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_get_delivery_date_enabled_null.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/47/send_recall_courier_to_mqm_new_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromPartnerSettingsWithoutParamExpressNewFlow() {
        deliveryDateUpdatedHandlerFromPartnerSettingsWithoutParamExpress(true);
    }

    @SneakyThrows
    private void deliveryDateUpdatedHandlerFromPartnerSettingsWithoutParamExpress(boolean newFlowEnabled) {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            "controller/tracker/request/47/lo1_order_delivery_date_updated_47.json",
            false,
            newFlowEnabled
        );
        softly.assertThat(log47Checkpoint()).isTrue();
        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
        recallCourierToMqm();
    }

    @DisplayName("Есть активная заявка")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DatabaseSetup(
        value = "/controller/tracker/before/setup_active_update_delivery_date_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/processing_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerWithActiveRequest(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handlePartnerWithoutDeliveryDateUpdateApi(status, request, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @DisplayName("Экспресс. Есть активная заявка, нужно обрабатывать дату")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerNeedToProcessArguments")
    @DatabaseSetup(
        value = "/controller/tracker/before/setup_active_update_delivery_date_request.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/processing_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerWithActiveRequestExpressNeedToProcessDate(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handlePartnerWithoutDeliveryDateUpdateApi(status, request, newFlowEnabled);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Экспресс. Есть активная заявка, не нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/setup_active_update_delivery_date_request.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/processing_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerWithActiveRequestExpress(boolean newFlowEnabled) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        handlePartnerWithoutDeliveryDateUpdateApi(
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            "controller/tracker/request/47/lo1_order_delivery_date_updated_47.json",
            newFlowEnabled
        );
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerArguments")
    @DisplayName("Параметр из партнера, нет настроек")
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromPartnerNoSettings(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("deliveryDateUpdatedHandlerNeedToProcessArguments")
    @DisplayName("Экспресс. Параметр из партнера, нет настроек, нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/created_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromPartnerNoSettingsExpressNeedToProcessDate(
        @SuppressWarnings("unused") String name,
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(status, request, true, newFlowEnabled);
        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
        softly.assertThat(log47Checkpoint()).isFalse();
    }

    @Test
    @DisplayName("Экспресс. Параметр из партнера, нет настроек, не нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/47/send_recall_courier_to_mqm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromPartnerNoSettingsExpress() {
        deliveryDateUpdatedHandlerFromPartnerNoSettingsExpress(false);
    }

    @Test
    @DisplayName("Экспресс. Параметр из партнера, нет настроек, не нужно обрабатывать дату")
    @DatabaseSetup(
        value = "/controller/tracker/before/express_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/success_request_for_date_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/47/send_recall_courier_to_mqm_new_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryDateUpdatedHandlerFromPartnerNoSettingsExpressNewFlow() {
        deliveryDateUpdatedHandlerFromPartnerNoSettingsExpress(true);
    }

    @SneakyThrows
    private void deliveryDateUpdatedHandlerFromPartnerNoSettingsExpress(boolean newFlowEnabled) {
        setCheckpointsProcessingFlow(newFlowEnabled);
        mockLmsClientGetDsPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        handleAndCheckDeliveryDateUpdated(
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
            "controller/tracker/request/47/lo1_order_delivery_date_updated_47.json",
            false,
            newFlowEnabled
        );
        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
        softly.assertThat(log47Checkpoint()).isTrue();
        recallCourierToMqm();
    }

    private void recallCourierToMqm() {
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.NOTIFY_ORDER_ERROR_TO_MQM,
            new NotifyOrderErrorToMqmPayload(
                REQUEST_ID + "/1/1",
                1,
                null,
                "ext1",
                EventType.LOM_RECALL_COURIER,
                null,
                Map.of("recallReason", RecallCourierReason.ROTTEN_CALL_COURIER.name())
            ),
            2
        );
    }

    @Nonnull
    private static Stream<Arguments> deliveryDateUpdatedHandlerArguments() {
        List<Triple<String, OrderDeliveryCheckpointStatus, String>> arguments = List.of(
            Triple.of(
                "44 чекпоинт",
                OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP,
                "controller/tracker/request/44/lo1_order_delivery_date_updated_44.json"
            ),
            Triple.of(
                "46 чекпоинт",
                OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT,
                "controller/tracker/request/46/lo1_order_delivery_date_updated_46.json"
            ),
            Triple.of(
                "47 чекпоинт",
                OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
                "controller/tracker/request/47/lo1_order_delivery_date_updated_47.json"
            )
        );
        return Stream.concat(
            arguments.stream()
                .map(triple -> Arguments.of(
                    triple.getLeft() + getFlowName(true),
                    triple.getMiddle(),
                    triple.getRight(),
                    true
                )),
            arguments.stream()
                .map(triple -> Arguments.of(
                        triple.getLeft() + getFlowName(false),
                        triple.getMiddle(),
                        triple.getRight(),
                        false
                    )
                )
        );
    }

    @Nonnull
    private static Stream<Arguments> deliveryDateUpdatedHandlerNeedToProcessArguments() {
        List<Triple<String, OrderDeliveryCheckpointStatus, String>> arguments = List.of(
            Triple.of(
                "44 чекпоинт",
                OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP,
                "controller/tracker/request/44/lo1_order_delivery_date_updated_44.json"
            ),
            Triple.of(
                "46 чекпоинт",
                OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT,
                "controller/tracker/request/46/lo1_order_delivery_date_updated_46.json"
            )
        );
        return Stream.concat(
            arguments.stream()
                .map(triple -> Arguments.of(
                    triple.getLeft() + getFlowName(true),
                    triple.getMiddle(),
                    triple.getRight(),
                    true
                )),
            arguments.stream()
                .map(triple -> Arguments.of(
                        triple.getLeft() + getFlowName(false),
                        triple.getMiddle(),
                        triple.getRight(),
                        false
                    )
                )
        );
    }

    @Nonnull
    private static String getFlowName(boolean newCheckpointsFlowEnabled) {
        if (newCheckpointsFlowEnabled) {
            return " new flow";
        }

        return " old flow";
    }

    private void handleAndCheckDeliveryDateUpdated(
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean needToProcessDateChanging,
        boolean newCheckpointsFlowEnabled
    )
        throws Exception {
        notifyTracks(request, "controller/tracker/response/push_101_ok.json");

        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(1, status, 101);
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlowEnabled,
            segmentCheckpointsPayload,
            orderIdDeliveryTrackPayload
        );

        if (!needToProcessDateChanging) {
            return;
        }
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_DELIVERY_DATE_UPDATED_BY_DS,
            createChangeOrderRequestPayload(1, "2", 1, 1)
        );
    }

    private void handlePartnerWithoutDeliveryDateUpdateApi(
        OrderDeliveryCheckpointStatus status,
        String request,
        boolean newFlowEnabled
    ) throws Exception {
        mockLmsClientGetDsPartner(List.of());
        notifyTracks(request, "controller/tracker/response/push_101_ok.json");

        var orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(1, status, 101);
        var segmentCheckpointPayload = processSegmentCheckpointsPayloadWithSequence(2L, 101L);
        assertCheckpointsTaskCreatedAndRunTask(newFlowEnabled, segmentCheckpointPayload, orderIdDeliveryTrackPayload);

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_DELIVERY_DATE_UPDATED_BY_DS);

        verify(lmsClient).getPartner(DELIVERY_PARTNER_ID);
    }

    private boolean log47Checkpoint() {
        return backLogCaptor.getResults().stream().anyMatch(str -> str.contains(""
            + "level=WARN\t"
            + "format=plain\t"
            + "code=EXPRESS_DELIVERY_DATE_CHANGED_BY_DELIVERY\t"
            + "payload=Got 47 checkpoint for express order\t"
            + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t"
            + "entity_types=order,lom_order\t"
            + "entity_values=order:ext1,lom_order:1"
        ));
    }
}
