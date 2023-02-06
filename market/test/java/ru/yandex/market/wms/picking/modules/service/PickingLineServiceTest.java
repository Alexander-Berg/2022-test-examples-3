package ru.yandex.market.wms.picking.modules.service;

import java.util.Arrays;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.model.enums.LocationType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Loc;
import ru.yandex.market.wms.common.spring.exception.IncorrectFinalLocationException;
import ru.yandex.market.wms.transportation.client.TransportationClient;
import ru.yandex.market.wms.transportation.core.domain.Cursor;
import ru.yandex.market.wms.transportation.core.domain.TransportOrderStatus;
import ru.yandex.market.wms.transportation.core.model.response.GetTransportOrdersResponseWithCursor;
import ru.yandex.market.wms.transportation.core.model.response.TransportOrderResourceContent;

import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class PickingLineServiceTest extends IntegrationTest {

    @Autowired
    private PickingLineService pickingLineService;

    @Autowired
    @MockBean
    private TransportationClient transportationClient;

    @BeforeEach
    void reset() {
        Mockito.reset(transportationClient);
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/1/before.xml")
    public void getLineByOrder_pickTo_returnsConfig() {
        Loc loc = Loc.builder().locationType(LocationType.PICK_TO).build();
        Assertions.assertEquals("LINE", pickingLineService.getLineByOrder(Collections.singletonList("2"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/2/before.xml")
    public void getLineByOrder_stOutBuf_returnsDefaultLine() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("DEFAULT_LINE",
                pickingLineService.getLineByOrder(Collections.singletonList("2"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/3/before.xml")
    public void getLineByOrder_anotherContainerOnLine_returnLineFromWave() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("CONSLOC",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/4/before.xml")
    public void getLineByOrder_waveLine_returnWaveLine() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("CONS03",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/nonsort/before.xml")
    public void getLineByOrder_nonSortLine() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("PICKTO_NS",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/nonsort/before-with-conv.xml")
    @DatabaseSetup(value = "/service/get-line-by-order/nonsort/singles-conveyor-enabled.xml", type = REFRESH)
    public void getLineByOrder_nonSortLine_with_conveyor() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("NS-PACK-4",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/nonsort/before-no-conv-locs.xml")
    @DatabaseSetup(value = "/service/get-line-by-order/nonsort/singles-conveyor-enabled.xml", type = REFRESH)
    public void getLineByOrder_nonSortLine_with_conveyor_no_conveyorloc() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("PICKTO_NS",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }

    @Test
    public void getLineByOrder_incorrectLocType_throwException() {
        Loc loc = Loc.builder().locationType(LocationType.CONSOLIDATION).build();
        Assertions.assertThrows(IncorrectFinalLocationException.class,
                () -> pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/5/before.xml")
    @ExpectedDatabase(value = "/service/get-line-by-order/5/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getLineByOrder_lineIsUpdated() {
        GetTransportOrdersResponseWithCursor response =
                new GetTransportOrdersResponseWithCursor(new Cursor(""), Arrays.asList(
                        TransportOrderResourceContent.builder().status(TransportOrderStatus.FAILED).build(),
                        TransportOrderResourceContent.builder().status(TransportOrderStatus.FINISHED).build()));

        when(transportationClient.getTransportOrders(anyInt(), anyString(), any(), any(), any())).thenReturn(response);
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();

        Assertions.assertEquals("DOOR", pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
        Assertions.assertEquals("DOOR", pickingLineService.getLineByOrder(Collections.singletonList("2"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/6/before.xml")
    @ExpectedDatabase(value = "/service/get-line-by-order/6/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getLineByOrder_hasActiveOrder_returnsDefaultLine() {
        GetTransportOrdersResponseWithCursor response =
                new GetTransportOrdersResponseWithCursor(new Cursor(""), Arrays.asList(
                        TransportOrderResourceContent.builder().status(TransportOrderStatus.FAILED).build(),
                        TransportOrderResourceContent.builder().status(TransportOrderStatus.IN_PROGRESS).build()));

        when(transportationClient.getTransportOrders(anyInt(), anyString(), any(), any(), any())).thenReturn(response);
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();

        Assertions.assertEquals("DEFAULT_LINE",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
        Assertions.assertEquals("DEFAULT_LINE",
                pickingLineService.getLineByOrder(Collections.singletonList("2"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/7/before.xml")
    public void getLineByOrder_manualSortingStation_returnsSpecialNOK() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("MB1_NOK-02",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/8/before.xml")
    public void getLineByOrder_manualSortingStation_withoutAlternativeLine() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("CONS03",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }

    @Test
    @DatabaseSetup("/service/get-line-by-order/9/before.xml")
    public void getLineByOrder_returnsDefaultWithdrawalLine() {
        Loc loc = Loc.builder().locationType(LocationType.ST_OUT_BUF).build();
        Assertions.assertEquals("DEFAULT_WITHDRAWAL_LINE",
                pickingLineService.getLineByOrder(Collections.singletonList("1"), "TM1", loc));
    }
}
