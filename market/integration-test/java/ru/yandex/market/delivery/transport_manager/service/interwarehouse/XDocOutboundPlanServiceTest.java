package ru.yandex.market.delivery.transport_manager.service.interwarehouse;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;
import ru.yandex.market.delivery.transport_manager.service.xdoc.AxaptaMovementOrderPalletFilter;
import ru.yandex.market.delivery.transport_manager.service.xdoc.XDocOutboundPlanService;
import ru.yandex.market.delivery.transport_manager.util.matcher.GetAvailableLimitRequestArgumentMatcher;
import ru.yandex.market.delivery.transport_manager.util.matcher.UpdateQuotaRequestArgumentMatcher;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.AvailableLimitResponse;
import ru.yandex.market.logistics.calendaring.client.dto.RequestSizeResponse;
import ru.yandex.market.logistics.calendaring.client.dto.UpdateQuotaRequest;
import ru.yandex.market.logistics.calendaring.client.dto.UpdateSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;

@DatabaseSetup("/repository/distribution_unit_center/transportation.xml")
@DatabaseSetup(
    value = "/repository/task/no_tasks.xml",
    type = DatabaseOperation.DELETE_ALL,
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@ExpectedDatabase(
    value = "/repository/distribution_unit_center/after/put_outbound_register_task.xml",
    assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
public class XDocOutboundPlanServiceTest extends AbstractContextualTest {

    @Autowired
    private XDocOutboundPlanService xDocOutboundPlanService;

    @Autowired
    private CalendaringServiceClientApi csClient;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(csClient);
    }

    @Test
    void nothingFoundNoExcept() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(false);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }

    @Test
    @DisplayName("All inbounds in one car")
    @DatabaseSetup("/repository/distribution_unit_center/distribution_center_units_3.xml")
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/all_fit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAllFit() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_MOVEMENT_ID_INBOUND_FILTER))
            .thenReturn(false);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }


    @Test
    @DisplayName("All inbounds in one car: break bulk xdock")
    @DatabaseSetup("/repository/distribution_unit_center/distribution_center_units_3.xml")
    @DatabaseSetup(
        value = "/repository/distribution_unit_center/update/transportation_bbxd_subtype.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/all_fit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAllFitBreakBulkXDock() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_MOVEMENT_ID_INBOUND_FILTER))
            .thenReturn(true);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }

    @Test
    @DisplayName("All inbounds in one car with enables axapta filtering")
    @DatabaseSetup("/repository/distribution_unit_center/distribution_center_units_3.xml")
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/all_fit_filtered_by_axapta_movement.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAllFitWithAxaptaFilter() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(false);
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_MOVEMENT_ID_INBOUND_FILTER))
            .thenReturn(true);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }

    @Test
    @DisplayName("All inbounds in one car with enables axapta filtering and with axapta tag")
    @DatabaseSetup({
        "/repository/distribution_unit_center/distribution_center_units_3.xml",
        "/repository/distribution_unit_center/transportation_to_dc_with_units_and_tags.xml"
    })
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/all_fit_with_axapta_tag.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAllFitWithAxaptaFilterAndTags() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(false);
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_MOVEMENT_ID_INBOUND_FILTER))
            .thenReturn(true);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }

    @Test
    @DisplayName("First meta group didn't fit the car")
    @DatabaseSetup(
        value = {
            "/repository/distribution_unit_center/distribution_center_units_3.xml",
            "/repository/distribution_unit_center/additional_units_1_2.xml"
        }
    )
    @DatabaseSetup(
        value = "/repository/distribution_unit_center/update/2_pallets.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/not_fit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNotFit() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(false);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }

    @Test
    @DisplayName("First meta group fitted the car")
    @DatabaseSetup(
        value = {
            "/repository/distribution_unit_center/distribution_center_units_3.xml",
            "/repository/distribution_unit_center/additional_units_1_2.xml"
        }
    )
    @DatabaseSetup(
        value = "/repository/distribution_unit_center/update/3_pallets.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/first_fit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testFit() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(false);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }

    @Test
    @DisplayName("Third meta group fitted the car and quota")
    @DatabaseSetup({
        "/repository/distribution_unit_center/several_meta_supplies.xml",
        "/repository/distribution_unit_center/transportation.xml",
        "/repository/distribution_unit_center/transportations_to_dc_and_ff.xml",
    })
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/put_outbound_register_task_with_meta_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void testFitToQuota() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(true);
        LocalDate date = LocalDate.of(2021, 5, 3);
        Mockito
            .when(csClient.getAvailableLimit(Mockito.argThat(
                new GetAvailableLimitRequestArgumentMatcher(
                    2L,
                    BookingType.MOVEMENT_WITHDRAW,
                    date
                )
            )))
            .thenReturn(
                new AvailableLimitResponse(List.of(
                    new RequestSizeResponse(date, 0L, 100L)
                ))
            );
        Mockito.when(csClient.getAvailableLimit(Mockito.argThat(
            new GetAvailableLimitRequestArgumentMatcher(
                4L,
                BookingType.XDOCK_TRANSPORT_SUPPLY,
                date
            )
        ))).thenReturn(
            new AvailableLimitResponse(List.of(
                new RequestSizeResponse(date, 8L, 2L)
            ))
        );
        ZonedDateTime slotTimeStart = date.atTime(12, 0, 0).atZone(ZoneId.systemDefault());
        ZonedDateTime slotTimeEnd = date.atTime(12, 30, 0).atZone(ZoneId.systemDefault());
        Mockito.when(csClient.updateQuota(Mockito.eq(new UpdateQuotaRequest(1L, 1L, 1L, true))))
            .thenReturn(new UpdateSlotResponse(1L, 1L, slotTimeStart, slotTimeEnd));
        xDocOutboundPlanService.processOutboundPlanRegister(1L);

        Mockito.verify(csClient, Mockito.times(2)).getAvailableLimit(Mockito.any());
        Mockito.verify(csClient)
            .updateQuota(Mockito.argThat(new UpdateQuotaRequestArgumentMatcher(101L, 2L, 0, false)));
        Mockito.verify(csClient)
            .updateQuota(Mockito.argThat(new UpdateQuotaRequestArgumentMatcher(102L, 2L, 7L, false)));
        Mockito.verifyNoMoreInteractions(csClient);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/xdoc_transport_plan_register_removed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void cancelOutboundPlanRegister() {
        xDocOutboundPlanService.cancelOutboundPlanRegister(
            new Transportation()
                .setId(11L)
                .setOutboundUnit(new TransportationUnit().setId(12L).setLogisticPointId(2L))
        );
    }

    @DatabaseSetup({
            "/repository/distribution_unit_center/distribution_center_unit_no_pallets.xml",
    })
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/register_no_pallets.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void registryCommentNoPalletsTest() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_MOVEMENT_ID_INBOUND_FILTER))
                .thenReturn(false);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }

    @DatabaseSetup({
            "/repository/distribution_unit_center/distribution_center_unit_6.xml",
    })
    @ExpectedDatabase(
            value = "/repository/distribution_unit_center/after/register_no_place_car.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void registryCommentNoPlaceInCarTest() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_MOVEMENT_ID_INBOUND_FILTER))
                .thenReturn(false);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }

    @DatabaseSetup({
        "/repository/distribution_unit_center/distribution_center_unit_6.xml",
    })
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/register_no_place_xdoc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void registryCommentNoPlaceOnXdocTest() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(true);
        LocalDate date = LocalDate.of(2021, 5, 3);
        Mockito.when(csClient.getAvailableLimit(ArgumentMatchers.any())).thenReturn(
           new AvailableLimitResponse(List.of(
                new RequestSizeResponse(date, 0L, 4L)
            ))
        );
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
        Mockito.verify(csClient, Mockito.times(2)).getAvailableLimit(Mockito.any());
        Mockito.verify(csClient, Mockito.times(2)).updateQuota(Mockito.any());
        Mockito.verifyNoMoreInteractions(csClient);
    }

    @DatabaseSetup({
            "/repository/distribution_unit_center/distribution_center_unit_6.xml",
            "/repository/distribution_unit_center/transportation_to_dc_with_units_and_tags.xml"
    })
    @ExpectedDatabase(
            value = "/repository/distribution_unit_center/after/register_no_zper.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void registryCommentNoZperTest() {
        Mockito.mock(AxaptaMovementOrderPalletFilter.class);
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING)).thenReturn(false);
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_MOVEMENT_ID_INBOUND_FILTER))
                .thenReturn(true);
        xDocOutboundPlanService.processOutboundPlanRegister(1L);
    }
}
