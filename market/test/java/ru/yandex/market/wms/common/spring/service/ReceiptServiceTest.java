package ru.yandex.market.wms.common.spring.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.wms.common.model.enums.CounterName;
import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Loc;
import ru.yandex.market.wms.common.spring.dao.entity.PutawayZone;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptStatusHistory;
import ru.yandex.market.wms.common.spring.dao.entity.SkuCargotype;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.AnomalyLotDao;
import ru.yandex.market.wms.common.spring.dao.implementation.BomSkuDao;
import ru.yandex.market.wms.common.spring.dao.implementation.BuildingDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.PalletDao;
import ru.yandex.market.wms.common.spring.dao.implementation.PutawayZoneDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailIdentityDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailItemDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailStatusHistoryDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailUitDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptServicesDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptStatusHistoryDao;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuCargotypesDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;
import ru.yandex.market.wms.common.spring.dao.implementation.StorerDao;
import ru.yandex.market.wms.common.spring.dto.ReceiptDto;
import ru.yandex.market.wms.common.spring.enums.PutawayZoneType;
import ru.yandex.market.wms.common.spring.exception.ReceiptNotFoundException;
import ru.yandex.market.wms.common.spring.solomon.SolomonPushClient;
import ru.yandex.market.wms.core.base.response.GetChildContainersResponse;
import ru.yandex.market.wms.core.client.impl.CoreClientImpl;
import ru.yandex.market.wms.inbound_management.client.InboundManagementClient;
import ru.yandex.market.wms.receiving.dao.LocDao;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.spring.service.NamedCounterService.DEFAULT_WAREHOUSE_PREFIX;
import static ru.yandex.market.wms.common.spring.service.ReceiptService.CHECK_IDENTITIES_BBXD;
import static ru.yandex.market.wms.common.spring.service.ReceiptService.NEW_BBXD_FLOW;

public class ReceiptServiceTest extends BaseTest {

    private static final String RECEIPT_KEY = "0000012345";
    private static final String NONEXISTENT_RECEIPT_KEY = "0000000000";

    private ReceiptService receiptService;
    private ReceiptDao receiptDao;
    private ReceiptStatusHistoryDao receiptStatusHistoryDao;
    private Clock clock;
    private ReceiptDetailDao receiptDetailDao;
    private StorerDao storerDao;
    private CounterService counterService;
    private DbConfigService dbConfigService;
    private NamedCounterService namedCounterService;
    private AnomalyLotDao anomalyLotDao;
    private ReceiptDetailIdentityDao receiptDetailIdentityDao;
    private ReceiptDetailItemDao receiptDetailItemDao;
    private SkuCargotypesDAO skuCargotypesDAO;
    private InboundManagementClient inboundManagementClient;
    private SolomonPushClient solomonPushClient;
    private CoreClientImpl coreClient;
    private PalletDao palletDao;
    private LocDao locDao;
    private PutawayZoneDAO putawayZoneDAO;
    private ReceiptServicesDao receiptServicesDao;

