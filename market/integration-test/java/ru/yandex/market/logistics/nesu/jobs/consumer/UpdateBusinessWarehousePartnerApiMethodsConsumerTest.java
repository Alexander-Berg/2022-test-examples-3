package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodCreateDto;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPartnerIdPayload;
import ru.yandex.market.logistics.nesu.jobs.processor.UpdateBusinessWarehousePartnerApiMethodsProcessor;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsApiDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsApiUpdateDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsMethodsCreateDtos;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.DROPSHIP_METHODS;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.DROPSHIP_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.GET_ITEMS;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.GET_STOCKS;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.createDropshipMethodsExcept;

@ParametersAreNonnullByDefault
@DisplayName("Создание API методов в LMS для партнера бизнес-склада")
@DatabaseSetup("/jobs/consumer/update_business_warehouse_partner_api_methods/before/prepare_database.xml")
class UpdateBusinessWarehousePartnerApiMethodsConsumerTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private UpdateBusinessWarehousePartnerApiMethodsConsumer updateBusinessWarehousePartnerApiMethodsConsumer;

    @Autowired
    private UpdateBusinessWarehousePartnerApiMethodsProcessor updateBusinessWarehousePartnerApiMethodsProcessor;

    @Autowired
    private FeatureProperties featureProperties;

    @Captor
    private ArgumentCaptor<List<SettingsMethodCreateDto>> methodsCaptor;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Проверка создания API методов в LMS")
    void successUpdate(String name, Long partnerId) {
        doReturn(createSettingsApiDto(partnerId))
            .when(lmsClient).getPartnerApiSettings(partnerId);
        doReturn(createDropshipMethodsExcept(GET_STOCKS, GET_ITEMS))
            .when(lmsClient).getPartnerApiSettingsMethods(partnerId);

        updateBusinessWarehousePartnerApiMethodsConsumer.execute(createTask(partnerId));

        verifyGetPartnerApiSettings(partnerId);
        verify(lmsClient).createPartnerApiMethods(
            partnerId,
            createSettingsMethodsCreateDtos(
                List.of(
                    Pair.of(GET_STOCKS, "reference/" + partnerId + "/getStocks"),
                    Pair.of(GET_ITEMS, "reference/" + partnerId + "/getReferenceItems")
                )
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> successUpdate() {
        return Stream.of(
            Arguments.of("Dropship", 1L),
            Arguments.of("Crossdock", 123L)
        );
    }

    @Test
    @DisplayName("Проверка создания настроек API партнера в LMS")
    void successCreateApiSettings() {
        doReturn(null)
            .when(lmsClient).getPartnerApiSettings(DROPSHIP_PARTNER_ID);
        doReturn(createSettingsApiDto(DROPSHIP_PARTNER_ID))
            .when(lmsClient).createApiSettings(DROPSHIP_PARTNER_ID, createSettingsApiUpdateDto());
        doThrow(new RuntimeException())
            .when(lmsClient).getPartnerApiSettingsMethods(DROPSHIP_PARTNER_ID);

        updateBusinessWarehousePartnerApiMethodsConsumer.execute(createTask(DROPSHIP_PARTNER_ID));

        verifyGetPartnerApiSettings(DROPSHIP_PARTNER_ID);
        verify(lmsClient).createApiSettings(DROPSHIP_PARTNER_ID, createSettingsApiUpdateDto());
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Проверка создания метода партнера в LMS")
    void successUpdateMethod(String method, String url, boolean useL4S) {
        doReturn(useL4S ? List.of(method) : List.of()).when(featureProperties).getLogisticsForShopsFfApiMethods();

        doReturn(createSettingsApiDto(DROPSHIP_PARTNER_ID))
            .when(lmsClient).getPartnerApiSettings(DROPSHIP_PARTNER_ID);
        doReturn(createDropshipMethodsExcept(method))
            .when(lmsClient).getPartnerApiSettingsMethods(DROPSHIP_PARTNER_ID);

        updateBusinessWarehousePartnerApiMethodsConsumer.execute(createTask(DROPSHIP_PARTNER_ID));

        verifyGetPartnerApiSettings(DROPSHIP_PARTNER_ID);
        verify(lmsClient).createPartnerApiMethods(
            DROPSHIP_PARTNER_ID,
            createSettingsMethodsCreateDtos(List.of(Pair.of(method, url)), useL4S)
        );
    }

    @Test
    @DisplayName("Создание метода партнера в LMS с использованием l4s и методами из пропертей")
    void successFromProperties() {
        doReturn(createSettingsApiDto(DROPSHIP_PARTNER_ID))
            .when(lmsClient).getPartnerApiSettings(DROPSHIP_PARTNER_ID);
        doReturn(createDropshipMethodsExcept(
            "getOrdersStatus",
            "getOrderHistory",
            "putOutbound",
            "getOutbound",
            "getOutboundStatus",
            "getOutboundStatusHistory",
            "updateCourier",
            "getOrder"
        ))
            .when(lmsClient).getPartnerApiSettingsMethods(DROPSHIP_PARTNER_ID);

        updateBusinessWarehousePartnerApiMethodsConsumer.execute(createTask(DROPSHIP_PARTNER_ID));

        verifyGetPartnerApiSettings(DROPSHIP_PARTNER_ID);
        List<SettingsMethodCreateDto> methods = createSettingsMethodsCreateDtos(
            List.of(
                Pair.of("getOrdersStatus", "orders/getOrdersStatus"),
                Pair.of("getOrderHistory", "orders/getOrderHistory"),
                Pair.of("putOutbound", "outbounds/putOutbound"),
                Pair.of("getOutbound", "outbounds/getOutbound"),
                Pair.of("getOutboundStatus", "outbounds/getOutboundStatus"),
                Pair.of("getOutboundStatusHistory", "outbounds/getOutboundStatusHistory"),
                Pair.of("getOrder", "orders/getOrder")
            ),
            true
        );
        methods.addAll(
            createSettingsMethodsCreateDtos(List.of(Pair.of("updateCourier", "orders/updateCourier")), false)
        );
        verify(lmsClient).createPartnerApiMethods(eq(DROPSHIP_PARTNER_ID), methodsCaptor.capture());
        softly.assertThat(methodsCaptor.getValue())
            .containsExactlyInAnyOrderElementsOf(methods);
    }

    @Test
    @DisplayName("Несуществующий идентификатор магазина")
    void shopIdNotExist() {
        softly.assertThatThrownBy(() -> updateBusinessWarehousePartnerApiMethodsProcessor.processPayload(
                new ShopIdPartnerIdPayload(REQUEST_ID, DROPSHIP_PARTNER_ID, 2L)
            ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [SHOP] with ids [2]");
    }

    @Nonnull
    private static Stream<Arguments> successUpdateMethod() {
        return StreamEx.of(DROPSHIP_METHODS)
            .map(method -> Arguments.of(method.getKey(), method.getValue(), false))
            .append(
                DROPSHIP_METHODS.stream()
                    .map(method1 -> Arguments.of(method1.getKey(), method1.getValue(), true))
            );
    }

    @Nonnull
    private Task<ShopIdPartnerIdPayload> createTask(Long partnerId) {
        return new Task<>(
            new QueueShardId("1"),
            new ShopIdPartnerIdPayload(REQUEST_ID, partnerId, 1L),
            1,
            clock.instant().atZone(DateTimeUtils.MOSCOW_ZONE),
            null,
            null
        );
    }

    private void verifyGetPartnerApiSettings(long partnerId) {
        verify(lmsClient).getPartnerApiSettings(partnerId);
        verify(lmsClient).getPartnerApiSettingsMethods(partnerId);
    }
}
