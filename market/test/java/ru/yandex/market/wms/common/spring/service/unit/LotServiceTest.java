package ru.yandex.market.wms.common.spring.service.unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.model.enums.InventoryHoldType;
import ru.yandex.market.wms.common.model.enums.LotStatus;
import ru.yandex.market.wms.common.pojo.ShelfLifeDates;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.InventoryHold;
import ru.yandex.market.wms.common.spring.dao.entity.Lot;
import ru.yandex.market.wms.common.spring.dao.entity.LotAttribute;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.implementation.InventoryHoldDao;
import ru.yandex.market.wms.common.spring.dao.implementation.LotAttributeDao;
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao;
import ru.yandex.market.wms.common.spring.pojo.LotAggregatedFields;
import ru.yandex.market.wms.common.spring.pojo.LotCreationResult;
import ru.yandex.market.wms.common.spring.pojo.LotInputData;
import ru.yandex.market.wms.common.spring.pojo.SkuDimensions;
import ru.yandex.market.wms.common.spring.service.LotService;
import ru.yandex.market.wms.common.spring.service.NamedCounterService;
import ru.yandex.market.wms.common.utils.InventoryHoldStatusUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.spring.utils.EntityCreationUtils.createSku;

public class LotServiceTest extends BaseTest {

    private static final String USER = "TEST";

    private LotService lotService;
    private LotDao lotDao;
    private LotAttributeDao lotAttributeDao;
    private InventoryHoldDao inventoryHoldDao;
    private NamedCounterService namedCounterService;

    @BeforeEach
    public void setup() {
        super.setup();
        lotDao = mock(LotDao.class);
        lotAttributeDao = mock(LotAttributeDao.class);
        inventoryHoldDao = mock(InventoryHoldDao.class);
        namedCounterService = mock(NamedCounterService.class);
        lotService = new LotService(lotDao, lotAttributeDao, inventoryHoldDao, namedCounterService);
    }

    @Test
    public void findLot() {
        Sku sku = createSku();
        String expectedLotValue = "0000012345";
        String expectedLotValueOnReceiving = "0000012345";

        Lot lot = Lot.builder()
            .storerKey(sku.getStorerKey())
            .sku(sku.getSku())
            .lot(expectedLotValue)
            .lotOnReceiving(expectedLotValueOnReceiving)
            .build();

        Set<InventoryHoldStatus> holdStatuses = Set.of(InventoryHoldStatusUtils
                .getHoldStatusConsideringExpiration(InventoryHoldStatus.DAMAGE, true));

        LotInputData lotInputData = LotInputData.builder()
                .sku(sku)
                .creationDateTime(Instant.parse("2020-04-18T12:00:00.000Z"))
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .inventoryHoldStatuses(holdStatuses)
                .surplus(false)
                .qty(BigDecimal.ONE)
                .originalLot("0000012345")
                .build();

        when(lotDao.findAnyLot(lotInputData, LotStatus.HOLD, List.of(InventoryHoldStatus.DAMAGE)))
            .thenReturn(Optional.of(lot));

        LotCreationResult lotCreationResult = lotService.getAndUpdateLot(lotInputData, USER).get();

        assertions.assertThat(lotCreationResult.getLot()).isEqualTo(expectedLotValue);

        verify(lotDao).findAnyLot(lotInputData, LotStatus.HOLD, List.of(InventoryHoldStatus.DAMAGE));
        verify(lotDao).addToAggregatedFields(ImmutableMap.of(
                expectedLotValue, createLotAggregatedFields(1, 1, sku.getDimensions())),
                USER);
        verifyNoInteractions(inventoryHoldDao, namedCounterService, lotAttributeDao);
    }