    @BeforeEach
    public void setup() {
        super.setup();
        receiptDao = mock(ReceiptDao.class);
        receiptDetailDao = mock(ReceiptDetailDao.class);
        receiptStatusHistoryDao = mock(ReceiptStatusHistoryDao.class);
        clock = Clock.fixed(Instant.parse("2020-04-01T12:34:56.789Z"), ZoneOffset.UTC);
        counterService = mock(CounterService.class);
        dbConfigService = mock(DbConfigService.class);
        skuCargotypesDAO = mock(SkuCargotypesDAO.class);
        inboundManagementClient = mock(InboundManagementClient.class);
        solomonPushClient = mock(SolomonPushClient.class);
        receiptServicesDao = mock(ReceiptServicesDao.class);

        when(dbConfigService.getConfig(NamedCounterService.WAREHOUSE_PREFIX_CONFIG_NAME, DEFAULT_WAREHOUSE_PREFIX))
                .thenReturn("01");

        anomalyLotDao = mock(AnomalyLotDao.class);
        namedCounterService = new NamedCounterService(counterService, dbConfigService);
        storerDao = mock(StorerDao.class);
        receiptDetailIdentityDao = mock(ReceiptDetailIdentityDao.class);
        receiptDetailItemDao = mock(ReceiptDetailItemDao.class);
        coreClient = mock(CoreClientImpl.class);
        locDao = mock(LocDao.class);
        palletDao = mock(PalletDao.class);
        putawayZoneDAO = mock(PutawayZoneDAO.class);

        receiptService = new ReceiptService(
                receiptDao,
                receiptDetailDao,
                receiptStatusHistoryDao,
                mock(ReceiptDetailStatusHistoryDao.class),
                namedCounterService,
                storerDao,
                clock,
                anomalyLotDao,
                mock(SkuDaoImpl.class),
                mock(BomSkuDao.class),
                mock(ReceiptDetailUitDao.class),
                mock(ReceiptIdentityService.class),
                mock(TrailerService.class),
                mock(SecurityDataProvider.class),
                mock(BuildingDAO.class),
                receiptDetailIdentityDao,
                receiptDetailItemDao,
                mock(ReceiptAssortmentService.class),
                dbConfigService,
                skuCargotypesDAO,
                coreClient,
                palletDao,
                locDao,
                putawayZoneDAO,
                inboundManagementClient,
                solomonPushClient,
                receiptServicesDao);
    }

    @Test
    public void getEditDate() {
        String editDate = "2020-03-29 11:47:00";
        when(receiptDao.getEditDateAsString(RECEIPT_KEY)).thenReturn(editDate);
        String actualDate = receiptService.getEditDateString(RECEIPT_KEY);
        assertions.assertThat(actualDate).isEqualTo(editDate);
        verify(receiptDao).getEditDateAsString(RECEIPT_KEY);
        verifyNoMoreInteractions(receiptDao);
        verifyNoInteractions(receiptStatusHistoryDao);
    }

    @Test
    public void tryUpdateEditDate() {
        LocalDateTime editDate = LocalDateTime.parse("2020-03-29T11:47:00");
        String user = "TEST";
        String currentlyExpectedEditDate = "2020-03-24 12:43:22";
        when(receiptDao.tryUpdateEditDate(editDate, user, RECEIPT_KEY, currentlyExpectedEditDate)).thenReturn(1);
        int updated = receiptService.tryUpdateEditDate(editDate, user, RECEIPT_KEY, currentlyExpectedEditDate);
        assertions.assertThat(updated).isEqualTo(1);
        verify(receiptDao).tryUpdateEditDate(editDate, user, RECEIPT_KEY, currentlyExpectedEditDate);
        verifyNoMoreInteractions(receiptDao);
        verifyNoInteractions(receiptStatusHistoryDao);
    }

    @Test
    public void updateStatusWithoutEditDateAndEditWho() {
        ReceiptStatus status = ReceiptStatus.VERIFIED_CLOSED;
        String source = "ReceiptClose.ProcessStep";
        String user = "TEST";
        receiptService.updateStatusWithoutEditDateAndEditWho(RECEIPT_KEY, status, source, user);

        ReceiptStatusHistory historyEntity = ReceiptStatusHistory.builder()
                .receiptKey(RECEIPT_KEY)
                .receiptStatus(status)
                .source(source)
                .addDate(Instant.parse("2020-04-01T12:34:56.789Z"))
                .addWho(user)
                .build();

        verify(receiptStatusHistoryDao).insert(historyEntity);
        verify(receiptDao).updateStatusWithoutEditDateAndEditWho(RECEIPT_KEY, status);
        verifyNoMoreInteractions(receiptDao, receiptStatusHistoryDao);
    }

    @Test
    public void shouldFindReceiptByKey() {
        Receipt receiptEntity = Receipt.builder()
                .receiptKey(RECEIPT_KEY)
                .build();
        when(receiptDao.findReceiptByKey(RECEIPT_KEY)).thenReturn(Optional.of(receiptEntity));

        receiptService.getReceiptByKey(RECEIPT_KEY);

        verify(receiptDao).findReceiptByKey(RECEIPT_KEY);
        verifyNoMoreInteractions(receiptDao);
    }


