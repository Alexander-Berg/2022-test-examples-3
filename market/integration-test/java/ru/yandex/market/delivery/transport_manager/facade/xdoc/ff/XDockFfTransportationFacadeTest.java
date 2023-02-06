package ru.yandex.market.delivery.transport_manager.facade.xdoc.ff;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocRequestStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.submit_date.XDocSubmitInboundDateDto;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;
import ru.yandex.market.delivery.transport_manager.util.XDocTestingConstants;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.ff.client.dto.XDocFinalInboundDateDTO;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"}
)
@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
class XDockFfTransportationFacadeTest extends AbstractContextualTest {

    @Autowired
    private XDockFfTransportationFacade facade;

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @Autowired
    protected TmPropertyService propertyService;

    ShopRequestDetailsDTO shopRequestDetails1pDTO;
    ShopRequestDetailsDTO shopRequestDetailsBreakBulkXDockDTO;
    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2021, 5, 1, 20, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );

        this.shopRequestDetails1pDTO = XDocTestingConstants.createShopRequestDetails1pDTO();
        this.shopRequestDetailsBreakBulkXDockDTO = XDocTestingConstants.createShopRequestDetailsBreakBulkXDockDTO();
    }

    @DisplayName("Создание виртуального перемещения 3p товаров от мерча в FF")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_3p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_axapta_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_rc_to_ff_transportation_tag_3p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_dbqueue_single_transportation_3p.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persistItemsToFFTransportation3p() {
        facade.processXDocRequestCreatedOrValidated(
            XDocRequestStatus.VALIDATED,
            XDocTestingConstants.X_DOC_CREATE_DATA_3P
        );
    }

    @DisplayName("Создание виртуального перемещения 1p товаров от мерча в FF")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_1p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_axapta_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_rc_to_ff_transportation_tag_1p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_dbqueue_single_transportation_1p.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persistItemsToFFTransportationWaiting4Confirmation1p() {
        facade.processXDocRequestCreatedOrValidated(
            XDocRequestStatus.WAITING_FOR_CONFIRMATION,
            XDocTestingConstants.X_DOC_CREATE_DATA_1P
        );
    }

    @DisplayName("Создание виртуального перемещения Break Bulk XDock товаров от РЦ(WMS) в FF")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_break_bulk_xdock.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_axapta_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_rc_to_ff_transportation_tag_break_bulk_xdock.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_dbqueue_single_transportation_break_bulk_xdock.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createDcToFFTransportationBreakBulkXdock() {
        facade.processXDocRequestCreatedOrValidated(
            XDocRequestStatus.VALIDATED,
            XDocTestingConstants.X_DOC_CREATE_DATA_BREAK_BULK_XDOCK
        );
    }

    /**
     * Для 1p VALIDATED - это подтверждение поставщиком. Сами перемещения уже созданы, но нужно отправить поставку в WMS
     */
    @DisplayName("Подтверждение поставщиком виртуального перемещения 1p товаров из РЦ в FF")
    @DatabaseSetup("/repository/transportation/after/xdoc_to_ff_transportation_1p.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_1p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persistItemsToFFTransportationValid1p() {
        facade.processXDocRequestCreatedOrValidated(
            XDocRequestStatus.VALIDATED,
            XDocTestingConstants.X_DOC_CREATE_DATA_1P
        );
    }

    @DisplayName("Вычисление, сохранение и отправка в FFWF даты приёмки на FF")
    @DatabaseSetup("/repository/transportation/xdoc_items_virtual_transportation.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/xdoc_inbound_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void computeSetAndSubmitFFInboundDate() {
        facade.computeSetAndSubmitFFInboundDate(new XDocSubmitInboundDateDto(
            XDocTestingConstants.REQUEST_ID,
            3L,
            XDocTestingConstants.X_DOC_PARTNER_ID,
            XDocTestingConstants.TARGET_PARTNER_ID,
            XDocTestingConstants.X_DOC_OUTBOUND_DATE
        ));

        Mockito.verify(ffwfClient).commitXDocFinalInboundDate(
            new XDocFinalInboundDateDTO()
                .setShopRequestId(XDocTestingConstants.REQUEST_ID)
                .setDate(TimeUtil.getOffsetTimeFromLocalDateTime(XDocTestingConstants.X_DOC_OUTBOUND_DATE.plusDays(3)))
        );
        Mockito.verifyNoMoreInteractions(ffwfClient);
    }

    @DisplayName("Не отправляем в FFWF даты приёмки на FF из-за статуса, отличного от NEW")
    @DatabaseSetup("/repository/transportation/xdoc_items_virtual_transportation.xml")
    @DatabaseSetup(
        value = "/repository/transportation/update/status_accepted_for_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @Test
    void ignoreComputeSetAndSubmitFFInboundDate() {
        facade.computeSetAndSubmitFFInboundDate(new XDocSubmitInboundDateDto(
            XDocTestingConstants.REQUEST_ID,
            3L,
            XDocTestingConstants.X_DOC_PARTNER_ID,
            XDocTestingConstants.TARGET_PARTNER_ID,
            XDocTestingConstants.X_DOC_OUTBOUND_DATE
        ));
        Mockito.verifyNoMoreInteractions(ffwfClient);
    }


    @DatabaseSetup("/repository/transportation/xdoc_to_ff_transportation_1p.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_dbqueue_single_transportation_1p_submit_inbound_date_to_ff.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void sendInboundToFF() {
        facade.sendInboundToFF(XDocTestingConstants.X_DOC_CREATE_DATA_1P);
    }
}
