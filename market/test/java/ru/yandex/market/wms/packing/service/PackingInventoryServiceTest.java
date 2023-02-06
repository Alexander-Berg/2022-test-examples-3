package ru.yandex.market.wms.packing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.pojo.Carton;
import ru.yandex.market.wms.packing.enums.PackingSourceType;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.pojo.CloseParcelDto;
import ru.yandex.market.wms.packing.pojo.PackingTaskItem;
import ru.yandex.market.wms.packing.pojo.Sku;
import ru.yandex.market.wms.packing.pojo.SortingCell;

class PackingInventoryServiceTest extends IntegrationTest {

    @Autowired
    PackingInventoryService packingInventoryService;

    @Test
    @DatabaseSetup("/db/service/packing-inventory/1/before.xml")
    @ExpectedDatabase(value = "/db/service/packing-inventory/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void parcelWeightShouldBeConsideredIfProvided() {
        Sku sku = new Sku("storer", "sku", "putaway", "test sku",
                2.0, false, new HashSet<>(), 4.0, 3.0, 1.0, 12.0);

        SortingCell cell = SortingCell.builder().loc("table_loc").id("cell_id").build();
        String lot = "lot";
        List<PackingTaskItem> parcelItems = new ArrayList<>();

        PackingTaskItem item1 = buildTestItem(sku, cell, new BigDecimal(1), "pd1", lot);
        PackingTaskItem item2 = buildTestItem(sku, cell, new BigDecimal(2), "pd2", lot);
        PackingTaskItem item3 = buildTestItem(sku, cell, new BigDecimal(3), "pd3", lot);

        parcelItems.add(item1);
        parcelItems.add(item2);
        parcelItems.add(item3);

        Carton carton = Carton.builder().group("PK").type("MYA")
                .length(10.0d).width(10.0d).height(20.0d).volume(2000).tareWeight(5.0d).build();


        CloseParcelDto closeParcelDto = CloseParcelDto.builder()
                .selectedCarton(carton)
                .items(parcelItems)
                .orderKey("test_order")
                .parcelId("parcel_id")
                .tableLoc("table_loc")
                .ticketType(TicketType.SORTABLE)
                .sourceType(PackingSourceType.NEW_PACKING)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .user("test_user")
                .build();

        packingInventoryService.closeParcel(closeParcelDto);
    }

    @Test
    @DatabaseSetup("/db/service/packing-inventory/2/before.xml")
    @ExpectedDatabase(value = "/db/service/packing-inventory/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void parcelWeightShouldBeSafelyIgnoredIfAbsent() {
        Sku sku = new Sku("storer", "sku", "putaway", "test sku",
                2.0, false, new HashSet<>(), 4.0, 3.0, 1.0, 12.0);

        SortingCell cell = SortingCell.builder().loc("table_loc").id("cell_id").build();
        String lot = "lot";
        List<PackingTaskItem> parcelItems = new ArrayList<>();

        PackingTaskItem item1 = buildTestItem(sku, cell, new BigDecimal(1), "pd1", lot);
        PackingTaskItem item2 = buildTestItem(sku, cell, new BigDecimal(2), "pd2", lot);
        PackingTaskItem item3 = buildTestItem(sku, cell, new BigDecimal(3), "pd3", lot);

        parcelItems.add(item1);
        parcelItems.add(item2);
        parcelItems.add(item3);

        Carton carton = Carton.builder().group("PK").type("MYA")
                .length(10.0d).width(10.0d).height(20.0d).volume(2000).build();

        CloseParcelDto closeParcelDto = CloseParcelDto.builder()
                .selectedCarton(carton)
                .items(parcelItems)
                .orderKey("test_order")
                .parcelId("parcel_id")
                .tableLoc("table_loc")
                .ticketType(TicketType.SORTABLE)
                .sourceType(PackingSourceType.NEW_PACKING)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .user("test_user")
                .build();

        packingInventoryService.closeParcel(closeParcelDto);
    }