    @Test
    public void shouldThrowExceptionWhenNotFindReceiptByKey() {
        when(receiptDao.findReceiptByKey(NONEXISTENT_RECEIPT_KEY)).thenReturn(Optional.empty());
        try {
            receiptService.getReceiptByKey(NONEXISTENT_RECEIPT_KEY);
            assertions.fail("NotFoundException expected");
        } catch (Exception ex) {
            assertions.assertThat(ex.getClass()).isEqualTo(ReceiptNotFoundException.class);
        }
    }

    @Test
    public void createReceipt() {
        when(counterService.getNextCounterValue(CounterName.RECEIPT)).thenReturn("KEY1");

        ReceiptDto dto = ReceiptDto.builder()
                .notes("TestComments")
                .type(ReceiptType.DEFAULT)
                .build();
        Receipt r = receiptService.createOrUpdateReceipt(dto, "TEST");

        assertions.assertThat(r.getNotes()).isEqualTo("TestComments");
        assertions.assertThat(r.getReceiptKey()).isEqualTo("KEY1");
        assertions.assertThat(r.getStatus()).isEqualTo(ReceiptStatus.NEW);
        assertions.assertThat(r.getType()).isEqualTo(ReceiptType.DEFAULT);
        verify(receiptStatusHistoryDao).insert(any(ReceiptStatusHistory.class));
    }

    @Test
    void deleteWhenThereAreNoReceiptDetails() {
        ReceiptDetailItemDao receiptDetailItemDaoMock = mock(ReceiptDetailItemDao.class);
        final String receiptKey = "0000005555";
        when(receiptDetailDao.getReceiptDetails(receiptKey)).thenReturn(Collections.emptyList());

        Receipt receipt = Receipt.builder()
                .receiptKey(receiptKey)
                .build();
        receiptService.deleteRegistry(receipt);

        Mockito.verify(receiptDetailItemDaoMock, times(0))
                .deleteByReceiptKey(receipt.getReceiptKey());
    }

