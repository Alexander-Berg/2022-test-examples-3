package ru.yandex.market.wms.radiator.repository;

import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.entity.Storer;
import ru.yandex.market.wms.radiator.entity.StorerType;
import ru.yandex.market.wms.radiator.model.DatabaseSchema;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;

public class StorerRepositoryTest extends IntegrationTestBackend {

    @Autowired
    private StorerRepository repository;
    @Autowired
    private Dispatcher dispatcher;

    private static final List<Storer> storers = Arrays.asList(
            Storer.builder()
                    .code("1005005")
                    .cartonGroup("PK")
                    .name("КСЭ - МСК")
                    .supportsMultipacking("1")
                    .type(StorerType.CARRIER)
                    .build()
    );


    @Test
    @DatabaseSetup(value = "/fixtures/dbStorer/before.xml", connection = "wh1Connection")
    @ExpectedDatabase(
            value = "/fixtures/dbStorer/after.xml",
            connection = "wh1Connection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        insert(WH_1_ID);
    }

    private void insert(String warehouseId) {
        dispatcher.withWarehouseId(
                warehouseId,
                () -> repository.insert(storers, DatabaseSchema.WMWHSE1, "test")
        );
    }
}
