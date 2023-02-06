package ru.yandex.market.wms.common.spring.service.unit;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.enums.CounterName;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.LotId;
import ru.yandex.market.wms.common.spring.dao.entity.LotIdDetail;
import ru.yandex.market.wms.common.spring.dao.entity.LotIdHeader;
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.dao.implementation.LotIdDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.LotIdHeaderDao;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;
import ru.yandex.market.wms.common.spring.pojo.SourceLineKey;
import ru.yandex.market.wms.common.spring.service.CounterService;
import ru.yandex.market.wms.common.spring.service.LotIdService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class LotIdServiceTest extends BaseTest {

    private static final String LOT = "STAGE";
    private static final String ID = "CART123";
    private static final String SOURCE_KEY = "Receipt";
    private static final String LINE_NUMBER = "00001";
    private static final String STORER_KEY = "486752";
    private static final String SKU = "SKU";
    private static final String SERIAL_NUMBER = "1234567890";
    private static final SourceLineKey SOURCE_LINE_KEY = new ReceiptDetailKey(SOURCE_KEY, LINE_NUMBER);
    private static final SerialInventory SERIAL_INVENTORY = SerialInventory.builder()
            .serialNumber(SERIAL_NUMBER)
            .storerKey(STORER_KEY)
            .sku(SKU)
            .lot(LOT)
            .loc("DAMAGE")
            .id(ID)
            .quantity(BigDecimal.ONE)
            .build();
    private static final String LOT_ID_KEY = "0000023456";
    private static final String USER = "TEST";

    private LotIdService lotIdService;
    private LotIdHeaderDao lotIdHeaderDao;
    private LotIdDetailDao lotIdDetailDao;
    private CounterService counterService;

    @BeforeEach
    public void setup() {
        super.setup();
        lotIdHeaderDao = mock(LotIdHeaderDao.class);
        lotIdDetailDao = mock(LotIdDetailDao.class);
        counterService = mock(CounterService.class);
        lotIdService = new LotIdService(lotIdHeaderDao, lotIdDetailDao, counterService);
    }

    @AfterEach
    public void resetMocks() {
        reset(lotIdHeaderDao, lotIdDetailDao, counterService);
    }

    @Test
    public void createLotId() {
        when(counterService.getNextCounterValue(CounterName.LOT_ID_HEADER)).thenReturn(LOT_ID_KEY);

        LotId lotId = lotIdService.convertToInboundLotId(SOURCE_LINE_KEY, SERIAL_INVENTORY, USER);
        lotIdService.createLotId(lotId);

        verify(counterService).getNextCounterValue(CounterName.LOT_ID_HEADER);
        verify(lotIdHeaderDao).insert(Collections.singletonList(createLotIdHeader()));
        verify(lotIdDetailDao).insert(Collections.singletonList(createLotIdDetail("00001")));
        verifyNoMoreInteractions(lotIdHeaderDao, lotIdDetailDao, counterService);
    }

    private LotIdHeader createLotIdHeader() {
        return LotIdHeader.builder()
                .lotIdKey(LOT_ID_KEY)
                .storerKey(STORER_KEY)
                .sku(SKU)
                .ioFlag("I")
                .lot(LOT)
                .id(ID)
                .status("0")
                .sourceKey(SOURCE_KEY)
                .sourceLineNumber(LINE_NUMBER)
                .addWho(USER)
                .editWho(USER)
                .build();
    }

    private LotIdDetail createLotIdDetail(String lotIdLineNumber) {
        return LotIdDetail.builder()
                .lotIdKey(LOT_ID_KEY)
                .lotIdLineNumber(lotIdLineNumber)
                .serialNumber(SERIAL_NUMBER)
                .sku(SKU)
                .ioFlag("I")
                .lot(LOT)
                .id(ID)
                .sourceKey(SOURCE_KEY)
                .sourceLineNumber(LINE_NUMBER)
                .quantity(BigDecimal.ONE)
                .addWho(USER)
                .editWho(USER)
                .build();
    }
}