    @Test
    void checkIdentities1() {
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");
        Receipt receipt = Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.DEFAULT)
                .build();
        when(skuCargotypesDAO.findBySku(skuId))
                .thenReturn(Set.of(SkuCargotype.builder()
                        .storer(skuId.getStorerKey())
                        .sku(skuId.getSku())
                        .cargotype(980)
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build()));
        assertions.assertThat(receiptService.checkIdentities(receipt.getType(), skuId))
                .isTrue();
    }

    @Test
    void checkIdentities2() {
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");
        Receipt receipt = Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.DEFAULT)
                .build();
        when(skuCargotypesDAO.findBySku(skuId))
                .thenReturn(Set.of(SkuCargotype.builder()
                        .storer(skuId.getStorerKey())
                        .sku(skuId.getSku())
                        .cargotype(990)
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build()));
        assertions.assertThat(receiptService.checkIdentities(receipt.getType(), skuId))
                .isTrue();
    }

    @Test
    void checkIdentities3() {
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");
        Receipt receipt = Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.XDOCK)
                .build();
        when(dbConfigService.getConfigAsBoolean(CHECK_IDENTITIES_BBXD, true))
                .thenReturn(false);
        when(skuCargotypesDAO.findBySku(skuId))
                .thenReturn(Set.of(SkuCargotype.builder()
                        .storer(skuId.getStorerKey())
                        .sku(skuId.getSku())
                        .cargotype(985)
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build()));
        assertions.assertThat(receiptService.checkIdentities(receipt.getType(), skuId))
                .isFalse();
    }

    @Test
    void checkIdentities4() {
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");
        Receipt receipt = Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.XDOCK)
                .build();
        when(dbConfigService.getConfigAsBoolean(CHECK_IDENTITIES_BBXD, true))
                .thenReturn(true);
        when(skuCargotypesDAO.findBySku(skuId))
                .thenReturn(Set.of(SkuCargotype.builder()
                        .storer(skuId.getStorerKey())
                        .sku(skuId.getSku())
                        .cargotype(985)
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build()));
        assertions.assertThat(receiptService.checkIdentities(receipt.getType(), skuId))
                .isTrue();
    }

    @Test
    void checkIdentities5() {
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");
        Receipt receipt = Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.XDOCK)
                .build();
        when(dbConfigService.getConfigAsBoolean(CHECK_IDENTITIES_BBXD, true))
                .thenReturn(true);
        when(skuCargotypesDAO.findBySku(skuId))
                .thenReturn(Set.of(SkuCargotype.builder()
                        .storer(skuId.getStorerKey())
                        .sku(skuId.getSku())
                        .cargotype(990)
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build()));
        assertions.assertThat(receiptService.checkIdentities(receipt.getType(), skuId))
                .isFalse();
    }

    @Test
    void getStartSortingAllowedNotXdock() {
        boolean result = receiptService.getStartSortingAllowed(null, ReceiptType.DEFAULT);
        assertions.assertThat(result).isFalse();
    }

    @Test
    void getStartSortingAllowedNoBoxesOnPallet() {
        String palletId = "PLT123";
        when(coreClient.getChildContainers(palletId))
                .thenReturn(new GetChildContainersResponse(Collections.emptyList()));
        when(dbConfigService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(true);

        boolean result = receiptService.getStartSortingAllowed(palletId, ReceiptType.XDOCK);
        assertions.assertThat(result).isFalse();
    }

    @Test
    void getStartSortingAllowedOldFlow() {
        String palletId = "PLT123";
        when(coreClient.getChildContainers(palletId))
                .thenReturn(new GetChildContainersResponse(List.of("BOX1")));
        when(dbConfigService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(false);

        boolean result = receiptService.getStartSortingAllowed(palletId, ReceiptType.XDOCK);
        assertions.assertThat(result).isFalse();
    }

    @Test
    void getStartSortingAllowedNoBoxSerials() {
        String palletId = "PLT123";
        when(coreClient.getChildContainers(palletId))
                .thenReturn(new GetChildContainersResponse(List.of("BOX1")));
        when(dbConfigService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(true);
        when(palletDao.getCurrentLocation(palletId)).thenReturn(Optional.empty());
        assertions.assertThatThrownBy(() -> receiptService.getStartSortingAllowed(palletId, ReceiptType.XDOCK))
                .hasMessageContaining("На отсканированной паллете больше нет коробок");
    }

    @Test
    void getStartSortingAllowedNotSorterZone() {
        String palletId = "PLT123";
        String loc = "LOC123";
        String zone = "ZONE";

        when(coreClient.getChildContainers(palletId))
                .thenReturn(new GetChildContainersResponse(List.of("BOX1")));
        when(dbConfigService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(true);
        when(palletDao.getCurrentLocation(palletId)).thenReturn(Optional.of(loc));
        when(locDao.getLocation(loc)).thenReturn(
                Optional.of(Loc.builder()
                        .loc(loc)
                        .putawayzone(zone)
                        .build()));
        when(putawayZoneDAO.find(zone)).thenReturn(
                PutawayZone.builder()
                        .putawayZone(zone)
                        .type(PutawayZoneType.BBXD_RECEIVING)
                        .build());

        boolean result = receiptService.getStartSortingAllowed(palletId, ReceiptType.XDOCK);
        assertions.assertThat(result).isFalse();
    }

    @Test
    void getStartSortingAllowedOk() {
        String palletId = "PLT123";
        String loc = "LOC123";
        String zone = "ZONE";

        when(coreClient.getChildContainers(palletId))
                .thenReturn(new GetChildContainersResponse(List.of("BOX1")));
        when(dbConfigService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(true);
        when(palletDao.getCurrentLocation(palletId)).thenReturn(Optional.of(loc));
        when(locDao.getLocation(loc)).thenReturn(
                Optional.of(Loc.builder()
                        .loc(loc)
                        .putawayzone(zone)
                        .build()));
        when(putawayZoneDAO.find(zone)).thenReturn(
                PutawayZone.builder()
                        .putawayZone(zone)
                        .type(PutawayZoneType.BBXD_SORTER)
                        .build());

        boolean result = receiptService.getStartSortingAllowed(palletId, ReceiptType.XDOCK);
        assertions.assertThat(result).isTrue();
    }
}
