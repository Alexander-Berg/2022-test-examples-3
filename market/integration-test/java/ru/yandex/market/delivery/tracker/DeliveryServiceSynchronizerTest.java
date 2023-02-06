package ru.yandex.market.delivery.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import ru.yandex.market.delivery.tracker.configuration.properties.DeliveryServiceSyncProperties;
import ru.yandex.market.delivery.tracker.dao.repository.DbDeliveryServiceRepository;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.enums.MethodTypeToRequestType;
import ru.yandex.market.delivery.tracker.service.tracking_service.DeliveryServiceSynchronizer;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsApiFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryServiceSynchronizerTest extends AbstractContextualTest {

    private static final Set<PartnerType> SYNCHRONIZED_PARTNER_TYPES = EnumSet.of(
        PartnerType.DELIVERY,
        PartnerType.FULFILLMENT,
        PartnerType.SORTING_CENTER,
        PartnerType.DROPSHIP,
        PartnerType.SUPPLIER,
        PartnerType.DISTRIBUTION_CENTER,
        PartnerType.XDOC
    );
    private static final Set<String> SYNCHRONIZED_METHOD_TYPES = Set.of(
        MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(),
        MethodTypeToRequestType.GET_ORDER_HISTORY.getMethodType(),
        MethodTypeToRequestType.GET_WH_ORDERS_STATUS.getMethodType(),
        MethodTypeToRequestType.GET_WH_ORDER_HISTORY.getMethodType(),
        MethodTypeToRequestType.GET_MOVEMENT_STATUS.getMethodType(),
        MethodTypeToRequestType.GET_MOVEMENT_STATUS_HISTORY.getMethodType(),
        MethodTypeToRequestType.GET_INBOUND_STATUS.getMethodType(),
        MethodTypeToRequestType.GET_INBOUND_STATUS_HISTORY.getMethodType(),
        MethodTypeToRequestType.GET_OUTBOUND_STATUS.getMethodType(),
        MethodTypeToRequestType.GET_OUTBOUND_STATUS_HISTORY.getMethodType(),
        MethodTypeToRequestType.GET_TRANSFERS_STATUS.getMethodType(),
        MethodTypeToRequestType.GET_TRANSFER_HISTORY.getMethodType(),
        MethodTypeToRequestType.GET_INBOUNDS_STATUS_OLD.getMethodType(),
        MethodTypeToRequestType.GET_INBOUND_HISTORY_OLD.getMethodType(),
        MethodTypeToRequestType.GET_OUTBOUNDS_STATUS_OLD.getMethodType(),
        MethodTypeToRequestType.GET_OUTBOUND_HISTORY_OLD.getMethodType(),
        MethodTypeToRequestType.GET_EXTERNAL_ORDER_HISTORY.getMethodType(),
        MethodTypeToRequestType.GET_EXTERNAL_ORDERS_STATUS.getMethodType()
    );
    public static final Set<Long> PARTNER_IDS = LongStream.range(1, 1005)
        .boxed()
        .collect(Collectors.toSet());
    private static final String UNSUPPORTED_METHOD_TYPE = "UNSUPPORTED_TYPE";
    private static final String TRUE_VALUE = "1";

    @Autowired
    private DeliveryServiceSynchronizer synchronizer;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private DbDeliveryServiceRepository dbDeliveryServiceRepository;

    @Autowired
    private DeliveryServiceSyncProperties deliveryServiceSyncProperties;

    @Test
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_all_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeWithAllNew() {
        mockLmsClientSearchPartners(
            createCommonPartner(1),
            partner(2, PartnerType.DELIVERY, null, false, false, true)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L, 2L),
            createSettingsApi(1, "token_1", "2.*"),
            createSettingsApi(2, "token_2", "3.*")
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L, 2L),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDER_HISTORY.getMethodType(), true, 660),
            createSettingsMethod(2, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, null),
            createSettingsMethod(2, MethodTypeToRequestType.GET_ORDER_HISTORY.getMethodType(), true, 1320),
            createSettingsMethod(1, MethodTypeToRequestType.GET_MOVEMENT_STATUS.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_MOVEMENT_STATUS_HISTORY.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_INBOUND_STATUS.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_INBOUND_STATUS_HISTORY.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_OUTBOUND_STATUS.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_OUTBOUND_STATUS_HISTORY.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_TRANSFERS_STATUS.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_TRANSFER_HISTORY.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_INBOUNDS_STATUS_OLD.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_INBOUND_HISTORY_OLD.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_OUTBOUNDS_STATUS_OLD.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_OUTBOUND_HISTORY_OLD.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_EXTERNAL_ORDERS_STATUS.getMethodType(), true, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_EXTERNAL_ORDER_HISTORY.getMethodType(), true, 660)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    @DatabaseSetup("/database/states/tracking_service/synchronize_with_new_and_existing.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_new_and_existing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeWithNewAndExisting() {
        mockLmsClientSearchPartners(
            createCommonPartner(1),
            partner(3, PartnerType.DELIVERY, null, false, false, true)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L, 3L),
            createSettingsApi(1, "token_1", "2.*"),
            createSettingsApi(3, "token_3", "3.*")
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L, 3L),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), false, 600),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDER_HISTORY.getMethodType(), false, 660),
            createSettingsMethod(3, MethodTypeToRequestType.GET_WH_ORDERS_STATUS.getMethodType(), false, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_WH_ORDER_HISTORY.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_MOVEMENT_STATUS.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_MOVEMENT_STATUS_HISTORY.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_INBOUND_STATUS.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_INBOUND_STATUS_HISTORY.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_OUTBOUND_STATUS.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_OUTBOUND_STATUS_HISTORY.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_TRANSFERS_STATUS.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_TRANSFER_HISTORY.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_INBOUNDS_STATUS_OLD.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_INBOUND_HISTORY_OLD.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_OUTBOUNDS_STATUS_OLD.getMethodType(), true, null),
            createSettingsMethod(3, MethodTypeToRequestType.GET_OUTBOUND_HISTORY_OLD.getMethodType(), true, null)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    @DatabaseSetup("/database/states/tracking_service/synchronize_with_existing_service_and_no_methods.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_existing_service_and_no_methods.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeWithExistingServiceAndNoMethods() {
        mockLmsClientSearchPartners(
            createCommonPartner(1)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L),
            createSettingsApi(1, "token_1", "2.*")
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    @DatabaseSetup("/database/expected/tracking_service/synchronize_with_all_new.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_all_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeWithNoServices() {
        mockLmsClientSearchPartners();

        synchronizer.synchronize();
        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(0)).searchPartnerApiSettings(any());
        verify(lmsClient, times(0)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    @DatabaseSetup("/database/states/tracking_service/synchronize_with_invalid_settings.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_invalid_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeWithUnsupportedMethodType() {
        mockLmsClientSearchPartners(
            createCommonPartner(1)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L),
            createSettingsApi(1, "token_1", "2.*")
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L),
            createSettingsMethod(1, UNSUPPORTED_METHOD_TYPE, false, 600)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    @DatabaseSetup("/database/states/tracking_service/synchronize_with_invalid_settings.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_invalid_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeWithInvalidApiVersion() {
        mockLmsClientSearchPartners(
            createCommonPartner(1)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L),
            createSettingsApi(1, "token_1", "INVALID_VERSION")
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    void synchronizeWithNoSettingsApi() {
        mockLmsClientSearchPartners(
            createCommonPartner(1)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L)
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );

        assertThrows(IllegalStateException.class, () -> synchronizer.synchronize());
    }

    @Test
    @DatabaseSetup("/database/states/tracking_service/synchronize_above_batch_size.xml")
    void synchronizeWithNewAndExistingAboveBatchSize() {
        mockLmsClientSearchPartners(
            IntStream.range(1, 1005)
                .mapToObj(this::createCommonPartner).toArray(PartnerResponse[]::new)
        );

        mockLmsClientSearchPartnerApiSettings(
            PARTNER_IDS,
            IntStream.range(1, 1005)
                .mapToObj(value -> createSettingsApi(value, "token_1", "2.*"))
                .toArray(SettingsApiDto[]::new)
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            PARTNER_IDS,
            IntStream.range(1, 1005)
                .mapToObj(value -> createSettingsMethod(
                    value,
                    MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(),
                    true,
                    null
                ))
                .toArray(SettingsMethodDto[]::new)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());

        int activeServicesAfterSync = dbDeliveryServiceRepository.getAllAvailableServices().size();
        List<DeliveryService> existing = dbDeliveryServiceRepository.getServicesOfSource(1234);

        assertions
            .assertThat(activeServicesAfterSync)
            .as("Active services amount after sync should be 1004")
            .isEqualTo(1004);
        assertions
            .assertThat(existing)
            .as("Existing services are not active anymore")
            .extracting(DeliveryService::isActive)
            .containsExactly(false, false);
    }

    @Test
    @DatabaseSetup("/database/states/tracking_service/synchronize_empty.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_multiple.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeMultipleApiVersions() {
        long partnerId = 1L;
        Set<Long> partnerIds = Set.of(partnerId);

        mockLmsClientSearchPartners(createCommonPartner(partnerId));
        mockLmsClientSearchPartnerApiSettings(
            partnerIds,
            createSettingsApi(10, 1, "token_12", "2.*"),
            createSettingsApi(11, 1, "token_13", "3.*")
        );
        mockLmsClientSearchPartnerApiSettingsMethods(
            partnerIds,
            createSettingsMethod(10, 1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600),
            createSettingsMethod(11, 1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_different_priority_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeWithDifferentPartnersTypes() {
        mockLmsClientSearchPartners(
            createCommonPartner(1),
            createExpressPartner(2)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L, 2L),
            createSettingsApi(1, "token_1", "2.*"),
            createSettingsApi(2, "token_2", "3.*")
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L, 2L),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600),
            createSettingsMethod(2, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    @DatabaseSetup("/database/states/tracking_service/synchronize_with_update_priority_type.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_update_priority_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeWithUpdatePartnersTypes() {
        mockLmsClientSearchPartners(
            createCommonPartner(1),
            createCommonPartner(2)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L, 2L),
            createSettingsApi(1, "token_1", "2.*"),
            createSettingsApi(2, "token_2", "3.*")
        );

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L, 2L),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600),
            createSettingsMethod(2, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @Test
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_roles.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void synchronizeSaveRoles() {
        mockLmsClientSearchPartners(
            partner(1, PartnerType.DELIVERY),
            partner(2, PartnerType.DELIVERY, 1L),
            partner(3, PartnerType.DELIVERY, 2L),
            partner(4, PartnerType.DELIVERY, 3L),
            partner(5, PartnerType.DELIVERY, 4L),
            partner(6, PartnerType.DELIVERY, 5L),
            partner(7, PartnerType.DELIVERY, 103L),
            partner(8, PartnerType.DELIVERY, 67L),
            partner(9, PartnerType.DELIVERY, 8L),
            partner(10, PartnerType.DELIVERY, 34L),
            partner(1006360, PartnerType.DELIVERY),
            partner(12, PartnerType.FULFILLMENT),
            partner(13, PartnerType.SORTING_CENTER),
            partner(14, PartnerType.SORTING_CENTER, 6L),
            partner(15, PartnerType.SORTING_CENTER, 7L),
            partner(16, PartnerType.SORTING_CENTER, 136L),
            partner(17, PartnerType.SORTING_CENTER, 68L),
            partner(18, PartnerType.SORTING_CENTER, null, false, true, false),
            partner(21, PartnerType.DISTRIBUTION_CENTER),
            partner(19, PartnerType.DROPSHIP),
            partner(20, PartnerType.DROPSHIP, null, true, false, false),
            partner(22, PartnerType.SUPPLIER),
            partner(23, PartnerType.XDOC)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(1)).searchPartnerApiSettings(any());
        verify(lmsClient, times(1)).searchPartnerApiSettingsMethods(any());
    }

    @DisplayName("Синхронизации не происходит при ошибке получения партнеров из LMS")
    @Test
    @DatabaseSetup("/database/expected/tracking_service/synchronize_with_all_new.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_all_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotSynchronizeOnErrorDuringPartnerFetching() {
        when(lmsClient.searchPartners(any()))
            .thenThrow(new HttpTemplateException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Some error"));

        assertThrows(
            RuntimeException.class,
            () -> synchronizer.synchronize(),
            "Error occurred during partners fetch from LMS"
        );

        verify(lmsClient, times(1)).searchPartners(any(), any());
    }

    @DisplayName("Успешная синхронизация после уменьшения размера батча")
    @Test
    void syncWasSuccessfullAfterLoweringBatchSize() {
        deliveryServiceSyncProperties.setBatchSize(2);
        mockLmsClientSearchPartners(
            partner(1, PartnerType.DELIVERY),
            partner(2, PartnerType.DELIVERY, 1L),
            partner(3, PartnerType.DELIVERY, 2L),
            partner(4, PartnerType.DELIVERY, 3L)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L, 2L),
            createSettingsApi(1, "token_1", "2.*"),
            createSettingsApi(2, "token_2", "3.*")
        );
        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L, 2L),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600),
            createSettingsMethod(2, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );

        when(lmsClient.searchPartnerApiSettings(SettingsApiFilter.newBuilder().partnerIds(Set.of(3L, 4L)).build()))
            .thenThrow(ResourceAccessException.class);

        mockLmsClientSearchPartnerApiSettings(Set.of(3L), createSettingsApi(3, "token_3", "2.*"));
        mockLmsClientSearchPartnerApiSettings(Set.of(4L), createSettingsApi(4, "token_4", "2.*"));

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(3L),
            createSettingsMethod(3, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );
        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(4L),
            createSettingsMethod(4, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(4)).searchPartnerApiSettings(any());
        verify(lmsClient, times(3)).searchPartnerApiSettingsMethods(any());
    }

    @DisplayName("Успешная синхронизация c уменьшением размера батча после ошибки получения методов")
    @Test
    void syncWasSuccessfullAfterErrorDuringMethodsFetching() {
        deliveryServiceSyncProperties.setBatchSize(2);
        mockLmsClientSearchPartners(
            partner(1, PartnerType.DELIVERY),
            partner(2, PartnerType.DELIVERY, 1L),
            partner(3, PartnerType.DELIVERY, 2L),
            partner(4, PartnerType.DELIVERY, 3L)
        );

        mockLmsClientSearchPartnerApiSettings(
            Set.of(1L, 2L),
            createSettingsApi(1, "token_1", "2.*"),
            createSettingsApi(2, "token_2", "3.*")
        );
        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(1L, 2L),
            createSettingsMethod(1, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600),
            createSettingsMethod(2, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );
        mockLmsClientSearchPartnerApiSettings(
            Set.of(3L, 4L),
            createSettingsApi(3, "token_3", "2.*"),
            createSettingsApi(4, "token_4", "3.*")
        );

        when(lmsClient.searchPartnerApiSettingsMethods(
            SettingsMethodFilter.newBuilder()
                .partnerIds(Set.of(3L, 4L))
                .methodTypes(SYNCHRONIZED_METHOD_TYPES)
                .build()
        ))
            .thenThrow(ResourceAccessException.class);

        mockLmsClientSearchPartnerApiSettings(Set.of(3L), createSettingsApi(3, "token_3", "2.*"));
        mockLmsClientSearchPartnerApiSettings(Set.of(4L), createSettingsApi(4, "token_4", "2.*"));

        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(3L),
            createSettingsMethod(3, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );
        mockLmsClientSearchPartnerApiSettingsMethods(
            Set.of(4L),
            createSettingsMethod(4, MethodTypeToRequestType.GET_ORDERS_STATUS.getMethodType(), true, 600)
        );

        synchronizer.synchronize();

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(4)).searchPartnerApiSettings(any());
        verify(lmsClient, times(4)).searchPartnerApiSettingsMethods(any());

    }

    @DisplayName("Синхронизация не происходит при достижении лимита попыток")
    @Test
    @DatabaseSetup("/database/expected/tracking_service/synchronize_with_all_new.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_all_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncFailedAfterRetryLimitWasReached() {
        mockLmsClientSearchPartners(createCommonPartner(1L));
        when(lmsClient.searchPartnerApiSettingsMethods(any()))
            .thenThrow(ResourceAccessException.class);

        assertThrows(RuntimeException.class, () -> synchronizer.synchronize());

        verify(lmsClient, times(1)).searchPartners(any(), any());
        verify(lmsClient, times(3)).searchPartnerApiSettings(any());
        verify(lmsClient, times(3)).searchPartnerApiSettingsMethods(any());
    }

    @DisplayName("Синхронизация не происходит при достижении лимита попыток получения страницы партнеров")
    @Test
    @DatabaseSetup("/database/expected/tracking_service/synchronize_with_all_new.xml")
    @ExpectedDatabase(
        value = "/database/expected/tracking_service/synchronize_with_all_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryServiceSyncFailedAfterRetryLimitWasReached() {
        when(lmsClient.searchPartners(any(), any()))
            .thenThrow(ResourceAccessException.class);

        assertThrows(RuntimeException.class, () -> synchronizer.synchronize());
        verify(lmsClient, times(3)).searchPartners(any(), any());
        verify(lmsClient, times(0)).searchPartnerApiSettings(any());
        verify(lmsClient, times(0)).searchPartnerApiSettingsMethods(any());
    }

    private void mockLmsClientSearchPartners(PartnerResponse... partnerResponse) {
        PageRequest pageRequest = new PageRequest(0, deliveryServiceSyncProperties.getPageSize());
        PageResult<PartnerResponse> pageResult = new PageResult<>();
        pageResult.setData(List.of(partnerResponse));
        when(lmsClient.searchPartners(
                SearchPartnerFilter.builder().setTypes(SYNCHRONIZED_PARTNER_TYPES).build(),
                pageRequest
            )
        ).thenReturn(pageResult);
    }

    private void mockLmsClientSearchPartnerApiSettings(Set<Long> partnerIds, SettingsApiDto... settingsApis) {
        SettingsApiFilter settingsApiFilter = SettingsApiFilter.newBuilder()
            .partnerIds(partnerIds)
            .build();

        when(lmsClient.searchPartnerApiSettings(settingsApiFilter))
            .thenReturn(Arrays.asList(settingsApis));
    }

    private void mockLmsClientSearchPartnerApiSettingsMethods(
        Set<Long> partnerIds,
        SettingsMethodDto... settingsMethods
    ) {
        SettingsMethodFilter settingsMethodFilter = SettingsMethodFilter.newBuilder()
            .partnerIds(partnerIds)
            .methodTypes(SYNCHRONIZED_METHOD_TYPES)
            .build();

        when(lmsClient.searchPartnerApiSettingsMethods(settingsMethodFilter))
            .thenReturn(Arrays.asList(settingsMethods));
    }

    private PartnerResponse createCommonPartner(long id) {
        return partner(id, PartnerType.DELIVERY, null, false, false, false);
    }

    private PartnerResponse createExpressPartner(long id) {
        return partner(id, PartnerType.DELIVERY, null, true, false, false);
    }

    private PartnerResponse partner(long id, PartnerType partnerType) {
        return partner(id, partnerType, null, false, false, false);
    }

    private PartnerResponse partner(long id, PartnerType partnerType, long subtypeId) {
        return partner(id, partnerType, subtypeId, false, false, false);
    }

    private PartnerResponse partner(
        long id,
        PartnerType partnerType,
        Long subtypeId,
        boolean express,
        boolean dropoff,
        boolean pullTrackingEnabled
    ) {
        PartnerResponse.PartnerResponseBuilder builder =
            PartnerResponse.newBuilder()
                .id(id)
                .readableName("name_" + id)
                .partnerType(partnerType);
        if (subtypeId != null) {
            builder.subtype(
                PartnerSubtypeResponse.newBuilder()
                    .id(subtypeId)
                    .build()
            );
        }

        List<PartnerExternalParam> params = new ArrayList<>();

        if (express) {
            addExternalParamTrue(params, PartnerExternalParamType.DROPSHIP_EXPRESS);
        }
        if (dropoff) {
            addExternalParamTrue(params, PartnerExternalParamType.IS_DROPOFF);
        }
        if (pullTrackingEnabled) {
            addExternalParamTrue(params, PartnerExternalParamType.GET_ORDER_HISTORY_PULL_TRACKING_ENABLED);
        }

        if (!params.isEmpty()) {
            builder.params(params);
        }
        return builder.build();
    }

    private void addExternalParamTrue(List<PartnerExternalParam> params, PartnerExternalParamType dropshipExpress) {
        params.add(
            new PartnerExternalParam(
                dropshipExpress.name(),
                "",
                TRUE_VALUE
            )
        );
    }

    @Nonnull
    private SettingsApiDto createSettingsApi(long partnerId, String token, String version) {
        return createSettingsApi(partnerId, partnerId, token, version);
    }

    @Nonnull
    private SettingsApiDto createSettingsApi(long id, long partnerId, String token, String version) {
        return SettingsApiDto.newBuilder()
            .id(id)
            .partnerId(partnerId)
            .token(token)
            .version(version)
            .build();
    }

    @Nonnull
    private SettingsMethodDto createSettingsMethod(
        long partnerId,
        String methodType,
        boolean isActive,
        Integer pollingFrequencyInSecs
    ) {
        return createSettingsMethod(partnerId, partnerId, methodType, isActive, pollingFrequencyInSecs);
    }

    @Nonnull
    private SettingsMethodDto createSettingsMethod(
        long settingsId,
        long partnerId,
        String methodType,
        boolean isActive,
        Integer pollingFrequencyInSecs
    ) {
        return SettingsMethodDto.newBuilder()
            .settingsApiId(settingsId)
            .partnerId(partnerId)
            .method(methodType)
            .url("http://localhost/" + methodType)
            .active(isActive)
            .entityPollingFrequencyInSecs(pollingFrequencyInSecs)
            .build();
    }
}