    @Test
    public void createWithHoldLotStatus() {
        Sku sku = createSku();
        Instant creationDateTime = Instant.parse("2020-04-18T12:00:00.000Z");
        Instant expirationDateTime = Instant.parse("2020-04-25T15:00:00.000Z");
        ShelfLifeDates shelfLifeDates = new ShelfLifeDates(creationDateTime, expirationDateTime);
        String expectedLotValue = "0000012345";
        String expectedInventoryHoldKey = "0001234567";
        Set<InventoryHoldStatus> holdStatuses = Set.of(InventoryHoldStatusUtils
            .getHoldStatusConsideringExpiration(InventoryHoldStatus.DAMAGE_DISPOSAL, false));
        LotInputData lotInputData = LotInputData.builder()
                .sku(sku)
                .inventoryHoldStatuses(holdStatuses)
                .creationDateTime(creationDateTime)
                .expirationDateTime(expirationDateTime)
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();

        when(namedCounterService.getNextLot()).thenReturn(expectedLotValue);
        when(namedCounterService.getNextInventoryHoldKey()).thenReturn(expectedInventoryHoldKey);

        LotCreationResult lotCreationResult = lotService.createLot(lotInputData, USER);

        assertions.assertThat(lotCreationResult.getLot()).isEqualTo(expectedLotValue);

        verify(namedCounterService).getNextLot();
        verify(namedCounterService).getNextInventoryHoldKey();

        Lot expectedLot = createLot(expectedLotValue, sku, LotStatus.HOLD, 1);
        LotAttribute expectedLotAttribute = createLotAttribute(expectedLotValue, sku, shelfLifeDates, false);
        verify(lotDao).createLots(Collections.singletonList(expectedLot));
        verify(lotAttributeDao).save(DatabaseSchema.WMWHSE1, Collections.singletonList(expectedLotAttribute));

        ArgumentCaptor<List<InventoryHold>> holdCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryHoldDao).insert(holdCaptor.capture());
        List<InventoryHold> insertedHolds = holdCaptor.getValue();
        assertions.assertThat(insertedHolds).hasSize(1);
        InventoryHold insertedHold = insertedHolds.get(0);
        InventoryHold expectedHold =
            createInventoryHold(expectedInventoryHoldKey, expectedLotValue, InventoryHoldStatus.DAMAGE_DISPOSAL);
        assertions.assertThat(insertedHold).isEqualTo(expectedHold);

