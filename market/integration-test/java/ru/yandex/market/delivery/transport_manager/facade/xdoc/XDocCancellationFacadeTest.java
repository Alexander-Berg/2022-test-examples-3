package ru.yandex.market.delivery.transport_manager.facade.xdoc;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.CancellationUnitProducer;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;

class XDocCancellationFacadeTest extends AbstractContextualTest {
    public static final long ROOT_REQUEST_ID = 1L;
    @Autowired
    private XDocCancellationFacade facade;

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @Autowired
    private CancellationUnitProducer cancellationUnitProducer;

    @Autowired
    private TmPropertyService propertyService;

    @BeforeEach
    void setUp() {
        Mockito.doReturn(true).when(propertyService).getBoolean(TmPropertyKey.UNITS_CAN_BE_CANCELLED);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
        "/repository/transportation/cancel/xdoc_cancel_methods.xml"
    })
    @Test
    void cancelRequest() {
        facade.cancelRequest(ROOT_REQUEST_ID);

        Mockito.verify(cancellationUnitProducer).enqueue(21L);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_to_ff_transportations.xml",
    })
    @Test
    void cancelRequestMissing() {
        facade.cancelRequest(ROOT_REQUEST_ID);
        Mockito.verifyNoMoreInteractions(ffwfClient);
    }

    @DisplayName("Поставщик привёз товары на ФФ - отмена перемещения на РЦ")
    @DatabaseSetup({
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
        "/repository/transportation/cancel/xdoc_cancel_methods.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_dc_transportations_moved_to_ff_directly_substatus.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void cancelTransportationToDcIfGoodsMovedDirectlyToFF() {
        facade.cancelTransportationToDcIfGoodsMovedToFFDirectly(1L);

        Mockito.verify(cancellationUnitProducer).enqueue(21L);
    }
}
