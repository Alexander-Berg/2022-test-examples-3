package ru.yandex.market.logistics.iris.service.export;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.Fulfillment;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static org.mockito.Mockito.verify;

public class SkuAvailabilityFileUpdaterFacadeTest extends AbstractContextualTest {

    @Autowired
    MdsRepository mdsRepository;

    @Autowired
    private SkuAvailabilityFileUpdaterFacade facade;

    private static final Fulfillment.Korobyte DEFAULT_KOROBYTES = Fulfillment.Korobyte.newBuilder()
            .setWeight(1.0)
            .setHeight(100.0)
            .setWidth(100.0)
            .setLength(100.0)
            .build();

    private static final Fulfillment.Warehouses DEFAULT_MESSAGE = Fulfillment.Warehouses.newBuilder()
            .addStocks(Fulfillment.WarehouseStocks.newBuilder()
                    .setWarehouseId(147)
                    .addStocks(createStocks("sku", 123455, DEFAULT_KOROBYTES)).build()
            ).build();

    /**
     * Кейс, когда в базе несколько айтемов с разными типами.
     * <p>
     * В ответе должен сформироваться файл с единственной записью у которой валидные вгх и sourceType = WAREHOUSE.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/sku_availability_file_updater/items.xml")
    public void updateOnItemsFromDifferentSources() throws IOException {
        facade.instantUpdate();

        verify(mdsRepository).set(DEFAULT_MESSAGE);
    }

    @Test
    public void updateOnEmptyDataBase() throws IOException {
        Fulfillment.Warehouses expectedMessage = Fulfillment.Warehouses.newBuilder().build();

        facade.instantUpdate();

        verify(mdsRepository).set(expectedMessage);
    }

    /**
     * Кейс с проверкой на фильтрацию айтемов с невалидными вгх.
     * В базе данных:
     * - один валидный айтем
     * - 16 айтемов со всеми комбинациями для полей вгх по значениям 0/1.
     * - 16 айтемов со всеми комбинациями для полей вгх по значениям null/1.
     * - айтем у которого отсутсвуют габариты
     * - айтем у которого нет веса
     * - 4 айтема с вгх не числового типа.
     * <p>
     * В ответе должен сформироваться файл с единственной записью у которой валидные вгх.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/sku_availability_file_updater/items-with-invalid-korobytes.xml")
    public void updateOnItemsWithZeroKorobytes() throws IOException {
        facade.instantUpdate();

        verify(mdsRepository).set(DEFAULT_MESSAGE);
    }

    /**
     * Число валидных айтемов кратно размеру батча.
     * <p>
     * В БД 4 валидных айтема. Размер батча 2.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/sku_availability_file_updater/four-items.xml")
    public void updateOnItemsSizeMultipleOfBatchSize() throws IOException {
        Fulfillment.Warehouses expectedMessage = Fulfillment.Warehouses.newBuilder()
                .addStocks(Fulfillment.WarehouseStocks.newBuilder()
                        .setWarehouseId(147)
                        .addStocks(createStocks("sku1", 123455, DEFAULT_KOROBYTES))
                        .addStocks(createStocks("sku2", 123455, DEFAULT_KOROBYTES))
                        .addStocks(createStocks("sku3", 123455, DEFAULT_KOROBYTES))
                        .addStocks(createStocks("sku4", 123455, DEFAULT_KOROBYTES))
                        .build()
                ).build();

        facade.instantUpdate();

        verify(mdsRepository).set(expectedMessage);
    }

    /**
     * Число валидных айтемов не кратно размеру батча.
     * <p>
     * В БД 5 валидных айтемов. Размер батча 2.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/sku_availability_file_updater/five-items.xml")
    public void updateOnItemsWhichSizeIsNotMultipleOfBatchSize() throws IOException {
        Fulfillment.Warehouses expectedMessage = Fulfillment.Warehouses.newBuilder()
                .addStocks(Fulfillment.WarehouseStocks.newBuilder()
                        .setWarehouseId(147)
                        .addStocks(createStocks("sku1", 123455, DEFAULT_KOROBYTES))
                        .addStocks(createStocks("sku2", 123455, DEFAULT_KOROBYTES))
                        .addStocks(createStocks("sku3", 123455, DEFAULT_KOROBYTES))
                        .addStocks(createStocks("sku4", 123455, DEFAULT_KOROBYTES))
                        .addStocks(createStocks("sku5", 123455, DEFAULT_KOROBYTES))
                        .build()
                ).build();

        facade.instantUpdate();

        verify(mdsRepository).set(expectedMessage);
    }

    /**
     * Размер батча больше числа валидных айтемов.
     * <p>
     * В БД 1 валидный айтем. Размер батча 2.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/sku_availability_file_updater/items.xml")
    public void updateOnItemsWhichSizeIsLessThenBatchSize() throws IOException {
        facade.instantUpdate();

        verify(mdsRepository).set(DEFAULT_MESSAGE);
    }

    private static Fulfillment.Stocks createStocks(String sku,
                                                   int vendorId,
                                                   Fulfillment.Korobyte korobytes) {
        return Fulfillment.Stocks.newBuilder()
                .setSku(sku)
                .addShops(Fulfillment.Shops.newBuilder()
                        .setShopId(vendorId)
                        .setKorobyte(korobytes)
                ).build();
    }
}