        verifyNoMoreInteractions(lotDao, lotAttributeDao, inventoryHoldDao, namedCounterService);
    }

    @Test
    public void createWithOkInventoryHoldAndExpired() {
        Sku sku = createSku();
        Instant creationDateTime = Instant.parse("2020-04-18T12:00:00.000Z");
        Instant expirationDateTime = Instant.parse("2020-04-25T15:00:00.000Z");
        ShelfLifeDates shelfLifeDates = new ShelfLifeDates(creationDateTime, expirationDateTime);
        String expectedLotValue = "0000012345";
        String expectedInventoryHoldKey = "0001234567";
        Set<InventoryHoldStatus> holdStatuses = Set.of(InventoryHoldStatusUtils
            .getHoldStatusConsideringExpiration(InventoryHoldStatus.OK, true));
        LotInputData lotInputData = LotInputData.builder()
                .sku(sku)
                .inventoryHoldStatuses(holdStatuses)
                .creationDateTime(creationDateTime)
                .expirationDateTime(expirationDateTime)
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();

        when(namedCounterService.getNextLot()).thenReturn(expectedLotValue);
        when(namedCounterService.getNextInventoryHoldKey()).thenReturn(expectedInventoryHoldKey);

        LotCreationResult lotCreationResult = lotService.createLot(lotInputData, USER);

        assertions.assertThat(lotCreationResult.getLot()).isEqualTo(expectedLotValue);

        verify(namedCounterService).getNextLot();
        verify(namedCounterService).getNextInventoryHoldKey();

        Lot expectedLot = createLot(expectedLotValue, sku, LotStatus.HOLD, 1);
        LotAttribute expectedLotAttribute = createLotAttribute(expectedLotValue, sku, shelfLifeDates, false);
        verify(lotDao).createLots(Collections.singletonList(expectedLot));
        verify(lotAttributeDao).save(DatabaseSchema.WMWHSE1, Collections.singletonList(expectedLotAttribute));

        ArgumentCaptor<List<InventoryHold>> holdCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryHoldDao).insert(holdCaptor.capture());
        List<InventoryHold> insertedHolds = holdCaptor.getValue();
        assertions.assertThat(insertedHolds).hasSize(1);
        InventoryHold insertedHold = insertedHolds.get(0);
        InventoryHold expectedHold =
            createInventoryHold(expectedInventoryHoldKey, expectedLotValue, InventoryHoldStatus.EXPIRED);
        assertions.assertThat(insertedHold).isEqualTo(expectedHold);

        verifyNoMoreInteractions(lotDao, lotAttributeDao, inventoryHoldDao, namedCounterService);
    }

    @Test
    public void createWithOkLotStatus() {
        Sku sku = createSku();
        Instant creationDateTime = Instant.parse("2020-04-18T12:00:00.000Z");
        Instant expirationDateTime = Instant.parse("2020-04-25T15:00:00.000Z");
        ShelfLifeDates shelfLifeDates = new ShelfLifeDates(creationDateTime, expirationDateTime);
        String expectedLotValue = "0000012345";
        String expectedInventoryHoldKey = "0001234567";
        Set<InventoryHoldStatus> holdStatuses = Set.of(InventoryHoldStatusUtils
            .getHoldStatusConsideringExpiration(InventoryHoldStatus.OK, false));
        LotInputData lotInputData = LotInputData.builder()
                .sku(sku)
                .inventoryHoldStatuses(holdStatuses)
                .creationDateTime(creationDateTime)
                .expirationDateTime(expirationDateTime)
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();

        when(namedCounterService.getNextLot()).thenReturn(expectedLotValue);
        when(namedCounterService.getNextInventoryHoldKey()).thenReturn(expectedInventoryHoldKey);

        LotCreationResult lotCreationResult = lotService.createLot(lotInputData, USER);

        assertions.assertThat(lotCreationResult.getLot()).isEqualTo(expectedLotValue);

        verify(namedCounterService).getNextLot();

        Lot expectedLot = createLot(expectedLotValue, sku, LotStatus.OK, 0);
        LotAttribute expectedLotAttribute = createLotAttribute(expectedLotValue, sku, shelfLifeDates, false);
        verify(lotDao).createLots(Collections.singletonList(expectedLot));
        verify(lotAttributeDao).save(DatabaseSchema.WMWHSE1, Collections.singletonList(expectedLotAttribute));

        verifyNoMoreInteractions(lotDao, lotAttributeDao, namedCounterService);
        verifyNoInteractions(inventoryHoldDao);
    }

    private Lot createLot(String lot, Sku sku, LotStatus lotStatus, long quantityOnHold) {
        return Lot.builder()
            .lot(lot)
            .sku(sku.getSku())
            .storerKey(sku.getStorerKey())
            .lotStatus(lotStatus)
            .addWho(USER)
            .editWho(USER)
            .lotAggregatedFields(LotAggregatedFields.builder()
                .quantity(1)
                .quantityOnHold(quantityOnHold)
                .dimensions(sku.getDimensions())
                .build())
            .build();
    }

    private LotAttribute createLotAttribute(String lot, Sku sku, ShelfLifeDates shelfLifeDates, boolean surplus) {
        return LotAttribute.builder()
            .storerKey(sku.getStorerKey())
            .sku(sku.getSku())
            .lot(lot)
            .expirationDateTime(shelfLifeDates.getExpirationDateTime())
            .creationDateTime(shelfLifeDates.getCreationDateTime())
            .surplus(surplus)
            .putAwayClass(sku.getPutAwayClass())
            .addWho(USER)
            .editWho(USER)
            .originalLot(lot)
            .build();
    }

    private InventoryHold createInventoryHold(String inventoryHoldKey, String lot, InventoryHoldStatus status) {
        return InventoryHold.builder()
            .inventoryHoldKey(inventoryHoldKey)
            .lot(lot)
            .loc("")
            .id("")
            .hold(InventoryHoldType.HOLD)
            .status(status)
            .addWho(USER)
            .editWho(USER)
            .build();
    }

    private LotAggregatedFields createLotAggregatedFields(long quantity, long quantityOnHold,
                                                          SkuDimensions dimensions) {
        return LotAggregatedFields.builder()
            .quantity(quantity)
            .quantityOnHold(quantityOnHold)
            .dimensions(dimensions)
            .build();
    }
}
