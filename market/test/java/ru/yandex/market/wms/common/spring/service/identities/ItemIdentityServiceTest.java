package ru.yandex.market.wms.common.spring.service.identities;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.InstanceIdentity;
import ru.yandex.market.wms.common.spring.dao.implementation.InstanceIdentityDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailIdentityDao;
import ru.yandex.market.wms.common.spring.service.LotIdService;
import ru.yandex.market.wms.common.spring.service.SerialInventoryService;
import ru.yandex.market.wms.common.spring.utils.CisParser;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemIdentityServiceTest extends BaseTest {

    private LotIdService lotIdService;
    private OrderDao orderDao;
    private InstanceIdentityDAO instanceIdentityDAO;
    private ReceiptDetailIdentityDao receiptDetailIdentityDao;
    private SerialInventoryService serialInventoryService;
    private CisParser cisParser;
    private ItemIdentityService service;

    @BeforeEach
    void setUp() {
        this.lotIdService = mock(LotIdService.class);
        this.orderDao = mock(OrderDao.class);
        this.instanceIdentityDAO = mock(InstanceIdentityDAO.class);
        this.serialInventoryService = mock(SerialInventoryService.class);
        this.cisParser = mock(CisParser.class);
        this.receiptDetailIdentityDao = mock(ReceiptDetailIdentityDao.class);
        this.service = new ItemIdentityService(lotIdService, orderDao, instanceIdentityDAO, receiptDetailIdentityDao,
                        serialInventoryService, cisParser);
    }

    @AfterEach
    void after() {
        Mockito.reset(lotIdService, orderDao, instanceIdentityDAO, serialInventoryService, cisParser);
    }

    @Test
    void findIdentities_collectSerialsFlag_false() {
        List<String> serials = List.of("123");
        when(instanceIdentityDAO.findByInstanceList(serials)).thenReturn(List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder()
                                .instance("123")
                                .identity("ID")
                                .type("IMEI")
                                .build())
                        .build()
        ));
        List<InstanceIdentity> result = service.findIdentities(serials, false);
        assertions.assertThat(result).hasOnlyOneElementSatisfying(id -> {
            assertions.assertThat(id.getPk().getInstance()).isEqualTo("123");
            assertions.assertThat(id.getPk().getIdentity()).isEqualTo("ID");
        });
    }

    @Test
    void findIdentities_collectSerialsFlag_true() {
        List<String> serials = List.of("123", "456");
        when(instanceIdentityDAO.findByInstanceList(serials)).thenReturn(List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder()
                                .instance("123")
                                .identity("ID")
                                .type("IMEI")
                                .build())
                        .build()
        ));
        List<InstanceIdentity> result = service.findIdentities(serials, true);
        assertions.assertThat(result).anySatisfy(id -> {
            assertions.assertThat(id.getPk().getInstance()).isEqualTo("123");
            assertions.assertThat(id.getPk().getIdentity()).isEqualTo("ID");
            assertions.assertThat(id.isNoIdentities()).isFalse();
        });
        assertions.assertThat(result).anySatisfy(id -> {
            assertions.assertThat(id.getPk().getInstance()).isEqualTo("456");
            assertions.assertThat(id.isNoIdentities()).isTrue();
        });

    }
}
