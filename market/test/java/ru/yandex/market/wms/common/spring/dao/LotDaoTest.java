package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.model.enums.LotStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Lot;
import ru.yandex.market.wms.common.spring.dao.entity.LotWithUnnamedAttributes;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.UnnamedLotAttributes;
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao;
import ru.yandex.market.wms.common.spring.pojo.LotAggregatedFields;
import ru.yandex.market.wms.common.spring.pojo.LotInputData;
import ru.yandex.market.wms.common.spring.pojo.SkuDimensions;
import ru.yandex.market.wms.common.spring.utils.columnFilters.EditDateFilter;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static ru.yandex.market.wms.common.spring.utils.EntityCreationUtils.createLotAggregatedFields;

public class LotDaoTest extends IntegrationTest {

    @Autowired
    private LotDao lotDao;

    @Test
    @DatabaseSetup("/db/dao/lot/before/before-for-find-without-inventory-hold.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before-for-find-without-inventory-hold.xml", assertionMode =
            NON_STRICT)
    public void findAnyLotWithoutInventoryHoldStatusesIfExists() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(Collections.singleton(InventoryHoldStatus.OK))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK, Collections.emptyList());
        assertions.assertThat(maybeLot).isPresent();
        assertFoundLotIsCorrect(maybeLot.get(), "0000012346");
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before-for-find-without-inventory-hold.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before-for-find-without-inventory-hold.xml", assertionMode =
            NON_STRICT)
    public void findAnyLotWithoutInventoryHoldStatusesIfNotExists() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001457").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(Collections.singleton(InventoryHoldStatus.OK))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK, Collections.emptyList());
        assertions.assertThat(maybeLot).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before.xml", assertionMode = NON_STRICT)
    public void findAnyLotWithShelfLifesWhenItExists() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(Collections.singleton(InventoryHoldStatus.OK))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Collections.singletonList(InventoryHoldStatus.DAMAGE));
        assertions.assertThat(maybeLot).isPresent();
        assertFoundLotIsCorrect(maybeLot.get(), "0000012345");
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before.xml", assertionMode = NON_STRICT)
    public void findAnyLotWithShelfLifesWhenItNotExistsBecauseOfLotStatus() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(Collections.singleton(InventoryHoldStatus.DAMAGE))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.HOLD,
                Collections.singletonList(InventoryHoldStatus.DAMAGE));

        assertions.assertThat(maybeLot).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before-without-shelf-lifes.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before-without-shelf-lifes.xml", assertionMode = NON_STRICT)
    public void findAnyLotWithoutShelfLifesWhenItExists() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .inventoryHoldStatuses(Collections.singleton(InventoryHoldStatus.DAMAGE))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Collections.singletonList(InventoryHoldStatus.DAMAGE));

        assertions.assertThat(maybeLot).isPresent();
        assertFoundLotIsCorrect(maybeLot.get(), "0000012345");
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before.xml", assertionMode = NON_STRICT)
    public void findAnyLotWithShelfLifesWhenItIsNotExistBecauseOfSurplus() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(ImmutableSet.of(InventoryHoldStatus.DAMAGE, InventoryHoldStatus.OK))
                .surplus(true)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Collections.singletonList(InventoryHoldStatus.DAMAGE));
        assertions.assertThat(maybeLot).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before.xml", assertionMode = NON_STRICT)
    public void findAnyLotWithShelfLifesWhenItIsNotExistBecauseOfShelfLifes() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-18T12:00:00.000Z"))
                .inventoryHoldStatuses(ImmutableSet.of(InventoryHoldStatus.DAMAGE, InventoryHoldStatus.OK))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Collections.singletonList(InventoryHoldStatus.DAMAGE));
        assertions.assertThat(maybeLot).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before-with-different-inventory-holds.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before-with-different-inventory-holds.xml", assertionMode =
            NON_STRICT)
    public void findAnyLotWithShelfLifesAndOneInventoryHoldWhenItIsNotExistBecauseOfInventoryHold() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(Collections.singleton(InventoryHoldStatus.OK))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Collections.singletonList(InventoryHoldStatus.OK));
        assertions.assertThat(maybeLot).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before-with-different-inventory-holds.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before-with-different-inventory-holds.xml", assertionMode =
            NON_STRICT)
    public void findAnyLotWithShelfLifesAndOneInventoryHoldsWhenItIsNotExistBecauseThereIsAnotherInventoryHoldForLot() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(ImmutableSet.of(InventoryHoldStatus.DAMAGE, InventoryHoldStatus.OK))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Collections.singletonList(InventoryHoldStatus.DAMAGE));
        assertions.assertThat(maybeLot).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before-with-different-inventory-holds.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before-with-different-inventory-holds.xml", assertionMode =
            NON_STRICT)
    public void findAnyLotWithShelfLifesAndTwoInventoryHoldsWhenItIsNotExistBecauseOfInventoryHoldType() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(ImmutableSet.of(InventoryHoldStatus.DAMAGE, InventoryHoldStatus.OK,
                        InventoryHoldStatus.EXPIRED))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Arrays.asList(InventoryHoldStatus.DAMAGE, InventoryHoldStatus.EXPIRED));
        assertions.assertThat(maybeLot).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before-with-different-inventory-holds.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before-with-different-inventory-holds.xml", assertionMode =
            NON_STRICT)
    public void findAnyLotWithShelfLifesAndTwoInventoryHoldsWhenItIsNotExistBecauseOfInventoryHoldStatus() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001457").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .creationDateTime(Instant.parse("2020-04-17T12:00:00.000Z"))
                .inventoryHoldStatuses(ImmutableSet.of(InventoryHoldStatus.DAMAGE, InventoryHoldStatus.OK,
                        InventoryHoldStatus.DAMAGE_DISPOSAL))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Arrays.asList(InventoryHoldStatus.DAMAGE, InventoryHoldStatus.DAMAGE_DISPOSAL));
        assertions.assertThat(maybeLot).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before-without-shelf-lifes.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before-without-shelf-lifes.xml", assertionMode = NON_STRICT)
    public void findAnyLotWithoutShelfLifesWhenItIsNotExistBecauseOfShelfLifes() {
        LotInputData lotInputData = LotInputData.builder()
                .sku(Sku.builder().sku("ROV0000000000000001456").storerKey("465852").build())
                .expirationDateTime(Instant.parse("2020-04-25T15:00:00.000Z"))
                .inventoryHoldStatuses(ImmutableSet.of(InventoryHoldStatus.DAMAGE, InventoryHoldStatus.OK))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .build();
        Optional<Lot> maybeLot = lotDao.findAnyLot(lotInputData, LotStatus.OK,
                Collections.singletonList(InventoryHoldStatus.DAMAGE));
        assertions.assertThat(maybeLot).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before.xml", assertionMode = NON_STRICT)
    public void findByLotWhenItExists() {
        Optional<LotWithUnnamedAttributes> maybeLotWithAttributes = lotDao.findByLot("0000012345");
        assertions.assertThat(maybeLotWithAttributes).isPresent();
        LotWithUnnamedAttributes lotWithAttributes = maybeLotWithAttributes.get();
        Lot lot = lotWithAttributes.getLot();
        UnnamedLotAttributes lotAttributes = lotWithAttributes.getLotAttributes();
        assertFoundLotIsCorrect(lot, "0000012345");
        assertFoundLotAttributesAreCorrect(lotAttributes);
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot/before/before.xml", assertionMode = NON_STRICT)
    public void findByLotWhenItNotExists() {
        Optional<LotWithUnnamedAttributes> maybeLotWithAttributes = lotDao.findByLot("0000012347");

        assertions.assertThat(maybeLotWithAttributes).isNotPresent();
    }

    @Test
    @DatabaseSetups({@DatabaseSetup(value = "/db/dao/lot/before/before.xml", type = DatabaseOperation.INSERT),
            @DatabaseSetup(value = "/db/dao/lot/before/before-fit.xml", type = DatabaseOperation.INSERT)})
    public void findFitQtyBySkuIds() {
        SkuId skuId = new SkuId("465852", "ROV0000000000000001456");
        SkuId skuId2 = new SkuId("465852", "ROV0000000000000001457");
        Map<SkuId, Integer> result = lotDao.getFitQuantityBySkuIds(List.of(skuId, skuId2));
        assertions.assertThat(result.containsKey(skuId));
    }

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/lot/after/after-create.xml", assertionMode = NON_STRICT)
    public void createLot() {
        Lot lot = Lot.builder()
                .sku("ROV0000000000000001456")
                .storerKey("465852")
                .lot("0000012345")
                .lotStatus(LotStatus.OK)
                .addWho("TEST")
                .editWho("TEST")
                .lotAggregatedFields(createLotAggregatedFields(1, 0, 4.33, 3.49, 2.15))
                .build();

        lotDao.createLot(lot);
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/lot/after/after-allocate-1.xml",
            assertionMode = NON_STRICT,
            columnFilters = {EditDateFilter.class}
    )
    public void updateLocAllocatedQtyWithSelfHealTest() {
        String sku = "ROV0000000000000001456";
        String lot = "0000012345";
        String storerKey = "465852";
        String user = "TEST";
        int qtyAllocated = 1;

        lotDao.updateLocAllocatedQtyWithSelfHeal(qtyAllocated, lot, sku, storerKey, user);
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/lot/after/after-allocate-2.xml",
            assertionMode = NON_STRICT,
            columnFilters = {EditDateFilter.class}
    )
    public void updateLocAllocatedQtyWithSelfHealTestWhenQtyLess() {
        String sku = "ROV0000000000000001456";
        String lot = "0000012345";
        String storerKey = "465852";
        String user = "TEST";
        int qtyAllocated = 15;

        lotDao.updateLocAllocatedQtyWithSelfHeal(qtyAllocated, lot, sku, storerKey, user);
    }

    @Test
    @DatabaseSetup("/db/dao/lot/before/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot/after/after-update.xml", assertionMode = NON_STRICT)
    public void addOneQtyAndQtyOnHoldToLot() {
        lotDao.addToAggregatedFields("0000012345",
                createLotAggregatedFields(1, 0, 10.16, 9.55, 1.15), "TEST");
    }

    private void assertFoundLotIsCorrect(Lot lot, String expectedLot) {
        assertions.assertThat(lot.getLot()).isEqualTo(expectedLot);
        assertions.assertThat(lot.getLotStatus()).isEqualTo(LotStatus.OK);
        LotAggregatedFields aggregatedFields = lot.getLotAggregatedFields();
        assertions.assertThat(aggregatedFields.getQuantity()).isEqualTo(10);
        SkuDimensions dimensions = aggregatedFields.getDimensions();
        assertions.assertThat(dimensions.getCube()).isEqualByComparingTo(BigDecimal.valueOf(15.15));
        assertions.assertThat(dimensions.getGrossWeight()).isEqualByComparingTo(BigDecimal.valueOf(100.25));
        assertions.assertThat(dimensions.getNetWeight()).isEqualByComparingTo(BigDecimal.valueOf(96.53));
        assertions.assertThat(lot.getAddWho()).isEqualTo("TEST1");
        assertions.assertThat(lot.getEditWho()).isEqualTo("TEST2");
    }

    private void assertFoundLotAttributesAreCorrect(UnnamedLotAttributes lotAttributes) {
        assertions.assertThat(lotAttributes.getLottable01()).isEqualTo("lottable01");
        assertions.assertThat(lotAttributes.getLottable02()).isEqualTo("lottable02");
        assertions.assertThat(lotAttributes.getLottable03()).isEqualTo("lottable03");
        assertions.assertThat(lotAttributes.getLottable04()).isEqualTo("2020-04-17 12:00:00");
        assertions.assertThat(lotAttributes.getLottable05()).isEqualTo("2020-04-25 15:00:00");
        assertions.assertThat(lotAttributes.getLottable06()).isEqualTo("lottable06");
        assertions.assertThat(lotAttributes.getLottable07()).isEqualTo("");
        assertions.assertThat(lotAttributes.getLottable08()).isEqualTo("1");
        assertions.assertThat(lotAttributes.getLottable09()).isEqualTo("ALL");
        assertions.assertThat(lotAttributes.getLottable10()).isEqualTo("0000012345");
        assertions.assertThat(lotAttributes.getLottable11()).isNull();
        assertions.assertThat(lotAttributes.getLottable12()).isNull();
    }
}