    @Test
    @DatabaseSetup("/db/service/packing-inventory/3/before.xml")
    @ExpectedDatabase(value = "/db/service/packing-inventory/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void parcelNonpack() {
        Sku sku1 = new Sku("465852", "ROV0000000000000000004", "putaway", "test sku",
                5.2, false, new HashSet<>(), 7.0, 1.0, 2.0, 14.0);
        Sku sku2 = new Sku("465852", "ROV0000000000000000005", "putaway", "test sku",
                2.2, false, new HashSet<>(), 2.0, 10.0, 7.0, 70.0);
        Sku sku3 = new Sku("465852", "ROV0000000000000000006", "putaway", "test sku",
                1.6, false, new HashSet<>(), 3.0, 9.0, 14.0, 378.0);

        SortingCell cell = SortingCell.builder().loc("table_loc").id("cell_id").build();
        String lot = "lot";
        List<PackingTaskItem> parcelItems = new ArrayList<>();

        PackingTaskItem item1 = buildTestItem(sku1, cell, new BigDecimal(1), "pd1", lot);
        PackingTaskItem item2 = buildTestItem(sku2, cell, new BigDecimal(2), "pd2", lot);
        PackingTaskItem item3 = buildTestItem(sku3, cell, new BigDecimal(3), "pd3", lot);

        parcelItems.add(item1);
        parcelItems.add(item2);
        parcelItems.add(item3);

        Carton carton = Carton.builder().group("PK").type("NONPACK").build();

        CloseParcelDto closeParcelDto = CloseParcelDto.builder()
                .selectedCarton(carton)
                .items(parcelItems)
                .orderKey("test_order")
                .parcelId("parcel_id")
                .tableLoc("table_loc")
                .ticketType(TicketType.SORTABLE)
                .sourceType(PackingSourceType.NEW_PACKING)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .user("test_user")
                .build();

        packingInventoryService.closeParcel(closeParcelDto);
    }

    @Test
    @DatabaseSetup("/db/service/packing-inventory/3/before.xml")
    @ExpectedDatabase(value = "/db/service/packing-inventory/3/after-stretch.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void parcelStretch() {
        Sku sku1 = new Sku("465852", "ROV0000000000000000004", "putaway", "test sku",
                5.2, false, new HashSet<>(), 7.0, 1.0, 2.0, 14.0);
        Sku sku2 = new Sku("465852", "ROV0000000000000000005", "putaway", "test sku",
                2.2, false, new HashSet<>(), 2.0, 10.0, 7.0, 70.0);
        Sku sku3 = new Sku("465852", "ROV0000000000000000006", "putaway", "test sku",
                1.6, false, new HashSet<>(), 3.0, 9.0, 14.0, 378.0);

        SortingCell cell = SortingCell.builder().loc("table_loc").id("cell_id").build();
        String lot = "lot";
        List<PackingTaskItem> parcelItems = new ArrayList<>();

        PackingTaskItem item1 = buildTestItem(sku1, cell, new BigDecimal(1), "pd1", lot);
        PackingTaskItem item2 = buildTestItem(sku2, cell, new BigDecimal(2), "pd2", lot);
        PackingTaskItem item3 = buildTestItem(sku3, cell, new BigDecimal(3), "pd3", lot);

        parcelItems.add(item1);
        parcelItems.add(item2);
        parcelItems.add(item3);

        Carton carton = Carton.builder().group("PK").type("STRETCH").build();

        CloseParcelDto closeParcelDto = CloseParcelDto.builder()
                .selectedCarton(carton)
                .items(parcelItems)
                .orderKey("test_order")
                .parcelId("parcel_id")
                .tableLoc("table_loc")
                .ticketType(TicketType.SORTABLE)
                .sourceType(PackingSourceType.NEW_PACKING)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .user("test_user")
                .build();

        packingInventoryService.closeParcel(closeParcelDto);
    }

    private PackingTaskItem buildTestItem(Sku sku, SortingCell cell, BigDecimal qty, String pickDetailKey, String lot) {
        return PackingTaskItem.builder()
                .sku(sku)
                .qty(qty)
                .sortingCell(cell)
                .pickDetailKey(pickDetailKey)
                .lot(lot)
                .build();
    }



}
