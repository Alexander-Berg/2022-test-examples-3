package ru.yandex.market.wms.timetracker.service;

import java.util.Objects;
import java.util.Optional;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.wms.timetracker.dao.postgres.WarehouseDao;
import ru.yandex.market.wms.timetracker.model.WarehouseModel;

import static org.hamcrest.Matchers.samePropertyValuesAs;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        WarehouseServiceTest.TestConfig.class,
        WarehouseService.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
class WarehouseServiceTest {

    @Autowired
    private WarehouseDao warehouseDao;

    @Autowired
    private WarehouseService warehouseService;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        WarehouseDao warehouseDao() {
            return Mockito.mock(WarehouseDao.class);
        }
    }

    @Test
    void readWarehouseWhenEmpty() {
        Mockito.when(warehouseDao.findByName(ArgumentMatchers.eq("SOF")))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                RuntimeException.class,
                () -> warehouseService.readWarehouse("SOF"));
    }

    @Test
    void readWarehouse() {
        WarehouseModel warehouseModel = WarehouseModel.builder()
                .id(1L)
                .build();

        Mockito.when(warehouseDao.findByName(ArgumentMatchers.eq("SOF")))
                .thenReturn(Optional.ofNullable(warehouseModel));

        final WarehouseModel result = warehouseService.readWarehouse("SOF");

        MatcherAssert.assertThat(result, samePropertyValuesAs(Objects.requireNonNull(warehouseModel)));
    }
}
