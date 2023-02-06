package ru.yandex.market.mbi.tariffs.service.courier_fine;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.model.CourierFineType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

/**
 * Тесты для {@link CourierFineTypeService}
 */
public class CourierFineTypeServiceTest extends FunctionalTest {

    @Autowired
    private CourierFineTypeService courierFineTypeService;

    @BeforeEach
    void setUp() {
        if (courierFineTypeService instanceof CachedCourierFineTypeService cached) {
            cached.invalidateCache();
        }
    }

    @Test
    @DbUnitDataSet(before = "/ru/yandex/market/mbi/tariffs/service/courier_fine/getAll.before.csv")
    void testGetAll() {
        List<CourierFineType> all = courierFineTypeService.getAll();

        assertThat(all, hasSize(3));
        assertThat(
                all.stream().map(CourierFineType::getCode).toList(),
                containsInAnyOrder("a", "b", "c")
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/mbi/tariffs/service/courier_fine/saveAll.before.csv",
            after = "/ru/yandex/market/mbi/tariffs/service/courier_fine/saveAll.after.csv"
    )
    void testSaveAll() {
        courierFineTypeService.saveAll(List.of(
                new CourierFineType()
                        .code("a")
                        .name("shortA"),
                new CourierFineType()
                        .code("c")
                        .name("shortC")
        ));
    }
}
