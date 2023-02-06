package ru.yandex.market.mboc.common.logisticsparams.warehouse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.jooq.repo.JooqRepository;
import ru.yandex.market.mboc.common.BaseJooqRepositoryTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.WarehouseType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.WarehouseUsingType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Warehouse;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class WarehouseRepositoryTest extends BaseJooqRepositoryTestClass<Warehouse, Long> {

    public static final List<Warehouse> WAREHOUSE_LIST = Arrays.asList(
        new Warehouse().setId(145L).setName("Маршрут ФФ")
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
            .setCalendaringEnabled(false).setType(WarehouseType.FULFILLMENT),
        new Warehouse().setId(147L).setName("Яндекс.Маркет Ростов")
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
            .setCalendaringEnabled(true).setType(WarehouseType.FULFILLMENT),
        new Warehouse().setId(163L).setName("Лаборатория Контента")
            .setCalendaringEnabled(true).setType(WarehouseType.FULFILLMENT),
        new Warehouse().setId(171L).setName("Яндекс Маркет Томилино")
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
            .setCalendaringEnabled(false).setType(WarehouseType.FULFILLMENT),
        new Warehouse().setId(172L).setName("Яндекс Маркет Софьино")
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
            .setCalendaringEnabled(false).setType(WarehouseType.FULFILLMENT),
        new Warehouse().setId(999999999L).setName("PEK50 x-dock")
            .setCalendaringEnabled(true).setType(WarehouseType.FULFILLMENT),
        new Warehouse().setId(42323L).setName("Dropship 1")
            .setCalendaringEnabled(true).setType(WarehouseType.DROPSHIP)
    );

    @Autowired
    private WarehouseRepository repository;

    public WarehouseRepositoryTest() {
        super(Warehouse.class, Warehouse::getId);
        generatedFields = new String[]{"modifiedAt"};
    }

    @Override
    protected JooqRepository<Warehouse, ?, Long, ?, ?> repository() {
        return repository;
    }

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @Test
    public void filterWithEmptyWarehouseListShouldReturnEmptyResult() {
        repository.save(WAREHOUSE_LIST);
        List<Warehouse> warehouses = repository.find(new WarehouseRepository.Filter().setWarehouseIds(new HashSet<>()));
        assertThat(warehouses).isEmpty();
    }

    @Test
    public void findAllTest() {
        repository.save(WAREHOUSE_LIST);
        List<Warehouse> warehouses = repository.findAll();
        assertThat(warehouses)
            .usingElementComparatorIgnoringFields(generatedFields)
            .containsExactlyElementsOf(WAREHOUSE_LIST);
    }

    @Test
    public void findByIdsTest() {
        repository.save(WAREHOUSE_LIST);
        List<Warehouse> houses = repository
            .find(new WarehouseRepository.Filter().setWarehouseIds(List.of(
                WAREHOUSE_LIST.get(0).getId(),
                WAREHOUSE_LIST.get(1).getId())));
        assertThat(houses)
            .usingElementComparatorIgnoringFields(generatedFields)
            .containsExactlyInAnyOrder(WAREHOUSE_LIST.get(0), WAREHOUSE_LIST.get(1));
    }

    @Test
    public void findByNamesTest() {
        repository.save(WAREHOUSE_LIST);
        List<Warehouse> houses = repository
            .find(new WarehouseRepository.Filter().setNames(List.of(
                WAREHOUSE_LIST.get(0).getName(),
                WAREHOUSE_LIST.get(1).getName())));
        assertThat(houses)
            .usingElementComparatorIgnoringFields(generatedFields)
            .containsExactlyInAnyOrder(WAREHOUSE_LIST.get(0), WAREHOUSE_LIST.get(1));
    }

}
