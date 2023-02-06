package ru.yandex.market.wms.common.spring.service.unit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.config.LostWriteOffServiceTestConfig;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.common.spring.service.LostWriteOffService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DatabaseSetup(value = "/db/dao/serial-inventory-lost/lost-db.xml")
@DatabaseSetup(value = "/db/service/lost-writeoff/before-writeoff.xml")
@DatabaseSetup(value = "/db/service/lost-writeoff/before-writeoff-archive.xml", connection = "archiveWmwhseConnection")
@SpringBootTest(classes = {IntegrationTestConfig.class, LostWriteOffServiceTestConfig.class})
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"wmwhseConnection", "enterpriseConnection", "scprdd1DboConnection",
                "archiveWmwhseConnection"})
public class LostWriteOffServiceTest extends IntegrationTest {

    @Autowired
    LostWriteOffService lostWriteOffService;

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        lostWriteOffService.setWriteOffInNewTransaction(false);
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/lost-db-after-operlost-writeoff.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/db/dao/lost-log/after-operlost-log.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/db/service/lost-writeoff/after-operlost-writeoff.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/db/service/lost-writeoff/after-operlost-writeoff-archive.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "archiveWmwhseConnection")
    public void writeOffOperLostTest() {
        processOrder(OrderType.OUTBOUND_OPER_LOST_INVENTARIZATION);
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/lost-db-after-fixlost-writeoff.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/db/dao/lost-log/after-fixlost-log.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/db/service/lost-writeoff/after-fixlost-writeoff.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/db/service/lost-writeoff/after-fixlost-writeoff-archive.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "archiveWmwhseConnection")
    public void writeOffFixLostTest() {
        processOrder(OrderType.OUTBOUND_FIX_LOST_INVENTARIZATION);
    }

    @Test
    @ExpectedDatabase(value = "/db/service/lost-writeoff/after-writeoff-exception.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void writeOffWrongTypeTest()  {
        Exception e = assertThrows(Exception.class, () -> processOrder(OrderType.BATCH_ORDER));
        assertEquals("Ошибка при списании LOST: Тип заказа 100 не поддерживается для списания SerialInventory",
                e.getMessage());
    }

    private void processOrder(OrderType orderType) {
        OrderDTO order = new OrderDTO();
        order.setOrderkey("ORDER_1");
        order.setExternorderkey("EXTERN_1");
        order.setType(orderType.getCode());
        order.setReceiptkey("");
        lostWriteOffService.writeOff(order);
    }
}
